package com.pokevian.app.fingerpush;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import kr.co.fingerpush.android.GCMFingerPushManager;
import kr.co.fingerpush.android.NetworkUtility;
import kr.co.fingerpush.android.dataset.TagList;

import org.apache.log4j.Logger;

/**
 * Created by ian on 2016-04-14.
 */
 class PushManager {
    private static PushManager singleton = null;
    private GCMFingerPushManager mManager;

    static PushManager getInstance(Context context) {
        if(singleton == null) {
            singleton = new PushManager(context);
        }

        return singleton;
    }

    private PushManager(final Context context) {
        mManager = GCMFingerPushManager.getInstance(context);
    }

    protected void checkPush(String messageIdx, String pushType, final PushManagerCallback callback) {
        mManager.checkPush(messageIdx, pushType, new NetworkUtility.NetworkDataListener() {

            @Override
            public void onError(String code, String message) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onComplete(String code, String resultMessage, ArrayList<?> dataList, Integer totalArticleCount, Integer currentPageNo) {
                callback.onComplete();
            }

            @Override
            public void onCancel() {
                // TODO Auto-generated method stub
            }
        });
    }

    protected void getDeviceInfo(final PushManagerCallback callback) {
        mManager.getDeviceInfo(new NetworkUtility.NetworkObjectListener() {
            @Override
            public void onComplete(String code, String resultMessage, JSONObject ObjectData, Integer TotalArticleCount, Integer CurrentPageNo) {
                if (ObjectData != null) {
                    String appkey = ObjectData.optString(kr.co.fingerpush.android.dataset.DeviceInfo.APPKEY);
                    String device_type = ObjectData.optString(kr.co.fingerpush.android.dataset.DeviceInfo.DEVICE_TYPE);
                    String activity = ObjectData.optString(kr.co.fingerpush.android.dataset.DeviceInfo.ACTIVITY);
                    String identity = ObjectData.optString(kr.co.fingerpush.android.dataset.DeviceInfo.IDENTITY);

                    Logger.getLogger("pushmanager").trace(String.format("onComplete@getDeviceInfo#%s-%s-%s", device_type, identity, activity));
                    if (callback != null) {
                        callback.onComplete();
                    }
                }
            }

            @Override
            public void onError(String code, String errorMessage) {
//                logger.warn("onError@getDeviceInfo#" + errorMessage);
                if (callback != null) {
                    callback.onError(code, errorMessage);
                }
            }

            @Override
            public void onCancel() {

            }
        });
    }

    protected void setDevice(final PushManagerCallback callback) {
        mManager.setDevice(new NetworkUtility.NetworkDataListener() {
            @Override
            public void onComplete(String code, String resultMessage, ArrayList<?> dataList, Integer totalArticleCount, Integer currentPageNo) {
                callback.onComplete();
            }

            @Override
            public void onError(String code, String errorMessage) {
                callback.onError(code, errorMessage);
            }

            @Override
            public void onCancel() {

            }
        });
    }

    protected void setAlive(boolean isAlive) {
        mManager.setPushAlive(isAlive, new NetworkUtility.NetworkDataListener() {

            @Override
            public void onError(String code, String errorMessage) {
                // TODO Auto-generated method stub
                Log.e("", "code ::: " + code + "\nmsg ::: " + errorMessage);
            }

            @Override
            public void onComplete(String code, String resultMessage,
                                   ArrayList<?> DataList, Integer TotalArticleCount,
                                   Integer CurrentPageNo) {
                // TODO Auto-generated method stub
                Log.e("", "code ::: " + code);
            }

            @Override
            public void onCancel() {
                // TODO Auto-generated method stub

            }
        });
    }

    protected void setIdentity(String identity, final PushManagerCallback callback) {
        mManager.setIdentity(identity, new NetworkUtility.NetworkObjectListener() {

            @Override
            public void onError(String code, String errorMessage) {
                callback.onError(code, errorMessage);
            }

            @Override
            public void onComplete(String code, String resultMessage, JSONObject ObjectData, Integer TotalArticleCount, Integer CurrentPageNo) {
                callback.onComplete();
            }

            @Override
            public void onCancel() {
            }
        });
    }

    protected void removeIdentity() {
        mManager.removeIdentity(new NetworkUtility.NetworkObjectListener() {

            @Override
            public void onError(String code, String errorMessage) {
                // TODO Auto-generated method stub
                final String emsg = errorMessage;
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        Toast.makeText(getApplicationContext(), emsg, Toast.LENGTH_SHORT).show();
//                    }
//                });
            }

            @Override
            public void onComplete(String code, String resultMessage,
                                   JSONObject ObjectData, Integer TotalArticleCount,
                                   Integer CurrentPageNo) {
                // TODO Auto-generated method stub
//                et0.setText("");
//                Toast.makeText(getApplicationContext(), resultMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                // TODO Auto-generated method stub
            }
        });
    }

    protected void setTag(final PushManagerCallback callback, String ... tags) {
//        removeAllTag();

        if (tags != null) {
            for (String tag : tags) {
                setTag(tag, new PushManagerCallback() {
                    @Override
                    public void onError(String code, String errorMessage) {
                        callback.onError(code, errorMessage);
                    }

                    @Override
                    public void onComplete() {
                        callback.onComplete();
                    }
                });
            }
        }

    }

    protected void getAllTag() {
        mManager.getAllTag(new NetworkUtility.NetworkArrayListener() {

            @Override
            public void onError(String code, String errorMessage) {
                // TODO Auto-generated method stub
//                logger.warn("onError@getAllTag#" + code + "@" + errorMessage);
            }

            @Override
            public void onComplete(String code, String resultMessage, JSONArray ArrayData, Integer TotalArticleCount, Integer CurrentPageNo) {

                try {
                    ArrayList<TagList> dataList = new ArrayList<TagList>();
                    TagList list = null;
                    if(ArrayData != null) {
                        for (int i = 0; i < ArrayData.length(); i++) {
                            list = new TagList();
                            list.date = ArrayData.getJSONObject(i).optString("date");
                            list.tag = ArrayData.getJSONObject(i).optString("tag");

                            Logger.getLogger("pushmanager").trace("onComplete@getAllTag#" + list.tag);

                            dataList.add(list);
                        }
                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancel() {

            }
        });
    }

    void setTag(String tag, final PushManagerCallback callback) {
        mManager.setTag(tag.trim(), new NetworkUtility.NetworkArrayListener() {

            @Override
            public void onError(String code, String errorMessage) {
                callback.onError(code,errorMessage);
            }

            @Override
            public void onComplete(String code, String resultMessage, JSONArray ArrayData, Integer TotalArticleCount, Integer CurrentPageNo) {
                callback.onComplete();
            }

            @Override
            public void onCancel() {
            }
        });
    }

    void removeTag(String tag, final PushManagerCallback callback) {
        if (TextUtils.isEmpty(tag)) {
            return;
        }
        mManager.removeTag(tag.trim(), new NetworkUtility.NetworkArrayListener() {

            @Override
            public void onError(String code, String errorMessage) {
                callback.onError(code, errorMessage);
            }

            @Override
            public void onComplete(String code, String resultMessage, JSONArray ArrayData, Integer TotalArticleCount, Integer CurrentPageNo) {
//                logger.trace("onComplete@removeTag#" + code + "@" + resultMessage);
                callback.onComplete();
            }

            @Override
            public void onCancel() {
            }
        });
    }

    public void getDeviceTag() {
        mManager.getDeviceTag(new NetworkUtility.NetworkArrayListener() {

            @Override
            public void onError(String code, String errorMessage) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onComplete(String code, String resultMessage, JSONArray ArrayData, Integer TotalArticleCount, Integer CurrentPageNo) {

                try {
                    ArrayList<TagList> tagList = new ArrayList<TagList>();
                    TagList list = null;
                    if(ArrayData != null) {
                        for (int i = 0; i < ArrayData.length(); i++) {
                            list = new TagList();
                            list.date = ArrayData.getJSONObject(i).optString("date");
                            list.tag = ArrayData.getJSONObject(i).optString("tag");

                            tagList.add(list);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancel() {
                // TODO Auto-generated method stub

            }
        });
    }

    public interface PushManagerCallback {
        public void onError(String code, String errorMessage);
        public void onComplete();
    }
}


