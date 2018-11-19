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
package edu.uci.calit2.antmonitor.lib.logging;

import android.os.Process;
import android.util.Log;

import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor.TrafficType;
import edu.uci.calit2.antmonitor.lib.util.PacketDumpInfo;


/**
 * Runnable that continuously runs and queries the
 * {@link PacketLogQueue} for available packet dumps.
 * Each dump is processed by the {@link PacketConsumer}.
 *
 * @author Anastasia Shuba
 */
public class PacketQueueReader implements Runnable {

    private PacketLogQueue mPacketQueue;

    private final String TAG;

    private PacketConsumer mConsumer;

    public PacketQueueReader(PacketLogQueue packetQueue, PacketConsumer consumer) {
        this.mPacketQueue = packetQueue;
        this.mConsumer = consumer;
        TAG = getClass().getName() + "-" + consumer.mTrafficType;
    }

    @Override
    public void run() {

        // Upload speed is less, so queue will not get full fast
        if (mConsumer.mTrafficType.equals(TrafficType.OUTGOING_PACKETS))
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        // The PacketDumper runs until it is interrupted.
        while(!Thread.interrupted()) {
            try {
                // Poll the queue (blocking call)
                PacketDumpInfo dump = mPacketQueue.get();
                if(dump != null){
                    mConsumer.consumePacket(dump);
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        stop();
        Log.d(TAG, "Terminated.");
    }

    private void stop() {
        mPacketQueue.clear();
        mConsumer.onStop();
    }
}
