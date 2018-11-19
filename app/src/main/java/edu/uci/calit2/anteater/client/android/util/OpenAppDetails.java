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
package edu.uci.calit2.anteater.client.android.util;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import edu.uci.calit2.anteater.client.android.fragment.RealTimeFragment;
import edu.uci.calit2.antmonitor.lib.logging.ConnectionValue;
import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor;
import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor.TrafficType;
import edu.uci.calit2.antmonitor.lib.util.Protocol;

/**
 * Class describing the details of all open applications in the mobile for visualization of data leaks
 * @author Nivedhita Sathyamurthy
 */
public class OpenAppDetails {

    private Context mContext;

    /** Variable to let us know if the user is currently viewing the visualization */
    private static boolean isVisualizationOn = false;

    public OpenAppDetails(Context context) {
        mContext = context;
    }

    public void addTCPConnection(String remoteIp, int srcPort, int destPort) {
        if(isVisualizationOn) {
            ConnectionValue cv = PacketProcessor.getInstance(mContext).getConnValue(srcPort);
            String appName = cv == null ? "No.mapping.after.2.attempts" : cv.getAppName();

            Intent intent = new Intent(RealTimeFragment.ADD_CONNECTION);
            intent.putExtra(RealTimeFragment.PACKAGE_NAME, appName);
            intent.putExtra(RealTimeFragment.DESTINATION_IP, remoteIp);
            intent.putExtra(RealTimeFragment.PACKET_LENGTH, 0);
            intent.putExtra(RealTimeFragment.PROTOCOL_NAME,"TCP");
            intent.putExtra(RealTimeFragment.TRAFFIC_TYPE, RealTimeFragment.OUTGOING);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }
    }

    public void addTCPConnection(String remoteIp, String appName) {
        if (!isVisualizationOn) {
            Intent intent = new Intent(RealTimeFragment.ADD_CONNECTION);
            intent.putExtra(RealTimeFragment.PACKAGE_NAME, appName);
            intent.putExtra(RealTimeFragment.DESTINATION_IP, remoteIp);
            intent.putExtra(RealTimeFragment.PACKET_LENGTH, 0);
            intent.putExtra(RealTimeFragment.PROTOCOL_NAME,"TCP");
            intent.putExtra(RealTimeFragment.TRAFFIC_TYPE, RealTimeFragment.OUTGOING);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }
    }

    public void removeConnection(String remoteIp, int srcPort, int destPort) {
        if (isVisualizationOn) {
            ConnectionValue cv = PacketProcessor.getInstance(mContext).getConnValue(srcPort);
            String appName = cv == null ? "No.mapping.after.2.attempts" : cv.getAppName();

            Intent intent = new Intent(RealTimeFragment.REMOVE_CONNECTION);
            intent.putExtra(RealTimeFragment.PACKAGE_NAME, appName);
            intent.putExtra(RealTimeFragment.DESTINATION_IP,remoteIp);

            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        }
    }

    public static void setIsVisualizationOn(boolean isOn) { isVisualizationOn = isOn; }

}
