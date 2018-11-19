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
package edu.uci.calit2.anteater.client.android.database;

/**
 * A java object representation of Privacy Leaks that corresponds to the table columns for "Table Filters"
 * @author Hieu Le
 */
public class PrivacyFilterModel {

    private int id = -1;
    private String packageName;
    private String piiValue;
    private String piiLabel;
    private boolean isSearchEnabled = false;
    private boolean isCustom = false;
    private String action;

    public PrivacyFilterModel(int id, String packageName, String piiLabel, String piiValue, String action, boolean isSearchEnabled, boolean isCustom) {
        this.setId(id);
        this.setPackageName(packageName);
        this.setPiiLabel(piiLabel);
        this.setPiiValue(piiValue);
        this.setAction(action);
        this.setSearchEnabled(isSearchEnabled);
        this.setCustom(isCustom);
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPiiValue() {
        return piiValue;
    }

    public void setPiiValue(String piiValue) {
        this.piiValue = piiValue;
    }

    public String getPiiLabel() {
        return piiLabel;
    }

    public void setPiiLabel(String piiLabel) {
        this.piiLabel = piiLabel;
    }

    public boolean isSearchEnabled() {
        return isSearchEnabled;
    }

    public void setSearchEnabled(boolean searchEnabled) {
        isSearchEnabled = searchEnabled;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
