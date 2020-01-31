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
package edu.uci.calit2.antmonitor.lib.vpn;

import android.content.Context;

import java.net.InetAddress;

import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor;


/**
 * A {@link PacketFilter} designed for inspecting outgoing packets. Contains several
 * convenience methods for watching open/closing TCP connections.
 *
 * @author Nivedhita Sathyamurthy, Anastasia Shuba
 */
public class OutPacketFilter extends PacketFilter {
    /**
     * Constructor that prepares this filter for mapping.
     * Children MUST call super() for proper function of the {@link PacketFilter}!
     *
     * @param context     used for accessing various Android services. Cannot be {@code null}.
     * @throws IllegalStateException if {@code context} is {@code null}.
     */
    public OutPacketFilter(Context context) {
        super(context, PacketProcessor.TrafficType.OUTGOING_PACKETS);
    }

    /**
     * Gets called when a TCP connection is closed.
     * @param srcIp source IP address
     * @param dstIp destination IP address
     * @param srcPort source port number
     * @param destPort destination port number
     */
    public void onTCPConnectionClosed(String srcIp, String dstIp, int srcPort, int destPort) {
        return;
    }

    /**
     * Gets called when a TCP connection is opened.
     * @param remoteIp IP of server being contacted
     * @param srcPort source port number
     * @param destPort destination port number
     */
    public void onTCPConnectionOpened(String remoteIp, int srcPort, int destPort) {
        return;
    }

    /**
     * Gets called when TLS interception fails for a particular domain/app combo.
     * @param domain the certificate Common Name of the server to which TLS interception was
     *               attempted.
     * @param packageName package name of the application trying to contact the given domain.
     */
    protected void onDomainAppPin(String domain, String packageName) { return; }

    /**
     * Walks through a list of open TCP connections and calls
     * {@link #onTCPConnectionOpened(String, int, int)} for each one of them.
     */
    public void triggerOpenTCPConnections() {
        InetAddress server = null;
        for (TCPForwarder fwd : ForwarderManager.mActiveTCPForwarderMap.values()) {
            server = fwd.mServerIP;
            if (server != null)
                onTCPConnectionOpened(server.toString().substring(1), fwd.mSrc.mPort, fwd.mDst.mPort);
        }
    }

}
