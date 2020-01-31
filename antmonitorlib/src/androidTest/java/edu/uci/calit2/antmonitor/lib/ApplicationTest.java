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

package edu.uci.calit2.antmonitor.lib;

import android.app.Application;
import android.test.ApplicationTestCase;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import edu.uci.calit2.antmonitor.lib.util.AhoCorasickInterface;

/**
 * Runs test cases on the device
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        createApplication();
    }

    public void testAhoCorasick() throws Exception {
        String[] strsOfInterest = {"phonenumber", "zipcode"};
        String packetSimple = "GET /o1cbbfc3/49eec09807_v21_phone.jpg HTTP/1.1\\r\\n";
        String packetOneStr = "GET /" + strsOfInterest[0] + ".jpg HTTP/1.1\\r\\n";
        String packetTwoStr = "GET /" + strsOfInterest[0] + "AND" + strsOfInterest[1] +
                ".jpg HTTP/1.1\\r\\n";
        AhoCorasickInterface.getInstance().init(strsOfInterest);

        ByteBuffer bb = ByteBuffer.allocateDirect(1024 * 16);
        bb.put(packetSimple.getBytes());
        ArrayList<String> foundStrs = AhoCorasickInterface.getInstance().search(bb, bb.position());
        assertEquals(0, foundStrs.size());

        bb.position(0);
        bb.put(packetOneStr.getBytes());
        foundStrs = AhoCorasickInterface.getInstance().search(bb, bb.position());
        assertEquals(2, foundStrs.size());
        assertEquals(strsOfInterest[0], foundStrs.get(0));
        assertEquals(packetOneStr.indexOf(strsOfInterest[0]), Integer.parseInt(foundStrs.get(1)));

        bb.position(0);
        bb.put(packetTwoStr.getBytes());
        foundStrs = AhoCorasickInterface.getInstance().search(bb, bb.position());
        assertEquals(4, foundStrs.size());
        assertEquals(strsOfInterest[0], foundStrs.get(0));
        assertEquals(packetTwoStr.indexOf(strsOfInterest[0]), Integer.parseInt(foundStrs.get(1)));
        assertEquals(strsOfInterest[1], foundStrs.get(2));
        assertEquals(packetTwoStr.indexOf(strsOfInterest[1]), Integer.parseInt(foundStrs.get(3)));
    }
}