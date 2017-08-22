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

package com.pokevian.app.smartfleet.ui.video;

import android.content.Context;
import android.os.SystemClock;

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


public class VideoCopyThread extends Thread {

    static final String TAG = "VideoCopyThread";
    final Logger logger = Logger.getLogger(TAG);

    private final ArrayList<File> mFilesToCopy;
    private final File mTargetDir;
    private Callback mCallback;

    public interface Callback {
        void onPreExecute(int maxValue);

        void onPostExecute();

        void onProgress(int value);
    }

    public VideoCopyThread(ArrayList<File> items, File targetDir, Context context, Callback callback) {
        super("VideoCopyThread");

        mFilesToCopy = new ArrayList<>();
        mFilesToCopy.addAll(items);
        mTargetDir = targetDir;
        mCallback = callback;
    }


    @Override
    public void run() {
        long startTime = SystemClock.elapsedRealtime();

        if (mCallback != null) {
            mCallback.onPreExecute(mFilesToCopy.size());
        }

        if (mFilesToCopy.size() > 0) {
            int ncopy = 0;

            for (File file : mFilesToCopy) {
                int ret = copyFile(file, mTargetDir, true);
                if (ret == -1) {
                    //LOGGER.debug("interrupted while copying");
                    break;
                } else if (ret > 0) {
                    File metadataFile = FileUtils.getMetadataFileFromVideoFile(file);
                    if (metadataFile.exists()) {
                        copyFile(metadataFile, mTargetDir, false);
                    }

                    ncopy++;
                    if (mCallback != null) {
                        mCallback.onProgress(ncopy);
                    }
                }
            }
        } else {
            logger.error("The size of the file to be copied is less than 0.");
        }

        long endTime = SystemClock.elapsedRealtime();
        long elapsedTime = endTime - startTime;
        if (elapsedTime < 1500) {
            try {
                Thread.sleep(1500 - elapsedTime);
            } catch (InterruptedException e) {
            }
        }

        if (mCallback != null) {
            mCallback.onPostExecute();
        }
        logger.debug("copy done!");
    }

    private int copyFile(File src, File dstDir, boolean interruptable) {
        dstDir.mkdirs();
        File newFile = new File(dstDir, src.getName());
        InputStream in = null;
        OutputStream out = null;
        int ret = 0;
        try {
            if (newFile.createNewFile()) {
                in = new BufferedInputStream(new FileInputStream(src));
                out = new BufferedOutputStream(new FileOutputStream(newFile));
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    if (interruptable && interrupted()) {
                        out.close();
                        out = null;
                        newFile.delete();
                        logger.debug(
                                "thread interrupted!");
                        return -1;
                    }
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
            ret = 1;
        } catch (IOException e) {
            ret = 0;
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
            }
            if (out != null) try {
                out.close();
            } catch (IOException e) {
            }
        }

        return ret;
    }

}
