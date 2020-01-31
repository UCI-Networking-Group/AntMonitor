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
package edu.uci.calit2.antmonitor.lib.logging;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import java.io.File;

import edu.uci.calit2.antmonitor.lib.logging.PacketProcessor.TrafficType;
import edu.uci.calit2.antmonitor.lib.util.PcapngFile;

/**
 * Manages the state of {@link PcapngFile}s
 *
 * @author Simon Langhoff, Janus Varmarken, Anastasia Shuba
 */
class DumperFileStateManager {
    private static final String TAG = DumperFileStateManager.class.getSimpleName();

    private final Context mContext;

    private Pair<PcapngFile, PcapngFile> files;

    // Split file when size reaches 10mb
    private final long maxFileSize = 10485760;

    public DumperFileStateManager(Context context) {
        mContext = context;
    }

    /**
     * Updates the {@code Pair<PcapngFile, PcapngFile>} referenced by this manager to {@code newFiles}.
     * @param newFiles The new files to be written to in order to store the logs.
     */
    public synchronized void setFiles(Pair<PcapngFile, PcapngFile> newFiles){
        files = newFiles;
    }

    /** Get the file for the specified {@link TrafficType} and create a new one if
     * size limit was reached.
     * @param type
     * @return the file
     */
    public synchronized PcapngFile getExistingFile(TrafficType type){
        PcapngFile file = getFileForType(type);

        // If file is too big, create a new file set
        if (file.getSectionLength() > maxFileSize) {
            endFiles();

            // Create a new file set
            Pair<PcapngFile, PcapngFile> files = TrafficLogFiles.createNewActiveFileSet(mContext);
            setFiles(files);

            return getFileForType(type);
        }

        return file;
    }

    /** Convenience method to get the file for the specified {@link TrafficType}
     * @param type
     * @return the file
     */
    private synchronized PcapngFile getFileForType (TrafficType type){
        PcapngFile file;
        if(type == TrafficType.INCOMING_PACKETS)
            file = files.first;

        else if (type == TrafficType.OUTGOING_PACKETS)
            file = files.second;

        else throw new IllegalStateException("Traffic Type was neither incoming or Outgoing");

        return file;
    }

    /** Marks the two files as complete and clean-up any abandoned stream files (due to crash) */
    private synchronized void endFiles() {
        files.first.renameAndEndFile(TrafficLogFiles.getCompletedFileName(
                files.first.getFile()));
        files.second.renameAndEndFile(TrafficLogFiles.getCompletedFileName(
                files.second.getFile()));

        // Rename any remaining "active" files - they were left this way due to a crash or etc.
        File folder = mContext.getFilesDir();
        for(File f : folder.listFiles()) {
            if (f.getName().startsWith(TrafficLogFiles.ACTIVE_LOG_FILE_PREFIX)) {
                // Calling this renames the file to COMPLETED
                TrafficLogFiles.getCompletedFileName(f);
            }
        }
    }

    /** Mark files as complete and delete them if they are empty */
    public synchronized void finishLogging() {
        endFiles();

        // If no packets have been captured, remove files
        if (files.first.isFileEmpty())
            files.first.getFile().delete();

        if (files.second.isFileEmpty())
            files.second.getFile().delete();
    }
}
