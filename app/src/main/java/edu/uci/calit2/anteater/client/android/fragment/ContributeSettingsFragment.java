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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.activity.AntMonitorMainActivity;
import edu.uci.calit2.anteater.client.android.activity.ApplicationsForLoggingActivity;
import edu.uci.calit2.anteater.client.android.util.UIHelper;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ContributeSettingsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ContributeSettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContributeSettingsFragment extends Fragment {
    private OnFragmentInteractionListener mListener;

    public ContributeSettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ContributeSettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ContributeSettingsFragment newInstance(String param1, String param2) {
        ContributeSettingsFragment fragment = new ContributeSettingsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contribute_settings, container, false);

        RadioGroup contributionGroup = (RadioGroup) view.findViewById(R.id.contribute_group);

        contributionGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int radioButtonId = radioGroup.getCheckedRadioButtonId();
                switch(radioButtonId){
                    case R.id.low_contribute:
                        UIHelper.setContributionLevel(getContext(), radioButtonId);
                        break;
                    case R.id.med_contribute:
                        UIHelper.setContributionLevel(getContext(), radioButtonId);
                        break;
                    case R.id.high_contribute:
                        UIHelper.setContributionLevel(getContext(), radioButtonId);
                        break;
                }
                RadioButton button = (RadioButton) radioGroup.findViewById(radioButtonId);
                button.setChecked(true);
            }
        });

        int defaultContributionLevel = UIHelper.getDefaultContributionLevel(getContext());
        contributionGroup.check(defaultContributionLevel);

        if (!AntMonitorMainActivity.ENABLE_CONTRIBUTION_SECTIONS) {
            final int MEDIUM_INDEX = 2;
            for (int i = MEDIUM_INDEX; i < contributionGroup.getChildCount(); i++) {
                View childView = contributionGroup.getChildAt(i);
                childView.setEnabled(AntMonitorMainActivity.ENABLE_CONTRIBUTION_SECTIONS);
            }
        }

        TextView uploadNow = (TextView) view.findViewById(R.id.upload_now);
        uploadNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UIHelper.uploadLogs(getActivity(), getContext());
                // if successful
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.logs_uploaded_alert)
                        .setTitle(R.string.completed);
                builder.setPositiveButton(R.string.ok, new  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        TextView privacyTermsTextView = (TextView) view.findViewById(R.id.privacy_terms);
        privacyTermsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.getting_started_privacy_terms_info)
                        .setTitle(R.string.getting_started_privacy_terms_title);
                builder.setPositiveButton(R.string.ok, new  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        TextView selectApplications = (TextView) view.findViewById(R.id.select_applications);
        selectApplications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ApplicationsForLoggingActivity.class);
                startActivity(intent);
            }
        });
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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
