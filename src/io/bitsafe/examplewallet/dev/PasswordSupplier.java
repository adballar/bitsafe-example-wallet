/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.dev;

/** Supplies password on demand.
 *
 * @author Chris Chua
 */
public interface PasswordSupplier {
    /** Get a wallet password from somewhere (perhaps from a modal dialog box).
     * @return The wallet password as a byte array, or null if the user
     *         cancelled.
     */
    public abstract byte[] getPassword();
}
