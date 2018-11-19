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

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.fragment.CellularTabFragment;
import edu.uci.calit2.anteater.client.android.fragment.WifiTabFragment;

/**
 * @author Emmanouil Alimpertis
 */
public class WirelessSignalsActivity extends FragmentActivity {

    private Menu signalsMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signals);


       TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout_id);
       ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager_id);

       viewPager.setAdapter(new SectionPagerAdapter(getSupportFragmentManager()));
       tabLayout.setupWithViewPager(viewPager);

        //debug of LocationMonitor

        /*final LocationMonitor myLocManager=new LocationMonitor(this);
        final CellularMonitor myCellMonitor=new CellularMonitor(this.getApplicationContext());

        HandlerThread hThread = new HandlerThread("HandlerThread");
        hThread.start();

        final Handler handler = new Handler(hThread.getLooper());
        final long fiveSec = 20 * 1000;

        Runnable eachMinute = new Runnable() {
            @Override
            public void run() {
                Log.i("SIGNALS", "READING DATA FROM ANDROID TELEPHONY");
                Log.i("SIGNALS", "my loc is: " + myLocManager.getLocationStr());
                Log.i("SIGNALS", "my net summary is: " + myCellMonitor.getNetworkSummary());
                handler.postDelayed(this, fiveSec);
            }
        };


// Schedule the first execution
        handler.postDelayed(eachMinute, fiveSec);
        */

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_signals, menu);
        signalsMenu = menu;
        return true;
    }

    private class SectionPagerAdapter extends FragmentPagerAdapter {

        public SectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new WifiTabFragment();
                case 1:
                    return new CellularTabFragment();
                //case 2:
                //    return new LocationMonitor();
                //case 3: Neither ready Nor extensively tested
                //    return new MapsTabFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Wi-Fi Info";
                case 1:
                    return "Cellular Info";
                //case 2:
                //     return "Location";
                //case 3: Neither ready Nor extensively tested
                //    return "Maps";
                default:
                    return "Cellular Info";
            }
        }
    }
}


