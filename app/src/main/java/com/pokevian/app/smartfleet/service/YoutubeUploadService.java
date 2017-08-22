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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokevian.app.smartfleet.BuildConfig;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.VehicleData.VehicleLocation;
import com.pokevian.app.smartfleet.model.YoutubePrivacy;
import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.app.smartfleet.volley.GsonRequest.GsonDateTypeAdapter;
import com.pokevian.caroo.common.model.code.VideoTpCd;
import com.pokevian.caroo.common.smart.model.SmartRecord;
import com.pokevian.caroo.common.smart.model.SmartMessage;
import com.pokevian.caroo.common.smart.model.SmartMessageList;
import com.pokevian.caroo.common.smart.model.SmartVideo;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxEventType;

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class YoutubeUploadService extends Service {

    static final String TAG = "YoutubeUploadService";
    final Logger logger = Logger.getLogger(TAG);

    public static final String EXTRA_VIDEO_INFO = "video_info";
    public static final String EXTRA_WAKED_UP = "waked_up";

    private Gson mGson;
    private RequestStore mStore;
    private WorkerThread mWorker;

    @Override
    public void onCreate() {
        super.onCreate();

        mGson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new GsonDateTypeAdapter())
                .create();
        mStore = new RequestStore(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.debug("onStartCommand(): intent=" + intent);

        if (intent != null) {
            boolean isWakedUp = intent.getBooleanExtra(EXTRA_WAKED_UP, false);
            if (!isWakedUp) {
                VideoInfo videoInfo = (VideoInfo) intent.getSerializableExtra(EXTRA_VIDEO_INFO);
                if (videoInfo != null) {
                    try {
                        String data = mGson.toJson(videoInfo);
                        mStore.add(data);
                    } catch (Exception e) {
                        logger.error("failed to Gson.toJson: videoInfo=" + videoInfo, e);
                    }
                }
            }
        }

        if (mWorker == null) {
            mWorker = new WorkerThread(this);
            mWorker.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        int count = mStore.getCount();
        logger.info("Pending request count=" + count);
        if (count > 0) {
            setAlarm();
        }
        mStore.close();

        super.onDestroy();
    }

    private void setAlarm() {
        Intent intent = new Intent(this, YoutubeUploadService.class);
        intent.putExtra(EXTRA_WAKED_UP, true);
        PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long triggerAt = SystemClock.elapsedRealtime() + Consts.DATA_SENDER_WAKE_UP_DELAY;

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME, triggerAt, pi);
    }

    private class WorkerThread extends Thread implements MediaHttpUploaderProgressListener {

        static final String TAG = "WorkerThread";

        private final Context mContext;
        private final ConnectivityManager mConnectivityManager;
        private final HttpTransport mTransport = AndroidHttp.newCompatibleTransport();
        private final JsonFactory mJsonFactory = new GsonFactory();
        private final NotificationManager mNm;
        private NotificationCompat.Builder mNb;
        private int mNotificationId;

        private WorkerThread(Context context) {
            super(TAG);

            mContext = context;
            mConnectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
            mNm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        }

        public void run() {
            RequestItem item = mStore.get();

            while (item != null) {
                logger.info(item.toString());

                VideoInfo videoInfo = mGson.fromJson(item.data, VideoInfo.class);

                String vehicleId = videoInfo.getVehicleId();
                File videoFile = videoInfo.getVideoFile();

                if (vehicleId != null && videoFile != null && videoFile.isFile() && videoFile.exists()) {
                    Video uploadedVideo = upload(videoInfo);
                    if (uploadedVideo != null) {
                        mStore.delete(item.id);

                        videoInfo.setVideoId(uploadedVideo.getId());

                        SmartMessageList sml = toServerBean(videoInfo);
                        String data = mGson.toJson(sml);

                        // Call DataUploadIntentService
                        Intent serviceIntent = new Intent(mContext, DataUploadService.class);
                        serviceIntent.putExtra(DataUploadService.EXTRA_DATA, data);
                        startService(serviceIntent);
                    } else {
                        break;
                    }
                } else {
                    // Invalid request (maybe video file is not exist)
                    mStore.delete(item.id);
                }

                // Next request
                item = mStore.get();
            }

            stopSelf();
        }

        private Video upload(VideoInfo videoInfo) {
            if (!checkNetwork()) {
                return null;
            }

            mNotificationId = nextNotificationId();
            mNb = new NotificationCompat.Builder(mContext);

            try {
                final GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        mContext, Arrays.asList(YouTubeScopes.YOUTUBE_UPLOAD));
                Logger.getLogger(TAG).debug("upload#" + videoInfo.getAccountName());
                credential.setSelectedAccountName(videoInfo.getAccountName());

                YouTube youtube = new YouTube.Builder(mTransport, mJsonFactory,
                        new HttpRequestInitializer() {
                            public void initialize(HttpRequest request) throws IOException {
                                credential.initialize(request);
                                request.setIOExceptionHandler(new HttpBackOffIOExceptionHandler(
                                        new ExponentialBackOff()));
                            }
                        })
                        .setApplicationName(Consts.YOUTUBE_PROJECT_NAME)
                        .build();

                YouTube.Videos.Insert videoInsert = prepare(youtube, videoInfo);

                Video returnedVideo = videoInsert.execute();

                onMediaComplete(returnedVideo);

                logger.info("================== Returned Video ==================\n"
                        + "  - Account: " + videoInfo.getAccountName() + "\n"
                        + "  - Id: " + returnedVideo.getId() + "\n"
                        + "  - Title: " + returnedVideo.getSnippet().getTitle() + "\n"
                        + "  - Tags: " + returnedVideo.getSnippet().getTags() + "\n"
                        + "  - Privacy Status: " + returnedVideo.getStatus().getPrivacyStatus());

                return returnedVideo;
            } catch (GooglePlayServicesAvailabilityIOException e) {
                logger.error("upload():  google playe services is not available");

                cancelNotification();
            } catch (UserRecoverableAuthIOException e) {
                logger.warn("upload(): user recoverable auth exception");
                Intent authIntent = e.getIntent();
                authIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(authIntent);

                cancelNotification();
            } catch (IOException e) {
                logger.error("upload(): failed to upload video to youtube: file="
                        + videoInfo.getVideoFile(), e);

                cancelNotification();
            }

            return null;
        }

        private YouTube.Videos.Insert prepare(YouTube youtube, VideoInfo videoInfo) throws IOException {
            Video video = new Video();

			/*
             * Set the video to "public", "unlisted" or "private".
		     */
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus(videoInfo.getPrivacy().getCode());
            video.setStatus(status);

			/*
             * Set the metadata with the VideoSnippet object.
			 */
            VideoSnippet snippet = new VideoSnippet();
            snippet.setTitle(Consts.YOUTUBE_TITLE + " on " + Calendar.getInstance().getTime());
            snippet.setDescription(Consts.YOUTUBE_DESCRIPTION);
            snippet.setTags(Arrays.asList(Consts.YOUTUBE_TAGS));
            video.setSnippet(snippet);

			/*
             * Set the stream content.
			 */
            File videoFile = videoInfo.getVideoFile();
            InputStreamContent mediaContent = new InputStreamContent(
                    "video/*", new BufferedInputStream(new FileInputStream(videoFile)));
            mediaContent.setLength(videoFile.length());

			/*
             * The upload command includes:
		     *   1. Information we want returned after file is successfully uploaded.
		     *   2. Metadata we want associated with the uploaded video.
		     *   3. Video file itself.
		     */
            YouTube.Videos.Insert videoInsert = youtube.videos().insert(
                    "snippet,statistics,status", video, mediaContent);

			/*
             * Sets whether direct media upload is enabled or disabled.
		     *   True = whole media content is uploaded in a single request.
		     *   False (default) = resumable media upload protocol to upload in data chunks.
		     */
            MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();
            uploader.setDirectUploadEnabled(false);
            uploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE); // 256KB. default:10MB
            logger.debug("youtube upload chunk size=" + uploader.getChunkSize());

			/*
			 * Add event listener.
			 */
            uploader.setProgressListener(this);

            return videoInsert;
        }

        @Override
        public void progressChanged(MediaHttpUploader uploader) throws IOException {
            logger.debug("progressChanged(): state=" + uploader.getUploadState()
                    + ", progress=" + uploader.getProgress());

            switch (uploader.getUploadState()) {
                case INITIATION_STARTED:
                    onInitiationStarated();
                    break;
                case MEDIA_IN_PROGRESS:
                case MEDIA_COMPLETE:
                    onMediaInProgress(uploader.getProgress());
                    break;
                default:
                    break;
            }
        }

        private boolean checkNetwork() {
            NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }

        private void onInitiationStarated() {
            mNb.setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setTicker(mContext.getString(R.string.youtube_ticker_initiation_started))
                    .setContentTitle(mContext.getString(R.string.youtube_title_upload))
                    .setContentText(mContext.getString(R.string.youtube_content_initiation_started))
                    .setProgress(100, 0, true)
                    .setOngoing(true);

            Intent i = new Intent("com.pokevian.app.smartfleet.action.DUMMY");
            PendingIntent pi = PendingIntent.getBroadcast(mContext, mNotificationId, i, 0);
            mNb.setContentIntent(pi);

            mNm.notify(mNotificationId, mNb.build());
        }

        private void onMediaInProgress(double progress) {
            mNb.setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setContentTitle(mContext.getString(R.string.youtube_title_upload))
                    .setContentText(mContext.getString(R.string.youtube_content_media_process))
                    .setProgress(100, (int) (progress * 100), false)
                    .setOngoing(true);

            Intent i = new Intent("com.pokevian.app.smartfleet.action.DUMMY");
            PendingIntent pi = PendingIntent.getBroadcast(mContext, mNotificationId, i,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mNb.setContentIntent(pi);

            mNm.notify(mNotificationId, mNb.build());
        }

        private void onMediaComplete(Video returnedVideo) {
            mNb.setSmallIcon(android.R.drawable.stat_sys_upload_done)
                    .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher))
                    .setContentTitle(mContext.getString(R.string.youtube_title_upload))
                    .setContentText(mContext.getString(R.string.youtube_content_media_complete))
                    .setProgress(0, 0, false)
                    .setOngoing(false);

            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v="
                    + returnedVideo.getId()));
            PendingIntent pi = PendingIntent.getActivity(mContext, mNotificationId, i,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mNb.setContentIntent(pi);

            if (BuildConfig.DEBUG) {
                try {
                    NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                    VideoSnippet snippet = returnedVideo.getSnippet();
                    inboxStyle.setBigContentTitle("Youtube Video Snippet")
                            .addLine("ID: " + returnedVideo.getId())
                            .addLine("Title: " + snippet.getTitle())
                            .addLine("Tags: " + snippet.getTags())
                            .addLine("privacy: " + returnedVideo.getStatus().getPrivacyStatus())
                            .addLine("Thumbnail: " + snippet.getThumbnails().getDefault().getUrl());
                    mNb.setStyle(inboxStyle);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }

            mNm.notify(mNotificationId, mNb.build());
        }

        private void cancelNotification() {
            mNm.cancel(mNotificationId);
        }

        private int nextNotificationId() {
            SharedPreferences prefs = mContext.getSharedPreferences("youtube_upload_service", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            int notificationId = prefs.getInt("notification_id", 0) + 1;
            editor.putInt("notification_id", notificationId);
            editor.apply();
            return notificationId;
        }

        private SmartMessageList toServerBean(VideoInfo videoInfo) {
            SmartMessageList sml = new SmartMessageList();
            sml.setMemberNo(videoInfo.getAccountId());
            sml.setCarNo(videoInfo.getVehicleId());

            ArrayList<SmartMessage> smList = new ArrayList<>();
            sml.setMsgList(smList);

            SmartMessage sm = new SmartMessage();
            smList.add(sm);

            SmartVideo sv = new SmartVideo();
            sv.setVideoId(videoInfo.getVideoId());
            sv.setStartDt(videoInfo.getBeginTime());
            sv.setEndDt(videoInfo.getEndTime());
            String eventType = videoInfo.getBlackboxEventType();
            if (BlackboxEventType.USER.name().equals(eventType)) {
                sv.setVideoTpCd(VideoTpCd.user.name());
            } else {
                sv.setVideoTpCd(VideoTpCd.sensor.name());
            }
            sv.setLocationJson(videoInfo.getMetaJson());
            sm.setVideo(sv);

            VehicleLocation location = videoInfo.getLocation();
            if (location != null) {
                SmartRecord sl = location.toServerBean();
                sm.setLocation(sl);
            }

            return sml;
        }

    }

    private final class RequestStore extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "youtube-request";
        private static final int DATABASE_VERSION = 1;
        private static final String TABLE_NAME = "request";

        public RequestStore(Context context) {
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

        private long add(String videoInfo) {
            SQLiteDatabase db = getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("data", videoInfo);

            return db.insert(TABLE_NAME, null, values);
        }

        private void delete(long id) {
            SQLiteDatabase db = getWritableDatabase();

            db.delete(TABLE_NAME, "id=?", new String[]{String.valueOf(id)});
        }

        private RequestItem get() {
            SQLiteDatabase db = getReadableDatabase();

            String sql = "SELECT id, data FROM " + TABLE_NAME + " LIMIT 1";
            Cursor c = db.rawQuery(sql, null);
            if (c.moveToFirst()) {
                long id = c.getLong(0);
                String videoInfo = c.getString(1);
                return new RequestItem(id, videoInfo);
            }
            c.close();

            return null;
        }

        private int getCount() {
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

    private final class RequestItem {
        private final long id;
        private final String data;

        private RequestItem(long id, String data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public String toString() {
            return "Request [id=" + id + ", data=" + data + "]";
        }
    }

    public static class VideoInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String accountName;
        private final String accountId;
        private final String vehicleId;
        private File videoFile;
        private Date occurTime;
        private Date beginTime;
        private Date endTime;
        private String blackboxEventType;
        private String metaJson;
        private YoutubePrivacy privacy;
        private VehicleLocation location;
        private String videoId;

        public VideoInfo(String accountName, String accountId, String vehicleId) {
            this.accountName = accountName;
            this.accountId = accountId;
            this.vehicleId = vehicleId;
        }

        public String getAccountName() {
            return accountName;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getVehicleId() {
            return vehicleId;
        }

        public File getVideoFile() {
            return videoFile;
        }

        public void setVideoFile(File videoFile) {
            this.videoFile = videoFile;
        }

        public Date getOccurTime() {
            return occurTime;
        }

        public void setOccurTime(Date occurTime) {
            this.occurTime = occurTime;
        }

        public Date getBeginTime() {
            return beginTime;
        }

        public void setBeginTime(Date beginTime) {
            this.beginTime = beginTime;
        }

        public Date getEndTime() {
            return endTime;
        }

        public void setEndTime(Date endTime) {
            this.endTime = endTime;
        }

        public String getBlackboxEventType() {
            return blackboxEventType;
        }

        public void setBlackboxEventType(String blackboxEventType) {
            this.blackboxEventType = blackboxEventType;
        }

        public String getMetaJson() {
            return metaJson;
        }

        public void setMetaJson(String metaJson) {
            this.metaJson = metaJson;
        }

        public YoutubePrivacy getPrivacy() {
            return privacy;
        }

        public void setPrivacy(YoutubePrivacy privacy) {
            this.privacy = privacy;
        }

        public VehicleLocation getLocation() {
            return location;
        }

        public void setLocation(VehicleLocation location) {
            this.location = location;
        }

        public String getVideoId() {
            return videoId;
        }

        public void setVideoId(String videoId) {
            this.videoId = videoId;
        }

        @Override
        public String toString() {
            return "VideoInfo [accountName=" + accountName + ", accountId="
                    + accountId + ", vehicleId=" + vehicleId + ", videoFile="
                    + videoFile + ", videoId=" + videoId + ",blackboxEventType="
                    + blackboxEventType + "]";
        }
    }

}
