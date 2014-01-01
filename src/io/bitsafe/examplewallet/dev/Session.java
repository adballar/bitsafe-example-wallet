/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.dev;

import com.google.protobuf.ByteString;
import io.bitsafe.examplewallet.dev.Messages.ButtonAck;
import io.bitsafe.examplewallet.dev.Messages.DeleteWallet;
import io.bitsafe.examplewallet.dev.Messages.Failure;
import io.bitsafe.examplewallet.dev.Messages.Features;
import io.bitsafe.examplewallet.dev.Messages.Initialize;
import io.bitsafe.examplewallet.dev.Messages.ListWallets;
import io.bitsafe.examplewallet.dev.Messages.LoadWallet;
import io.bitsafe.examplewallet.dev.Messages.NewWallet;
import io.bitsafe.examplewallet.dev.Messages.OtpAck;
import io.bitsafe.examplewallet.dev.Messages.OtpCancel;
import io.bitsafe.examplewallet.dev.Messages.PinAck;
import io.bitsafe.examplewallet.dev.Messages.PinCancel;
import io.bitsafe.examplewallet.dev.Messages.Ping;
import io.bitsafe.examplewallet.dev.Messages.PingResponse;
import io.bitsafe.examplewallet.dev.Messages.WalletInfo;
import io.bitsafe.examplewallet.dev.Messages.Wallets;
import io.bitsafe.examplewallet.gui.Console;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 *
 * @author Chris Chua
 */
public class Session {

    private final PacketWrapper bitsafe;
    // TODO: decouple from GUI by not using console
    private final Console console;

    public Session(BitSafeDevice newDev, Console inConsole) {
        bitsafe = new PacketWrapper(newDev);
        console = inConsole;
    }

    public void initialize(byte[] sessionId) {
        // TODO: Ensure that initialize is called (and is successful) before
        // allowing anything else.
        Initialize.Builder initialize = Initialize.newBuilder();
        initialize.setSessionId(ByteString.copyFrom(sessionId));
        Packet p = new Packet(Packet.PACKET_TYPE_INITIALIZE, initialize.build().toByteArray());
        try {
            bitsafe.sendPacket(p);
            p = bitsafe.receivePacket();
            if (p.getType() == Packet.PACKET_TYPE_FEATURES) {
                Features features = Features.parseFrom(p.getStorage());
                console.log("Features:\n");
                console.log(features.toString());
            } else {
                throw new UnexpectedResponseException(p.getType(), "Initialize");
            }
        } catch (IOException | UnexpectedResponseException e) {
            console.log(e);
        }
    }

    public void initialize() {
        initialize(Double.toString(Math.random()).getBytes());
    }

    public void ping(String greeting) {
        Ping.Builder ping = Ping.newBuilder();
        ping.setGreeting(greeting);
        Packet p = new Packet(Packet.PACKET_TYPE_PING, ping.build().toByteArray());
        try {
            bitsafe.sendPacket(p);
            p = bitsafe.receivePacket();
            if (p.getType() == Packet.PACKET_TYPE_PING_RESPONSE) {
                PingResponse pingResponse = PingResponse.parseFrom(p.getStorage());
                console.log("PingResponse:\n");
                console.log(pingResponse.toString());
            } else {
                throw new UnexpectedResponseException(p.getType(), "Ping");
            }
        } catch (IOException | UnexpectedResponseException e) {
            console.log(e);
        }
    }

    // TODO: Maybe refactor all the wallets stuff into WalletManager?
    public void deleteWallet(int walletNumber, PasswordSupplier passwordSupplier) {
        DeleteWallet.Builder deleteWallet = DeleteWallet.newBuilder();
        deleteWallet.setWalletHandle(walletNumber);
        Packet p = new Packet(Packet.PACKET_TYPE_DELETE_WALLET, deleteWallet.build().toByteArray());
        try {
            bitsafe.sendPacket(p);
            while (true) {
                p = bitsafe.receivePacket();
                if (p.getType() == Packet.PACKET_TYPE_SUCCESS) {
                    // TODO: should return something to indicate success
                    console.log("Deleted wallet " + walletNumber + "\n");
                    return;
                } else if (p.getType() == Packet.PACKET_TYPE_FAILURE) {
                    throw new BitSafeFailureException(Failure.parseFrom(p.getStorage()));
                } else if (p.getType() == Packet.PACKET_TYPE_BUTTON_REQUEST) {
                    bitsafe.sendPacket(Packet.PACKET_TYPE_BUTTON_ACK, ButtonAck.newBuilder());
                } else if (p.getType() == Packet.PACKET_TYPE_OTP_REQUEST) {
                    // One-time password required.
                    byte[] password = passwordSupplier.getPassword();
                    if (password != null) {
                        OtpAck.Builder otpAck = OtpAck.newBuilder();
                        otpAck.setOtp(new String(password));
                        p = new Packet(Packet.PACKET_TYPE_OTP_ACK, otpAck.build().toByteArray());
                        bitsafe.sendPacket(p);
                    } else {
                        bitsafe.sendPacket(Packet.PACKET_TYPE_OTP_CANCEL, OtpCancel.newBuilder());
                    }
                } else {
                    throw new UnexpectedResponseException(p.getType(), "Ping");
                }
            }
        } catch (IOException | UnexpectedResponseException | BitSafeFailureException e) {
            console.log(e);
        }
    }

