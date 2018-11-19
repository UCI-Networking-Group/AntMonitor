/*
 *  This file is part of AntMonitor <https://athinagroup.eng.uci.edu/projects/antmonitor/>.
 *  Copyright (C) 2018 Anastasia Shuba and the UCI Networking Group
 *  <https://athinagroup.eng.uci.edu>, University of California, Irvine.
 *  Copyright (c) 2012 supp.sandrob@gmail.com
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
package edu.uci.calit2.antmonitor.lib.vpn;

import android.util.Log;

import org.sandrob.bouncycastle.asn1.ASN1EncodableVector;
import org.sandrob.bouncycastle.asn1.DEREncodableVector;
import org.sandrob.bouncycastle.asn1.DERObjectIdentifier;
import org.sandrob.bouncycastle.asn1.DERSequence;
import org.sandrob.bouncycastle.asn1.x509.BasicConstraints;
import org.sandrob.bouncycastle.asn1.x509.GeneralName;
import org.sandrob.bouncycastle.asn1.x509.GeneralNames;
import org.sandrob.bouncycastle.asn1.x509.X509Extensions;
import org.sandrob.bouncycastle.x509.X509V3CertificateGenerator;
import org.sandrob.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.sandrob.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.sandrop.webscarab.plugin.proxy.SiteData;
import org.sandroproxy.constants.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

/**
 * Adopted from SandroProxy:
 * https://github.com/nxtreaming/sandrop/blob/master/projects/SandroProxyLib/src/org/sandrop/webscarab/plugin/proxy/SSLSocketFactoryFactory.java
 */
class AntSSLSocketFactory {

    private static final long DEFAULT_VALIDITY = 10L * 365L * 24L * 60L * 60L
            * 1000L;

    private static final String TAG = AntSSLSocketFactory.class.getSimpleName();

    private static Logger _logger = Logger
            .getLogger(AntSSLSocketFactory.class.getName());

    private static final String CA = "CA";
    private static X500Principal CA_NAME;

    static {
        try {

            //CA_NAME = new X500Principal("cn=OWASP Custom CA for "
            //        + java.net.InetAddress.getLocalHost().getHostName()
            //        + " at " + new Date()
            //        + ",ou=OWASP Custom CA,o=OWASP,l=OWASP,st=OWASP,c=OWASP");

            CA_NAME = new X500Principal("cn=SandroProxy Custom CA"
                    + ",ou=SandroProxy Custom CA,o=SandroProxy,l=SandroProxy,st=SandroProxy,c=SandroProxy");
            _logger.setLevel(Level.FINEST);
        } catch (Exception ex) {
            ex.printStackTrace();
            CA_NAME = null;
        }
    }

    private PrivateKey caKey;

    private X509Certificate[] caCerts;

    private String filenameCA;
    private String filenameCert;

    private KeyStore keystoreCert;
    private KeyStore keystoreCA;

    private char[] passwordCA;
    private char[] passwordCerts;

    private boolean reuseKeys = false;

    private Map<String, SSLContext> contextCache = new ConcurrentHashMap<>();
    final Map<String, String> domainToContextKey = new ConcurrentHashMap<>();
    Map<String, Set<String>> domainToAltNames = new ConcurrentHashMap<>();
    private Map<String, Object> contextLocks = new ConcurrentHashMap<>();

    private Set<BigInteger> serials = new HashSet<BigInteger>();

