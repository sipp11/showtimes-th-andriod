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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dropbox.sync.android.DbxAccountManager;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;

import java.io.File;


/**
 * Settings Fragment
 */
public class GearFragment extends Fragment {
    ImageView playStoreIcon;
    private onGearListener mListener;
    private ShowtimesApplication app;
    private ViewGroup mSettingViewGroup;

    public static GearFragment newInstance() {
        GearFragment fragment = new GearFragment();
        return fragment;
    }

    public GearFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gear, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = ShowtimesApplication.getInstance();
        Context context = getActivity();
        try {
            String versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            ((TextView) view.findViewById(R.id.app_version)).setText("v." + versionName);
        } catch (PackageManager.NameNotFoundException e) {

        }

        mSettingViewGroup = (ViewGroup) view.findViewById(R.id.gear_container);
        ImageView iv = (ImageView) view.findViewById(R.id.logo);
        iv.setImageResource(R.drawable.ic_launcher);

        playStoreIcon = ((ImageView) view.findViewById(R.id.playStoreIcon));
        if (savedInstanceState != null) {
            Bitmap bitmap = savedInstanceState.getParcelable("playStoreIcon");
            playStoreIcon.setImageBitmap(bitmap);
        } else {
            String url = "http://zzyzx.co/static/Google_Play_Store_96.png";
            if (getResources().getDisplayMetrics().density > 1.5) {
                url = "http://zzyzx.co/static/Google_Play_Store_192.png";
            }
            getFile(playStoreIcon, url, context.getFileStreamPath(randomFile()).getAbsolutePath());
        }
        playStoreIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=co.zzyzx.showtimes.android"));
                startActivity(intent);
            }
        });

        if (mListener != null) {
            mListener.onGearFragmentCreated();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            BitmapDrawable drawable = (BitmapDrawable) playStoreIcon.getDrawable();
            Bitmap bitmap = drawable.getBitmap();
            outState.putParcelable("playStoreIcon", bitmap);
        } catch (NullPointerException e) {
        }
    }

    public void initSettingGroup(final DbxAccountManager accountManager) {
        ViewGroup newView;

        // Dropbox Setting
        if (accountManager.hasLinkedAccount()) {
            newView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(
                    R.layout.list_item_pref_checkbox, mSettingViewGroup, false);
            ((TextView) newView.findViewById(android.R.id.text1)).setText(getString(R.string.dropbox_unlink));
            ((TextView) newView.findViewById(android.R.id.text2))
                    .setText(getString(R.string.dropbox_unlink_desc));
            newView.findViewById(R.id.checkBox).setVisibility(View.GONE);
            newView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    accountManager.unlink();
                    mSettingViewGroup.removeView(v);
                }
            });
            mSettingViewGroup.addView(newView, mSettingViewGroup.getChildCount());
        }
        // Opensource thingy
        newView = (ViewGroup) LayoutInflater.from(getActivity()).inflate(
                R.layout.list_item_pref_checkbox, mSettingViewGroup, false);
        // Set the text in the new row to a random country.
        ((TextView) newView.findViewById(android.R.id.text1)).setText(getString(R.string.opensource_license));
        newView.findViewById(android.R.id.text2).setVisibility(View.GONE);
        newView.findViewById(R.id.checkBox).setVisibility(View.GONE);
        newView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), OpenSourcesActivity.class);
                startActivity(intent);
            }
        });
        mSettingViewGroup.addView(newView, mSettingViewGroup.getChildCount());
    }

    private String randomFile() {
        return ((Long) Math.round(Math.random() * 1000)).toString() + ".png";
    }

    private void assignImageView(final ImageView iv, final BitmapDrawable bd) {
        try {
            iv.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    iv.setImageDrawable(bd);
                }
            });
        } catch (NullPointerException e) {

        }
    }

    private void getFile(final ImageView iv, String url, final String filename) {
        AsyncHttpClient.getDefaultInstance().executeFile(new AsyncHttpGet(url), filename, new AsyncHttpClient.FileCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse response, File result) {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }
                Bitmap bitmap = BitmapFactory.decodeFile(filename);
                result.delete();
                if (bitmap == null)
                    return;
                try {
                    BitmapDrawable bd = new BitmapDrawable(getActivity().getResources(), bitmap);
                    assignImageView(iv, bd);
                } catch (NullPointerException npe) {
                }
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (onGearListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement onGearListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface onGearListener {
        public void onDropboxLink();

        public void onGearFragmentCreated();
    }

}
