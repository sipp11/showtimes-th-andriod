/*
 * Copyright (C) 2014 Zzyzx Co., Ltd. and Sippakorn Khaimook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.zzyzx.showtimes.android;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A Group Fragment which so far is static
 * since there are only 2 groups: sf & major.
 */
public class GroupFragment extends Fragment {

    private OnGroupListener mListener;
    private ViewGroup mContainerView;

    public static GroupFragment newInstance() {
        GroupFragment fragment = new GroupFragment();
        /*Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);*/
        return fragment;
    }

    public GroupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            /*mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);*/
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_scrollable_list, container, false);
    }

    public void onGroupPressed(String str) {
        if (mListener != null) {
            mListener.onGroupSelected(str);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContainerView = (ViewGroup) view.findViewById(R.id.container);
        initGroup();
    }

    private void initGroup() {
        /*
         * since we have exactly 2 groups and it's not going to change in showtimes-serv
         * in a programmatical way anytime soon, we can build from static data.
         */
        addItem("SF", "sf");
        addItem("Major", "major");
    }

    private void addItem(String title, String options) {
        // Instantiate a new "row" view.
        final ViewGroup newView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(
                R.layout.list_item_theater, mContainerView, false);

        // if there is an addition, then hide empty text
        try {
            getView().findViewById(android.R.id.empty).setVisibility(View.GONE);
        } catch (NullPointerException e) {
        }

        final String mOptions = ((options != null) ? options : title);
        // Set the text in the new row to a random country.
        ((TextView) newView.findViewById(android.R.id.text1)).setText(title);
        TextView tv1 = (TextView) newView.findViewById(android.R.id.text1);
        tv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onGroupPressed(mOptions);
                return;
            }
        });
        newView.findViewById(R.id.button_container).setVisibility(View.GONE);

        View groupColor = newView.findViewById(R.id.group_color);
        if (title.equalsIgnoreCase("sf")) {
            groupColor.setBackgroundColor(getResources().getColor(R.color.group_sf));
        } else if (title.equalsIgnoreCase("major")) {
            groupColor.setBackgroundColor(getResources().getColor(R.color.group_major));
        }
        mContainerView.addView(newView, mContainerView.getChildCount());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnGroupListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnGroupListener {
        public void onGroupSelected(String sel);
    }

}
