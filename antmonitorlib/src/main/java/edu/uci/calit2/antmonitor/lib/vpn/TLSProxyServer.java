/*
 *  This file is part of AntMonitor <https://athinagroup.eng.uci.edu/projects/antmonitor/>.
 *  Copyright (C) 2018 Anastasia Shuba and the UCI Networking Group
 *  <https://athinagroup.eng.uci.edu>, University of California, Irvine.
 *  Copyright (C) 2014  Yihang Song
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

import android.util.JsonReader;
import android.util.Log;

import org.sandrop.webscarab.plugin.proxy.SiteData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import edu.uci.calit2.antmonitor.lib.logging.ConnectionValue;
import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor;

/**
 * Adopted from PrivacyGuard: https://bitbucket.org/Near/privacyguard/src
 * @author Anastasia Shuba (ashuba@uci.edu)
 */
class TLSProxyServer extends Thread {
    /** Tag for logging */
    private static final String TAG = TLSProxyServer.class.getSimpleName();

    /** Port at which this server listens */
    public static int port;

    /** SSL port, used to identify HTTPS traffic */
    static final int SSLPort = 443;

    /** VPN Service, used to protect sockets */
    private VpnClient vpnService;

    /** Set of IPs that implement SSL pinning */
    private static final ConcurrentHashMap<String, Object> resolverLock = new ConcurrentHashMap<>();

    // IP one will mirror this one
    static final ConcurrentHashMap<String, Set<String>> pinnedDomains = new ConcurrentHashMap<>();

    /** Holds IPs of hosts that use certificate pinning and other techniques to prevent MITM attack */
/*    static Set<InetAddress> sslPinningIPs = Collections.newSetFromMap(
                                                new ConcurrentHashMap<InetAddress, Boolean>());*/

    /** Host name resolver used for SSL bumping */
    protected static Resolver hostNameResolver;

    /** Socket Factory used for SSL bumping */
    protected static AntSSLSocketFactory sslSocketFactory;

    public TLSProxyServer(VpnClient vpnService) {
        this.vpnService = vpnService;

        //Thread selectorThread = new Thread(selectorRunable, "TLSSelectorThread");
        //selectorThread.start();
    }

    class ForwarderHandler implements Runnable {
        private final String TAG = ForwarderHandler.class.getSimpleName();
        private SocketChannel clientChannel;

