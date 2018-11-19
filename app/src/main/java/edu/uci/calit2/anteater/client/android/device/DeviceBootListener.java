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
import android.util.Log;

import edu.uci.calit2.anteater.client.android.activity.VpnStarterActivity;

/**
 * <p>
 *      A {@link android.content.BroadcastReceiver} that is invoked when the device has booted. It
 *      attempts to reestablish the VPN connection on boot so that data collection may continue after
 *      a reboot.
 * </p>
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public class DeviceBootListener extends BroadcastReceiver {

    private final String mClassTag = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(mClassTag, "Boot completed, attempting to restart VPN.");

            // Launch invisible activity (only way to request VPN rights)
            Intent vpnStartRequest = new Intent(context, VpnStarterActivity.class);
            vpnStartRequest.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(vpnStartRequest);
        }
    }
}
