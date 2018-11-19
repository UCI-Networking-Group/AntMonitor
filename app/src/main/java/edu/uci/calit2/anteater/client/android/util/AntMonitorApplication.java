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
package edu.uci.calit2.anteater.client.android.util;

import android.app.Application;
import android.content.Context;
import android.os.Looper;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import edu.uci.calit2.antmonitor.lib.R;

/**
 * Helper class for Google Analytics tracking and retrieving the app context statically.
 * For explanation of the latter, see the following:
 * http://stackoverflow.com/questions/987072/using-application-context-everywhere
 *
 * @author Simon Langhoff, Janus Varmarken, Anastasia Shuba
 */
public class AntMonitorApplication extends Application {

    /** Keep a reference to app context */
    private static AntMonitorApplication instance;

    private Tracker mAppTracker;

    public AntMonitorApplication() {
        instance = this;
    }

    /** Use this to get a reference to {@link android.content.Context} when needing it outside
     * of an activity
     * @return app {@link android.content.Context}
     */
    public static Context getAppContext() {
        return instance;
    }

    public synchronized Tracker getAppTracker() {
        if (mAppTracker == null) {
            GoogleAnalytics ga = GoogleAnalytics.getInstance(this);
            ga.enableAutoActivityReports(this);
            mAppTracker = ga.newTracker(R.xml.app_tracker_config);
        }
        return mAppTracker;
    }


}
