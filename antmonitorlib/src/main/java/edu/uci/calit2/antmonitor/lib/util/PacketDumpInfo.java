/*
 *  This file is part of AntMonitor <https://athinagroup.eng.uci.edu/projects/antmonitor/>.
 *  Copyright (C) 2018 Anastasia Shuba and the UCI Networking Group
 *  <https://athinagroup.eng.uci.edu>, University of California, Irvine.
 *
 *  AntMonitor is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2 of the License.
 *
 *  AntMonitor is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with AntMonitor. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.uci.calit2.antmonitor.lib.util;

import edu.uci.calit2.antmonitor.lib.logging.PacketAnnotation;

/**
 * This class represents a partial or full copy of a network packet.
 * It contains a byte array for storing a binary dump of a network packet along with a timestamp and the original length of the message.
 * The original length of a packet is useful to determine whether or not the dump of the datagram is partial or not.
 * In addition, it contains the {@link PacketAnnotation} that was created within a
 * {@link edu.uci.calit2.antmonitor.lib.vpn.PacketFilter} when the corresponding packet was
 * processed.
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public class PacketDumpInfo {
    private final byte[] dump;
    private final long timestamp;
    private final int originalLength;
    private int captureLength;

    /** The annotation returned by a {@link edu.uci.calit2.antmonitor.lib.vpn.PacketFilter} when
     * the corresponding packet ({@link #dump}) was allowed through by the filter. */
    private final PacketAnnotation packetAnnotation;

    /**
     * @param dump A packet in binary format
     * @param packetAnnotation the annotation returned by a
     * {@link edu.uci.calit2.antmonitor.lib.vpn.PacketFilter} when the packet dump was allowed
     *                         through by the filter.
     */
    public PacketDumpInfo(byte[] dump, PacketAnnotation packetAnnotation) {
        this.dump = dump;
        // TODO: perhaps this should be done off-line
        this.originalLength = IpDatagram.readDatagramLength(dump);
        this.timestamp = System.currentTimeMillis();
        captureLength = dump.length;
        this.packetAnnotation = packetAnnotation;
    }

    public byte[] getDump() {
        return dump;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getOriginalLength() { return originalLength; }

    public void setCaptureLength(int captureLength) {this.captureLength = captureLength; }

    public int getCaptureLength() { return captureLength; }

    public PacketAnnotation getPacketAnnotation() { return packetAnnotation; }
}