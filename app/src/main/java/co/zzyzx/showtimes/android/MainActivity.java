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
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFields;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxRuntimeException;
import com.dropbox.sync.android.DbxTable;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Main Activity which contains a pager and most logic of an app.
 */
public class MainActivity extends ActionBarActivity implements
        GroupFragment.OnGroupListener,
        FavFragment.OnFavListener,
        GearFragment.onGearListener,
        ActionBar.TabListener,
        DbxDatastore.SyncStatusListener {
    private static final String LOG_TAG = "MAIN";
    private ShowtimesApplication app;

    TabPagerAdapter mTabPagerAdapter;
    ViewPager mViewPager;

    static final int REQUEST_LINK_TO_DBX = 80;
    private DbxAccountManager mAccountManager;
    private ArrayList<String> savedFavList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        app = ShowtimesApplication.getInstance();

        mTabPagerAdapter = new TabPagerAdapter(getFragmentManager());
        final ActionBar ab = getSupportActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ab.setDisplayShowHomeEnabled(false);  // hides action bar icon
        ab.setDisplayShowTitleEnabled(false); // hides action bar title

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mTabPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                ab.setSelectedNavigationItem(position);
            }
        });
        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mTabPagerAdapter.getCount(); i++) {
            ab.addTab(
                    ab.newTab()
                            .setIcon(mTabPagerAdapter.getIcon(i))
                            .setTabListener(this));
        }
        // Set up the account manager
        mAccountManager = DbxAccountManager.getInstance(getApplicationContext(),
                app.appKey, app.appSecret);
        // Set up the app.datastore manager
        if (mAccountManager.hasLinkedAccount()) {
            try {
                // Use Dropbox datastores
                this.app.datastoreManager = DbxDatastoreManager.forAccount(mAccountManager.getLinkedAccount());
                //Log.i(LOG_TAG, "Account was linked");
            } catch (DbxException.Unauthorized e) {
                //Log.i(LOG_TAG, "Account was unlinked remotely");
            }
        } else {
            //Log.i(LOG_TAG, "Account was local");
            this.app.datastoreManager = DbxDatastoreManager.localManager(mAccountManager);
        }

        if (!app.isDirty && savedInstanceState != null) {
            savedFavList = savedInstanceState.getStringArrayList("favList");
            app.isDirty = false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (savedFavList != null && savedFavList.size() > 0) {
            outState.putStringArrayList("favList", savedFavList);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (app.datastore == null)
            try {
                app.datastore = app.datastoreManager.openDefaultDatastore();
            } catch (DbxException e) {
                e.printStackTrace();
            }
        app.datastore.addSyncStatusListener(this);
        // only update if anything changed.
        if (app.isDirty) {
            updateFavList();
            app.isDirty = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        app.datastore.removeSyncStatusListener(this);
        app.datastore.close();
    }

    @Override
    public void onDatastoreStatusChange(DbxDatastore dbxDatastore) {
        /*Log.i(LOG_TAG, "onDatastoreStatusChange:");
        Log.i(LOG_TAG, "   " + dbxDatastore.getSyncStatus().toString());
        Log.i(LOG_TAG, "   outgoing: " + dbxDatastore.getSyncStatus().hasOutgoing);
        Log.i(LOG_TAG, "   incoming: " + dbxDatastore.getSyncStatus().hasIncoming);
        Log.i(LOG_TAG, "   connected: " + dbxDatastore.getSyncStatus().isConnected);
        Log.i(LOG_TAG, "   downloading: " + dbxDatastore.getSyncStatus().isDownloading);
        Log.i(LOG_TAG, "   uploading: " + dbxDatastore.getSyncStatus().isUploading);
        Log.i(LOG_TAG, "   #" + dbxDatastore.getRecordCount());*/

        // only update when there is something change.
        // hasOutgoing & hasIncoming is for multiple clients apparently
        if (dbxDatastore.getSyncStatus().isDownloading
                || dbxDatastore.getSyncStatus().isUploading) {
            updateFavList();
        }
        if (dbxDatastore.getSyncStatus().hasIncoming) {
            boolean hasDeleteRecord = false;
            try {
                Map<String, Set<DbxRecord>> changes = dbxDatastore.sync();
                // Handle the updated data
                for (Map.Entry e : changes.entrySet()) {
                    if (!e.getKey().equals("favTheater"))
                        continue;

                    // we care only favTheater
                    Set<DbxRecord> setOfIncoming = (Set<DbxRecord>) e.getValue();
                    Log.i(LOG_TAG, "   incoming fav #" + setOfIncoming.size());
                    for (DbxRecord d : setOfIncoming) {
                        // if there are deleted records from remote, then do full sync instead
                        try {
                            savedFavList.add(d.getString("group") + "|" +
                                    d.getString("theaterName") + "|" +
                                    d.getString("theaterCode"));
                        } catch (DbxRuntimeException runtimeE) {
                            runtimeE.printStackTrace();
                            Log.d(LOG_TAG, d.fieldNames().toString());
                            hasDeleteRecord = true;
                            break;
                        }
                    }
                }
            } catch (DbxException e) {
                // Handle exception
            }
            if (hasDeleteRecord) {
                Log.i(LOG_TAG, "has deleted record incoming ");
                updateFavList();
            } else {
                Log.i(LOG_TAG, "has new record incoming ");
                updateFavFragmentFromSavedFavList();
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LINK_TO_DBX) {
            if (resultCode == Activity.RESULT_OK) {
                DbxAccount account = mAccountManager.getLinkedAccount();
                try {
                    // Migrate any local datastores.
                    app.datastoreManager.migrateToAccount(account);
                    // Start using the remote app.datastore manager.
                    app.datastoreManager = DbxDatastoreManager.forAccount(account);
                } catch (DbxException e) {
                    e.printStackTrace();
                } catch (DbxRuntimeException runtimeE) {
                    runtimeE.printStackTrace();
                }
                updateFavList();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateFavList() {
        ArrayList<DbxRecord> records = new ArrayList<DbxRecord>();
        try {
            if (!app.datastore.isOpen())
                app.datastore = app.datastoreManager.openDefaultDatastore();
            for (DbxRecord dbx : app.datastore.getTable("favTheater").query().asList()) {
                //Log.i(LOG_TAG, "[UpdateFavData] <background-loop> dbx :" + dbx.getId());
                records.add(dbx);
            }
            updateFavFragment(records);
            return;
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (DbxRuntimeException runtimeE) {
            runtimeE.printStackTrace();
        }
        Log.i(LOG_TAG, "nah, there is no records");
    }

    private void updateFavFragment(ArrayList<DbxRecord> data) {
        if (savedFavList == null) {
            savedFavList = new ArrayList<String>();
        }
        if (data == null) {
            updateFavFragmentFromSavedFavList();
            return;
        } else {
            savedFavList.clear();
        }
        TabPagerAdapter pagerAdapter = (TabPagerAdapter) mViewPager.getAdapter();
        FavFragment favFragment = (FavFragment) pagerAdapter.instantiateItem(mViewPager, 0);
        for (DbxRecord d : data) {
            savedFavList.add(
                    d.getString("group") + "|" +
                            d.getString("theaterName") + "|" +
                            d.getString("theaterCode")
            );
        }
        //Log.i(LOG_TAG, "[updateFavFragment] data: " + data.size() + " | saved: " + savedFavList.size());
        if (favFragment != null) {
            favFragment.updateList(data);
            if (!mAccountManager.hasLinkedAccount()) {
                favFragment.displayDropboxLink(true);
            } else {
                favFragment.displayDropboxLink(false);
            }
        }
    }

    /**
     * This is a fallback function when Dropbox app.datastore gets kinky
     */
    private void updateFavFragmentFromSavedFavList() {
        //Log.i(LOG_TAG, "updateFavFragmentFromSavedFavList #" + savedFavList.size());
        TabPagerAdapter pagerAdapter = (TabPagerAdapter) mViewPager.getAdapter();
        FavFragment favFragment = (FavFragment) pagerAdapter.instantiateItem(mViewPager, 0);
        if (favFragment != null) {
            favFragment.updateListFromSavedFavList(savedFavList);
            if (!mAccountManager.hasLinkedAccount()) {
                favFragment.displayDropboxLink(true);
            } else {
                favFragment.displayDropboxLink(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDropboxLink() {
        if (!mAccountManager.hasLinkedAccount()) {
            // If we're not already linked, start the linking process.
            mAccountManager.startLink(this, REQUEST_LINK_TO_DBX);
        } else {
            // If we're linked, unlink and start using a local app.datastore manager again.
            mAccountManager.unlink();
            app.datastoreManager = DbxDatastoreManager.localManager(mAccountManager);
        }
    }

    @Override
    public void onGearFragmentCreated() {
        TabPagerAdapter pagerAdapter = (TabPagerAdapter) mViewPager.getAdapter();
        GearFragment gearFragment = (GearFragment) pagerAdapter.instantiateItem(mViewPager, 2);
        if (gearFragment != null) {
            gearFragment.initSettingGroup(mAccountManager);
        }
    }

    @Override
    public void onFavFragmentCreated() {

        if (savedFavList == null) {
            updateFavList();
            return;
        }
        if (savedFavList.size() == 0) {
            updateFavList();
            return;
        }
        updateFavFragmentFromSavedFavList();
    }

    @Override
    public void onFavDeletion(String group, String tCode) {
        // clear favList to force updating
        // UI task
        for (int i = 0; i < savedFavList.size(); i++) {
            String str = savedFavList.get(i);
            if (str.contains(group) && str.contains(tCode)) {
                savedFavList.remove(i);
            }
        }
        updateFavFragmentFromSavedFavList();
        try {
            if (!app.datastore.isOpen())
                app.datastore = app.datastoreManager.openDefaultDatastore();
            DbxTable favTheaterTable = app.datastore.getTable("favTheater");
            DbxFields queryParams = new DbxFields()
                    .set("group", group)
                    .set("theaterCode", tCode);
            DbxTable.QueryResult results = favTheaterTable.query(queryParams);
            for (DbxRecord d : results.asList()) {
                d.deleteRecord();
            }
            app.datastore.sync();
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFavTheaterClick(String group, String tCode, String tName) {
        Intent intent = new Intent(this, ShowtimesActivity.class);
        intent.putExtra("group", group);
        intent.putExtra("tCode", tCode);
        intent.putExtra("tName", tName);
        startActivity(intent);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onGroupSelected(String sel) {
        Intent intent = new Intent(this, GroupTheaterActivity.class);
        intent.putExtra("group", sel);
        startActivity(intent);
    }

    private class TabPagerAdapter extends FragmentPagerAdapter {
        public ArrayList<Fragment> pages;

        private TabPagerAdapter(FragmentManager fm) {
            super(fm);
            pages = new ArrayList<Fragment>();
        }

        @Override
        public Fragment getItem(int position) {
            /*if (pages.size() < 1) {
                pages.add(FavFragment.newInstance());
                pages.add(GroupFragment.newInstance());
            }*/
            if (position == 0) {
                return FavFragment.newInstance();
            } else if (position == 1) {
                return GroupFragment.newInstance();
            } else {
                return GearFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        public int getIcon(int position) {
            switch (position) {
                case 0:
                    return R.drawable.ic_action_favorite; //  "Fav";
                case 1:
                    return R.drawable.ic_action_view_as_list;  // "All";
                case 2:
                    return R.drawable.ic_action_settings;  // "Settings";
            }
            return R.drawable.ic_launcher;
        }

    }

    /**
     * Convenient version of {@link FragmentManager#findFragmentById(int)}, which throws
     * an exception if the fragment doesn't exist.
     */
    /*@SuppressWarnings("unchecked")
    public <T extends Fragment> T getFragment(int id) {
        T result = (T) getFragmentManager().findFragmentById(id);
        if (result == null) {
            throw new IllegalArgumentException("fragment 0x" + Integer.toHexString(id)
                    + " doesn't exist");
        }
        return result;
    }

    */

    /**
     * Convenient version of {@link #findViewById(int)}, which throws
     * an exception if the view doesn't exist.
     *//*
    @SuppressWarnings("unchecked")
    public <T extends View> T getView(int id) {
        T result = (T) findViewById(id);
        if (result == null) {
            throw new IllegalArgumentException("view 0x" + Integer.toHexString(id)
                    + " doesn't exist");
        }
        return result;
    }*/
}
