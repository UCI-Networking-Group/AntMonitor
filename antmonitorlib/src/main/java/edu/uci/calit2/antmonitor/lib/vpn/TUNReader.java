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

import android.util.Log;

import java.nio.ByteBuffer;

import edu.uci.calit2.antmonitor.lib.logging.PacketAnnotation;

/**
 * Responsible for reading data from TUN, processing it and queuing it to be written to the NET
 *
 * @author Anastasia Shuba
 */
class TUNReader implements Runnable {
    private static final int POLL_TIMEOUT = 1000; //in ms

    /** Stores data read from TUN */
    private ByteBuffer mReadTunBuffer =
            ByteBuffer.allocateDirect(ForwarderManager.SOCKET_BYTEBUFFER_WRITE_SIZE);

    /** Instance of the {@link edu.uci.calit2.antmonitor.lib.vpn.ForwarderManager} for
     * synchronization purposes */
    private final ForwarderManager manager;

    public TUNReader(ForwarderManager manager) {
        this.manager = manager;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
        while (!Thread.currentThread().isInterrupted()) {

            // First we read packet from TUN
            mReadTunBuffer.clear();
            // Sanity check
            if (ForwarderManager.mTunInterfaceJni == null) {
                continue;
            }

            int bytesRead =
                    ForwarderManager.mTunInterfaceJni.pollRead(POLL_TIMEOUT, mReadTunBuffer, 0);

            if (bytesRead < 0) {
                // Check if we are in the process of shutting down
                // See {@link ForwarderManager#shutdown()}
                synchronized (manager) {
                    // If we are shutting down, the TUN error was expected. Terminate this thread.
                    if (!manager.isRunning()) {
                        // This is a special condition where even though this thread was
                        // interrupted by ForwarderManager, the interrupt was not set
                        Log.i(TUNReader.class.getSimpleName(), "Stopping...");
                        break;
                    }
                }

                // Sometimes TUN read error occurs, but it's OK to keep reading (?)
                Log.w(TUNReader.class.getSimpleName(), " error reading from TUN");
                continue;
            }

            if (bytesRead == 0)
                continue;

            PacketAnnotation packetAnnot =
                    ForwarderManager.mOutFilter.acceptIPDatagram(mReadTunBuffer);
            if (!packetAnnot.isAllowed()) {
                // Discard any packet that doesn't match the filter implementation.
                continue;
            }

            byte[] packet = new byte[bytesRead];
            if (mReadTunBuffer.hasArray()) {
                System.arraycopy(mReadTunBuffer.array(), mReadTunBuffer.arrayOffset(), packet, 0,
                        bytesRead);
            } else {
                mReadTunBuffer.get(packet);
            }

            // Then we process the packet
            manager.processTUNReadData(packet, packetAnnot);
        }
        ForwarderManager.mActiveThreads.remove(Thread.currentThread().getId());
        Log.i(TUNReader.class.getSimpleName(), " finished.");
    }
}
