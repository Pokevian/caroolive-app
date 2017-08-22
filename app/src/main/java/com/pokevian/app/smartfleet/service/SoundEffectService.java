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

import android.app.Service;
import android.content.Intent;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.TextUtils;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.app.smartfleet.setting.SettingsStore;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Locale;

public class SoundEffectService extends Service {

    static final String TAG = "SoundEffectService";
    final Logger logger = Logger.getLogger(TAG);

    public static final String EXTRA_CMD = "extra.CMD";
    public static final String EXTRA_SOUND_ID = "extra.SOUND_ID";
    public static final String EXTRA_LOOP = "extra.LOOP";
    public static final String EXTRA_TTS_TEXT = "extra.TTS_TEXT";

    public static final int SOUND_PLAY = 0; // M:EXTRA_SOUND_ID, O:EXTRA_LOOP
    public static final int SOUND_STOP = 1; // M:EXTRA_SOUND_ID
    public static final int TTS_SPEAK = 2; // M:EXTRA_TTS_TEXT, O:EXTRA_TTS_WAIT
    private static final int INVALID_CMD = -1;

    public static final int SID_OVERSPEED = 0;
    public static final int SID_IMPACT = 1;
    public static final int SID_ENGINE_START = 2;
    public static final int SID_ENGINE_STOP = 3;
    public static final int SID_HARSH = 4;
    private static final int SID_MAX = 5;
    private static final int INVALID_SOUND_ID = -1;

    private Sound[] mSounds;
    private SoundPool mSoundPool;

    private TextToSpeech mTTS;
    private HashMap<String, String> mTTSParams;

//    private SettingsStore mSettings;

    @Override
    public void onCreate() {
        super.onCreate();

        initSound();
//        mSettings = SettingsStore.getInstance();
    }

    private void initSound() {
        if (mSoundPool == null) {
            mSoundPool = new SoundPool(SID_MAX, Consts.AUDIO_TARGET_STREAM, 0);
            mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
//                    Logger.getLogger(TAG).debug("onLoadComplete@initSound@service#" + sampleId);
                    logger.trace("sound loaded: sampleId=" + sampleId);
                }
            });

            mSounds = new Sound[SID_MAX];
            mSounds[SID_OVERSPEED] = new Sound(mSoundPool.load(this, R.raw.overspeed, 1));
            mSounds[SID_IMPACT] = new Sound(mSoundPool.load(this, R.raw.impact, 1));
            mSounds[SID_ENGINE_START] = new Sound(mSoundPool.load(this, R.raw.engine_start, 1));
            mSounds[SID_ENGINE_STOP] = new Sound(mSoundPool.load(this, R.raw.engine_stop, 1));
            mSounds[SID_HARSH] = new Sound(mSoundPool.load(this, R.raw.harsh, 1));
        }
    }

    private void releaseSound() {
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
    }

    private void initTts() {
        if (mTTS == null) {
            mTTS = new TextToSpeech(this, new OnInitListener() {
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        Locale locale = Locale.getDefault();
                        if (isTTSSupported(locale)) {
                            int result = mTTS.setLanguage(locale);
                            if (result == TextToSpeech.LANG_MISSING_DATA
                                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                logger.info("TTS Language is not supported: " + locale);
                                mTTS.shutdown();
                                mTTS = null;
                            } else {
                                logger.info("TTS Initialized : " + locale);
                            }
                        } else {
                            mTTS.shutdown();
                            mTTS = null;
                        }
                    } else {
                        logger.warn("TTS Failed to initialize!");
                        mTTS.shutdown();
                        mTTS = null;
                    }
                }
            });
            mTTSParams = new HashMap<>(1);
            mTTSParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                    String.valueOf(Consts.AUDIO_TARGET_STREAM));
        }
    }

    private void releaseTts() {
        if (mTTS != null) {
            mTTS.shutdown();
            mTTS = null;
        }
    }

    @Override
    public void onDestroy() {
        Logger.getLogger(TAG).debug("onDestroy@service");
        releaseSound();
        releaseTts();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.debug("onStartCommand@service#" + SettingsStore.getInstance().getSoundEffectEnabled());

        initSound();
//        mSettings = SettingsStore.getInstance();


        if (SettingsStore.getInstance().getSoundEffectEnabled() && intent != null) {
            int cmd = intent.getIntExtra(EXTRA_CMD, INVALID_CMD);
            logger.trace("cmd=" + cmd);

            if (cmd == SOUND_PLAY) {
                int soundId = intent.getIntExtra(EXTRA_SOUND_ID, INVALID_SOUND_ID);
                logger.trace("SOUND_PLAY: soundId=" + soundId);
                if (soundId != INVALID_SOUND_ID) {
                    int loop = intent.getIntExtra(EXTRA_LOOP, 0);
                    playSound(soundId, loop);
                }
            } else if (cmd == SOUND_STOP) {
                int soundId = intent.getIntExtra(EXTRA_SOUND_ID, INVALID_SOUND_ID);
                logger.trace("SOUND_STOP: soundId=" + soundId);
                if (soundId != INVALID_SOUND_ID) {
                    stopSound(soundId);
                }
            } else if (cmd == TTS_SPEAK) {
                String text = intent.getStringExtra(EXTRA_TTS_TEXT);
                logger.trace("TTS_SPEAK: text=" + text);
                if (!TextUtils.isEmpty(text)) {
                    speak(text);
                }
            }
        }

//        return super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void playSound(int soundId, int loop) {
        if (mSoundPool != null) {
            float leftVolume = 1;
            float rightVolume = 1;
            int priority = 1;
            float rate = 1;

            mSounds[soundId].streamId = mSoundPool.play(mSounds[soundId].sampleId,
                    leftVolume, rightVolume, priority, loop, rate);
        }
    }

    private void stopSound(int soundId) {
        if (mSoundPool != null) {
            mSoundPool.stop(mSounds[soundId].streamId);
        }
    }

    private boolean isTTSSupported(Locale locale) {
        String lang = locale.getISO3Language();
        // KOR only for now
        return "kor".equals(lang);
    }

    private void speak(String text) {
        if (mTTS != null) {
            if (!TextUtils.isEmpty(text)) {
                mTTS.speak(text, TextToSpeech.QUEUE_ADD, mTTSParams);
            }
        }
    }

    class Sound {
        int sampleId;
        int streamId;

        Sound(int sampleId) {
            this.sampleId = sampleId;
        }
    }

}
