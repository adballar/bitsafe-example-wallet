/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.dev;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.protobuf.ByteString;
import io.bitsafe.examplewallet.bitcoin.AddressGenerator;
import io.bitsafe.examplewallet.bitcoin.TransactionSigner;
import io.bitsafe.examplewallet.dev.Messages.ButtonAck;
import io.bitsafe.examplewallet.dev.Messages.Failure;
import io.bitsafe.examplewallet.dev.Messages.GetNumberOfAddresses;
import io.bitsafe.examplewallet.dev.Messages.NewAddress;
import io.bitsafe.examplewallet.dev.Messages.NumberOfAddresses;
import io.bitsafe.examplewallet.dev.Messages.SignTransaction;
import io.bitsafe.examplewallet.dev.Messages.Signature;
import io.bitsafe.examplewallet.gui.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import org.spongycastle.util.Arrays;
/**
 *
 * @author Chris Chua
 */
public class Wallet implements AddressGenerator, TransactionSigner {

    private transient final PacketWrapper bitsafe;
    private transient final Console console;
    private final int walletNumber;

    private int numberOfAddresses;
    private ArrayList<Address> addresses;

    public Wallet(PacketWrapper inBitsafe, Console inConsole, int inWalletNumber) {
        bitsafe = inBitsafe;
        console = inConsole;
        walletNumber = inWalletNumber;
        addresses = new ArrayList<>();
    }

    // Should this be done in constructor? It can take a long time, especially
    // if the wallet has lots of addresses.
    public void sync(com.google.bitcoin.core.Wallet bitcoinjWallet) {
        try {
            // Synchronise number of addresses.
            bitsafe.sendPacket(Packet.PACKET_TYPE_GET_NUM_ADDRESSES, GetNumberOfAddresses.newBuilder());
            Packet p = bitsafe.receivePacket();
            if (p.getType() == Packet.PACKET_TYPE_NUM_ADDRESSES) {
                NumberOfAddresses numAddresses = NumberOfAddresses.parseFrom(p.getStorage());
                numberOfAddresses = numAddresses.getNumberOfAddresses();
            } else if (p.getType() == Packet.PACKET_TYPE_FAILURE) {
                throw new BitSafeFailureException(Messages.Failure.parseFrom(p.getStorage()));
            } else {
                throw new UnexpectedResponseException(p.getType(), "GetNumberOfAddresses");
            }
            // Get all addresses that we don't have.
            for (int i = addresses.size(); i < numberOfAddresses; i++) {
                // Address handles start at 1 and are sequential.
                Address addr = new Address(i + 1, bitsafe, console);
                addresses.add(addr);
            }
        } catch (IOException | BitSafeFailureException | UnexpectedResponseException e) {
            console.log(e);
        }

        // Ensure the bitcoinj wallet has all the keys in there.
        for (int i = 0; i < numberOfAddresses; i++) {
            Address addr = addresses.get(i);
            if (addr != null) {
                bitcoinjWallet.addKey(addresses.get(i).toECKey());
            }
        }
    }

    public static File getWalletFile(String directoryPrefix, String filePrefix, int inWalletNumber) {
        File directory = new File(directoryPrefix);
        return new File(directory, filePrefix + ".addresses" + inWalletNumber);
    }

