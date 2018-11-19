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
package edu.uci.calit2.anteater.client.android.analysis;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 *  Facilitates filtering specific apps such that those apps will not have packets that are tied to
 *  the application added to the log files.
 * </p>
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public class ApplicationFilter{
    private static ApplicationFilter instance;
    public static final String SHARED_PREFS_FILTER_TAG = "APPFILTERPREFS";
    public static final String SHARED_PREFS_FILTERED_APPS_KEY = "edu.uci.calit2.anteater.APPS_FILTERED_KEY" ;

    // Concurrent HashMap.
    Set<String> filteredApps = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private Context mContext;

    /**
     * Singleton getter.
     * @param c context used to access shared preferences from.
     * @return The current instance of the ApplicationFilter, if none, a new instance is created.
     */
    public static ApplicationFilter getInstance(Context c)
    {
        if (instance == null)
        {
            // Create the instance
            instance = new ApplicationFilter(c);
        }
        // Return the instance
        return instance;
    }

    // Private constructor to prevent instantiation.
    private ApplicationFilter(Context c)
    {
        // Private because of singleton
        mContext = c;

        // Initialize Concurrent Set using values from shared preferences if possible.
        updateFilterFromPrefs();
    }

    public Set<String> getFilteredApps(){
        return filteredApps;
    }

    /**
     * Adds an application name to the filter.
     * @param appName The fully qualified package name for the application to filter.
     */
    public void addToFilter(String appName){
        filteredApps.add(appName);
    }

    /**
     * Removes the application name from the filter.
     * @param appName The fully qualified package name for the application to filter.
     */
    public void removeFromFilter(String appName){
        filteredApps.remove(appName);
    }

    /**
     * Clear the current filter and populate it with the application names found in the application filter previously persisted through shared preferences.
     */
    public void updateFilterFromPrefs(){
        if(mContext != null) {
            Set<String> set = mContext.getSharedPreferences(SHARED_PREFS_FILTER_TAG, Context.MODE_PRIVATE).
                                getStringSet(SHARED_PREFS_FILTERED_APPS_KEY, null);
            if (set != null) {
                filteredApps.clear();
                filteredApps.addAll(set);
            }
        }
    }


    public void enableAllApplicationsLogging(PackageManager pm, ArrayList<ApplicationInfo> installedApps, boolean includeSystemApps) {
        final String antMonitorPackageName = mContext.getPackageName();
        for(ApplicationInfo applicationInfo : installedApps){
            String packageName = applicationInfo.packageName;
            // Don't include our own app.
            if(!packageName.equals(antMonitorPackageName)){
                this.addToFilter(packageName);
            }
        }

        if (includeSystemApps) {
            setSystemAppsForLogging(pm, installedApps);
        }
    }

    public void disableAllApplicationsLogging(PackageManager pm, ArrayList<ApplicationInfo> installedApps, boolean includeSystemApps) {
        final String antMonitorPackageName = mContext.getPackageName();
        for(ApplicationInfo applicationInfo : installedApps){
            String packageName = applicationInfo.packageName;
            // Don't include our own app.
            if(!packageName.equals(antMonitorPackageName)){
                this.removeFromFilter(packageName);
            }
        }

        if (includeSystemApps) {
            clearSystemAppsFromLogging(pm, installedApps);
        }
    }

    public void setSystemAppsForLogging(PackageManager pm, ArrayList<ApplicationInfo> installedApps) {
        List<ApplicationInfo> appInfos = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        // list of package names from the installed apps list.
        ArrayList<String> packageNames = new ArrayList<String>();
        for (ApplicationInfo app : installedApps) {
            packageNames.add(app.packageName);
        }

        for (ApplicationInfo appInfo : appInfos) {
            if (!appInfo.packageName.equals(mContext.getApplicationContext().getPackageName()) && isSystemPackage(appInfo)) {
                // Don't add apps that are already part of the installed list.
                // This ensures that the user does not accidentally add apps from the list by
                // selecting system apps.
                if (packageNames.contains(appInfo.packageName)) {
                    continue;
                }

                // Add each system app to filter.
                addToFilter(appInfo.packageName);
            }
        }

        // Somehow these are not added under system apps per default, so adding them manually.
        addToFilter("com.google.process.gapps");
        addToFilter("System");
        addToFilter("com.google.android.googlequicksearchbox:search");
        addToFilter("com.google.process.location");

    }

    public void clearSystemAppsFromLogging(PackageManager pm, ArrayList<ApplicationInfo> installedApps) {
        List<ApplicationInfo> appInfos = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        // list of package names from the installed apps list.
        ArrayList<String> packageNames = new ArrayList<String>();
        for (ApplicationInfo app : installedApps) {
            packageNames.add(app.packageName);
        }


        for (ApplicationInfo appInfo : appInfos) {
            if (!appInfo.packageName.equals(mContext.getApplicationContext().getPackageName()) && isSystemPackage(appInfo)) {
                // Only remove system apps that are not part of the viewable list.
                if (packageNames.contains(appInfo.packageName)) {
                    continue;
                }

                //Remove each system app from filter
                removeFromFilter(appInfo.packageName);
            }
        }

        // Somehow these are not added under system apps per default, so removing them manually.
        removeFromFilter("com.google.process.gapps");
        removeFromFilter("System");
        removeFromFilter("com.google.android.googlequicksearchbox:search");
        removeFromFilter("com.google.process.location");

    }
    /**
     * Check if the {@code FLAG_SYSTEM} or {@code FLAG_UPDATED_SYSTEM_APP} is set for the application.
     * @param applicationInfo The application to check.
     * @return true if the {@code applicationInfo} belongs to a system or updated system application.
     */
    public boolean isSystemPackage(ApplicationInfo applicationInfo) {
        return (((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) || (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
    }
}
