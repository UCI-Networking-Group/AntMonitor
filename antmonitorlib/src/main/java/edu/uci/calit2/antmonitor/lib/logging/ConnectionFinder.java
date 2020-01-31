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

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.MalformedInputException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps a mapping of open TCP and UDP connections to apps responsible for them, using the
 * source port as a key for identifying a connection.
 *
 * @author Anastasia Shuba
 */
class ConnectionFinder {
    private String TAG = ConnectionFinder.class.getSimpleName();

    /** Maintains a list of open connections, keyed by client/source port */
    private final HashMap<Integer, ConnectionValue> map;

    /** Used to retrieve app names from UIDs */
    private final PackageManager pm;

    /** Keep track of our own UID to avoid saving our own connections in the {@link #map} */
    private final int myUID = android.os.Process.myUid();

    /** Used to periodically remove old connections from {@link #map} */
    private ConnectionRemover connectionRemover;

    /** Regex pattern used to match against IPv4 proc file contents */
    private final Pattern ipv4Pattern;

    /** Regex pattern used to match against IPv6 proc file contents */
    private final Pattern ipv6Pattern;

    /**
     * Constructor
     *
     * @param context
     *            - used to getSystemService when retrieving app names
     */
    public ConnectionFinder(Context context) {
        pm = context.getPackageManager();
        map = new HashMap<>();

        // Prepare regex
        String ipv4Str = "\\s+\\d+:\\s([0-9A-F]{8}):([0-9A-F]{4})\\s([0-9A-F]{8}):" +
                "([0-9A-F]{4})\\s([0-9A-F]{2})\\s[0-9A-F]{8}:[0-9A-F]{8}\\s[0-9A-F]{2}:" +
                "[0-9A-F]{8}\\s[0-9A-F]{8}\\s+([0-9A-F]+)";
        ipv4Pattern = Pattern.compile(ipv4Str,
                Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES | Pattern.DOTALL);

        String ipv6Str = "\\s+\\d+:\\s([0-9A-F]{32}):([0-9A-F]{4})\\s([0-9A-F]{32}):" +
                "([0-9A-F]{4})\\s([0-9A-F]{2})\\s[0-9A-F]{8}:[0-9A-F]{8}\\s[0-9A-F]{2}:" +
                "[0-9A-F]{8}\\s[0-9A-F]{8}\\s+([0-9A-F]+)";
        ipv6Pattern = Pattern.compile(ipv6Str,
                Pattern.CASE_INSENSITIVE | Pattern.UNIX_LINES | Pattern.DOTALL);

        // Periodically clean the hash map
        connectionRemover = new ConnectionRemover(this);
    }

    /**
     * Put connection value for a given connection key
     *
     * @param key
     * @param overWrite {@code true} if old ConnectionValue should be replaced by
     * @param value
     */
    public void putConnection(int key, ConnectionValue value, boolean overWrite) {
        synchronized(map) {
            if (!overWrite && map.get(key) != null)
                return;
            map.put(key, value);
        }
    }

    /**
     * Get connection values for a given connection key
     *
     * @param key
     * @return ConnectionValue that belongs to the key
     */
    public ConnectionValue getConnection(int key) {
        ConnectionValue v = null;
        synchronized(map) {
            v = map.get(key);
        }
        return v;
    }

    /**
     * @return the entire set of (key, value) pairs stored in the hash map
     */
    public Set<Entry<Integer, ConnectionValue>> getSet() {
        Set<Entry<Integer, ConnectionValue>> s = null;
        synchronized(map) {
            s = map.entrySet();
        }
        return s;
    }

    /**
     * Remove the connection corresponding to key
     * @param key
     */
    public void removeConnection(int key) {
        synchronized(map) {
            map.remove(key);
        }
    }

    /**
     * Reads proc files for any open TCP and UDP connections to add to the {@link #map}.
     */
    public void findConnections() {
        parseProcFile("/proc/net/tcp", ipv4Pattern);
        parseProcFile("/proc/net/udp", ipv4Pattern);

        parseProcFile("/proc/net/tcp6", ipv6Pattern);
        parseProcFile("/proc/net/udp6", ipv6Pattern);
    }

