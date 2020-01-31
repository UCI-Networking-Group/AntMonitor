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
package edu.uci.calit2.anteater.client.android.signals;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import java.util.List;

/**
 * @author Emmanouil Alimpertis
 */
@TargetApi(18)
public class CellularMonitor {

    private final String TAG = CellularMonitor.class.getSimpleName();
    private TelephonyManager tm;
    private ConnectivityManager cm;
    PhoneStateListener myPhoneStateListener;
    private int currentApiVersion;

    private SignalStrength myCurrentSignalStrength;
    private List<CellInfo> cellInfoList=null;
    private int networkTypeCode=0;
    private String networkType="Unknown";
    private String currentPhoneConnectivity="Unknown";

    static final Object lockObjNetworkType = new Object();
    static final Object lockObjSignalStrength = new Object();
    static final Object lockObjCellInfoList = new Object();
    static final Object lockConnectivityMode = new Object();

    //Singleton
    private static CellularMonitor singleton;

    public static CellularMonitor getInstance(Context c){
        if(singleton==null)
            singleton = new CellularMonitor(c);
        return singleton;
    }

    //since it's singleton, constructor is private
    private CellularMonitor(Context c){

        tm  = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        myCurrentSignalStrength=null;
        this.networkTypeCode=tm.getNetworkType();
        this.networkType=this.determineNetworkType(this.networkTypeCode);

        this.currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (this.currentApiVersion >= Build.VERSION_CODES.JELLY_BEAN){
                this.cellInfoList=tm.getAllCellInfo();
        }
        phoneStateListenerSetup();
    }

    public String determineNetworkType(int netTypeCode){
        String networkTypeStr;
        switch(netTypeCode){
            case 0: networkTypeStr = "Unknown"; break;
            case 1: networkTypeStr = "GPRS"; break;
            case 2: networkTypeStr = "EDGE"; break;
            case 3: networkTypeStr = "UMTS"; break;
            case 4: networkTypeStr = "CDMA"; break;
            case 5: networkTypeStr = "EVDO_0"; break;
            case 6: networkTypeStr = "EVDO_A"; break;
            case 7: networkTypeStr = "1xRTT"; break;
            case 8: networkTypeStr = "HSDPA"; break;
            case 9: networkTypeStr = "HSUPA"; break;
            case 10: networkTypeStr = "HSPA"; break;
            case 11: networkTypeStr = "iDen"; break;
            case 12: networkTypeStr = "EVDO_B"; break;
            case 13: networkTypeStr = "LTE"; break;
            case 14: networkTypeStr = "eHRPD"; break;
            case 15: networkTypeStr = "HSPA+"; break;
            default: networkTypeStr = "Unknown"; break;
        }
        return networkTypeStr;
    }


