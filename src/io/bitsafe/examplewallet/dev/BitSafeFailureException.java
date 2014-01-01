/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.dev;

import io.bitsafe.examplewallet.dev.Messages.Failure;

/** Wraps Failure message from the BitSafe.
 *
 * @author Chris Chua
 */
public class BitSafeFailureException extends Exception {
    public BitSafeFailureException(Failure failure) {
        super("Error " + failure.getErrorCode() + ": " + failure.getErrorMessage());
    }
}
