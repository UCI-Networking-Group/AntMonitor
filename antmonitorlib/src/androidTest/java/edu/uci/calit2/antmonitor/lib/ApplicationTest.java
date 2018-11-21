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
        assertEquals(foundStrs.get(0), strsOfInterest[0]);
        assertEquals(Integer.parseInt(foundStrs.get(1)),
                packetOneStr.indexOf(strsOfInterest[0]) + (strsOfInterest[0].length() - 1));

        bb.position(0);
        bb.put(packetTwoStr.getBytes());
        foundStrs = AhoCorasickInterface.getInstance().search(bb, bb.position());
        assertEquals(4, foundStrs.size());
        assertEquals(foundStrs.get(0), strsOfInterest[0]);
        assertEquals(Integer.parseInt(foundStrs.get(1)),
                packetTwoStr.indexOf(strsOfInterest[0]) + (strsOfInterest[0].length() - 1));
        assertEquals(foundStrs.get(2), strsOfInterest[1]);
        assertEquals(Integer.parseInt(foundStrs.get(3)),
                packetTwoStr.indexOf(strsOfInterest[1]) + (strsOfInterest[1].length() - 1));
    }
}