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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import edu.uci.calit2.antmonitor.lib.AntMonitorActivity;
import edu.uci.calit2.antmonitor.lib.logging.PacketConsumer;

/**
 * A helper class meant to be used by Activities that are responsible for starting/stopping
 * the VPN service.
 *
 * @author Anastasia Shuba
 */
public class VpnController {
    /** Use this with {@link android.app.Activity#startActivityForResult}
     * to ask the user for VPN rights */
    public static final int REQUEST_VPN_RIGHTS = 1;

    /** Use this with {@link android.app.Activity#startActivityForResult}
     * to ask the user to install a root certificate, needed for SSL bumping. */
    public static final int REQUEST_INSTALL_CERT = 2;

    private static final String TAG = VpnController.class.getSimpleName();

    private static VpnStateReceiver mVpnStateReceiver;

    private VpnClient.VpnClientBinder mService = null;

    private static AntMonitorActivity mActivity;

    private static Context mActivContext;

    private static VpnController vpnController = new VpnController();

    private VpnController() {}

    /** Initializes the controller
     * @param activity must be a {@link Context} instance
     * @return an instance of the {@link VpnController}
     * @throws ClassCastException if {@code activity} is not a {@link Context} instance
     */
    public static VpnController getInstance(AntMonitorActivity activity) throws ClassCastException {
        if (activity == null)
            throw new IllegalArgumentException("activity cannot be null.");

        mActivContext = (Context) activity;
        mActivity = activity;

        mVpnStateReceiver = new VpnStateReceiver();

        return vpnController;
    }

