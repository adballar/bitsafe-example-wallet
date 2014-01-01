/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.dev;

import java.io.IOException;

/** A USB HID report is malformed. This is indicative of a communications
 * error. Alternatively, perhaps the USB device we're talking to doesn't
 * speak using the wire protocol implemented in PacketInterface.
 *
 * @author Chris Chua
 */
class ReportFormatException extends IOException {
    public ReportFormatException(String why) {
        super(why);
    }
}
