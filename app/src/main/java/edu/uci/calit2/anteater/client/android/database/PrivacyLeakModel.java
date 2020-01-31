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
package edu.uci.calit2.anteater.client.android.database;

/**
 * This class holds the leak values as we process it and geolocation values for processing.
 * The {@link PrivacyFilterModel} represents the existing Privacy Leak that the values correspond to
 *
 * @author Hieu Le
 */
public class PrivacyLeakModel {

    private final int INVALID_INDEX = -1;
    private String packageName;
    private String piiValue;
    private String piiIndex;
    private PrivacyFilterModel privacyFilterModel;
    private int latitudeIndex = INVALID_INDEX;
    private int longitudeIndex = INVALID_INDEX;

    public PrivacyLeakModel(String packageName, String piiValue, String piiIndex, PrivacyFilterModel privacyFilterModel) {
        setPackageName(packageName);
        setPiiValue(piiValue);
        setPiiIndex(piiIndex);
        setPrivacyFilterModel(privacyFilterModel);
    }

    public PrivacyLeakModel(String packageName, String piiValue, String piiIndex, PrivacyFilterModel privacyFilterModel, int latitudeIndex, int longitudeIndex) {
        setPackageName(packageName);
        setPiiValue(piiValue);
        setPiiIndex(piiIndex);
        setPrivacyFilterModel(privacyFilterModel);
        setLatitudeIndex(latitudeIndex);
        setLongitudeIndex(longitudeIndex);
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

    public String getPiiIndex() {
        return piiIndex;
    }

    public void setPiiIndex(String piiIndex) {
        this.piiIndex = piiIndex;
    }

    public PrivacyFilterModel getPrivacyFilterModel() {
        return privacyFilterModel;
    }

    public void setPrivacyFilterModel(PrivacyFilterModel privacyFilterModel) {
        this.privacyFilterModel = privacyFilterModel;
    }

    public int getLatitudeIndex() {
        return latitudeIndex;
    }

    public void setLatitudeIndex(int latitudeIndex) {
        this.latitudeIndex = latitudeIndex;
    }

    public int getLongitudeIndex() {
        return longitudeIndex;
    }

    public void setLongitudeIndex(int longitudeIndex) {
        this.longitudeIndex = longitudeIndex;
    }

}
