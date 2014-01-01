/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.dev;

import com.google.bitcoin.core.ECKey;
import io.bitsafe.examplewallet.dev.Messages.GetAddressAndPublicKey;
import io.bitsafe.examplewallet.dev.Messages.Failure;
import io.bitsafe.examplewallet.gui.Console;
import java.io.IOException;
import java.io.Serializable;

/** View of a BitSafe address.
 *
 * @author Chris Chua
 */
public class Address implements Serializable {
    private final int addressHandle;
    private final byte[] publicKey;
    private final byte[] address;

    public Address(int inAddressHandle, byte[] inPublicKey, byte[] inAddress) {
        addressHandle = inAddressHandle;
        publicKey = inPublicKey;
        address = inAddress;
    }

    public Address(int inAddressHandle, PacketWrapper bitsafe, Console console)
            throws IOException, BitSafeFailureException, UnexpectedResponseException {
        addressHandle = inAddressHandle;
        GetAddressAndPublicKey.Builder getAddress = GetAddressAndPublicKey.newBuilder();
        getAddress.setAddressHandle(addressHandle);
        Packet p = new Packet(Packet.PACKET_TYPE_GET_ADDRESS_PUBKEY, getAddress.build().toByteArray());
        bitsafe.sendPacket(p);
        p = bitsafe.receivePacket();
        if (p.getType() == Packet.PACKET_TYPE_ADDRESS_PUBKEY) {
            Messages.Address addrM = Messages.Address.parseFrom(p.getStorage());
            publicKey = addrM.getPublicKey().toByteArray();
            address = addrM.getAddress().toByteArray();
        } else if (p.getType() == Packet.PACKET_TYPE_FAILURE) {
            throw new BitSafeFailureException(Failure.parseFrom(p.getStorage()));
        } else {
            throw new UnexpectedResponseException(p.getType(), "GetAddressAndPublicKey");
        }
    }

    public ECKey toECKey() {
        byte[] privKeyBytes = new byte[32]; // use junk
        ECKey key = new ECKey(privKeyBytes, publicKey);
        key.clearPrivateKey(); // convert into watch-only key
        return key;
    }

    public int getAddressHandle() {
        return addressHandle;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getAddress() {
        return address;
    }
}
