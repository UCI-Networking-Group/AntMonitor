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
package edu.uci.calit2.anteater.client.android.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverCallNotAllowedException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.util.Log;

/**
 * Abstract receiver that watches for WiFi and power supply connectivity. Subclasses should implement {@link #onPowerAndOnWifi(Context context)} to react to power and WiFi being connected.
 * <p>
 *      If an {@code Intent} with action equal to {@link Intent#ACTION_POWER_CONNECTED} is received, this receiver queries the charging state.
 *      If the device is also charging, this receiver invokes {@link #onPowerAndOnWifi(Context context)} in order to let subclass implementations react to the device being connected to WiFi and power.
 * </p>
 *
 * <p>
 *      If an {@code Intent} with action equal to {@link android.net.wifi.WifiManager#NETWORK_STATE_CHANGED_ACTION} is received, this receiver checks if the new WiFi state is the connected state and if power is also connected.
 *      If WiFi and power are both connected, this receiver invokes {@link #onPowerAndOnWifi(Context context)} in order to let subclass implementations react to the device being connected to WiFi and power.
 * </p>
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public abstract class WifiAndPowerConnectivityReceiver extends BroadcastReceiver {

    /**
     * Tag used for debug log.
     */
    protected final String TAG = getClass().getSimpleName();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Broadcast with action: " + intent.getAction() + ". Context == null:  " + (context == null));

        if (context == null)
            return;

        boolean onPower;
        boolean onWifi;

        if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            // Connected to power
            Log.d(TAG, "Connected to power supply!");
            onPower = true;
            // Now check if we are also on WiFi.
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            onWifi = wifiInfo.isConnected();
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            // WiFi connection state changed.
            // Check if we were connected.
            NetworkInfo nwInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            onWifi = nwInfo.isConnected();
            // Check if we are connected to power.
            IntentFilter battFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            try {
                Intent battStatusIntent = context.registerReceiver(null, battFilter);
                if (battStatusIntent == null)
                    return;

                int battStatus = battStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                onPower = battStatus == BatteryManager.BATTERY_STATUS_CHARGING || battStatus == BatteryManager.BATTERY_STATUS_FULL;
            } catch (ReceiverCallNotAllowedException e) {
                // This happens when VPN rights were not yet given
                // to the app by the user, so we simply ignore
                Log.w(TAG, e.getMessage(), e.getCause());
                return;
            }
        } else {
            // Invoked with intent that we do not support.
            // (Someone messed up when registering IntentFilters for this receiver)
            Log.e(TAG, "Returning from invocation of " + TAG + " onReceive without performing any work (due to action mismatch).");
            return;
        }

        if (onPower && onWifi) {
            // Let subclass know that we registered power and WiFi connectivity.
            this.onPowerAndOnWifi(context);
        }
    }

    /**
     * Invoked when this {@code WifiAndPowerConnectivityReceiver} determines that we are connected to power and WiFi.
     *
     * @param context The {@code Context} that was passed to the {@link #onReceive(android.content.Context, android.content.Intent)} invocation in which power and WiFi connectivity was detected.
     */
    protected abstract void onPowerAndOnWifi(Context context);
}
