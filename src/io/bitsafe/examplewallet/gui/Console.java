/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.gui;

/**
 *
 * @author Chris Chua
 */
public interface Console {
    public abstract void log(String text);
    public abstract void log(Exception e);
}
