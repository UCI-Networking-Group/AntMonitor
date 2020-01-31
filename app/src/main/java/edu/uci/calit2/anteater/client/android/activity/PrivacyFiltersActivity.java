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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.analysis.ActionReceiver;
import edu.uci.calit2.anteater.client.android.analysis.AnalysisHelper;
import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.anteater.client.android.fragment.PrivacyFilterDialogFragment;
import edu.uci.calit2.anteater.client.android.fragment.PrivacyFiltersFragment;
import edu.uci.calit2.antmonitor.lib.vpn.VpnController;

/**
 * @author Hieu Le
 */
public class PrivacyFiltersActivity extends AppCompatActivity implements
        PrivacyFilterDialogFragment.PrivacyFilterDialogFragmentListener,
        PrivacyFiltersFragment.OnFragmentInteractionListener {

    public static final String INTENT_GLOBAL_FILTER = "INTENT_GLOBAL_FILTER";
    private PrivacyFiltersFragment privacyFiltersFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_filters);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // set the back arrow
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // figure out whether we are showing app specific or global filters
        Intent intent = getIntent();
        boolean isGlobal = intent.getBooleanExtra(INTENT_GLOBAL_FILTER, true);

        FloatingActionButton addPrivacyFilterButton = (FloatingActionButton) findViewById(R.id.add_privacy_filter_button);

        if (isGlobal) {
            addPrivacyFilterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DialogFragment dialog = new PrivacyFilterDialogFragment();
                    dialog.show(getSupportFragmentManager(), "PrivacyFilterDialogFragment");
                }
            });

        } else {
            // hide the add button
            addPrivacyFilterButton.setVisibility(View.GONE);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getResources().getString(R.string.privacy_filters_app));
            }
        }

        // default fragment is realtimefragment
        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            privacyFiltersFragment = new PrivacyFiltersFragment();
            args.putBoolean(PrivacyFiltersActivity.INTENT_GLOBAL_FILTER, isGlobal);
            privacyFiltersFragment.setArguments(args);
            android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_layout, privacyFiltersFragment);
            fragmentTransaction.commit();
        }

        if (savedInstanceState != null) {
            refreshPrivacyFilters();
        }
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
    protected void onStop() {
        // rebuild searchStrings
        AnalysisHelper.startRebuildSearchTree(this);
        super.onStop();
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        TextInputLayout filterLabelTextInputLayout = (TextInputLayout) dialog.getDialog().findViewById(R.id.privacy_filters_label);
        TextInputLayout filterValueTextInputLayout = (TextInputLayout) dialog.getDialog().findViewById(R.id.privacy_filters_value);

        String filterLabel = filterLabelTextInputLayout.getEditText().getText().toString();
        String filterValue = filterValueTextInputLayout.getEditText().getText().toString();

        PrivacyDB database = PrivacyDB.getInstance(this);
        database.addGlobalFilterAsyncTask(dialog.getContext(), null, filterValue, filterLabel, true, ActionReceiver.ACTION_HASH, true);

        refreshPrivacyFilters();
    }

    protected void refreshPrivacyFilters() {
        if (privacyFiltersFragment != null) {
            privacyFiltersFragment.refreshPrivacyFilters();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == VpnController.REQUEST_INSTALL_CERT) {
            // if result is ok and we are on the view with tls switch, then turn it on
            if (result == RESULT_OK) {
                Switch tlsSwitch = (Switch) this.findViewById(R.id.tls_switch);
                if (tlsSwitch != null && !tlsSwitch.isChecked()) {
                    tlsSwitch.setChecked(true);
                }
            }
        }
    }
}
