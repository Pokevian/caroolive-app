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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;

import com.pokevian.app.smartfleet.model.BlackboxMetadata;
import com.pokevian.app.smartfleet.model.BlackboxMetadata.SmiTag;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.lib.obd2.defs.Unit;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlackboxMetadataWriter extends Handler {

    static final String TAG = "BlackboxMetadataWriter";
    final Logger logger = Logger.getLogger(TAG);

    private static final int MSG_START = 1;
    private static final int MSG_STOP = 2;
    private static final int MSG_WRITE = 3;
    private static final int MSG_EVENT_START = 4;
    private static final int MSG_EVENT_STOP = 5;
    private static final int MSG_EXIT = 999;

    private enum State {
        IDLE, RUNNING;
    }

    private State mState = State.IDLE;
    /*private final Context mContext;*/
    private boolean mIsPrevDataWrited = false;

    private final ConcurrentLinkedQueue<BlackboxMetadata> mNormalMetadataQueue;
    private OutputStream mNormalFileOutputStream = null;
    private long mNormalFileLastStartTime = 0;
    private long mNormalFileLastStopTime = 0;

    private final ConcurrentLinkedQueue<BlackboxMetadata> mEventMetadataQueue;
    private long mEventFileStartTime = 0;
    private final int mEventQueueSize;
    private File mEventMetaFile = null;

    private Unit mSpeedUnit;

    public BlackboxMetadataWriter(Context context, Looper looper, int eventFileDuration) {
        super(looper);

		/*mContext = context;*/
        mNormalMetadataQueue = new ConcurrentLinkedQueue<BlackboxMetadata>();
        mEventQueueSize = (int) ((eventFileDuration / 1000) * 2);
        mEventMetadataQueue = new ConcurrentLinkedQueue<BlackboxMetadata>();

        mSpeedUnit = SettingsStore.getInstance().getSpeedUnit();
    }

    public void start(long startTime, File metaFile) {
        Message msg = obtainMessage(MSG_START);
        Bundle data = new Bundle();
        data.putSerializable("meta_file", metaFile);
        data.putLong("start_time", startTime);
        msg.setData(data);
        sendMessage(msg);
    }

    public void stop(long stopTime) {
        Message msg = obtainMessage(MSG_STOP);
        Bundle data = new Bundle();
        data.putLong("stop_time", stopTime);
        msg.setData(data);
        sendMessage(msg);
    }

    public void exit() {
        if (getLooper().getThread().isAlive()) {
            Message msg = obtainMessage(MSG_EXIT);
            Bundle data = new Bundle();
            data.putLong("exit_time", System.currentTimeMillis());
            msg.setData(data);
            sendMessage(msg);
        }
    }

    public void event(long startTime, File metaFile) {
        Message msg = obtainMessage(MSG_EVENT_START);
        Bundle data = new Bundle();
        data.putLong("start_time", startTime);
        data.putSerializable("meta_file", metaFile);
        msg.setData(data);
        sendMessage(msg);
    }

    public void writeEventMetadata(long stopTime) {
        Message msg = obtainMessage(MSG_EVENT_STOP);
        Bundle data = new Bundle();
        data.putLong("stop_time", stopTime);
        msg.setData(data);
        sendMessage(msg);
    }

    public void writeNormalMetadata(BlackboxMetadata meta) {
        if (mState == State.RUNNING) {
            Message msg = obtainMessage(MSG_WRITE);
            Bundle data = new Bundle();
            BlackboxMetadata prev = mNormalMetadataQueue.peek();
            if (prev != null) {
                data.putSerializable("prev_metadata", prev);
            }
            data.putSerializable("curr_metadata", meta);
            msg.setData(data);
            sendMessage(msg);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_START:
                handleStartMessage(msg);
                break;
            case MSG_STOP:
                handleStopMessage(msg);
                break;
            case MSG_EVENT_START:
                handleEventStartMessage(msg);
                break;
            case MSG_EVENT_STOP:
                handleEventStopMessage(msg);
                break;
            case MSG_WRITE:
                handleMetadataWriteMessage(msg);
                break;
            case MSG_EXIT:
                handleExitMessage(msg);
                break;
            default:
                break;
        }
    }

    private void handleStartMessage(Message msg) {
        if (mState == State.IDLE) {
            mIsPrevDataWrited = false;
            mNormalFileLastStopTime = 0;
            mNormalFileLastStartTime = msg.getData().getLong("start_time");
            File metaFile = (File) msg.getData().getSerializable("meta_file");

            try {
                mNormalFileOutputStream = new FileOutputStream(metaFile);
                openSmi(mNormalFileOutputStream);

                mState = State.RUNNING;
            } catch (FileNotFoundException e) {
                logger.error("[normal] failed to open output stream");
            } catch (IOException e) {
                logger.error("[normal] failed to open smi");
            }
        }
    }

    private void handleStopMessage(Message msg) {
        if (mState == State.RUNNING) {
            if (msg != null) {
                mNormalFileLastStopTime = msg.getData().getLong("stop_time");
            }

            if (mNormalFileOutputStream != null) {
                try {
                    closeSmi(mNormalFileOutputStream);
                } catch (IOException e) {
                    logger.error("[normal] failed to close smi");
                }
                try {
                    mNormalFileOutputStream.close();
                } catch (IOException e) {
                    logger.error("[normal] failed to close output stream");
                }
                mNormalFileOutputStream = null;
            }

            mState = State.IDLE;
        }
    }

    private void handleEventStartMessage(Message msg) {
        Bundle data = msg.getData();
        mEventFileStartTime = data.getLong("start_time", 0);
        mEventMetaFile = (File) data.getSerializable("meta_file");
        logger.debug("handleEventStartMessage(): startTime=" + mEventFileStartTime
                + ", metaFile=" + mEventMetaFile);
    }

    private void handleEventStopMessage(Message msg) {
        long endTime = msg.getData().getLong("stop_time");
        logger.debug("handleEventStopMessage(): endTime=" + endTime);

        if (mEventMetaFile != null) {
            runEventMetadataWriteThread(endTime);
        }
    }

    private void runEventMetadataWriteThread(long endTime) {
        ConcurrentLinkedQueue<BlackboxMetadata> queue = new ConcurrentLinkedQueue<BlackboxMetadata>();
        queue.addAll(mEventMetadataQueue);

        Thread thread = new EventMetadataWriteThread(queue, mEventFileStartTime, endTime, mEventMetaFile);
        thread.start();
        mEventMetaFile = null;
    }

    private void handleMetadataWriteMessage(Message msg) {
        if (mState == State.RUNNING) {
            Bundle data = msg.getData();
            BlackboxMetadata prevMetadata = (BlackboxMetadata) data.getSerializable("prev_metadata");
            BlackboxMetadata currMetadata = (BlackboxMetadata) data.getSerializable("curr_metadata");
            addNormalMetadataQueue(currMetadata);
            try {
                writeMetadata(prevMetadata, currMetadata);
            } catch (IOException e) {
                logger.error("failed to write metadata");
            }
        }
    }

    private void writeMetadata(BlackboxMetadata prevMetadata, BlackboxMetadata meta) throws IOException {
        if (mState == State.RUNNING && meta != null) {
            long timestamp = meta.timestamp;
            long duration = timestamp - mNormalFileLastStartTime;

            if (!mIsPrevDataWrited && duration > 0) {
                if (prevMetadata != null) {
                    writeMetadata(0, mNormalFileOutputStream, prevMetadata);
                    addEventMetadataQueue(prevMetadata);
                    logger.debug("writeMetadata(): Normal prev metadata write succeeded!");
                } else {
                    duration = 0;
                }
                mIsPrevDataWrited = true;
            }

            boolean isNotAvailableData = false;
            if (mNormalFileLastStopTime > 0) {
                long fileDuration = mNormalFileLastStopTime - mNormalFileLastStartTime;
                if (duration > fileDuration) {
                    isNotAvailableData = true;
                }
            }

            if (duration >= 0 && !isNotAvailableData) {
                writeMetadata(duration, mNormalFileOutputStream, meta);
                addEventMetadataQueue(meta);
            }
        }
    }

    private void handleExitMessage(Message msg) {
        handleStopMessage(null);

        if (mEventMetaFile != null) {
            runEventMetadataWriteThread(msg.getData().getLong("exit_time"));
        }

        getLooper().quit();
    }

    private void addNormalMetadataQueue(BlackboxMetadata data) {
        mNormalMetadataQueue.add(data);
        if (mNormalMetadataQueue.size() > 2) {
            mNormalMetadataQueue.poll();
            //logger.debug("addNormalMetadataQueue(): Remove Normal Queue");
        }
    }

    public void clearNormalMetadataQueue() {
        mNormalMetadataQueue.clear();
    }

    private void addEventMetadataQueue(BlackboxMetadata data) {
        //logger.debug("addEventMetadataQueue(): time=" + data.timestamp);
        if (mEventMetadataQueue.size() >= mEventQueueSize) {
            mEventMetadataQueue.poll();
        }
        mEventMetadataQueue.add(data);
    }

    public void clearEventMetadataQueue() {
        mEventMetadataQueue.clear();
        //logger.debug("clearEventMetadataQueue()");
    }

    private void openSmi(OutputStream output) throws IOException {
        if (output != null) {
            output.write(SmiTag.SMI_OPEN_TAG.getBytes());
            output.write(SmiTag.CSS_LINE_BREAK.getBytes());

            output.write(SmiTag.HEAD_OPEN_TAG.getBytes());
            output.write(SmiTag.CSS_LINE_BREAK.getBytes());

            output.write(SmiTag.POKE_TITLE.getBytes());
            output.write(SmiTag.CSS_LINE_BREAK.getBytes());

            output.write(SmiTag.POKE_STYLE.getBytes());
            output.write(SmiTag.CSS_LINE_BREAK.getBytes());

            output.write(SmiTag.HEAD_CLOSE_TAG.getBytes());
            output.write(SmiTag.CSS_LINE_BREAK.getBytes());

            output.write(SmiTag.CSS_LINE_BREAK.getBytes());

            output.write(SmiTag.BODY_OPEN_TAG.getBytes());
            output.write(SmiTag.CSS_LINE_BREAK.getBytes());
        }
    }

    private void closeSmi(OutputStream output) throws IOException {
        if (output != null) {
            output.write(SmiTag.BODY_CLOSE_TAG.getBytes());
            output.write(SmiTag.CSS_LINE_BREAK.getBytes());

            output.write(SmiTag.SMI_CLOSE_TAG.getBytes());
        }
    }

    private void writeMetadata(long duration, OutputStream output, BlackboxMetadata meta) throws IOException {
        if (output != null) {
            writeDuration(output, duration);
            writeMetadataComments(output, meta);
            writeMetadataCaptions(output, meta);
        }
    }

    private void writeDuration(OutputStream output, long duration) throws IOException {
        //logger.debug("writeDuration(): duration=" + duration));

        if (output != null) {
            String sync = SmiTag.SYNC_OPEN_TAG_START
                    + SmiTag.SYNC_OPEN_TAG_START_ATTR + String.valueOf(duration)
                    + SmiTag.SYNC_OPEN_TAG_END;
            output.write(sync.getBytes());
            output.write(SmiTag.CSS_LINE_BREAK.getBytes());
        }
    }

    private void writeMetadataComments(OutputStream output, BlackboxMetadata meta) throws IOException {
        if (output != null) {
            String comments = makeParagraphComments(meta);
            output.write(comments.getBytes());
        }
    }

    private void writeMetadataCaptions(OutputStream output, BlackboxMetadata meta) throws IOException {
        if (output != null) {
            String caption = makeParagraphCaption(meta);
            output.write(caption.getBytes());
        }
    }

    private String makeParagraphComments(BlackboxMetadata meta) {
        String encoded = Base64.encodeToString(meta.flatten().getBytes(), Base64.DEFAULT);
        encoded = encoded.replace("\n", " ");
        String comments = SmiTag.TAB + SmiTag.PARAGRAPH_OPEN_TAG_START
                + SmiTag.PARAGRAPH_OPEN_TAG_CLASS_ATTR + SmiTag.EN_CAPTION_NAME
                + SmiTag.PARAGRAPH_OPEN_TAG_END + SmiTag.COMMENTS_OPEN_TAG
                + encoded + SmiTag.COMMENTS_CLOSE_TAG + SmiTag.CSS_LINE_BREAK;
        return comments;
    }

    private String makeParagraphCaption(BlackboxMetadata meta) {
        long time = meta.timestamp;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
        String timedate = sdf.format(new Date(time));
        //String timedate = TimeStringUtils.toString(TimeStringUtils.Type.LOCAL_DATE_TIME, time);
        String speedText = getMetadataSpeedText(meta);
        String comments = SmiTag.TAB + SmiTag.PARAGRAPH_OPEN_TAG_START
                + SmiTag.PARAGRAPH_OPEN_TAG_CLASS_ATTR + SmiTag.EN_CAPTION_NAME
                + SmiTag.PARAGRAPH_OPEN_TAG_END + timedate
                + SmiTag.SPACE + SmiTag.SPACE + SmiTag.SPACE + SmiTag.SPACE + SmiTag.SPACE
                + SmiTag.SPACE + SmiTag.SPACE + SmiTag.SPACE + SmiTag.SPACE + SmiTag.SPACE
                + speedText + SmiTag.CSS_LINE_BREAK;
        return comments;
    }

    public String getMetadataSpeedText(BlackboxMetadata meta) {
        String speedText = "";
        if (meta.speedData.isValid) {
            speedText = String.valueOf(meta.speedData.currentSpeed) + mSpeedUnit.toString();
        } else {
            speedText = "---" + mSpeedUnit.toString();
        }
        return speedText;
    }

    private final class EventMetadataWriteThread extends Thread {

        private final ConcurrentLinkedQueue<BlackboxMetadata> mMetaQueue;
        private final long mStartTime;
        private final long mEndTime;
        private OutputStream mOutput = null;

        private EventMetadataWriteThread(ConcurrentLinkedQueue<BlackboxMetadata> queue,
                                         long startTime, long endTime, File output) {
            mMetaQueue = queue;
            mStartTime = startTime;
            mEndTime = endTime;
            try {
                mOutput = new FileOutputStream(output);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            if (mOutput == null) {
                return;
            }

            try {
                openSmi(mOutput);

                Iterator<BlackboxMetadata> iter = mMetaQueue.iterator();
                BlackboxMetadata prevMetadata = null;
                int writeCount = 0;

                while (iter.hasNext()) {
                    BlackboxMetadata meta = iter.next();

                    long timestamp = meta.timestamp;
                    if (timestamp >= mStartTime && timestamp < mEndTime) {
                        long duration = timestamp - mStartTime;
                        if (writeCount == 0) {
                            if (prevMetadata != null) {
                                writeMetadata(0, mOutput, prevMetadata);
                            } else {
                                duration = 0;
                            }
                        }
                        writeMetadata(duration, mOutput, meta);
                        writeCount++;
                    }

                    prevMetadata = meta;
                }

                closeSmi(mOutput);

                mOutput.close();
            } catch (IOException e) {
                logger.error("failed to write event metadata file", e);
            } finally {
                try {
                    mOutput.close();
                } catch (IOException e) {
                }
            }
        }

    }

}
