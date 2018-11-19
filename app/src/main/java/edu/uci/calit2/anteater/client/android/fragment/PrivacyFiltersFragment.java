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
package edu.uci.calit2.anteater.client.android.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.activity.PrivacyFiltersActivity;
import edu.uci.calit2.anteater.client.android.activity.VpnStarterUtils;
import edu.uci.calit2.anteater.client.android.activity.uielements.PrivacyFilterListAdapter;
import edu.uci.calit2.anteater.client.android.activity.uielements.PrivacyFiltersCursorLoader;
import edu.uci.calit2.anteater.client.android.analysis.ActionReceiver;
import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.anteater.client.android.util.PreferenceTags;
import edu.uci.calit2.antmonitor.lib.vpn.VpnController;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PrivacyFiltersFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class PrivacyFiltersFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> ,
        PrivacyFilterListAdapter.OnAdapterInteractionListener {

    public static final int LOADER_PRIVACY_FILTERS = 0;

    PrivacyFilterListAdapter mAdapter;
    private OnFragmentInteractionListener mListener;
    boolean isGlobal = true;

    BroadcastReceiver privacyFiltersChangedReceiver;
    private IntentFilter privacyFiltersFilter;

    public PrivacyFiltersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    public void onResume() {
        super.onResume();
        refreshPrivacyFilters();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(privacyFiltersChangedReceiver, privacyFiltersFilter);
    }

    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(privacyFiltersChangedReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            isGlobal = args.getBoolean(PrivacyFiltersActivity.INTENT_GLOBAL_FILTER, true);
        }
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_privacy_filters, container, false);
        mAdapter = new PrivacyFilterListAdapter(getContext(), null, false, isGlobal, this);
        ListView listView = (ListView) view.findViewById(R.id.privacy_filters_list);
        setListHeaderView(listView, inflater);
        listView.setAdapter(mAdapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                final View coordinatorLayoutView = getActivity().findViewById(R.id.coordinatorLayout);
                final TextView textView = (TextView) view.findViewById(R.id.privacy_filters_label);
                Spinner spinner = (Spinner) view.findViewById(R.id.protection_lvl);
                final long filterId = (long)spinner.getTag();
                if (textView != null && filterId > -1) {
                    String text = getResources().getString(R.string.privacy_filters_remove, textView.getText());
                    Snackbar
                            .make(coordinatorLayoutView, text, Snackbar.LENGTH_LONG)
                            .setAction(R.string.yes, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    PrivacyDB database = PrivacyDB.getInstance(getContext());
                                    database.deleteFilterAsyncTask(getContext(), new PrivacyDB.OnFilterUpdateListener() {
                                        @Override
                                        public void onFilterUpdateDone() {
                                            refreshPrivacyFilters();
                                        }
                                    }, filterId);

                                }
                            })
                            .show();
                    return true;
                }

                return false;
            }
        });
        getLoaderManager().initLoader(LOADER_PRIVACY_FILTERS, null, this);
        initTLSSwitch(view);

        privacyFiltersChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // reset the view when privacy filters change
                if (intent.getAction().equals(PrivacyDB.DB_FILTER_CHANGED)) {
                    refreshPrivacyFilters();
                }
            }
        };

        privacyFiltersFilter = new IntentFilter(PrivacyDB.DB_FILTER_CHANGED);

        return view;
    }

    protected void initTLSSwitch(View view) {
        if (isGlobal) {
            Switch tlsSwitch = (Switch) view.findViewById(R.id.tls_switch);
            tlsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    try {
                        VpnController.setSSLBumpingEnabled(getActivity(), b);
                    } catch (IllegalStateException e) {
                        compoundButton.setChecked(false);
                        VpnStarterUtils.installSSLCert(getActivity());
                    }
                }
            });
            // Set button state according to user prefs
            SharedPreferences sp = getActivity().getSharedPreferences(PreferenceTags.PREFS_TAG,
                    Context.MODE_PRIVATE);
            boolean sslEnabled = sp.getBoolean(PreferenceTags.PREFS_SSL_BUMPING, false);
            tlsSwitch.setChecked(sslEnabled);
        }
    }

    protected void setListHeaderView(ListView listView, LayoutInflater inflater) {
        View headerView = isGlobal ?
                inflater.inflate(R.layout.content_privacy_filters_header, null) :
                inflater.inflate(R.layout.content_privacy_filters_app_header, null);
        listView.addHeaderView(headerView, null, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (isGlobal) {
            inflater.inflate(R.menu.menu_privacy_filters, menu);
        } else {
            inflater.inflate(R.menu.menu_privacy_filters_app, menu);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PrivacyDB database;

        // handle item selection
        switch (item.getItemId()) {
            case R.id.menu_option_select_all:
                database = PrivacyDB.getInstance(getActivity());
                database.enableAllGlobalFiltersAsyncTask(getActivity(), new PrivacyDB.OnFilterUpdateListener() {
                    @Override
                    public void onFilterUpdateDone() {
                        refreshPrivacyFilters();
                    }
                });
                return true;
            case R.id.menu_option_select_none:
                database = PrivacyDB.getInstance(getActivity());
                database.disableAllGlobalFiltersAsyncTask(getActivity(), new PrivacyDB.OnFilterUpdateListener() {
                    @Override
                    public void onFilterUpdateDone() {
                        refreshPrivacyFilters();
                    }
                });
                return true;
            case R.id.menu_option_clear_filters:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.clear_privacy_filters_confirm_alert)
                        .setTitle(R.string.are_you_sure);
                builder.setPositiveButton(R.string.ok, new  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PrivacyDB db = PrivacyDB.getInstance(getActivity());
                        db.clearPrivacyFiltersAsyncTask(getContext(), new PrivacyDB.OnFilterUpdateListener() {
                            @Override
                            public void onFilterUpdateDone() {
                                refreshPrivacyFilters();
                                clearAppFiltersDialogDone();
                            }
                        });
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

    private void clearAppFiltersDialogDone() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.clear_privacy_filters_alert)
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

        if (isGlobal) {
            // Save preferences
            SharedPreferences.Editor editor = getActivity().
                    getSharedPreferences(PreferenceTags.PREFS_TAG, Context.MODE_PRIVATE).edit();
            editor.putBoolean(PreferenceTags.PREFS_SSL_BUMPING,
                    VpnController.getSSLBumpingEnabled());
            editor.apply();
        }

    }

    public void refreshPrivacyFilters() {
        getLoaderManager().restartLoader(PrivacyFiltersFragment.LOADER_PRIVACY_FILTERS, null, this);
    }

    @Override
    public void adapterCallsRefreshCursor() {
        refreshPrivacyFilters();
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

    // TODO V2: For the following onCreateLoader :
    // use http://stackoverflow.com/questions/7182485/cursorloader-usage-without-contentprovider
    // since we have no contentprovider
    // Also https://developer.android.com/guide/topics/ui/layout/listview.html
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new PrivacyFiltersCursorLoader(getContext(), isGlobal);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
