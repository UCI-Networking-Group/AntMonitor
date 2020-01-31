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
package edu.uci.calit2.anteater.client.android.fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.activity.AntMonitorMainActivity;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GSPrivacyFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GSPrivacyFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GSPrivacyFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    public GSPrivacyFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment GSPrivacyFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GSPrivacyFragment newInstance(String param1, String param2) {
        GSPrivacyFragment fragment = new GSPrivacyFragment();
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
        View view = inflater.inflate(R.layout.fragment_gsprivacy, container, false);
        Button nextButton = (Button) view.findViewById(R.id.next);
        Button startAntmonitor = (Button) view.findViewById(R.id.start_antmonitor);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            ((AppCompatButton) nextButton).setSupportBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accent)));
            ((AppCompatButton) startAntmonitor).setSupportBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accent)));
        }

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.showNextFragment(GSPrivacyFragment.this);
                }
            }
        });

        startAntmonitor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.showNextFragment(GSPrivacyFragment.this);
                }
            }
        });

        Button backButton = (Button) view.findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.showPreviousFragment(GSPrivacyFragment.this);
                }
            }
        });

        // If Contribute is on then show different buttons
        if (AntMonitorMainActivity.ENABLE_CONTRIBUTION_SECTIONS) {
            startAntmonitor.setVisibility(View.GONE);
        } else {
            nextButton.setVisibility(View.GONE);
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
        void showNextFragment(Fragment currentFragment);
        void showPreviousFragment(Fragment currentFragment);
    }
}
