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