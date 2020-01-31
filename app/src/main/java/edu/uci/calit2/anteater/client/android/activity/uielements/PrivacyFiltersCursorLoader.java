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
import android.database.Cursor;

import edu.uci.calit2.anteater.client.android.database.PrivacyDB;

/**
 * @author Hieu Le
 */
public class PrivacyFiltersCursorLoader extends SimpleCursorLoader {

    private final Context context;
    private boolean globalFilters = true;

    public PrivacyFiltersCursorLoader(Context context) {
        super(context);
        this.context = context;
    }

    public PrivacyFiltersCursorLoader(Context context, boolean globalFilters) {
        super(context);
        this.context = context;
        this.globalFilters = globalFilters;
    }

    @Override
    public Cursor loadInBackground() {
        Cursor c;
        if (globalFilters) {
            c = PrivacyDB.getInstance(context).getGlobalPrivacyFilters();
        } else {
            c = PrivacyDB.getInstance(context).getAppSpecificPrivacyFilters();
        }
        return c;
    }
}