    public Wallet createWallet(int walletNumber, String walletName, boolean doEncrypt, String password) {
        NewWallet.Builder newWallet = NewWallet.newBuilder();
        newWallet.setWalletNumber(walletNumber);
        if (doEncrypt) {
            newWallet.setPassword(ByteString.copyFromUtf8(password));
        }
        if (walletName != null) {
            newWallet.setWalletName(ByteString.copyFromUtf8(walletName));
        }
        newWallet.setIsHidden(false);
        Packet p = new Packet(Packet.PACKET_TYPE_NEW_WALLET, newWallet.build().toByteArray());
        try {
            bitsafe.sendPacket(p);
            while (true) {
                p = bitsafe.receivePacket();
                if (p.getType() == Packet.PACKET_TYPE_SUCCESS) {
                    return new Wallet(bitsafe, console, walletNumber);
                } else if (p.getType() == Packet.PACKET_TYPE_FAILURE) {
                    throw new BitSafeFailureException(Failure.parseFrom(p.getStorage()));
                } else if (p.getType() == Packet.PACKET_TYPE_BUTTON_REQUEST) {
                    bitsafe.sendPacket(Packet.PACKET_TYPE_BUTTON_ACK, ButtonAck.newBuilder());
                } else {
                    throw new UnexpectedResponseException(p.getType(), "Ping");
                }
            }
        } catch (IOException | UnexpectedResponseException | BitSafeFailureException e) {
            console.log(e);
        }
        return null;
    }

    public void listWallets() {
        try {
            bitsafe.sendPacket(Packet.PACKET_TYPE_LIST_WALLETS, ListWallets.newBuilder());
            Packet p = bitsafe.receivePacket();
            if (p.getType() == Packet.PACKET_TYPE_WALLETS) {
                Wallets wallets = Wallets.parseFrom(p.getStorage());
                console.log("Wallets:\n");
                Iterator<WalletInfo> iterator = wallets.getWalletInfoList().iterator();
                while (iterator.hasNext()) {
                    WalletInfo walletInfo = iterator.next();
                    String walletName = new String(walletInfo.getWalletName().toByteArray(), Charset.forName("UTF-8"));
                    console.log(walletInfo.getWalletNumber() + ": " + walletName + "\n");
                }
            } else if (p.getType() == Packet.PACKET_TYPE_FAILURE) {
                throw new BitSafeFailureException(Failure.parseFrom(p.getStorage()));
            } else {
                throw new UnexpectedResponseException(p.getType(), "ListWallets");
            }
        } catch (IOException | BitSafeFailureException | UnexpectedResponseException e) {
            console.log(e);
        }
    }

    public Wallet loadWallet(int walletNumber, PasswordSupplier passwordSupplier) {
        LoadWallet.Builder loadWallet = LoadWallet.newBuilder();
        loadWallet.setWalletNumber(walletNumber);
        Packet p = new Packet(Packet.PACKET_TYPE_LOAD_WALLET, loadWallet.build().toByteArray());
        try {
            bitsafe.sendPacket(p);
            while (true) {
                p = bitsafe.receivePacket();
                if (p.getType() == Packet.PACKET_TYPE_SUCCESS) {
                    return new Wallet(bitsafe, console, walletNumber);
                } else if (p.getType() == Packet.PACKET_TYPE_FAILURE) {
                    throw new BitSafeFailureException(Failure.parseFrom(p.getStorage()));
                } else if (p.getType() == Packet.PACKET_TYPE_PIN_REQUEST) {
                    // Wallet requires a password.
                    byte[] password = passwordSupplier.getPassword();
                    if (password != null) {
                        PinAck.Builder pinAck = PinAck.newBuilder();
                        pinAck.setPassword(ByteString.copyFrom(password));
                        p = new Packet(Packet.PACKET_TYPE_PIN_ACK, pinAck.build().toByteArray());
                        bitsafe.sendPacket(p);
                    } else {
                        bitsafe.sendPacket(Packet.PACKET_TYPE_PIN_CANCEL, PinCancel.newBuilder());
                    }
                }  else {
                    throw new UnexpectedResponseException(p.getType(), "LoadWallet");
                }
            }
        } catch (IOException | BitSafeFailureException | UnexpectedResponseException e) {
            console.log(e);
        }
        return null;
    }
}
