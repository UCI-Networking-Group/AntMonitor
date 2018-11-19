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
import android.widget.Button;
import android.widget.TextView;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.activity.AntMonitorMainActivity;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GSMainAboutFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GSMainAboutFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GSMainAboutFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    public static final String GETTING_STARTED_VIEW = "GETTING_STARTED_VIEW";

    public GSMainAboutFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment GSMainFragment.
     */
    public static GSMainAboutFragment newInstance(String param1, String param2) {
        GSMainAboutFragment fragment = new GSMainAboutFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
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
        View view = inflater.inflate(R.layout.fragment_gsmainabout, container, false);
        TextView privacyTermsTextView = (TextView) view.findViewById(R.id.privacy_terms_link);
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

        Button learnMoreButton = (Button) view.findViewById(R.id.learn_more);
        if (learnMoreButton != null) {
            learnMoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent learnMoreLink = new Intent(Intent.ACTION_VIEW);
                    learnMoreLink.addCategory(Intent.CATEGORY_BROWSABLE);
                    learnMoreLink.setData(Uri.parse(getResources().getString(R.string.getting_started_learn_more_url)));
                    startActivity(learnMoreLink);
                }
            });
        }

        TextView contributeLogTextView = (TextView) view.findViewById(R.id.contribute_logs_title);
        if (!AntMonitorMainActivity.ENABLE_CONTRIBUTION_SECTIONS) {
            contributeLogTextView.setText(getResources().getString(R.string.getting_started_contribute_off));
        }

        Bundle args = getArguments();
        if (args != null) {
            boolean isGettingStartedView = args.getBoolean(GETTING_STARTED_VIEW, true);
            if (!isGettingStartedView) {
                if (learnMoreButton != null) {
                    learnMoreButton.setVisibility(View.VISIBLE);
                }

                TextView gettingStartedTextView = (TextView) view.findViewById(R.id.getting_started_textView);
                if (gettingStartedTextView != null) {
                    gettingStartedTextView.setVisibility(View.GONE);
                }
            }
        }

        return view;
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
        void onFragmentInteraction(Uri uri);
    }
}
