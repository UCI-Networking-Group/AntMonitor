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
package edu.uci.calit2.anteater.client.android.analysis;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.uci.calit2.anteater.client.android.database.PrivacyDB;
import edu.uci.calit2.anteater.client.android.database.PrivacyFilterModel;
import edu.uci.calit2.anteater.client.android.database.PrivacyLeakModel;
import edu.uci.calit2.anteater.client.android.signals.LocationMonitor;
import edu.uci.calit2.anteater.client.android.util.NotificationsHelper;
import edu.uci.calit2.anteater.client.android.util.OpenAppDetails;
import edu.uci.calit2.antmonitor.lib.R;
import edu.uci.calit2.antmonitor.lib.logging.ConnectionValue;
import edu.uci.calit2.antmonitor.lib.logging.PacketAnnotation;
import edu.uci.calit2.antmonitor.lib.util.AhoCorasickInterface;
import edu.uci.calit2.antmonitor.lib.util.IpDatagram;
import edu.uci.calit2.antmonitor.lib.util.TCPReassemblyInfo;
import edu.uci.calit2.antmonitor.lib.vpn.OutPacketFilter;

/**
 * @author Anastasia Shuba
 */
public class InspectorPacketFilter extends OutPacketFilter {

    /** Tag for logging */
    private final String TAG = InspectorPacketFilter.class.getSimpleName();

    /** Annotation used for blocking packets */
    private final PacketAnnotation BLOCK_ANNOTATION = new PacketAnnotation(false);

    /** Indicates whether or not inspection is enabled */
    private static boolean ENABLED;

    /** Used when processing lists of leaks */
    final private int INVALID_INDEX = -1;

    /** Database for logging leaks, extracting filters, etc. */
    private PrivacyDB DATABASE;

    /** Used for detecting current location */
    private LocationMonitor locationMonitor;

    /** ID used for notifications about leaks */
    private int id;

    /** Icon used in notification displayed upon a leak occurrence */
    private final Bitmap icon;

    /** Used for controlling the visualization module */
    private OpenAppDetails openAppDetails;

