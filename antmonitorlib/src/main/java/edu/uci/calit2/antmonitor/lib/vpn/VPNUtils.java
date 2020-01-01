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

import android.os.Looper;

import java.net.SocketAddress;
import java.nio.channels.Channel;

import edu.uci.calit2.antmonitor.lib.util.IpDatagram;

/**
 * Hosts multiple helper classes used by {@link ForwarderManager}, {@link TCPForwarder}, etc.
 * @author Anastasia Shuba
 */
class VPNUtils {
    /* One byte of the 4 bytes that correspond to broadcast IP: -1 (255) */
    private static final byte BROADCAST_IP_BYTE = -1;

    /** Broadcast IP address (255.255.255.255) */
    static final byte[] BROADCAST_IP_ARRAY = {BROADCAST_IP_BYTE, BROADCAST_IP_BYTE,
                                              BROADCAST_IP_BYTE, BROADCAST_IP_BYTE};

    /** Broadcast IP address as an integer */
    static final int BROADCAST_IP_INT = IpDatagram.convertIPv4IPArrayToInt(BROADCAST_IP_ARRAY);

    /**
     * Store a Request to change the Interest Ops of a Selector
     * @author anh
     */
    public static class ChangeRequest {
        public Channel mChannel;
        public int mSelectionKey;
        public Object mAttachment;

        public ChangeRequest(Channel channel, int selectionKey, Object attachment) {
            mChannel = channel;
            mSelectionKey = selectionKey;
            mAttachment = attachment;
        }
    }

    /**
     * Store data to be sent to NET through a Channel by the SelectorNetIO Thread
     * @author anh
     */
    public static class DataWriteToNet {
        /* Needed by DatagramSocket as it is un-connected */
        SocketAddress mRemoteAddress;

        // Used by TCP after sending.
        long mAckNum;

        byte[] mData;
        int mDataOffset;
        int mDataLen;

        public DataWriteToNet(SocketAddress remoteAddress, long ackNum, byte[] data,
                              int dataOffset, int dataLen) {
            mRemoteAddress = remoteAddress;
            mAckNum = ackNum;
            mData = data;
            mDataOffset = dataOffset;
            mDataLen = dataLen;
        }
    }

    /**
     * Represent a connection end point consisting a tuple of IP and port.
     * @author anh
     */
    public static class Tuple {
        int mIp;
        public byte[] mIpArray;
        public int mPort;


        public Tuple(int ip, byte[] ipArray, int port) {
            mIp = ip;
            mIpArray = ipArray;
            mPort = port;
        }

        @Override
        public String toString() {
            String str = "" + mIp + ":" + mPort;
            return str;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Tuple)) {
                return false;
            }

            Tuple otherTuple = (Tuple) other;
            return (mIp == otherTuple.mIp && mPort == otherTuple.mPort);
        }
    }

    /**
     * TCP States that a TCPForwarder needs to keep track of
     * @author Anh
     */
    public enum TCPState {
        CLOSED, LISTEN, SYN_RECEIVED, SYN_SENT, ESTABLISHED, FIN_WAIT_1, FIN_WAIT_2, CLOSING, CLOSE_WAIT, LAST_ACK, TIME_WAIT
    }

    /**
     * Verifies if the calling thread is the main thread.
     * If not, a {@link RuntimeException} is thrown.
     * @param excMsg Message to include in the {@code RuntimeException} that is thrown if the
     *               calling thread is not the main thread.
     */
    public static void ensureRunningOnMainThread(String excMsg) {
        if(Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new RuntimeException(excMsg);
        }
    }
}
