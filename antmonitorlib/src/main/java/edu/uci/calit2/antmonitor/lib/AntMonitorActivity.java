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
package edu.uci.calit2.antmonitor.lib;

import edu.uci.calit2.antmonitor.lib.vpn.VpnState;

/**
 * In order to use {@link edu.uci.calit2.antmonitor.lib.vpn.VpnController}, a class must
 * implement this interface. Then, it will receive updates about the status of the VPN
 * through the {@link #onVpnStateChanged(VpnState)} call.
 *
 * @author Anastasia Shuba
 */
public interface AntMonitorActivity {

    /**
     * Called when the state of the VPN connection changes.
     * @param vpnState the new state of the VPN connection.
     */
    void onVpnStateChanged(VpnState vpnState);
}
