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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import edu.uci.calit2.antmonitor.lib.util.IpDatagram;
import edu.uci.calit2.antmonitor.lib.util.Protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Hieu Le
 */
public class IPv4Test {
    // Packet 1 from TCP dump
    private final String IPDatagramTCPTest1 = "45000040a33a40004006d95ea9ea2dee462aa01cfea400501fe29b9200000000b002ffff80870000020405b4010303050101080a257618770000000004020000";
    private final String OFFSET = "10";
    private final int OFFSET_VALUE = 1;

    private byte[] TCP1Bytes;
    private byte[] TCP1BytesWithOffset;

    private ByteBuffer TCP1Buf;
    private ByteBuffer TCP1BufWithOffset;

    @Before
    public void setUp() {

        TCP1Bytes = new BigInteger(IPDatagramTCPTest1, 16).toByteArray();
        TCP1Buf = ByteBuffer.wrap(TCP1Bytes);

        TCP1BytesWithOffset = new BigInteger(OFFSET + IPDatagramTCPTest1, 16).toByteArray();
        TCP1BufWithOffset = ByteBuffer.wrap(TCP1BytesWithOffset);

    }

    @After
    public void tearDown() {
        TCP1Bytes = null;
        TCP1Buf = null;
        TCP1BufWithOffset = null;
        TCP1BytesWithOffset = null;
    }

