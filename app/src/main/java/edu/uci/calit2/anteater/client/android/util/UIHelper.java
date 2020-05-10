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
package edu.uci.calit2.anteater.client.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.util.HashMap;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.analysis.AMPacketConsumer;
import edu.uci.calit2.anteater.client.android.analysis.FileUploadService;
import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.antmonitor.lib.logging.TrafficLogFiles;

/**
 * Util class containing the various methods for connecting various UI fragments/activities to backend
 * @author Nivedhita Sathyamurthy
 */
public class UIHelper {

    private static final String TAG = UIHelper.class.getSimpleName();
    private static final HashMap<String, String> packageToImageBase64 = new HashMap();

    public static void setPackageToImageBase64(String packageName, String imageBase64) {
        if (!packageToImageBase64.containsKey(packageName)) {
            packageToImageBase64.put(packageName, imageBase64);
        }
    }

    public static String getImageBase64(String packageName) {
        if (packageToImageBase64.containsKey(packageName)) {
            return packageToImageBase64.get(packageName);
        }

        return null;
    }

    public static int getDefaultContributionLevel(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PreferenceTags.PREFS_TAG, Context.MODE_PRIVATE);
        return sp.getInt(PreferenceTags.CONTRIBUTION_PREFS, R.id.low_contribute);
    }

    public static void setContributionLevel(Context context, int level) {
        //Log.d(TAG, "setContributionLevel: " + level);
        SharedPreferences sp = context.getSharedPreferences(PreferenceTags.PREFS_TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PreferenceTags.CONTRIBUTION_PREFS, level).apply();
        AMPacketConsumer.setPacketLogging(level);
    }

    public static void uploadLogs(Activity activity, Context context) {
        File[] filesToUpload = TrafficLogFiles.getCompleted();
        for (int i = 0; i < filesToUpload.length; i++) {
            if (!filesToUpload[i].exists()) {
                // ignore file if it was removed from disk
                continue;
            }

            Intent serviceStarter = new Intent(activity, FileUploadService.class);
            serviceStarter.putExtra(FileUploadService.EXTRA_FILE, filesToUpload[i].getAbsolutePath());

            Log.d(TAG, "Starting upload for: " + filesToUpload[i].getAbsolutePath());
            context.startService(serviceStarter);
        }

        if (filesToUpload.length != 0 && (TrafficLogFiles.getCompleted().length - filesToUpload.length) == 0) {
            PrivacyDB database = PrivacyDB.getInstance(context);
            database.logUpload(filesToUpload.length);
            database.close();
        }
    }

    public static void clearLogs(Activity activity) {
        File[] filesToUpload = TrafficLogFiles.getCompleted();

        for (int i =0; i < filesToUpload.length; i++) {
            if (!filesToUpload[i].exists()) {
                //continue if file was removed from disk
                continue;
            }

            filesToUpload[i].delete();
        }
    }

}
