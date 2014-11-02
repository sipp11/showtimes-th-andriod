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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * ShowtimesActivity display all showtimes for each theater
 * by fetching data from http://showtimes.everyday.in.th/api/
 * and displaying in ListView-like
 */
public class ShowtimesActivity extends ActionBarActivity {
    final private String LOG_TAG = "ShowtimesAct";
    private ViewGroup mContainerView;
    final SimpleDateFormat iso_format = new SimpleDateFormat("yyyy-MM-dd");
    ShowtimesApplication app;
    private String group;
    private String tCode;
    private String tName;
    private float metricsDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrollable_list);
        app = ShowtimesApplication.getInstance();
        mContainerView = (ViewGroup) findViewById(R.id.container);
        ((TextView) findViewById(android.R.id.empty)).setText(R.string.loading);
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            group = intent.getStringExtra("group");
            tCode = intent.getStringExtra("tCode");
            tName = intent.getStringExtra("tName");
        } else {
            group = savedInstanceState.getString("group");
            tCode = savedInstanceState.getString("tCode");
            tName = savedInstanceState.getString("tName");
        }
        populateShowtimes(group, tCode);
        initActionBar(tName);


        DisplayMetrics metrics = getResources().getDisplayMetrics();
        metricsDensity = metrics.density;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("group", group);
        outState.putString("tCode", tCode);
        outState.putString("tName", tName);
    }

    private void populateShowtimes(String group, String tCode) {
        // prevent error from blank fetching
        if (group.isEmpty() || tCode.isEmpty()) {
            ((TextView) findViewById(android.R.id.empty)).setText(R.string.empty_data);
            return;
        }
        String url = app.apiBaseUrl + "showtimes/" + group + "/" + tCode + "/?d="
                + iso_format.format(new Date());

        final Handler mHandler = new Handler();
        AsyncHttpClient.getDefaultInstance()
                .executeJSONObject(new AsyncHttpGet(url), new AsyncHttpClient.JSONObjectCallback() {
                    @Override
                    public void onCompleted(Exception e, AsyncHttpResponse source, JSONObject result) {
                        if (e != null) {
                            e.printStackTrace();
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView) findViewById(android.R.id.empty)).setText(R.string.async_error);
                                }
                            });
                            return;
                        }
                        try {
                            JSONArray objArr = result.getJSONArray("objects");
                            if (objArr.length() == 0) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((TextView) findViewById(android.R.id.empty)).setText(R.string.no_data_msg);
                                    }
                                });
                                return;
                            }
                            for (int i = 0; i < objArr.length(); i++) {
                                final JSONObject t = objArr.getJSONObject(i);
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        addItem(t);
                                    }
                                });
                            }
                        } catch (JSONException jsonException) {
                            jsonException.printStackTrace();
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView) findViewById(android.R.id.empty))
                                            .setText(R.string.async_error);
                                }
                            });
                        }
                    }
                });
    }

    private void addItem(JSONObject obj) {
        // Instantiate a new "row" view.
        final ViewGroup newView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.list_item_showtimes, mContainerView, false);

        // if there is an addition, then hide empty text
        try {
            findViewById(android.R.id.empty).setVisibility(View.GONE);
        } catch (NullPointerException e) {
        }
        try {
            ((TextView) newView.findViewById(R.id.movie_name)).setText(obj.getString("movie"));
            JSONArray timesJsonArray = obj.getJSONArray("showtimes");
            ArrayList<String> timesAList = new ArrayList<String>();
            for (int i = 0; i < timesJsonArray.length(); i++) {
                String x = (String) timesJsonArray.get(i);
                timesAList.add(x);
            }
            Joiner joiner = Joiner.on(", ").skipNulls();
            ((TextView) newView.findViewById(R.id.showtimes_list)).setText(joiner.join(timesAList));

            if (obj.has("audio")) {
                ((TextView) newView.findViewById(R.id.audio)).setText(obj.getString("audio"));
            }

            if (obj.has("cinema")) {
                ((TextView) newView.findViewById(R.id.room)).setText(cinemaStringProcessor(obj.getString("cinema")));
            } else {
                newView.findViewById(R.id.room).setVisibility(View.GONE);
            }
            ImageView posterView = (ImageView) newView.findViewById(R.id.poster);
            // TODO: change to "Contacts" avatar which should be lighter
            String posterSize = ((metricsDensity > 1.5) ? "w185" : "w92");
            String url = "http://zzyzx.co/static/" + posterSize + "-blank.png";
            if (obj.has("poster_path")) {
                url = "http://image.tmdb.org/t/p/" + posterSize + obj.getString("poster_path");
            }
            final int mMovieId = ((obj.has("movie_id")) ? obj.getInt("movie_id") : 0);
            final String movieTitle = obj.getString("movie");
            final String mImdbId = ((obj.has("imdb_id")) ? obj.getString("imdb_id") : "");
            final String mVoteCount = ((obj.has("vote_count")) ? obj.getString("vote_count") : "");
            final String mVoteAvg = ((obj.has("vote_average")) ? obj.getString("vote_average") : "");
            posterView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mMovieId == 0) {
                        Toast.makeText(getApplicationContext(), getString(R.string.no_movie_info), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent movieIntent = new Intent(getBaseContext(), MovieInfoActivity.class);
                    movieIntent.putExtra("title", movieTitle);
                    movieIntent.putExtra("movie_id", "" + mMovieId);
                    movieIntent.putExtra("imdb_id", "" + mImdbId);
                    movieIntent.putExtra("vote_average", "" + mVoteAvg);
                    movieIntent.putExtra("vote_count", "" + mVoteCount);
                    startActivity(movieIntent);
                }
            });
            getFile(posterView, url, getFileStreamPath(randomFile()).getAbsolutePath());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            newView.findViewById(R.id.button).setVisibility(View.GONE);
        } catch (NullPointerException npe) {
        }
        mContainerView.addView(newView, mContainerView.getChildCount());
    }

    private String cinemaStringProcessor(String str) {
        /**
         * this should be a bit tricky since Major has such a weird info and
         * need to process to get only what we need. -- I guess I should do when crawling,
         * but it's ok to double-check here too.
         * - KTB 1 theatre-ktb  --> KTB 1
         * - Theatre 1          --> Theatre 1
         * - KTB1 1 theatre-htb --> KTB1
         * - Theatre2 2         --> Theatre2
         *
         * What we haven't taken care of :: Major MEGA CINEPLEX
         * - Ultra Screen 4     --> ???? return all of them?
         */
        ArrayList<String> p = Lists.newArrayList(Splitter.on(' ')
                .trimResults()
                .omitEmptyStrings()
                .split(str));
        if (p.size() == 1)
            return str;

        // if p size is >= 2, then we only consider first 2 anyway
        String last = p.get(0).substring(p.get(0).length() - 1);
        boolean isEndWithNumber = false;
        try {
            Double x = Double.parseDouble(last);
            isEndWithNumber = true;
        } catch (NumberFormatException e) {
        }
        if (isEndWithNumber)
            return p.get(0);

        return Joiner.on(" ").skipNulls().join(p.get(0), p.get(1));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private void initActionBar(String title) {
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(title.toUpperCase() + " " + iso_format.format(new Date()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);
                    assignImageView(iv, bd);
                } catch (NullPointerException npe) {
                }
            }
        });
    }
}