    @Test
    public void readDestinationIP_ByteBuffer_ReturnsTrue() {
        int beforePosition = TCP1Buf.position();
        String destinationIP = IpDatagram.readDestinationIP(TCP1Buf);
        int afterPosition = TCP1Buf.position();
        assertEquals("70.42.160.28", destinationIP);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readDestinationIP_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCP1BufWithOffset.position();
        String destinationIP = IpDatagram.readDestinationIP(TCP1BufWithOffset, OFFSET_VALUE);
        int afterPosition = TCP1BufWithOffset.position();
        assertEquals("70.42.160.28", destinationIP);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readDestinationIP_ByteArray_ReturnsTrue() {
        String destinationIP = IpDatagram.readDestinationIP(TCP1Bytes);
        assertEquals("70.42.160.28", destinationIP);
    }

    @Test
    public void readDestinationIP_ByteArrayWithOffset_ReturnsTrue() {
        String destinationIP = IpDatagram.readDestinationIP(TCP1BytesWithOffset, OFFSET_VALUE);
        assertEquals("70.42.160.28", destinationIP);
    }

    @Test
    public void extractIPv4DestinationIPArray_ByteArray_ReturnsTrue() {
        String destinationIP = IpDatagram.ipv4addressBytesToString(IpDatagram.extractIPv4DestinationIPArray(TCP1Bytes));
        assertEquals("70.42.160.28", destinationIP);
    }

    @Test
    public void extractIPv4DestinationIPArray_ByteArrayWithOffset_ReturnsTrue() {
        String destinationIP = IpDatagram.ipv4addressBytesToString(IpDatagram.extractIPv4DestinationIPArray(TCP1BytesWithOffset, OFFSET_VALUE));
        assertEquals("70.42.160.28", destinationIP);
    }

    @Test
    public void readSourceIP_ByteBuffer_ReturnsTrue() throws UnknownHostException{
        int beforePosition = TCP1Buf.position();
        String sourceIP = IpDatagram.readSourceIP(TCP1Buf);
        int afterPosition = TCP1Buf.position();
        assertEquals("169.234.45.238", sourceIP);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readSourceIP_ByteBufferWithOffset_ReturnsTrue() throws UnknownHostException{
        int beforePosition = TCP1BufWithOffset.position();
        String sourceIP = IpDatagram.readSourceIP(TCP1BufWithOffset, OFFSET_VALUE);
        int afterPosition = TCP1BufWithOffset.position();
        assertEquals("169.234.45.238", sourceIP);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readSourceIP_ByteArray_ReturnsTrue() {
        String sourceIP = IpDatagram.readSourceIP(TCP1Bytes);
        assertEquals("169.234.45.238", sourceIP);
    }

    @Test
    public void readSourceIP_ByteArrayWithOffset_ReturnsTrue() {
        String sourceIP = IpDatagram.readSourceIP(TCP1BytesWithOffset, OFFSET_VALUE);
        assertEquals("169.234.45.238", sourceIP);
    }

    @Test
    public void extractIPv4SourceIPArray_ByteArray_ReturnsTrue() {
        byte[] sourceIP = IpDatagram.extractIPv4SourceIPArray(TCP1Bytes);
        assertEquals("169.234.45.238", IpDatagram.ipv4addressBytesToString(sourceIP));
    }

    @Test
    public void extractIPv4SourceIPArray_ByteArrayWithOffset_ReturnsTrue() {
        byte[] sourceIP = IpDatagram.extractIPv4SourceIPArray(TCP1BytesWithOffset, OFFSET_VALUE);
        assertEquals("169.234.45.238", IpDatagram.ipv4addressBytesToString(sourceIP));
    }

    @Test
    public void readSourcePort_ByteBuffer_ReturnsTrue() {
        int beforePosition = TCP1Buf.position();
        int sourcePort = IpDatagram.readSourcePort(TCP1Buf);
        int afterPosition = TCP1Buf.position();
        assertEquals(65188, sourcePort);
        assertEquals(beforePosition, afterPosition);    }

    @Test
    public void readSourcePort_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCP1BufWithOffset.position();
        int sourcePort = IpDatagram.readSourcePort(TCP1BufWithOffset, OFFSET_VALUE);
        int afterPosition = TCP1BufWithOffset.position();
        assertEquals(65188, sourcePort);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readSourcePort_ByteArray_ReturnsTrue() {
        int sourcePort = IpDatagram.readSourcePort(TCP1Bytes);
        assertEquals(65188, sourcePort);
    }

    @Test
    public void readSourcePort_ByteArrayWithOffset_ReturnsTrue() {
        int sourcePort = IpDatagram.readSourcePort(TCP1BytesWithOffset, OFFSET_VALUE);
        assertEquals(65188, sourcePort);
    }

    @Test
    public void readDestinationPort_ByteBuffer_ReturnsTrue() {
        int beforePosition = TCP1Buf.position();
        int destinationPort = IpDatagram.readDestinationPort(TCP1Buf);
        int afterPosition = TCP1Buf.position();
        assertEquals(80, destinationPort);
        assertEquals(beforePosition, afterPosition);    }

    @Test
    public void readDestinationPort_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCP1BufWithOffset.position();
        int destinationPort = IpDatagram.readDestinationPort(TCP1BufWithOffset, OFFSET_VALUE);
        int afterPosition = TCP1BufWithOffset.position();
        assertEquals(80, destinationPort);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readDestinationPort_ByteArray_ReturnsTrue() {
        int destinationPort = IpDatagram.readDestinationPort(TCP1Bytes);
        assertEquals(80, destinationPort);
    }

    @Test
    public void readDestinationPort_ByteArrayWithOffset_ReturnsTrue() {
        int destinationPort = IpDatagram.readDestinationPort(TCP1BytesWithOffset, OFFSET_VALUE);
        assertEquals(80, destinationPort);
    }

    @Test
    public void readDestinationPortWithProtocol_ByteBuffer_ReturnsTrue() {
        int beforePosition = TCP1Buf.position();
        int destinationPort = IpDatagram.readDestinationPort(TCP1Buf, IpDatagram.TCP);
        int afterPosition = TCP1Buf.position();
        assertEquals(80, destinationPort);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readDestinationPortWithProtocol_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCP1BufWithOffset.position();
        int destinationPort = IpDatagram.readDestinationPort(TCP1BufWithOffset, IpDatagram.TCP, OFFSET_VALUE);
        int afterPosition = TCP1BufWithOffset.position();
        assertEquals(80, destinationPort);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void isChecksumValid_ByteArray_ReturnsTrue() {
        fail("Not yet implemented");
    }

    @Test
    public void isChecksumValid_ByteBuffer_ReturnsTrue() {
        fail("Not yet implemented");
    }

    @Test
    public void calculateIPv4Checksum_ByteArray_ReturnsTrue() {
        fail("Not yet implemented");
    }

    @Test
    public void calculateIPv4Checksum_ByteArrayWithOffset_ReturnsTrue() {
        fail("Not yet implemented");
    }

    @Test
    public void calculateIPv4Checksum_ByteArrayWithOffsetAndExistingSum_ReturnsTrue() {
        fail("Not yet implemented");
    }

    @Test
    public void readProtocol_ByteBuffer_ReturnsTrue() {
        int beforePosition = TCP1Buf.position();
        byte protocol = IpDatagram.readProtocol(TCP1Buf);
        int afterPosition = TCP1Buf.position();
        assertEquals(IpDatagram.TCP, protocol);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readProtocol_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCP1BufWithOffset.position();
        byte protocol = IpDatagram.readProtocol(TCP1BufWithOffset, OFFSET_VALUE);
        int afterPosition = TCP1BufWithOffset.position();
        assertEquals(IpDatagram.TCP, protocol);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readProtocol_ByteArray_ReturnsTrue() {
        byte protocol = IpDatagram.readProtocol(TCP1Bytes);
        assertEquals(IpDatagram.TCP, protocol);
    }

    @Test
    public void readTransportProtocol_ByteArray_ReturnsTrue() {
        short transportProtocol = IpDatagram.readTransportProtocol(TCP1Bytes);
        assertEquals(IpDatagram.TCP, transportProtocol);
    }

    @Test
    public void readTransportProtocol_ByteArrayWithOffset_ReturnsTrue() {
        short transportProtocol = IpDatagram.readTransportProtocol(TCP1BytesWithOffset, OFFSET_VALUE);
        assertEquals(IpDatagram.TCP, transportProtocol);
    }

    @Test
    public void readTransportProtocol_ByteBuffer_ReturnsTrue() {
        int beforePosition = TCP1Buf.position();
        short transportProtocol = IpDatagram.readTransportProtocol(TCP1Buf);
        int afterPosition = TCP1Buf.position();
        assertEquals(IpDatagram.TCP, transportProtocol);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void isTransportProtocolOfType_ByteArray_ReturnsTrue() {
        assertTrue(IpDatagram.isTransportProtocolOfType(Protocol.TCP, TCP1Bytes));
    }

    @Test
    public void isTransportProtocolOfType_ByteArrayWithOffset_ReturnsTrue() {
        assertTrue(IpDatagram.isTransportProtocolOfType(Protocol.TCP, TCP1BytesWithOffset, OFFSET_VALUE));
    }

    @Test
    public void readIPHeaderLength_ByteBuffer_ReturnsTrue() {
        int beforePosition = TCP1Buf.position();
        byte ipHeaderLength = IpDatagram.readIPHeaderLength(TCP1Buf);
        int afterPosition = TCP1Buf.position();
        assertEquals(5, ipHeaderLength);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readIPHeaderLength_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCP1BufWithOffset.position();
        byte ipHeaderLength = IpDatagram.readIPHeaderLength(TCP1BufWithOffset, OFFSET_VALUE);
        int afterPosition = TCP1BufWithOffset.position();
        assertEquals(5, ipHeaderLength);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readIPHeaderLength_ByteArray_ReturnsTrue() {
        byte ipHeaderLength = IpDatagram.readIPHeaderLength(TCP1Bytes);
        assertEquals(5, ipHeaderLength);
    }

    @Test
    public void readIPHeaderLength_ByteArrayWithOffset_ReturnsTrue() {
        byte ipHeaderLength = IpDatagram.readIPHeaderLength(TCP1BytesWithOffset, OFFSET_VALUE);
        assertEquals(5, ipHeaderLength);
    }

    @Test
    public void extractIPv4HeaderLength_ByteArray_ReturnsTrue() {
        int ipHeaderLength = IpDatagram.extractIPv4HeaderLength(TCP1Bytes);
        assertEquals(20, ipHeaderLength);
    }

    @Test
    public void extractIPv4HeaderLength_ByteArrayWithOffset_ReturnsTrue() {
        int ipHeaderLength = IpDatagram.extractIPv4HeaderLength(TCP1BytesWithOffset, OFFSET_VALUE);
        assertEquals(20, ipHeaderLength);
    }

    @Test
    public void readTransportHeaderLength_ByteBuffer_ReturnsTrue() {
        int beforePosition = TCP1Buf.position();
        short transportHeaderLength = IpDatagram.readTransportHeaderLength(TCP1Buf, IpDatagram.readIPHeaderLength(TCP1Buf), IpDatagram.TCP);
        int afterPosition = TCP1Buf.position();
        assertEquals(11, transportHeaderLength);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readTransportHeaderLength_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCP1BufWithOffset.position();
        short transportHeaderLength = IpDatagram.readTransportHeaderLength(TCP1BufWithOffset, IpDatagram.readIPHeaderLength(TCP1BufWithOffset, OFFSET_VALUE), IpDatagram.TCP, OFFSET_VALUE);
        int afterPosition = TCP1BufWithOffset.position();
        assertEquals(11, transportHeaderLength);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readDatagramLength_ByteBuffer_ReturnsTrue() {
        int beforePosition = TCP1Buf.position();
        int datagramLength = IpDatagram.readDatagramLength(TCP1Buf);
        int afterPosition = TCP1Buf.position();
        assertEquals(64, datagramLength);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readDatagramLength_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCP1BufWithOffset.position();
        int datagramLength = IpDatagram.readDatagramLength(TCP1BufWithOffset, OFFSET_VALUE);
        int afterPosition = TCP1BufWithOffset.position();
        assertEquals(64, datagramLength);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readDatagramLength_ByteArray_ReturnsTrue() {
        int datagramLength = IpDatagram.readDatagramLength(TCP1Bytes);
        assertEquals(64, datagramLength);
    }

    @Test
    public void readIPChecksum_ByteBuffer_ReturnsTrue() {
        int beforePosition = TCP1Buf.position();
        int checksum = IpDatagram.readIPChecksum(TCP1Buf);
        int afterPosition = TCP1Buf.position();
        assertEquals(55646, checksum);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readIPChecksum_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCP1BufWithOffset.position();
        int checksum = IpDatagram.readIPChecksum(TCP1BufWithOffset, OFFSET_VALUE);
        int afterPosition = TCP1BufWithOffset.position();
        assertEquals(55646, checksum);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readIPVersion_ByteBuffer_ReturnsTrue() {
        int beforePosition = TCP1Buf.position();
        byte ipVersion = IpDatagram.readIPVersion(TCP1Buf);
        int afterPosition = TCP1Buf.position();
        assertEquals(IpDatagram.IPV4, ipVersion);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void readIPVersion_ByteBufferWithOffset_ReturnsTrue() {
        int beforePosition = TCP1BufWithOffset.position();
        byte ipVersion = IpDatagram.readIPVersion(TCP1BufWithOffset, OFFSET_VALUE);
        int afterPosition = TCP1BufWithOffset.position();
        assertEquals(IpDatagram.IPV4, ipVersion);
        assertEquals(beforePosition, afterPosition);
    }

    @Test
    public void extractIPVersion_ByteArrayWithOffset_ReturnsTrue() {
        int ipVersion = IpDatagram.extractIPVersion(TCP1BytesWithOffset, OFFSET_VALUE);
        assertEquals(IpDatagram.IPV4, ipVersion);
    }

    @Test
    public void extractIPVersion_ByteArray_ReturnsTrue() {
        int ipVersion = IpDatagram.extractIPVersion(TCP1Bytes);
        assertEquals(IpDatagram.IPV4, ipVersion);
    }

    @Test
    public void extractIPv4SourceIP_ByteArrayWithOffset_ReturnsTrue() {
        int sourceIP = IpDatagram.extractIPv4SourceIP(TCP1BytesWithOffset, OFFSET_VALUE);
        int expectedIP = IpDatagram.convertIPv4IPArrayToInt(IpDatagram.extractIPv4SourceIPArray(TCP1BytesWithOffset, OFFSET_VALUE));
        assertEquals(expectedIP, sourceIP);
    }

    @Test
    public void extractIPv4SourceIP_ByteArray_ReturnsTrue() {
        int sourceIP = IpDatagram.extractIPv4SourceIP(TCP1Bytes);
        int expectedIP = IpDatagram.convertIPv4IPArrayToInt(IpDatagram.extractIPv4SourceIPArray(TCP1Bytes));
        assertEquals(expectedIP, sourceIP);
    }

    @Test
    public void extractIPv4DestinationIP_ByteArrayWithOffset_ReturnsTrue() {
        int destinationIP = IpDatagram.extractIPv4DestinationIP(TCP1BytesWithOffset, OFFSET_VALUE);
        int expectedIP = IpDatagram.convertIPv4IPArrayToInt(IpDatagram.extractIPv4DestinationIPArray(TCP1BytesWithOffset, OFFSET_VALUE));
        assertEquals(expectedIP, destinationIP);
    }

    @Test
    public void extractIPv4DestinationIP_ByteArray_ReturnsTrue() {
        int destinationIP = IpDatagram.extractIPv4DestinationIP(TCP1Bytes);
        int expectedIP = IpDatagram.convertIPv4IPArrayToInt(IpDatagram.extractIPv4DestinationIPArray(TCP1Bytes));
        assertEquals(expectedIP, destinationIP);
    }

    @Test
    public void convertIPv4IPArrayToInt_ByteArray_ReturnsTrue() {
        fail("Not yet implemented");
    }

    @Test
    public void ipv4addressBytesToString_ByteBufferWithOffset_ReturnsTrue() {
        try {
            String ipStr = "255.255.255.255";
            InetAddress address = InetAddress.getByName(ipStr);
            assertEquals(ipStr, IpDatagram.ipv4addressBytesToString(address.getAddress()));

            ipStr = "0.0.0.0";
            address = InetAddress.getByName(ipStr);
            assertEquals(ipStr, IpDatagram.ipv4addressBytesToString(address.getAddress()));

            ipStr = "192.168.0.2";
            address = InetAddress.getByName(ipStr);
            assertEquals(ipStr, IpDatagram.ipv4addressBytesToString(address.getAddress()));

            ipStr = "0.127.255.255";
            address = InetAddress.getByName(ipStr);
            assertEquals(ipStr, IpDatagram.ipv4addressBytesToString(address.getAddress()));

        } catch (UnknownHostException e) {
            // test fails if invalid IP
            assertTrue(false);
        }
    }
}