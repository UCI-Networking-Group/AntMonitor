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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.io.File;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.antmonitor.lib.logging.TrafficLogFiles;

/**
 * <p>
 *      Builds and manages a confirmation dialog for when the user initiates log file upload.
 * </p>
 *
 * @author Simon Langhoff, Janus Varmarken
 */
public class ConfirmUploadDialogFragment extends DialogFragment {

    private static final String dataUnitArg = "dataUnitArg";

    /**
     * Factory method for {@code ConfirmUploadDialogFragment}.
     * @param dataUnit Specifies what unit should be used for the byte count when confronting
     *                     the user with the total amount of bytes that will be sent to the server
     *                     if she confirms the upload.
     * @return A new {@code ConfirmUploadDialogFragment} instance reflecting the parameters provided
     *         here.
     */
    public static ConfirmUploadDialogFragment newInstance(MemoryUnit dataUnit) {
        ConfirmUploadDialogFragment fragment = new ConfirmUploadDialogFragment();

        Bundle args = new Bundle();
        args.putSerializable(dataUnitArg, dataUnit);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Specifies what unit should be used for the byte count when confronting the user with the
     * total amount of bytes that will be sent to the server if she confirms the upload.
     */
    private MemoryUnit mDataUnit;

    /**
     * The {@code Activity} hosting this fragment.
     */
    private ConfirmUploadDialogListener mHost;



    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Enforce interface implementation requirement.
        mHost = (ConfirmUploadDialogListener) activity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fetch parameters.
        mDataUnit = (MemoryUnit) getArguments().getSerializable(dataUnitArg);
        if (mDataUnit == null) {
            throw new NullPointerException("missing value for file size unit");
        }
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder diaBuilder = new AlertDialog.Builder(getActivity());
        diaBuilder.setTitle(R.string.dialog_title_confirm_upload);

        // Get snapshot of files that are ready for upload at the point in time when the dialog is created.
        final File[] filesToUpload = TrafficLogFiles.getCompleted(getActivity());

        // Calculate the aggregated file size across all files currently available for upload.
        long aggregatedFileSize = 0L;
        for(int i = 0; i < filesToUpload.length; i++) {
            aggregatedFileSize += filesToUpload[i].length();
        }
        // Prepare the dialog message.
        String prettyfiedBytecount = mDataUnit.prettyPrint(aggregatedFileSize);
        String diaMsg = getResources().getString(R.string.dialog_detail_text_confirm_upload, prettyfiedBytecount);
        diaBuilder.setMessage(diaMsg);

        // Set buttons to invoke host when clicked.
        diaBuilder.setPositiveButton(R.string.dialog_btn_positive_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ConfirmUploadDialogFragment.this.mHost.onUploadConfirmed(ConfirmUploadDialogFragment.this, filesToUpload);
            }
        });
        diaBuilder.setNegativeButton(R.string.dialog_btn_negative_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ConfirmUploadDialogFragment.this.mHost.onUploadDeclined(ConfirmUploadDialogFragment.this);
            }
        });
        return diaBuilder.create();
    }

    /**
     * Callback interface. Used by
     * {@link ConfirmUploadDialogFragment} to
     * deliver information to its host.
     * Any activity hosting a
     * {@link ConfirmUploadDialogFragment}
     * <em>must</em> implement this interface (if not, a crash will occur during
     * {@link #onAttach(android.app.Activity)}).
     */
    public interface ConfirmUploadDialogListener {
        /**
         * Invoked when the user confirms to start the file upload.
         *
         * @param dialogFragment The {@link ConfirmUploadDialogFragment}
         *                       that invoked this method.
         * @param filesToUpload The set of files that should be uploaded (i.e. the set of files that
         *                      has the user's approval). Note that this might be a subset of
         *                      {@link edu.uci.calit2.antmonitor.lib.logging.TrafficLogFiles#getCompleted(android.content.Context)}
         *                      as the set of completed files may have increased if there is heavy
         *                      data traffic in the background while waiting for the user to confirm
         *                      the upload. <em>As such you should only upload {@code filesToUpload} and
         *                      not the super set exposed by
         *                      {@link edu.uci.calit2.antmonitor.lib.logging.TrafficLogFiles#getCompleted(android.content.Context)}
         *                      .</em>
         */
        void onUploadConfirmed(ConfirmUploadDialogFragment dialogFragment, File[] filesToUpload);

        /**
         * Invoked when the user declines to start the file upload.
         *
         * @param dialogFragment The {@link ConfirmUploadDialogFragment}
         *                       that invoked this method.
         */
        void onUploadDeclined(ConfirmUploadDialogFragment dialogFragment);
    }

    /**
     * Utility enum for representing units used when dealing with memory.
     */
    public static enum MemoryUnit {
        BYTES {
            @Override
            public String prettyPrint(long byteCount) {
                if (byteCount == 1) {
                    return String.format("%d byte", byteCount);
                } else {
                    return String.format("%d bytes", byteCount);
                }
            }
        }, KILO_BYTES {
            @Override
            public String prettyPrint(long byteCount) {
                // convert to kilo bytes
                double kb = Long.valueOf(byteCount).doubleValue() / 1024.0;
                return String.format("%.2f KB", kb);
            }
        }, MEGA_BYTES {
            @Override
            public String prettyPrint(long byteCount) {
                // convert bytes to mega bytes.
                double mb = Long.valueOf(byteCount).doubleValue() / 1024.0 / 1024.0;
                return String.format("%.2f MB", mb);
            }
        }, GIGA_BYTES {
            @Override
            public String prettyPrint(long byteCount) {
                // convert bytes to giga bytes
                double gb = Long.valueOf(byteCount).doubleValue() / 1024.0 / 1024.0 / 1024.0;
                return String.format("%.2f GB", gb);
            }
        };

        /**
         * Pretty prints the given number of bytes followed by the memory unit specified by this enum instance.
         * This method converts the given byte count into its corresponding KB, MB, GB value.
         * E.g. if you pass 1024 as the value for {@code byteCount} to the {@code KILO_BYTES} instance,
         * this method will return the string "1 KB".
         *
         * @param byteCount the number of bytes.
         * @return A string formatted like in the given example.
         */
        public abstract String prettyPrint(long byteCount);
    }
}
