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
package edu.uci.calit2.antmonitor.lib.logging;

import android.content.Context;
import android.util.Log;

import java.util.UUID;

import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor.TrafficType;
import edu.uci.calit2.antmonitor.lib.util.PacketDumpInfo;
import edu.uci.calit2.antmonitor.lib.vpn.VpnController;

/**
 * This class receives packets from {@link PacketQueueReader} via the {@code consumePacket}
 * call. Children of this class can override {@code consumePacket} and process the packets as
 * they wish. Children of this class should be passed to the system during the
 * {@code connect} call of {@link VpnController} so that
 * {@link PacketQueueReader}s know to use them instead of the default {@link PacketConsumer}.
 *
 * @author Anastasia Shuba
 */
public class PacketConsumer {
    /** Indicates whether this {@link PacketConsumer} consumes outgoing or incoming packets */
    protected TrafficType mTrafficType;

    private Context mContext;

    /**
     * Constructor that prepares this consumer for both mapping and logging.
     * Children MUST call super() for proper function of the PacketConsumer!
     * @param context used for accessing various Android services. Cannot be {@code null}.
     * @param trafficType indicates whether this {@link PacketConsumer} will consume outgoing or
     *                    incoming packets. Cannot be {@code null} if you plan to call either
     *                    {@link #mapPacketToApp(PacketDumpInfo)} or
     *                    {@link #log(PacketDumpInfo, String)}
     * @param userID user id used for marking log files as belonging to a certain user.
     *               Usually, a {@link UUID}.
     *               Cannot be {@code null} if you plan to call
     *               {@link #log(PacketDumpInfo, String)}
     * @throws IllegalStateException if {@code context} is {@code null}.
     */
    public PacketConsumer(Context context, TrafficType trafficType, String  userID) {
        if (context == null)
            throw new IllegalStateException("Context cannot be null.");

        mContext = context;
        mTrafficType = trafficType;

        TrafficLogFiles.setUserID(userID);
    }

    /**
     * Called when {@link PacketQueueReader} has a packet available for processing
     * @param packetDumpInfo the packet (and its associated metadata)
     */
    protected void consumePacket(PacketDumpInfo packetDumpInfo) {
        // do nothing
    }

    /**
     * Gets called when the VPN connection is being torn down. Use this method to perform any
     * clean-up needed with any logged files and etc.
     */
    protected void onStop() {
        // do nothing
    }

    /**
     * Maps the given packet to an app
     * @param packet the packet (and its associated metadata) which to map
     * @return {@link ConnectionValue} containing the app name and version number, if available.
     *          If datagram could not be mapped, the app name instead contains one of the messages
     *          in {@link ConnectionValue.MappingErrors}
     * @throws IllegalStateException if this {@link PacketConsumer} was created without a
     * {@link TrafficType}
     */
    protected ConnectionValue mapPacketToApp(PacketDumpInfo packet) throws IllegalStateException {
        if (mTrafficType == null)
            throw new IllegalStateException("Cannot map packets when traffic type was not set.");
        return PacketProcessor.getInstance(mContext).mapPacketToApp(packet, mTrafficType);
    }

    /**
     * Convenience method for logging full ICMP, UDP, and HTTP(S) packets.
     * @param packet the original packet containing headers and data
     * @param protocol the protocol
     * @param destinationPort port number of the server that is being contacted or is responding -
     *                        used to determine whether the packet is HTTP/HTTPS (port 80/443)
     *                        or not.
     * @return the length of the entire packet if the packet is ICMP, UDP, or HTTP(S).
     * Otherwise the length of the headers is returned.
     * @throws IllegalArgumentException if the protocol is unknown
     */
    protected int getFullPacketSize(byte[] packet, short protocol, int destinationPort) {
        return PacketProcessor.getInstance(mContext).getFullPacketSize(packet, protocol, destinationPort);
    }

    /**
     * Returns the length of the header(s). If protocol is ICMP, full packet size is returned.
     * @param packet the original packet containing headers and data
     * @param protocol the protocol
     * @return the the length of the header(s). If protocol is ICMP, full packet size is returned.
     * @throws IllegalArgumentException if the protocol is unknown */
    protected int getHeadersSize(byte[] packet, short protocol) {
        return PacketProcessor.getInstance(mContext).getHeadersSize(packet, protocol);
    }

    /**
     * Writes the packet to a pcapng file on disk.
     * @param packet The packet (and its associated metadata) that is to be written to file.
     * @param comment a {@link String} containing metadata/comments such as the name of the app
     *                responsible for the packet.
     * @throws IllegalStateException if this {@link PacketConsumer} was created without a
     * {@link TrafficType} or without a user id.
     */
    protected void log(PacketDumpInfo packet, String comment) {
        if (mTrafficType == null)
            throw new IllegalStateException("Cannot log packets when traffic type was not set.");

        if (TrafficLogFiles.getUserID() == null)
            throw new IllegalStateException("Cannot log packets when no user id was specified.");

        PacketProcessor.getInstance(mContext).log(packet, comment, mTrafficType);
    }
}
