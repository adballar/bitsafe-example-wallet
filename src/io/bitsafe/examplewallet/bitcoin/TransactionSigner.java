/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.bitcoin;

import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;

/**
 *
 * @author Chris Chua
 */
public interface TransactionSigner {
    /**
     * @param t Transaction to sign
     * @param w BitcoinJ wallet object, for convenience
     * @return true on success, false if anything went wrong (this will discard
     *         the transaction)
     */
    public abstract boolean signTransaction(Transaction t, Wallet w);
}
