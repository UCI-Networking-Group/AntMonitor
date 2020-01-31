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
package edu.uci.calit2.anteater.client.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.activity.uielements.PrivacyLeaksAppHistoryCursorLoader;
import edu.uci.calit2.anteater.client.android.analysis.ActionReceiver;
import edu.uci.calit2.anteater.client.android.database.PrivacyDB;

/**
 * @author Hieu Le
 */
public class LeaksAppHistoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String INTENT_EXTRA_APP_NAME = "INTENT_APP_NAME";
    public static final String INTENT_EXTRA_PACKAGE_NAME = "INTENT_PACKAGE_NAME";

    private SimpleCursorAdapter mAdapter;
    private static final int LOADER_APP_HISTORY = 0;
    private String pkgName;

    BroadcastReceiver leaksChangedReceiver;
    private IntentFilter leaksFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaks_app_history);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        pkgName = intent.getStringExtra(INTENT_EXTRA_PACKAGE_NAME);
        String appName = intent.getStringExtra(INTENT_EXTRA_APP_NAME);

        if (appName != null && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(appName + " " + getResources().getString(R.string.leaks_history));
        }

        // set back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ListView historyList = (ListView) findViewById(R.id.leak_history_list);

        // the columns refer to the data that will be coming from the cursor and how it ties to the layout elements
        String[] fromColumns = {PrivacyDB.COLUMN_PII_LABEL, PrivacyDB.COLUMN_ACTION, PrivacyDB.COLUMN_TIME, PrivacyDB.COLUMN_PII_VALUE, PrivacyDB.COLUMN_ACTION, PrivacyDB.COLUMN_ACTION, PrivacyDB.COLUMN_ACTION, PrivacyDB.COLUMN_ACTION};
        int[] toViews = {R.id.filter_label, R.id.filter_type, R.id.timestamp, R.id.filter_value, R.id.filter_icon_allow, R.id.filter_icon_hash, R.id.filter_icon_block, R.id.filter_icon_unknown}; // The TextViews in list_item_privacy_reports

        mAdapter = new SimpleCursorAdapter(this,
                R.layout.list_item_privacy_application_leak_history, null,
                fromColumns, toViews, 0);

        // Used to post-process the data for a more user-friendly display
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

            public boolean setViewValue(View toView, Cursor cursor, int columnIndex) {

                if (toView instanceof TextView && columnIndex == cursor.getColumnIndex(PrivacyDB.COLUMN_ACTION)) {
                    TextView textView = (TextView) toView;
                    String action = cursor.getString(columnIndex);
                    if (action.equals(ActionReceiver.ACTION_ALLOW)) {
                        action = "Information allowed to be exposed";
                    } else if (action.equals(ActionReceiver.ACTION_DENY)) {
                        action = "Packet Blocked";
                    } else if (action.equals(ActionReceiver.ACTION_HASH)) {
                        action = "Information was Hashed";
                    } else {
                        action = "Unknown";
                    }

                    textView.setText(action);
                    return true;
                } else if (toView instanceof ImageView && columnIndex == cursor.getColumnIndex(PrivacyDB.COLUMN_ACTION)) {
                    ImageView imageView = (ImageView) toView;
                    String action = cursor.getString(columnIndex);
                    if (action.equals(ActionReceiver.ACTION_ALLOW)) {
                        if (imageView.getId() == R.id.filter_icon_allow) {
                            imageView.setVisibility(View.VISIBLE);
                        }
                    } else if (action.equals(ActionReceiver.ACTION_DENY)) {
                        if (imageView.getId() == R.id.filter_icon_block) {
                            imageView.setVisibility(View.VISIBLE);
                        }
                    } else if (action.equals(ActionReceiver.ACTION_HASH)) {
                        if (imageView.getId() == R.id.filter_icon_hash) {
                            imageView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (imageView.getId() == R.id.filter_icon_unknown) {
                            imageView.setVisibility(View.VISIBLE);
                        }                    }
                    return true;
                } else if (toView instanceof TextView && columnIndex == cursor.getColumnIndex(PrivacyDB.COLUMN_TIME)) {
                    TextView textView = (TextView) toView;
                    SimpleDateFormat df = new SimpleDateFormat("MMM d hh:mm a");
                    df.setTimeZone(TimeZone.getDefault());
                    String result = df.format(cursor.getLong(columnIndex));
                    textView.setText(result);
                    return true;
                }

                return false;
            }
        });

        historyList.setAdapter(mAdapter);
        getSupportLoaderManager().initLoader(LOADER_APP_HISTORY, null, this);

        leaksChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String pkgLeaked = intent.getStringExtra(PrivacyDB.COLUMN_APP);
                // reset the view when there is new data
                if (intent.getAction().equals(PrivacyDB.DB_LEAK_CHANGED) && pkgName.equals(pkgLeaked)) {
                    refreshView();
                }
            }
        };

        leaksFilter = new IntentFilter(PrivacyDB.DB_LEAK_CHANGED);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new PrivacyLeaksAppHistoryCursorLoader(this, pkgName);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private void refreshView() {
        getSupportLoaderManager().restartLoader(LOADER_APP_HISTORY, null, this);
    }

    public void onResume() {
        super.onResume();
        refreshView();
        LocalBroadcastManager.getInstance(this).registerReceiver(leaksChangedReceiver, leaksFilter);
    }

    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(leaksChangedReceiver);
    }
}
