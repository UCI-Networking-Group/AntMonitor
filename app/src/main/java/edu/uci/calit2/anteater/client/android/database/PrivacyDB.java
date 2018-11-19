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
package edu.uci.calit2.anteater.client.android.database;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Patterns;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import edu.uci.calit2.anteater.client.android.activity.uielements.PrivacyLeaksReportCursorLoader;
import edu.uci.calit2.anteater.client.android.analysis.ActionReceiver;
import edu.uci.calit2.anteater.client.android.device.DeviceUtils;

/**
 * @author Anastasia Shuba
 */
public class PrivacyDB {

    private static final String TAG = "PrivacyDB";

    // used for intent filters
    public static final String DB_LEAK_CHANGED = "DB_LEAK_CHANGED";
    public static final String DB_FILTER_CHANGED = "DB_FILTER_CHANGED";

    private static final String DATABASE_NAME = "ANTMONITOR_DATABASE";

    /** Keeps filters */
    private static final String TABLE_FILTERS = "TABLE_FILTERS";

    /** Keeps history of leaks */
    private static final String TABLE_HISTORY = "TABLE_HISTORY";

    /** Keeps history of contributions */
    private static final String TABLE_LOGS = "TABLE_LOGS";

    /* ****** COLUMN NAMES PERTAINING TO ALL TABLES ****** */
    public static final String COLUMN_ID = "_id";

    /* ****** COLUMN NAMES PERTAINING TO {@link #TABLE_FILTERS} and {@link #TABLE_HISTORY} ****** */
    public static final String COLUMN_APP = "appname";
    public static final String COLUMN_PII_VALUE = "pii";
    public static final String COLUMN_PII_LABEL = "label";
    public static final String COLUMN_ACTION = "action";
    public static final String COLUMN_PII_LABEL__DEFAULT = "Unlabeled";
    public static final String COLUMN_REMOTE_IP = "remoteIp";


    /** Used in {@link #TABLE_FILTERS} to indicate whether or not user wants to search for this string */
    public static final String COLUMN_SEARCH_ENABLED = "search_enabled";
    /** Used in {@link #TABLE_FILTERS} to indicate whether or not this is a custom filter */
    public static final String COLUMN_IS_CUSTOM = "is_custom";

    /** Used in {@link #TABLE_HISTORY} to indicate when the leak occured and in {@link #TABLE_LOGS} to indicate when the uploads happened */
    public static final String COLUMN_TIME = "timestampt";

    /** Used in {@link #TABLE_LOGS} to indicate how many log files were uploaded */
    public static final String COLUMN_COUNT = "count";

    // Labels for the default STATIC PIIs used in {@link #TABLE_FILTERS}
    public static final String DEFAULT_PII__IMEI = "IMEI";
    public static final String DEFAULT_PII__DEVICE_ID = "Device ID";
    public static final String DEFAULT_PII__PHONE_NUMBER = "Phone Number";
    public static final String DEFAULT_PII__EMAIL = "Email";
    public static final String DEFAULT_PII__ADVERTISER_ID = "Advertiser ID";
    public static final String DEFAULT_PII__SERIAL_NUMBER = "Serial Number";
    public static final String DEFAULT_PII__ICC_ID = "ICC ID";
    public static final String DEFAULT_PII__IMSI = "IMSI";
    public static final String DEFAULT_PII__MAC_ADDRESS = "MAC Address";

    // Labels for the default DYNAMIC PIIs used in {@link #TABLE_FILTERS}
    public static final String DEFAULT_PII__LOCATION = "Location";
    public static final String DEFAULT_PII_VALUE__LOCATION = "DEFAULT_PII_VALUE__LOCATION";


    public static final int ENABLED_ATTRIBUTE_VALUE = 1;
    public static final int DISABLED_ATTRIBUTE_VALUE = 0;

    private static PrivacyDB instance;
    private static final int DATABASE_VERSION = 3;
    private SQLHandler sqlHandler;
    private SQLiteDatabase _database;

    private boolean hasDefaultGlobalFilters = false;


    private static class SQLHandler extends SQLiteOpenHelper {

        public SQLHandler(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /** Called when database is first created */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_FILTERS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_APP + " TEXT, "
                    + COLUMN_PII_VALUE + " TEXT NOT NULL, "
                    + COLUMN_PII_LABEL + " TEXT NOT NULL, "
                    + COLUMN_IS_CUSTOM + " INTEGER DEFAULT 0, "
                    + COLUMN_ACTION + " TEXT NOT NULL, "
                    + COLUMN_SEARCH_ENABLED + " INTEGER DEFAULT 0);");

            db.execSQL("CREATE TABLE " + TABLE_HISTORY + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_APP + " TEXT NOT NULL, "
                    + COLUMN_PII_VALUE + " TEXT NOT NULL, "
                    + COLUMN_PII_LABEL + " TEXT NOT NULL, "
                    + COLUMN_ACTION + " TEXT NOT NULL, "
                    + COLUMN_REMOTE_IP + " TEXT NOT NULL, "
                    + COLUMN_TIME + " INTEGER DEFAULT 0);");

            db.execSQL("CREATE TABLE " + TABLE_LOGS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_TIME + " INTEGER DEFAULT 0, "
                    + COLUMN_COUNT + " INTEGER DEFAULT 0);");
        }

        /** If database exists, this method will be called */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Remove legacy table if it exists
            db.execSQL("DROP TABLE IF EXISTS PRIVACY_TABLE");

