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
package edu.uci.calit2.antmonitor.lib.vpn;

import org.sandrop.webscarab.plugin.proxy.SiteData;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;


import edu.uci.calit2.antmonitor.lib.logging.ConnectionValue;
import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor;
import edu.uci.calit2.antmonitor.lib.util.IpDatagram;
import edu.uci.calit2.antmonitor.lib.util.Protocol;
import edu.uci.calit2.antmonitor.lib.util.TCPPacket;
import edu.uci.calit2.antmonitor.lib.vpn.VPNUtils.ChangeRequest;
import edu.uci.calit2.antmonitor.lib.vpn.ForwarderManager.Logg;

/**
 * Maintains an external socket connection to send and receive TCP data.
 * One TCP forwarder is created for one TCP connection made by an app.
 *
 * @author Anh Le
 */
class TCPForwarder {
    /** Source address and port number of the flow being routed by this Forwarder */
    VPNUtils.Tuple mSrc;

    /** Destination address and port number of the flow being routed by this Forwarder */
    VPNUtils.Tuple mDst;

    String mServerName = null;
    Thread TLSFwdThread;
    boolean isInHandshake = false;

    InetAddress mServerIP;
    SocketChannel mSocketChannel;
    long mSequenceNumberToClient;
    long mAckNumberToClient;
    long mAckNumberToServer;
    private long mFinSequenceNumberToClient = -1;
    private byte[] mFinToClient;

    private static final long MAX_SEQUENCE_NUMBER = (long) (Math.pow(2, 32) - 1);
    private static final long INITIAL_SEQUENCE_NUMBER = 1;
    private static final long DELAY_DESTROYING_FORWARDER = 100;
    static final long DELAY_SEND_FIN_TO_CLIENT = 100;

    static final int SOCKET_TIMEOUT = 3000;

    private static final String TAG = TCPForwarder.class.getSimpleName();

    VPNUtils.TCPState mClientState = VPNUtils.TCPState.CLOSED;

    VPNUtils.TCPState mServerState = VPNUtils.TCPState.LISTEN;

    private Runnable mCloseConnection = new Runnable() {
        public void run() {
            destroy();
        }
    };

    /**
     * Send FIN packet to client. Required state are:
     * - ESTABLISHED -> FIN_WAIT_1
     * - FIN_WAIT_1 -> FIN_WAIT_1 (Retransmission of FIN)
     * - CLOSE_WAIT -> LAST_ACK (Already received FIN from client first)
     */
    Runnable mSendFinToClient = new Runnable() {
        public void run() {
            // Sanity check
/*                if (mServerState != TCPState.ESTABLISHED && mServerState != TCPState.FIN_WAIT_1 && mServerState != TCPState.CLOSE_WAIT) {
                    Logg.e(TAG, TCPForwarder.this.toString() + " is NOT in an eligible state to send FIN to client. Server state = " + mServerState);
                    return;
                }*/

            synchronized(TCPForwarder.this) {
                if (mFinToClient == null) {
                    mFinToClient = constructTcpIpPacketToClient(false, true, true, false, null, 0, 0);
                    mSequenceNumberToClient++;
                }

                mFinSequenceNumberToClient = mSequenceNumberToClient;

                ForwarderManager.writeDirectToTun(mFinToClient);
                //Logg.e(TAG, "Forwarder " + TCPForwarder.this.toString() +" sent FIN to client.");

                if (mServerState == VPNUtils.TCPState.ESTABLISHED) {
                    mServerState = VPNUtils.TCPState.FIN_WAIT_1;
                } else if (mServerState == VPNUtils.TCPState.CLOSE_WAIT) {
                    mServerState = VPNUtils.TCPState.LAST_ACK;
                } else {
                    // FIN_WAIT_1 stay the same
                }

                try {
                    TCPForwarder.this.mSocketChannel.close();
                } catch(Exception e1) {
                    Logg.e(TAG, "Exception closing TCP Read Socket: " + e1.getMessage());
                }
            }
        }
    };

    private Runnable mTLSConnect = new Runnable() {
        public void run() {
            Socket socket = mSocketChannel.socket();
            String serverIP = mServerIP.toString().substring(1);

            // Get certificate Common Name to be used as the domain
            SiteData newSiteData = new SiteData();
            newSiteData.tcpAddress = serverIP;
            newSiteData.destPort = mDst.mPort;
            newSiteData.sourcePort = mSrc.mPort;
            newSiteData.name = "";

            String certCN = TLSProxyServer.hostNameResolver.getCertCommonName(newSiteData);

            try {
                if (certCN == null || certCN.isEmpty()) {
                    Logg.e(TAG, TCPForwarder.this + ": Skipping TLS: no certs found for remote.");
                    connectDirectly();
                    return;
                }

                if (TLSProxyServer.pinnedDomains.containsKey(certCN)) {
                    // Check if this domain is pinned for this particular app
                    ConnectionValue cv = PacketProcessor.getInstance(ForwarderManager.mService).
                            getConnValue(mSrc.mPort);

                    if (cv == null) {
                        Logg.e(TAG, TCPForwarder.this + ": Skipping TLS: could not get app name.");
                        connectDirectly();
                        return;
                    }

                    if (TLSProxyServer.pinnedDomains.get(certCN).contains(cv.getAppName())) {
                        Logg.d(TAG, TCPForwarder.this + ": Skipping TLS: app = " + cv.getAppName() +
                            "; CN = " + certCN);
                        connectDirectly();
                        return;
                    }
                }

                // Proceed to SSL bumping
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), mSrc.mPort));
                mSocketChannel.connect(new InetSocketAddress(TLSProxyServer.port));

