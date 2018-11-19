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
package edu.uci.calit2.antmonitor.lib;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple class to facilitate writing to DEBUG file for debugging various issues.
 * 
 * @author Simon Langhoff, Janus Varmarken
 */
class DebugFile {
    private static final String DEBUG = "DEBUG";

    public static void AppendToDebugFile(String debugString){
        AppendToDebugFile(debugString, true);
    }

    public static void AppendToDebugFile(String debugString, boolean withTime){
        String root = Environment.getExternalStorageDirectory().toString();
        File dir = new File(root + "/anteater/");
        dir.mkdirs();
        File debugFile = new File(dir, DEBUG);
        FileWriter writer = null;

        try {
            writer = new FileWriter(debugFile, true);

            if(withTime) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS");
                Date dt = new Date();
                String timestamp = sdf.format(dt);
                writer.write(timestamp + " ");
                writer.write("[ " + Thread.currentThread().getName() + "] ");
            }
            writer.write(debugString);
            writer.write("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
