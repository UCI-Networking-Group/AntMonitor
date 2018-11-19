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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.analysis.AnalysisHelper;
import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.anteater.client.android.device.Installation;
import edu.uci.calit2.anteater.client.android.fragment.AboutFragment;
import edu.uci.calit2.anteater.client.android.fragment.ContributeLogsFragment;
import edu.uci.calit2.anteater.client.android.fragment.ContributeSettingsFragment;
import edu.uci.calit2.anteater.client.android.fragment.GSMainAboutFragment;
import edu.uci.calit2.anteater.client.android.fragment.HelpFragment;
import edu.uci.calit2.anteater.client.android.fragment.PrivacyLeaksReportFragment;
import edu.uci.calit2.anteater.client.android.fragment.PrivacySettingsFragment;
import edu.uci.calit2.anteater.client.android.fragment.RealTimeFragment;
import edu.uci.calit2.anteater.client.android.util.PermissionHelper;
import edu.uci.calit2.anteater.client.android.util.PreferenceTags;
import edu.uci.calit2.antmonitor.lib.AntMonitorActivity;
import edu.uci.calit2.antmonitor.lib.logging.PacketConsumer;
import edu.uci.calit2.antmonitor.lib.vpn.IncPacketFilter;
import edu.uci.calit2.antmonitor.lib.vpn.OutPacketFilter;
import edu.uci.calit2.antmonitor.lib.vpn.VpnController;
import edu.uci.calit2.antmonitor.lib.vpn.VpnState;

/**
 * @author Hieu Le
 */