        public ForwarderHandler(SocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void run() {
            try {
                Socket client = clientChannel.socket();
                TCPForwarder forwarder = ForwarderManager.mActiveTCPForwarderMap.
                                                                            get(client.getPort());

                InetAddress server = InetAddress.getByAddress(forwarder.mDst.mIpArray);
                String strServer = server.toString().substring(1);

                SiteData newSiteData = new SiteData();
                newSiteData.tcpAddress = strServer;
                newSiteData.destPort = forwarder.mDst.mPort;

                if (ForwarderManager.SSL_SNI_ENABLED && forwarder.mServerName != null
                        && !forwarder.mServerName.isEmpty()) {
                    newSiteData.hostName = forwarder.mServerName;
                    ForwarderManager.Logg.e(TAG, forwarder + " has SNI: " + newSiteData.hostName);
                }

                newSiteData.sourcePort = forwarder.mSrc.mPort;
                newSiteData.name = "";

                // TODO: improve locking mech - connect to mTLSrunnable. No clean way to do it...
                Object lock = resolverLock.get(hostNameResolver.getSecureHostKey(newSiteData));
                if (lock == null) {
                    lock = new Object();
                    resolverLock.put(hostNameResolver.getSecureHostKey(newSiteData), lock);
                }
                synchronized (lock) {
                    //ForwarderManager.Logg.d(TAG, forwarder + " locking on " + newSiteData.tcpAddress);

                    SiteData remoteData = hostNameResolver.getSecureHost(sslSocketFactory, newSiteData, true);

                    if (remoteData != null && remoteData.name != null && !remoteData.name.isEmpty()) {
                        synchronized (forwarder) {
                            forwarder.isInHandshake = true;
                        }

                        SocketChannel targetChannel = SocketChannel.open();
                        Socket target = targetChannel.socket();
                        vpnService.protect(target);
                        targetChannel.connect(new InetSocketAddress(server, forwarder.mDst.mPort));

                        SSLSocket ssl_client = SSLSocketBuilder.negotiateSSL(client, remoteData, false, sslSocketFactory);

                        try {
                            ForwarderManager.Logg.d(TAG, "starting ssl_client handshake: " + forwarder);
                            ssl_client.startHandshake();
                        } catch (IOException e) {
                            ForwarderManager.Logg.e(TAG, "Exception with ssl_client handshake: " + forwarder +", " + e.getMessage(), e);
                        }

                        SSLSession session = ssl_client.getSession();

                        ForwarderManager.Logg.d(TAG, forwarder + " session: " + session.isValid() + " " + strServer);

                        if (session.isValid()) {
                            Socket ssl_target = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(target, strServer, forwarder.mDst.mPort, true);

                            try {
                                ForwarderManager.Logg.d(TAG, "starting ssl_target handshake: " + forwarder);
                                ((SSLSocket) ssl_target).startHandshake();
                            } catch (IOException e) {
                                ForwarderManager.Logg.e(TAG, "Exception with ssl_target handshake: " + forwarder +", " + e.getMessage(), e);
                            }

                            SSLSession tmp_session = ((SSLSocket) ssl_target).getSession();

                            TCPForwarder newFwd = ForwarderManager.mActiveTCPForwarderMap.get(client.getPort());
                            ForwarderManager.Logg.d(TAG, forwarder + " target socket created " + ssl_client.isConnected() + " "
                                + ssl_target.isConnected() + " tmp_ss = " + tmp_session.isValid() + ", newFwd: " + (newFwd != null));

                            if (ssl_client.isConnected() && ssl_target.isConnected() && newFwd != null && newFwd.mClientState == VPNUtils.TCPState.ESTABLISHED) {
                                ForwarderManager.Logg.d(TAG, forwarder + ": about to forward TLS " + remoteData.name);
                                TLSProxyForwarder.connect(ssl_client, ssl_target, forwarder);
                                //Log.d(TAG, forwarder + " Remote Handshake : " + tmp_session + " is valid : " + tmp_session.isValid());

                                synchronized (forwarder) {
                                    forwarder.isInHandshake = false;
                                }

                                return;
                            }

                            ssl_target.close();
                        }

                        // Get app name before destorying TCP connections
                        ConnectionValue cv = PacketProcessor.getInstance(ForwarderManager.mService).
                                getConnValue(forwarder.mSrc.mPort);

                        TCPForwarder newFwd = ForwarderManager.mActiveTCPForwarderMap.get(forwarder.mSrc.mPort);
                        if (newFwd != null) {
                            // Reset the connection so that client knows to try again
                            newFwd.resetAndDestroy();
                        }

                        ssl_client.close();
                        client.close();
                        target.close();

                        // TODO: Ips + Callback
                        //sslPinningIPs.add(server);

                        // Log for our own debugging:
                        ForwarderManager.Logg.d(TAG, "Pinning " + remoteData.name + "; " +
                                forwarder + " for " + cv);

                        if (cv == null) {
                            ForwarderManager.mOutFilter.onDomainAppPin(remoteData.name, null);
                            return;
                        }

                        ForwarderManager.mOutFilter.onDomainAppPin(remoteData.name, cv.getAppName());

                        pinDomainApp(remoteData.name, cv.getAppName());

                    } else {
                        // This shouldn't happen often since TCPForwarder vets this case
                        ForwarderManager.Logg.w(TAG, "Did not try SSL. fwd = " + forwarder);

                        TCPForwarder newFwd = ForwarderManager.mActiveTCPForwarderMap.get(client.getPort());
                        if (newFwd != null) {
                            // Reset the connection so that client knows to try again
                            newFwd.resetAndDestroy();
                        }

                        client.close();
                    }
                }


                //selectorRunable.addFlow(clientChannel, targetChannel);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    @Override
    public void run() {
        // Initialize socket channel
        ServerSocketChannel serverSocketChannel;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().setReuseAddress(true);
            serverSocketChannel.socket().bind(null);
            port = serverSocketChannel.socket().getLocalPort();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }

        // Check if we have been properly initialized
        if (hostNameResolver == null)
            hostNameResolver = new Resolver(vpnService);

        if (sslSocketFactory == null)
            sslSocketFactory = TLSCertificateActivity.generateCACertificate(
                    vpnService.getFilesDir().getPath());

        while (!isInterrupted()) {
            try {
                //Log.d(TAG, "Accepting");
                SocketChannel socketChannel = serverSocketChannel.accept();



                //Socket socket = socketChannel.socket();
                //Log.d(TAG, "Receiving : " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

                //socketChannel.configureBlocking(false);

                TCPForwarder forwarder = ForwarderManager.mActiveTCPForwarderMap.
                        get(socketChannel.socket().getPort());
                synchronized (forwarder) {
                    forwarder.TLSFwdThread = new Thread(new ForwarderHandler(socketChannel));
                    if (forwarder.mServerName != null) {
                        //Log.d(TAG, forwarder + " Starting TLS thread from within Proxy...");
                        forwarder.TLSFwdThread.setName(ForwarderHandler.class.getSimpleName() +
                                "-" + forwarder.mSrc.mPort);
                        forwarder.TLSFwdThread.start();
                    }
                }
                //new Thread(new ForwarderHandler(socketChannel)).start();
                //Log.d(TAG, "Not blocked");
            } catch (Exception e) {
                //Log.e(TAG, e.getMessage(), e);
            }
        }
        //Log.d(TAG, "Stop Listening");
    }

    /**
     * See {@link VpnController#addPinnedDomains(InputStream)} for a description
     * @return {@code true} if the JSON stream was read correctly, and {@code false} otherwise
     */
    static boolean addPinnedCNs(InputStream jsonStream) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(jsonStream, "UTF-8"));
        reader.beginArray();
        while(reader.hasNext()) {
            reader.beginObject();

            String pkgName = null;
            ArrayList<String> domains = null;
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "packageName":
                        pkgName = reader.nextString();
                        break;
                    case "domains":
                        reader.beginArray();
                        domains = new ArrayList<>();
                        while (reader.hasNext()) {
                            domains.add(reader.nextString());
                        }
                        reader.endArray();
                        break;
                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();

            if (pkgName == null || domains == null || domains.isEmpty())
                return false;

            for (String dom : domains)
                pinDomainApp(dom, pkgName);
        }

        reader.endArray();
        return true;
    }

    private static void pinDomainApp(String domain, String packageName) {
        Set<String> appNames = pinnedDomains.get(domain);
        if (appNames == null) {
            appNames = new HashSet<String>(5); // Assuming ~5 apps will block a dom
            pinnedDomains.put(domain, appNames);
        }

        appNames.add(packageName);
    }
}
