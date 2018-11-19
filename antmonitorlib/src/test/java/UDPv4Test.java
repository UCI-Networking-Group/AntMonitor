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
import edu.uci.calit2.antmonitor.lib.util.UDPPacket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Hieu Le
 */
public class UDPv4Test {

    // Wireshark sample UDP packet 450
    private String IPDatagramUDPTest1 = "45000092268e0000011148a9c0a8012cefc0988febc31a73007e2d1342542d534541524348202a20485454502f312e310d0a486f73743a203233392e3139322e3135322e3134333a363737310d0a506f72743a20363838310d0a496e666f686173683a20316566323632366364323536323630326363396362656430353533376232663938393261626533650d0a0d0a0d0a";

    private final String OFFSET = "10";
    private final int OFFSET_VALUE = 1;

    private byte[] UDPBytes;
    private byte[] UDPBytesWithOffset;

    private ByteBuffer UDPBuf;
    private ByteBuffer UDPBufWithOffset;

    @Before
    public void setUp() {

        UDPBytes = new BigInteger(IPDatagramUDPTest1, 16).toByteArray();
        UDPBuf = ByteBuffer.wrap(UDPBytes);

        UDPBytesWithOffset = new BigInteger(OFFSET + IPDatagramUDPTest1, 16).toByteArray();
        UDPBufWithOffset = ByteBuffer.wrap(UDPBytesWithOffset);
    }

    @After
    public void tearDown() {
        UDPBytes = null;
        UDPBuf = null;
        UDPBufWithOffset = null;
        UDPBytesWithOffset = null;
    }

    @Test
    public void readHeader_BufferByte_ReturnsTrue() {
        fail("Not yet implemented");
    }

    @Test
    public void readDestinationPort_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = UDPBufWithOffset.position();
        int destinationPort = UDPPacket.readDestinationPort(UDPBufWithOffset, OFFSET_VALUE, IpDatagram.wordsToBytes(IpDatagram.readIPHeaderLength(UDPBufWithOffset, OFFSET_VALUE)));
        int afterPosition = UDPBufWithOffset.position();
        assertEquals(6771, destinationPort);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readTransportHeaderLength_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = UDPBufWithOffset.position();
        int transportHeaderLength = UDPPacket.readTransportHeaderLength(UDPBufWithOffset, OFFSET_VALUE, IpDatagram.wordsToBytes(IpDatagram.readIPHeaderLength(UDPBufWithOffset, OFFSET_VALUE)));
        int afterPosition = UDPBufWithOffset.position();
        assertEquals(126, transportHeaderLength);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void extractUDPv4DataLength_ByteArrayWithOffset_ReturnsTrue() {
        int dataLength = UDPPacket.extractUDPv4DataLength(UDPBytesWithOffset, OFFSET_VALUE);
        assertEquals(126 - IpDatagram.UDP_HEADER_DEFAULT_LENGTH, dataLength);
    }

    @Test
    public void extractUDPv4DataLength_ByteArray_ReturnsTrue() {
        int dataLength = UDPPacket.extractUDPv4DataLength(UDPBytes);
        assertEquals(126 - IpDatagram.UDP_HEADER_DEFAULT_LENGTH, dataLength);
    }

    @Test
    public void extractUDPv4Txid_ByteArray_ReturnsTrue() {
        int txid = UDPPacket.extractUDPv4Txid(UDPBytes);
        assertEquals(17664, txid);
    }

    @Test
    public void extractUDPv4Data_ByteArrayWithOffset_ReturnsTrue() {
        fail("Not yet implemented");

    }

    @Test
    public void extractUDPv4Data_ByteArray_ReturnsTrue() {
        fail("Not yet implemented");
    }

}
