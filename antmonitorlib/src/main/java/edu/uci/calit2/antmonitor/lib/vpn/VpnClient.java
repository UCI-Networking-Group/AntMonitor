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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;

import edu.uci.calit2.antmonitor.lib.R;
import edu.uci.calit2.antmonitor.lib.logging.PacketConsumer;
import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor;
import edu.uci.calit2.antmonitor.lib.logging.PacketQueueReader;
import edu.uci.calit2.antmonitor.lib.logging.PacketLogQueue;

/**
 * This class is responsible for establishing and maintaining the VPN connection.
 * Although nothing in it should be used by the library's extensions, it is made public in order
 * to be declared in the library Manifest file.
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public class VpnClient extends android.net.VpnService {

    static final int VPN_FOREGROUND_ID = 42;

    static final String VPN_ACTION_NOTIFICATION = "edu.uci.calit2.anteater.ACTION.NOTIFICATION";
    static final String VPN_STATE_BROADCAST_ACTION = "edu.uci.calit2.anteater.VPN_STATE_BROADCAST_ACTION";
    static final String VPN_STATE_BROADCAST_STATE_KEY = "edu.uci.calit2.anteater.VPN_STATE_BROADCAST_STATE_KEY";

    static final long STARTING_RECONNECT_DELAY_MILLIS = 5000;

    /**
     * Extra key used when launching a {@link VpnClient} via
     * {@link Context#startService(Intent)}.
     * The key should map a boolean value that denotes if the {@code VpnClient} should automatically
     * connect to the VPN server as part of its {@link #onStartCommand(Intent, int, int)}
     * invocation.
     */
    static final String EXTRA_CONNECT_ON_STARTUP = "edu.uci.calit2.anteater.EXTRA_CONNECT_ON_STARTUP";
    
    private static final String TAG = VpnClient.class.getSimpleName();

    /** IP address used for the TUN*/
    public static final String mTunInterfaceIP = "192.168.0.2";

    /** TUN interface of phone. */
    private ParcelFileDescriptor mTunInterface;

    private static final PacketLogQueue mOutDatagramQueue = new PacketLogQueue();
    private static final PacketLogQueue mIncDatagramQueue = new PacketLogQueue();

    private static PacketConsumer mOutPacketConsumer;
    private static PacketConsumer mIncPacketConsumer;

    private static IncPacketFilter mIncPacketFilter;
    private static OutPacketFilter mOutPacketFilter;

    /**
     * Delay in milliseconds before attempting to reconnect.
     * This is increased for every attempt up till 1 day.
     */
    private long mReconnectDelayMillis = STARTING_RECONNECT_DELAY_MILLIS;

    /**
     * Handler tied to the main thread. Facilitates delayed invocation of connect attempts.
     * <em>THIS HANDLER SHOULD ONLY BE USED FOR CONNECT/DISCONNECT JOBS.</em>
     * As connect/disconnect jobs clears the message queue, there are no guarantees that your
     * code will run should you decide to post other jobs to this handler, as your code might
     * get removed from the message queue before it is scheduled to run.
     */
    private final Handler mHandler = new Handler();

    /** Server responsible for intercepting SSL connections */
    private TLSProxyServer sslBumpingServer;

    /**
     * Performs logging of incoming packets in a background thread.
     */
    private BackgroundJob<PacketQueueReader> mIncomingDumper;

    /**
     * Performs logging of outgoing packets in a background thread.
     */
    private BackgroundJob<PacketQueueReader> mOutgoingDumper;
    /**
     * The current state of the VPN connection managed by this {@link VpnClient}.
     * <em>Make sure to always synchronize access via {@link #mVpnStateLock}.</em>
     */
    private volatile VpnState mVpnState = VpnState.DISCONNECTED;

    /**
     * Lock object providing mutually exclusive access to {@link #mVpnState}.
     */
    private final Object mVpnStateLock = new Object();

    /**
     * Interface provided to clients allowing these to perform operations on this service.
     */
    private final VpnClientBinder mVpnClientBinder = new VpnClientBinder();

    /** The main class responsible for traffic routing. Started by this class. */
    private ForwarderManager manager;

    private BroadcastReceiver connectivityReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() invoked");
        //DebugFile.AppendToDebugFile("onBind invoked");
        return mVpnClientBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind() invoked");
        //DebugFile.AppendToDebugFile("onUnbind invoked");
        return super.onUnbind(intent);
    }

    class VpnClientBinder extends Binder {

        /**
         * Gets the current state of the VPN connection.
         * @return The current state of the VPN connection.
         */
        VpnState getVpnState() {
            synchronized (VpnClient.this.mVpnStateLock) {
                return VpnClient.this.mVpnState;
            }
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            // Slight hack to make sure onRevoke is called when binding to the outer service.
            // Courtesy of http://stackoverflow.com/a/15731435/1214974
            if (code == IBinder.LAST_CALL_TRANSACTION) {
                onRevoke();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        /**
         * Connects to the VPN server.
         * @param incConsumer
         * @param outConsumer
         */
        void connectToVpn(IncPacketFilter incFilter, OutPacketFilter outFilter,
                          PacketConsumer incConsumer, PacketConsumer outConsumer) {
            prepareVPNconnection(incFilter, outFilter, incConsumer, outConsumer);
            // Only do a single attempt since this is a manual connection attempt (i.e. initiated by the user).
            VpnClient.this.connect(0L, false);
        }

        /**
         * Disconnects the active VPN session, if any.
         */
        void disconnectVpn() {
            //DebugFile.AppendToDebugFile("Manual disconnect initiated.");
            VpnClient.this.disconnect();
        }

    }

    static void prepareVPNconnection(IncPacketFilter incFilter, OutPacketFilter outFilter,
                                     PacketConsumer incConsumer, PacketConsumer outConsumer) {
        mIncPacketConsumer = incConsumer;
        mOutPacketConsumer = outConsumer;
        mIncPacketFilter = incFilter;
        mOutPacketFilter = outFilter;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the service as a foreground service.
        startForeground(VPN_FOREGROUND_ID, buildVpnStateNotification());

        // Did client specify that we should connect the VPN right away?
        boolean autoConnect = intent.getBooleanExtra(EXTRA_CONNECT_ON_STARTUP, false);
        if (autoConnect) {
            Log.d(TAG, "Automatically connecting VPN as part of onStartCommand...");
            //DebugFile.AppendToDebugFile("Automatically connecting VPN as part of onStartCommand...");
            connect(0L, true);
        }


        // Don't restart the service unless StartService() is explicitly called.
        return START_NOT_STICKY;
        // TODO consider different intent.
        // return START_REDELIVER_INTENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        //Log.d(TAG, "Running onDestroy() on thread '" + Thread.currentThread().getName() + "'.");
        //DebugFile.AppendToDebugFile("Running onDestroy()");

        // Kill any open VPN connection (and publish new vpn state).
        this.disconnect();

        //DebugFile.AppendToDebugFile("Exiting");
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRevoke() {
        Log.d(TAG, ">>> VPN rights lost, running onRevoke! <<<");
        //DebugFile.AppendToDebugFile(">>> VPN rights lost, running onRevoke! <<<");
        // Shut down and publish new VPN state
        this.disconnect();
        super.onRevoke();
    }

    /**
     * Updates and publishes the state of the VPN connection managed by this {@link VpnClient}.
     * @param newVpnState The new state of the VPN connection.
     */
    private void publishVpnState(VpnState newVpnState) {
        //Log.d(TAG, "Publishing new VPN state with value = " + newVpnState);
        //DebugFile.AppendToDebugFile(TAG + " Publishing new VPN state with value = " + newVpnState);
        synchronized (mVpnStateLock) {
            // First: update the VPN state.
            mVpnState = newVpnState;

            // Second: broadcast the new VPN state
            Intent intent = new Intent();
            intent.setAction(VPN_STATE_BROADCAST_ACTION);
            intent.putExtra(VPN_STATE_BROADCAST_STATE_KEY, mVpnState);
            // We only publish the state to this application.
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            // Third: update the ongoing VPN state notification.
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(VPN_FOREGROUND_ID, buildVpnStateNotification());
        }
    }

    /**
     * <p>
     *     Performs a complete teardown of the VPN connection.
     *     This includes terminating the two threads used for reading and writing to and from the TUN interface and VPN tunnel,
     *     terminating the packet loggers, as well as closing the TUN interface and the socket used for the VPN tunnel.
     *     It is up to the caller to update and publish the new VPN state using {@link #publishVpnState(VpnState)} after closing the connection.
     * </p>
     */
    private void teardown() {
        //Log.d(TAG, "Running teardown()");
        //DebugFile.AppendToDebugFile("Running teardown()");

        if (connectivityReceiver != null) {
            unregisterReceiver(connectivityReceiver);
            connectivityReceiver = null;
        }

        // Stop main routing thread
        if (manager != null) {
            manager.shutdown();
            manager = null;
        }

        // Stop SSL bumping thread + resolver
        if (sslBumpingServer != null) {
            sslBumpingServer.interrupt();
            sslBumpingServer = null;
        }

        // Let the filters know we are done
        if (mIncPacketFilter != null)
            mIncPacketFilter.onVPNStop();
        if (mOutPacketFilter != null)
            mOutPacketFilter.onVPNStop();

        // This code must run on the main thread.
        VPNUtils.ensureRunningOnMainThread(
                "teardown() invoked on wrong thread: it must be invoked on the main thread.");

        // Then we close the packet loggers.
        if (mIncomingDumper != null) {
            //Log.d(TAG, "teardown(): interrupting PacketDumper for incoming packets");
            mIncomingDumper.interrupt();
            mIncomingDumper = null;
        }
        if (mOutgoingDumper != null) {
            //Log.d(TAG, "teardown(): interrupting PacketDumper for outgoing packets");
            mOutgoingDumper.interrupt();
            mOutgoingDumper = null;
        }

        // Then we close the TUN interface
        if(mTunInterface != null) {
            //Log.d(TAG, "teardown(): closing TUN");
            try {
                // Close the TUN interface.
                mTunInterface.close();
            } catch (IOException ioe) {
                Log.e(TAG, "Error closing the TUN interface");
            }
            mTunInterface = null;
        }

        // Clean up
        PacketProcessor.shutdown();

        //Log.d(TAG, "teardown() done.");
        //DebugFile.AppendToDebugFile("Teardown done");
    }

    /**
     * Builds a new TUN interface
     * @return The established TUN interface or null if the application is not prepared.
     */
    public ParcelFileDescriptor buildTunInterface() {
        // TODO possible to re-use interface?

        // Configure TUN
        Builder builder = new Builder();

        // Set MTU size to match socket-write size
        builder.setMtu(ForwarderManager.SOCKET_BYTEBUFFER_WRITE_SIZE);

        // 32-bit IP
        builder.addAddress(mTunInterfaceIP, 32);

        // Route all traffic
        builder.addRoute("0.0.0.0", 0);

        // Use Google's DNS server
        builder.addDnsServer("8.8.8.8");

        // Create a new interface using the builder and save the parameters.
        ParcelFileDescriptor tunInterface = null;
        try {
            tunInterface = builder.setSession(TAG)
                    .setConfigureIntent(null)
                    .establish();
        } catch (IllegalStateException e) {
            /*Toast.makeText(this, "TUN interface busy, cannot connect. Scheduling re-attempt...",
                            Toast.LENGTH_LONG).show();*/
            Log.e(TAG, e.getMessage(), e.getCause());
            return null;
        }

        if (tunInterface == null)
            Log.e(TAG, "Could not establish VPN connection: VPN rights were not granted.");

        //Log.i(TAG, "New interface");
        return tunInterface;
    }

    /**
     * TODO: Add Class Doc
     * TODO change Boolean return type to enum to allow better error reports (e.g. wrong password).
     */
    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        private final String classTag = getClass().getSimpleName();

        private long DAY_IN_MILLIS = 86400000;
        private final boolean mRetryOnFailure;

        /**
         * Create a new {@link VpnClient.ConnectTask}.
         * @param retryOnFailure Specifies how to handle a failed connection attempt. {@code true} indicates that a new attempt should be initiated.
         */
        ConnectTask(boolean retryOnFailure) {
            this.mRetryOnFailure = retryOnFailure;
        }

        @Override
        protected void onPreExecute() {
            //Log.d(this.classTag, "onPreExecute before locking on state.");
            synchronized (VpnClient.this.mVpnStateLock) {
                //Log.d(this.classTag, "onPreExecute, VpnState = " + VpnClient.this.mVpnClientBinder.getVpnState());
                //DebugFile.AppendToDebugFile( "onPreExecute, VpnState = " + VpnClient.this.mVpnClientBinder.getVpnState());
                VpnState currState = VpnClient.this.mVpnClientBinder.getVpnState();
                if (currState == VpnState.CONNECTED || currState == VpnState.CONNECTING) {
                    // If some other instance started connecting or successfully connected, cancel this one.
                    // cancel(boolean) ensures that onPostExecute does not run.
                    this.cancel(false);
                    return;
                }
                // Remove any other pending connect attempts - this attempt takes precedence.
                VpnClient.this.mHandler.removeCallbacksAndMessages(null);
                // We are initiating a connection attempt so the VPN connection enters a new state.
                // Update state and and broadcast it to interested clients.
                VpnClient.this.publishVpnState(VpnState.CONNECTING);
            }
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (isCancelled()) {
                // Another instance started execution before this one.
                return false;
            }

            //Log.d("AsyncRetryScheduler", "Running DoInBackground");
            //DebugFile.AppendToDebugFile("Running DoInBackground");

            if(!(new ConnectionManager(VpnClient.this).isConnectedToInternet())) {
                Log.d(this.classTag, "No internet connection. Cancelling connect attempt.");
                //DebugFile.AppendToDebugFile("No internet connection. Cancelling task.");
                return false;
            }
/*            try {
                // Make sure we closed properly before attempting reconnect
                if (this.mTunInterface != null)
                    mTunInterface.close();*/

            mTunInterface = buildTunInterface();
            if (mTunInterface == null)
                return false;

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            synchronized (mVpnStateLock) {
                //Log.d(this.classTag, "onPostExecute result: " + result);
                //DebugFile.AppendToDebugFile("onPostExecute result: " + result);
                if (VpnClient.this.mVpnClientBinder.getVpnState() == VpnState.DISCONNECTED) {
                    // A disconnect call was run during doInBackground.
                    // To ensure proper state, we must "re-disconnect".
                    VpnClient.this.disconnect();
                    return;
                }

                if (result) {
                    // Successfully connected.
                    // Update VpnClient with new threads for handling the connection.

                    //TODO: for now leave a bunch of this stuff out
                    this.setupParentForNewConnection();

                    // Reset connection delay.
                    VpnClient.this.mReconnectDelayMillis = 0;
                    long sessionStart = System.currentTimeMillis();
/*                    VpnClient.this.mDownloadTrafficStats.startSession(sessionStart);
                    VpnClient.this.mUploadTrafficStats.startSession(sessionStart);*/
                    // Update and broadcast new VPN state.
                    VpnClient.this.publishVpnState(VpnState.CONNECTED);
                    return;
                }

                // Unsuccessful connection attempt.
                // Should we keep trying?
                if (this.mRetryOnFailure) {
                    Log.d(this.classTag, "VPN connection attempt failed. Scheduling retry...");
                    //DebugFile.AppendToDebugFile("VPN connection attempt failed. Scheduling retry...");
                    // Delay next attempt.
                    this.incrementDelay();
                    // Update VPN state to reflect that we are about to schedule a new attempt.
                    VpnClient.this.publishVpnState(VpnState.SCHEDULED_CONNECT);
                    // Schedule the new attempt.
                    VpnClient.this.connect(VpnClient.this.mReconnectDelayMillis, true);
                } else {
                    Log.d(this.classTag, "VPN connection attempt failed. No retry.");
                    //DebugFile.AppendToDebugFile("VPN connection attempt failed. No retry.");
                    // Update VPN state to reflect that the VPN is disconnected and that there are no scheduled connection attempts.
                    VpnClient.this.publishVpnState(VpnState.DISCONNECTED);
                }
            }
        }

        private void incrementDelay() {
            Log.d(this.classTag, "Incrementing Delay From: " + VpnClient.this.mReconnectDelayMillis);
            //DebugFile.AppendToDebugFile("Incrementing Delay From: " + VpnClient.this.mReconnectDelayMillis);
            if(VpnClient.this.mReconnectDelayMillis == 0){
                VpnClient.this.mReconnectDelayMillis = STARTING_RECONNECT_DELAY_MILLIS;
            } else if(VpnClient.this.mReconnectDelayMillis >= DAY_IN_MILLIS){
                // Maximum Period is 1 Day
                VpnClient.this.mReconnectDelayMillis = DAY_IN_MILLIS;
            } else {
                VpnClient.this.mReconnectDelayMillis = VpnClient.this.mReconnectDelayMillis * 2;
            }
            Log.d(this.classTag, "Delay is set to: " + VpnClient.this.mReconnectDelayMillis);
            //DebugFile.AppendToDebugFile("Delay is set to: " + VpnClient.this.mReconnectDelayMillis);
        }

        /**
         * Updates the surrounding {@link VpnClient} to use the
         * connection established by {@link VpnClient.ConnectTask}.
         * <em>In order to avoid threading issues, make sure to invoke this method on the UI thread.</em>
         */
        private void setupParentForNewConnection() {
            VPNUtils.ensureRunningOnMainThread("setupParentForNewConnection must run on main thread.");

            // Start SSL server
            sslBumpingServer = new TLSProxyServer(VpnClient.this);
            sslBumpingServer.setName(TLSProxyServer.class.getSimpleName());
            sslBumpingServer.start();


            if (mIncPacketConsumer != null) {
                VpnClient.this.mIncomingDumper = new BackgroundJob<>(new PacketQueueReader(
                        mIncDatagramQueue, mIncPacketConsumer));
            }

            if (mOutPacketConsumer != null) {
                VpnClient.this.mOutgoingDumper = new BackgroundJob<>(new PacketQueueReader(
                        mOutDatagramQueue, mOutPacketConsumer));
            }


            if (mIncPacketFilter == null)
                mIncPacketFilter = new IncPacketFilter(VpnClient.this);

            if (mOutPacketFilter == null)
                mOutPacketFilter = new OutPacketFilter(VpnClient.this);

            // Let the filters know that we are starting
            mIncPacketFilter.onVPNStart();
            mOutPacketFilter.onVPNStart();

            // pass in null if there are no default packet consumers
            manager = ForwarderManager.getInstance(VpnClient.this,
                                                    mIncPacketConsumer != null ? mIncDatagramQueue : null,
                                                    mOutPacketConsumer != null ? mOutDatagramQueue : null,
                                                    mIncPacketFilter, mOutPacketFilter);
            manager.start(mTunInterface);

            // Start the packet-to-application mapper if they not null
            if (VpnClient.this.mIncomingDumper != null)
                VpnClient.this.mIncomingDumper.start();

            if (VpnClient.this.mOutgoingDumper != null)
                VpnClient.this.mOutgoingDumper.start();

            // Listen to changes in Internet connectivity:
            connectivityReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    boolean noConnection =
                            intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

                    // If we lost internet connectivity, tell manager to reset all socket channels
                    if (noConnection)
                        manager.teardownConnections();
                }
            };

            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(connectivityReceiver, filter);
        }
    }

    void onTunnelError() {
        // Error occurred in mVpnReader or mVpnWriter.
        Log.d(TAG, "attempting automatic recconection");
        //DebugFile.AppendToDebugFile("onTunnelError(IOException) invoked by thread: " + Thread.currentThread().getName());
        reconnectOnError(true);
    }

    /**
     * <p>
     *     Attempts to connect the VPN.
     *     This closes any existing connection before attempting to establish a new connection.
     * </p>
     * <p>
     *     If a scheduled connect attempt is pending, this method cancels it.
     *     Moreover, if a connect attempt is currently in progress, this method lets the ongoing
     *     attempt run - i.e. this method does not schedule a new attempt.
     * </p>
     * @param delayMillis Delays the connection attempt (milliseconds).
     *                    Set to 0, or any value less than zero, to schedule the connect attempt to run immediately.
     *                    Note that even if a delay is specified, this method is asynchronous.
     *                    The connect attempt is scheduled to run on a background thread.
     * @param retryOnFailure Specifies what to do in case the connect attempt fails.
     *      {@code true}: repeatedly try to connect until successfully connected.
     *                       {@code false}: only perform a single attempt at connecting the VPN.
     */
    private void connect(long delayMillis, final boolean retryOnFailure) {
        //DebugFile.AppendToDebugFile("Scheduling a connect");
        //DebugFile.AppendToDebugFile("Delay = " + delayMillis);
        //DebugFile.AppendToDebugFile("Retry: " + retryOnFailure);

        // If currently attempting to connect, do nothing.
        if (VpnClient.this.mVpnClientBinder.getVpnState() == VpnState.CONNECTING) {
            //DebugFile.AppendToDebugFile("Aborting Scheduled Connect");
            return;
        }

        // Cancel any future connects.
        mHandler.removeCallbacksAndMessages(null);

        // Job that wraps the steps to be carried out when connecting the VPN.
        Runnable connectJob =  new Runnable() {
            @Override
            public void run() {
                // Disconnect and cleanup old connection.
                //DebugFile.AppendToDebugFile("Running Teardown from connectJob");
                VpnClient.this.teardown();
                // Initiate new connection attempt
                new ConnectTask(retryOnFailure).execute();
            }
        };

        if (delayMillis > 0) {
            // Delay connect job if a delay has been specified.
            mHandler.postDelayed(connectJob, delayMillis);
        } else {
            // Schedule connect job for immediate execution.
            mHandler.post(connectJob);
        }
    }

    /**
     * Schedules a reconnect attempt if no reconnect attempt is already scheduled or running.
     * @param retryOnFailure Specifies what to do in case the reconnect attempt fails.
     *      {@code true}: repeatedly try to connect until successfully connected.
     *      {@code false}: only perform a single attempt at connecting the VPN.
     */
    private void reconnectOnError(final boolean retryOnFailure) {
        synchronized (mVpnStateLock) {
            VpnState currState = VpnClient.this.mVpnClientBinder.getVpnState();
            if (currState == VpnState.CONNECTING || currState == VpnState.SCHEDULED_CONNECT
                   || currState == VpnState.DISCONNECTED) {
                // Reconnect already initiated
                // or we are no longer attempting to reconnect (user disconnected) so do nothing.
                return;
            }
            // No planned reconnect.
            // This is the first error message received.
            // Schedule reconnect attempt.
            publishVpnState(VpnState.SCHEDULED_CONNECT);
            connect(0L, retryOnFailure);
        }
    }

    /**
     * Disconnects the VPN connection and updates and publishes the new state, i.e. the disconnected
     * state, of the VPN connection. The disconnect job is sent to the main thread message queue,
     * so it may not have run when this method call returns (if invoked from a background thread).
     * Moreover, this method clears any pending connect attempts before initiating the disconnect.
     */
    private void disconnect() {
        //DebugFile.AppendToDebugFile("Disconnected Called");
        mHandler.removeCallbacksAndMessages(null);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Close down TUN interface, file descriptors, sockets etc.
                //DebugFile.AppendToDebugFile("Calling Teardown From Disconnect Runnable");
                synchronized (VpnClient.this.mVpnStateLock) {
                    VpnClient.this.teardown();
                    // Update and broadcast new VPN state.
                    VpnClient.this.publishVpnState(VpnState.DISCONNECTED);

                    // Remove service from foreground.
                    VpnClient.this.stopForeground(true);
                }
            }
        });
    }

    /**
     * Builds a {@link Notification} that provides information about the current state
     * of the VPN connection.
     * @return a {@link Notification} that provides information about the current state
     * of the VPN connection.
     */
    private Notification buildVpnStateNotification() {
        //TODO (library modularization) - allow them to pass image to display?
        Intent notificationIntent = new Intent(this, VpnClient.class);
        notificationIntent.setAction(VPN_ACTION_NOTIFICATION);
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this);
        notifBuilder.setContentTitle(getResources().getString(R.string.notification_title_vpnservice));
        notifBuilder.setSmallIcon(R.mipmap.shield);
        notifBuilder.setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false));
        notifBuilder.setContentIntent(pendingIntent);
        notifBuilder.setOngoing(true);

        // Set detail text according to current VPN state.
        String stateStr;
        switch (mVpnClientBinder.getVpnState()) {
            case DISCONNECTED:
                stateStr = getResources().getString(R.string.notification_detail_text_vpnservice_disconnected);
                break;
            case CONNECTING:
                stateStr = getResources().getString(R.string.notification_detail_text_vpnservice_connecting);
                break;
            case CONNECTED:
                stateStr = getResources().getString(R.string.notification_detail_text_vpnservice_connected);
                break;
            case SCHEDULED_CONNECT:
                stateStr = getResources().getString(R.string.notification_detail_text_vpnservice_scheduled_connect);
                break;
            default:
                stateStr = "ERROR: UNKNOWN STATE";
                break;
        }
        notifBuilder.setContentText(stateStr);

        return notifBuilder.build();
    }

}
