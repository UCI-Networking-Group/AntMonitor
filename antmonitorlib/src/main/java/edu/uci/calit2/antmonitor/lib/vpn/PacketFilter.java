/*
 *  This file is part of AntMonitor <https://athinagroup.eng.uci.edu/projects/antmonitor/>.
 *  Copyright (C) 2018 Anastasia Shuba and the UCI Networking Group
 *  <https://athinagroup.eng.uci.edu>, University of California, Irvine.
 *
 *  AntMonitor is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  AntMonitor is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with AntMonitor. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.uci.calit2.antmonitor.lib.vpn;

import android.content.Context;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import edu.uci.calit2.antmonitor.lib.logging.PacketAnnotation;
import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor;
import edu.uci.calit2.antmonitor.lib.logging.ConnectionValue;

import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor.TrafficType;
import edu.uci.calit2.antmonitor.lib.util.IpDatagram;
import edu.uci.calit2.antmonitor.lib.util.Protocol;
import edu.uci.calit2.antmonitor.lib.util.TCPReassemblyInfo;

/**
 * Parent class for {@link OutPacketFilter} and {@link IncPacketFilter}.
 *
 * @author Anastasia Shuba
 */
public abstract class PacketFilter {

    /** Indicates whether this {@link PacketFilter} filters outgoing or incoming packets */
    protected TrafficType mTrafficType;

    /** {@link Context} object used for accessing various Android services */
    protected Context mContext;

    /** Default annotation that allows packets through */
    protected final static PacketAnnotation DEFAULT_ANNOTATION = new PacketAnnotation();

    /**
     * Constructor that prepares this filter for mapping.
     * Children MUST call super() for proper function of the {@link PacketFilter}!
     * @param context used for accessing various Android services. Cannot be {@code null}.
     * @param trafficType indicates whether this {@link PacketFilter} will filter outgoing or
     *                    incoming packets. Cannot be {@code null} if you plan to call
     *                    {@link #mapDatagramToApp(ByteBuffer)}
     * @throws IllegalStateException if {@code context} is {@code null}.
     */
    public PacketFilter(Context context, TrafficType trafficType) {
        if (context == null)
            throw new IllegalStateException("Context cannot be null.");

        mContext = context;
        mTrafficType = trafficType;
    }

    /**
     * This method is called for all datagrams going in and out of the device. Since this method is
     * called on-line, implementations should not be too heavy so as not to slow-down network
     * throughput. The default implementation accepts all packets.
     * @param packet the IP datagram
     * @return {@code true} (within the {@link PacketAnnotation}) if the packet is allowed to be
     * sent, {@code false} otherwise.
     */
    public PacketAnnotation acceptIPDatagram(ByteBuffer packet) {
        return DEFAULT_ANNOTATION;
    }

    /**
     * This method is called for all packets that go through SSL bumping. Since this method is
     * called on-line, implementations should not be too heavy so as not to slow-down network
     * throughput. The default implementation accepts all packets.
     * In addition to the packet itself, other parameters
     * are passed in case they are needed for analysis since the decrypted packet is not an IP
     * datagram, but is a TCP packet, and thus does not contain this information.
     * @param packet the decrypted packet. WARNING: DO NOT STORE THIS PACKET AS IT CONTAINS
     *               SENSITIVE INFORMATION. USE IT FOR ANALYSIS ONLY.
     * @param tcpInfo contains info needed for mapping packets to apps and also for
     *                mapping this decrypted packet back to its encrypted TCP segment(s).
     * @return {@code true} (within the {@link PacketAnnotation})
     * if the packet is allowed to be sent, {@code false} otherwise.
     */
    public PacketAnnotation acceptDecryptedSSLPacket(ByteBuffer packet, TCPReassemblyInfo tcpInfo) {
        return DEFAULT_ANNOTATION;
    }

    /**
     * Maps the given datagram to an app. Call this function only when needed as it may
     * slow-down network throughput due to I/O operations required for mapping.
     * @param packet the IP datagram which to map
     * @return {@link ConnectionValue} containing the app name and version number, if available.
     *          If datagram could not be mapped, the app name instead contains one of the messages
     *          in {@link ConnectionValue.MappingErrors}
     */
    protected ConnectionValue mapDatagramToApp(ByteBuffer packet) {
        ConnectionValue cv = null;

        short protocolNumber = IpDatagram.readTransportProtocol(packet);

        // For now we can only map TCP/UDP packets (due to the implementation of ConnectionKey)
        if (protocolNumber ==  Protocol.UDP.getProtocolNumber() ||
                protocolNumber ==  Protocol.TCP.getProtocolNumber()) {

            int srcPort = -1;

            try {
                if (mTrafficType == TrafficType.OUTGOING_PACKETS)
                    srcPort = IpDatagram.readSourcePort(packet);
                else
                    srcPort = IpDatagram.readDestinationPort(packet);

                // Proceed to mapping
                cv = PacketProcessor.getInstance(mContext).getConnValue(srcPort);

                if (cv == null)
                    cv = ConnectionValue.MappingErrors.CV_NOT_FOUND;

            } catch (IndexOutOfBoundsException e) {
                // Write that we couldn't map packet due to malformed packet
                cv = ConnectionValue.MappingErrors.CV_MALFORMED_PACKET;
            }
        } else {
            // Write that we couldn't map packet due to its use of a transport protocol other than tcp/udp.
            cv = new ConnectionValue(ConnectionValue.MappingErrors.INVALID_PROTOCOL + protocolNumber, null);
        }

        return cv;
    }

    /**
     * Maps the given parameters to an app. Call this function only when needed as it may
     * slow-down network throughput due to I/O operations required for mapping.
     *
     * @param remoteIp the IP of the Internet host being contacted
     * @param srcPort port used by the app responsible for the connection
     * @param destPort port used by the Internet host
     * @return {@link ConnectionValue} containing the app name and version number, if available.
     *          If packet could not be mapped, the app name instead contains one of the messages
     *          in {@link ConnectionValue.MappingErrors}
     */
    protected ConnectionValue mapParamsToApp(String remoteIp, int srcPort, int destPort) {

        // Mark as outgoing so that src/dest IPs are not flipped since we already know what
        // is the remote IP and what is the app IP
        ConnectionValue cv = PacketProcessor.getInstance(mContext).getConnValue(srcPort);

        if (cv == null)
            cv = ConnectionValue.MappingErrors.CV_NOT_FOUND;

        return cv;
    }

    /** Called when VPN is starting */
    protected void onVPNStart() {}

    /** Called when the VPN connection is brought down */
    protected void onVPNStop() {}

}