                sendSYNACK();

            } catch (ConnectException ce) {
                // This means there's no internet connectivity - ignore packet
                destroy();
                return;
            } catch (Exception e) {
                Logg.e(TAG, this + " Exception initializing TCP Forwarder: " + e.getMessage(), e);
                resetAndDestroy();
                return;
            }
        }

        private void connectDirectly() throws ConnectException, Exception {
            TCPForwarder.this.mSocketChannel.connect(new InetSocketAddress(
                    TCPForwarder.this.mServerIP, TCPForwarder.this.mDst.mPort));
            sendSYNACK();
        }
    };

    public TCPForwarder(VPNUtils.Tuple src, VPNUtils.Tuple dst) {
        mSrc = src;
        mDst = dst;
        mFinSequenceNumberToClient = -1;
        mFinToClient = null;
    }

    /**
     * Process a packet and adjust CLIENT and SERVER states accordingly.
     * @param packetFromClient the packet to process
     */
    public synchronized void processPacket(byte[] packetFromClient) {
        boolean isSyn = TCPPacket.isSynPacket(packetFromClient);
        boolean isAck = TCPPacket.isAckPacket(packetFromClient);
        boolean isFin = TCPPacket.isFinPacket(packetFromClient);
        boolean isReset = TCPPacket.isResetPacket(packetFromClient);
        boolean hasData = TCPPacket.hasData(packetFromClient);
        long ackNum = TCPPacket.extractTCPv4AckNumber(packetFromClient);
        long seqNum = TCPPacket.extractTCPv4SequenceNumber(packetFromClient);
        int ipHeaderLen = IpDatagram.extractIPv4HeaderLength(packetFromClient);
        int tcpHeaderLen = TCPPacket.extractTCPv4HeaderLength(packetFromClient);
        int dataLen = packetFromClient.length - ipHeaderLen - tcpHeaderLen;

        // keep track of latest ackNum to Server
        if (isAck) mAckNumberToServer = ackNum;

        if (mServerState == VPNUtils.TCPState.LISTEN) {
            // Ignore everything except reset packet
            if (isReset) {
                Logg.e(TAG, this + " received a RESET packet while in LISTEN state.");
                destroy();
                return;
            }

            // Only process SYN packet. Send Reset for other packets that has data or is Fin.
            if (!isSyn) {
                if (dataLen > 0 || isFin) {

                    mSequenceNumberToClient = ackNum;
                    mAckNumberToClient = (seqNum + dataLen) % MAX_SEQUENCE_NUMBER;

                    if (isFin) {
                        mAckNumberToClient = (mAckNumberToClient + 1) % MAX_SEQUENCE_NUMBER;
                        //resetPacketToClient = constructTcpIpPacketToClient(false, true, false, false, null, 0, 0);
                        Logg.e(TAG, this + " in LISTEN state but received FIN packet. DataLen=" + dataLen + ". Sent a FIN-ACK packet");

                        //TODO: duplicate code with isFin
                        // Send ACK packet, then FIN
                        // Skip sending ack here as sendFinToClient will have ACK
                        // So instead of ACK, then FIN, we do FIN-ACK
/*                        mAckNumberToClient = (seqNum + dataLen + 1) % MAX_SEQUENCE_NUMBER;

                        mServerState = VPNUtils.TCPState.CLOSE_WAIT;
                        mClientState = VPNUtils.TCPState.FIN_WAIT_2; // assume perfect channel, client would receive the ACK of his FIN.

                        Logg.d(TAG, this + " got FIN from client. Sending FIN to client. 1");
                        ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mSendFinToClient);
                        ForwarderManager.mForwarderBacklogHandler.postDelayed(mSendFinToClient, 0);*/

                    } else {
                        Logg.e(TAG, this + " in LISTEN state but received a NON-SYN packet. DataLen=" + dataLen + ". Sent a RESET packet");
                    }
                }
                resetAndDestroy();
                return;
            }

            // Discard a possible duplicate SYN
            if (mClientState == VPNUtils.TCPState.SYN_SENT)
                return;

            // Below handle SYN packet
            //Logg.d(TAG, this + " Got SYN. Updating to SYN_SENT");

            mClientState = VPNUtils.TCPState.SYN_SENT;
            try {
                InetAddress server = InetAddress.getByAddress(mDst.mIpArray);
                mServerIP = server;
                InetAddress clientAddr = InetAddress.getByAddress(mSrc.mIpArray);

                mSocketChannel = SocketChannel.open();
                Socket socket = mSocketChannel.socket();
                socket.setSoTimeout(SOCKET_TIMEOUT);
                ForwarderManager.mService.protect(socket);
                mSocketChannel.configureBlocking(false);

                ForwarderManager.mSocketChannelToForwarderMap.put(mSocketChannel, this);
                mSequenceNumberToClient = INITIAL_SEQUENCE_NUMBER;
                mAckNumberToClient = (seqNum + dataLen + 1) % MAX_SEQUENCE_NUMBER;

                // If SSL traffic, forward it to a proxy for SSL bumping
                if (ForwarderManager.SSL_BUMPING_ENABLED && mDst.mPort == TLSProxyServer.SSLPort) {
                    // Continue attempting to connect to TLS Proxy in a separate thread
                    ForwarderManager.mTLSHandler.postDelayed(mTLSConnect, 0);
                } else { // Otherwise, connect to destination directly
                    mSocketChannel.connect(new InetSocketAddress(server, mDst.mPort));

                    // If connect is successful, send a SYN-ACK packet
                    sendSYNACK();
                }

            } catch (ConnectException ce) {
                // This means there's no internet connectivity - ignore packet
                this.destroy();
                return;
            } catch (Exception e) {
                Logg.e(TAG, this + " Exception while intializing a TCP Forwarder: " + e.getMessage(), e);
                resetAndDestroy();
                return;
            }

        } else if (mServerState == VPNUtils.TCPState.SYN_RECEIVED) {
            // Reset connection if packet is reset, ack reset
            if (isReset) {
                Logg.e(TAG, this + " received a RESET packet while in SYN_RECEIVED state.");
                destroy();
                return;
            }

            // Only process ACK packet of our SYNACK. Send Reset for other packets that has data or is Fin.
            if ((!isAck || (isAck && ackNum != INITIAL_SEQUENCE_NUMBER+1)) && (dataLen > 0 || isFin)) {
                mServerState = VPNUtils.TCPState.LISTEN;
                if (!isAck) {
                    Logg.e(TAG, this + " in SYN_RECEIVED state but received a NON-ACK packet.");
                } else {
                    Logg.e(TAG, this + " in SYN_RECEIVED state but received an out-of-order ACK packet.");
                }
                if (mClientState != VPNUtils.TCPState.CLOSED) {
                    mSequenceNumberToClient = ackNum;

                    mAckNumberToClient = (seqNum + dataLen) % MAX_SEQUENCE_NUMBER;
                    if (isFin) {
                        mAckNumberToClient = (mAckNumberToClient + 1) % MAX_SEQUENCE_NUMBER;
                    }

                    byte[] resetPacketToClient;
                    if (isFin) {
                        resetPacketToClient = constructTcpIpPacketToClient(false, true, false, false, null, 0, 0);
                    } else {
                        resetPacketToClient = constructTcpIpPacketToClient(false, true, false, true, null, 0, 0);
                    }

                    ForwarderManager.writeDirectToTun(resetPacketToClient);
                    Logg.e(TAG, this + " in LISTEN state but received a NON-ACK/Out-of-order-ACK packet. DataLen=" + dataLen + " isFin="+isFin + ". Sent a RESET packet");
                }
                return;
            }

            // Received an ACK for our SYN-ACK packet
            mServerState = VPNUtils.TCPState.ESTABLISHED;
            mClientState = VPNUtils.TCPState.ESTABLISHED;
            Logg.d(TAG, this + " Connection established. TLS ? " + (mSocketChannel.socket().getPort() == TLSProxyServer.port));

            if (hasData) {
                forwardData(packetFromClient);
            }

        } else if (mServerState == VPNUtils.TCPState.ESTABLISHED) {
            // Reset connection if packet is reset
            if (isReset) {
                    /* To deal with
                      this, a special control message, reset, has been devised.  If the
                      receiving TCP is in a  non-synchronized state (i.e., SYN-SENT,
                      SYN-RECEIVED), it returns to LISTEN on receiving an acceptable reset.
                      If the TCP is in one of the synchronized states (ESTABLISHED,
                      FIN-WAIT-1, FIN-WAIT-2, CLOSE-WAIT, CLOSING, LAST-ACK, TIME-WAIT), it
                      aborts the connection and informs its user. */
                Logg.e(TAG, this + " received a RESET packet while in ESTABLISHED state");

                if (isInHandshake)
                    return;

                ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mCloseConnection);
                ForwarderManager.mForwarderBacklogHandler.postDelayed(mCloseConnection, 0);
/*                    mServerState = TCPState.LISTEN;
                    mClientState = TCPState.CLOSED;
                    SelectionKey key = mSocketChannel.keyFor(ForwarderManager.mSocketSelector);
                    if (key != null) {
                        key.cancel();
                    }

                    ackData(packetFromClient);*/
                return;
            }

            // Here, we might receive ACK, FIN, DATA but not SYN. Send a Reset if we received SYN.
            if (isSyn) {
                mServerState = VPNUtils.TCPState.LISTEN;
                Logg.e(TAG, this + " in ESTABLISHED state but received a SYN packet.");

                if (mClientState != VPNUtils.TCPState.CLOSED) {
                    mSequenceNumberToClient = INITIAL_SEQUENCE_NUMBER;

                    mAckNumberToClient = (seqNum + dataLen + 1) % MAX_SEQUENCE_NUMBER;
                    byte[] resetPacketToClient = constructTcpIpPacketToClient(false, true, false, true, null, 0, 0);

                    ForwarderManager.writeDirectToTun(resetPacketToClient);
                    Logg.e(TAG, this + " Sent a RESET packet");

                    // Destroy connection because we need to reset everything (ack numbers)
                    ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mCloseConnection);
                    ForwarderManager.mForwarderBacklogHandler.postDelayed(mCloseConnection, 0);
                }
                return;
            }

            // Below handle ACK, FIN, DATA packets
            if (isAck) {
                // Do nothing, assume perfect channel
            }

            if (hasData) {
                forwardData(packetFromClient);
            }

            if (isFin) {
                // Send ACK packet, then FIN
                // Skip sending ack here as sendFinToClient will have ACK
                // So instead of ACK, then FIN, we do FIN-ACK
                mAckNumberToClient = (seqNum + dataLen + 1) % MAX_SEQUENCE_NUMBER;

                //TODO: bug might be here WAIT 1?
                mServerState = VPNUtils.TCPState.CLOSE_WAIT;
                mClientState = VPNUtils.TCPState.FIN_WAIT_2; // assume perfect channel, client would receive the ACK of his FIN.

                Logg.d(TAG, this + " got FIN from client. Sending FIN to client. 2");
                ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mSendFinToClient);
                ForwarderManager.mForwarderBacklogHandler.postDelayed(mSendFinToClient, 0);
            }
        } else if (mServerState == VPNUtils.TCPState.FIN_WAIT_1) {
            // Reset connection if packet is reset
            if (isReset) {
                Logg.e(TAG, this + " received a RESET packet while in FIN_WAIT_1 state");
                ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mCloseConnection);
                ForwarderManager.mForwarderBacklogHandler.postDelayed(mCloseConnection, 0);

                return;
            }

            // Acknowledge if there is data. Don't forward
            if (hasData) {
                ackData(packetFromClient);
            }

            // Only handle ACK of our FIN or FIN. Ignore other packets
            if (isAck && ackNum == mFinSequenceNumberToClient) {
                Logg.i(TAG, this + " Got ACK for FIN we sent to client. isFin = " + isFin);
                mServerState = VPNUtils.TCPState.FIN_WAIT_2;
            }

            if (isFin) {
                // Acknowledge the Fin
                mAckNumberToClient = (seqNum + dataLen + 1) % MAX_SEQUENCE_NUMBER;
                byte[] ackPacketToClient = constructTcpIpPacketToClient(false, true, false, false, null, 0, 0);
                ForwarderManager.writeDirectToTun(ackPacketToClient);
                Logg.e(TAG, this + " Sent an ACK to client's FIN packet 2");

                if (mServerState == VPNUtils.TCPState.FIN_WAIT_1) {
                    Logg.d(TAG, "simultaneous close");
                    mServerState = VPNUtils.TCPState.CLOSING; // simultaneous close
                    // Client State could be FIN_WAIT_1 if he never received the FIN server sent, or LAST_ACK if he does
                    // If server is still in FIN_WAIT_1, it meant client did not send ACK of FIN, implies client has not received FIN
                    mClientState = VPNUtils.TCPState.FIN_WAIT_1;
                } else if (mServerState == VPNUtils.TCPState.FIN_WAIT_2) {
                    Logg.d(TAG, "mServerState == TCPState.FIN_WAIT_2");
                    mServerState = VPNUtils.TCPState.TIME_WAIT;
                    mClientState = VPNUtils.TCPState.LAST_ACK;
                    // Wait for 3 second to destroy the connection
                    ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mCloseConnection);
                    ForwarderManager.mForwarderBacklogHandler.postDelayed(mCloseConnection, DELAY_DESTROYING_FORWARDER);
                }
            }

        } else if (mServerState == VPNUtils.TCPState.FIN_WAIT_2) {
            // Reset connection if packet is reset
            if (isReset) {
                Logg.e(TAG, this + " received a RESET packet while in FIN_WAIT_2 state.");
                ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mCloseConnection);
                ForwarderManager.mForwarderBacklogHandler.postDelayed(mCloseConnection, 0);

                return;
            }

            // Acknowledge if there is data. Don't forward
            if (hasData) {
                Logg.e(TAG, this + " FIN_WAIT_2 has data");
                ackData(packetFromClient);
            }

            // Only handle FIN. Ignore other packets
            if (isFin) {
                // Acknowledge the Fin
                mAckNumberToClient = (seqNum + dataLen + 1) % MAX_SEQUENCE_NUMBER;
                byte[] ackPacketToClient = constructTcpIpPacketToClient(false, true, false, false, null, 0, 0);
                ForwarderManager.writeDirectToTun(ackPacketToClient);
                Logg.e(TAG, this + " Sent an ACK to client's FIN packet 1");

                mServerState = VPNUtils.TCPState.TIME_WAIT;
                mClientState = VPNUtils.TCPState.LAST_ACK;
                // Wait for 3 second to destroy the connection
                ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mCloseConnection);
                ForwarderManager.mForwarderBacklogHandler.postDelayed(mCloseConnection, DELAY_DESTROYING_FORWARDER);
            }
        } else if (mServerState == VPNUtils.TCPState.CLOSING) {
            // Reset connection if packet is reset
            if (isReset) {
                Logg.e(TAG, this + " received a RESET packet while in CLOSING state");
                ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mCloseConnection);
                ForwarderManager.mForwarderBacklogHandler.postDelayed(mCloseConnection, 0);
                return;
            }

            // Acknowledge if there is data. Don't forward
            if (hasData) {
                ackData(packetFromClient);
            }

            // Only handle ACK of our FIN or FIN. Ignore other packets
            if (isAck && ackNum == mFinSequenceNumberToClient) {
                mServerState = VPNUtils.TCPState.TIME_WAIT;
                mClientState = VPNUtils.TCPState.CLOSED;
                // Wait for 3 second to destroy the connection
                ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mCloseConnection);
                ForwarderManager.mForwarderBacklogHandler.postDelayed(mCloseConnection, DELAY_DESTROYING_FORWARDER);
            }

        } else if (mServerState == VPNUtils.TCPState.TIME_WAIT) {
            ackData(packetFromClient);

            // Logg.e(TAG, "Forwarder " + toString() + " received a packet while in TIME_WAIT state.");
        } else if (mServerState == VPNUtils.TCPState.CLOSE_WAIT) {
            if (isReset) {
                Logg.e(TAG, this + " received a RESET packet while in  CLOSE_WAIT state.");
                ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mCloseConnection);
                ForwarderManager.mForwarderBacklogHandler.postDelayed(mCloseConnection, 0);

                return;
            }

            Logg.e(TAG, this + " received a packet in CLOSE_WAIT. isFin=" +
                    isFin + "; isAck=" + isAck + "; isSyn=" + isSyn);
        } else if (mServerState == VPNUtils.TCPState.LAST_ACK) {
            // Reset connection if packet is reset
            if (isReset) {
                Logg.e(TAG, this + " received a RESET packet while in LAST_ACK state");
                ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mCloseConnection);
                ForwarderManager.mForwarderBacklogHandler.postDelayed(mCloseConnection, 0);

                return;
            }

            // Only handle ACK of our FIN or FIN. Ignore other packets
            if (isAck && ackNum == mFinSequenceNumberToClient) {
                mServerState = VPNUtils.TCPState.TIME_WAIT;
                mClientState = VPNUtils.TCPState.CLOSED;
                // Wait for 3 second to destroy the connection
                ForwarderManager.mForwarderBacklogHandler.removeCallbacks(mCloseConnection);
                ForwarderManager.mForwarderBacklogHandler.postDelayed(mCloseConnection, DELAY_DESTROYING_FORWARDER);
            }
        }

    }



    /**
     * Acknowledge data from a client to a server.
     * @param packetFromClient
     */
    private synchronized void ackData(byte[] packetFromClient) {

        int headerLen = IpDatagram.extractIPv4HeaderLength(packetFromClient) + TCPPacket.extractTCPv4HeaderLength(packetFromClient);
        int dataLen = packetFromClient.length - headerLen;
        boolean isFin = TCPPacket.isFinPacket(packetFromClient);
        boolean isSyn = TCPPacket.isSynPacket(packetFromClient);
        boolean isReset = TCPPacket.isResetPacket(packetFromClient);

        // Always ACK right away
        long seqNum = TCPPacket.extractTCPv4SequenceNumber(packetFromClient);
        mAckNumberToClient = (seqNum + dataLen) % MAX_SEQUENCE_NUMBER;
        if (isFin || isSyn || isReset) {
            mAckNumberToClient = (mAckNumberToClient + 1) % MAX_SEQUENCE_NUMBER;
        }
        byte[] packetToClient = constructTcpIpPacketToClient(false, true, false, false, null, 0, 0);
        ForwarderManager.writeDirectToTun(packetToClient);
    }


    /**
     * Get acknowledge number to be sent to client after successfully sending data to a server.
     * @param packetFromClient
     */
    private synchronized long getAckNumber(byte[] packetFromClient) {

        int headerLen = IpDatagram.extractIPv4HeaderLength(packetFromClient) + TCPPacket.extractTCPv4HeaderLength(packetFromClient);
        int dataLen = packetFromClient.length - headerLen;
        boolean isFin = TCPPacket.isFinPacket(packetFromClient);
        boolean isSyn = TCPPacket.isSynPacket(packetFromClient);
        boolean isReset = TCPPacket.isResetPacket(packetFromClient);

        // Always ACK right away
        long seqNum = TCPPacket.extractTCPv4SequenceNumber(packetFromClient);
        long ackNumberToClient = (seqNum + dataLen) % MAX_SEQUENCE_NUMBER;
        if (isFin || isSyn || isReset) {
            ackNumberToClient = (ackNumberToClient + 1) % MAX_SEQUENCE_NUMBER;
        }

        return ackNumberToClient;
    }


    /**
     * Forward data from a client to a server. The client and server TCP states must be both ESTABLISHED.
     * @param packetFromClient
     */
    private void forwardData(byte[] packetFromClient) {


        // Sanity check
        if (mClientState != VPNUtils.TCPState.ESTABLISHED || mServerState != VPNUtils.TCPState.ESTABLISHED) {
            Logg.e(TAG, "Client sent data but connection is not ESTABLISHED yet: client="+mClientState + " server=" + mServerState);
            return;
        }

        int headerLen = IpDatagram.extractIPv4HeaderLength(packetFromClient) + TCPPacket.extractTCPv4HeaderLength(packetFromClient);
        int dataLen = packetFromClient.length - headerLen;

        // Attempt to forward data to the server, if there is a problem, close the connection
        if (dataLen > 0) {
            //Logg.i(TAG, "Forwarding data: " + dataLen);
            // Request to change channel interests and queue the data to write
            synchronized(ForwarderManager.mChangeRequestQueue) {
                ForwarderManager.mChangeRequestQueue.offerLast(new ChangeRequest(mSocketChannel, SelectionKey.OP_WRITE, null));

                synchronized(ForwarderManager.mWriteToNetMap) {
                    LinkedList<VPNUtils.DataWriteToNet> queue = ForwarderManager.mWriteToNetMap.get(mSocketChannel);
                    if (queue == null) {
                        queue = new LinkedList<VPNUtils.DataWriteToNet>();
                        ForwarderManager.mWriteToNetMap.put(mSocketChannel, queue);
                    }

                    // Logg.i(TAG, "Write queue size = " + queue.size());

                    long ackNum = getAckNumber(packetFromClient);
                    VPNUtils.DataWriteToNet writeData = new VPNUtils.DataWriteToNet(null, ackNum, packetFromClient, headerLen, dataLen);

                    queue.add(writeData);

                    // Detect a Client Hello Message to start the TLS ForwarderHandler
                    if (mSocketChannel.socket().getPort() == TLSProxyServer.port &&
                            mServerName == null && // Sometimes we get more than one Client Hello
                            TCPPacket.isClientHello(writeData.mData, writeData.mDataOffset)) {
                        if (ForwarderManager.SSL_SNI_ENABLED) {
                            mServerName = TCPPacket.extractServerNameFromClientHello(
                                                        writeData.mData, writeData.mDataOffset);
                        }

                        ForwarderManager.Logg.d(TAG, "Got server name from Client Hello: " + this);
                        if (TLSFwdThread != null && !TLSFwdThread.isAlive()) {
                            ForwarderManager.Logg.d(TAG, this + ": starting tls thread from TCPForwarder");
                            TLSFwdThread.setName(
                                    TLSProxyServer.ForwarderHandler.class.getSimpleName() + "-" +
                                            mSrc.mPort);
                            TLSFwdThread.start();
                        } else {
                            // We did not go through TLS Proxy Server yet, so
                            // the Proxy will start the TLS ForwarderHandler
                            if (mServerName == null)
                                mServerName = "";
                        }
                    }
                }
            }

            ForwarderManager.mSocketSelector.wakeup();
            //Logg.i(TAG, "Added data to queue. Woke up Selector.");
        }
    }

    /**
     * Construct an IP packet to send to the client. All packets have push flag.
     *
     * @param isSyn
     * @param isAck
     * @param isFin
     * @param data
     * @param dataOffset
     * @param dataLen
     * @return
     */
    public synchronized byte[] constructTcpIpPacketToClient(boolean isSyn, boolean isAck, boolean isFin, boolean isReset, byte[] data, int dataOffset, int dataLen) {
        // Number of bytes for MSS TCP Option and End Option List
        int tcpOptionLen = 0;
        if (isSyn) {
            tcpOptionLen = 8;
        }

        byte[] packetToClient = new byte[IpDatagram.IP_HEADER_DEFAULT_LENGTH  + IpDatagram.TCP_HEADER_DEFAULT_LENGTH + tcpOptionLen + dataLen];

        // First 20 bytes are IP header
        // First byte: Version 4, IP Header Len = 5: 0b01000101
        packetToClient[0] = 0b01000101;

        // Second byte: Differentiated Services: not used

        // 3rd and 4th Byte is total length
        int totalLen = IpDatagram.IP_HEADER_DEFAULT_LENGTH  + IpDatagram.TCP_HEADER_DEFAULT_LENGTH + tcpOptionLen + dataLen;
        packetToClient[2] = (byte) (totalLen >> 8);
        packetToClient[3] = (byte) (totalLen);

        // 5th and 6th Byte is Identification for fragmentation:
        ForwarderManager.mIpIdentification++;
        if (ForwarderManager.mIpIdentification == Short.MAX_VALUE) {
            ForwarderManager.mIpIdentification = 0;
        }
        packetToClient[4] = (byte) (ForwarderManager.mIpIdentification >> 8);
        packetToClient[5] = (byte) (ForwarderManager.mIpIdentification);

        // 7th and 8th are Flags and Fragment offset: not used
        // 9th is TTL. Set to 20
        packetToClient[8] = (byte) 20;
        // 10th is Protocol: TCP = 6
        packetToClient[9] = IpDatagram.TCP;

        // 13, 14, 15, 16 are Source IP
        packetToClient[12] = mDst.mIpArray[0];
        packetToClient[13] = mDst.mIpArray[1];
        packetToClient[14] = mDst.mIpArray[2];
        packetToClient[15] = mDst.mIpArray[3];

        // 17, 18, 19, 20 are Dest IP
        packetToClient[16] = mSrc.mIpArray[0];
        packetToClient[17] = mSrc.mIpArray[1];
        packetToClient[18] = mSrc.mIpArray[2];
        packetToClient[19] = mSrc.mIpArray[3];

        // 11th and 12th are header Check sum
        long checkSum = IpDatagram.calculateIPv4Checksum(packetToClient, 0, IpDatagram.IP_HEADER_DEFAULT_LENGTH);
        packetToClient[10] = (byte) (checkSum >> 8);
        packetToClient[11] = (byte) (checkSum);

        // Next 20 bytes are TCP header
        // 1st and 2nd: Source port
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH] = (byte) (mDst.mPort >> 8);
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+1] = (byte) (mDst.mPort);

        // 3rd and 4th: Destination port
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+2] = (byte) (mSrc.mPort >> 8);
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+3] = (byte) (mSrc.mPort);

        // 5-8: Sequence Number
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+4] = (byte) (mSequenceNumberToClient >> 24);
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+5] = (byte) (mSequenceNumberToClient >> 16);
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+6] = (byte) (mSequenceNumberToClient >> 8);
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+7] = (byte) (mSequenceNumberToClient);

        // 9-12: ACK Number
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+8] = (byte) (mAckNumberToClient >> 24);
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+9] = (byte) (mAckNumberToClient >> 16);
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+10] = (byte) (mAckNumberToClient >> 8);
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+11] = (byte) (mAckNumberToClient);

        // 13th and 14th: Data Offset, Reserved, ECN, Control Bits
        byte offset = (byte) 0b01010000; // 5-words = 20 bytes = TCP header len = data offset
        if (isSyn) { // include option
            offset = (byte) 0b01110000; // 7 words TCP header
        }

        // set syn and ack flag. Last 6 bits: Urg, Ack, Push, Reset, Syn, Fin

        byte pusMask = (byte) 0b00001000;
        byte synMask = (byte) 0b00000010;
        byte ackMask = (byte) 0b00010000;
        byte finMask = (byte) 0b00000001;
        byte rstMask = (byte) 0b00000100;
        byte controlBits = 0;

        if (dataLen > 0) {
            controlBits = (byte) (pusMask | controlBits);
        }
        if (isSyn) {
            controlBits = (byte) (synMask | controlBits);
        }
        if (isAck) {
            controlBits = (byte) (ackMask | controlBits);
        }
        if (isFin) {
            controlBits = (byte) (finMask | controlBits);
        }
        if (isReset) {
            controlBits = (byte) (rstMask | controlBits);
        }
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+12] = offset;
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+13] = controlBits;

        // 15th and 16th: Window
        int maxWindow = 65535;
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+14] = (byte) (maxWindow >> 8);
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+15] = (byte) (maxWindow);

        // 19th and 20th: Urgent Pointer: Not used

        // If this is a SYN packe, set MSS Option using 21-24 bytes.
        // Set it at maximum 65536
        // End option list usign 25rd and 28th bytes
        // http://www.networksorcery.com/enp/protocol/tcp/option002.htm
        //
        if (isSyn) {
            packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+20] = 2;
            packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+21] = 4;
            packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+22] = (byte) 0xEF;
            packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+23] = (byte) 0xFF;

            // 25-28 bytes must be all zeros
        }


        // Fill in the data
        if (data != null) {
            System.arraycopy(data, dataOffset,
                    packetToClient, IpDatagram.IP_HEADER_DEFAULT_LENGTH + IpDatagram.TCP_HEADER_DEFAULT_LENGTH,
                    dataLen);
        }

        // 17th and 18th: Checksum
        // Checksum here is computed with pseudo-ip-header, tcp-header-with-zero-checksum, option, and data
        byte[] pseudoIpHeaderAndTcpHeader = new byte[IpDatagram.IP_HEADER_PSEUDO_LENGTH +
                IpDatagram.TCP_HEADER_DEFAULT_LENGTH + tcpOptionLen];
        // Fill in pseudo-header: srcIP, dstIp, reserved, protocol TCP, TCP segment length
        pseudoIpHeaderAndTcpHeader[0] = mDst.mIpArray[0];
        pseudoIpHeaderAndTcpHeader[1] = mDst.mIpArray[1];
        pseudoIpHeaderAndTcpHeader[2] = mDst.mIpArray[2];
        pseudoIpHeaderAndTcpHeader[3] = mDst.mIpArray[3];
        pseudoIpHeaderAndTcpHeader[4] = mSrc.mIpArray[0];
        pseudoIpHeaderAndTcpHeader[5] = mSrc.mIpArray[1];
        pseudoIpHeaderAndTcpHeader[6] = mSrc.mIpArray[2];
        pseudoIpHeaderAndTcpHeader[7] = mSrc.mIpArray[3];
        pseudoIpHeaderAndTcpHeader[9] = IpDatagram.TCP;
        int totalTCPLen = IpDatagram.TCP_HEADER_DEFAULT_LENGTH + tcpOptionLen + dataLen;
        pseudoIpHeaderAndTcpHeader[10] = (byte) (totalTCPLen >> 8);
        pseudoIpHeaderAndTcpHeader[11] = (byte) totalTCPLen;
        // Copy over TCP header-with-zero-checksum
        System.arraycopy(packetToClient, IpDatagram.IP_HEADER_DEFAULT_LENGTH,
                pseudoIpHeaderAndTcpHeader, IpDatagram.IP_HEADER_PSEUDO_LENGTH, IpDatagram.TCP_HEADER_DEFAULT_LENGTH + tcpOptionLen);

        long tcpCheckSum = TCPPacket.calculateChecksum(pseudoIpHeaderAndTcpHeader, 0, pseudoIpHeaderAndTcpHeader.length, data, dataOffset, dataLen);
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+16] = (byte) (tcpCheckSum >> 8);
        packetToClient[IpDatagram.IP_HEADER_DEFAULT_LENGTH+17] = (byte) (tcpCheckSum);

        return packetToClient;
    }

    /**
     * Send a SYN-ACK packet to client app, just header, no data
     */
    private void sendSYNACK() {
        // Reconstruct TCP header, then IP header, then inject this to TUN interface
        // 16 (optional) + 20 + 20 = 56
        byte[] synAckPacket = constructTcpIpPacketToClient(true, true, false, false, null, 0, 0);

        synchronized (ForwarderManager.mChangeRequestQueue) {
            ForwarderManager.mChangeRequestQueue.offerLast(new ChangeRequest(mSocketChannel,
                    SelectionKey.OP_CONNECT, synAckPacket));
        }
        ForwarderManager.mSocketSelector.wakeup();
    }

    /**
     * Sends RESET packet to client app and destroys this forwarder
     */
    protected void resetAndDestroy() {
        byte[] resetPacket = constructTcpIpPacketToClient(false, true, false, true, null, 0, 0);
        ForwarderManager.writeDirectToTun(resetPacket);
        this.destroy();
    }

    /** Remove this TCPForwarder from active forwarders map and remove the
     * corresponding SocketChannel. */
    public void destroy() {
        String srcIp = IpDatagram.ipv4addressBytesToString(mSrc.mIpArray);
        String dstIp = IpDatagram.ipv4addressBytesToString(mDst.mIpArray);
        ForwarderManager.mOutFilter.onTCPConnectionClosed(srcIp, dstIp, mSrc.mPort, mDst.mPort);

        Logg.e(TAG, "Destroying a TCP forwarder: " + toString());
        // Remove this forwarder
        ForwarderManager.mActiveTCPForwarderMap.remove(this.mSrc.mPort);

        if (mSocketChannel != null) {
            ForwarderManager.mSocketChannelToForwarderMap.remove(mSocketChannel);
            ForwarderManager.mWriteToNetMap.remove(mSocketChannel);
        }

        if (mSocketChannel != null && ForwarderManager.mSocketSelector != null) {
            SelectionKey key = mSocketChannel.keyFor(ForwarderManager.mSocketSelector);
            if (key != null) {
                key.cancel();
            }
        }
        if (mSocketChannel != null) {
            try {
                mSocketChannel.close();
            } catch (Exception e) {
                Logg.e(TAG, "Exception closing socket of a TCPForwarder: " + e.getMessage());
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
        String str = ":" + mSrc.mPort + mServerIP + ":" + mDst.mPort + " : " + (mServerName != null ? mServerName : "");
        //" -> " + mDst.mIpArray[0] + "." + mDst.mIpArray[1] + "." + mDst.mIpArray[2] + "." + mDst.mIpArray[3] + ":" + mDst.mPort;
        return str;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TCPForwarder)) {
            return false;
        }

        TCPForwarder otherTCPForwarder = (TCPForwarder) other;
        return (mSrc.equals(otherTCPForwarder.mSrc) && mDst.equals(otherTCPForwarder.mDst));
    }

}
