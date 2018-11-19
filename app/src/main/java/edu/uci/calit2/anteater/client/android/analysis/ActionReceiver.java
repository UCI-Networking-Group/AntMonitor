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
package edu.uci.calit2.anteater.client.android.analysis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.anteater.client.android.util.NotificationsHelper;

/**
 * @author Anastasia Shuba
 */
public class ActionReceiver extends BroadcastReceiver {
    public static final String ACTION_ALLOW = ActionReceiver.class.getPackage().getName() + ".ALLOW";
    public static final String ACTION_DENY = ActionReceiver.class.getPackage().getName() + ".DENY";
    public static final String ACTION_HASH = ActionReceiver.class.getPackage().getName() + ".HASH";

    // skip means the user provided no input, so we do nothing
    public static final String ACTION_SKIP = ActionReceiver.class.getPackage().getName() + ".SKIP";

    public static final String ACTION_DEFAULT = ACTION_HASH;

    public static final String APP = "APP";
    public static final String STRING = "STRING";
    public static final String LABEL = "LABEL";

    public static final String NOTIFICATION_ID = "NOTIFICATION_ID";

    private final String TAG = ActionReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String app = intent.getStringExtra(APP);
        if (app == null) {
            Log.w(TAG, "Invalid intent received.");
            return;
        }

        String pii = intent.getStringExtra(STRING);
        if (pii == null) {
            Log.w(TAG, "Invalid intent received.");
            return;
        }

        int id = intent.getIntExtra(NOTIFICATION_ID, -1);
        if (id == -1) {
            Log.w(TAG, "Invalid intent received.");
            return;
        }

        String label = intent.getStringExtra(ActionReceiver.LABEL);

        // Notify that the user close the notification
        NotificationsHelper.removeLeakNotification(id, app, label);

        // Dismiss notification
        NotificationManagerCompat.from(context).cancel(id);

        // Process user action
        String action = intent.getAction();
        PrivacyDB db = PrivacyDB.getInstance(context);

        if (ACTION_SKIP.equals(action)) {
            // the user has not provided a response to the notification but cleared it instead.
            // so we do nothing
            return;
        }

        String matchedAction = null;
        if (ACTION_ALLOW.equals(action)) {
            matchedAction = ACTION_ALLOW;
        } else if (ACTION_DENY.equals(action)){
            matchedAction = ACTION_DENY;
        } else if (ACTION_HASH.equals(action)){
            matchedAction = ACTION_HASH;
        }

        if (matchedAction != null){
            // ANTMONITOR-144: If we are dealing with locations, then the filter is not going to use the real pii value.
            if (label.equals(PrivacyDB.DEFAULT_PII__LOCATION)) {
                pii = PrivacyDB.DEFAULT_PII_VALUE__LOCATION;
            }
            db.updateOrCreateCustomFilterAsyncTask(context, null, app, matchedAction, pii, label);
        }
    }
}
