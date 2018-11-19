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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.activity.VpnStarterUtils;
import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.anteater.client.android.util.AntMonitorApplication;
import edu.uci.calit2.anteater.client.android.util.OpenAppDetails;
import edu.uci.calit2.anteater.client.android.util.PreferenceTags;
import edu.uci.calit2.anteater.client.android.util.UIHelper;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RealTimeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RealTimeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RealTimeFragment extends Fragment implements AdapterView.OnItemSelectedListener{
    private BroadcastReceiver connectionReceiver;
    private IntentFilter filter;

    public static final String ADD_CONNECTION = "VISUALIZATION_ADD_CONNECTION";
    public static final String REMOVE_CONNECTION = "VISUALIZATION_REMOVE_CONNECTION";

    public static final String PACKAGE_NAME = "VISUALIZATION_PACKAGE_NAME";
    public static final String DESTINATION_IP = "VISUALIZATION_DESTINATION_IP";
    public static final String PACKET_LENGTH = "VISUALIZATION_PACKET_LENGTH";
    public static final String PROTOCOL_NAME = "VISUALIZATION_PROTOCOL";
    public static final String TRAFFIC_TYPE = "VISUALIZATION_TRAFFIC_TYPE";
    public static final String INCOMING = "VISUALIZATION_INCOMING";
    public static final String OUTGOING = "VISUALIZATION_OUTGOING";

    private WebView webView;
    private TextView tvTotalBytes;
    private Spinner dropdown;
    private float totalBytes = 0;
    private String serverImgageBase64;
    private boolean webViewFinishInit = false;

    private final String REAL_TIME_VISUAL = "Real-time";
    private final String PRIVACY_LEAKS_VISUAL = "Privacy leaks";
    private String currentSelectedVisual = REAL_TIME_VISUAL;

    private OnFragmentInteractionListener mListener;

    private PrivacyDB database;

    /** Keep reference to a Context as 'getActivity()' sometimes returns {@code null} */
    private Context mContext;

    private final int VISUAL_UPDATE_TIME_LIMIT = 500; // ms
    private final Handler updateVisualHandler = new Handler();
    private final Runnable updateVisualRunnable = new Runnable() {
        @Override
        public void run() {
            if (webView != null && webViewFinishInit) {
                webView.loadUrl("javascript:update()");
            }
        }
    };

    private long lastVisualDataChange = -1;

    public RealTimeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RealTimeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RealTimeFragment newInstance(String param1, String param2) {
        RealTimeFragment fragment = new RealTimeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences sp = getContext().getSharedPreferences(PreferenceTags.PREFS_TAG, Context.MODE_PRIVATE);
        boolean hasShownTutorial = sp.getBoolean(PreferenceTags.PREFS_REALTIME_OVERLAY_FIRST, false);
        if (!hasShownTutorial) {
            sp.edit().putBoolean(PreferenceTags.PREFS_REALTIME_OVERLAY_FIRST, true).apply();
            DialogFragment dialog = new RealTimeTutorialDialogOverlay();
            dialog.show(getFragmentManager(), "dialog");
        }
        webView.loadUrl("file:///android_asset/html/graph.html");
        if (dropdown.getOnItemSelectedListener() == null) {
            dropdown.setOnItemSelectedListener(this);
        }
    }

    public void onResume() {
        super.onResume();
        // Clear the current view, once loaded, it will re-draw open connections
        LocalBroadcastManager.getInstance(mContext).registerReceiver(connectionReceiver, filter);
    }

    public void onStop() {
        super.onStop();
        OpenAppDetails.setIsVisualizationOn(false);
        totalBytes = 0;
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(connectionReceiver);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_real_time, container, false);
        configureWebView(view);
        connectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // if the last time we had a data change was less than the visual update limit, then cancel it.
                if (lastVisualDataChange != -1 && System.currentTimeMillis() - lastVisualDataChange < VISUAL_UPDATE_TIME_LIMIT) {
                    updateVisualHandler.removeCallbacks(updateVisualRunnable);
                }

                if (intent.getAction().equals(ADD_CONNECTION)) {
                    String appName = intent.getStringExtra(PACKAGE_NAME);
                    String ip = intent.getStringExtra(DESTINATION_IP);
                    int length = intent.getIntExtra(PACKET_LENGTH, 0);
                    String protocol = intent.getStringExtra(PROTOCOL_NAME);
                    String trafficType = intent.getStringExtra(TRAFFIC_TYPE);

                    if (appName != null && ip != null) {
                        addConnection(appName, ip, length, protocol, trafficType);
                    }
                } else {
                    String appName = intent.getStringExtra(PACKAGE_NAME);
                    String ip = intent.getStringExtra(DESTINATION_IP);
                    if (appName != null && ip != null) {
                        removeConnection(appName, ip);
                    }
                }

                lastVisualDataChange = System.currentTimeMillis();
                updateVisualHandler.postDelayed(updateVisualRunnable, VISUAL_UPDATE_TIME_LIMIT);
            }
        };

        database = PrivacyDB.getInstance(getContext());
        dropdown = (Spinner) view.findViewById(R.id.spinner);
        String[] items = new String[]{"Real-time", "Privacy leaks"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),R.layout.support_simple_spinner_dropdown_item,items);
        dropdown.setAdapter(adapter);

        tvTotalBytes = (TextView) view.findViewById(R.id.tvByteCount);
        tvTotalBytes.setText(String.format("Total Data Transferred: %.3f MB",
                totalBytes));


        filter = new IntentFilter(ADD_CONNECTION);
        filter.addAction(REMOVE_CONNECTION);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(connectionReceiver, filter);

        // create server icon for visualization
        Drawable serverDrawable = getResources().getDrawable(R.mipmap.server);
        Bitmap serverBitmap = ((BitmapDrawable) serverDrawable).getBitmap();
        // Convert bitmap to Base64 encoded image for web
        ByteArrayOutputStream serverByteArrayOutputStream = new ByteArrayOutputStream();
        serverBitmap.compress(Bitmap.CompressFormat.PNG, 100, serverByteArrayOutputStream);
        byte[] serverByteArray = serverByteArrayOutputStream.toByteArray();
        serverImgageBase64 = Base64.encodeToString(serverByteArray, Base64.NO_WRAP);

        return view;
    }

    private void configureWebView(View view) {
        webView = (WebView) view.findViewById(R.id.visualization_screen);
        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);
        webSettings.setUseWideViewPort(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setInitialScale(1);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webViewFinishInit = true;
                if (currentSelectedVisual.equals(REAL_TIME_VISUAL))
                    initRealTimeNodes();
                else
                    initPrivacyLeaksNodes();
            }
        });
        // Ignore warning since the JS page is our own - we know it's not malicious
        webView.addJavascriptInterface(this, "JavaInterface");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.clearHistory();
            webView.clearCache(true);
            webView.removeAllViews();
            webView.destroy();
            webView = null;
            webViewFinishInit = false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static Bitmap getAppIcon(PackageManager mPackageManager, String packageName) {
        try {
            Drawable drawable = mPackageManager.getApplicationIcon(packageName);

            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            } else if (drawable instanceof AdaptiveIconDrawable) {
                Drawable backgroundDr = ((AdaptiveIconDrawable) drawable).getBackground();
                Drawable foregroundDr = ((AdaptiveIconDrawable) drawable).getForeground();

                Drawable[] drr = new Drawable[2];
                drr[0] = backgroundDr;
                drr[1] = foregroundDr;

                LayerDrawable layerDrawable = new LayerDrawable(drr);

                int width = layerDrawable.getIntrinsicWidth();
                int height = layerDrawable.getIntrinsicHeight();

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmap);

                layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                layerDrawable.draw(canvas);

                return bitmap;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addConnection(String appName, String ip, int length, String protocol,
                               String trafficType) {
        String imageBase64 = UIHelper.getImageBase64(appName);
        if (imageBase64 == null) {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo appInfo;
            try {
                appInfo = pm.getApplicationInfo(appName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                // TODO: default image for System. Disable logging as it happens frequently for now...
                //Log.e("D", "Could not find " + appName);
                return;
            }

            // Get app icon
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= 26) {
                bitmap = getAppIcon(pm, appName);
            } else {
                Drawable iconDrawable = appInfo.loadIcon(pm);
                bitmap = ((BitmapDrawable) iconDrawable).getBitmap();
            }

            // Convert bitmap to Base64 encoded image for web
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            imageBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP);
            UIHelper.setPackageToImageBase64(appName, imageBase64);
        }

        totalBytes += (float) length/1048576;
        tvTotalBytes.setText(String.format("Total Data Transferred: %.3f MB",
                totalBytes));
        // Run JS
        if(trafficType.equals(RealTimeFragment.INCOMING) && webView != null) {
            webView.loadUrl("javascript:addConnection(\"" + appName +
                    "\", \"" + ip + "\", \"" + imageBase64 + "\", \"" + serverImgageBase64 + "\", \"" + length + "\", \"incoming\")");
        } else if(trafficType.equals(RealTimeFragment.OUTGOING) && webView != null) {
            webView.loadUrl("javascript:addConnection(\"" + appName +
                    "\", \"" + ip + "\", \"" + imageBase64 + "\", \"" + serverImgageBase64 + "\", \"" + length + "\", \"outgoing\")");
        }
    }

    private void removeConnection(String appName, String ip) {

        if (!appName.startsWith("No mapping") && webView != null) {
            webView.loadUrl("javascript:removeConnection(\"" + appName +
                    "\", \"" + ip + "\")");
        }
    }

    /**
     * Displays a dialog showing the user which app the node represents
     * Called from drawGraph.js
     * @param packageName the package name of the app-node
     */
    @JavascriptInterface
    public void showAppInfo(String packageName) {
        String appName = packageName;
        PackageManager pm = mContext.getPackageManager();
        try {
            appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            // do nothing, we will keep it as pkgName
        }

        showDialog("App: " + appName);
    }

    /**
     * Displays a dialog showing the user which server the node represents
     * Called from drawGraph.js
     * @param ip the ip address of the server-node
     */
    @JavascriptInterface
    public void showServerInfo(String ip) {
        try {
            ip = InetAddress.getByName(ip).getCanonicalHostName();
        } catch(UnknownHostException e) {
            // Keep as IP number
        }

        showDialog("Server: " + ip);
    }

    /**
     * Convenience method for showing a dialog for node information
     * @param title title of the dialog to be displayed
     */
    private void showDialog(String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setPositiveButton(R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_real_time, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.menu_option_tutorial:
                DialogFragment dialog = new RealTimeTutorialDialogOverlay();
                dialog.show(getFragmentManager(), "dialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
            mContext = context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void initRealTimeNodes() {
        webView.loadUrl("javascript:initGraph(" + (webView.getHeight()) + ", " + (webView.getWidth()) + ")");
        OpenAppDetails.setIsVisualizationOn(true);
        VpnStarterUtils.getOutFilter(AntMonitorApplication.getAppContext()).
                triggerOpenTCPConnections();
    }

    private void initPrivacyLeaksNodes() {
        OpenAppDetails.setIsVisualizationOn(false);
        webView.loadUrl("javascript:initGraph(" + (webView.getHeight()) + ", " + (webView.getWidth()) + ")");
        String[] leaks = database.printLeaks();
        if (leaks != null) {
            OpenAppDetails openAppDetails = new OpenAppDetails(getContext());
            for (int j =0; j<leaks.length; j++) {
                String appName = leaks[j].split(", ")[1];
                String remoteIP = leaks[j].split(", ")[2];
                openAppDetails.addTCPConnection(remoteIP, appName);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView.getItemAtPosition(i).equals(REAL_TIME_VISUAL) && !currentSelectedVisual.equals(REAL_TIME_VISUAL)) {
            currentSelectedVisual = REAL_TIME_VISUAL;
            if (webView != null && webViewFinishInit) {
                initRealTimeNodes();
            }
       } else if (adapterView.getItemAtPosition(i).equals(PRIVACY_LEAKS_VISUAL) && !currentSelectedVisual.equals(PRIVACY_LEAKS_VISUAL)) {
            currentSelectedVisual = PRIVACY_LEAKS_VISUAL;
            if (webView != null && webViewFinishInit) {
                initPrivacyLeaksNodes();
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
