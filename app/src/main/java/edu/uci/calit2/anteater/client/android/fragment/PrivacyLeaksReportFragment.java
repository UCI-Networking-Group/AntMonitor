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
package edu.uci.calit2.anteater.client.android.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.activity.LeaksAppHistoryActivity;
import edu.uci.calit2.anteater.client.android.activity.uielements.PrivacyLeaksReportCursorLoader;
import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.anteater.client.android.util.OpenAppDetails;
import edu.uci.calit2.anteater.client.android.util.PreferenceTags;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PrivacyLeaksReportFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PrivacyLeaksReportFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PrivacyLeaksReportFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "PrivacyLeaksReportF";

    private static final int LOADER_RECENT = 0;
    private static final int LOADER_WEEKLY = 1;
    private static final int LOADER_MONTHLY = 2;

    private SimpleCursorAdapter mAdapter;
    private OnFragmentInteractionListener mListener;

    BroadcastReceiver leaksChangedReceiver;
    private IntentFilter leaksFilter;

    public PrivacyLeaksReportFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PrivacyLeaksReportFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PrivacyLeaksReportFragment newInstance(String param1, String param2) {
        PrivacyLeaksReportFragment fragment = new PrivacyLeaksReportFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_privacy_reports, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // handle item selection
        switch (item.getItemId()) {
            case R.id.menu_option_clear_reports:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.clear_privacy_reports_confirm_alert)
                        .setTitle(R.string.are_you_sure);
                builder.setPositiveButton(R.string.ok, new  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PrivacyDB database = PrivacyDB.getInstance(getActivity());
                        database.clearLeaksHistory();
                        database.close();
                        dialog.dismiss();
                        refreshView();
                        clearPrivacyReportDialogDone();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearPrivacyReportDialogDone() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.clear_privacy_reports_alert)
                .setTitle(R.string.completed);
        builder.setPositiveButton(R.string.ok, new  DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final LoaderManager.LoaderCallbacks callbacks = this;
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_privacy_leaks_report, container, false);

        ListView reportsListView = (ListView) view.findViewById(R.id.privacy_reports_list);
        View listHeaderView = inflater.inflate(R.layout.fragment_privacy_leaks_report_header, null);
        reportsListView.addHeaderView(listHeaderView, null, false);
        // the columns refer to the data that will be coming from the cursor and how it ties to the layout elements
        String[] fromColumns = {PrivacyDB.COLUMN_APP, PrivacyLeaksReportCursorLoader.COLUMN_COUNT, PrivacyDB.COLUMN_APP};
        int[] toViews = {R.id.app_name, R.id.leaks_count, R.id.app_icon}; // The TextViews in list_item_privacy_reports

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_item_privacy_reports, null,
                fromColumns, toViews, 0);

        // Used to post-process the data for a more user-friendly display
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

            public boolean setViewValue(View toView, Cursor cursor, int columnIndex) {
                if (columnIndex == cursor.getColumnIndex(PrivacyDB.COLUMN_APP)) {
                    String pkgName = cursor.getString(columnIndex);
                    final PackageManager pm = getContext().getPackageManager();
                    ApplicationInfo appInfo = null;
                    try {
                        appInfo = pm.getApplicationInfo(pkgName, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(TAG, "Application not found for package: " + pkgName);
                    }

                    // Even if app was not found, we need to set something,
                    // otherwise old values are re-used and inconsistencies arise
                    // Must be a peculiarity of CursorAdapter
                    if (toView instanceof ImageView) {
                        // Set app icon
                        ImageView appIcon = (ImageView) toView;
                        appIcon.setImageDrawable(appInfo == null ? null : appInfo.loadIcon(pm));
                        return true;
                    } else if (toView instanceof TextView) {
                        // Set app name instead of package name if available
                        TextView textView = (TextView) toView;
                        textView.setText(appInfo == null ? pkgName : pm.getApplicationLabel(appInfo));

                        // Keep package name as a tag
                        //Log.w(TAG, "Setting tag for " + pkgName);
                        textView.setTag(pkgName);
                        return true;
                    }
                }
                return false;
            }
        });

        reportsListView.setAdapter(mAdapter);
        getLoaderManager().initLoader(LOADER_RECENT, null, callbacks);

        reportsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TextView appNameTextView = (TextView) view.findViewById(R.id.app_name);

                Intent intent = new Intent(getActivity(), LeaksAppHistoryActivity.class);
                intent.putExtra(LeaksAppHistoryActivity.INTENT_EXTRA_PACKAGE_NAME,
                        (String) appNameTextView.getTag());
                intent.putExtra(LeaksAppHistoryActivity.INTENT_EXTRA_APP_NAME,
                        appNameTextView.getText());
                startActivity(intent);
            }
        });

        RadioGroup reportGroup = (RadioGroup) view.findViewById(R.id.report_group);
        reportGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                final int NO_SELECTION = -1;
                for (int j = 0; j < radioGroup.getChildCount(); j++) {
                    final ToggleButton view = (ToggleButton) radioGroup.getChildAt(j);
                    view.setChecked(view.getId() == i);
                }

                if (i != NO_SELECTION && i != radioGroup.getId()) {

                    ToggleButton checkedButton = (ToggleButton) radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
                    if (checkedButton != null) {
                        // reset loader
                        resetLoaderBasedOnRadioButtonId(checkedButton.getId());

                        // update the shared preferences
                        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PreferenceTags.PREFS_TAG, Context.MODE_PRIVATE);
                        sharedPreferences.edit().putInt(PreferenceTags.PREFS_LEAK_REPORT_SORT, checkedButton.getId()).apply();
                    }
                }
            }
        });

        // init the sort of the list based on user preferences
        SharedPreferences sp = getContext().getSharedPreferences(PreferenceTags.PREFS_TAG, Context.MODE_PRIVATE);
        int sortType = sp.getInt(PreferenceTags.PREFS_LEAK_REPORT_SORT, R.id.most_recent);
        //Log.d(TAG, "sort type passed in : " + sortType);
        reportGroup.check(sortType);

        leaksChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // reset the view when there is new data
                if (intent.getAction().equals(PrivacyDB.DB_LEAK_CHANGED)) {
                    refreshView();
                }
            }
        };

        leaksFilter = new IntentFilter(PrivacyDB.DB_LEAK_CHANGED);

        return view;
    }

    public void onResume() {
        super.onResume();
        refreshView();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(leaksChangedReceiver, leaksFilter);
    }

    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(leaksChangedReceiver);
    }
    private void resetLoaderBasedOnRadioButtonId(int radioId) {
        if (radioId == R.id.most_recent) {
            getLoaderManager().restartLoader(LOADER_RECENT, null, this);
        } else if (radioId == R.id.last_week) {
            getLoaderManager().restartLoader(LOADER_WEEKLY, null, this);
        } else if (radioId == R.id.last_month) {
            getLoaderManager().restartLoader(LOADER_MONTHLY, null, this);
        }
    }

    private void refreshView() {
        SharedPreferences sp = getContext().getSharedPreferences(PreferenceTags.PREFS_TAG, Context.MODE_PRIVATE);
        int sortType = sp.getInt(PreferenceTags.PREFS_LEAK_REPORT_SORT, R.id.most_recent);
        resetLoaderBasedOnRadioButtonId(sortType);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_RECENT:
                return new PrivacyLeaksReportCursorLoader(getContext(), PrivacyLeaksReportCursorLoader.RECENT_SORT);
            case LOADER_WEEKLY:
                return new PrivacyLeaksReportCursorLoader(getContext(), PrivacyLeaksReportCursorLoader.WEEKLY_SORT);
            case LOADER_MONTHLY:
                return new PrivacyLeaksReportCursorLoader(getContext(), PrivacyLeaksReportCursorLoader.MONTHLY_SORT);
            default:
                return new PrivacyLeaksReportCursorLoader(getContext(), PrivacyLeaksReportCursorLoader.RECENT_SORT);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
