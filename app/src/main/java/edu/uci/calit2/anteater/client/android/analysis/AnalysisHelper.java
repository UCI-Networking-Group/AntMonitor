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
package edu.uci.calit2.anteater.client.android.analysis;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.anteater.client.android.signals.LocationMonitor;
import edu.uci.calit2.antmonitor.lib.util.AhoCorasickInterface;

/**
 * Util class analysis
 *
 * @author Hieu Le
 */
public class AnalysisHelper {

    private static final String TAG = AnalysisHelper.class.getSimpleName();

    private static RebuildSearchTreeTask rebuildSearchTreeTask = null;

    /**
     * Rebuilds the Aho-Corasick search tree
     * @param cxt {@link Context} used to get {@link PrivacyDB} and {@link LocationMonitor}
     */
    private static void rebuildSearchTree(Context cxt) {

        PrivacyDB database = PrivacyDB.getInstance(cxt);
        LocationMonitor monitor = LocationMonitor.getInstance(cxt);

        List latitude = monitor.getLatitudeValues();
        List longitude = monitor.getLongitudeValues();

        // If user does not want to search for location, do not add in to the tree
        // NOTE: Nastia: this condition may be a problem later since we are stopping the detection of location based ONLY on the global filter settings for location
        // for example, if we want to disable location for global filter but only let specific apps have location..then how do we do that?
        if (!database.isLocationSearchEnabled(null)) {
            latitude = null;
            longitude = null;
        }

        AnalysisHelper.addSearchStrings(database.getAllEnabledPrivacyFiltersValues(), latitude, longitude);
    }

    public static void startRebuildSearchTree(Context context) {
        if (rebuildSearchTreeTask == null) {
            rebuildSearchTreeTask = new RebuildSearchTreeTask(context);
            rebuildSearchTreeTask.execute();
        }
    }

    private static class RebuildSearchTreeTask extends AsyncTask<Void, Void, Boolean> {

        private Context mContext;
        public RebuildSearchTreeTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            AnalysisHelper.rebuildSearchTree(mContext);
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (rebuildSearchTreeTask != null) {
                rebuildSearchTreeTask = null;
            }
        }

    }

    /**
     * Adds strings to the C library for searches
     */
    public static void addSearchStrings(HashSet<String> searchStrings,
                                        List latitude, List longitude) {
        ArrayList<String> finalSearchStrings = new ArrayList<>();

        if (latitude != null && longitude != null) {
            //finalSearchStrings.add(latitude);
            //finalSearchStrings.add(longitude);

            finalSearchStrings.addAll(latitude);
            finalSearchStrings.addAll(longitude);
        }

        if (!searchStrings.isEmpty()){
            finalSearchStrings.addAll(searchStrings);
        }

        Log.d(TAG, "addSearchStrings: " + finalSearchStrings.toString());

        if (!finalSearchStrings.isEmpty()) {
            String[] finalStrings = finalSearchStrings.toArray(new String[finalSearchStrings.size()]);
            // TODO: check init return value for errors
            AhoCorasickInterface.getInstance().init(finalStrings);

            // Make sure Inspector is enabled
            InspectorPacketFilter.enable();
        } else {
            // else don't look for anything -> disable Inspector
            InspectorPacketFilter.disable();
        }
    }

}
