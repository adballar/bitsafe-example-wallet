/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.dev;

/**
 *
 * @author Chris Chua
 */
public class UnexpectedResponseException extends Exception {
    public UnexpectedResponseException(String why) {
        super(why);
    }

    public UnexpectedResponseException(int type, String requestName) {
        super("Unexpected response " + type + " to " + requestName);
    }
}
