/*
 * Copyright (c) 2015. Pokevian Ltd.
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

package com.pokevian.app.smartfleet.service.floatinghead;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by dg.kim on 2015-04-13.
 */
public class TtsHandler {

    private static final String TAG = "TtsHandler";

    private TextToSpeech mTts;
    private final Callbacks mCallbacks;

    public TtsHandler(Context context, Callbacks callbacks) {
        mTts = new TextToSpeech(context, new TtsInitListener());
        mCallbacks = callbacks;
    }

    public void shutdown() {
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
            mTts = null;
        }
    }

    public boolean speak(CharSequence text) {
        if (mTts != null) {
            String utteranceId = String.valueOf(Math.random());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            } else {
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
                mTts.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, params);
            }
            mCallbacks.onTtsStart(utteranceId);
            return true;
        } else {
            return false;
        }
    }

    public void stopSpeak() {
        if (mTts != null && mTts.isSpeaking()) {
            mTts.stop();
        }
    }

    private void setUtteranceListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    Logger.getLogger(TAG).info("tts start");
                }

                @Override
                public void onDone(String utteranceId) {
                    Logger.getLogger(TAG).info("tts done");
                    mCallbacks.onTtsDone(utteranceId);
                }

                @Override
                public void onError(String utteranceId) {
                    Logger.getLogger(TAG).info("tts error");
                    mCallbacks.onTtsDone(utteranceId);
                }
            });
        } else {
            mTts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    Logger.getLogger(TAG).info("tts done");
                    mCallbacks.onTtsDone(utteranceId);
                }
            });
        }
    }

    private class TtsInitListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                int result = mTts.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Logger.getLogger(TAG).warn(Locale.getDefault() + " is not supported!");
                    mTts.shutdown();
                    mTts = null;
                    mCallbacks.onTtsInit(false);
                } else {
                    Logger.getLogger(TAG).warn(Locale.getDefault() + "TTS initialized");
                    setUtteranceListener();
                    mCallbacks.onTtsInit(true);
                }
            } else {
                Logger.getLogger(TAG).warn(Locale.getDefault() + "failed to initialize TTS");
            }
        }
    }

    public interface Callbacks {
        void onTtsInit(boolean result);

        void onTtsStart(String utteranceId);

        void onTtsDone(String utteranceId);
    }
}