    public AntSSLSocketFactory(String fileNameCA, String fileNameCert, String type,
                                   char[] password)
            throws GeneralSecurityException, IOException {
        _logger.setLevel(Level.FINEST);
        this.filenameCA = fileNameCA;
        this.passwordCA = password;
        this.passwordCerts = password;
        this.filenameCert = fileNameCert;
        boolean haveNewCA = false;
        String keyStoreProvider = "BC";
        keystoreCA = KeyStore.getInstance(type, keyStoreProvider);
        File fileCA = new File(filenameCA);
        if (filenameCA == null) {
            _logger.info("No keystore provided, keys and certificates will be transient!");
        }
        String caAliasValue = "";
        // ca stuff
        if (fileCA.exists() && fileCA.canRead()) {
            _logger.fine("Loading keys from " + filenameCA);
            InputStream is = new FileInputStream(fileCA);
            keystoreCA.load(is, passwordCA);
            is.close();
            String storeAlias;
            Enumeration<String> enAliases = keystoreCA.aliases();
            Date lastStoredAliasDate = null;
            // it should be just one
            while(enAliases.hasMoreElements()){
                storeAlias = enAliases.nextElement();
                Date lastStoredDate = keystoreCA.getCreationDate(storeAlias);
                if (lastStoredAliasDate == null || lastStoredDate.after(lastStoredAliasDate)){
                    lastStoredAliasDate = lastStoredDate;
                    caAliasValue = storeAlias;
                }
            }
            caKey = (PrivateKey) keystoreCA.getKey(caAliasValue, passwordCA);
            if (caKey == null) {
                _logger.warning("Keystore does not contain an entry for '" + caAliasValue
                        + "'");
            }
            caCerts = cast(keystoreCA.getCertificateChain(caAliasValue));
        } else {
            _logger.info("Generating CA key");
            keystoreCA.load(null, passwordCA);
            generateCA(CA_NAME);
            haveNewCA = true;
            saveKeystore(keystoreCA, filenameCA, passwordCA);
            caAliasValue = keystoreCA.aliases().nextElement();
        }
        // store ca cert to be used for export
        {
            FileOutputStream fos = null;
            try{
                X509Certificate caCert = (X509Certificate) keystoreCA.getCertificate(caAliasValue);
                byte[] caByteArray = caCert.getEncoded();
                String exportFilename = filenameCA + Constants.CA_FILE_EXPORT_POSTFIX;
                fos = new FileOutputStream(exportFilename);
                fos.write(caByteArray);
                fos.close();
                _logger.fine("CA cert exported to " + exportFilename);
            }catch (Exception ex){
                ex.printStackTrace();
                if (fos != null){
                    fos.close();
                }
            }
        }
        // cert stuff
        File fileCert = new File(filenameCert);
        if (haveNewCA || fileCert == null || !fileCert.exists()){
            keystoreCert = KeyStore.getInstance(type, keyStoreProvider);
            keystoreCert.load(null, passwordCerts);
            saveKeystore(keystoreCert, filenameCert, passwordCerts);
        }else{
            InputStream is = new FileInputStream(fileCert);
            try{
                keystoreCert = KeyStore.getInstance(type, keyStoreProvider);
                keystoreCert.load(is, passwordCerts);
            }catch(Exception ex){
                // problems opening exisiting so we create new one
                _logger.fine("problems opening exisiting cert keystore so we create new one");
                keystoreCert = KeyStore.getInstance(type, keyStoreProvider);
                keystoreCert.load(null, passwordCerts);
                saveKeystore(keystoreCert, filenameCert, passwordCerts);
            }
            is.close();
            initSerials();
        }
    }

    /**
     * Determines whether the public and private key generated for the CA will
     * be reused for other hosts as well.
     *
     * This is mostly just a performance optimisation, to save time generating a
     * key pair for each host. Paranoid clients may have an issue with this, in
     * theory.
     *
     * @param reuse
     *            true to reuse the CA key pair, false to generate a new key
     *            pair for each host
     */
    public void setReuseKeys(boolean reuse) {
        reuseKeys = reuse;
    }

    protected String getCertEntry(SiteData hostData) {
        String certEntry = hostData.tcpAddress != null ? hostData.tcpAddress + "_" + hostData.destPort: hostData.name;
        if (hostData.hostName != null) {
            certEntry += "_" + hostData.hostName;
        }
        return certEntry;
    }

