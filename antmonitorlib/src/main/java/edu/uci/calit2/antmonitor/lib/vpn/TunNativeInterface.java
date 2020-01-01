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

import java.nio.ByteBuffer;

/**
 * This class provide a way to poll the TUN interface and read packets from it.
 * - This implementation is required to support polling read from TUN interface on Android
 * versions below 5.0.
 * - Note that Android 5.0+ already supports polling with Java API, however the performance is not
 * good enough due to polling and reading are done in two separate calls to native code.
 *
 * Created by Anh Le
 */
class TunNativeInterface {

    // Load native library needed here for tun read/write
    // We also load library in AhoCorasickInterface, but this is to avoid
    // possible crashes as discussed here:
    // http://stackoverflow.com/questions/10645249/intermittent-android-ndk-unsatisfiedlinkerror
    static {
        System.loadLibrary("antMonitorNative");
    }

    // Native file descriptor (int) of the TUN interface
    private static int mTunFD;

    // Singleton
    private static TunNativeInterface singleton = new TunNativeInterface();

    // Private constructor making this a singleton
    private TunNativeInterface() {
    }


    /**
     * Get the singleton instance
     *
     * @param tunFD The native file descriptor (int) of the TUN interface
     * @return the singleton of TunPollReaderJni
     */
    public static TunNativeInterface getTunPollJni(int tunFD) {
        mTunFD = tunFD;
        return singleton;
    }


    /**
     * Poll and read the TUN interface. The poll and read is carried out in native code.
     * If there is data within timeout, then return the number of bytes read, the bytes read are
     * also stored in mBuffer. The caller can access the bytes read by calling getBuffer().
     * If there is no data within timeout, return 0.
     *
     * @param timeout The timeout duration for the poll in ms.
     * @param buffer The buffer to read the data to
     * @param bufferHeaderSize The size of the header (already pre-filled) in the buffer
     * @return The number of bytes read.
     */
    public int pollRead(long timeout, ByteBuffer buffer, int bufferHeaderSize) {

        int bytes = 0;
        bytes = pollRead(mTunFD, timeout, buffer, bufferHeaderSize);

        if (bytes == -1) {
            // Return -1 to indicate broken channel.
            return -1;
        } else if (bytes == 0) {
            return 0;
        } else { // bytes > 0
            // Prepare buffer for subsequent reading.
            buffer.position(0);
            buffer.limit(bytes);
            return bytes;
        }
    }

    /**
     * Write to the TUN interface
     * @param buffer the packet to write to TUN
     * @return number of bytes written
     */
    public int write(ByteBuffer buffer, int messageLength) {
        return write(mTunFD, buffer, messageLength);
    }

    /**
     * Native implementation:
     * Pass the file descriptor (int) of the TUN in addition to other parameters
     */
    private native int pollRead(int tunFD, long timeout, ByteBuffer buffer, int bufferHeaderSize);

    /**
     * Native implementation:
     * Pass the file descriptor (int) of the TUN in addition to other parameters
     */
    private native int write(int tunFD, ByteBuffer buffer, int messageLength);
}
