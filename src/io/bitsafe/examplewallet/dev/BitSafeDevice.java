/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.dev;

import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDManager;
import io.bitsafe.examplewallet.gui.Console;
import java.io.IOException;
import java.util.Arrays;

/** Lowest level (USB HID) access to BitSafe device.
 *
 * @author Chris Chua
 */
public class BitSafeDevice {
    /** USB vendor ID of target device. */
    private static final int TARGET_VID	= 0x04f3;
    /** USB product ID of target device. */
    private static final int TARGET_PID = 0x0210;
    /** Maximum USB packet size for a USB full-speed Interrupt pipe. */
    public static final int MAX_PACKET_SIZE = 64;

    private final HIDManager manager;
    private final Console console;
    private HIDDevice device;

    public BitSafeDevice(Console inConsole) throws IOException {
        ClassPathLibraryLoader.loadNativeHIDLibrary();
        manager = HIDManager.getInstance();
        console = inConsole;
    }

    public void connect() {
        try {
            if (device == null) {
                device = manager.openById(TARGET_VID, TARGET_PID, null);
                console.log("Opened device\n");
                console.log("Product: " + device.getProductString() + "\n");
                console.log("Manufacturer: " + device.getManufacturerString() + "\n");
                console.log("Serial no.: " + device.getSerialNumberString() + "\n");
            }
        } catch (IOException e) {
            console.log(e);
            console.log("Please check that this program is running as root\n");
        } catch (NullPointerException e) {
            // HIDManager.openById() throws NullPointerException when there
            // are no devices.
            console.log("No USB HID devices detected\n");
        }
        
    }

    public void disconnect() {
        // TODO: always call manager.release() on exit
        try {
            if (device != null) {
                device.close();
                console.log("Closed device\n");
                device = null;
            }
        } catch (IOException e) {
            console.log(e);
        }
    }

    // Don't catch IOException so that upper layers can abort sending a
    // sequence of reports.
    public void sendReport(byte[] report) throws IOException {
        device.write(report);
    }

    // Don't catch IOException so that upper layers can abort receiving a
    // sequence of reports.
    public byte[] receiveReport() throws IOException {
        // At this point, the report size is unknown, so allocate a buffer
        // that can store all possible reports.
        byte[] buf = new byte[MAX_PACKET_SIZE];
        int bytesRead = device.read(buf);
        // Resize buffer to correct length.
        return Arrays.copyOf(buf, bytesRead);
    }
}
