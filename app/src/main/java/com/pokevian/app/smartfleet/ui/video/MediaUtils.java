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


import android.annotation.TargetApi;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;

import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class MediaUtils {

    static final String TAG = "MediaUtils";
    static final Logger LOGGER = Logger.getLogger(TAG);

    private MediaUtils() {
    }

    public static class MediaMetadata {

        /**
         * the video width.
         */
        public int videoWidth = -1;

        /**
         * the video height.
         */
        public int videoHeight = -1;

        /**
         * the playback duration of the data source.
         */
        public int duration = -1;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("MediaMetadata:\n");
            sb.append("    videoWidth" + videoWidth + "\n");
            sb.append("    videoHeight" + videoHeight + "\n");
            sb.append("    duration" + duration + "\n");
            return sb.toString();
        }

    }

    public static MediaMetadata extractMetadata(String mediaFilePath) {
        MediaMetadata metadata = null;
        if (Build.VERSION_CODES.ICE_CREAM_SANDWICH <= Build.VERSION.SDK_INT) {
        /*if (SdkUtils.isIceCreamSandwichSupported()) {*/
            metadata = extractMetadataWithExtractor(mediaFilePath);
        }
        if (metadata == null) {
            metadata = extractMetadataWithNative(mediaFilePath);
        }
        if (metadata == null) {
            metadata = extractMetadataWithPlayer(mediaFilePath);
        }
        return metadata;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static MediaMetadata extractMetadataWithExtractor(String mediaFilePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(mediaFilePath);

            MediaMetadata metadata = new MediaMetadata();
            try {
                metadata.videoWidth = Integer.valueOf(
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            } catch (NumberFormatException e) {
            }
            try {
                metadata.videoHeight = Integer.valueOf(
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            } catch (NumberFormatException e) {
            }
            try {
                metadata.duration = Integer.valueOf(
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            } catch (NumberFormatException e) {
            }
            return metadata;
        } catch (IllegalArgumentException e) {
            LOGGER.error("media path is invalid");
        } catch (Exception e) {
            LOGGER.error("uncaught exception", e);
        } finally {
            retriever.release();
        }
        return null;
    }

    private static MediaMetadata extractMetadataWithPlayer(String mediaFilePath) {
        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(mediaFilePath);
            player.prepare();

            MediaMetadata metadata = new MediaMetadata();
            metadata.videoWidth = player.getVideoWidth();
            metadata.videoHeight = player.getVideoHeight();
            metadata.duration = player.getDuration();
            return metadata;
        } catch (IllegalStateException e) {
            LOGGER.error("media path is invalid");
        } catch (Exception e) {
            LOGGER.error("uncaught exception", e);
        } finally {
            player.release();
        }
        return null;
    }

    private static MediaMetadata extractMetadataWithNative(String mediaFilePath) {
        MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setDataSource(mediaFilePath);

        if (mediaInfo.getVideoWidth() > 0 && mediaInfo.getVideoHeight() > 0
                && mediaInfo.getDuration() > 0) {
            MediaMetadata metadata = new MediaMetadata();
            metadata.videoWidth = mediaInfo.getVideoWidth();
            metadata.videoHeight = mediaInfo.getVideoHeight();
            metadata.duration = mediaInfo.getDuration();
            return metadata;
        }

        return null;
    }

    private static class MediaInfo {
        int duration = -1;
        int videoWidth = -1;
        int videoHeight = -1;

        public void setDataSource(String path) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(path, "r");
                setDataSource(raf);
            } catch (FileNotFoundException e) {
            } catch (Exception e) {
            } finally {
                close(raf);
            }
        }

        public int getVideoWidth() {
            return videoWidth;
        }

        public int getVideoHeight() {
            return videoHeight;
        }

        public int getDuration() {
            return duration;
        }

        private void setDataSource(RandomAccessFile raf) throws IOException {
            byte[] moov = getMoovBox(raf);
            if (null == moov) {
                moov = getMoovBoxBy4ByteCompare(raf);
            }
            if (moov != null) {
                parse(moov);
            }
        }

        private byte[] getMoovBoxBy4ByteCompare(RandomAccessFile raf) throws IOException {
            byte[] buf = new byte[64 * 1024];
            int size, box;
            long offset = raf.length() - buf.length;
            if (offset < 0) offset = 0;

            raf.seek(offset);
            while (true) {
                int l = raf.read(buf);
                if (l < 8) break;

                int rx = 0;
                while (rx + 8 <= l) {
                    box = byteArrayToInt(buf, rx + 4);

                    if (0X6D6F6F76 == box) {
                        size = byteArrayToInt(buf, rx);
                        if (size < 12) return null;

                        byte[] moov = new byte[size - 8];
                        raf.seek(offset + rx + 8);
                        raf.read(moov);

                        return moov;
                    }
                    rx++;
                }
                offset -= rx;
                if (offset < 0) break;
                raf.seek(offset);
            }

            return null;
        }

        private byte[] getMoovBox(RandomAccessFile raf) throws IOException {
            byte[] buf = new byte[1024 * 2];
            int size, box;
            int offset = 0;

            while (true) {
                int l = raf.read(buf);
                if (l < 8) break;

                int rx = 0;
                while (rx + 8 <= l) {
                    size = byteArrayToInt(buf, rx);
                    box = byteArrayToInt(buf, rx + 4);
                    if (size < 12) return null;

                    if (0x6D646174 == box) {
                        rx += size;
                        break;
                    } else if (0X6D6F6F76 == box) {
                        byte[] moov = new byte[size - 8];
                        raf.seek(offset + rx + 8);
                        raf.read(moov);

                        return moov;
                    }
                    rx += size;
                }
                offset += rx;
                if (offset > raf.length()) return null;
                raf.seek(offset);
            }

            return null;
        }

        private void parse(byte[] moov) {
            int rx = 0;
            int loop = moov.length - 8;
            int size, box;
            while (loop > rx) {
                size = byteArrayToInt(moov, rx);
                box = byteArrayToInt(moov, rx + 4);

                switch (box) {
                    case 0x6D766864:
                        duration = byteArrayToInt(moov, rx + 24) / byteArrayToInt(moov, rx + 20) * 1000;
                        rx += size;
                        break;
                    case 0x7472616B:
                    case 0x6D646961:
                        rx += 8;
                        break;
                    case 0x68646C72:
                        if (0x76696465 == byteArrayToInt(moov, rx + 16))
                            return;
                        rx += size;
                        break;
                    case 0x746B6864:
                        videoWidth = byteArrayToInt(moov, rx + 84) >> 16;
                        videoHeight = byteArrayToInt(moov, rx + 88) >> 16;
                        rx += size;
                        break;
                    default:
                        rx += size;
                        break;
                }

            }
        }

        private int byteArrayToInt(byte[] b, int offset) {
            return ((b[offset] & 0xFF) << 24) | ((b[offset + 1] & 0xFF) << 16)
                    | ((b[offset + 2] & 0xFF) << 8) | (b[offset + 3] & 0xFF);
        }

        private void close(Closeable c) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                }
            }
        }
    }

}
