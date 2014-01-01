/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.gui;

/**
 *
 * @author Chris Chua
 */
public class PasswordMismatchException extends Exception {
    public PasswordMismatchException(String why) {
        super(why);
    }
}
