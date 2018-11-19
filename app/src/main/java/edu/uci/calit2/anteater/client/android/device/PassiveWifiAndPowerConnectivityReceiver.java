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
package edu.uci.calit2.anteater.client.android.device;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;

import edu.uci.calit2.anteater.client.android.analysis.FileUploadService;
import edu.uci.calit2.antmonitor.lib.logging.TrafficLogFiles;

/**
 * <p>
 *      A receiver that watches the WiFi and power supply connection states.
 *      If both WiFi and power are connected, this receiver checks for completed traffic log files and uploads these by starting a {@link FileUploadService}.
 * </p>
 *
 * <p>
 *      This implementation of {@link WifiAndPowerConnectivityReceiver} is intended for passive use, i.e. declaring the receiver in the manifest.
 *      By registering the receiver in the manifest, traffic log file uploads can be triggered even when this application is not active/running.
 * </p>
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public class PassiveWifiAndPowerConnectivityReceiver extends WifiAndPowerConnectivityReceiver {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPowerAndOnWifi(Context context) {
        // Get the files that are ready for upload.
        File[] filesToUpload = TrafficLogFiles.getCompleted(context);
        for(int i = 0; i < filesToUpload.length; i++) {
            Intent serviceStarter = new Intent(context, FileUploadService.class);
            serviceStarter.putExtra(FileUploadService.EXTRA_FILE, filesToUpload[i].getAbsolutePath());
            // We add a start request for each file.
            // As FileUploadService is an android.app.IntentService, these requests are put into a queue and handled one by one on the background worker thread of the service.
            Log.d(TAG, "Starting upload for: " + filesToUpload[i].getAbsolutePath());
            context.startService(serviceStarter);
        }
    }

}