    /**
     * Parses the provided /proc/net file and fills the {@link #map}
     * Reference: http://man7.org/linux/man-pages/man5/proc.5.html
     *
     * @param fileName the proc file to parse
     * @param pattern the pattern to match against (IPv4 vs IPv6)
     */
    private void parseProcFile(String fileName, Pattern pattern) {
        StringBuilder sb;
        try {
            File file = new File(fileName);
            BufferedReader readerForTcp4File = new BufferedReader(new FileReader(file));
            String line = "";
            sb = new StringBuilder();

            while ((line = readerForTcp4File.readLine()) != null) {
                sb.append(line);
            }
            readerForTcp4File.close();
        } catch (Exception ex) {
            Log.e(TAG, "Error parsing", ex);
            return;
        }

        String fileContent = sb.toString();
        if (fileContent.length() <= 0)
            return;

        Matcher matcher = pattern.matcher(fileContent);
        while (matcher.find()) {
            int uidEntry = Integer.valueOf(matcher.group(6));

            // Do not keep connection mappings of our own app since when it intercepts TLS
            // it uses the same port -> this leads to multiple apps being mapped to the same port
            // Plus, we get to save on space by not keeping track of our app in the HashMap
            if (uidEntry == myUID)
                continue;

            String srcPortEntry = matcher.group(2);
            int srcPort = Integer.valueOf(srcPortEntry, 16);

            String appName = getAppName(uidEntry);
            synchronized (map) {
                ConnectionValue oldValue = map.get(srcPort);

                if (oldValue != null) {
                    if (appName.equals(oldValue.getAppName())) {
                        // avoid calling getPackageInfo -> takes a lot of memory
                        // so we can simply re-use our existing ConnectionValue
                        continue;
                    } else if (appName.equals("System")) {
                        // When apps get killed, their uid in /proc files become 0
                        // Old app name might be better than System, so we keep it if we have it
                        appName = oldValue.getAppName();
                    }
                }

                String versionName = null;
                try {
                    versionName = pm.getPackageInfo(appName, 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing, some apps (s.t. System) are versionless
                }

                ConnectionValue v = new ConnectionValue(appName, versionName);
                map.put(srcPort, v);
            }
        }
    }

    /**
     * Helper function to transform String of hex values to a byte array
     * Taken from http://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
     * @param s - the string of hex values
     * @return byte array representation of String s
     */
    public static byte[] hexStringToByteArray (String s) throws MalformedInputException {
        int len = s.length();

        // Check the length
        if (len % 2 != 0)
            throw new MalformedInputException(len);

        // Convert
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Retrieves the name of the app based on the given uid
     *
     * @param uid
     *            - of the app
     * @return the name of the package of the app with the given uid, or "Unknown" if
     *         no name could be found for the uid.
     */
    private String getAppName(int uid) {
		/* IMPORTANT NOTE:
		 * From https://source.android.com/devices/tech/security/ : "The Android
		 * system assigns a unique user ID (UID) to each Android application and
		 * runs it as that user in a separate process"
		 *
		 * However, there is an exception: "A closer relationship with a shared
		 * Application Sandbox is allowed via the shared UID feature where two
		 * or more applications signed with same developer key can declare a
		 * shared UID in their manifest."
		 */

        // See if this is root
        if (uid == 0)
            return "System";

        // If we can't find a running app, just get a list of packages that map to the uid
        String[] packages = pm.getPackagesForUid(uid);
        if (packages != null && packages.length > 0)
            return packages[0];

        Log.w(TAG, "Process with uid " + uid + " does not appear to be running!");
        return "Unknown";
    }

    /**
     * Shuts down background processes
     * (e.g., {@link ConnectionRemover})
     */
    public void shutdown() {
        connectionRemover.shutdown();
    }
}