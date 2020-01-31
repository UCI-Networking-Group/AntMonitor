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
package edu.uci.calit2.anteater.client.android.activity.uielements;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.analysis.ActionReceiver;
import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.anteater.client.android.signals.LocationMonitor;

/**
 * @author Hieu Le
 */
public class PrivacyFilterListAdapter extends CursorAdapter {

    public static final String TAG = "PrivacyFilterLA";

    private PrivacyDB database;
    private boolean isGlobal = true;
    private PackageManager packageManager;
    private OnAdapterInteractionListener listener;
    private List<Integer> selectedFilters = new ArrayList<>();

    static class CheckBoxViewHolder {
        long filterId;
        int cursorPosition;
    }

    public PrivacyFilterListAdapter(Context context, Cursor c, boolean autoRequery, boolean isGlobal, OnAdapterInteractionListener listener) {
        super(context, c, autoRequery);
        database =  PrivacyDB.getInstance(context);
        this.isGlobal = isGlobal;
        this.packageManager = context.getPackageManager();
        this.listener = listener;
    }

    public static String getActionLabel(final String protectionLvl) {
        if ( protectionLvl.equals(ActionReceiver.ACTION_ALLOW)) {
            return "None";
        } else if (protectionLvl.equals(ActionReceiver.ACTION_DENY)) {
            return "Block";
        } else if (protectionLvl.equals(ActionReceiver.ACTION_HASH)) {
            return "Hash";
        }

        return "None";
    }
    public static String getAction(final String protectionLabel) {
        if ( protectionLabel.equals("None")) {
            return ActionReceiver.ACTION_ALLOW;
        } else if (protectionLabel.equals("Block")) {
            return ActionReceiver.ACTION_DENY;
        } else if (protectionLabel.equals("Hash")) {
            return ActionReceiver.ACTION_HASH;
        }
        return ActionReceiver.ACTION_ALLOW;
    }

    private void updateSelectedFilter(int cursorPosition, boolean isSelected) {
        if (isSelected) {
            if (!selectedFilters.contains(cursorPosition)) {
                selectedFilters.add(cursorPosition);
            }
        } else {
            if (selectedFilters.contains(cursorPosition)) {
                selectedFilters.remove((Object)cursorPosition);
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        //Log.d(TAG, "New view: " + cursor.getPosition());
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_privacy_filters, parent, false);
        Spinner spinner = (Spinner) view.findViewById(R.id.protection_lvl);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.privacy_filter_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String protectionLabel = (String) adapterView.getItemAtPosition(i);
                String filterAction = getAction(protectionLabel);
                long filterId = (long) adapterView.getTag();
                database.updateFilterActionAsyncTask(adapterView.getContext(), null, filterId, filterAction);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // not sure if we need to do anything here.
            }
        });

        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox_filter);
        ImageView appIcon = (ImageView) view.findViewById(R.id.app_icon);

        if (isGlobal) {
            // init values for selectedFilters
            boolean isFilterEnabled =
                    cursor.getInt(cursor.getColumnIndex(PrivacyDB.COLUMN_SEARCH_ENABLED)) == 1;
            updateSelectedFilter(cursor.getPosition(), isFilterEnabled);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    CheckBoxViewHolder checkBoxViewHolder = (CheckBoxViewHolder) compoundButton.getTag();
                    if (selectedFilters.contains(checkBoxViewHolder.cursorPosition) != b) {
                        database.updateFilterSearchEnabledAsyncTask(compoundButton.getContext(), new PrivacyDB.OnFilterUpdateListener() {
                            @Override
                            public void onFilterUpdateDone() {
                                listener.adapterCallsRefreshCursor();
                            }
                        }, checkBoxViewHolder.filterId, b);
                    }
                    updateSelectedFilter(checkBoxViewHolder.cursorPosition, b);
                }
            });
            //appIcon.setVisibility(View.GONE);
            appIcon.setImageDrawable(ContextCompat.getDrawable(context.getApplicationContext(), R.drawable.ic_public_black_24dp));
            appIcon.setColorFilter(ContextCompat.getColor(context,R.color.common_signin_btn_light_text_default));

        } else {
            // hide check box when it is not global filter
            checkBox.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        boolean isFilterEnabled =
                cursor.getInt(cursor.getColumnIndex(PrivacyDB.COLUMN_SEARCH_ENABLED)) == 1;
        String filterLabel = cursor.getString(cursor.getColumnIndex(PrivacyDB.COLUMN_PII_LABEL));
        String filterAction = cursor.getString(cursor.getColumnIndex(PrivacyDB.COLUMN_ACTION));
        String filterValue = cursor.getString(cursor.getColumnIndex(PrivacyDB.COLUMN_PII_VALUE));
        String filterApp = cursor.getString(cursor.getColumnIndex(PrivacyDB.COLUMN_APP));

        //Log.d(TAG, "bind view: " + cursor.getPosition());
        // location filter is special
        if (filterLabel.equals(PrivacyDB.DEFAULT_PII__LOCATION)) {
            filterValue = LocationMonitor.getInstance(context).getLocationStr();
        }

        // set app icon
        ImageView appIcon = (ImageView) view.findViewById(R.id.app_icon);

        if (filterApp != null) {

            PackageManager pm = context.getPackageManager();

            ApplicationInfo appInfo;
            try {
                appInfo = pm.getApplicationInfo(filterApp, 0);
                appIcon.setImageDrawable(appInfo.loadIcon(pm));
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Application icon not found for package: " + filterApp);
                appIcon.setImageResource(R.mipmap.default_icon_app);
            }

            String appDisplayName = "Unknown App";
            try {
                appDisplayName = pm.getApplicationLabel(pm.getApplicationInfo(filterApp, 0)).toString();
            } catch (PackageManager.NameNotFoundException e) {
                // do nothing, we will keep it as pkgName
            }
            filterLabel = appDisplayName + " - " + filterLabel;
        } else if (!isGlobal) {
            appIcon.setImageResource(R.mipmap.default_icon_app);
        }

        //Log.d(TAG,  "filterLabel: " + filterLabel + ", filterAction: " + filterAction + ", filterEnabled: " + isFilterEnabled);

        long filterId = cursor.getLong(cursor.getColumnIndex(PrivacyDB.COLUMN_ID));

        if (isGlobal) {
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox_filter);
            CheckBoxViewHolder checkBoxViewHolder = new CheckBoxViewHolder();
            checkBoxViewHolder.cursorPosition = cursor.getPosition();
            checkBoxViewHolder.filterId = filterId;
            checkBox.setTag(checkBoxViewHolder);
            checkBox.setChecked(isFilterEnabled);
        }

        TextView textView = (TextView) view.findViewById(R.id.privacy_filters_label);
        textView.setText(filterLabel);

        TextView valueTextView = (TextView) view.findViewById(R.id.privacy_filters_value);
        valueTextView.setText(filterValue);

        Spinner spinner = (Spinner) view.findViewById(R.id.protection_lvl);
        spinner.setTag(filterId);
        ArrayAdapter spinnerAdapter = (ArrayAdapter) spinner.getAdapter();
        int selectedPosition = spinnerAdapter.getPosition(getActionLabel(filterAction));
        spinner.setSelection(selectedPosition);
    }

    public interface OnAdapterInteractionListener {
        void adapterCallsRefreshCursor();
    }
}
