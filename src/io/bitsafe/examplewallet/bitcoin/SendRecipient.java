/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.bitcoin;

import java.math.BigInteger;

/**
 *
 * @author Chris Chua
 */
public class SendRecipient {
    private final BigInteger amount;
    private final String address;

    public SendRecipient(BigInteger newAmount, String newAddress) {
        amount = newAmount;
        address = newAddress;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public String getAddress() {
        return address;
    }
}
