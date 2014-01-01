/* This file is licensed as described by the file LICENCE. */

package io.bitsafe.examplewallet.dev;

import com.google.protobuf.AbstractMessage.Builder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.spongycastle.util.Arrays;

/** Wraps BitSafeDevice object to expose a higher level Packet based interface.
 *
 * @author Chris Chua
 */
public class PacketWrapper {
    /** Maximum size of a single USB HID report. */
    public static final int MAX_REPORT_SIZE = 63;

    private final BitSafeDevice dev;

    public PacketWrapper(BitSafeDevice newDev) {
        dev = newDev;
    }

    public void sendPacket(Packet p) throws IOException {
        // Prepend header.
        int messageLength = p.getStorage().length;
        int totalLength = messageLength + 8;
        byte[] buf = new byte[totalLength];
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN);
        bb.put((byte)'#');
        bb.put((byte)'#');
        bb.putShort((short)p.getType());
        bb.putInt(messageLength);
        bb.put(p.getStorage());

        // Divide into HID reports and send them.
        int reportLength;
        int index = 0;
        while (totalLength > 0) {
            if (totalLength > MAX_REPORT_SIZE) {
                reportLength = MAX_REPORT_SIZE;
            } else {
                reportLength = totalLength;
            }
            byte[] report = new byte[reportLength + 1];
            report[0] = (byte)reportLength;
            System.arraycopy(buf, index, report, 1, reportLength);
            dev.sendReport(report);
            totalLength -= reportLength;
            index += reportLength;
        }
    }

    /** Wrapper around sendPacket() which sends empty protocol buffer messages.
     * @param messageType One of the PACKET_TYPE_x constants from Packet
     * @param messageBuilder Builder for the protocol buffer message
     * @throws IOException 
     */
    public void sendPacket(int messageType, Builder messageBuilder) throws IOException {
        Packet p = new Packet(messageType, messageBuilder.build().toByteArray());
        sendPacket(p);
    }

    /** Check that a USB HID report has the expected format (one report ID byte
     * followed by report contents), and add the contents to a ByteBuffer.
     * @param report USB HID report to check
     * @param bb ByteBuffer to add contents to. The contents of a report are
     *           all bytes of the report except for the first byte.
     * @throws ReportFormatException 
     */
    private void checkAndAddReport(byte[] report, ByteBuffer bb) throws ReportFormatException {
        if (report.length < 1) {
            throw new ReportFormatException("Report length is 0");
        } else {
            int reportID = report[0];
            if ((reportID < 0) || (reportID > MAX_REPORT_SIZE)) {
                throw new ReportFormatException("Invalid report ID byte");
            }
            // Skip this check because on Windows systems, the USB HID
            // driver always returns maximum-sized reports regardless of the
            // true size of a report.
            //if (reportID != (report.length - 1)) {
            //    throw new ReportFormatException("Report length doesn't match report ID");
            //}
            // Need to strip off report ID byte.
            bb.put(report, 1, reportID);
        }
    }

    public Packet receivePacket() throws IOException, ReportFormatException {
        // Need to read the header to know the length of the packet.
        // Try to read the first 8 bytes.
        // Allocating buf with a size of MAX_REPORT_SIZE * 2 guarantees that
        // it won't overflow, since we abort after 8 bytes.
        byte[] headerBuf = new byte[MAX_REPORT_SIZE * 2];
        ByteBuffer header = ByteBuffer.wrap(headerBuf).order(ByteOrder.BIG_ENDIAN);
        while (header.position() < 8) {
            checkAndAddReport(dev.receiveReport(), header);
        }
        header.flip();

        // Read the header.
        for (int i = 0; i < 2; i++) {
            if (header.get() != '#') {
                throw new ReportFormatException("Header magic bytes not found");
            }
        }
        int packetType = header.getShort();
        int messageLength = header.getInt();
        int totalLength = messageLength + 8;

        // Now the rest of the packet can be read.
        byte[] buf = new byte[totalLength];
        ByteBuffer entirePacket = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN);
        entirePacket.put(headerBuf, 0, header.limit());
        while (entirePacket.position() < totalLength) {
            checkAndAddReport(dev.receiveReport(), entirePacket);
        }

        return new Packet(packetType, Arrays.copyOfRange(buf, 8, totalLength));
    }
}
