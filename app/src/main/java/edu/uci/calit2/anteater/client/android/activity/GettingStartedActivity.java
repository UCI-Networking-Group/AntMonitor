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
package edu.uci.calit2.anteater.client.android.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.ViewFlipper;

import edu.uci.calit2.anteater.BuildConfig;
import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.fragment.ApplicationsForLoggingFragment;
import edu.uci.calit2.anteater.client.android.fragment.GSAppFragment;
import edu.uci.calit2.anteater.client.android.fragment.GSContibuteFragment;
import edu.uci.calit2.anteater.client.android.fragment.GSMainAboutFragment;
import edu.uci.calit2.anteater.client.android.fragment.GSMainFragment;
import edu.uci.calit2.anteater.client.android.fragment.GSPrivacyFragment;
import edu.uci.calit2.anteater.client.android.fragment.PrivacyFiltersFragment;
import edu.uci.calit2.anteater.client.android.util.PermissionHelper;
import edu.uci.calit2.anteater.client.android.util.PreferenceTags;
import edu.uci.calit2.antmonitor.lib.vpn.VpnController;

public class GettingStartedActivity extends AppCompatActivity
    implements GSMainFragment.OnFragmentInteractionListener,
        GSMainAboutFragment.OnFragmentInteractionListener,
        GSAppFragment.OnFragmentInteractionListener,
        GSContibuteFragment.OnFragmentInteractionListener,
        GSPrivacyFragment.OnFragmentInteractionListener,
        PrivacyFiltersFragment.OnFragmentInteractionListener,
        ApplicationsForLoggingFragment.OnFragmentInteractionListener{

    ViewFlipper mViewFlipper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getting_started);
        mViewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);

        // don't let user come back here if its been completed already (when we are not building for debug)
        SharedPreferences sp = getSharedPreferences(PreferenceTags.PREFS_TAG, Context.MODE_PRIVATE);
        boolean gettingStartedCompleted = sp.getBoolean(PreferenceTags.PREFS_GETTING_STARTED_COMPLETE, false);
        if (!BuildConfig.DEBUG && gettingStartedCompleted) {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        PermissionHelper.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (mViewFlipper.getDisplayedChild() <= 0) {
            super.onBackPressed();
        } else {
            mViewFlipper.showPrevious();
        }
    }

    @Override
    public void showNextFragment(Fragment currentFragment) {

        View view = currentFragment.getView();
        if (view != null) {
            Button startAntmonitor = (Button) view.findViewById(R.id.start_antmonitor);
            if (startAntmonitor != null && startAntmonitor.getVisibility() != View.GONE) {
                Intent intent = new Intent(GettingStartedActivity.this, AntMonitorMainActivity.class);
                intent.putExtra(AntMonitorMainActivity.INTENT_START_ANTMONITOR, true);
                startActivity(intent);
                finish(); // must call finish so users cannot go back into this activity
            } else {
                mViewFlipper.showNext();
            }
        }
    }

    @Override
    public void showPreviousFragment(Fragment currentFragment) {
        if (currentFragment.getId() != R.id.main_fragment) {
            mViewFlipper.showPrevious();
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
                View currentView = mViewFlipper.getCurrentView();
                if (currentView != null) {
                    Switch tlsSwitch = (Switch) currentView.findViewById(R.id.tls_switch);
                    if (tlsSwitch != null && !tlsSwitch.isChecked()) {
                        tlsSwitch.setChecked(true);
                    }
                }
            }
        }
    }

}
