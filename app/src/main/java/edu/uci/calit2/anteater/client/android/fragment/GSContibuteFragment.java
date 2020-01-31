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
import android.widget.RadioButton;
import android.widget.RadioGroup;

import edu.uci.calit2.anteater.R;
import edu.uci.calit2.anteater.client.android.activity.AntMonitorMainActivity;
import edu.uci.calit2.anteater.client.android.util.UIHelper;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GSContibuteFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GSContibuteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GSContibuteFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    public GSContibuteFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment GSContibuteFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GSContibuteFragment newInstance(String param1, String param2) {
        GSContibuteFragment fragment = new GSContibuteFragment();
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
        View view = inflater.inflate(R.layout.fragment_gscontibute, container, false);

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
            contributionGroup.setEnabled(AntMonitorMainActivity.ENABLE_CONTRIBUTION_SECTIONS);
            View medButton = contributionGroup.findViewById(R.id.med_contribute);
            View medText = contributionGroup.findViewById(R.id.med_contribute_text);
            View highButton = contributionGroup.findViewById(R.id.high_contribute);
            View highText = contributionGroup.findViewById(R.id.high_contribute_text);
            medButton.setVisibility(View.GONE);
            medText.setVisibility(View.GONE);
            highButton.setVisibility(View.GONE);
            highText.setVisibility(View.GONE);
        }

        Button nextButton = (Button) view.findViewById(R.id.next);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            ((AppCompatButton) nextButton).setSupportBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accent)));
        }

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.showNextFragment(GSContibuteFragment.this);
                }
            }
        });

        Button backButton = (Button) view.findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.showPreviousFragment(GSContibuteFragment.this);
                }
            }
        });

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
