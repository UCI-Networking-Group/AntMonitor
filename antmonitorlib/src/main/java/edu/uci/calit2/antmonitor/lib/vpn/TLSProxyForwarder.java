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

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import edu.uci.calit2.antmonitor.lib.util.TCPReassemblyInfo;


class TLSProxyForwarder implements Runnable {
    private static String TAG = TLSProxyForwarder.class.getSimpleName();

    /** This map keep track of active TLSProxyForwarders to clean them up when connection closes.
     * Map is keyed by inSocket */
    private static ConcurrentMap<Socket, Thread> mActiveThreads = new ConcurrentHashMap<>();

    private boolean outgoing = false;

    private Socket inSocket;
    private Socket outSocket;
    private InputStream in;
    private OutputStream out;

    private TCPForwarder forwarder;

    /**
     * Begins reading/writing from/to given sockets
     * @param clientSocket socket from {@link ForwarderManager} to {@link TLSProxyServer}
     * @param serverSocket socket from {@link TLSProxyServer} to the intended Internet host
     * @param forwarder {@link TCPForwarder} responsbile for the connection inside
     * {@link ForwarderManager}
     * @throws Exception
     */
    public static void connect(Socket clientSocket, Socket serverSocket, TCPForwarder forwarder)
            throws Exception {
        if (clientSocket != null && serverSocket != null && clientSocket.isConnected() &&
                serverSocket.isConnected()) {
            clientSocket.setSoTimeout(TCPForwarder.SOCKET_TIMEOUT);
            serverSocket.setSoTimeout(TCPForwarder.SOCKET_TIMEOUT);
            TLSProxyForwarder clientServer =
                    new TLSProxyForwarder(clientSocket, serverSocket, true, forwarder);
            TLSProxyForwarder serverClient =
                    new TLSProxyForwarder(serverSocket, clientSocket, false, forwarder);

            int currSize = mActiveThreads.size();
            Thread csThread = new Thread(clientServer, "TLSFwd-" + (currSize + 1));
            Thread scThread = new Thread(serverClient, "TLSFwd-" + (currSize + 2));

            mActiveThreads.put(clientSocket, csThread);
            mActiveThreads.put(serverSocket, scThread);
            csThread.start();
            scThread.start();
        } else {
            Log.e(TAG, "skipping socket forwarding because of invalid sockets");
            if (clientSocket != null && clientSocket.isConnected()) {
                clientSocket.close();
            }
            if (serverSocket != null && serverSocket.isConnected()) {
                serverSocket.close();
            }
        }
    }

    public TLSProxyForwarder(Socket inSocket, Socket outSocket, boolean outgoing, TCPForwarder fwd) {
        this.inSocket = inSocket;
        this.outSocket = outSocket;
        try {
            this.in = inSocket.getInputStream();
            this.out = outSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.outgoing = outgoing;
        this.forwarder = fwd;
        //setDaemon(true);
    }

    public void run() {
        try {
            //boolean print = true;

            int got = 0;
            ByteBuffer buffer = ByteBuffer.allocateDirect(ForwarderManager.SOCKET_BYTEBUFFER_WRITE_SIZE);

            while (!Thread.currentThread().isInterrupted() &&
                    (got = in.read(buffer.array(), buffer.arrayOffset(),
                            ForwarderManager.SOCKET_BYTEBUFFER_WRITE_SIZE)) > -1) {
                buffer.limit(got);

                boolean accepted;
                TCPReassemblyInfo tcpInfo;
                if (outgoing) {
                    synchronized (ForwarderManager.mTCPReassemblyMap) {
                        tcpInfo = ForwarderManager.mTCPReassemblyMap.remove(forwarder.mSrc.mPort);
                    }


                    accepted = ForwarderManager.mOutFilter.acceptDecryptedSSLPacket(buffer, tcpInfo).
                            isAllowed();
                } else {
                    synchronized (forwarder) {
                        tcpInfo = new TCPReassemblyInfo(
                                inSocket.getInetAddress().toString().substring(1),
                                forwarder.mSrc.mPort, inSocket.getPort(),
                                forwarder.mAckNumberToClient, forwarder.mSequenceNumberToClient);
                    }

                    accepted = ForwarderManager.mIncFilter.acceptDecryptedSSLPacket(buffer, tcpInfo).
                            isAllowed();
                }

                if (!accepted) {
                    continue;
                }

                out.write(buffer.array(), buffer.arrayOffset(), got);
                out.flush();

/*                Log.e(TAG, outSocket.getLocalPort() + "->" + outSocket.getPort() +
                        "Bytes written to server " + got);
                //Arrays.toString(buffer.array(), 0, 0);
                if (print) {
                    String x = new String(buffer.array(), buffer.arrayOffset(), got);
                    Log.e(TAG, x);
                    print = false;
                }*/


                buffer.position(0);
            }
            //Log.e(TAG, outSocket.getLocalPort() + "->" + outSocket.getPort() + " SocketForwarder stop, got : " + got);
        } catch (Exception e) {
            //Log.e(TAG, e.getMessage(), e);
            //Log.d(TAG, outSocket.getLocalPort() + "->" + outSocket.getPort());
        } finally {
            //Log.e(TAG, outSocket.getLocalPort() + "->" + outSocket.getPort() + " finishing.");

            // Clean-up and remove this thread
            try {
                if (!inSocket.isClosed())
                    inSocket.close();
                if (!outSocket.isClosed())
                    outSocket.close();
            } catch (IOException e) {
                Log.w(TAG, e.getMessage(), e);
            }
            mActiveThreads.remove(inSocket);

            //Log.e(TAG, "fwds = " + mActiveThreads.size());
        }
    }
}
