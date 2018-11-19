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
package edu.uci.calit2.anteater.client.android.activity.uielements;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.analysis.ApplicationFilter;

/**
 * @author Hieu Le
 */
public class ApplicationLogListAdapter extends ArrayAdapter<ApplicationInfo> {
    private PackageManager pm;

    private static class ViewHolder {
        private TextView txtView;
        private ImageView imgView;
        private Switch applicationSwitch;
    }

    public ApplicationLogListAdapter(Context context, List<ApplicationInfo> app, PackageManager pm) {
        super(context, R.layout.app_list_item, app);
        this.pm = pm;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ApplicationLogListAdapter.ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.list_item_application_log, parent, false);

            viewHolder = new ApplicationLogListAdapter.ViewHolder();
            viewHolder.applicationSwitch = (Switch)convertView.findViewById(R.id.application_switch);
            viewHolder.imgView = (ImageView)convertView.findViewById(R.id.app_icon);
            viewHolder.txtView = (TextView)convertView.findViewById(R.id.application_name);
            convertView.setTag(viewHolder);

        } else {
            viewHolder =  (ApplicationLogListAdapter.ViewHolder) convertView.getTag();
        }


        ApplicationInfo item = getItem(position);
        if (item!= null) {
            viewHolder.txtView.setText(item.loadLabel(pm));
            viewHolder.imgView.setImageDrawable(item.loadIcon(pm));
            viewHolder.applicationSwitch.setOnCheckedChangeListener(null);
            viewHolder.applicationSwitch.setChecked(ApplicationFilter.getInstance(getContext()).getFilteredApps().contains(item.packageName));
        }

        viewHolder.applicationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                onClicked(position, viewHolder);
            }
        });

        return convertView;
    }

    private void onClicked(int position, ApplicationLogListAdapter.ViewHolder viewHolder){
        // Item clicked, add to filter.
        ApplicationInfo item = getItem(position);

        if (item != null) {
            if(viewHolder.applicationSwitch.isChecked()){
                // Add to filter
                ApplicationFilter.getInstance(getContext()).addToFilter(item.packageName);
            } else {
                // Remove from filter
                ApplicationFilter.getInstance(getContext()).removeFromFilter(item.packageName);
            }
        }

    }

}
