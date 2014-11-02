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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dropbox.sync.android.DbxRecord;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * FavFragment which contains all favorite theaters for easier access
 * this fragment only display what MainActivity sends via favFragment.updateList(data)
 * No logic here, but UI tasks
 */
public class FavFragment extends Fragment {
    private static final String LOG_TAG = "FavFragment";
    private OnFavListener mListener;
    private ViewGroup mContainerView;
    private View emptyTv;

    public static FavFragment newInstance() {
        FavFragment fragment = new FavFragment();
        return fragment;
    }

    public FavFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_scrollable_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContainerView = (ViewGroup) view.findViewById(R.id.container);
        emptyTv = view.findViewById(android.R.id.empty);

        if (mListener != null) {
            mListener.onFavFragmentCreated();
        }
    }

    public void updateList(ArrayList<DbxRecord> records) {
        Log.i(LOG_TAG, "updateList [DBX]  #" + records.size());
        HashMap<String, ArrayList<String>> processed = new HashMap<String, ArrayList<String>>() {
        };
        for (DbxRecord dr : records) {
            ArrayList<String> a = new ArrayList<String>();
            a.add(dr.getString("group"));
            a.add(dr.getString("theaterName"));
            a.add(dr.getString("theaterCode"));
            processed.put(dr.getString("theaterCode"), a);
        }
        populateList(processed);
    }

    /**
     * This is a fallback function when Dropbox datastore gets kinky
     */
    public void updateListFromSavedFavList(ArrayList<String> records) {
        Log.i(LOG_TAG, "updateList [Saved]  #" + records.size());
        HashMap<String, ArrayList<String>> processed = new HashMap<String, ArrayList<String>>() {
        };
        for (String str : records) {
            ArrayList<String> aa = Lists.newArrayList(Splitter.on('|').trimResults().split(str));
            processed.put(aa.get(2), aa);
        }
        populateList(processed);
    }

    private void populateList(HashMap<String, ArrayList<String>> records) {
        if (mContainerView == null) {
            return;
        }
        ArrayList<String> toAdd = new ArrayList<String>();
        ArrayList<String> toRemove = new ArrayList<String>();
        // add first, remove later
        for (int i = 0; i < mContainerView.getChildCount(); i++) {
            View item = mContainerView.getChildAt(i);
            String itemTheaterCode = (String) ((TextView) item.findViewById(R.id.code)).getText();
            if (records.containsKey(itemTheaterCode)) {
                // remove from records -- left with what to add/remove later
                records.remove(itemTheaterCode);
            } else {
                // if existing fav is no more in new one, remove it.
                toRemove.add(itemTheaterCode);
            }
        }
        // what left is what we need to add to ViewGroup
        for (Map.Entry e : records.entrySet()) {
            toAdd.add(((ArrayList<String>) e.getValue()).get(2));
        }
        // UI real task
        // adding
        for (String d : toAdd) {
            ArrayList<String> al = records.get(d);
            addItem(al.get(0), al.get(1), al.get(2));
        }
        // removing
        for (int i = 0; i < mContainerView.getChildCount(); i++) {
            View item = mContainerView.getChildAt(i);
            String itemTheaterCode = (String) ((TextView) item.findViewById(R.id.code)).getText();
            if (toRemove.contains(itemTheaterCode)) {
                mContainerView.removeView(item);
            }
        }
    }

    public void displayDropboxLink(boolean doShow) {
        if (!doShow) {
            if (mContainerView == null) return;
            for (int i = 0; i < mContainerView.getChildCount(); i++) {
                View item = mContainerView.getChildAt(i);
                String itemCode = (String) ((TextView) item.findViewById(R.id.code)).getText();
                if (itemCode.toLowerCase().equals("dropbox")) {
                    mContainerView.removeView(item);
                }
            }
            if (mContainerView.getChildCount() == 0) {
                try {
                    emptyTv.setVisibility(View.VISIBLE);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            return;
        }
        // add Dropbox Link
        boolean willAdd = true;

        for (int i = 0; i < mContainerView.getChildCount(); i++) {
            View item = mContainerView.getChildAt(i);
            String itemCode = (String) ((TextView) item.findViewById(R.id.code)).getText();
            if (itemCode.toLowerCase().equals("dropbox")) {
                willAdd = false;
            }
        }
        if (!willAdd) {
            return;
        }
        try {
            emptyTv.setVisibility(View.GONE);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        ViewGroup newView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(
                R.layout.list_item_pref_checkbox, mContainerView, false);
        // Set the text in the new row to a random country.
        ((TextView) newView.findViewById(android.R.id.text1)).setText(getString(R.string.dropbox_link));
        ((TextView) newView.findViewById(R.id.code)).setText("dropbox");
        ((TextView) newView.findViewById(android.R.id.text2)).setText(getString(R.string.dropbox_link_desc));
        newView.findViewById(R.id.checkBox).setVisibility(View.GONE);
        newView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDropboxLink();
                return;
            }
        });
        mContainerView.addView(newView, mContainerView.getChildCount());
    }

    private void addItem(String group, String title, String tCode) {
        ViewGroup newView;
        try {
            newView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(
                    R.layout.list_item_fav, mContainerView, false);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }
        // if there is an addition, then hide empty text
        try {
            emptyTv.setVisibility(View.GONE);
        } catch (NullPointerException e) {
        }

        final String mGroup = group;
        final String mtCode = tCode;
        final String mtName = title;
        // Set the text in the new row to a random country.
        ((TextView) newView.findViewById(android.R.id.text1)).setText(title);
        ((TextView) newView.findViewById(R.id.code)).setText(tCode);
        TextView tv1 = (TextView) newView.findViewById(android.R.id.text1);
        tv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemPressed(mGroup, mtCode, mtName);
                return;
            }
        });
        ImageButton ib = (ImageButton) newView.findViewById(R.id.button);
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favRemoval(mGroup, mtCode);
            }
        });

        View groupColor = newView.findViewById(R.id.group_color);
        if (group.equalsIgnoreCase("sf")) {
            groupColor.setBackgroundColor(getResources().getColor(R.color.group_sf));
        } else if (group.equalsIgnoreCase("major")) {
            groupColor.setBackgroundColor(getResources().getColor(R.color.group_major));
        }
        mContainerView.addView(newView, mContainerView.getChildCount());
    }

    public void onDropboxLink() {
        if (mListener != null) {
            mListener.onDropboxLink();
        }
    }

    public void onItemPressed(String group, String tCode, String tName) {
        if (mListener != null) {
            mListener.onFavTheaterClick(group, tCode, tName);
        }
    }

    private void favRemoval(String group, String tCode) {
        if (mListener != null) {
            mListener.onFavDeletion(group, tCode);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFavListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFavListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFavListener {
        public void onFavTheaterClick(String group, String tCode, String tName);

        public void onFavDeletion(String group, String tCode);

        public void onDropboxLink();

        public void onFavFragmentCreated();
    }

}
