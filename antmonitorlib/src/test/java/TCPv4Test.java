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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import edu.uci.calit2.antmonitor.lib.util.IpDatagram;
import edu.uci.calit2.antmonitor.lib.util.TCPPacket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Hieu Le
 */
public class TCPv4Test {
    private String IPDatagramTCPTest2 = "450000340000400030068ca5462aa01ca9ea2dee0050fea474b307091fe29b938012390841b30000020405b40101040201030307";

    private final String OFFSET = "10";
    private final int OFFSET_VALUE = 1;

    private byte[] TCPBytes;
    private byte[] TCPBytesWithOffset;

    private ByteBuffer TCPBuf;
    private ByteBuffer TCPBufWithOffset;

    @Before
    public void setUp() {

        TCPBytes = new BigInteger(IPDatagramTCPTest2, 16).toByteArray();
        TCPBuf = ByteBuffer.wrap(TCPBytes);

        TCPBytesWithOffset = new BigInteger(OFFSET + IPDatagramTCPTest2, 16).toByteArray();
        TCPBufWithOffset = ByteBuffer.wrap(TCPBytesWithOffset);
    }

    @After
    public void tearDown() {
        TCPBytes = null;
        TCPBuf = null;
        TCPBufWithOffset = null;
        TCPBytesWithOffset = null;
    }
    @Test
    public void readHeader_ByteBuffer_ReturnsTrue() {
        fail("Not yet implemented");
    }

    @Test
    public void readDestinationPort_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCPBufWithOffset.position();
        int destinationPort = TCPPacket.readDestinationPort(TCPBufWithOffset, OFFSET_VALUE, IpDatagram.wordsToBytes(IpDatagram.readIPHeaderLength(TCPBufWithOffset, OFFSET_VALUE)));
        int afterPosition = TCPBufWithOffset.position();
        assertEquals(65188, destinationPort);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readTransportHeaderLength_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCPBufWithOffset.position();
        short transportHeaderLength = TCPPacket.readTransportHeaderLength(TCPBufWithOffset, OFFSET_VALUE, IpDatagram.wordsToBytes(IpDatagram.readIPHeaderLength(TCPBufWithOffset, OFFSET_VALUE)));
        int afterPosition = TCPBufWithOffset.position();
        assertEquals(8, transportHeaderLength);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readHeaderLength_ByteArray_ReturnsTrue() {
        short headerLength = TCPPacket.readHeaderLength(TCPBytes, IpDatagram.readIPHeaderLength(TCPBufWithOffset, OFFSET_VALUE));
        assertEquals(8, headerLength);
    }

    @Test
    public void extractTCPv4SequenceNumber_ByteArrayWithOffset_ReturnsTrue() {
        long sequenceNumber = TCPPacket.extractTCPv4SequenceNumber(TCPBytesWithOffset, OFFSET_VALUE);
        assertEquals(Integer.parseInt("74b30709", 16), sequenceNumber);
    }

    @Test
    public void extractTCPv4SequenceNumber_ByteArray_ReturnsTrue() {
        long sequenceNumber = TCPPacket.extractTCPv4SequenceNumber(TCPBytes);
        assertEquals(Integer.parseInt("74b30709", 16), sequenceNumber);
    }

    @Test
    public void isSynPacket_ByteArrayWithOffset_ReturnsTrue() {
        boolean isSyn = TCPPacket.isSynPacket(TCPBytesWithOffset, OFFSET_VALUE);
        assertEquals(true, isSyn);
    }

    @Test
    public void isSynPacket_ByteArray_ReturnsTrue() {
        boolean isSyn = TCPPacket.isSynPacket(TCPBytes);
        assertEquals(true, isSyn);
    }

    @Test
    public void isResetPacket_ByteArray_ReturnsTrue() {
        boolean isReset = TCPPacket.isResetPacket(TCPBytes);
        assertEquals(false, isReset);
    }

    @Test
    public void isResetPacket_ByteArrayWithOffset_ReturnsTrue() {
        boolean isReset = TCPPacket.isResetPacket(TCPBytesWithOffset, OFFSET_VALUE);
        assertEquals(false, isReset);
    }

    @Test
    public void isFinPacket_ByteArray_ReturnsTrue() {
        boolean isFin = TCPPacket.isFinPacket(TCPBytes);
        assertEquals(false, isFin);
    }

    @Test
    public void isFinPacket_ByteArrayWithOffset_ReturnsTrue() {
        boolean isFin = TCPPacket.isFinPacket(TCPBytesWithOffset, OFFSET_VALUE);
        assertEquals(false, isFin);
    }

    @Test
    public void isAckPacket_ByteArrayWithOffset_ReturnsTrue() {
        boolean isAck = TCPPacket.isAckPacket(TCPBytesWithOffset, OFFSET_VALUE);
        assertEquals(true, isAck);
    }

    @Test
    public void isAckPacket_ByteArray_ReturnsTrue() {
        boolean isAck = TCPPacket.isAckPacket(TCPBytes);
        assertEquals(true, isAck);
    }

    @Test
    public void hasData_ByteArrayWithOffset_ReturnsTrue() {
        boolean hasData = TCPPacket.isAckPacket(TCPBytesWithOffset, OFFSET_VALUE);
        assertEquals(true, hasData);
    }

    @Test
    public void hasData_ByteArray_ReturnsTrue() {
        boolean hasData = TCPPacket.isAckPacket(TCPBytes);
        assertEquals(true, hasData);
    }

    @Test
    public void extractTCPv4AckNumber_ByteArray_ReturnsTrue() {
        long ackNumber = TCPPacket.extractTCPv4AckNumber(TCPBytes);
        assertEquals(534944659, ackNumber);
    }

    @Test
    public void extractTCPv4AckNumber_ByteArrayWithOffset_ReturnsTrue() {
        long ackNumber = TCPPacket.extractTCPv4AckNumber(TCPBytesWithOffset, OFFSET_VALUE);
        assertEquals(534944659, ackNumber);
    }

    @Test
    public void extractTCPv4HeaderLength_ByteArrayWithOffset_ReturnsTrue() {
        int headerLength = TCPPacket.extractTCPv4HeaderLength(TCPBytesWithOffset, OFFSET_VALUE);
        assertEquals(32, headerLength);
    }

    @Test
    public void extractTCPv4HeaderLength_ByteArray_ReturnsTrue() {
        int headerLength = TCPPacket.extractTCPv4HeaderLength(TCPBytes);
        assertEquals(32, headerLength);
    }

    @Test
    public void calculateChecksum_ReturnsTrue() {
        fail("Not yet implemented");
    }
}
