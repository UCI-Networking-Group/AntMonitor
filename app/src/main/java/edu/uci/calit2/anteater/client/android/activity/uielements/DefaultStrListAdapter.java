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
package edu.uci.calit2.anteater.client.android.activity.uielements;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import java.util.HashSet;
import java.util.List;

import edu.uci.calit2.anteater.R;

/**
 * An adapter for displaying default PII strings to the user
 * @author Anastasia Shuba
 */
public class DefaultStrListAdapter extends ArrayAdapter<String> {

    /** Contains strings that the user checked */
    HashSet<String> checkedStrs;

    class ViewHolder {
        CheckedTextView txtView;
    }

    public DefaultStrListAdapter(Context context, List<String> strings, HashSet<String> checkedStrs) {
        super(context, R.layout.string_list_item, strings);
        this.checkedStrs = checkedStrs;
    }

    ViewHolder createOrReuseView(View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.string_list_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.txtView = (CheckedTextView)convertView.findViewById(R.id.chkStr);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        return viewHolder;
    }



    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.string_list_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.txtView = (CheckedTextView)convertView.findViewById(R.id.chkStr);

            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String item = getItem(position);
        if (item!= null) {
            viewHolder.txtView.setText(Html.fromHtml(item));
            viewHolder.txtView.setChecked(checkedStrs.contains(item));
        }

        viewHolder.txtView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Item clicked, add to filter.
                String item = getItem(position);

                viewHolder.txtView.setChecked(!viewHolder.txtView.isChecked());

                if(viewHolder.txtView.isChecked()){
                    // Add to filter
                    checkedStrs.add(item); //Needs to be diff from displayed strings
                } else {
                    // Remove from filter
                    checkedStrs.remove(item);
                }
            }
        });

        return convertView;
    }
}
