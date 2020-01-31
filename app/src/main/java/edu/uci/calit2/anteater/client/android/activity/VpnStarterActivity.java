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
package edu.uci.calit2.anteater.client.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.analysis.AnalysisHelper;
import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.antmonitor.lib.logging.PacketConsumer;
import edu.uci.calit2.antmonitor.lib.vpn.IncPacketFilter;
import edu.uci.calit2.antmonitor.lib.vpn.OutPacketFilter;
import edu.uci.calit2.antmonitor.lib.vpn.VpnClient;
import edu.uci.calit2.antmonitor.lib.vpn.VpnController;


/**
 * A no-UI {@code Activity} that facilitates establishment of the VPN connection.
 * This is a two step process:
 * <ol>
 *      <li>
 *          First, this {@code Activity} calls
 *          {@link android.app.Activity#startActivityForResult(android.content.Intent, int)}
 *          in order to obtain user consent for setting up the VPN connection.
 *      </li>
 *      <li>
 *          Second, if the user consents to set up VPN connection, this {@code Activity} starts
 *          the {@link VpnClient} service with an {@code Intent} that has
 *          the {@link VpnClient#EXTRA_CONNECT_ON_STARTUP} extra set to
 *          {@code true} (in order to immediately connect to the VPN server).
 *      </li>
 * </ol>
 * <p>
 *     This {@code Activity} was designed to work in conjunction with
 *     {@link edu.uci.calit2.anteater.client.android.device.DeviceBootListener}.
 *     Together, these two classes facilitates
 *     automated VPN connection establishment following a device reboot.
 * </p>
 * @author Simon Langhoff, Janus Varmarken
 */
public class VpnStarterActivity extends Activity {

    /**
     * Logcat tag.
     */
    private final String mClassTag = this.getClass().getSimpleName();

    /**
     * Request code used when obtaining user consent for setting up the VPN connection.
     */
    private final int mPrepareVpnRequestCode = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(mClassTag, this.getClass().getSimpleName() + " started");

        Intent intent = android.net.VpnService.prepare(this);
        if (intent != null) {
            Log.d(mClassTag, "Requesting VPN rights via startActivityForResult...");
            // Request user consent.
            startActivityForResult(intent, mPrepareVpnRequestCode);
        } else {
            // User has already consented.
            onActivityResult(mPrepareVpnRequestCode, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == mPrepareVpnRequestCode && resultCode == RESULT_OK) {
            // VPN rights granted.
            Log.d(mClassTag, "VPN rights granted");

            VpnStarterUtils.setSSLBumping(this);
            PacketConsumer incConsumer = VpnStarterUtils.getIncConsumer(this);
            PacketConsumer outConsumer = VpnStarterUtils.getOutConsumer(this);
            OutPacketFilter outFilter = VpnStarterUtils.getOutFilter(this);
            IncPacketFilter incFilter = VpnStarterUtils.getIncFilter(this);

            VpnController.connectInBackground(this, incFilter, outFilter, incConsumer, outConsumer);

            Log.d(mClassTag, "called startService for VpnClient");
        } else {
            Log.d(mClassTag, "User declined request to obtain VPN rights.");
        }
        // Shut down this "worker" activity.
        this.finish();
    }
}
