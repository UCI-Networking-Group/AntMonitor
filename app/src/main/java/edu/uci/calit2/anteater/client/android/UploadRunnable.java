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
package edu.uci.calit2.anteater.client.android;

import android.content.Context;
import android.util.Log;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.device.Installation;
import edu.uci.calit2.antmonitor.lib.logging.FileUploader;

/**
 * @author Anastasia Shuba
 */
public class UploadRunnable implements Runnable  {

    /** Tag used for logging. */
    private final String TAG = UploadRunnable.class.getSimpleName();

    /** Uploader instance */
    private final FileUploader uploader;

    /** Address of the server to which to upload files */
    private static final String SERVER_ADDRESS = "https://128.195.185.110:9050/";

    /** Server secret */
    public static final String SERVER_SECRET = "02a68622-4680-4013-8e4c-85e137c0fc4e";

    public UploadRunnable (Context context) {
        uploader = new FileUploader(context, R.raw.certificate, SERVER_ADDRESS, SERVER_SECRET,
                                    Installation.id(context));
    }

    @Override
    public void run() {
        int failures = uploader.upload();
        Log.d(TAG, "Upload finished. Number of failed uploads = " + failures);
    }
}