            // Upgrade new tables
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FILTERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
            onCreate(db);
        }

    }

    /** Database constructor */
    public PrivacyDB(Context c) {
        sqlHandler = new SQLHandler(c);
        hasDefaultGlobalFilters = false;
    }

    /**Singleton getter.
     * @param c context used to open the database
     * @return The current instance of PrivacyDB, if none, a new instance is created.
     * After calling this method, the database is open for writing. */
    public static PrivacyDB getInstance(Context c)
    {
        if (instance == null)
            instance = new PrivacyDB(c);

        if (instance._database == null) {
            instance._database = instance.sqlHandler.getWritableDatabase();
        }

        if (!instance.hasDefaultFilters(c))
            instance.setDefaultValues(c);

        return instance;
    }

    public SQLiteDatabase getDatabase() {
        if (this.isClose()) {
            _database = sqlHandler.getWritableDatabase();
        }
        return _database;
    }

    private synchronized boolean hasDefaultFilters(Context context) {
        if (!hasDefaultGlobalFilters) {
            Cursor cursor = getGlobalPrivacyFilterByLabel(PrivacyDB.DEFAULT_PII__LOCATION);
            if (cursor != null && cursor.getCount() > 0) {
                hasDefaultGlobalFilters = true;
            }
        }

        return hasDefaultGlobalFilters;
    }

    /**Sets default values for any of our tables upon creation
     * @param context context used to open the database
     **/
    public synchronized void setDefaultValues(Context context) {

        final boolean isCustom = false;
        final boolean isSearchable = true;

        // check to see if table for privacy filters is empty first, else return
        Cursor cursor = getPrivacyFilters();
        if (cursor != null && cursor.getCount() >0 ) {
            cursor.close();
            return;
        }

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        // Set deviceId
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            addGlobalFilter(deviceId, PrivacyDB.DEFAULT_PII__DEVICE_ID, isCustom, ActionReceiver.ACTION_DEFAULT, isSearchable);
        }

        // Set IMEI
        String imei = telephonyManager.getDeviceId();
        if (imei != null && !imei.trim().isEmpty()) {
            addGlobalFilter(imei, PrivacyDB.DEFAULT_PII__IMEI, isCustom, ActionReceiver.ACTION_DEFAULT, isSearchable);
        }

        // Set PhoneNumber
        String phoneNumber = telephonyManager.getLine1Number();
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            addGlobalFilter(phoneNumber, PrivacyDB.DEFAULT_PII__PHONE_NUMBER, isCustom, ActionReceiver.ACTION_DEFAULT, isSearchable);
        }

        // Set email
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                // Avoid duplicates in case user has multiple of the same account
                if (account.name != null && !account.name.trim().isEmpty()) {
                    addGlobalFilter(account.name, PrivacyDB.DEFAULT_PII__EMAIL, isCustom, ActionReceiver.ACTION_DEFAULT, isSearchable);
                    break;
                }
            }
        }

        // add location (value has default to find the filter easily but the value will not be used)
        // default to ALLOW for location
        addGlobalFilter(PrivacyDB.DEFAULT_PII_VALUE__LOCATION, PrivacyDB.DEFAULT_PII__LOCATION, isCustom, ActionReceiver.ACTION_ALLOW, isSearchable);

        // set Advertiser ID
        AdvertiserIDTask task = new AdvertiserIDTask(context, isCustom, ActionReceiver.ACTION_DEFAULT, isSearchable);
        task.execute();

        // set Serial Number
        // note this is available for API 9+
        if (Build.SERIAL != null) {
            addGlobalFilter(Build.SERIAL, PrivacyDB.DEFAULT_PII__SERIAL_NUMBER, isCustom, ActionReceiver.ACTION_DEFAULT, isSearchable);
        }

        // set ICC ID
        // if more than lollipop, then use Subscription Manager, else use Telephony Manager
        // see http://stackoverflow.com/questions/9751823/how-can-i-get-the-iccid-number-of-the-phone
        if ( android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            SubscriptionManager sm = SubscriptionManager.from(context);
            List<SubscriptionInfo> activeSubInfoList = sm.getActiveSubscriptionInfoList();
            if (activeSubInfoList != null) {
                int counter = 1;
                for (SubscriptionInfo subInfo : activeSubInfoList) {
                    String iccID = subInfo.getIccId();
                    if (iccID != null && !iccID.trim().isEmpty()) {
                        addGlobalFilter(iccID, PrivacyDB.DEFAULT_PII__ICC_ID + " " + counter, isCustom, ActionReceiver.ACTION_DEFAULT, isSearchable);
                        counter++;
                    }
                }
            }
        } else {
            String iccID = telephonyManager.getSimSerialNumber();
            if (iccID != null && !iccID.trim().isEmpty()) {
                addGlobalFilter(iccID, PrivacyDB.DEFAULT_PII__ICC_ID, isCustom, ActionReceiver.ACTION_DEFAULT, isSearchable);
            }
        }

        // set IMSI
        String imsi = telephonyManager.getSubscriberId();
        if (imsi != null && !imsi.trim().isEmpty()) {
            addGlobalFilter(imsi, PrivacyDB.DEFAULT_PII__IMSI, isCustom, ActionReceiver.ACTION_DEFAULT, isSearchable);
        }

        // set macaddr
        updateMacAddrAsyncTask(context, null);

    }

    public synchronized String[] printTable(String table) {
        Cursor c =  getDatabase().rawQuery("SELECT * FROM " + table, null);
        if(c.getCount() <= 0) {
            c.close();
            Log.d(TAG, "null");
            return null;
        }

        String[] names = new String[c.getCount()];
        Log.d(TAG, Arrays.toString(c.getColumnNames()));

        int i = 0;
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {

            String toPrint = "";
            for(int j=0; j < c.getColumnCount(); j++)
                toPrint += c.getString(j) + ", ";
            Log.d(TAG, toPrint);
            names[i] = c.getString(0);
            i++;
        }
        c.close();
        return names;
    }

    public String[] printLeaks() {
        Cursor c =  getDatabase().query(TABLE_HISTORY, new String[] {
                        COLUMN_ID, COLUMN_APP, COLUMN_REMOTE_IP},
                null, null, null, null,
                COLUMN_TIME + " DESC");
        if(c.getCount() <= 0) {
            c.close();
            Log.d(TAG, "null");
            return null;
        }

        String[] leaks = new String[c.getCount()];
        Log.d(TAG, Arrays.toString(c.getColumnNames()));

        int i = 0;
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {

            String toPrint = "";
            for (int j = 0; j < c.getColumnCount(); j++)
                toPrint += c.getString(j) + ", ";
            //Log.d(TAG, toPrint);

            leaks[i] = toPrint;
            i++;
        }
        c.close();
        return leaks;
    }

    /**
     * Inserts a new filter for the given parameters
     * @param piiValue the leaking string
     * @param piiLabel {@code piiValue}'s label
     * @param isCustom indicates whether or not this is a custom filter
     * @param action the action to be taken
     * @param isSearchEnabled indicates whether or not user wants to search for this pii
     * @return the row ID of the updated row, or -1 if an error occurred
     */
    private synchronized long addGlobalFilter(String piiValue, String piiLabel, boolean isCustom,
                                             String action, boolean isSearchEnabled) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_PII_VALUE, piiValue);
        cv.put(COLUMN_PII_LABEL, piiLabel);
        cv.put(COLUMN_IS_CUSTOM, isCustom ? ENABLED_ATTRIBUTE_VALUE : DISABLED_ATTRIBUTE_VALUE);
        cv.put(COLUMN_ACTION, action);
        cv.put(COLUMN_SEARCH_ENABLED, isSearchEnabled ? ENABLED_ATTRIBUTE_VALUE : DISABLED_ATTRIBUTE_VALUE);

        return getDatabase().insert(TABLE_FILTERS, null, cv);
    }

    /**
     * Inserts a new filter for the given parameters
     * @param packageName the application package
     * @param piiValue the leaking string
     * @param piiLabel {@code piiValue}'s label
     * @param isCustom indicates whether or not this is a custom filter
     * @param action the action to be taken
     * @param isSearchEnabled indicates whether or not user wants to search for this pii
     * @return the row ID of the updated row, or -1 if an error occurred
     */
    private synchronized long addApplicationFilter(String packageName, String piiValue, String piiLabel, boolean isCustom,
                                             String action, boolean isSearchEnabled) {
        ContentValues cv = new ContentValues();
        String label = piiLabel != null ? piiLabel : COLUMN_PII_LABEL__DEFAULT;

        // if this is a location filter, then we will change the value to something else since location is dynamic
        String value = piiValue;
        if (label.equals(PrivacyDB.DEFAULT_PII__LOCATION)) {
            value = PrivacyDB.DEFAULT_PII_VALUE__LOCATION;
        }

        cv.put(COLUMN_APP, packageName);
        cv.put(COLUMN_PII_VALUE, value);
        cv.put(COLUMN_PII_LABEL,label);
        cv.put(COLUMN_IS_CUSTOM, isCustom ? ENABLED_ATTRIBUTE_VALUE : DISABLED_ATTRIBUTE_VALUE);
        cv.put(COLUMN_ACTION, action);
        cv.put(COLUMN_SEARCH_ENABLED, isSearchEnabled ? ENABLED_ATTRIBUTE_VALUE : DISABLED_ATTRIBUTE_VALUE);

        return getDatabase().insert(TABLE_FILTERS, null, cv);
    }

    /**
    * Updates whether a filter is enabled only if the value to be changed is different
    * @param id id of the row
    * @param enabled change the filter to be enabled or not
    * @return the number of rows that were affected
    */
    private synchronized int updateFilterSearchEnabled(long id, boolean enabled) {
        int isSearchEnabled = enabled ? ENABLED_ATTRIBUTE_VALUE : DISABLED_ATTRIBUTE_VALUE;
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_SEARCH_ENABLED, isSearchEnabled);
        return getDatabase().update(TABLE_FILTERS, contentValues,
                COLUMN_ID + " = ?" + " AND " + COLUMN_SEARCH_ENABLED + " != ?",
                new String[]{Long.toString(id), Integer.toString(isSearchEnabled)} );
    }
    /**
     * Updates whether a filter is enabled only if the value to be changed is different
     * @param id id of the row
     * @param action the action value
     * @return the number of rows that were affected
     */
    private synchronized int updateFilterAction(long id, String action) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_ACTION, action);
        if (action != null && !action.isEmpty()) {
            return getDatabase().update(TABLE_FILTERS, contentValues,
                    COLUMN_ID + " = ?" + " AND " + COLUMN_ACTION + " != ?",
                    new String[]{Long.toString(id), action} );
        }

        return 0;
    }

    /**
     * Updates whether a filter is enabled only if the value to be changed is different
     * @param id id of the row
     * @param action the action value
     * @param pii value of the filter
     * @param piiLabel label of the filter
     * @param isSearchable is filter enabled
     * @return the number of rows that were affected
     */
    private synchronized int updateGlobalFilter(long id, String action, String pii, String piiLabel, boolean isSearchable) {
        ContentValues contentValues = new ContentValues();
        if (action != null && !action.isEmpty()) {
            contentValues.put(COLUMN_ACTION, action);
        }

        if (pii != null && !pii.trim().isEmpty()) {
            contentValues.put(COLUMN_PII_VALUE, pii);
        }

        if (piiLabel != null && !piiLabel.trim().isEmpty()) {
            contentValues.put(COLUMN_PII_LABEL, piiLabel);
        }

        int isSearchEnabled = isSearchable ? ENABLED_ATTRIBUTE_VALUE : DISABLED_ATTRIBUTE_VALUE;
        contentValues.put(COLUMN_SEARCH_ENABLED, isSearchEnabled);

        if (contentValues.size() > 0) {
            return getDatabase().update(TABLE_FILTERS, contentValues,
                    COLUMN_ID + " = ?",
                    new String[]{Long.toString(id)} );
        }

        return 0;
    }

    /**
     * Updates whether a filter is enabled only if the value to be changed is different
     * @param packageName package of the app
     * @param action the action value
     * @param pii value of the filter
     * @param piiLabel label of the filter
     * @return the number of rows that were affected, 1 if it was created
     */
    private synchronized int updateOrCreateCustomFilter(final String packageName, final String action, final String pii, final String piiLabel) {
        int retVal = 0;
        Cursor cursor = getPrivacyFilter(packageName, pii);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                 retVal = updateFilterAction(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)), action);
            }
            cursor.close();
        } else {
            addApplicationFilter(packageName, pii, piiLabel, true, action, true);
        }

        return retVal;
    }

    /**
     * Updates whether a filter is enabled only if the value to be changed is different
     * @param action the action value
     * @param pii value of the filter
     * @param piiLabel label of the filter
     * @return the number of rows that were affected, 1 if it was created
     */
    private synchronized int updateOrCreateGlobalFilter(final String action, final String pii, final String piiLabel) {
        int retVal = 0;
        Cursor cursor = getGlobalPrivacyFilterByLabel(piiLabel);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                boolean isSearchable = cursor.getInt(cursor.getColumnIndex(COLUMN_SEARCH_ENABLED)) == ENABLED_ATTRIBUTE_VALUE;
                retVal = updateGlobalFilter(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)), action, pii, piiLabel, isSearchable);
            }
            cursor.close();
        } else {
            addGlobalFilter(pii, piiLabel, false, action != null ? action : ActionReceiver.ACTION_DEFAULT, true);
        }

        return retVal;
    }

    /** Logs the leak for historical purposes
     * @param appName the name of the app responsible for the leak
     * @param pii the leaking string
     * @param action the action being taken
     * @return the row ID of the updated row, or -1 if an error occurred
     */
    private synchronized long logLeak (String appName, String remoteIp, String pii, String label, String action) {
        // Add leak to history
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_APP, appName);
        cv.put(COLUMN_PII_VALUE, pii);
        cv.put(COLUMN_PII_LABEL, label != null ? label : COLUMN_PII_LABEL__DEFAULT);
        cv.put(COLUMN_ACTION, action);
        cv.put(COLUMN_TIME, System.currentTimeMillis());
        cv.put(COLUMN_REMOTE_IP, remoteIp);

        return getDatabase().insert(TABLE_HISTORY, null, cv);
    }

    /**
     * Logs the upload details (number of files uploaded) at current timestamp
     * @param count
     * @return
     */
    public synchronized long logUpload (int count) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_COUNT, count);
        cv.put(COLUMN_TIME, System.currentTimeMillis());

        return getDatabase().insert(TABLE_LOGS, null, cv);
    }

    /** Returns the cursor for the any privacy filter (global or app specific)
     * @param appName the application package
     * @param pii the leaking string
     * @return the cursor to the result
     * */
    public synchronized Cursor getAnyPrivacyFilter(String appName, String pii) {
        //Log.d(TAG, "getAnyPrivacyFilter: " + appName + ", " + pii);
        Cursor cursor = getPrivacyFilter(appName, pii);

        if (cursor == null) {
            cursor = getGlobalPrivacyFilter(pii);
        }

        return cursor;
    }

    /** Returns the cursor for the any privacy filter (global or app specific) by label
     * @param appName the application package
     * @param piiLabel the pii label
     * @return the cursor to the result
     * */
    public synchronized Cursor getAnyPrivacyFilterByLabel(String appName, String piiLabel) {
        Cursor cursor = getPrivacyFilterByLabel(appName, piiLabel);

        if (cursor == null) {
            cursor = getGlobalPrivacyFilterByLabel(piiLabel);
        }
        return cursor;
    }

    /**
     * Returns a cursor pointing to an entry with the given parameters
     * @param appName name of app. If this is NULL, then it will try to find a global filter
     * @param pii the leaking string
     * @return Cursor pointing to the first entry, or null if one does not exist.
     * Callers must close cursor.
     */
    public synchronized Cursor getPrivacyFilter(String appName, String pii) {
        String columnAppSelection = COLUMN_APP + " = ? ";
        String[] selectionArgs = new String[]{appName, pii};
        if (appName == null) {
            columnAppSelection = COLUMN_APP + " is null ";
            selectionArgs = new String[]{pii};
        }

        Cursor c = getDatabase().query(TABLE_FILTERS, new String[] {COLUMN_ID, COLUMN_APP, COLUMN_PII_LABEL,
                        COLUMN_PII_VALUE, COLUMN_SEARCH_ENABLED, COLUMN_ACTION, COLUMN_IS_CUSTOM},
                columnAppSelection + " AND " + COLUMN_PII_VALUE + " = ?",
                selectionArgs, null, null, null);

        if (c.moveToFirst()) {
            return c;
        }

        c.close();
        return null;
    }

    /**
     * Returns a cursor pointing to an entry with the given parameters
     * @param appName name of app. If this is NULL, then it will try to find a global filter
     * @param piiLabel the pii label
     * @return Cursor pointing to the first entry, or null if one does not exist.
     * Callers must close cursor.
     */
    public synchronized Cursor getPrivacyFilterByLabel(String appName, String piiLabel) {
        String columnAppSelection = COLUMN_APP + " = ? ";
        String[] selectionArgs = new String[]{appName, piiLabel};
        if (appName == null) {
            columnAppSelection = COLUMN_APP + " is null ";
            selectionArgs = new String[]{piiLabel};
        }

        Cursor c = getDatabase().query(TABLE_FILTERS, new String[] {COLUMN_ID, COLUMN_APP, COLUMN_PII_LABEL,
                        COLUMN_PII_VALUE, COLUMN_SEARCH_ENABLED, COLUMN_ACTION, COLUMN_IS_CUSTOM},
                columnAppSelection + " AND " + COLUMN_PII_LABEL + " = ?",
                selectionArgs, null, null, null);

        if (c.moveToFirst()) {
            return c;
        }

        c.close();
        return null;
    }

    public synchronized PrivacyFilterModel convertToPrivacyFilterModel(Cursor cursor){
        PrivacyFilterModel model = null;

        if (cursor != null) {
            model = new PrivacyFilterModel(
                    cursor.getInt(cursor.getColumnIndex(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_APP)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_PII_LABEL)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_PII_VALUE)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_ACTION)),
                    cursor.getInt(cursor.getColumnIndex(COLUMN_SEARCH_ENABLED)) == ENABLED_ATTRIBUTE_VALUE,
                    cursor.getInt(cursor.getColumnIndex(COLUMN_IS_CUSTOM)) == ENABLED_ATTRIBUTE_VALUE
            );
            cursor.close();
        }

        return model;
    }

    /**
     * Returns a cursor pointing to an entry with the given parameters
     * @param pii the leaking string
     * @return Cursor pointing to the first entry, or null if one does not exist.
     * Callers must close cursor.
     */
    private synchronized Cursor getGlobalPrivacyFilter(String pii) {
        return getPrivacyFilter(null, pii);
    }

    /**
     * Returns a cursor pointing to an entry with the given parameters
     * @param piiLabel the pii label
     * @return Cursor pointing to the first entry, or null if one does not exist.
     * Callers must close cursor.
     */
    private synchronized Cursor getGlobalPrivacyFilterByLabel(String piiLabel) {
        return getPrivacyFilterByLabel(null, piiLabel);
    }

    /**Retrieves information about when logs have been uploaded.
     * @return A cursor pointing to the data. Caller must close the cursor.
     * Cursor should have some upload info (how many logs were uploaded during one time or how much data were in those logs), and timestamp */
    public synchronized Cursor getContributeLogHistory() {
        printTable(TABLE_LOGS);

        return getDatabase().query(TABLE_LOGS, new String[] {COLUMN_ID, COLUMN_COUNT, COLUMN_TIME},null, null, null, null, null);
    }

    /**Retrieves all privacy filters
     * @return A cursor pointing to the data. Caller must close the cursor.
     * Cursor should have data for filter label, filter value, is it enabled, protection level */
    public synchronized Cursor getPrivacyFilters() {

        //printTable(TABLE_FILTERS);

        return getDatabase().query(TABLE_FILTERS, new String[] {COLUMN_ID, COLUMN_APP, COLUMN_PII_LABEL,
                COLUMN_PII_VALUE, COLUMN_SEARCH_ENABLED, COLUMN_ACTION, COLUMN_IS_CUSTOM},
                null, null, null, null, null);
    }

    /**Retrieves all GLOBAL privacy filters
     * @return A cursor pointing to the data. Caller must close the cursor.
     */
    public synchronized Cursor getGlobalPrivacyFilters() {

        return getDatabase().query(TABLE_FILTERS, new String[] {COLUMN_ID, COLUMN_APP, COLUMN_PII_LABEL,
                        COLUMN_PII_VALUE, COLUMN_SEARCH_ENABLED, COLUMN_ACTION, COLUMN_IS_CUSTOM},
                COLUMN_APP + " is null", null, null, null, null);
    }

    /**Retrieves all App specific privacy filters
     * @return A cursor pointing to the data. Caller must close the cursor.
     */
    public synchronized Cursor getAppSpecificPrivacyFilters() {

        return getDatabase().query(TABLE_FILTERS, new String[] {COLUMN_ID, COLUMN_APP, COLUMN_PII_LABEL,
                        COLUMN_PII_VALUE, COLUMN_SEARCH_ENABLED, COLUMN_ACTION, COLUMN_IS_CUSTOM},
                COLUMN_APP + " is not null", null, null, null, null);
    }

    /**Retrieves a history of leaked events for an Application
     * @return A cursor pointing to the data. Caller must close the cursor.
     * Cursor should have data for filter label, filter action taken (none, hashed, blocked, etc), and timestamp */
    public synchronized Cursor getPrivacyLeaksAppHistory(final String appName) {
        return getDatabase().query(TABLE_HISTORY, new String[] {
                            COLUMN_ID, COLUMN_APP, COLUMN_PII_LABEL, COLUMN_PII_VALUE, COLUMN_ACTION, COLUMN_TIME},
                        COLUMN_APP + " = ?", new String[] {appName},
                        null, null, COLUMN_TIME + " DESC");
    }
    /**Retrieves information for all apps and how many leaks there were based on sort type
     * @return A cursor pointing to the data. Caller must close the cursor.
     * Cursor should have app name and leak summation based on a sort type
     * Sort types: PrivacyLeaksReportCursorLoader.RECENT_SORT, PrivacyLeaksReportCursorLoader.WEEKLY_SORT, PrivacyLeaksReportCursorLoader.MONTHLY_SORT */
    public synchronized Cursor getPrivacyLeaksReport(final String sortType) {

        String timeSelection;
        switch (sortType) {
            case PrivacyLeaksReportCursorLoader.MONTHLY_SORT:
                timeSelection = Long.toString(
                        System.currentTimeMillis() - PrivacyLeaksReportCursorLoader.ONE_MONTH);
                break;
            case PrivacyLeaksReportCursorLoader.WEEKLY_SORT:
                timeSelection = Long.toString(
                        System.currentTimeMillis() - 2*PrivacyLeaksReportCursorLoader.ONE_WEEK);
                break;
            case PrivacyLeaksReportCursorLoader.RECENT_SORT:
            default:
                timeSelection = Long.toString(
                        System.currentTimeMillis() - PrivacyLeaksReportCursorLoader.ONE_WEEK);
                break;
        }

        return getDatabase().query(TABLE_HISTORY, new String[] {COLUMN_ID, COLUMN_APP,
                                    "COUNT(" + COLUMN_APP + ") AS " +
                                    PrivacyLeaksReportCursorLoader.COLUMN_COUNT},
                                COLUMN_TIME + " > ?", new String[] {timeSelection},
                                COLUMN_APP, null,
                                PrivacyLeaksReportCursorLoader.COLUMN_COUNT + " DESC");
    }

    /**
     * Enables all filters by Application  in {@link #TABLE_FILTERS}
     * @param packageName Package name of application
     * @return the number of rows affected
     */
    private synchronized int enableAllFiltersByApp(String packageName) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SEARCH_ENABLED, ENABLED_ATTRIBUTE_VALUE);
        if (packageName != null && !packageName.isEmpty()) {
            return getDatabase().update(TABLE_FILTERS, cv, COLUMN_APP + " = ? ", new String[] {packageName} );
        }
        return 0;
    }

    /**
     * Disables all filters by Application  in {@link #TABLE_FILTERS}
     * @param packageName Package name of application
     * @return the number of rows affected
     */
    private synchronized int disableAllFiltersByApp(String packageName) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SEARCH_ENABLED, DISABLED_ATTRIBUTE_VALUE);
        if (packageName != null && !packageName.isEmpty()) {
            return getDatabase().update(TABLE_FILTERS, cv, COLUMN_APP + " = ? ", new String[] {packageName} );
        }
        return 0;
    }

    /**
     * Enables all global filters in {@link #TABLE_FILTERS}
     * @return the number of rows affected
     */
    private synchronized int enableAllGlobalFilters() {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SEARCH_ENABLED, ENABLED_ATTRIBUTE_VALUE);
        return getDatabase().update(TABLE_FILTERS, cv, COLUMN_APP + " is null", null );
    }

    /**
     * Disables all global filters in {@link #TABLE_FILTERS}
     * @return the number of rows affected
     */
    private synchronized int disableAllGlobalFilters() {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SEARCH_ENABLED, DISABLED_ATTRIBUTE_VALUE);
        return getDatabase().update(TABLE_FILTERS, cv, COLUMN_APP + " is null", null);
    }

    /**
     * Enables all filters in {@link #TABLE_FILTERS}
     * @return the number of rows affected
     */
    private synchronized int enableAllFilters() {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SEARCH_ENABLED, ENABLED_ATTRIBUTE_VALUE);
        return getDatabase().update(TABLE_FILTERS, cv, null, null);
    }


    /**
     * Disables all filters in {@link #TABLE_FILTERS}
     * @return the number of rows affected
     */
    private synchronized int disableAllFilters() {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SEARCH_ENABLED, DISABLED_ATTRIBUTE_VALUE);
        return getDatabase().update(TABLE_FILTERS, cv, null, null);
    }


    /**
     * Clears {@link #TABLE_HISTORY}, thus clearing away any history of past leaks
     */
    public synchronized void clearLeaksHistory() {
        // Clear all entries in table
        getDatabase().execSQL("delete from "+ TABLE_HISTORY);
    }

    /**
     * Clears custom filters from {@link #TABLE_FILTERS}
     */
    private synchronized void clearPrivacyFilters() {
        getDatabase().delete(TABLE_FILTERS, COLUMN_IS_CUSTOM + " = 1", null);
    }


    /**
     * Clears custom filters from {@link #TABLE_FILTERS}
     */
    private synchronized void deletePrivacyFilter(long id) {
        getDatabase().delete(TABLE_FILTERS, PrivacyDB.COLUMN_ID + " = " + id, null);
    }

    /**
     * Clears {@link #TABLE_LOGS}, thus clearing away any history of past contributiona
     */
    public synchronized void clearUploadHistory() {
        getDatabase().execSQL("delete from " + TABLE_LOGS);
    }
    
    /** Deletes all entries in the database */
    public synchronized void clearDatabase() {
        getDatabase().delete(TABLE_FILTERS, null, null);
        getDatabase().delete(TABLE_HISTORY, null, null);
        getDatabase().delete(TABLE_LOGS, null, null);
    }

    /**
     * Returns whether a packageName has location search enabled. If no packageName, then return global Location search enabled value
     * @param packageName package name of application
     * @return whether search is enabled for location
     */
    public boolean isLocationSearchEnabled(String packageName) {
        boolean isEnabled = false;
        Cursor cursor = this.getAnyPrivacyFilter(packageName, DEFAULT_PII_VALUE__LOCATION);
        if (cursor != null) {
            isEnabled = cursor.getInt(cursor.getColumnIndex(COLUMN_SEARCH_ENABLED)) == ENABLED_ATTRIBUTE_VALUE;
            cursor.close();
        }
        return isEnabled;
    }

    /**
     * All values for privacy filters without location since that is special (must be enabled)
     * @return Hashset of privacy filter values
     */
    public HashSet<String> getAllEnabledPrivacyFiltersValues() {
        HashSet<String> privacyFilterValues = new HashSet<>();
        Cursor cursor = this.getPrivacyFilters();
        if (cursor.moveToFirst()) {
            while(!cursor.isAfterLast()) {
                String piiValue = cursor.getString(cursor.getColumnIndex(COLUMN_PII_VALUE));
                boolean isEnabled = cursor.getInt(cursor.getColumnIndex(COLUMN_SEARCH_ENABLED)) == ENABLED_ATTRIBUTE_VALUE;

                // skip location related privacy filter
                if (piiValue != null && isEnabled && !piiValue.trim().isEmpty() && !piiValue.equals(DEFAULT_PII_VALUE__LOCATION)) {
                    if (!privacyFilterValues.contains(piiValue)) {
                        privacyFilterValues.add(piiValue);
                    }
                }
                cursor.moveToNext();
            }
        }
        cursor.close();

        return privacyFilterValues;
    }

    /** Close the database */
    public synchronized void close() {
        sqlHandler.close();
        _database = null;
    }

    private synchronized boolean isClose() {
        return _database == null;
    }

    /** Async task methods and class **/

    public void enableAllGlobalFiltersAsyncTask(Context context, OnFilterUpdateListener listener) {
        UpdateFiltersTask task = new UpdateFiltersTask(context, UpdateFiltersTask.ENABLE_GLOBAL_FILTERS);
        task.setOnFilterUpdateListener(listener);
        task.execute();
    }

    public void disableAllGlobalFiltersAsyncTask(Context context, OnFilterUpdateListener listener) {
        UpdateFiltersTask task = new UpdateFiltersTask(context, UpdateFiltersTask.DISABLE_GLOBAL_FILTERS);
        task.setOnFilterUpdateListener(listener);
        task.execute();
    }

    public void clearPrivacyFiltersAsyncTask(Context context, OnFilterUpdateListener listener) {
        UpdateFiltersTask task = new UpdateFiltersTask(context, UpdateFiltersTask.CLEAR_PRIVACY_FILTERS);
        task.setOnFilterUpdateListener(listener);
        task.execute();
    }

    public void updateOrCreateCustomFilterAsyncTask(Context context, OnFilterUpdateListener listener, String packageName, String action, String pii, String piiLabel) {
        UpdateFiltersTask task = new UpdateFiltersTask(context, UpdateFiltersTask.UPDATE_OR_CREATE_CUSTOM_FILTER, packageName, action, pii, piiLabel);
        task.setOnFilterUpdateListener(new DatabaseOnFilterUpdateListener(context, packageName, piiLabel, listener));
        task.execute();
    }

    public void updateOrCreateGlobalFilterAsyncTask(Context context, OnFilterUpdateListener listener, String action, String pii, String piiLabel) {
        UpdateFiltersTask task = new UpdateFiltersTask(context, UpdateFiltersTask.UPDATE_OR_CREATE_GLOBAL_FILTER, null, action, pii, piiLabel);
        task.setOnFilterUpdateListener(listener);
        task.execute();
    }

    public void updateFilterActionAsyncTask(Context context, OnFilterUpdateListener listener, long id, String action) {
        UpdateFiltersTask task = new UpdateFiltersTask(context, UpdateFiltersTask.UPDATE_FILTER_ACTION, id, action);
        task.setOnFilterUpdateListener(listener);
        task.execute();
    }

    public void updateFilterSearchEnabledAsyncTask(Context context, OnFilterUpdateListener listener, long id, boolean searchEnabled) {
        UpdateFiltersTask task = new UpdateFiltersTask(context, UpdateFiltersTask.UPDATE_FILTER_SEARCH_ENABLED, id, searchEnabled);
        task.setOnFilterUpdateListener(listener);
        task.execute();
    }

    public void deleteFilterAsyncTask(Context context, OnFilterUpdateListener listener, long id) {
        UpdateFiltersTask task = new UpdateFiltersTask(context, UpdateFiltersTask.DELETE_FILTER, id, null);
        task.setOnFilterUpdateListener(new DatabaseOnFilterUpdateListener(context, listener));
        task.execute();
    }
    public void addApplicationFilterAsyncTask(Context context, OnFilterUpdateListener listener, String packageName, String piiValue, String piiLabel, boolean isCustom,
                                              String action, boolean isSearchEnabled) {
        UpdateFiltersTask task = new UpdateFiltersTask(context, UpdateFiltersTask.ADD_APPLICATION_FILTER, packageName, piiValue, piiLabel, isCustom, action, isSearchEnabled);
        task.setOnFilterUpdateListener(new DatabaseOnFilterUpdateListener(context, packageName, piiLabel, listener));
        task.execute();
    }

    public void addGlobalFilterAsyncTask(Context context, OnFilterUpdateListener listener, String piiValue, String piiLabel, boolean isCustom,
                                              String action, boolean isSearchEnabled) {
        UpdateFiltersTask task = new UpdateFiltersTask(context, UpdateFiltersTask.ADD_GLOBAL_FILTER, null, piiValue, piiLabel, isCustom, action, isSearchEnabled);
        task.setOnFilterUpdateListener(listener);
        task.execute();
    }

    public void updateMacAddrAsyncTask(Context context, OnFilterUpdateListener listener) {

        // set macaddr
        String macAddress = DeviceUtils.getMACAddress("wlan0");
        if (macAddress == null) {
            macAddress = DeviceUtils.getMACAddress("eth0");
        }
        if (macAddress != null && !macAddress.trim().isEmpty()) {
            updateOrCreateGlobalFilterAsyncTask(context, listener, null, macAddress, PrivacyDB.DEFAULT_PII__MAC_ADDRESS);
        }
    }

    public void logLeakAsyncTask(Context context, final OnLogLeakListener listener,
                                 String packageName, String remoteIp, String piiValue,
                                 String piiLabel, String action) {
        LogLeakTask task = new LogLeakTask(context, packageName, remoteIp, action,  piiValue, piiLabel);
        task.setOnLogLeakListener(new DatabaseOnLogLeakListener(context, packageName, piiLabel, listener));
        task.execute();
    }

    private Intent createDBLeakedIntent(String packageName, String piiLabel) {
        Intent intent = new Intent(PrivacyDB.DB_LEAK_CHANGED);
        if (packageName != null) {
            intent.putExtra(PrivacyDB.COLUMN_APP, packageName);
        }

        if (piiLabel != null) {
            intent.putExtra(PrivacyDB.COLUMN_PII_LABEL, piiLabel);
        }
        return intent;
    }

    private Intent createDBFilterIntent(String packageName, String piiLabel) {
        Intent intent = new Intent(PrivacyDB.DB_FILTER_CHANGED);
        if (packageName != null) {
            intent.putExtra(PrivacyDB.COLUMN_APP, packageName);
        }

        if (piiLabel != null) {
            intent.putExtra(PrivacyDB.COLUMN_PII_LABEL, piiLabel);
        }
        return intent;
    }

    private class AdvertiserIDTask extends AsyncTask<Void, Void, String> {

        private Context mContext;
        private boolean isCustom = false;
        private boolean isSearchable = true;
        private String action = ActionReceiver.ACTION_ALLOW;

        public AdvertiserIDTask(Context context, boolean isCustom, String action, boolean isSearchable) {
            this.mContext = context;
            this.isCustom = isCustom;
            this.action = action;
            this.isSearchable = isSearchable;
        }

        @Override
        protected String doInBackground(Void... params) {
            AdvertisingIdClient.Info idInfo = null;
            try {
                idInfo = AdvertisingIdClient.getAdvertisingIdInfo(mContext);
            } catch (GooglePlayServicesNotAvailableException e) {
                Log.e(TAG, "Could not get Advertiser ID: " + e.getMessage(), e);
            } catch (GooglePlayServicesRepairableException e) {
                Log.e(TAG, "Could not get Advertiser ID: " + e.getMessage(), e);
            } catch (IOException e) {
                Log.e(TAG, "Could not get Advertiser ID: " + e.getMessage(), e);
            }
            String advertId = null;
            try{
                advertId = idInfo.getId();
            }catch (NullPointerException e){
                e.printStackTrace();
            }

            return advertId;
        }

        @Override
        protected void onPostExecute(String advertId) {
            if (advertId != null && !advertId.trim().isEmpty()) {
                addGlobalFilter(advertId, PrivacyDB.DEFAULT_PII__ADVERTISER_ID, this.isCustom, this.action, this.isSearchable);
            }
        }

    }

    public class LogLeakTask extends AsyncTask<Void, Void, Boolean> {

        private Context mContext;
        private String packageName;
        private String remoteIp;
        private String action;
        private String pii;
        private String piiLabel;
        private OnLogLeakListener mListener;

        public LogLeakTask(Context context, String packageName, String remoteIp, String action,
                           String pii, String piiLabel) {
            this.mContext = context;
            this.packageName = packageName;
            this.remoteIp = remoteIp;
            this.action = action;
            this.pii = pii;
            this.piiLabel = piiLabel;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            PrivacyDB db = PrivacyDB.getInstance(mContext);
            db.logLeak(this.packageName, this.remoteIp, this.pii, this.piiLabel, this.action);
            return true;
        }

        public void setOnLogLeakListener(OnLogLeakListener listener) {
            mListener = listener;
        }

        protected void onPostExecute(Boolean result) {
            if (mListener != null) {
                mListener.OnLogLeakListenerDone();
            }
        }
    }

    public class UpdateFiltersTask extends AsyncTask<Void, Void, Boolean> {

        public static final String ENABLE_ALL_FILTERS = "ENABLE_ALL_FILTERS";
        public static final String DISABLE_ALL_FILTERS = "DISABLE_ALL_FILTERS";
        public static final String ENABLE_GLOBAL_FILTERS = "ENABLE_GLOBAL_FILTERS";
        public static final String DISABLE_GLOBAL_FILTERS = "DISABLE_GLOBAL_FILTERS";
        public static final String ENABLE_APP_FILTERS = "ENABLE_APP_FILTERS";
        public static final String DISABLE_APP_FILTERS = "DISABLE_APP_FILTERS";
        public static final String CLEAR_PRIVACY_FILTERS = "CLEAR_PRIVACY_FILTERS";
        public static final String UPDATE_OR_CREATE_CUSTOM_FILTER = "UPDATE_OR_CREATE_CUSTOM_FILTER";
        public static final String UPDATE_FILTER_ACTION = "UPDATE_FILTER_ACTION";
        public static final String UPDATE_FILTER_SEARCH_ENABLED = "UPDATE_FILTER_SEARCH_ENABLED";
        public static final String ADD_APPLICATION_FILTER = "ADD_APPLICATION_FILTER";
        public static final String ADD_GLOBAL_FILTER = "ADD_GLOBAL_FILTER";
        public static final String UPDATE_OR_CREATE_GLOBAL_FILTER = "UPDATE_OR_CREATE_GLOBAL_FILTER";
        public static final String DELETE_FILTER = "DELETE_FILTER";

        private Context mContext;
        private String updateType;
        private String packageName;
        private String action;
        private String pii;
        private String piiLabel;
        private long id;
        private boolean searchEnabled;
        private boolean isCustom;

        private OnFilterUpdateListener mListener;


        public UpdateFiltersTask(Context context, String updateType) {
            this.mContext = context;
            this.updateType = updateType;
        }

        public UpdateFiltersTask(Context context, String updateType, String packageName, String action, String pii, String piiLabel) {
            this.mContext = context;
            this.updateType = updateType;
            this.packageName = packageName;
            this.action = action;
            this.pii = pii;
            this.piiLabel = piiLabel;
        }

        public UpdateFiltersTask(Context context, String updateType, String packageName, String piiValue, String piiLabel, boolean isCustom,
                                 String action, boolean isSearchEnabled) {
            this.mContext = context;
            this.updateType = updateType;
            this.packageName = packageName;
            this.action = action;
            this.pii = piiValue;
            this.piiLabel = piiLabel;
            this.searchEnabled = isSearchEnabled;
            this.isCustom = isCustom;
        }

        public UpdateFiltersTask(Context context, String updateType, long id, String action) {
            this.mContext = context;
            this.updateType = updateType;
            this.id = id;
            this.action = action;
        }

        public UpdateFiltersTask(Context context, String updateType, long id, boolean searchEnabled) {
            this.mContext = context;
            this.updateType = updateType;
            this.id = id;
            this.searchEnabled = searchEnabled;
        }


        public void setOnFilterUpdateListener(OnFilterUpdateListener listener) {
            mListener = listener;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (updateType != null) {
                PrivacyDB db = PrivacyDB.getInstance(mContext);
                if (updateType.equals(ENABLE_ALL_FILTERS)) {
                    db.enableAllFilters();
                } else if (updateType.equals(DISABLE_ALL_FILTERS)) {
                    db.disableAllFilters();
                } else if (updateType.equals(ENABLE_GLOBAL_FILTERS)) {
                    db.enableAllGlobalFilters();
                } else if (updateType.equals(DISABLE_GLOBAL_FILTERS)) {
                    db.disableAllGlobalFilters();
                } else if (updateType.equals(CLEAR_PRIVACY_FILTERS)) {
                    db.clearPrivacyFilters();
                } else if (updateType.equals(ENABLE_APP_FILTERS)) {
                    if (packageName != null) {
                        db.enableAllFiltersByApp(packageName);
                    }
                } else if (updateType.equals(DISABLE_APP_FILTERS)) {
                    if (packageName != null) {
                        db.disableAllFiltersByApp(packageName);
                    }
                } else if (updateType.equals(UPDATE_OR_CREATE_CUSTOM_FILTER)) {
                    db.updateOrCreateCustomFilter(packageName, action, pii, piiLabel);
                } else if (updateType.equals(UPDATE_FILTER_ACTION)) {
                    db.updateFilterAction(id, action);
                } else if (updateType.equals(UPDATE_FILTER_SEARCH_ENABLED)) {
                    db.updateFilterSearchEnabled(id, searchEnabled);
                } else if (updateType.equals(ADD_APPLICATION_FILTER)) {
                    db.addApplicationFilter(packageName, pii, piiLabel, isCustom, action, searchEnabled);
                } else if (updateType.equals(ADD_GLOBAL_FILTER)) {
                    db.addGlobalFilter(pii, piiLabel, isCustom, action, searchEnabled);
                } else if (updateType.equals(UPDATE_OR_CREATE_GLOBAL_FILTER)) {
                    db.updateOrCreateGlobalFilter(action, pii, piiLabel);
                } else if (updateType.equals(DELETE_FILTER)) {
                    db.deletePrivacyFilter(id);
                }
            }

            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (mListener != null) {
                mListener.onFilterUpdateDone();
            }
        }

    }

    /**
     * Listener for handling when update to filters are done
     */
    public interface OnFilterUpdateListener {

        public void onFilterUpdateDone();
    }

    /**
     * Listener for handling when update to filters are done
     */
    public interface OnLogLeakListener {

        public void OnLogLeakListenerDone();
    }

    public class DatabaseOnLogLeakListener implements OnLogLeakListener {

        private String appName;
        private String piiLabel;
        private  Context context;

        // external listener
        private OnLogLeakListener listener;

        public DatabaseOnLogLeakListener(Context context, String appName, String piiLabel, OnLogLeakListener listener) {
            this.context = context;
            this.appName = appName;
            this.piiLabel = piiLabel;
            this.listener = listener;
        }

        @Override
        public void OnLogLeakListenerDone() {
            final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
            final Intent intent = createDBLeakedIntent(appName, piiLabel);
            localBroadcastManager.sendBroadcast(intent);
            if (listener != null) {
                listener.OnLogLeakListenerDone();
            }
        }
    }

    public class DatabaseOnFilterUpdateListener implements OnFilterUpdateListener {

        private String appName;
        private String piiLabel;
        private  Context context;

        // external listener
        private OnFilterUpdateListener listener;

        public DatabaseOnFilterUpdateListener(Context context, String appName, String piiLabel, OnFilterUpdateListener listener) {
            this.context = context;
            this.appName = appName;
            this.piiLabel = piiLabel;
            this.listener = listener;
        }

        public DatabaseOnFilterUpdateListener(Context context, OnFilterUpdateListener listener) {
            this.context = context;
            this.listener = listener;
        }

        @Override
        public void onFilterUpdateDone() {
            final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
            final Intent intent = createDBFilterIntent(appName, piiLabel);
            localBroadcastManager.sendBroadcast(intent);
            if (listener != null) {
                listener.onFilterUpdateDone();
            }
        }
    }
}
