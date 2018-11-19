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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import edu.uci.calit2.antmonitor.lib.util.PcapngFile;
import edu.uci.calit2.antmonitor.lib.vpn.ConnectionManager;

/**
 * Provides utility methods to manage and access traffic log files.
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public class TrafficLogFiles {

    /**
     * Files that are currently used for logging should use this prefix in the their file names.
     * This allows us to tell what log files are currently receiving log data and hence not ready for upload.
     */
    public static final String ACTIVE_LOG_FILE_PREFIX = "STREAM";

    /**
     * Traffic log files that are no longer used for logging should use this prefix in their file names.
     * This allows us to tell what log files are considered inactive/done and hence ready for upload.
     */
    public static final String COMPLETED_LOG_FILE_PREFIX = "COMPLETED";

    /**
     * Tag used for Android debug/error log.
     */
    private static final String TAG = TrafficLogFiles.class.getSimpleName();

    private static long sessionID = 0;

    private static String userID;

    /**
     * Gets an array containing the completed log files, i.e. all log files except the two currently used for logging.
     * @param ctx A context object of this application. Required in order to access internal storage for this application.
     * @return An array containing the completed log files, i.e. all log files except the two currently used for logging.
     */
    public static File[] getCompleted(Context ctx) {
        if (ctx == null)
            return new File[]{};
        // Find all files in the app folder to upload and delete the rest
        File folder = ctx.getFilesDir(); // incFile.getParentFile();
        return folder.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                // Only include log files we are no longer writing data to.
                return filename.startsWith(COMPLETED_LOG_FILE_PREFIX);
            }

        });
    }

    static synchronized String getUserID() { return userID; }

    static synchronized void setUserID(String id) { userID = id; }

    /**
     * Factory for creating a new set of stream files of the PCAPNG format, i.e. files to which
     * current network traffic can be written (streamed).
     * @param context A context object of this application.
     *                Required in order to access internal storage of this application.
     * @return A {@link android.util.Pair} of {@link PcapngFile}s. The
     *      first file of the pair is for inbound traffic, and the second file of the pair is for
     *      outbound traffic.
     */
    static Pair<PcapngFile, PcapngFile> createNewActiveFileSet(Context context) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        // Name file according to timestamp.
        String baseFilePathName = context.getFilesDir().getPath() + "/" +
                ACTIVE_LOG_FILE_PREFIX + "_" +
                cal.get(Calendar.DAY_OF_MONTH) + "-" +
                // January has value = 0. Add 1 to enhance readability.
                (cal.get(Calendar.MONTH) + 1) + "-" +
                cal.get(Calendar.YEAR) + "_" +
                cal.get(Calendar.HOUR_OF_DAY) + "-" +
                cal.get(Calendar.MINUTE) + "-" +
                cal.get(Calendar.SECOND) + "-" +
                cal.get(Calendar.MILLISECOND);

        PcapngFile incFile = createFile(baseFilePathName +
                PacketProcessor.TrafficType.INCOMING_PACKETS.getTrafficTypeString() +
                ".pcapng", context);

        PcapngFile outFile = createFile(baseFilePathName +
                PacketProcessor.TrafficType.OUTGOING_PACKETS.getTrafficTypeString() +
                ".pcapng", context);

        return new Pair<PcapngFile, PcapngFile>(incFile, outFile);
    }

    /**
     * Marks a file as completed, i.e. changes the file prefix from {@link #ACTIVE_LOG_FILE_PREFIX} to {@link #COMPLETED_LOG_FILE_PREFIX}.
     * @param file The file that is currently active and should now be renamed as completed.
     * @return {@code true} if the file was successfully marked as completed, false otherwise.
     */
    static File getCompletedFileName(File file) {
        String oldAbsPath = file.getAbsolutePath();

        Log.d(TAG, "getCompletedFilename(File) called for file: " + oldAbsPath);
        if (!file.getName().startsWith(ACTIVE_LOG_FILE_PREFIX)) {
            Log.e(TAG, "Aborting attempt to mark non-stream file as completed.");
            return null;
        }

        String newName = file.getName().replaceFirst(ACTIVE_LOG_FILE_PREFIX, COMPLETED_LOG_FILE_PREFIX);
        String parentDir = file.getParent();
        File newAbsPath = parentDir != null ? new File(parentDir + File.separator + newName) : new File(newName);

        boolean success = file.renameTo(newAbsPath);

        if(success) {
            Log.d(TAG, "Renamed '" + oldAbsPath + "' to '" + newAbsPath.getAbsolutePath() + "'");
        } else {
            Log.d(TAG, "Error renaming '" + oldAbsPath + "' to '" + newAbsPath.getAbsolutePath() + "'");
        }

        return file;
    }


    private static PcapngFile createFile(String filePathName, Context context){
        File file = new File(filePathName);
        // Create the file.
        boolean fileCreated = false;
        try {
            fileCreated = file.createNewFile();
        } catch(IOException ioe) {
            Log.e(TAG, "Error creating new dump file with absolute path: " + file.getAbsolutePath());
            ioe.printStackTrace();
        }


        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String version = pInfo.versionName;

        NetworkInfo info = ConnectionManager.getManager(context).getActiveNetworkInfo();
        String headerComment = "Installation ID = " + getUserID() + "\n" + "Session ID = " + sessionID + "\n";
        String ifName = "";
        String hardwareInfo = DeviceInfo.getDeviceName();
        String osInfo = Build.VERSION.RELEASE;
        String appInfo = version;
        String ifDescription = "";
        String ifMacAddr = "";
        long speed = 0; // Speed will be 0 for mobile, otherwise set to speed units in bps specified by the wifi link.

        int ifTimezone = 0;
        String ifFilter = "DefaultPacketLogger"; // TODO: Add defaultPacketLogger class here? or Connection filtering string?
        long tsOffset = 0;
        byte tsResolution = 3; // Timestamp is in milliseconds

        String ifIPAddr = DeviceInfo.getIPAddress(true);

        if(info != null){
            ifName = info.getTypeName();

            if(info.getTypeName().equals("MOBILE")){
                ifDescription = info.getSubtypeName();
            } else {
                ifDescription = info.getExtraInfo();
                ifMacAddr = DeviceInfo.getMACAddress(DeviceInfo.getRunningInterfaceName());

                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    speed = wifiInfo.getLinkSpeed() * 1000000; //mbps to bps
                }
            }
        }

        PcapngFile pcapFile = null;
        if (ifDescription == null) {
            Log.w(TAG, "Could not get interface description!");
            ifDescription = "N/A";
        }
        try {
            pcapFile = new PcapngFile(file, headerComment, hardwareInfo, osInfo, appInfo, ifName, ifDescription, ifIPAddr, ifMacAddr, speed, ifTimezone, ifFilter, tsOffset, tsResolution);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return pcapFile;
    }

}
