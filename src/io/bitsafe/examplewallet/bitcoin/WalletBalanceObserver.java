/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.bitcoin;

import java.math.BigInteger;

/**
 *
 * @author Chris Chua
 */
public interface WalletBalanceObserver {
    /** Will always be called when wallet balance changes. Will sometimes
     * be called spuriously when wallet balance doesn't change.
     * @param confirmed Confirmed balance in satoshi
     * @param unconfirmed Unconfirmed balance in satoshi
     */
    public abstract void balanceChanged(BigInteger confirmed, BigInteger unconfirmed);
}
