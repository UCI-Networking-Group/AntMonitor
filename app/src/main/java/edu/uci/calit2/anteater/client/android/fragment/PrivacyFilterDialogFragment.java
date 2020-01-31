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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.anteater.client.android.database.PrivacyFilterModel;

/**
 * @author Hieu Le
 */
public class PrivacyFilterDialogFragment extends DialogFragment {

    public interface PrivacyFilterDialogFragmentListener {
        public void onDialogPositiveClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    PrivacyFilterDialogFragmentListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.privacy_filters_dialog_title));
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_privacy_filter, null))
                // Add action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Leave empty
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PrivacyFilterDialogFragment.this.getDialog().cancel();
                    }
                });

        // create dialog
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = ((AlertDialog)dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TextInputLayout filterLabelTextInputLayout = (TextInputLayout) PrivacyFilterDialogFragment.this.getDialog().findViewById(R.id.privacy_filters_label);
                        TextInputLayout filterValueTextInputLayout = (TextInputLayout) PrivacyFilterDialogFragment.this.getDialog().findViewById(R.id.privacy_filters_value);

                        final String filterLabel = filterLabelTextInputLayout.getEditText().getText().toString();
                        final String filterValue = filterValueTextInputLayout.getEditText().getText().toString();

                        if (filterLabel.isEmpty()) {
                            filterLabelTextInputLayout.setError(getResources().getString(R.string.privacy_filters_dialog_filter_error));
                        } else {
                            filterLabelTextInputLayout.setError(null);
                        }

                        if (filterValue.isEmpty()) {
                            filterValueTextInputLayout.setError(getResources().getString(R.string.privacy_filters_dialog_filter_error));
                        } else {
                            filterValueTextInputLayout.setError(null);
                        }

                        if (filterLabelTextInputLayout.getError() == null) {
                            // check if global filter already exists for this label
                            // right now it is case sensitive
                            PrivacyDB database = PrivacyDB.getInstance(getContext());
                            PrivacyFilterModel globalFilterModel = database.convertToPrivacyFilterModel(database.getAnyPrivacyFilterByLabel(null, filterLabel));
                            if (globalFilterModel != null) {
                                filterLabelTextInputLayout.setError(getResources().getString(R.string.privacy_filters_dialog_filter_exists));
                            } else {
                                filterLabelTextInputLayout.setError(null);
                            }
                        }

                        // if both required fields are filled in
                        if (filterLabelTextInputLayout.getError() == null && filterValueTextInputLayout.getError() == null) {
                            mListener.onDialogPositiveClick(PrivacyFilterDialogFragment.this);
                            PrivacyFilterDialogFragment.this.getDialog().cancel();
                        }
                    }
                });
            }
        });


        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the PrivacyFilterDialogFragmentListener so we can send events to the host
            mListener = (PrivacyFilterDialogFragmentListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement PrivacyFilterDialogFragmentListener");
        }
    }
}