    protected String getContextKey(SiteData hostData) {
        if (hostData.name != null) {
            String contextKey = domainToContextKey.get(hostData.name);
            if (contextKey != null) {
                ForwarderManager.Logg.d(TAG, "Found contextKey for: " + hostData.name);
                return contextKey;
            } else {
                ForwarderManager.Logg.d(TAG, "Host name that had no contextKey: " + hostData.name);
            }
        }
        // new context
        String contextKey = UUID.randomUUID().toString();

        Set<String> listAlts = domainToAltNames.get(hostData.name);
        if (listAlts != null) {
            for (String s : listAlts)
                domainToContextKey.put(s, contextKey);

            domainToAltNames.remove(hostData.name);
        }
        return contextKey;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.owasp.proxy.daemon.CertificateProvider#getSocketFactory(java.lang
     * .String, int)
     */
    public SSLSocketFactory getSocketFactory(SiteData hostData)
            throws IOException, GeneralSecurityException {
        final String certEntry = getContextKey(hostData);

        Object lock = contextLocks.get(certEntry);
        if (lock == null) {
            lock = new Object();
            contextLocks.put(certEntry, lock);
        }
        synchronized (lock) {
            ForwarderManager.Logg.d(TAG, "locked " + hostData.sourcePort + ": " + hostData.name);
            SSLContext sslContext = (SSLContext) contextCache.get(certEntry);
            if (sslContext == null) {
                X509KeyManager km;
                boolean inKeyStore;
                inKeyStore = keystoreCert.containsAlias(certEntry);
                if (!inKeyStore) {
                    km = createKeyMaterial(hostData, certEntry);
                } else {
                    // TODO: #200 check if this works with a random UUID - does it use File IO?
                    ForwarderManager.Logg.d(TAG, "loadKeyMaterial " + hostData.sourcePort + ": " + hostData.name);
                    km = loadKeyMaterial(hostData, certEntry);
                    ForwarderManager.Logg.d(TAG, "loadKeyMaterial done" + hostData.sourcePort + ": " + hostData.name);
                }

                // here, trust managers is a single trust-all manager
                TrustManager[] trustManagers = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            public void checkClientTrusted(
                                    X509Certificate[] certs, String authType) {
                                _logger.fine("trust manager checkClientTrusted authType:" + authType);
                                if (certs != null) {
                                    for (int i = 0; i < certs.length; i++) {
                                        _logger.fine("trust manager checkClientTrusted:" + certs[i]);
                                    }
                                }
                            }

                            public void checkServerTrusted(
                                    X509Certificate[] certs, String authType) {
                                _logger.fine("trust manager checkServerTrusted authType:" + authType);
                                if (certs != null) {
                                    for (int i = 0; i < certs.length; i++) {
                                        _logger.fine("trust manager checkServerTrusted:" + certs[i]);
                                    }
                                }
                            }
                        }
                };
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(new KeyManager[]{km}, trustManagers, null);
                contextCache.put(certEntry, sslContext);
                domainToContextKey.put(hostData.name, certEntry);
            }
            ForwarderManager.Logg.d(TAG, "locked done " + hostData.sourcePort + ": " + hostData.name);
            return sslContext.getSocketFactory();
        }

    }

    private X509Certificate[] cast(Certificate[] chain) {
        X509Certificate[] certs = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; i++) {
            certs[i] = (X509Certificate) chain[i];
        }
        return certs;
    }

    private X509KeyManager loadKeyMaterial(SiteData hostData, final String certEntry) throws GeneralSecurityException, IOException {
        X509Certificate[] certs = null;
        //String certEntry = getCertEntry(hostData);
        Certificate[] chain = keystoreCert.getCertificateChain(certEntry);
        if (chain != null) {
            certs = cast(chain);
        } else {
            throw new GeneralSecurityException(
                    "Internal error: certificate chain for " + hostData.name
                            + " not found!");
        }

        PrivateKey pk = (PrivateKey) keystoreCert.getKey(certEntry, passwordCerts);
        if (pk == null) {
            throw new GeneralSecurityException(
                    "Internal error: private key for " + hostData.name + " not found!");
        }
        _logger.finest("loading keys for " + certEntry);
        return new HostKeyManager(hostData, pk, certs);
    }

    private void saveKeystore(KeyStore keystore, String filename, char[] password) {
        if (filename == null)
            return;
        try {
            OutputStream out = new FileOutputStream(filename);
            keystore.store(out, password);
            out.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (GeneralSecurityException gse) {
            gse.printStackTrace();
        }
    }

    private void saveKeystoreThread(final KeyStore keystore, final String filename,
                                    final char[] password) {
        if (filename == null)
            return;

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream out = new FileOutputStream(filename);
                    keystore.store(out, password);
                    out.close();
                } catch (IOException | GeneralSecurityException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    private void generateCA(X500Principal caName)
            throws GeneralSecurityException, IOException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair caPair = keyGen.generateKeyPair();
        caKey = caPair.getPrivate();
        PublicKey caPubKey = caPair.getPublic();
        Date begin = new Date();
        Date ends = new Date(begin.getTime() + DEFAULT_VALIDITY);


        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        // X509v3CertificateBuilder   certGen = new X509v3CertificateBuilder();
        certGen.setSerialNumber(BigInteger.ONE);
        certGen.setIssuerDN(caName);
        certGen.setNotBefore(begin);
        certGen.setNotAfter(ends);
        certGen.setSubjectDN(caName);
        certGen.setPublicKey(caPubKey);
        certGen.setSignatureAlgorithm("SHA256withRSA");
        BasicConstraints bc = new BasicConstraints(true);
        certGen.addExtension(new DERObjectIdentifier("2.5.29.19"), true, bc.toASN1Object().getEncoded());
        X509Certificate cert = certGen.generate(caKey, "BC");

        caCerts = new X509Certificate[] { cert };

        keystoreCA.setKeyEntry(CA, caKey, passwordCA, caCerts);
    }

    private void initSerials() throws GeneralSecurityException {
        Enumeration<String> e = keystoreCert.aliases();
        while (e.hasMoreElements()) {
            String alias = (String) e.nextElement();
            X509Certificate cert = (X509Certificate) keystoreCert
                    .getCertificate(alias);
            BigInteger serial = cert.getSerialNumber();
            serials.add(serial);
        }
    }

    protected X500Principal getSubjectPrincipal(String host) {
        return new X500Principal("cn=" + host + ",ou=UNTRUSTED SandroProxy,o=UNTRUSTED SandroProxy");
    }

    protected BigInteger getNextSerialNo() {
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        while (serials.contains(serial))
            serial.add(BigInteger.ONE);
        serials.add(serial);
        return serial;
    }

    private X509KeyManager createKeyMaterial(SiteData hostData, final String certEntry)
            throws GeneralSecurityException {
        KeyPair keyPair;

        if (reuseKeys) {
            keyPair = new KeyPair(caCerts[0].getPublicKey(), caKey);
        } else {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            keygen.initialize(1024);
            keyPair = keygen.generateKeyPair();
        }

        X500Principal subject = getSubjectPrincipal(hostData.name);
        Date begin = new Date();
        Date ends = new Date(begin.getTime() + DEFAULT_VALIDITY);

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(getNextSerialNo());
        certGen.setIssuerDN(caCerts[0].getSubjectX500Principal());
        certGen.setNotBefore(begin);
        certGen.setNotAfter(ends);
        certGen.setSubjectDN(subject);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256withRSA");

        // generate alternative names
        if (hostData.certs != null && hostData.certs.length > 0){
            Collection<List<?>> coll = hostData.certs[0].getSubjectAlternativeNames();
            if (coll != null && coll.size() > 0){
                Iterator<List<?>> iter = coll.iterator();
                final int SUBALTNAME_DNSNAME = 2;
                DEREncodableVector derVector = new ASN1EncodableVector();
                while (iter.hasNext()) {
                    List<?> next = (List<?>) iter.next();
                    int OID = ((Integer) next.get(0)).intValue();
                    switch (OID) {
                        case SUBALTNAME_DNSNAME:
                            final String dnsName = (String) next.get(1);
                            GeneralName gn = new GeneralName(GeneralName.dNSName, dnsName);
                            derVector.add(gn);

                            break;
                    }
                }
                DERSequence sequence = new DERSequence((ASN1EncodableVector)derVector);
                GeneralNames subjectAltName = new GeneralNames(sequence);
                certGen.addExtension(X509Extensions.SubjectAlternativeName, false, subjectAltName);
            }
        }

        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(caCerts[0]));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(keyPair.getPublic()));
        X509Certificate cert = certGen.generate(caKey, "BC");

        X509Certificate[] chain = new X509Certificate[caCerts.length + 1];
        System.arraycopy(caCerts, 0, chain, 1, caCerts.length);
        chain[0] = cert;
        PrivateKey pk = keyPair.getPrivate();

        //String certEntry = getCertEntry(hostData);

        keystoreCert.setKeyEntry(certEntry, pk, passwordCerts, chain);

        // TODO: #200 this creates a bunch of allocations. Avoid for now
        //saveKeystoreThread(keystoreCert, filenameCert, passwordCerts);

        return new HostKeyManager(hostData, pk, chain);
    }

    private class HostKeyManager implements X509KeyManager {

        private SiteData hostData;

        private PrivateKey pk;

        private X509Certificate[] certs;

        public HostKeyManager(SiteData hostData, PrivateKey pk,
                              X509Certificate[] certs) {
            this.hostData = hostData;
            this.pk = pk;
            this.certs = certs;
        }

        public String chooseClientAlias(String[] keyType, Principal[] issuers,
                                        Socket socket) {
            return null;
            // throw new UnsupportedOperationException("Not implemented");
        }

        public String chooseServerAlias(String keyType, Principal[] issuers,
                                        Socket socket) {
            return hostData.name;
        }

        public X509Certificate[] getCertificateChain(String alias) {
            return certs;
        }

        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return null;
            //throw new UnsupportedOperationException("Not implemented");
        }

        public PrivateKey getPrivateKey(String alias) {
            return pk;
        }

        public String[] getServerAliases(String keyType, Principal[] issuers) {

//            if (hostData.alternativeNames == null || hostData.alternativeNames.size() == 0){
//                return new String[]{hostData.name};
//            }
//
//            return (String[]) hostData.alternativeNames.toArray();
            return new String[]{hostData.name};
        }

    }

}

