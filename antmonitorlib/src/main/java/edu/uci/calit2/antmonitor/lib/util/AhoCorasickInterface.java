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
package edu.uci.calit2.antmonitor.lib.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * An interface for accessing the C library implementing the Aho-Corasick algorithm for DPI
 * NOTE: currently disabled
 *
 * @author Anastasia Shuba
 */
public class AhoCorasickInterface {

    // Load native library needed here for DPI
    // We also load library in TunNativeInterface, but this is to avoid
    // possible crashes as discussed here:
    // http://stackoverflow.com/questions/10645249/intermittent-android-ndk-unsatisfiedlinkerror
    static {
        System.loadLibrary("antMonitorNative");
    }

    // Singleton
    private static AhoCorasickInterface singleton = new AhoCorasickInterface();

    // Private constructor making this a singleton
    private AhoCorasickInterface() {}


    /**
     * Get the singleton instance
     * @return the singleton of AhoCorasickInterface
     */
    public static AhoCorasickInterface getInstance() {
        return singleton;
    }

    /**
     * Initializes the library: builds the tree used in searching
     * @param searchStrings the strings to search for
     */
    public synchronized native boolean init(String[] searchStrings);

    /**
     * Perform DPI on the given packet, searching for strings that were passed in
     * previously in the init method
     * NOTE: Currently this method returns an empty list
     *
     * @param packet a {@link ByteBuffer} containing the packet
     * @param size size of the packet
     * @return a list of strings found. Each string is followed by the ending position
     * of where it was found in the packet.
     */
    public synchronized ArrayList<String> search(ByteBuffer packet, int size) {
        ArrayList<String> originalList = searchNative(packet, size);
        if (originalList == null)
            return null;

        /* Note: originalList will change when another function calls 'search' since it's tied to
        native C. So, we must copy the list, so that the current caller maintains the original
        list, unmodified by future calls to C code. */
        ArrayList<String> copyList = new ArrayList<>(originalList);
        return copyList;
    }


    /**
     * Native C implementation of {@link #search(ByteBuffer, int)}
     * @param packet a {@link ByteBuffer} containing the packet
     * @param size size of the packet
     * @return a list of strings found. Each string is followed by the ending position
     * of where it was found in the packet.
     */
    private synchronized native ArrayList<String> searchNative(ByteBuffer packet, int size);
}
