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

/**
 * Used as the value in the hash map of ConnectionFinder. Stores app name and version number.
 * @author Anastasia Shuba
 */
public class ConnectionValue {

    /** A list of possible errors that can occur during mapping of a packet to an app */
    public interface MappingErrors {
        /** Prefix: all errors begin with this prefix */
        String PREFIX = "No mapping: ";

        /** This error is raised when the 4-tuple (source and destination ip:port) was not
         * found in the proc files */
        String NOT_FOUND = PREFIX + "entry not found";

        /** This error is raised when the packet is malformed and the mapper could not
         * retrieve the 4-tuple (source and destination ip:port) */
        String MALFORMED_PACKET = PREFIX + "malformed packet";

        /** This error is raised when the source or destination ip:port could not be resolved */
        String INVALID_TUPLE = PREFIX + "could not resolve source or dest ip:port";

        /** This error is raised when the packet is not a TCP or UDP packet. The protocol
         * number that was received is typically appended at the end of this string. */
        String INVALID_PROTOCOL = PREFIX + "non tcp/udp packet. Protocol: ";

        /** {@link ConnectionValue} corresponding to {@link #NOT_FOUND} */
        ConnectionValue CV_NOT_FOUND = new ConnectionValue(NOT_FOUND, null);

        /** {@link ConnectionValue} corresponding to {@link #MALFORMED_PACKET} */
        ConnectionValue CV_MALFORMED_PACKET = new ConnectionValue(MALFORMED_PACKET, null);

        /** {@link ConnectionValue} corresponding to {@link #INVALID_TUPLE} */
        ConnectionValue CV_INVALID_TUPLE = new ConnectionValue(INVALID_TUPLE, null);
    }

    /** Timer used for cleaning the hash map. If the connection value is old,
     * {@link ConnectionRemover} will remove it. */
    int timer;

    /** Package name of the app */
    private String appName;

    /** Version number of the app */
    private String versionNumber;

    /*
        Currently unused

        Mapping is from OSMonitor's jni/include/core/connectionInfo.pb.h
        public static final String[] STATUSES = {null, "ESTABLISHED", "SYN_SENT", "SYN_RECV",
            "FIN_WAIT1", "FIN_WAIT2", "TIME_WAIT",
            "CLOSE", "CLOSE_WAIT", "LAST_ACK",
            "LISTEN", "CLOSING"};
    */

    public ConnectionValue(String appName, String versionNumber) {
        this.appName = appName;
        this.versionNumber = versionNumber;
        timer = 0;
    }

    public String getAppName() { return appName; }
    public String getVersionNum() { return versionNumber; }

    /** Resets timer to zero so that
     * {@link ConnectionRemover} will keep this value
     * in the map a little longer */
    public void resetTimer() { timer = 0; }

    @Override
    public String toString() {
        return appName + ", " + versionNumber + ", " + timer;
    }
}
