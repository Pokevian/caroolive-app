/*
 * Copyright (c) 2014. Pokevian Ltd.
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

package com.pokevian.app.smartfleet.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.util.GZipUtils;
import com.pokevian.app.smartfleet.util.NetworkUtils;
import com.pokevian.app.smartfleet.volley.GsonRequest;
import com.pokevian.app.smartfleet.volley.VolleyParams;
import com.pokevian.app.smartfleet.volley.VolleySingleton;
import com.pokevian.caroo.common.smart.model.SmartResponse;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;

public class DataUploadService extends Service {

    static final String TAG = "DataUploadService";

    public static final String EXTRA_DATA = "data";

    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();
    private RequestStore mStore;

    private PowerManager.WakeLock mWakeLock;
//    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.getLogger(TAG).trace("create data upload service");
        mStore = new RequestStore(this);
        mRequestQueue.cancelAll(TAG);

//        mHandler = new Handler();

        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.getLogger(TAG).debug("onStartCommand(): intent=" + intent);

        // filter out redelivery intent
        if (intent != null) {
            String data = intent.getStringExtra(EXTRA_DATA);
            Logger.getLogger(TAG).trace("onStartCommand(): data=" + data);
            if (data != null) {
                mStore.add(data);
            }
        }

        upload();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        int count = mStore.count();
        Logger.getLogger(TAG).debug("Pending request count=" + count);
        if (count > 0) {
            setAlarm(DataUploadService.this, Consts.DATA_SENDER_WAKE_UP_DELAY);
        }
        mStore.close();
        releaseWakeLock();

        super.onDestroy();
    }

    private void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private void upload() {
        if (NetworkUtils.isConnected(this)) {
            RequestItem item = mStore.get();
            if (item != null) {
                request(item.id, item.data);
                return;
            }
        }

        stopSelf();
    }

    private void request(long id, String data) {
        Logger.getLogger(TAG).debug("request#" + data);

        try {
            Request request = new Request(id, data);
            request.setTag(TAG);
            mRequestQueue.add(request);
        } catch (IOException e) {
            Logger.getLogger(TAG).error("request data: " + e.getMessage());
        }
    }

    public static void setAlarm(Context context, int delay) {
        Logger.getLogger(TAG).debug("# set data upload alarm: delay=" + delay + "ms");

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = getAlarmIntent(context);
        long triggerAt = SystemClock.elapsedRealtime() + delay;
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, operation);
    }

    private static PendingIntent getAlarmIntent(Context context) {
        Intent receiver = new Intent(context, DataUploadService.class);
        return PendingIntent.getService(context, R.id.req_wake_up_data_upload_service,
                receiver, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private final class Request extends GsonRequest<SmartResponse> {
        private final long id;

        private Request(long id, String data) throws IOException {
            super(ServerUrl.DATA_UPLOAD_API, SmartResponse.class, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Logger.getLogger(TAG).error("Request#" + error.getMessage());
                    setAlarm(DataUploadService.this, Consts.DATA_SENDER_WAKE_UP_DELAY);
                }
            });
            this.id = id;

            String zipData = GZipUtils.compress(data);

            VolleyParams params = new VolleyParams();
            params.put("cmd", "carMessage");
            params.put("data", zipData);
            params.put("zipFlag", "Y");
            setParams(params);

            setListener(createResponseListener());

            TripManager.totalSendSize += zipData.length() * 2;

            Logger.getLogger(TAG).trace("Request#" + data.length() + "/" + zipData.length() + "@" + TripManager.totalSendSize + "b");

        }

        private Listener<SmartResponse> createResponseListener() {
            return new Listener<SmartResponse>() {
                public void onResponse(SmartResponse response) {
                    if (SmartResponse.RESULT_OK.equals(response.getResult())) {
                        mStore.delete(id);
                        upload();
                    } else {
                        Logger.getLogger(TAG).warn("onResponse#" + response.getResult() + "@ "+ response.getErrMessage());
                        mStore.delete(id);
                        upload();
                    }
                }
            };

        }
    }

    private final class RequestStore extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "data-request";
        private static final int DATABASE_VERSION = 1;
        private static final String TABLE_NAME = "request";

        private RequestStore(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "CREATE TABLE " + TABLE_NAME + " ("
                    + "id INTEGER PRIMARY KEY"
                    + ", data TEXT"
                    + ")";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
            db.execSQL(sql);

            onCreate(db);
        }

        @Override
        public synchronized void close() {
            vacuum();

            super.close();
        }

        private void vacuum() {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("vacuum");
            db.close();
        }

        private long add(String data) {
            SQLiteDatabase db = getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("data", data);

            return db.insert(TABLE_NAME, null, values);
        }

        private void delete(long id) {
            SQLiteDatabase db = getWritableDatabase();

            db.delete(TABLE_NAME, "id=?", new String[]{String.valueOf(id)});
        }

        @SuppressWarnings("unused")
        private RequestItem get() {
            SQLiteDatabase db = getReadableDatabase();

            String sql = "SELECT id, data FROM " + TABLE_NAME + " LIMIT 1";
            Cursor c = db.rawQuery(sql, null);
            if (c.moveToFirst()) {
                long id = c.getLong(0);
                String data = c.getString(1);
                return new RequestItem(id, data);
            }
            c.close();

            return null;
        }

        private ArrayList<RequestItem> getAll() {
            SQLiteDatabase db = getReadableDatabase();

            ArrayList<RequestItem> list = new ArrayList<RequestItem>();

            String sql = "SELECT id, data FROM " + TABLE_NAME + " LIMIT 1";
            Cursor c = db.rawQuery(sql, null);
            if (c.moveToFirst()) {
                long id = c.getLong(0);
                String data = c.getString(1);
                list.add(new RequestItem(id, data));
            }
            c.close();

            return list;
        }

        private int count() {
            SQLiteDatabase db = getReadableDatabase();

            String sql = "SELECT count(id) FROM " + TABLE_NAME;
            Cursor c = db.rawQuery(sql, null);
            int count = 0;
            if (c.moveToFirst()) {
                count = c.getInt(0);
            }
            c.close();

            return count;
        }

    }

    private class RequestItem {
        private final long id;
        private final String data;

        private RequestItem(long id, String data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public String toString() {
            return "RequestItem [id=" + id + ", data=" + data + "]";
        }
    }

}
