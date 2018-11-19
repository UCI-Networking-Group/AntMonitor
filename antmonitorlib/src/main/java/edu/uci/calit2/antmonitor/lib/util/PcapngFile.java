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
package edu.uci.calit2.antmonitor.lib.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a single PCAPNG file.
 * <p>
 * This implementation of PCAPNG follows the IETF internet-draft from June 26, 2014 by M. Tuxen, et al. Please note
 * that this specification is a work in progress and as such it cannot be guaranteed that the specification has changed
 * since this implementation.
 * For the latest PCAPNG specification consult IETF.org's <a href="https://datatracker.ietf.org/doc/search/?name=pcapng&rfcs=on&activedrafts=on&olddrafts=on&sort=" target="_blank">Data Tracker</a>.
 *</p>
 * <p>
 * The underlying write buffer for the file associated with an instance of this class is guarded by a lock to ensure
 * thread safety when modifying the associated file.
 * </p>
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public class PcapngFile{
    //@GuardedBy("mWriterLock")
    BufferedOutputStream bos;

    // Room for biggest possible packet + 5 Ints and 1 Long
    ByteBuffer data = ByteBuffer.allocate(1024 * 16 + 548).order(ByteOrder.BIG_ENDIAN);

    private File file;

    private RandomAccessFile raf;

    private int limit;

    private long headersOnlyFileSize;

    /**
     * Provides mutually exclusive access to {@link #bos}.
     */
    private final ReentrantLock mWriterLock = new ReentrantLock();

    // SHB fields in order of appearance in file structure
    private int shbType = 0x0A0D0D0A;
    private int shbLength; // length for the SHB
    private int byteOrder = 0x1A2B3C4D;
    private short majorVersion = 1;
    private short minorVersion = 0;
    private long sectionLength = -1; // Length for the whole data section for this block.

    // IDB fields in order for appearance in file structure.
    private int idbType = 0x00000001;
    private int idbLength;
    private short linkType = 101;
    private short reserved = 0;
    private int snapLen = 0; // We store all packets, no matter size.


    // TODO: check IP version

    /**
     * This constructor creates a new file on the filesystem and writes a Section Header Block (SHB) and a Interface Description Block (IDB) to the beginning of the file.
     * Note that specifying an already existing file, will result in the SHB and IDB to be appended to the end of the specified file.
     * @param file The file to write the data to
     * @param headerComment SHB option meant to contain information not captured by other options. eg. session / installation ID.
     * @param hardwareInfo SHB option for capturing information about the hardware on which the packets were captured.
     * @param osInfo SHB option for capturing information about the OS
     * @param appInfo SHB option describing information about the application
     * @param ifName IDB option containing the name of the network interface
     * @param ifDescription IDB option containing a description of the network interface
     * @param ifIPAddr IDB option with the IP address of the interface.
     * @param ifMacAddr IDB option containing the mac address of the network interface
     * @param ifSpeed IDB option containing the speed of the network connection in bps.
     * @param ifTimezone IDB option - current time zone
     * @param ifFilter IDB option - The filter which was used while the packets were captured
     * @param tsOffset IDB option - tsOffset - Timezone offset
     * @param tsRes IDB option - Resolution of the timestamp - default value is 6 which denotes a timescale of 10^6. For milliseconds this value should be 3.
     * @throws FileNotFoundException If the {@code file} does not exist.
     */
    public PcapngFile(File file, String headerComment,  String hardwareInfo, String osInfo, String appInfo, String ifName, String ifDescription,
                      String ifIPAddr, String ifMacAddr, long ifSpeed, int ifTimezone, String ifFilter, long tsOffset, byte tsRes) throws FileNotFoundException {

        this.file = file;
        bos = new BufferedOutputStream(new FileOutputStream(file, true));
        raf = new RandomAccessFile(file, "rw");

        // BEGIN SBH OPTIONS //
        byte[] hardwareBytes = hardwareInfo.getBytes();
        byte[] osBytes = osInfo.getBytes();
        byte[] appBytes = appInfo.getBytes();
        byte[] sessionIDBytes = headerComment.getBytes();


        // Create empty byte arrays with a specific padding for each option string to align it with a 32-bit word.
        byte[] hardwarePadding = new byte[(4 - hardwareBytes.length % 4) % 4];
        byte[] osPadding = new byte[(4 - osBytes.length % 4) % 4];
        byte[] appPadding = new byte[(4 - appBytes.length % 4) % 4];
        byte[] sessionIDPadding = new byte[(4 - sessionIDBytes.length % 4) % 4];

        // Calculate the total length of the options
        int shbOptionsLength = hardwareBytes.length + hardwarePadding.length + 4 +
                osBytes.length + osPadding.length + 4 +
                appBytes.length + appPadding.length + 4 +
                sessionIDBytes.length + sessionIDPadding.length + 4 +
                + 4; // 4 bytes for opt_endofopt;

        // END SBH OPTIONS //
        // calculate SHB length
        shbLength = 28 + shbOptionsLength ;


        // BEGIN IDB OPTIONS //
        // Get string bytes
        byte[] ifNameBytes = ifName.getBytes();
        byte[] ifDescrBytes = ifDescription.getBytes();
        byte[] ifIPAddrBytes = ifIPAddr.getBytes();
        byte[] ifMacAddrBytes = ifMacAddr.getBytes();
        byte[] ifFilterBytes = ifFilter.getBytes();

        // Create empty byte arrays with a specific padding for each option string to align it with a 32-bit word.
        byte[] ifNamePadding = new byte[(4 - ifNameBytes.length % 4) % 4];
        byte[] ifDescrPadding = new byte[(4 - ifDescrBytes.length % 4) % 4];
        byte[] ifIPAddrPadding = new byte[(4 - ifIPAddrBytes.length % 4) % 4];
        byte[] ifMacAddrPadding = new byte[(4 - ifMacAddrBytes.length % 4) % 4];
        byte[] ifFilterPadding = new byte[(4 - ifFilterBytes.length % 4) % 4];


        // Calculate the total length of the options
        int idbOptionsLength = ifNameBytes.length + ifNamePadding.length + 4 + //Options value (variable) + (opt_code / opt_len pair (4))
                ifDescrBytes.length + ifDescrPadding.length + 4 +
                ifIPAddrBytes.length + ifIPAddrPadding.length + 4 +
                ifMacAddrBytes.length + ifMacAddrPadding.length + 4 +
                ifFilterBytes.length + ifFilterPadding.length + 4 +
                + (12 * 2) + 8  + 4 + 8; // 12+12 bytes for ifSpeed + ifOffset, 8 bytes for ifTimezone, 8 bytes for tsRes, 4 bytes for endOpt

        // END IDB OPTIONS //
        // calculate IDB length
        idbLength = 20 + idbOptionsLength ; // IDB non-option fields length + options length

        // Now we know the size of the IDB + SHB, create buffer.
        // ByteBuffer data = ByteBuffer.allocate(idbLength + shbLength).order(ByteOrder.BIG_ENDIAN);
        limit = (idbLength + shbLength);

        // Write SHB
        data.putInt(shbType);
        data.putInt(shbLength);
        data.putInt(byteOrder);
        data.putShort(majorVersion);
        data.putShort(minorVersion);
        data.putLong(sectionLength);

        // write options
        data.putShort((short) 1); // Comment - Session ID
        data.putShort((short) sessionIDBytes.length);
        data.put(sessionIDBytes);
        data.put(sessionIDPadding);


        data.putShort((short) 2); // shb_hardware
        data.putShort((short) hardwareBytes.length); // length without padding
        data.put(hardwareBytes);
        data.put(hardwarePadding);


        data.putShort((short) 3); // shb_os
        data.putShort((short) osBytes.length);
        data.put(osBytes);
        data.put(osPadding);


        data.putShort((short) 4); // shb_userappl
        data.putShort((short) appBytes.length);
        data.put(appBytes);
        data.put(appPadding);


        // END OPTIONS
        data.putShort((short) 0); // opt_endofopt
        data.putShort((short) 0); // opt_length == 0

        data.putInt(shbLength);

        // Write IDB
        data.putInt(idbType);
        data.putInt(idbLength);
        data.putShort(linkType);
        data.putShort(reserved);
        data.putInt(snapLen);

        // Write IDB options
        data.putShort((short) 2); // if_name
        data.putShort((short) ifNameBytes.length); // length without padding
        data.put(ifNameBytes);   // value
        data.put(ifNamePadding); // padding

        data.putShort((short) 3); // if_description
        data.putShort((short) ifDescrBytes.length);
        data.put(ifDescrBytes);
        data.put(ifDescrPadding);

        data.putShort((short) 4); // if_ipAddress //TODO: check IP version
        data.putShort((short) ifIPAddrBytes.length);
        data.put(ifIPAddrBytes);
        data.put(ifIPAddrPadding);

        data.putShort((short) 6); // if_MacAddress
        data.putShort((short) ifMacAddrBytes.length);
        data.put(ifMacAddrBytes);
        data.put(ifMacAddrPadding);

        data.putShort((short) 8); // ifSpeed
        data.putShort((short) 8); // 64 bit
        data.putLong(ifSpeed);

        data.putShort((short) 9); // tsRes
        data.putShort((short) 1);
        data.put(tsRes);
        data.put((byte)0); // Align to 32 bits
        data.put((byte)0);
        data.put((byte)0);

        data.putShort((short) 10); // ifTzone
        data.putShort((short) 4); // only a short value
        data.putInt(ifTimezone);

        data.putShort((short) 11); // ifFilter
        data.putShort((short) ifFilterBytes.length);
        data.put(ifFilterBytes);
        data.put(ifFilterPadding);

        data.putShort((short) 14); // ifOffset
        data.putShort((short) 8);
        data.putLong(tsOffset);

        // END OPTIONS
        data.putShort((short) 0); // opt_endofopt
        data.putShort((short) 0); // opt_length == 0

        // Write length
        data.putInt(idbLength);

        // Update Section Length
        sectionLength = idbLength;

        writeToFile(data);

        headersOnlyFileSize = file.length();
    }

    /**
     * Create an Enhanced Packet Block (EPB) and append it to the current file. This method also updates the section length 32 bit word of the previous SHB with the added length of this packet block.
     * @param timestamp The timestamp for when the packet was captured. This should adhere to the format specified in the IDB.
     * @param capturedLength The number of bytes captured from the packet. (padding not included).
     * @param originalLength The length of the original packet as it was sent on the wire.
     * @param packet The packet
     * @param comment a comment about the packet captured. An example hereof could the name of the application from which the packet was intercepted.
     */
    public void appendEnhancedPacketBlock(long timestamp, int capturedLength, int originalLength, byte[] packet, String comment){
        int interfaceID = 0; // TODO: right now only one interface is supported

        byte[] packetDataPadding = new byte[(4 - capturedLength % 4) % 4]; // padding for packet

        byte[] commentBytes = comment.getBytes();

        // Create empty byte arrays with a specific padding for each option string to align it with a 32-bit word.
        byte[] commentPadding = new byte[(4 - commentBytes.length % 4) % 4];

        int idbOptionsLength = 4 + commentBytes.length + commentPadding.length + 4; // 4 bytes for comment option header + 4 bytes for end_opt.

        //Calculate total length
        int epbLength = 32 + capturedLength + packetDataPadding.length + idbOptionsLength;

//        ByteBuffer data = ByteBuffer.allocate(epbLength).order(ByteOrder.BIG_ENDIAN);
        data.position(0);
        limit = epbLength;

        // Write EPB
        data.putInt(0x00000006); //blocktype
        data.putInt(epbLength);
        data.putInt(interfaceID);
        data.putLong(timestamp);
        data.putInt(capturedLength);
        data.putInt(originalLength);
        data.put(packet, 0, capturedLength);
        data.put(packetDataPadding);


        // Write Options
        // TODO: add comment to each packet?
        data.putShort((short)1); // Comment
        data.putShort((short) commentBytes.length); // comment length without padding
        data.put(commentBytes);
        data.put(commentPadding);


        // END OPTIONS
        data.putShort((short) 0); // opt_endofopt
        data.putShort((short) 0); // opt_length == 0

        data.putInt(epbLength);

        // Update section length
        sectionLength = epbLength + sectionLength;

        // Write to file
        writeToFile(data);
    }

    private void overwriteSectionLength(long sectionLength){
        // Update the section length block with the new length.
        // Note: with this approach we cannot merge multiple section header blocks into one pcapfile,
        // then extra care would be needed to correctly override the section length for the correct SHB
        try {
            raf.seek(16);
            raf.writeLong(sectionLength);
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void writeToFile(ByteBuffer data){
        mWriterLock.lock();
        try {
            bos.write(data.array(), 0, limit);
            bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mWriterLock.unlock();
        }
    }

    /**
     * A getter for the file that currently represents this PCAPNG file.
     * @return the file.
     */
    public File getFile(){
        return this.file;
    }

    /**
     * Closes the underlying {@link FileOutputStream} and renames the file to that of the specified file
     * @param newFile a file containing the name/path of the final file.
     */
    public void renameAndEndFile(File newFile){
        overwriteSectionLength(sectionLength);

        mWriterLock.lock();
        file.renameTo(newFile);
        try {
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mWriterLock.unlock();
        }
    }

    /**
     * Determines whether or not this file contains any packets.
     * In case there is only a SHB and IDB defined (or none), this will return true
     * @return True if no EHB blocks has been appended. Otherwise false.
     */
    public boolean isFileEmpty(){
        return file.length() <= this.headersOnlyFileSize;
    }

    public long getSectionLength(){
        return sectionLength;
    }
}
