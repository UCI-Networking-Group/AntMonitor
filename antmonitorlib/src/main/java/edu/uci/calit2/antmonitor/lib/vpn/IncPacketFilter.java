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

import android.content.Context;

import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor;

/**
 * A {@link PacketFilter} designed for inspecting incoming packets.
 *
 * @author Nivedhita Sathyamurthy
 */
public class IncPacketFilter extends PacketFilter {

    /**
     * Constructor that prepares this filter for mapping.
     * Children MUST call super() for proper function of the {@link PacketFilter}!
     *
     * @param context     used for accessing various Android services. Cannot be {@code null}.
     * @throws IllegalStateException if {@code context} is {@code null}.
     */
    public IncPacketFilter(Context context) {
        super(context, PacketProcessor.TrafficType.INCOMING_PACKETS);
    }

}