    public void saveToFile(String directoryPrefix, String filePrefix) {
        File f = getWalletFile(directoryPrefix, filePrefix, walletNumber);
        try {
            FileOutputStream fos = new FileOutputStream(f);
            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeInt(numberOfAddresses);
                oos.writeObject(addresses);
            }
        } catch (IOException e) {
            console.log(e);
        }
    }

    public void loadFromFile(String directoryPrefix, String filePrefix) {
        File f = getWalletFile(directoryPrefix, filePrefix, walletNumber);
        try {
            FileInputStream fis = new FileInputStream(f);
            try (ObjectInputStream ois = new ObjectInputStream(fis)) {
                numberOfAddresses = ois.readInt();
                addresses = (ArrayList<Address>)ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            console.log(e);
        }
    }

    @Override
    public ECKey generateNewKey() {
        try {
            bitsafe.sendPacket(Packet.PACKET_TYPE_NEW_ADDRESS, NewAddress.newBuilder());
            while (true) {
                Packet p = bitsafe.receivePacket();
                if (p.getType() == Packet.PACKET_TYPE_ADDRESS_PUBKEY) {
                    Messages.Address addrM = Messages.Address.parseFrom(p.getStorage());
                    Address newAddr = new Address(
                            addrM.getAddressHandle(),
                            addrM.getPublicKey().toByteArray(),
                            addrM.getAddress().toByteArray());
                    addresses.add(newAddr);
                    numberOfAddresses++;
                    return newAddr.toECKey();
                } else if (p.getType() == Packet.PACKET_TYPE_FAILURE) {
                    throw new BitSafeFailureException(Messages.Failure.parseFrom(p.getStorage()));
                } else if (p.getType() == Packet.PACKET_TYPE_BUTTON_REQUEST) {
                    bitsafe.sendPacket(Packet.PACKET_TYPE_BUTTON_ACK, ButtonAck.newBuilder());
                } else {
                    throw new UnexpectedResponseException(p.getType(), "NewAddress");
                }
            }
        } catch (IOException | BitSafeFailureException | UnexpectedResponseException e) {
            console.log(e);
        }
        return null;
    }

    public ArrayList<Address> getAddresses() {
        return addresses;
    }

    public int getWalletNumber() {
        return walletNumber;
    }

    private Address getAddressByHash160(byte[] in) {
        // TODO: use HashMap for addresses
        for (int i = 0; i < addresses.size(); i++) {
            Address a = addresses.get(i);
            if (Arrays.areEqual(in, a.getAddress())) {
                return a;
            }
        }
        return null;
    }

    @Override
    public boolean signTransaction(Transaction t, com.google.bitcoin.core.Wallet w) {
        int numInputs = t.getInputs().size();
        byte[][] signatures = new byte[numInputs][];
        ECKey[] pubKeys = new ECKey[numInputs];
        boolean[] signaturesValid = new boolean[numInputs];

        for (int i = 0; i < numInputs; i++) {
            signaturesValid[i] = false;
        }

        // Collect Bitcoin-encoded signatures from BitSafe.
        try {
            for (int i = 0; i < numInputs; i++) {
                // getSignTransactionStream mangles the input scripts of t.
                // But that doesn't matter since the actual signatures are
                // included in a separate loop below.
                byte[] transactionData = TransactionFormatter.getSignTransactionStream(t, i);
                SignTransaction.Builder signTransaction = SignTransaction.newBuilder();
                ECKey key = t.getInput(i).getOutpoint().getConnectedKey(w);
                pubKeys[i] = key;
                // TODO: deal with possible null
                signTransaction.setAddressHandle(getAddressByHash160(key.getPubKeyHash()).getAddressHandle());
                signTransaction.setTransactionData(ByteString.copyFrom(transactionData));
                Packet p = new Packet(Packet.PACKET_TYPE_SIGN_TRANSACTION, signTransaction.build().toByteArray());
                bitsafe.sendPacket(p);
                while (true) {
                    p = bitsafe.receivePacket();
                    if (p.getType() == Packet.PACKET_TYPE_SIGNATURE) {
                        Signature signature = Signature.parseFrom(p.getStorage());
                        signatures[i] = signature.getSignatureData().toByteArray();
                        signaturesValid[i] = true;
                        break;
                    } else if (p.getType() == Packet.PACKET_TYPE_FAILURE) {
                        throw new BitSafeFailureException(Failure.parseFrom(p.getStorage()));
                    } else if (p.getType() == Packet.PACKET_TYPE_BUTTON_REQUEST) {
                        bitsafe.sendPacket(Packet.PACKET_TYPE_BUTTON_ACK, ButtonAck.newBuilder());
                    } else {
                        throw new UnexpectedResponseException(p.getType(), "SignTransaction");
                    }
                }
            }
        } catch (IOException | BitSafeFailureException | UnexpectedResponseException e) {
            console.log(e);
            return false;
        }

        // Sanity check.
        for (int i = 0; i < numInputs; i++) {
            if (!signaturesValid[i]) {
                return false;
            }
        }

        console.log("Sent:\n");
        for (int i = 0; i < t.getOutputs().size(); i++) {
            console.log(t.getOutput(i) + "\n");
        }

        // Include signatures in transaction.
        for (int i = 0; i < numInputs; i++) {
            TransactionSignature txSig = TransactionSignature.decodeFromBitcoin(signatures[i], true);
            t.getInput(i).setScriptSig(ScriptBuilder.createInputScript(txSig, pubKeys[i]));
        }
        return true;
    }

}
