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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.activity.uielements.ApplicationLogListAdapter;
import edu.uci.calit2.anteater.client.android.analysis.ApplicationFilter;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ApplicationsForLoggingFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ApplicationsForLoggingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ApplicationsForLoggingFragment extends Fragment {
    public static final String SHARED_PREFS_SELECTOR_TAG = "APPLICAGTIONSFORLOGGING_PREFS";
    public static final String SHARED_PREFS_SELECTOR_KEY = "edu.uci.calit2.anteater.APPLICATIONSFORLOGGING_SYSTEM_APPS_INCLUDED" ;


    ArrayList<ApplicationInfo> installedApps = new ArrayList<ApplicationInfo>();
    ApplicationLogListAdapter mAdapter;

    private Switch systemAppsSwitch;

    private OnFragmentInteractionListener mListener;
    private PackageManager pm = null;

    public ApplicationsForLoggingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment AboutFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ApplicationsForLoggingFragment newInstance(String param1, String param2) {
        ApplicationsForLoggingFragment fragment = new ApplicationsForLoggingFragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_applications_for_logging, container, false);
        // Get a list of installed apps that are visible in the app menu.
        pm = getContext().getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> packages = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);

        for(ResolveInfo app : packages){
            // Don't include our own app.
            if(!app.activityInfo.applicationInfo.packageName.equals(getContext().getApplicationContext().getPackageName())){
                installedApps.add(app.activityInfo.applicationInfo);
            }
        }

        mAdapter = new ApplicationLogListAdapter(getContext(), installedApps, pm);
        ListView listView = (ListView) view.findViewById(R.id.application_list);
        View headerView = inflater.inflate(R.layout.content_applications_for_logging_header, null);
        listView.addHeaderView(headerView, null, false);
        listView.setAdapter(mAdapter);

        systemAppsSwitch = (Switch) view.findViewById(R.id.system_app_switch);
        systemAppsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (systemAppsSwitch.isChecked()) {
                    ApplicationFilter.getInstance(getContext()).setSystemAppsForLogging(pm, installedApps);
                } else {
                    ApplicationFilter.getInstance(getContext()).clearSystemAppsFromLogging(pm, installedApps);
                }
            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Add your menu entries here
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_applications_for_logging, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.menu_option_select_all:
                ApplicationFilter.getInstance(getContext()).enableAllApplicationsLogging(pm, installedApps, systemAppsSwitch.isChecked());
                mAdapter.notifyDataSetChanged();;
                return true;
            case R.id.menu_option_select_none:
                ApplicationFilter.getInstance(getContext()).disableAllApplicationsLogging(pm, installedApps, systemAppsSwitch.isChecked());
                mAdapter.notifyDataSetChanged();;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences prefs = getContext().getSharedPreferences(SHARED_PREFS_SELECTOR_TAG, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(SHARED_PREFS_SELECTOR_KEY, systemAppsSwitch.isChecked()).apply();

        // Save currently Selected Apps to Shared Prefs
        prefs = getContext().getSharedPreferences(ApplicationFilter.SHARED_PREFS_FILTER_TAG, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(ApplicationFilter.SHARED_PREFS_FILTERED_APPS_KEY, ApplicationFilter.getInstance(getActivity()).getFilteredApps()).apply();

    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isSystemAppsIncluded = getContext().getSharedPreferences(SHARED_PREFS_SELECTOR_TAG, Context.MODE_PRIVATE).getBoolean(SHARED_PREFS_SELECTOR_KEY, false);
        systemAppsSwitch.setChecked(isSystemAppsIncluded);

    }

}
