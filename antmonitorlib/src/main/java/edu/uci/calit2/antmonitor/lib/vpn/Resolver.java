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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.sandrob.bouncycastle.asn1.ASN1EncodableVector;
import org.sandrob.bouncycastle.asn1.DEREncodableVector;
import org.sandrob.bouncycastle.asn1.x509.GeneralName;
import org.sandrop.webscarab.plugin.proxy.SiteData;
import org.sandroproxy.utils.PreferenceUtils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

/**
 * Retrieves certificates for given hosts
 * Adopted from PrivacyGuard: https://github.com/cryspuwaterloo/privacyguard
 */
class Resolver {
    private VpnClient vpnService;
    private Context mContext;
    private String mHostName;
    private boolean mListenerStarted = false;
    private ConcurrentHashMap<String, String> siteData;
    private ConcurrentHashMap<String, SiteData> ipPortSiteData;

    private static String TAG = Resolver.class.getSimpleName();
    private static boolean LOGD = false;

    public Resolver(VpnClient vpnService) {
        mContext = vpnService;
        this.vpnService = vpnService;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String hostName = pref.getString(PreferenceUtils.proxyTransparentHostNameKey, null);
        if (hostName != null && hostName.length() > 0) {
            mHostName = hostName;
        } else {
            startListenerForEvents();
        }
    }


    private void startListenerForEvents() {
        try {
            siteData = new ConcurrentHashMap<String, String>();
            ipPortSiteData = new ConcurrentHashMap<String, SiteData>();
            mListenerStarted = true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    String getSecureHostKey(SiteData secureHost) {
        return secureHost.tcpAddress + ":" + secureHost.destPort + ":" + secureHost.hostName;
    }

    SiteData getSecureHost(final AntSSLSocketFactory sslFactory, SiteData secureHostInit,
                                   final boolean save) {

        final SiteData secureHost = secureHostInit;

        ForwarderManager.Logg.d(TAG, secureHost.sourcePort +": getSecureHost: " +
                secureHost.tcpAddress + "; " + secureHost.hostName);

        final SiteData matchSecureHost = ipPortSiteData.get(getSecureHostKey(secureHost));
        if (matchSecureHost != null){
            ForwarderManager.Logg.d(TAG, secureHost.sourcePort + "Already have candidate for " + matchSecureHost.name +
                    ". No need to fetch " + matchSecureHost.tcpAddress);
            return matchSecureHost;
        }

        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    try {
                        if (certs != null && certs.length > 0 && certs[0].getSubjectDN() != null) {

                            // getting subject common name
                            String cnValue = certs[0].getSubjectDN().getName();
                            String[] cnValues = cnValue.split(",");
                            for (String val : cnValues) {
                                String[] parts = val.split("=");
                                if (parts.length == 2 && parts[0].equalsIgnoreCase("cn") &&
                                        parts[1] != null && parts[1].length() > 0) {
                                    secureHost.name = parts[1].trim();
                                    secureHost.certs = certs;

                                    ipPortSiteData.put(getSecureHostKey(secureHost), secureHost);

                                    if (LOGD) {
                                        Log.d(TAG, "Adding hostname to dictionary " + secureHost.name
                                                + " srcPort:" + secureHost.sourcePort
                                                + ":" + secureHost.tcpAddress
                                                + ":" + secureHost.destPort
                                                + ": " + (secureHost.hostName != null ? secureHost.hostName : "no host name"));

                                        if (certs != null) {
                                            for (X509Certificate cert : certs) {
                                                X500Principal principal = cert.getIssuerX500Principal();
                                                Log.d(TAG, "checkServerTrusted print whole cert:" + principal);
                                            }
                                        }
                                    }

                                    break;
                                }
                            }

                            Collection<List<?>> coll = certs[0].getSubjectAlternativeNames();
                            if (coll != null && coll.size() > 0) {
                                Iterator<List<?>> iter = coll.iterator();
                                final int SUBALTNAME_DNSNAME = 2;
                                Set<String> altNames = new HashSet<>();
                                while (iter.hasNext()) {
                                    List<?> next = (List<?>) iter.next();
                                    int OID = ((Integer) next.get(0)).intValue();
                                    switch (OID) {
                                        case SUBALTNAME_DNSNAME:
                                            final String dnsName = (String) next.get(1);
                                            //Log.d(TAG, secureHost.name + "- DNS NAME:" + dnsName);
                                            altNames.add(dnsName);
                                            break;
                                    }
                                }

                                sslFactory.domainToAltNames.put(secureHost.name, altNames);
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, e.getMessage(), e);
                    }
                }
            }
        };
        try {
            String hostName = secureHost.hostName != null ? secureHost.hostName : secureHost.tcpAddress;
            SocketChannel socketChannel = SocketChannel.open();
            Socket socket = socketChannel.socket();
            vpnService.protect(socket);
            socketChannel.connect(new InetSocketAddress(hostName, secureHost.destPort));
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory factory = sslContext.getSocketFactory();
            SSLSocket sslsocket = (SSLSocket) factory.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
            sslsocket.setUseClientMode(true);

            if (ForwarderManager.SSL_SNI_ENABLED && secureHost.hostName != null) {
                try {
                        ForwarderManager.Logg.d(TAG, secureHost.sourcePort +
                                " SSL_SNI_ENABLED: attempting to get certs: " + secureHost.hostName + ", ip dest: " + secureHost.tcpAddress);
                        java.lang.reflect.Method setHostnameMethod = sslsocket.getClass().getMethod("setHostname", String.class);
                        setHostnameMethod.invoke(sslsocket, secureHost.hostName);
                    } catch (Exception e) {
                        ForwarderManager.Logg.e(TAG, "SNI not useable", e);
                    }
            }

            sslsocket.getSession();
            sslsocket.close();
        } catch (Exception e) {
            Log.w(TAG, e.getMessage(), e);
            return null;
        }
        return secureHost;
    }

