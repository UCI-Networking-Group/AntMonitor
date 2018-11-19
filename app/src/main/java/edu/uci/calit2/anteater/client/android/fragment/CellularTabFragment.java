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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.signals.CellularMonitor;

/**
 * @author Emmanouil Alimpertis
 */
public class CellularTabFragment extends Fragment {

    CellularMonitor myCellularMonitor;
    private final String TAG = CellularMonitor.class.getSimpleName();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.signals_tab2_cellular_view, container, false);
        myCellularMonitor = CellularMonitor.getInstance(this.getActivity().getApplicationContext());

        TextView networkCarrierName= (TextView)v.findViewById(R.id.netWorkCarrierValue_id);
        networkCarrierName.setText(myCellularMonitor.getNetworkCarrierName());

        TextView myNetworkTypeLabel = (TextView)v.findViewById(R.id.netWorkTypeValue_id);
        myNetworkTypeLabel.setText(myCellularMonitor.getNetworkCarrierName());

        TextView mccLabel = (TextView)v.findViewById(R.id.MCCValue_id);
        mccLabel.setText(myCellularMonitor.getMccStr());

        TextView mncLabel = (TextView)v.findViewById(R.id.MNCValue_id);
        mncLabel.setText(myCellularMonitor.getMncStr());

        TextView lacLabel = (TextView)v.findViewById(R.id.LACValue_id);
        lacLabel.setText(myCellularMonitor.getLacStr());

        TextView rsrpLabel = (TextView)v.findViewById(R.id.LTE_RSRP_Value_id);
       // rsrpLabel.setText(myCellularMonitor.getLteRsrpStr());

        TextView rsrqLabel = (TextView)v.findViewById(R.id.LTE_RSRQ_Value_id);
       // rsrqLabel.setText(myCellularMonitor.getLteRsrqStr());


        /*Check GSMCellLocation Class*/
        TextView cidLabel_raw= (TextView)v.findViewById(R.id.CIDValue_id);
        cidLabel_raw.setText(myCellularMonitor.getGSMCellID_raw());
        //System.out.println("\n\n" + Integer.toBinaryString(myCellularMonitor.getGSMCellId()) + "\n\n");

        TextView cidLabel= (TextView)v.findViewById(R.id.CIDConvertedValue_id);
        cidLabel.setText(myCellularMonitor.getGSMCellIDStr());

        String tmpCarrierName=myCellularMonitor.getNetworkCarrierName();

        if(tmpCarrierName.equals("HSPA")||tmpCarrierName.equals("HSPA+") || tmpCarrierName.equals("HSDPA") || tmpCarrierName.equals("HSUPA") ) {

            System.out.println("Labels should have updated");
            TextView hspaRssLabel = (TextView)v.findViewById(R.id.HSPA_Value_id);
            hspaRssLabel.setText(myCellularMonitor.getWcdmaRSS());
        }


        //debug to print RSS several Values from LTE radio environment
/*******
        String[] tmp=myCellularMonitor.getSignalStrengthValuesLTE();

        for (final String str:tmp){
            Log.d(TAG, str);
        }
*******/



/*
        TextView mccTextView=(TextView)v.findViewById(R.id.MCCValue_id);
        mccTextView.setText(String.valueOf(this.getMcc()));
        TextView mncTextView=(TextView)v.findViewById(R.id.MNCValue_id);
        mncTextView.setText(String.valueOf(this.getMnc()));
        TextView networkCarrier=(TextView) v.findViewById(R.id.netWorkCarrierValue_id);
        networkCarrier.setText(tm.getNetworkOperatorName());
        TextView lacTextView=(TextView)v.findViewById(R.id.LACValue_id);
        lacTextView.setText(String.valueOf(LAC));

        ////////

        /*Converted value of C-ID*/
        /* int CELLID_converted = ci & 0xffff;
        TextView tmp_ciLabel_conv= (TextView)v.findViewById(R.id.CIDConvertedValue_id);
        StringBuilder sb_2 = new StringBuilder();
        sb_2.append("");
        sb_2.append(CELLID_converted);
        tmp_ciLabel_conv.setText(sb_2.toString());
        */

        return v;
    }



}
