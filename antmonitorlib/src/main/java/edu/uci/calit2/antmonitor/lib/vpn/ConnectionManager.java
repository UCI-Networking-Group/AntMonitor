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
package edu.uci.calit2.antmonitor.lib.vpn;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Helper class to get access to the Connectivity System service on Android.
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public class ConnectionManager {
    private Context mContext;

    public ConnectionManager(Context context){
        this.mContext = context;
    }

    /**
     * Check whether or not the device is connected to the internet.
     * @return true if the device has an active network interface connected to the internet.
     */
    public boolean isConnectedToInternet(){
        ConnectivityManager connectivity = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (NetworkInfo anInfo : info)
                    if (anInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
        }
        return false;
    }

    /**
     * Helper method to access the {@link android.net.ConnectivityManager}.
     * @param ctx context
     * @return an instance of the Connectivity system service.
     */
    public static ConnectivityManager getManager(Context ctx){
        return (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
}
