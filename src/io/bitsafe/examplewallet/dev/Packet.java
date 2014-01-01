/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.dev;

/** A serialized protobuf message. This is a byte array with an attached
 * message type. A message type is needed because protobuf messages do not
 * contain sufficient information to distinguish message types from each other.
 *
 * @author Chris Chua
 */
public class Packet {

    // All these types were copied from stream_comm.h.

    /** Request a response from the wallet. */
    public static final int PACKET_TYPE_PING                = 0x00;
    /** Create a new wallet. */
    public static final int PACKET_TYPE_NEW_WALLET          = 0x04;
    /** Create a new address in a wallet. */
    public static final int PACKET_TYPE_NEW_ADDRESS         = 0x05;
    /** Get number of addresses in a wallet. */
    public static final int PACKET_TYPE_GET_NUM_ADDRESSES   = 0x06;
    /** Get an address and its associated public key from a wallet. */
    public static final int PACKET_TYPE_GET_ADDRESS_PUBKEY  = 0x09;
    /** Sign a transaction. */
    public static final int PACKET_TYPE_SIGN_TRANSACTION    = 0x0A;
    /** Load (unlock) a wallet. */
    public static final int PACKET_TYPE_LOAD_WALLET         = 0x0B;
    /** Format storage area, erasing everything. */
    public static final int PACKET_TYPE_FORMAT              = 0x0D;
    /** Change encryption key of a wallet. */
    public static final int PACKET_TYPE_CHANGE_KEY          = 0x0E;
    /** Change name of a wallet. */
    public static final int PACKET_TYPE_CHANGE_NAME         = 0x0F;
    /** List all wallets. */
    public static final int PACKET_TYPE_LIST_WALLETS        = 0x10;
    /** Backup a wallet. */
    public static final int PACKET_TYPE_BACKUP_WALLET       = 0x11;
    /** Restore wallet from a backup. */
    public static final int PACKET_TYPE_RESTORE_WALLET      = 0x12;
    /** Get device UUID. */
    public static final int PACKET_TYPE_GET_DEVICE_UUID     = 0x13;
    /** Get bytes of entropy. */
    public static final int PACKET_TYPE_GET_ENTROPY         = 0x14;
    /** Get master public key. */
    public static final int PACKET_TYPE_GET_MASTER_KEY      = 0x15;
    /** Delete a wallet. */
    public static final int PACKET_TYPE_DELETE_WALLET       = 0x16;
    /** Initialise device's state. */
    public static final int PACKET_TYPE_INITIALIZE          = 0x17;
    /** An address from a wallet (response to #PACKET_TYPE_GET_ADDRESS_PUBKEY
      * or #PACKET_TYPE_NEW_ADDRESS). */
    public static final int PACKET_TYPE_ADDRESS_PUBKEY      = 0x30;
    /** Number of addresses in a wallet
      * (response to #PACKET_TYPE_GET_NUM_ADDRESSES). */
    public static final int PACKET_TYPE_NUM_ADDRESSES       = 0x31;
    /** Public information about all wallets
      * (response to #PACKET_TYPE_LIST_WALLETS). */
    public static final int PACKET_TYPE_WALLETS             = 0x32;
    /** Wallet's response to ping (see #PACKET_TYPE_PING). */
    public static final int PACKET_TYPE_PING_RESPONSE       = 0x33;
    /** Packet signifying successful completion of an operation. */
    public static final int PACKET_TYPE_SUCCESS             = 0x34;
    /** Packet signifying failure of an operation. */
    public static final int PACKET_TYPE_FAILURE             = 0x35;
    /** Device UUID (response to #PACKET_TYPE_GET_DEVICE_UUID). */
    public static final int PACKET_TYPE_DEVICE_UUID         = 0x36;
    /** Some bytes of entropy (response to #PACKET_TYPE_GET_ENTROPY). */
    public static final int PACKET_TYPE_ENTROPY             = 0x37;
    /** Master public key (response to #PACKET_TYPE_GET_MASTER_KEY). */
    public static final int PACKET_TYPE_MASTER_KEY          = 0x38;
    /** Signature (response to #PACKET_TYPE_SIGN_TRANSACTION). */
    public static final int PACKET_TYPE_SIGNATURE           = 0x39;
    /** Version information and list of features. */
    public static final int PACKET_TYPE_FEATURES            = 0x3a;
    /** Device wants to wait for button press (beginning of ButtonRequest
      * interjection). */
    public static final int PACKET_TYPE_BUTTON_REQUEST      = 0x50;
    /** Host will allow button press (response to #PACKET_TYPE_BUTTON_REQUEST). */
    public static final int PACKET_TYPE_BUTTON_ACK          = 0x51;
    /** Host will not allow button press (response
      * to #PACKET_TYPE_BUTTON_REQUEST). */
    public static final int PACKET_TYPE_BUTTON_CANCEL       = 0x52;
    /** Device wants host to send a password (beginning of PinRequest
      * interjection. */
    public static final int PACKET_TYPE_PIN_REQUEST         = 0x53;
    /** Host sends password (response to #PACKET_TYPE_PIN_REQUEST). */
    public static final int PACKET_TYPE_PIN_ACK             = 0x54;
    /** Host does not want to send password (response
      * to #PACKET_TYPE_PIN_REQUEST). */
    public static final int PACKET_TYPE_PIN_CANCEL          = 0x55;
    /** Device wants host to send a one-time password (beginning of OtpRequest
      * interjection. */
    public static final int PACKET_TYPE_OTP_REQUEST         = 0x56;
    /** Host sends one-time password (response to #PACKET_TYPE_OTP_REQUEST). */
    public static final int PACKET_TYPE_OTP_ACK             = 0x57;
    /** Host does not want to send one-time password (response
      * to #PACKET_TYPE_OTP_REQUEST). */
    public static final int PACKET_TYPE_OTP_CANCEL          = 0x58;

    private final int type;
    private final byte[] storage;

    public Packet(int newType, byte[] newStorage) {
        type = newType;
        storage = newStorage;
    }

    public int getType() {
        return type;
    }

    public byte[] getStorage() {
        return storage;
    }
}
