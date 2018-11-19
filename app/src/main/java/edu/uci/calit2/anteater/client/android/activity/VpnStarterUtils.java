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
package edu.uci.calit2.anteater.client.android.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.analysis.AMPacketConsumer;
import edu.uci.calit2.anteater.client.android.analysis.InspectorPacketFilter;
import edu.uci.calit2.anteater.client.android.device.Installation;
import edu.uci.calit2.anteater.client.android.util.PreferenceTags;
import edu.uci.calit2.antmonitor.lib.logging.PacketConsumer;
import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor;
import edu.uci.calit2.antmonitor.lib.vpn.IncPacketFilter;
import edu.uci.calit2.antmonitor.lib.vpn.OutPacketFilter;
import edu.uci.calit2.antmonitor.lib.vpn.PacketFilter;
import edu.uci.calit2.antmonitor.lib.vpn.TLSCertificateActivity;
import edu.uci.calit2.antmonitor.lib.vpn.VpnController;

/**
 * Helper class that contains methods used when starting the VPN service from an activity
 * @author Anastasia Shuba
 */
public class VpnStarterUtils {
    private static OutPacketFilter outPacketFilter;

    private static final String TAG = VpnStarterUtils.class.getSimpleName();

    /**
     * Convenience method used for retrieving the {@link PacketConsumer} used by our app
     * for incoming traffic
     * @param context
     * @return {@link AMPacketConsumer} marked for incoming traffic
     */
    static PacketConsumer getIncConsumer(Context context) {
        return new AMPacketConsumer(context, PacketProcessor.TrafficType.INCOMING_PACKETS,
                Installation.id(context));
    }

    /**
     * Convenience method used for retrieving the {@link PacketConsumer} used by our app
     * for outgoing traffic
     * @param context
     * @return {@link AMPacketConsumer} marked for outgoing traffic
     */
    static PacketConsumer getOutConsumer(Context context) {
        return new AMPacketConsumer(context, PacketProcessor.TrafficType.OUTGOING_PACKETS,
                Installation.id(context));
    }

    /**
     * Convenience method used for retrieving the {@link PacketFilter} used by our app
     * for outgoing real-time traffic
     * @param context
     * @return {@link InspectorPacketFilter} marked for outgoing traffic
     */
    public static OutPacketFilter getOutFilter(Context context) {
        if (outPacketFilter == null) {
            outPacketFilter = new InspectorPacketFilter(context);
        }
        return outPacketFilter;
    }

    /**
     * Convenience method used for retrieving the {@link IncPacketFilter} used by our app
     * for incoming real-time traffic
     * @param context
     * @return {@code null} for default
     */
    static IncPacketFilter getIncFilter(Context context) {
        return null;
    }

    /**
     * Sets SSL bumping to enabled/disabled according to user preferences
     */
    static void setSSLBumping(Activity activity) {
        // Check the ssl bumping user prefs
        SharedPreferences sp = activity.getSharedPreferences(PreferenceTags.PREFS_TAG,
                                                            Context.MODE_PRIVATE);
        boolean sslEnabled = sp.getBoolean(PreferenceTags.PREFS_SSL_BUMPING, false);

        // Enable SSL bumping if allowed by the user
        try {
            VpnController.setSSLBumpingEnabled(activity, sslEnabled);

            // If SSL bumping is successfully enabled, add pinned domains
            if (sslEnabled)
                addPinnedDomains(activity);

        } catch (IllegalStateException e) {
            installSSLCert(activity);
        }

    }

    private static void addPinnedDomains(Activity activity) {
        Log.d(TAG, "Adding domains...");

        InputStream in = activity.getResources().openRawResource(R.raw.domains);

        try {
            VpnController.addPinnedDomains(in);
        } catch (IOException e) {
            Log.e(TAG, "Could not add pinned domains.", e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                Log.w(TAG, "Could not close resource.", e);
            }
        }
    }

    public static void installSSLCert(Activity activity) {
        Intent i = new Intent(activity, TLSCertificateActivity.class);
        activity.startActivityForResult(i, VpnController.REQUEST_INSTALL_CERT);
    }
}
