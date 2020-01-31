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
package edu.uci.calit2.anteater.client.android.analysis;

import android.content.Context;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.util.OpenAppDetails;
import edu.uci.calit2.anteater.client.android.util.PreferenceTags;
import edu.uci.calit2.antmonitor.lib.logging.ConnectionValue;
import edu.uci.calit2.antmonitor.lib.logging.PacketConsumer;
import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor.TrafficType;
import edu.uci.calit2.antmonitor.lib.util.IpDatagram;
import edu.uci.calit2.antmonitor.lib.util.PacketDumpInfo;

/**
 * Our implementation of {@link PacketConsumer}. It maps all packets to apps and logs
 * full packets if allowed by the user.
 *
 * @author Anastasia Shuba
 */
public class AMPacketConsumer extends PacketConsumer {

    public static int CONTRIBUTION_PREFS = R.id.low_contribute;

    private final ApplicationFilter appFilter;


    public AMPacketConsumer(Context context, TrafficType trafficType, String userID) {
        super(context, trafficType, userID);

        // Check user preferences to see if they want to log full packets, just headers or nothing
        CONTRIBUTION_PREFS = context.getSharedPreferences(PreferenceTags.PREFS_TAG, Context.MODE_PRIVATE)
                .getInt(PreferenceTags.CONTRIBUTION_PREFS, R.id.low_contribute);

        appFilter = ApplicationFilter.getInstance(context);
    }

    @Override
    protected void consumePacket(PacketDumpInfo packetDumpInfo) {
        byte[] packet = packetDumpInfo.getDump();

        //Keeping the default capture size as zero (because the default contribution level is set to low)
        int captureSize = 0;

        try {
            switch (CONTRIBUTION_PREFS) {
                case R.id.low_contribute:
                    //returning from function as we do not want to log anything per user preferences
                    return;
                case R.id.med_contribute: // logging only headers
                    captureSize = getHeadersSize(packet, IpDatagram.readProtocol(packet));
                    break;
                case R.id.high_contribute: // logging full packets
                    int destPort;
                    if (mTrafficType.equals(TrafficType.OUTGOING_PACKETS))
                        destPort = IpDatagram.readDestinationPort(packet);
                    else
                        destPort = IpDatagram.readSourcePort(packet);

                    captureSize = getFullPacketSize(packet, IpDatagram.readProtocol(packet),
                            destPort);
                    break;
            }
        } catch (IllegalArgumentException e) {
            // Packet was of unknown protocol - can't log
            return;
        }

        packetDumpInfo.setCaptureLength(captureSize);

        ConnectionValue cv = mapPacketToApp(packetDumpInfo);
        if (!(cv.getAppName().startsWith(ConnectionValue.MappingErrors.PREFIX))) {
            // Log app only if user agreed to
            if (!appFilter.getFilteredApps().contains(cv.getAppName())) {
                return;
            }
        }

        String comment = "";
        comment = cv.getAppName();
        if (cv.getVersionNum() != null) {
            comment += "#" + cv.getVersionNum();
        }

        log(packetDumpInfo, comment);
    }

    public static void setPacketLogging(int contribution_prefs) {
        CONTRIBUTION_PREFS = contribution_prefs;
    }
}