    /** Binds to the VPN service and registers a receiver so that the calling activity can
     * receive updates about the VPN state through {@code onVpnStateChanged()}.
     * Typically called in the {@code onStart()} method of the {@link Activity} responsible
     * for starting/stopping the VPN connection. */
    public void bind() {
        // Bind to service.
        mActivContext.bindService(new Intent(mActivContext, VpnClient.class),
                                    mServiceConn, Activity.BIND_AUTO_CREATE);
        // Add receiver.
        IntentFilter intentFilter = new IntentFilter(VpnClient.VPN_STATE_BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(mActivContext).registerReceiver(mVpnStateReceiver,
                                                                            intentFilter);
    }

    /** Unbinds from the VPN service if we were previously bound to it and unregisters the
     * receiver used for VPN status updates. If the VPN is connected, it will remain connected.
     * Typically called in the {@code onStop()} method of the {@link Activity} responsible
     * for starting/stopping the VPN connection. */
    public void unbind() {
        // Only keep service alive if VPN is connected, is in the process of connecting,
        // or is scheduled to attempt to connect sometime in the future.
        // Note: must check this before unbinding as unbinding sets mService to null.
        boolean keepAlive = mService != null && mService.getVpnState() != VpnState.DISCONNECTED;

        if(mService != null) {
            // Unbind as interaction with service should only occur when activity is in the foreground.
            mActivContext.unbindService(mServiceConn);
            // Clear reference.
            mService = null;
        }

        if(!keepAlive) {
            mActivContext.stopService(new Intent(mActivContext, VpnClient.class));
        }

        // Remove receiver
        LocalBroadcastManager.getInstance(mActivContext).unregisterReceiver(mVpnStateReceiver);
    }

    /**
     * Attempts to start the VPN connection. Note that in order for this attempt to be successful,
     * you must be bound and you must have obtained VPN rights from the user.
     *
     * If any of the parameters are null, the default filter/consumer will be used.
     * @param incFilter this {@link PacketFilter} will be used to filter real-time incoming traffic
     * @param outFilter this {@link PacketFilter} will be used to filter real-time outgoing traffic
     * @param incPacketConsumer this {@link PacketConsumer}'s {@code consumePacket()} method will
     *                          be called when there is an incoming packet available for
     *                          off-line processing
     * @param outPacketConsumer this {@link PacketConsumer}'s {@code consumePacket()} method will
     *                          be called when there is an outgoing packet available for
     *                          off-line processing
     */
    public void connect(IncPacketFilter incFilter, OutPacketFilter outFilter, PacketConsumer
            incPacketConsumer, PacketConsumer outPacketConsumer) {
        // First: start the background VPN service so that it will stay alive even if we unbind.
        Intent intent = new Intent(mActivContext, VpnClient.class);
        mActivContext.startService(intent);
        // Then start connection procedure.
        mService.connectToVpn(incFilter, outFilter, incPacketConsumer, outPacketConsumer);
    }

    /**
     * Same as {@link #connect(IncPacketFilter, OutPacketFilter, PacketConsumer, PacketConsumer)}, except
     * it does not require binding (see {@link #bind()}). Use this method if you do not wish to
     * receive updates about the VPN state. Note that an unsuccessful connection from this method
     * will trigger automatic re-tries. This method is useful for re-connecting upon a device
     * boot. Use this method with caution as users may not want automatic re-connecting.
     *
     * @param context typically an {@link Activity} or a {@link android.app.Service}.
     * @param incFilter this {@link PacketFilter} will be used to filter real-time incoming traffic
     * @param outFilter this {@link PacketFilter} will be used to filter real-time outgoing traffic
     * @param incPacketConsumer this {@link PacketConsumer}'s {@code consumePacket()} method will
     *                          be called when there is an incoming packet available for
     *                          off-line processing
     * @param outPacketConsumer this {@link PacketConsumer}'s {@code consumePacket()} method will
     *                          be called when there is an outgoing packet available for
     *                          off-line processing
     */
    public static void connectInBackground(Context context, IncPacketFilter incFilter,
                                           OutPacketFilter outFilter,
                                           PacketConsumer incPacketConsumer,
                                           PacketConsumer outPacketConsumer) {
        VpnClient.prepareVPNconnection(incFilter, outFilter, incPacketConsumer, outPacketConsumer);
        Intent intent = new Intent(context, VpnClient.class);
        // Specify that the service should immediately establish the connection
        intent.putExtra(VpnClient.EXTRA_CONNECT_ON_STARTUP, true);

        // TODO: Re-enabling on restart does not work by this method on Android 10. Why?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //ContextCompat.startForegroundService(context, intent);
            //context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    static String getDnsServer () {
        return VpnClient.getDnsServer();
    }

    static void setDnsServer (String dnsServer) {
        VpnClient.setDnsServer(dnsServer);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setExcludedApps(List<String> excludedApps) {
        VpnClient.setExcludedApps(excludedApps);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void clearExcludedApps() {
        VpnClient.clearExcludedApps();
    }

    /** Disconnects the VPN if we are connected. Otherwise, nothing happens. */
    public void disconnect() {
        if (mService != null)
            mService.disconnectVpn();
    }

    /**
     * @param context used to check if certificate was installed
     * @param enabled pass {@code true} to enable SSL bumping, and
     * pass {@code false} otherwise
     * @throws IllegalStateException if certificate is not installed and {@code enabled} is
     * true
     */
    public static void setSSLBumpingEnabled(Context context, boolean enabled)
            throws IllegalStateException {
        ForwarderManager.setSSLBumpingEnabled(context, enabled);
    }

    /**
     * @param enabled enabled pass {@code true} to enable DNS cache, and
     * pass {@code false} otherwise
     */
    public static void setDnsCacheEnabled(boolean enabled) {
        ForwarderManager.KEEP_DNS_CACHE = enabled;
    }

    /**
     * Lookup hostname in DNS cache
     * @param address IP address to resolve
     * @return Resolved IP address
     */
    public static String retrieveHostname(String address) {
        return ForwarderManager.mDNScache.get(address);
    }

    /** @return {@code true} if SSL bumping is currently enabled, and {@code false} otherwise */
    public static boolean getSSLBumpingEnabled() {
        return ForwarderManager.getSSLBumpingEnabled();
    }

    /**
     * Add domain/app combos in the provided stream to a list of domain/apps that should not
     * attempt TLS interception. The provided JSON stream should be of the following format: <br>
            <pre>
             [
                 {
                     "packageName": "com.android.providers.downloads",
                     "domains": ["*.google.com"]
                 },

                 {
                     "packageName": "org.mozilla.firefox",
                     "domains": ["*.media.mozilla.com", "twitter.com"]
                 }
             ]
            </pre>
     Invoke this function every time you connect to VPN.
     * @param jsonStream the stream to read domain/app combos from. Callers must close the stream.
     * @throws IOException if the provided JSON could not be parsed correctly
     */
    public static void addPinnedDomains(InputStream jsonStream) throws IOException {
        if (!TLSProxyServer.addPinnedCNs(jsonStream))
            throw new IOException("Invalid JSON format. Please consult the library documentation.");
    }

    /** Convenience method for checking Internet connectivity
     * @return {@code true} if there is Internet connectivity, and {@code false} otherwise */
    public boolean isConnectedToInternet() {
        ConnectionManager manager = new ConnectionManager(mActivContext);
        return manager.isConnectedToInternet();
    }

    private final ServiceConnection mServiceConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = (VpnClient.VpnClientBinder) iBinder;
            // Fetch VPN state and update activity to reflect it.
            mActivity.onVpnStateChanged(mService.getVpnState());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };

    private static class VpnStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent == null)
                return;

            if(VpnClient.VPN_STATE_BROADCAST_ACTION.equals(intent.getAction())) {
                VpnState vpnState = (VpnState) intent.getSerializableExtra(
                                                VpnClient.VPN_STATE_BROADCAST_STATE_KEY);
                // Update to reflect new VPN state.
                mActivity.onVpnStateChanged(vpnState);
            }
        }
    }
}
