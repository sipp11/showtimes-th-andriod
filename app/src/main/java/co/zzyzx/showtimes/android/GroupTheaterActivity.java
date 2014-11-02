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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Theaters in group Activity
 * which fetches all data from http://showtimes.everyday.in.th/api/
 * and shows in ViewGroup acting like ListView
 */
public class GroupTheaterActivity extends ActionBarActivity {
    final private String LOG_TAG = "GroupTheater";
    private ViewGroup mContainerView;
    ShowtimesApplication app;
    private String thisGroup;
    private ArrayList<String> favTheaterList;

    private DbxAccountManager mAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = ShowtimesApplication.getInstance();
        setContentView(R.layout.activity_scrollable_list);
        mContainerView = (ViewGroup) findViewById(R.id.container);

        initDropboxDataStore();

        ((TextView) findViewById(android.R.id.empty)).setText(R.string.loading);
        thisGroup = "";
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            thisGroup = intent.getStringExtra("group");
        } else {
            thisGroup = savedInstanceState.getString("group");
            favTheaterList = savedInstanceState.getStringArrayList("favTheaterList");
        }
        initActionBar(thisGroup);
        initFavAndTheater();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("group", thisGroup);
        outState.putStringArrayList("favTheaterList", favTheaterList);
    }

    private void initDropboxDataStore() {
        // Set up the account manager
        mAccountManager = DbxAccountManager.getInstance(getApplicationContext(),
                app.appKey, app.appSecret);
        if (mAccountManager.hasLinkedAccount()) {
            try {
                app.datastoreManager = DbxDatastoreManager.forAccount(mAccountManager.getLinkedAccount());
                //Log.i(LOG_TAG, "Account was linked");
            } catch (DbxException.Unauthorized e) {
                Log.i(LOG_TAG, "Account was unlinked remotely");
            }
        } else {
            //Log.i(LOG_TAG, "Account was local");
            app.datastoreManager = DbxDatastoreManager.localManager(mAccountManager);
        }
    }

    private void initFavAndTheater() {
        if (favTheaterList == null || favTheaterList.size() == 0) {
            new UpdateFavData().execute();
            return;
        }
        populateTheaters(thisGroup);
    }

    private void updateFavList(ArrayList<String> favTheaterList) {
        this.favTheaterList = favTheaterList;
        populateTheaters(thisGroup);
    }

    private void populateTheaters(String group) {
        String url = app.apiBaseUrl + "theaters/" + group + "/";
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
                            for (int i = 0; i < objArr.length(); i++) {
                                final JSONObject t = objArr.getJSONObject(i);
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            addItem(t.getString("name"), t.getString("code"));
                                        } catch (JSONException jsonException) {
                                            jsonException.printStackTrace();
                                        }
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

    private void addItem(String title, String tCode) {
        // Instantiate a new "row" view.
        final ViewGroup newView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.list_item_theater, mContainerView, false);

        // if there is an addition, then hide empty text
        try {
            findViewById(android.R.id.empty).setVisibility(View.GONE);
        } catch (NullPointerException e) {
        }

        final String mtCode = tCode;
        final String mtName = title;
        // Set the text in the new row to a random country.
        ((TextView) newView.findViewById(android.R.id.text1)).setText(title);
        TextView tv1 = (TextView) newView.findViewById(android.R.id.text1);
        tv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTheaterPressed(mtCode, mtName);
                return;
            }
        });
        ImageButton btn = (ImageButton) newView.findViewById(R.id.button);
        if (favTheaterList.indexOf(tCode) < 0) {
            btn.setBackgroundResource(R.drawable.ic_action_favorite_gray);
        } else {
            btn.setBackgroundResource(R.drawable.ic_action_favorite_red);
        }
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean success = saveNewFav(mtCode, mtName);
                if (success) {
                    v.setBackgroundResource(R.drawable.ic_action_favorite_red);
                } else {
                    v.setBackgroundResource(R.drawable.ic_action_favorite_gray);
                }
                return;
            }
        });
        View groupColor = newView.findViewById(R.id.group_color);
        if (thisGroup.equalsIgnoreCase("sf")) {
            groupColor.setBackgroundColor(getResources().getColor(R.color.group_sf));
        } else if (thisGroup.equalsIgnoreCase("major")) {
            groupColor.setBackgroundColor(getResources().getColor(R.color.group_major));
        }
        mContainerView.addView(newView, mContainerView.getChildCount());
    }

    private boolean saveNewFav(String tCode, String tName) {
        try {
            if (!app.datastore.isOpen())
                app.datastore = app.datastoreManager.openDefaultDatastore();
            DbxTable favTheaterTable = app.datastore.getTable("favTheater");
            // check whether it's already there
            DbxFields queryParams = new DbxFields()
                    .set("group", thisGroup)
                    .set("theaterCode", tCode);
            DbxTable.QueryResult results = favTheaterTable.query(queryParams);
            if (!results.hasResults()) {
                DbxRecord newRecord = favTheaterTable.insert()
                        .set("group", thisGroup)
                        .set("theaterCode", tCode)
                        .set("theaterName", tName);
                favTheaterList.add(tCode);
                app.isDirty = true;
                Toast.makeText(getApplicationContext(), R.string.new_favorite_saved, Toast.LENGTH_SHORT).show();
                app.datastore.sync();
                return true;
            } else {
                for (DbxRecord d : results.asList()) {
                    d.deleteRecord();
                    favTheaterList.remove(tCode);
                    app.isDirty = true;
                }
                app.datastore.sync();
                Toast.makeText(getApplicationContext(), R.string.favorite_removed, Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void onTheaterPressed(String tCode, String tName) {
        Intent intent = new Intent(this, ShowtimesActivity.class);
        intent.putExtra("group", thisGroup);
        intent.putExtra("tCode", tCode);
        intent.putExtra("tName", tName);
        startActivity(intent);
    }

    private void initActionBar(String title) {
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(title.toUpperCase());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
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

    private class UpdateFavData extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            ArrayList<String> records = new ArrayList<String>();
            try {
                if (!app.datastore.isOpen()) {
                    app.datastore = app.datastoreManager.openDefaultDatastore();
                }
                for (DbxRecord dbx : app.datastore.getTable("favTheater").query().asList()) {
                    records.add(dbx.getString("theaterCode"));
                }
            } catch (DbxException e) {
                e.printStackTrace();
            } finally {
                return records;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> resultList) {
            //Log.i(LOG_TAG, "UpdateFavDataGROUP #" + resultList.size());
            updateFavList(resultList);
        }
    }
}
