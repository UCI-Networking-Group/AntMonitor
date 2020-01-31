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

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Removes old connections from the map filled out by {@link ConnectionFinder}
 * @author Anastasia Shuba
 */
class ConnectionRemover {
	public static final int R_COUNT = 5;
	public static final int PERIOD = 60; //60 sec
	
	private final ConnectionFinder finder;
	ScheduledThreadPoolExecutor exec;

	private String TAG = ConnectionRemover.class.getSimpleName();

	private class statusChecker implements Runnable {
		@Override
		public void run() {
			checkStatuses();
		}
	}

	public ConnectionRemover (ConnectionFinder finder) {
		this.finder = finder;
		exec = new ScheduledThreadPoolExecutor(1);
		exec.scheduleWithFixedDelay(new statusChecker(), 0, PERIOD, TimeUnit.SECONDS);
	}

    private void checkStatuses() {
        Set<Entry<Integer, ConnectionValue>> set = finder.getSet();
        ArrayList<Integer> keysToRemove = new ArrayList<>();

        for (Entry<Integer, ConnectionValue> e : set) {
            ConnectionValue v = e.getValue();

            v.timer++;
            // If connection was inactive for too long, remove it from the map
            if (v.timer >= R_COUNT)
                keysToRemove.add(e.getKey());
        }

        for (Integer k : keysToRemove) {
            //Log.d(TAG, "Removing " + k);
            finder.removeConnection(k);
        }
    }

	/**
	 * Stops the thread that cleans the hash map
	 */
	public void shutdown() {
		exec.shutdown();
	}
	
}
