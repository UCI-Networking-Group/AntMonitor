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

import java.nio.ByteBuffer;

/**
 * Container class for keeping some of the info stored in TCP headers needed for re-assembling
 * TCP segments. See also {@link
 * edu.uci.calit2.antmonitor.lib.vpn.PacketFilter#acceptDecryptedSSLPacket(ByteBuffer,
 * TCPReassemblyInfo)}
 *
 * @author Anastasia Shuba
 */
public class TCPReassemblyInfo {
    private String remoteIp;
    private int srcPort;
    private int destPort;
    private long ackNum;
    private long sequenceNum;
    private long dataLen;


    /**
     *
     * @param remoteIp the IP of the Internet host sending or receiving the packet
     * @param srcPort port used by the app responsible for sending or receiving the packet
     * @param destPort port used by the Internet host sending or receiving the packet
     * @param ackNum the ack number of the first TCP segment responsible for sending data
     * @param sequenceNum the sequence number of the first TCP segment responsible for sending data
     */
    public TCPReassemblyInfo(String remoteIp, int srcPort, int destPort, long ackNum,
                             long sequenceNum) {
        this(remoteIp, srcPort, destPort, ackNum, sequenceNum, -1);
    }

    /**
     *
     * @param remoteIp the IP of the Internet host sending or receiving the packet
     * @param srcPort port used by the app responsible for sending or receiving the packet
     * @param destPort port used by the Internet host sending or receiving the packet
     * @param ackNum the ack number of the first TCP segment responsible for sending data
     * @param sequenceNum the sequence number of the first TCP segment responsible for sending data
     * @param dataLen the combined length of the data carried by the TCP segment(s) prior to
     *                decryption
     */
    public TCPReassemblyInfo(String remoteIp, int srcPort, int destPort, long ackNum,
                             long sequenceNum, long dataLen) {
        this.remoteIp = remoteIp;
        this.srcPort = srcPort;
        this.destPort = destPort;
        this.ackNum = ackNum;
        this.sequenceNum = sequenceNum;
        this.dataLen = dataLen;
    }

    /** Retrieves the IP of the Internet host sending or receiving the packet
     * @return the IP of the Internet host sending or receiving the packet */
    public String getRemoteIp() { return remoteIp; }

    /** Retrieves the port used by the app responsible for sending or receiving the packet
     * @return the port used by the app responsible for sending or receiving the packet */
    public int getSrcPort() { return srcPort; }

    /** Retrieves the port used by the Internet host sending or receiving the packet
     * @return the port used by the Internet host sending or receiving the packet */
    public int getDestPort() { return destPort; }

    /** Retrieves the ack number of the first TCP segment responsible for sending data
     * @return the ack number of the first TCP segment responsible for sending data */
    public long getAckNum() { return ackNum; }

    /** Retrieves the sequence number of the first TCP segment responsible for sending data
     * @return the sequence number of the first TCP segment responsible for sending data */
    public long getSequenceNum() { return sequenceNum; }

    /** Retrieves the combined length of the data carried by the TCP segment(s) prior to decryption.
     * Note that this value is only set for outgoing packets.
     * @return the combined length of the data carried by the TCP segment(s) prior to decryption */
    public long getDataLen() { return dataLen; }

    /**
     * Increments {@link #dataLen} by the given length
     * @param dataLen the length to increment by
     */
    public void addToDataLen(long dataLen) {
        this.dataLen += dataLen;
    }
}
