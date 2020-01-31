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
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import edu.uci.calit2.anteater.R;

/**
 * @author Emmanouil Alimpertis
 */
public class WifiTabFragment extends Fragment {

    private WifiManager wifiManager;
    private WifiReceiver receiverWifi;
    private List<ScanResult> wifiAPs_List;
    StringBuilder sb = new StringBuilder();
    private ArrayAdapter<String> wiFiAPs_adapter;
    private ArrayList<String> wifiAPsFormattedTextInfo_list;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.signals_tab1_wifi_view, container, false);
        //necessary objects for monitoring Wi-Fi

        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
        wifiAPs_List = wifiManager.getScanResults();
        receiverWifi = new WifiReceiver();

        //Objects for data booking
        wifiAPsFormattedTextInfo_list=new ArrayList<String>();
        wiFiAPs_adapter = new ArrayAdapter<String>(getActivity(),R.layout.list_item_wifi_ap_info,R.id.list_item_wifi_ap_textview,wifiAPsFormattedTextInfo_list);

        ScanResult scanresult;
        for(int i = 0; i < wifiAPs_List.size(); i++){
            scanresult = wifiAPs_List.get(i);
            wifiAPsFormattedTextInfo_list.add("BSSID: "+scanresult.BSSID+ " RSSI: "+scanresult.level + " Freq: "+scanresult.frequency);
        }

        //GUI objects
        ListView myWiFiList_view= (ListView) v.findViewById(R.id.listview_wifiAPs);
        myWiFiList_view.setAdapter(wiFiAPs_adapter);

        //register BroadcastReceiver
        this.getActivity().registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));


        return v;
    }

    @Override
    public void onPause() {
        this.getActivity().unregisterReceiver(receiverWifi);
        super.onPause();
    }

    @Override
    public void onResume() {
        this.getActivity().registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            wifiAPs_List = wifiManager.getScanResults();
            wifiAPsFormattedTextInfo_list.clear();

            ScanResult scanresult;
            for(int i = 0; i < wifiAPs_List.size(); i++){
                scanresult = wifiAPs_List.get(i);
                wifiAPsFormattedTextInfo_list.add("BSSID: "+scanresult.BSSID+ " RSSI: "+scanresult.level + " Freq: "+scanresult.frequency);
            }
            wiFiAPs_adapter.notifyDataSetChanged();
        }
    }
}
