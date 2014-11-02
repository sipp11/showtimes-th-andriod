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


import android.app.Application;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastoreManager;

/**
 * This is a singleton approach for the app.
 */
public class ShowtimesApplication extends Application {
    public DbxAccountManager accountManager;
    public DbxDatastoreManager datastoreManager;
    public DbxDatastore datastore;
    public String apiBaseUrl = "http://showtimes.everyday.in.th/api/";
    public String appKey = "dropbox-app-key";
    public String appSecret = "dropbox-app-secret";
    public boolean isDirty = false;

    private static ShowtimesApplication singleton;

    public static ShowtimesApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        isDirty = false;
    }
}
