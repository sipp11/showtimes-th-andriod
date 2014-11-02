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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

/**
 * Movie Information Activity shows movie detail and such by
 * fetching data from http://showtimes.everyday.in.th/api/
 */
public class MovieInfoActivity extends ActionBarActivity {
    private ViewGroup infoContainerView;
    private String title;
    private ImageView posterView;
    private String posterSize;
    ShowtimesApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = ShowtimesApplication.getInstance();
        ActionBar ab = getSupportActionBar();
        ab.hide();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float metricsDensity = metrics.density;
        posterSize = ((metricsDensity > 1.5) ? "w780" : "w300");

        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        String imdb_id = intent.getStringExtra("imdb_id");
        String movie_id = intent.getStringExtra("movie_id");
        String tmdb_vote_avg = intent.getStringExtra("vote_average");
        String tmdb_vote_count = intent.getStringExtra("vote_count");

        setContentView(R.layout.activity_movie_info);
        posterView = (ImageView) findViewById(R.id.backdrop_imageview);
        ((TextView) findViewById(R.id.title)).setText(title);
        infoContainerView = (ViewGroup) findViewById(R.id.info_group);
        addRating("TMDB", tmdb_vote_avg, tmdb_vote_count);

        final Handler mHandler = new Handler();
        String movieUrl = app.apiBaseUrl + "movie/" + movie_id + "/";
        AsyncHttpClient.getDefaultInstance().executeJSONObject(
                new AsyncHttpGet(movieUrl), new AsyncHttpClient.JSONObjectCallback() {
                    @Override
                    public void onCompleted(Exception e, AsyncHttpResponse source, JSONObject result) {
                        try {
                            if (result.has("poster_path")) {
                                // TODO: change to "Contacts" avatar which should be lighter
                                final String mPosterPath = "http://image.tmdb.org/t/p/w780/" + result.getString("poster_path");
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        getFile(posterView, mPosterPath, getFileStreamPath(randomFile()).getAbsolutePath());
                                    }
                                });
                            }
                            ArrayList<String> mVideoList = new ArrayList<String>();
                            JSONArray videoArray = result.getJSONArray("videos");
                            for (int i = 0; i < videoArray.length(); i++) {
                                JSONObject x = (JSONObject) videoArray.get(i);
                                // skip if it's not trailer
                                String vType = (String) x.get("type");
                                if (!vType.equalsIgnoreCase("trailer"))
                                    continue;
                                String site = (String) x.get("site");
                                if (!site.equalsIgnoreCase("youtube"))
                                    continue;
                                String youtubeKey = (String) x.get("key");
                                String trailerUrl = "http://www.youtube.com/watch?v=" + youtubeKey;
                                mVideoList.add(trailerUrl);
                            }
                            final ArrayList<String> fVideoList = mVideoList;
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    for (String xUrl : fVideoList) {
                                        addVideo(xUrl);
                                    }
                                }
                            });

                        } catch (JSONException movieE) {
                            movieE.printStackTrace();
                        }
                    }
                }
        );
    }

    private void addRating(String provider, String voteAvg, String voteCount) {
        // Instantiate a new "row" view.
        final ViewGroup newView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.item_info_rating, infoContainerView, false);
        ((TextView) newView.findViewById(R.id.provider)).setText(provider);
        ((RatingBar) newView.findViewById(R.id.rating_bar)).setRating(Float.parseFloat(voteAvg)/2);
        String detail = String.format("%s (%s votes)", voteAvg, voteCount);
        ((TextView) newView.findViewById(R.id.description)).setText(detail);
        infoContainerView.addView(newView, infoContainerView.getChildCount());
    }

    private void addVideo(String link) {
        final String mLink = link;
        // Instantiate a new "row" view.
        final ViewGroup newView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.item_info_videos, infoContainerView, false);

        @SuppressLint("WrongViewCast")
        Button btn = (Button) newView.findViewById(R.id.button);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse(mLink));
                startActivity(intent);
            }
        });
        infoContainerView.addView(newView, infoContainerView.getChildCount());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //int id = item.getItemId();
        return super.onOptionsItemSelected(item);
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
