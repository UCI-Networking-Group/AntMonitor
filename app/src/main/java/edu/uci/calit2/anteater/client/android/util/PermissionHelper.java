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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Helper class that keeps constants and methods used by multiple classes
 * @author Hieu Le
 */
public class PermissionHelper {
    private static final String TAG = PermissionHelper.class.getSimpleName();

    public static final int PERMISSION_REQUEST = 1;

    public static boolean requestPermissions(Activity activity) {

        boolean hasRequiredPermissions = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                activity.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                activity.checkSelfPermission(Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_DENIED) {
                String[] permissions = {
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.GET_ACCOUNTS
                };
                hasRequiredPermissions = false;
                activity.requestPermissions(permissions, PERMISSION_REQUEST);
            }

        }

        return hasRequiredPermissions;
    }

    public static void onRequestPermissionsResult(Activity activity, int requestCode,
                                                  String permissions[], int[] grantResults) {

        switch (requestCode) {
            case PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted for location");
                } else {
                    // TODO: Figure out what to do if we do not get permission
                    Log.d(TAG, "Permission denied for location");
                }
                return;
            }
        }
    }

}
