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
public class PrivacyLeaksReportCursorLoader extends SimpleCursorLoader {

    public static final String RECENT_SORT = "RECENT_SORT";
    public static final String WEEKLY_SORT = "WEEKLY_SORT";
    public static final String MONTHLY_SORT = "MONTHLY_SORT";

    public static final long ONE_WEEK = 7 * 24 * 60 * 60 * 1000;
    public static final long ONE_MONTH = ONE_WEEK * 4;

    public static final String COLUMN_COUNT = "COLUMN_COUNT";

    private PrivacyDB database;
    private String sortType = RECENT_SORT;

    public PrivacyLeaksReportCursorLoader(Context context, String sortType) {
        super(context);
        database = PrivacyDB.getInstance(context);
        this.sortType = sortType;
    }

    @Override
    public Cursor loadInBackground() {
        Cursor c = database.getPrivacyLeaksReport(this.sortType);
        return c;
    }
}
