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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import edu.uci.calit2.antmonitor.lib.util.PacketDumpInfo;

/**
 * This class simply controls access to shared packet dumps using a
 * {@link java.util.concurrent.ArrayBlockingQueue} to synchronize access.
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public class PacketLogQueue {
    private ArrayBlockingQueue<PacketDumpInfo> queue = new ArrayBlockingQueue<PacketDumpInfo>(35000);

    private final long timeout = 1000l;

    /**
     * Simple inserts the supplied dump into the buffer. Blocks if queue is full.
     * @param dump A {@link PacketDumpInfo} containing the information to be inserted into the buffer.
     */
    public void put(PacketDumpInfo dump) {
        try {
            queue.put(dump);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Queries the {@link java.util.concurrent.ArrayBlockingQueue} for a packet dump.
     * This is a blocking call which returns when a packet dump is available from the buffer or the {@code timeout} is exceeded
     * @return a {@link PacketDumpInfo} from the buffer or null if {@code timeout} is exceeded.
     * @throws java.lang.InterruptedException if interrupted while waiting.
     */
    public PacketDumpInfo get() throws InterruptedException {
        return queue.poll(timeout, TimeUnit.MILLISECONDS);
    }

    public int getSize(){
        return queue.size();
    }

    public void clear() { queue.clear(); }
}