    public String getNetworkSummary(){
       // if(this.myCurrentSignalStrength!=null) {
           // Log.i(TAG, "signal string is " + this.myCurrentSignalStrength.toString());
            //Log.i(TAG, "getDbm is" + Integer.toString(((CellInfoWcdma) tm.getAllCellInfo().get(this.getCurrentCellIndex())).getCellSignalStrength().getDbm()));
        //}

        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= Build.VERSION_CODES.JELLY_BEAN) {
            synchronized (this.lockObjCellInfoList) {
                if (this.cellInfoList != null) {
                    return this.getNetworkCarrierName()+ "|" + this.getNetworkSummary_overAPI18()+ "|"+this.getNetworkConnectivityStr();
                }
            }
        }
        return this.getNetworkCarrierName()+ "|"+this.getNetworkSummary_underAPI18()+ "|"+this.getNetworkConnectivityStr();

    }

    private String getNetworkSummary_underAPI18(){

        int rssi=-1;

        synchronized (this.lockObjSignalStrength) {
            if (myCurrentSignalStrength == null)
                return this.getNetworkTypeStr() + "|Unknown";

            if (this.getNetworkTypeStr().equals("EDGE") || this.getNetworkTypeStr().equals("GPRS")) {
                rssi = this.convertGSMSignalStrength(myCurrentSignalStrength.getGsmSignalStrength());
                if (rssi == -1)
                    return this.getNetworkTypeStr() + "|" + "Unknown";
                else
                    return this.getNetworkTypeStr() + "|" + Integer.toString(rssi);
            } else if (this.getNetworkTypeStr().equals("EVDO_0") || this.getNetworkTypeStr().equals("EVDO_A") || this.getNetworkTypeStr().equals("EVDO_B")||this.getNetworkTypeStr().equals("eHRPD"))
                return this.getNetworkTypeStr() + "|" + Integer.toString(myCurrentSignalStrength.getEvdoDbm());
            else if (this.getNetworkTypeStr().equals("CDMA"))
                return this.getNetworkTypeStr() + "|" + Integer.toString(myCurrentSignalStrength.getCdmaDbm());
            else{ //TODO earlier android APIs are quite messy, they need a lot of work to be clarified
                //our best guess is that we have the typical -113+ 2* asu for the reported asu in signalStrength object
                //asu is the first available value in signalStrength.toString();
                try {
                    String[] values = myCurrentSignalStrength.toString().split(" ");
                    int rssLastHope_asuFormat = Integer.valueOf(values[0]);
                    int rssiLastHope = this.convertGSMSignalStrength(rssLastHope_asuFormat);
                    if (rssiLastHope == -1)
                        return this.getNetworkTypeStr() + "|" + "Unknown";
                    else
                        return this.getNetworkTypeStr() + "|" + Integer.toString(rssi);
                }
                catch(Exception e){
                    return this.getNetworkTypeStr()+ "|Unknown";
                }
            }
        }
    }

    private String getNetworkSummary_overAPI18(){

        String rssiStr="Unknown";
        int currentCellIndex=-1;


        if(this.getNetworkTypeStr().equals("Unknown"))
            return this.getNetworkTypeStr() + "|"+ "Unknown";
        else{
            currentCellIndex=this.getCurrentCellIndex();
            if(currentCellIndex==-1)
                return "Unknown|Unknown";
            else{
                if(this.cellInfoList==null || this.cellInfoList.size() <= currentCellIndex)
                    return this.getNetworkSummary_underAPI18();
                else{
                    CellInfo cellInfoTmp = this.cellInfoList.get(currentCellIndex);

                    if(cellInfoTmp instanceof CellInfoLte)
                        return this.getNetworkTypeStr()+"|"+Integer.toString(((CellInfoLte) cellInfoTmp).getCellSignalStrength().getDbm());
                    else if(cellInfoTmp instanceof CellInfoWcdma)
                        return this.getNetworkTypeStr()+"|"+Integer.toString(((CellInfoWcdma) cellInfoTmp).getCellSignalStrength().getDbm());
                    else if(cellInfoTmp instanceof CellInfoGsm)
                        return this.getNetworkTypeStr()+"|"+Integer.toString(((CellInfoGsm) cellInfoTmp).getCellSignalStrength().getDbm());
                    else if (cellInfoTmp instanceof CellInfoCdma)
                        return this.getNetworkTypeStr()+"|"+Integer.toString(((CellInfoCdma) cellInfoTmp).getCellSignalStrength().getDbm());
                }
            }
        }

        return this.getNetworkTypeStr() + "|" + rssiStr;
    }

    private String getNetworkTypeStr(){
        synchronized (this.lockObjNetworkType){
            return this.networkType;
        }
    }

    /****** Methods for phone Connectivity ****/
    private String getNetworkConnectivityStr(){
        synchronized (this.lockConnectivityMode){
            return this.currentPhoneConnectivity;
        }
    }

    private NetworkInfo getNetworkInfo(){
        return this.cm.getActiveNetworkInfo();
    }

    /**
     * Check if there is any connectivity to a mobile network
     * */
    private boolean isConnectedMobile(){
        NetworkInfo info = this.getNetworkInfo();
        return (info != null && info.isConnected() &&
                info.getType() == ConnectivityManager.TYPE_MOBILE);
    }


    /**
     * Check if there is any connectivity to a Wifi network
     */
    private boolean isConnectedWifi(){
        NetworkInfo info = this.getNetworkInfo();
        return (info != null && info.isConnected() &&
                info.getType() == ConnectivityManager.TYPE_WIFI);
    }

    ////////////***********************////////////
    /* Method for Debug */
    public void printAllCellInfo(){
        List<CellInfo> cellInfoList=tm.getAllCellInfo();

        if(this.myCurrentSignalStrength!=null)
            System.out.println("Overall String is: " + myCurrentSignalStrength.toString());

        for (final CellInfo cellInfoTmp : cellInfoList) {
            if (cellInfoTmp instanceof CellInfoGsm) {
                System.out.println("GSM cell-id: " + ((CellInfoGsm) cellInfoTmp).getCellIdentity().getCid() + " " +
                        ((CellInfoGsm) cellInfoTmp).getCellIdentity()+ " rssi: "+ ((CellInfoGsm) cellInfoTmp).getCellSignalStrength() +  "registered: "+ ((CellInfoGsm) cellInfoTmp).isRegistered());
            }
            else if(cellInfoTmp instanceof CellInfoLte){
                System.out.println("LTE cell-id: " + ((CellInfoLte) cellInfoTmp).getCellIdentity().getCi()+ " " +
                        ((CellInfoLte) cellInfoTmp).getCellIdentity()+ " rssi: "+ ((CellInfoLte) cellInfoTmp).getCellSignalStrength()+ "registered: "+ ((CellInfoLte) cellInfoTmp).isRegistered());
            }
            else if(cellInfoTmp instanceof CellInfoWcdma){
                System.out.println("WCDMA cell" + ((CellInfoWcdma) cellInfoTmp).getCellIdentity().getCid() + " rssi:" + ((CellInfoWcdma) cellInfoTmp).getCellSignalStrength() + "registered: " + ((CellInfoWcdma) cellInfoTmp).isRegistered()) ;
            } else if (cellInfoTmp instanceof CellInfoCdma){
                System.out.println("CDMA cell" + ((CellInfoCdma) cellInfoTmp).getCellIdentity().getBasestationId() + " " +((CellInfoCdma) cellInfoTmp).getCellIdentity()+ " rssi: "+ ((CellInfoCdma) cellInfoTmp).getCellSignalStrength());

            }

            /*
            if(cellInfoList.get(currentCellIdx) instanceof CellInfoLte){
                Log.d(TAG,"ASU: "+((CellInfoLte) cellInfoList.get(currentCellIdx)).getCellSignalStrength().getAsuLevel() );
                Log.d(TAG,"dBm level:" +((CellInfoLte) cellInfoList.get(currentCellIdx)).getCellSignalStrength().getDbm() );
                Log.d(TAG,"level:" +((CellInfoLte) cellInfoList.get(currentCellIdx)).getCellSignalStrength().getLevel() );
                Log.d(TAG,"TA: " +((CellInfoLte) cellInfoList.get(currentCellIdx)).getCellSignalStrength().getTimingAdvance() );
            }
            */
        }

    }
    ////////////******************////////////
    //MCC: Mobile Country Code, consists of 3 decimal digits.
    public int getMcc(){
        //Returns the numeric name (MCC+MNC) of current registered operator.
        String mccMncOperator = tm.getNetworkOperator();
        if (mccMncOperator.equals(""))//NO SIM - NO network
            return -1;
        int mcc = Integer.parseInt(mccMncOperator.substring(0, 3));//MCC position 0 to 2
        return mcc;
    }

    //MCC as String
    public String getMccStr(){
        int mcc=this.getMcc();
        if(mcc==-1)
            return "No Network";
        else
            return String.valueOf(mcc);
    }

    //MNC: Mobile Network Code, i.e. network carrier code, consists of 2 or 3 digits
    public int getMnc(){
        //Returns the numeric name (MCC+MNC) of current registered operator.
        String mccMncOperator = tm.getNetworkOperator();
        if (mccMncOperator.equals(""))//NO SIM - NO network
            return -1;
        int mnc = Integer.parseInt(mccMncOperator.substring(3));//MNC position 3 to end
        return mnc;
    }

    //MNC as String
    public String getMncStr(){
        int mnc=this.getMnc();
        if(mnc==-1)
            return "No Network";
        else
            return String.valueOf(mnc);
    }

    public String getNetworkCarrierName(){
        String netCarrierName="Unknown";
        if(tm!=null)
            netCarrierName=tm.getNetworkOperatorName();
        return netCarrierName;
    }

    //LAC: Location Area code
    public int getLac(){
        int lac=-1;
        CellLocation cl = tm.getCellLocation();
        if(cl instanceof GsmCellLocation){
            lac=((GsmCellLocation) cl).getLac();
        }
        return lac;
    }

    //LAC as String
    public String getLacStr(){
        int lac=this.getLac();
        if(lac==-1)
            return "No Network";
        else
            return String.valueOf(lac);
    }

    //GSM cell-ID
    public int getGSMCellId(){
        int cid=-1;
        CellLocation cl = tm.getCellLocation();
        if(cl instanceof GsmCellLocation){
            cid = ((GsmCellLocation) cl).getCid();
            cid = cid & 0xffff;
        }
        return cid;
    }

    //GSM cell-ID as String
    public String getGSMCellIDStr(){
        int cid=this.getGSMCellId();
        if(cid==-1)
            return "Unknown";
        else
            return String.valueOf(cid);
    }

    //temp get cell-ID non converted
    public String getGSMCellID_raw(){
        int cid=-1;
        CellLocation cl = tm.getCellLocation();
        if(cl instanceof GsmCellLocation){
            cid = ((GsmCellLocation) cl).getCid();
        }
        return String.valueOf(cid);
    }

    //query for getting the index of current/registered cell, return -1 if not found
    public int getCurrentCellIndex(){
        int currentCellIdx=0;
        List<CellInfo> cellInfoList=tm.getAllCellInfo();

        if (cellInfoList == null)
            return -1;

        for(final CellInfo cellInfoTmp : cellInfoList) {
            if(cellInfoTmp.isRegistered()==true)
                return currentCellIdx;
            currentCellIdx++;
        }
        return -1;//registered cell was not found
    }

    //for LTE
    public String[] getSignalStrengthValuesLTE(){
        String [] rssMeasurementsLTE;//=new String[6];//TODO: this must be determined dynamically
        int currentCellIdx = this.getCurrentCellIndex();

        if(currentCellIdx==-1)
            return null;
        else{
            List<CellInfo> cellInfoList=tm.getAllCellInfo();
            if(cellInfoList.get(currentCellIdx) instanceof CellInfoLte){
                String tmp=  ((CellInfoLte) cellInfoList.get(currentCellIdx)).getCellSignalStrength().toString();
                rssMeasurementsLTE = tmp.split(" ");
                return rssMeasurementsLTE;
            }
            else {
                return null;
            }
        }
    }

    //our best guess is that the CellSignalStrength.toString() is the return of the AT+CSQ command
    //custom method to capture a specific parameter of this "raw" string which seems that is not considered
    //by the rest of the Android Telephony API
    // Returns the int value of the parameter: param
    public int getLTEQualityMetricFromCSQ(String param){
        String []rssMeasurementsLTE=this.getSignalStrengthValuesLTE();

        for(final String measTmp:rssMeasurementsLTE){
            if(measTmp.toLowerCase().contains(param.toLowerCase())){
                return Integer.parseInt(measTmp.substring(measTmp.lastIndexOf("=")+1));
            }
        }
        return -1;
    }

    public int getLteRsrp(){
        if(this.getNetworkTypeStr().equals("LTE")) {
            CellInfoLte cellInfo=(CellInfoLte)tm.getAllCellInfo().get(this.getCurrentCellIndex());
            return cellInfo.getCellSignalStrength().getDbm();
        }
        return -1;
    }

    public String getLteRsrpStr(){
        int rsrp=this.getLteRsrp();
        if(rsrp==-1)
            return "Unknown";
        else
            return String.valueOf(rsrp)+" dBm";

    }

    public String getLteRsrqStr(){
        int rsrq=this.getLTEQualityMetricFromCSQ("rsrq");
        if(rsrq==-1)
            return "Unknown";
        else
            return String.valueOf(rsrq)+ " dBm";
    }



    public String getWcdmaRSS(){
        int currentCellIndex=this.getCurrentCellIndex();
        if(currentCellIndex==-1)
            return "Un--known";
        else{
            CellInfoWcdma tmp=(CellInfoWcdma)tm.getAllCellInfo().get(currentCellIndex);
            return Integer.toString(tmp.getCellSignalStrength().getDbm()) + " dBm";
        }

    }

    private int convertGSMSignalStrength(int asu){
        if(asu==99)
            return -1;
        else
            return -113 + 2*asu;
    }

    private void phoneStateListenerSetup(){
        myPhoneStateListener = new PhoneStateListener() {
            public void onCallForwardingIndicatorChanged(boolean cfi){}
            public void onCallStateChanged(int state, String incomingNumber){}
            public void onDataConnectionStateChanged(int state){}
            public void onMessageWaitIndicatorChanged(boolean mwi){}
            public void onCellLocationChanged(CellLocation location){
/*
                GsmCellLocation cl = (GsmCellLocation) tm.getCellLocation();
                String temp = tm.getNetworkOperator();
               // int MCC = Integer.parseInt(ut.Left(temp,3));
                int MNC = Integer.parseInt(temp.substring(3,temp.length()));
                int LAC = cl.getLac();
                int ci = cl.getCid();

                TextView mncTextView=(TextView)getActivity().findViewById(R.id.MNCValue_id);
                mncTextView.setText(MNC);
                TextView lacTextView=(TextView)getActivity().findViewById(R.id.LACValue_id);
                lacTextView.setText(LAC);
                TextView cidTextView=(TextView)getActivity().findViewById(R.id.CIDValue_id);
                cidTextView.setText(ci);
*/
            }
            public void onDataActivity(int direction){}
            public void onCellInfoChanged (List<CellInfo> cellInfoListUpdate) {
                synchronized(lockObjCellInfoList){
                    if(cellInfoListUpdate!=null)
                        cellInfoList=cellInfoListUpdate;
                }
                synchronized(lockConnectivityMode){
                    if(isConnectedMobile())
                        currentPhoneConnectivity="Cellular";
                    else if(isConnectedWifi())
                        currentPhoneConnectivity="WiFi";
                    else
                        currentPhoneConnectivity="Unknown";
                }
            }

            public void onServiceStateChanged(ServiceState serviceState){
                synchronized (lockObjNetworkType){
                    networkTypeCode=tm.getNetworkType();
                    networkType=determineNetworkType(networkTypeCode);
                }
                synchronized(lockConnectivityMode){
                    if(isConnectedMobile())
                        currentPhoneConnectivity="Cellular";
                    else if(isConnectedWifi())
                        currentPhoneConnectivity="WiFi";
                    else
                        currentPhoneConnectivity="Unknown";
                }

            }
            public void onSignalStrengthsChanged(SignalStrength signalStrength){
                synchronized (lockObjSignalStrength){
                    if(signalStrength!=null)
                        myCurrentSignalStrength=signalStrength;
                }
                //Log.d(TAG,"SIGNAL STRENGTH CHANGED");
                //Log.d(TAG,"SIGNAL STRENGTH NEW VALUES: "+signalStrength.toString());
                //Log.d(TAG,"CDMA_Dbm: "+signalStrength.getCdmaDbm());
                //Log.d(TAG,"CDMA_Ecio: "+signalStrength.getCdmaEcio());
                //Log.d(TAG,"EVDO_DBM: "+signalStrength.getEvdoDbm());
                //Log.d(TAG,"EVDO_ECIO: "+signalStrength.getEvdoEcio());
                //Log.d(TAG,"EVDO_SNR: "+signalStrength.getEvdoSnr());
                //Log.d(TAG,"GSM_BER: "+signalStrength.getGsmBitErrorRate());
                //Log.d(TAG,"GSM_rssi: "+signalStrength.getGsmSignalStrength());

            }
        };

        try{
            tm.listen(myPhoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION
                    | PhoneStateListener.LISTEN_DATA_ACTIVITY
                    | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                    | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                    | PhoneStateListener.LISTEN_CALL_STATE
                    | PhoneStateListener.LISTEN_CELL_INFO
                    | PhoneStateListener.LISTEN_SERVICE_STATE);
        }catch(Exception e){

        }
    }

}
