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

package edu.uci.calit2.anteater;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
        CopyTest();
    }

    // Simply tests the performance difference between system array copy and bytebuf get.
    public void CopyTest() {
        ByteBuffer packetBuf = ByteBuffer.allocate(2048);
        byte[] destinationArray = new byte[2048];

        Random rand = new Random();
        rand.nextBytes(packetBuf.array());

        // Use get
        long startTime = System.nanoTime();
        for(int i = 0; i < 10000000; i++){
            packetBuf.get(destinationArray);
            packetBuf.position(0);
        }
        long endTime = System.nanoTime();

        Log.d("COPYTEST", "Total Time Get = " + (endTime - startTime));

        // Use system
        byte[] packetArray = new byte[2048];
        rand.nextBytes(packetArray);

        startTime = System.nanoTime();
        for(int i = 0; i < 100000000; i++){
            System.arraycopy(packetArray, 0, destinationArray, 0, packetArray.length);
        }
        endTime = System.nanoTime();
        Log.d("COPYTEST", "Total Time ArrayCopy = " + (endTime - startTime));

    }
}