    String getCertCommonName(SiteData secureHost) {

        String commonName = null;

        ForwarderManager.Logg.d(TAG, secureHost.sourcePort + ": getCN: " + secureHost.tcpAddress);

        commonName = siteData.get(secureHost.tcpAddress);
        if (commonName != null){
            ForwarderManager.Logg.d(TAG, secureHost.sourcePort + "Already have candidate for " +
                    secureHost.tcpAddress);
            return commonName;
        }

        try {
            String hostName = secureHost.hostName != null ? secureHost.hostName : secureHost.tcpAddress;
            SocketChannel socketChannel = SocketChannel.open();
            Socket socket = socketChannel.socket();
            vpnService.protect(socket);
            socketChannel.connect(new InetSocketAddress(hostName, secureHost.destPort));
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, new SecureRandom());
            //sslContext.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory factory = sslContext.getSocketFactory();
            SSLSocket sslsocket = (SSLSocket) factory.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
            sslsocket.setUseClientMode(true);

            SSLSession session = sslsocket.getSession();
            commonName = extractCN(session.getPeerPrincipal().getName());
            sslsocket.close();

            ForwarderManager.Logg.d(TAG, secureHost.sourcePort + " cn: " + commonName);
            if (commonName != null)
                siteData.put(secureHost.tcpAddress, commonName);
        } catch (Exception e) {
            ForwarderManager.Logg.e(TAG, e.getMessage(), e);
            return null;
        }
        return commonName;
    }

    private String extractCN(String principalName) {
        String[] cnValues = principalName.split(",");
        for (String val : cnValues) {
            String[] parts = val.split("=");
            if (parts.length == 2 && parts[0].equalsIgnoreCase("cn") &&
                    parts[1] != null && parts[1].length() > 0) {
                return parts[1].trim();
            }
        }
        return null;
    }
}
