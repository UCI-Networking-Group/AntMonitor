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

/**
 * Helper class that keeps constants and methods used by multiple classes
 * @author Anastasia Shuba
 */
public class PreferenceTags {
    public static final String PREFS_TAG = "ANTMONITOR_USER_PREFS";

    // For Inspection prefs
    public static final String PREFS_SET_STRINGS_KEY = "SET_STRINGS_KEY";
    public static final String PREFS_CUSTOM_STRINGS_KEY = "CUSTOM_STRINGS_KEY";
    public static final String PREFS_CUSTOM_UNCHECKED_KEY = "CUSTOM_STRINGS_UNCHECKED_KEY";
    public static final String PREFS_LOCATION_KEY = "SEARCH_FOR_LOCATION";

    public static final String PREFS_SSL_BUMPING = "SSL_BUMPING";

    public static final String PREFS_LEAK_REPORT_SORT = "PREFS_LEAK_REPORT_SORT";
    public static final String PREFS_GETTING_STARTED_COMPLETE = "PREFS_GETTING_STARTED_COMPLETE";
    public static final String PREFS_REALTIME_OVERLAY_FIRST = "PREFS_REALTIME_OVERLAY_FIRST";

    // Full packet pref
    public static final String FULL_PACKET = "FULL_PACKET";

    public static final String CONTRIBUTION_PREFS = "CONTRIBUTION_PREFS";
}
