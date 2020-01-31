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
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.activity.uielements.ContributeLogsHistoryCursorLoader;
import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.anteater.client.android.util.UIHelper;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ContributeLogsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ContributeLogsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContributeLogsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter mAdapter;
    private OnFragmentInteractionListener mListener;
    private static final int LOADER_CONTRIBUTE_LOGS = 0;

    public ContributeLogsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment ContributeLogsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ContributeLogsFragment newInstance(String param1, String param2) {
        ContributeLogsFragment fragment = new ContributeLogsFragment();
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
        inflater.inflate(R.menu.menu_contribution_logs, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // handle item selection
        switch (item.getItemId()) {
            case R.id.menu_option_clear_logs_history:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.clear_logs_statistics_confirm_alert)
                        .setTitle(R.string.are_you_sure);
                builder.setPositiveButton(R.string.ok, new  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PrivacyDB database = PrivacyDB.getInstance(getActivity());
                        database.clearUploadHistory();
                        database.close();
                        clearUploadHistoryDialogDone();
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
            case R.id.menu_option_clear_logs:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
                builder1.setMessage(R.string.clear_logs_confirm_alert)
                        .setTitle(R.string.are_you_sure);
                builder1.setPositiveButton(R.string.ok, new  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        UIHelper.clearLogs(getActivity());
                        clearLogsDialogDone();
                    }
                });
                builder1.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog1 = builder1.create();
                dialog1.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearLogsDialogDone() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.clear_logs_alert)
                .setTitle(R.string.completed);
        builder.setPositiveButton(R.string.ok, new  DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void clearUploadHistoryDialogDone() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.clear_logs_statistics_alert)
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contribute_logs, container, false);


        ListView historyListView = (ListView) view.findViewById(R.id.upload_history_list);
        View listHeaderView = inflater.inflate(R.layout.fragment_contribute_logs_header, null);
        historyListView.addHeaderView(listHeaderView, null, false);
        // TODO V2: Change the column name later (for history list items)
        // the columns refer to the data that will be coming from the cursor and how it ties to the layout elements
        String[] fromColumns = {PrivacyDB.COLUMN_COUNT, PrivacyDB.COLUMN_TIME};
        int[] toViews = {R.id.upload_info, R.id.timestamp}; // The TextViews in list_item_upload_history

        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.list_item_upload_history, null,
                fromColumns, toViews, 0);
        historyListView.setAdapter(mAdapter);
        getLoaderManager().initLoader(LOADER_CONTRIBUTE_LOGS, null, this);

        TextView uploadNow = (TextView) view.findViewById(R.id.upload_now);
        uploadNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                UIHelper.uploadLogs(getActivity(), getContext());
                resetLoaderContributeLogsHistory();
                // if successful
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.logs_uploaded_alert)
                        .setTitle(R.string.completed);
                builder.setPositiveButton(R.string.ok, new  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        return view;
    }

    public void resetLoaderContributeLogsHistory() {
        getLoaderManager().restartLoader(LOADER_CONTRIBUTE_LOGS, null, this);
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

    // TODO V2: For the following onCreateLoader :
    // use http://stackoverflow.com/questions/7182485/cursorloader-usage-without-contentprovider
    // since we have no contentprovider
    // Also https://developer.android.co m/guide/topics/ui/layout/listview.html
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new ContributeLogsHistoryCursorLoader(getContext());
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