public class AntMonitorMainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        RealTimeFragment.OnFragmentInteractionListener,
        PrivacyLeaksReportFragment.OnFragmentInteractionListener,
        ContributeLogsFragment.OnFragmentInteractionListener,
        HelpFragment.OnFragmentInteractionListener,
        AboutFragment.OnFragmentInteractionListener,
        PrivacySettingsFragment.OnFragmentInteractionListener,
        ContributeSettingsFragment.OnFragmentInteractionListener,
        GSMainAboutFragment.OnFragmentInteractionListener,
        View.OnClickListener,
        AntMonitorActivity {

    private static final String TAG = "AntMonitorActivity";

    // See ANTMONITOR-98
    public static final boolean ENABLE_CONTRIBUTION_SECTIONS = false;

    public static final String INTENT_START_ANTMONITOR = "INTENT_START_ANTMONITOR";

    /** The controller that will be used to start/stop the VPN service */
    private VpnController mVpnController;

    /** Switch that allows users to turn the VPN service on/off */
    private Switch antMonitorSwitch;

    /** Indicates whether or not we got here from Getting Started */
    private boolean initialStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        /* Uncomment if you want strictmode logs */
        /*
        if (BuildConfig.DEBUG) {
            // Activate StrictMode
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    // alternatively .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    // alternatively .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
        }*/

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ant_monitor_main);

        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    public void onBackStackChanged() {
                        updateAfterBackStackChange();
                    }
                });


        // Initialize the controller
        mVpnController = VpnController.getInstance(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // if contribution is not enabled
        if (!AntMonitorMainActivity.ENABLE_CONTRIBUTION_SECTIONS) {
            Menu menu = navigationView.getMenu();
            MenuItem contributeMenuItem = menu.findItem(R.id.nav_contribute);
            contributeMenuItem.setEnabled(AntMonitorMainActivity.ENABLE_CONTRIBUTION_SECTIONS);
            contributeMenuItem.setTitle(getResources().getString(R.string.main_title_contribute_off));
        }

        View headerView = navigationView.getHeaderView(0);

        antMonitorSwitch = (Switch) headerView.findViewById(R.id.antMonitor_switch);

        // Connect and disconnect buttons are disabled by default.
        // We update enabled state when we receive a broadcast about VPN state from the service.
        // Or when the service connection is established.
        updateAntmonitorSwitch(false, false);

        // Use click events only, for simplicity
        antMonitorSwitch.setOnClickListener(this);
        // Disable swipe
        antMonitorSwitch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return event.getActionMasked() == MotionEvent.ACTION_MOVE;
            }
        });

        TextView antMonitorIDTextView = (TextView) headerView.findViewById(R.id.antMonitorId);
        updateAntMonitorID(antMonitorIDTextView);

        // default fragment is realtimefragment
        if (savedInstanceState == null) {
            String title = getResources().getString(R.string.main_title_real_time);
            RealTimeFragment fragment = new RealTimeFragment();
            android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.main_frame, fragment, title);
            fragmentTransaction.addToBackStack(title);
            fragmentTransaction.commit();
            toolbar.setTitle(title);
            navigationView.setCheckedItem(R.id.nav_real_time);
        }

        // update getting started complete so we do not ever go back to getting started
        SharedPreferences sp = getSharedPreferences(PreferenceTags.PREFS_TAG, Context.MODE_PRIVATE);
        boolean gettingStartedCompleted = sp.getBoolean(PreferenceTags.PREFS_GETTING_STARTED_COMPLETE, false);
        if (!gettingStartedCompleted) {
            sp.edit().putBoolean(PreferenceTags.PREFS_GETTING_STARTED_COMPLETE, true).apply();
        }

        // Remember if we got here from Getting Started
        Intent intent = getIntent();
        initialStart = intent.getBooleanExtra(INTENT_START_ANTMONITOR, false);

        // rebuild searchStrings
        AnalysisHelper.startRebuildSearchTree(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        PermissionHelper.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void updateAfterBackStackChange() {
        // current fragment showing after back pressing
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_frame);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (fragment != null) {
            // update title bar
            toolbar.setTitle(fragment.getTag());

            // update navigation
            if (fragment instanceof RealTimeFragment) {
                navigationView.setCheckedItem(R.id.nav_real_time);
            } else if (fragment instanceof PrivacyLeaksReportFragment) {
                navigationView.setCheckedItem(R.id.nav_privacy);
            } else if (fragment instanceof ContributeLogsFragment) {
                navigationView.setCheckedItem(R.id.nav_contribute);
            } else if (fragment instanceof  HelpFragment) {
                navigationView.setCheckedItem(R.id.nav_help);
            } else if (fragment instanceof  AboutFragment) {
                navigationView.setCheckedItem(R.id.nav_about);
            } else if (fragment instanceof PrivacySettingsFragment) {
                navigationView.setCheckedItem(R.id.nav_privacy_settings);
            } else if (fragment instanceof  ContributeSettingsFragment) {
                navigationView.setCheckedItem(R.id.nav_contribute_settings);
            } else {
                navigationView.setCheckedItem(R.id.nav_real_time);
            }
        }
    }

    /**
     * Convenience method for starting the VPN service
     */
    private void startAntMonitor() {
        // Check if we are connected to the internet to see if it makes sense to
        // establish the VPN connection
        if (!mVpnController.isConnectedToInternet()) {
            // Not connected to internet: inform the user and do nothing
            Toast.makeText(AntMonitorMainActivity.this, R.string.no_service,
                    Toast.LENGTH_LONG).show();

            // Set button states appropriately
            updateAntmonitorSwitch(true, false);
            return;
        }

        // Check to see if we have VPN rights from the user
        Intent intent = android.net.VpnService.prepare(AntMonitorMainActivity.this);
        if (intent != null) {
            // Ask user for VPN rights. If they are granted,
            // onActivityResult will be called with RESULT_OK
            startActivityForResult(intent, VpnController.REQUEST_VPN_RIGHTS);
        } else {
            // VPN rights were granted before, attempt a connection
            onActivityResult(VpnController.REQUEST_VPN_RIGHTS, RESULT_OK, null);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.antMonitor_switch){
            antMonitorSwitch.setEnabled(false);

            // User wants to connect:
            if (antMonitorSwitch.isChecked()) {
                startAntMonitor();
            } else {
                // User wants to disconnect
                mVpnController.disconnect();
            }
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == VpnController.REQUEST_VPN_RIGHTS) {
            // Check if the user granted us rights to VPN
            if (result == Activity.RESULT_OK) {
                // If so, we can attempt a connection

                // Get consumers/filters specific to AntMonitor
                PacketConsumer incConsumer = VpnStarterUtils.getIncConsumer(this);
                PacketConsumer outConsumer = VpnStarterUtils.getOutConsumer(this);
                OutPacketFilter outFilter = VpnStarterUtils.getOutFilter(this);
                IncPacketFilter incFilter = VpnStarterUtils.getIncFilter(this);
                VpnStarterUtils.setSSLBumping(this);

                mVpnController.connect(incFilter, outFilter, incConsumer, outConsumer);
            } else {
                // enable the switch again so user can try again
                antMonitorSwitch.setEnabled(true);
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.vpn_rights_needed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Updates this {@code Activity} to reflect a change in the state of the VPN connection.
     * Receiving this state change means we successfully bounded to the VPN service.
     * @param vpnState The new state of the VPN connection.
     */
    @Override
    public void onVpnStateChanged(VpnState vpnState) {
        boolean isConnecting = vpnState == VpnState.CONNECTING;
        boolean isConnected = vpnState == VpnState.CONNECTED;

        // Now that we are bound, we can check if we should connect automatically -> check
        // if we got here from getting started and we need to connect based on the current state
        if (!isConnecting && !isConnected && initialStart) {
            // Disable button during connection process
            updateAntmonitorSwitch(false, false);
            startAntMonitor();

            // In future calls to this method, do not attempt to re-start automatically
            initialStart = false;
        } else {
            updateAntmonitorSwitch(!isConnecting, isConnected);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to the service so we can receive VPN status updates (see onVpnStateChanged)
        mVpnController.bind();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service as we no longer need to receive VPN status updates,
        // since we don't have to change the button to enabled/disabled, etc.
        mVpnController.unbind();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
                finish();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.ant_monitor_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_option_privacy_settings) {
            MenuItem menuItem = navigationView.getMenu().findItem(R.id.nav_privacy_settings);
            if (menuItem != null) {
                this.onNavigationItemSelected(menuItem);
                navigationView.setCheckedItem(R.id.nav_privacy_settings);
                return true;
            }
        } else if (id == R.id.menu_option_contribute_settings) {
            MenuItem menuItem = navigationView.getMenu().findItem(R.id.nav_contribute_settings);
            if (menuItem != null) {
                this.onNavigationItemSelected(menuItem);
                navigationView.setCheckedItem(R.id.nav_contribute_settings);
                return true;
            }
        } else if (id == R.id.menu_option_help) {
            MenuItem menuItem = navigationView.getMenu().findItem(R.id.nav_help);
            if (menuItem != null) {
                this.onNavigationItemSelected(menuItem);
                navigationView.setCheckedItem(R.id.nav_help);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        String title = "";
        Class fragmentKlass = null;
        if (id == R.id.nav_real_time) {
            title = getResources().getString(R.string.main_title_real_time);
            fragmentKlass = RealTimeFragment.class;
        } else if (id == R.id.nav_privacy) {
            title = getResources().getString(R.string.main_title_privacy);
            fragmentKlass = PrivacyLeaksReportFragment.class;
        } else if (id == R.id.nav_contribute) {
            title = getResources().getString(R.string.main_title_contribute);
            fragmentKlass = ContributeLogsFragment.class;
        } else if (id == R.id.nav_help) {
            title = getResources().getString(R.string.main_title_help);
            fragmentKlass = HelpFragment.class;
        } else if (id == R.id.nav_about) {
            title = getResources().getString(R.string.main_title_about);
            fragmentKlass = AboutFragment.class;
        } else if (id == R.id.nav_privacy_settings) {
            title = getResources().getString(R.string.main_title_privacy_settings);
            fragmentKlass = PrivacySettingsFragment.class;
        } else if (id == R.id.nav_contribute_settings) {
            title = getResources().getString(R.string.main_title_contribute_settings);
            fragmentKlass = ContributeSettingsFragment.class;
        }
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame);
        if (fragmentKlass != null && currentFragment.getClass().equals(fragmentKlass)) {
            // if current fragment is the same, then do nothing
            return true;
        }

        if (title != null) {
            boolean fragmentPopped = getSupportFragmentManager().popBackStackImmediate(title, 0);

            // if the fragment is not in the backstack, then add it.
            if (!fragmentPopped && fragmentKlass != null && getSupportFragmentManager().findFragmentByTag(title) == null) {
                try {
                    //Log.d(TAG, "Adding to backstack: " + title);
                    Fragment fragment = null;
                    android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragment  = (Fragment) fragmentKlass.newInstance();
                    fragmentTransaction.replace(R.id.main_frame, fragment, title);
                    fragmentTransaction.addToBackStack(title);
                    fragmentTransaction.commit();
                    toolbar.setTitle(title);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    /**
     * Convenience method for setting the switch state based on the VPN state
     * @param enabled
     * @param checked
     */
    private void updateAntmonitorSwitch (boolean enabled, boolean checked) {
        antMonitorSwitch.setEnabled(enabled);
        antMonitorSwitch.setChecked(checked);
    }

    private void updateAntMonitorID (TextView textView) {
        String id = Installation.id(this);
        if (id != null){
            textView.setText(id);
        } else {
            textView.setText(getResources().getString(R.string.antmonitor_id));
        }
    }

    public void onPrivacyReportsToggle(View view) {
        RadioGroup radioGroup = (RadioGroup) view.getParent();
        radioGroup.clearCheck();
        radioGroup.check(view.getId());
    }
}
