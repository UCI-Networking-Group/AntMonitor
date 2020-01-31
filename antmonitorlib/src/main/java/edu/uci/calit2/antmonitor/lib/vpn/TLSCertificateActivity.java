/*
 *  This file is part of AntMonitor <https://athinagroup.eng.uci.edu/projects/antmonitor/>.
 *  Copyright (C) 2018 Anastasia Shuba and the UCI Networking Group
 *  <https://athinagroup.eng.uci.edu>, University of California, Irvine.
 *  Copyright (C) 2014  Yihang Song
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
package edu.uci.calit2.antmonitor.lib.vpn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.security.KeyChain;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Enumeration;

import javax.security.cert.CertificateEncodingException;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

/**
 * Use this {@link Activity} to install a root CA certificate.
 * Adopted from PrivacyGuard: https://github.com/cryspuwaterloo/privacyguard
 *
 * @author Anastasia Shuba
 */
public class TLSCertificateActivity extends Activity {

    private static final String TAG = TLSCertificateActivity.class.getSimpleName();

    private static String caName = "AntMonitor_CA";
    private static String certName = "AntMonitor_Cert";
    private static String keyType = "PKCS12";
    private static String password = "antmonitor";
    private static String issuer = "SandroProxy";

    /** The name of the {@link android.content.SharedPreferences} used by this class */
    public static String prefsName = "ANTMONITOR_LIB";

    /** Used to remember (inisde {@link android.content.SharedPreferences}
     * whether or not certificate was installed */
    public static String certInsallPref = "IS_CERT_INSTALLED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String dir = getFilesDir().getPath();
        SharedPreferences sp = getSharedPreferences(prefsName, Context.MODE_PRIVATE);

        try {
            // Need both checks to account for the fact that certificate may have been deleted
            // when the app data was erased
            if (isCACertificateInstalled() && sp.getBoolean(certInsallPref, false)) {
                // Certificate already installed - there's nothing to be done
                sendResult(RESULT_OK);
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not install certificate.", e);
            sendResult(RESULT_CANCELED);
            return;
        }

        generateCACertificate(dir);
        Intent intent = KeyChain.createInstallIntent();
        try {
            intent.putExtra(KeyChain.EXTRA_CERTIFICATE, getCACertificate(dir, caName).getEncoded());
        } catch (CertificateEncodingException e) {
            Log.i(TAG, "Could not install certificate.", e);
            sendResult(RESULT_CANCELED);
            return;
        }
        intent.putExtra(KeyChain.EXTRA_NAME, caName);

        //When used with startActivityForResult(Intent, int), RESULT_OK will be returned if a credential
        // was successfully installed, otherwise RESULT_CANCELED will be returned.
        startActivityForResult(intent, VpnController.REQUEST_INSTALL_CERT);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == VpnController.REQUEST_INSTALL_CERT) {
            if (result == RESULT_OK) {
                Log.d(TAG, "Everything went well 1...");

                // Remember that we installed the certificate
                SharedPreferences sp = getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean(certInsallPref, true);
                editor.apply();

                sendResult(result);
            } else {
                Log.d(TAG, "Did not install cert 1.");
                sendResult(RESULT_CANCELED);
            }
        }
    }

    private void sendResult(int resultCode) {
        setResult(resultCode, null);
        finish();
    }

    /**
     * Checks if certificate is installed inside the Trusted User Store
     * @return {@code true} if certificate is installed, {@code false} otherwise
     * @throws Exception if errors occur when accessing the store
     */
    static boolean isCACertificateInstalled() throws Exception {

        KeyStore keyStoreCA;
        keyStoreCA = KeyStore.getInstance("AndroidCAStore");
        keyStoreCA.load(null);

        Enumeration ex = keyStoreCA.aliases();
        while (ex.hasMoreElements()) {
            String is = (String) ex.nextElement();

            java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate)
                    keyStoreCA.getCertificate(is);
            if (cert.getIssuerDN().getName().contains(issuer)) {
                return true;
            }
        }
        return false;
    }

    /********************************************************************************************/
    /*                     CERTIFICATE GENERATION METHODS, FROM PrivacyGuard                    */
    /********************************************************************************************/

    // generate CA certificate but return a ssl socket factory factory which use this certificate
    static AntSSLSocketFactory generateCACertificate(String dir) {
        try {
            return new AntSSLSocketFactory(dir + "/" + caName, dir + "/" + certName, keyType,
                                                password.toCharArray());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // get the CA certificate by the path
    private X509Certificate getCACertificate(String dir, String caName) {
        String CERT_FILE = dir + "/" + caName + "_export.crt";
        File certFile = new File(CERT_FILE);
        FileInputStream certIs = null;
        try {
            certIs = new FileInputStream(CERT_FILE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        byte[] cert = new byte[(int) certFile.length()];
        try {
            certIs.read(cert);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return X509Certificate.getInstance(cert);
        } catch (CertificateException e) {
            e.printStackTrace();
            return null;
        }
    }

}