    /**
     * Create InspectorPacketFilter. Prepare database, icon for notifications, etc.
     * @param cxt - Context used by Broadcasts, notifications, and etc.
     */
    public InspectorPacketFilter(Context cxt) {
        super(cxt);

        // Enabled by default
        ENABLED = true;

        // Ready the DB
        DATABASE = PrivacyDB.getInstance(cxt);

        // Ready location detector
        locationMonitor = LocationMonitor.getInstance(cxt);

        // Ready the icon to avoid decoding resources
        icon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                cxt.getResources(), R.mipmap.ic_launcher), 128, 128, false);

        // Start with zero for notification id
        id = 0;

        openAppDetails = new OpenAppDetails(cxt);
    }

    /**
     * Accepts packets that do not contain privacy leaks. Since this method is invoked by multiple
     * threads, it locks on {@code this} object in order to avoid having the list of detected leaks
     * change by native code in the midst of execution.
     * @param packet - the decrypted TCP packet to inspect
     * @param tcpInfo - TCP parameters belonging to this packet
     */
    @Override
    public PacketAnnotation acceptDecryptedSSLPacket(final ByteBuffer packet,
                                                     TCPReassemblyInfo tcpInfo) {
        if (!ENABLED)
            return DEFAULT_ANNOTATION;

        final ArrayList<String> foundStrings = AhoCorasickInterface.getInstance().
                search(packet, packet.limit());
        if (foundStrings == null || foundStrings.isEmpty())
            return DEFAULT_ANNOTATION; // No malicious strings found, allow packet

        // We found something malicious, so
        // this packet gets to skip the line for mapping since
        // we need to know what app is doing this right away
        ConnectionValue v = mapParamsToApp(tcpInfo.getRemoteIp(), tcpInfo.getSrcPort(),
                tcpInfo.getDestPort());
        String app = (v.getVersionNum() != null) ? v.getAppName() : "Unknown";
        return processLeakNew(packet, app, tcpInfo.getRemoteIp(), foundStrings);
    }

    /**
     * Accepts packets that do not contain privacy leaks. Since this method is invoked by multiple
     * threads, it locks on {@code this} object in order to avoid having the list of detected leaks
     * change by native code in the midst of execution.
     * @param packet - the IP datagram to inspect
     */
    @Override
    public PacketAnnotation acceptIPDatagram(final ByteBuffer packet) {
        if (!ENABLED)
            return DEFAULT_ANNOTATION;

        // TODO: ignore non-TCP packets
        // TODO: Figure out a graceful way to discard headers later...
        final ArrayList<String> foundStrings = AhoCorasickInterface.getInstance().
                search(packet, packet.limit());
        if (foundStrings == null || foundStrings.isEmpty())
            return DEFAULT_ANNOTATION; // No malicious strings found, allow packet

        // We found something malicious, so
        // this packet gets to skip the line for mapping since
        // we need to know what app is doing this right away
        ConnectionValue v = mapDatagramToApp(packet);
        String app = (v.getVersionNum() != null) ? v.getAppName() : "Unknown";
        return processLeakNew(packet, app, IpDatagram.readDestinationIP(packet), foundStrings);
    }

    /**
     * Turns the leak strings into something usable so that we can process the leak later
     * @return list of {@link PrivacyLeakModel}
     */
    private List<PrivacyLeakModel> processFoundStrings(String packageName, ArrayList<String> foundStrings) {
        final int LOOP_SIZE = 2;
        int latIndex = INVALID_INDEX;
        int lonIndex = INVALID_INDEX;

        //Log.d(TAG, "location value: " + locationMonitor.getLatitude() + ", " + locationMonitor.getLongitude() );

        List<PrivacyLeakModel> privacyLeakModels = new ArrayList<>();
        for (int i = 0; i < foundStrings.size(); i += LOOP_SIZE) {

            String piiValue = foundStrings.get(i);
            String piiIndex = foundStrings.get(i+1);

            // Detect a location leak but process it in this block yet
            if (isLatitudeLeak(piiValue)) {
                latIndex = i;
                continue;
            } else if (isLongitudeLeak(piiValue)) {
                lonIndex = i;
                continue;
            }

            // Check if we have a filter saved for this case
            PrivacyFilterModel privacyFilter = DATABASE.convertToPrivacyFilterModel(DATABASE.getAnyPrivacyFilter(packageName, piiValue));

            PrivacyLeakModel leak = new PrivacyLeakModel(packageName, piiValue, piiIndex, privacyFilter);
            privacyLeakModels.add(leak);

        }

        // build leak for location if it happened
        if (latIndex > INVALID_INDEX || lonIndex > INVALID_INDEX) {
            PrivacyFilterModel privacyFilter = DATABASE.convertToPrivacyFilterModel(DATABASE.getAnyPrivacyFilter(packageName, PrivacyDB.DEFAULT_PII_VALUE__LOCATION));

            // ANTMONITOR-144: if we know it is a location leak, then we should use the entire the locationStr as the leak value
            String piiValue = LocationMonitor.getInstance(mContext).getLocationStr();
            PrivacyLeakModel leak = new PrivacyLeakModel(packageName, piiValue, null, privacyFilter, latIndex, lonIndex);
            privacyLeakModels.add(leak);
        }

        return privacyLeakModels;
    }

    private boolean isLatitudeLeak(String piiValue) {
        List values = locationMonitor.getLatitudeValues();
        return values.contains(piiValue);
    }

    private boolean isLongitudeLeak(String piiValue) {
        List values = locationMonitor.getLongitudeValues();
        return values.contains(piiValue);
    }

    /** Logs an entire packet of leaks in the Log Table
     * @param leaks list of leaks
     **/
    private void logBlockPacket(List<PrivacyLeakModel> leaks, String remoteIp) {
        for (PrivacyLeakModel leak : leaks) {
            String piiLabel = null;
            if (leak.getPrivacyFilterModel() != null) {
                piiLabel = leak.getPrivacyFilterModel().getPiiLabel();
            }
            DATABASE.logLeakAsyncTask(this.mContext, null, leak.getPackageName(), remoteIp,
                    leak.getPiiValue(), piiLabel, ActionReceiver.ACTION_DENY);
        }
    }

    /** If an action determined for a leak is a DENY, then return true (block the whole packet)
     * @param leaks list of leaks
     * @return boolean for whether we should block the entire packet
     **/
    private boolean shouldBlockPacket(List<PrivacyLeakModel> leaks){
        for (PrivacyLeakModel leak : leaks) {
            if (leak.getPrivacyFilterModel() != null) {
                if (leak.getPrivacyFilterModel().getAction().equals(ActionReceiver.ACTION_DENY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** If we have an existing filter, then we look at what action to do from it. If there is no action,
     *  will ask the user in a notification. If the filter is a global, we will also ask the user in a notification.
     * @param leak one {@link PrivacyLeakModel}
     **/
    private void processNotificationForLeak(PrivacyLeakModel leak){
        String action = null;
        boolean isGlobalFilter = true;

        // if there is an existing filter, we take the action from it and whether it is global filter
        if (leak.getPrivacyFilterModel() != null){
            String app = leak.getPrivacyFilterModel().getPackageName();
            action = leak.getPrivacyFilterModel().getAction();
            isGlobalFilter = app == null || app.isEmpty();
        }

        if (action == null){
            sendNotification(leak.getPackageName(), leak.getPiiValue(), null, ActionReceiver.ACTION_DEFAULT);

        } else if(isGlobalFilter) {
            sendNotification(leak.getPackageName(), leak.getPiiValue(), leak.getPrivacyFilterModel().getPiiLabel(), action);
        }
    }

    /**
     * Process a leak that occurred within the given packet
     * Notifies the user of the leak and hashes, accepts, or denies the packet
     * based on the user's preferences
     * @param packet the packet containing a leak
     * @param app package name of the app responsible for the leak
     * @param foundStrings the leaking strings
     * @return {@code true} if the packet can get sent out (hash or allow case), {@code false} o.w.
     */
    private PacketAnnotation processLeakNew(final ByteBuffer packet, String app, String remoteIp,
                                            ArrayList<String> foundStrings) {

        // TODO: this method is horribly inefficient, mostly due to location stuff

        List<PrivacyLeakModel> leaks = processFoundStrings(app, foundStrings);

        if (shouldBlockPacket(leaks)) {
            logBlockPacket(leaks, remoteIp);
            // TODO: ANTMONITOR-283 - fire notification?
            // return false means the packet will not go through
            return BLOCK_ANNOTATION;
        }

        // If we make it here, it means packet is not blocked.
        for (PrivacyLeakModel leak : leaks) {

            String action = null;
            String piiLabel = null;

            if (leak.getPrivacyFilterModel() != null){
                action = leak.getPrivacyFilterModel().getAction();
                piiLabel = leak.getPrivacyFilterModel().getPiiLabel();
            }

            if (action != null) {
                if (action.equals(ActionReceiver.ACTION_ALLOW)) {
                    String logLeakPiiValue = leak.getPiiValue();
                    // ANTMONITOR-144: if we know it is a location leak, then we should use the entire the locationStr as the leak value
                    if (piiLabel.equals(PrivacyDB.DEFAULT_PII__LOCATION)) {
                        logLeakPiiValue = LocationMonitor.getInstance(mContext).getLocationStr();
                    }
                    DATABASE.logLeakAsyncTask(this.mContext, null, leak.getPackageName(), remoteIp,
                            logLeakPiiValue, piiLabel, ActionReceiver.ACTION_ALLOW);

                } else if (action.equals(ActionReceiver.ACTION_HASH)) {
                    if (leak.getLatitudeIndex() > INVALID_INDEX || leak.getLongitudeIndex() > INVALID_INDEX) {
                        // hash location leak
                        // Note: Make sure we always use the leak.getLatitudeIndex() and leak.getLongitudeIndex() to hash and not the leak.getPiiValue() for locations
                        hashLocation(leak.getPackageName(), remoteIp, foundStrings, leak.getLatitudeIndex(), leak.getLongitudeIndex(), packet);
                    } else {
                        // hash other type of leak
                        replaceString(piiLabel, leak.getPiiValue().length(),
                                        Integer.parseInt(leak.getPiiIndex()), packet);
                        DATABASE.logLeakAsyncTask(this.mContext, null, leak.getPackageName(),
                                remoteIp, leak.getPiiValue(), piiLabel, ActionReceiver.ACTION_HASH);
                    }
                }
            }

            processNotificationForLeak(leak);
        }

        // If we made it here, packet is allowed to go through
        return DEFAULT_ANNOTATION;
    }

    private void hashLocation(String app, String remoteIp, ArrayList<String> foundStrings,
                              int latIndex, int lonIndex, ByteBuffer packet) {
        Random r = new Random();
        // The hundredth ranges from 0 to 99
        Integer hundredth = r.nextInt(100);

        // Aho-Corasick gives the ending position of the string, we need to back 2 spaces
        // to be in the position of the hundredth
        // since ANTMONITOR-203: We now hash the 2 numbers after the decimal.
        if (latIndex > INVALID_INDEX) {
            putNewString(Integer.parseInt(foundStrings.get(latIndex + 1)), hundredth.toString(),
                    packet);
        }

        // Repeat for longitude
        if (lonIndex > INVALID_INDEX) {
            hundredth = r.nextInt(100);
            putNewString(Integer.parseInt(foundStrings.get(lonIndex + 1)), hundredth.toString(),
                    packet);
        }

        // log
        DATABASE.logLeakAsyncTask(this.mContext, null, app, remoteIp,
                LocationMonitor.getInstance(mContext).getLocationStr(),
                PrivacyDB.DEFAULT_PII__LOCATION, ActionReceiver.ACTION_HASH);
    }

    /**
     * Replaces the given PII string. Changes position of the packet buffer
     * @param piiLabel the label of the leaking PII
     * @param piiLen the length of the leaking PII value
     * @param index starting position of the pii in the packet
     * @param packetBuff ByteBuffer representation of the packet
     */
    private void replaceString(String piiLabel, int piiLen, int index, ByteBuffer packetBuff) {
        String replacement;
        Random r = new Random();

        // Check if this is one of the defaults
        if (PrivacyDB.DEFAULT_PII__IMEI.equals(piiLabel) ||
                PrivacyDB.DEFAULT_PII__PHONE_NUMBER.equals(piiLabel)) {
            // IMEI is made of digits only
            replacement = generateStringOfNumbers(r, piiLen);
        } else {
            // Device ID is made of digits and letters
            // and assume any custom strings are also of digits and letters
            replacement = generateString(r, piiLen);
            // TODO: other default strings are more difficult to fake (email)
        }

        putNewString(index, replacement, packetBuff);
    }

    /**
     * Convenience method to put the given replacement string into the given ByteBuffer
     * This method changes the position of the ByteBuffer
     * @param position starting position where to put the given string
     * @param replacement the new string
     * @param packetBuff the packet
     */
    private void putNewString(int position, String replacement, ByteBuffer packetBuff) {
        packetBuff.position(position);
        packetBuff.put(replacement.getBytes());
    }

    /**
     * Creates a notification for the user
     * @param pkgName package name of the app creating the leak
     * @param pii the leaking string
     */
    private void sendNotification(String pkgName, String pii, String piiLabel, String action) {

        if (NotificationsHelper.hasLeakNotification(pkgName, piiLabel)) {
            //Log.d(TAG, "Notification already sent for " + pkgName + ", " + piiLabel);
            return;
        }

        String appName = pkgName;
        PackageManager pm = mContext.getPackageManager();
        try {
            appName = pm.getApplicationLabel(pm.getApplicationInfo(pkgName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            // do nothing, we will keep it as pkgName
        }
        String message = "";
        String title = piiLabel == null ?
                "String '" + pii + "'" : "Your " + piiLabel;
        if (action != null) {
            if (action.equals(ActionReceiver.ACTION_ALLOW)) {
                title += " was exposed before";
                message = "being sent by " + appName;
            }
            if (action.equals(ActionReceiver.ACTION_HASH)) {
                title += " was hashed before";
                message = "being sent by " + appName;
            }
            if (action.equals(ActionReceiver.ACTION_DENY)) {
                title += " was blocked and";
                message = "not sent by " + appName;
            }
        } else {
            title += " was exposed before";
            message = "being sent by " + appName;
        }

        message += ". Future action?";

        // Build intent for notification content
        Intent viewIntent = new Intent(mContext, InspectorPacketFilter.class);
        PendingIntent viewPendingIntent =
                PendingIntent.getActivity(mContext, 0, viewIntent, 0);

        Intent allowIntent = new Intent().setAction(ActionReceiver.ACTION_ALLOW)
                                            .putExtra(ActionReceiver.NOTIFICATION_ID, id)
                                            .putExtra(ActionReceiver.APP, pkgName)
                                            .putExtra(ActionReceiver.STRING, pii)
                                            .putExtra(ActionReceiver.LABEL, piiLabel);
        PendingIntent pIntentAllow = PendingIntent.getBroadcast(mContext, id,
                allowIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent hashIntent = allowIntent.setAction(ActionReceiver.ACTION_HASH);
        PendingIntent pIntentHash = PendingIntent.getBroadcast(mContext, id,
                hashIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent denyIntent = allowIntent.setAction(ActionReceiver.ACTION_SKIP);
        PendingIntent pIntentDeny = PendingIntent.getBroadcast(mContext, id,
                denyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(mContext)
                        .setLargeIcon(icon)
                        .setSmallIcon(R.mipmap.shield)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentIntent(viewPendingIntent)
                        .setWhen(0) // this is to ensure buttons show
                        .addAction(R.drawable.check_mark, "Allow", pIntentAllow)
                        .addAction(R.drawable.action_hash, "Hash", pIntentHash)
                        .addAction(R.drawable.deny_mark, "Block", pIntentDeny)
                        .setDeleteIntent(pIntentDeny); // If user simply dismisses, deny packet

        // Another trick to make buttons show
        // From http://stackoverflow.com/questions/18249871/android-notification-buttons-not-showing-up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            notificationBuilder.setPriority(Notification.PRIORITY_MAX);

        // Issue notification
        NotificationManagerCompat.from(mContext).notify(id, notificationBuilder.build());
        NotificationsHelper.trackLeakNotification(id, pkgName, piiLabel);

        // Increase id for the next notification. If it's 100, rollover back to 0
        id++;
        if (id == 100)
            id = 0;
    }

    private String generateString(Random rng, int length) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        char[] text = new char[length];
        for (int i = 0; i < length; i++)
            text[i] = alphabet.charAt(rng.nextInt(alphabet.length()));

        return new String(text);
    }

    private String generateStringOfNumbers(Random rng, int length) {
        // Variables used to generate random strings based on ASCII encodings
        // From ASCII table: 48-57 are characters representing 1-9
        int max = 58; // make 57 inclusive
        int min = 48;
        int randomNum;
        char[] cArray = new char[length];

        for (int i = 0; i < length; i++) {
            randomNum = rng.nextInt(max - min) + min;
            cArray[i] = (char) randomNum;
        }

        return new String(cArray);
    }

    /**
     * Disables the Inspector. This happens when
     * the user is not looking for any strings.
     */
    public static void disable() { ENABLED = false; }

    /**
     * Enables the Inspector. This happens when the users adds some
     * strings to search for.
     */
    public static void enable() { ENABLED = true; }

    public void onTCPConnectionClosed(String srcIp, String dstIp, int srcPort, int destPort) {
        openAppDetails.removeConnection(dstIp, srcPort, destPort);
    }

    public void onTCPConnectionOpened(String remoteIp, int srcPort, int destPort) {
        openAppDetails.addTCPConnection(remoteIp, srcPort, destPort);
    }
}
