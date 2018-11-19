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

/**
 * <p>
 *  Enum that contains the possible states for the VPN connection.
 * </p>
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public enum VpnState {
    /**
     * The VPN is not connected, and no future connection attempts have been planned.
     */
    DISCONNECTED,
    /**
     * An attempt to connect the VPN is currently being carried out.
     */
    CONNECTING,
    /**
     * The VPN is connected.
     */
    CONNECTED,
    /**
     * A VPN connection attempt has been scheduled to run sometime in the future.
     */
    SCHEDULED_CONNECT
}
