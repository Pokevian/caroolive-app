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

import android.util.Base64;

import com.pokevian.app.smartfleet.model.BlackboxMetadata;
import com.pokevian.app.smartfleet.model.BlackboxMetadata.SmiTag;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;

public final class MetadataReader {

    static final String TAG = "MetadataReader";
    static final Logger LOGGER = Logger.getLogger(TAG);

    private MetadataReader() {
    }

    public static LinkedHashMap<Long, BlackboxMetadata> read(InputStream input) {
        return readMetadataFile(input);
    }

    private static LinkedHashMap<Long, BlackboxMetadata> readMetadataFile(InputStream input) {
        LOGGER.debug("Metadata read");

        LinkedHashMap<Long, BlackboxMetadata> readedMetadataMap = new LinkedHashMap<Long, BlackboxMetadata>(0);
        long lastDuration = 0;
        if (input != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(input));
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    if (line.contains(SmiTag.TITLE_OPEN_TAG)) {
                        if (!line.contains(SmiTag.POKE_TITLE_CONTENT)) {
                            LOGGER.error("SMI File Parsing Error - NOT Poke Meta File");
                            break;
                        }
                    } else if (line.contains(SmiTag.SYNC_OPEN_TAG_START)) {
                        int start = line.indexOf(SmiTag.SYNC_OPEN_TAG_START_ATTR) + SmiTag.SYNC_OPEN_TAG_START_ATTR.length();
                        //#804 add exception handling
                        int end = line.indexOf(">");
                        if (start >= 0 && end > start) {
                            String duration = line.substring(start, end);
                            lastDuration = Long.valueOf(duration);
                            LOGGER.debug("read> sync start=" + duration);
                        } else {
                            LOGGER.error("read> sync error (reason: start<0 or start>=end)");
                        }

                    } else if (line.contains(SmiTag.PARAGRAPH_OPEN_TAG_START) && line.contains(SmiTag.PARAGRAPH_OPEN_TAG_START)) {

                        String content = line;
                        if (line.contains(SmiTag.COMMENTS_OPEN_TAG)) {
                            LOGGER.debug("read> commentdata!");

                            int start = content.indexOf(SmiTag.COMMENTS_OPEN_TAG) + SmiTag.COMMENTS_OPEN_TAG.length();
                            int end = content.indexOf(SmiTag.COMMENTS_CLOSE_TAG);

                            //#804 add exception handling
                            if (start >= 0 && end > start) {
                                content = content.substring(start, end);
                                byte[] decodedContent = Base64.decode(content.getBytes(), Base64.DEFAULT);
                                BlackboxMetadata data = readMetadata(new String(decodedContent));

                                if (data != null) {
                                    readedMetadataMap.put(lastDuration, data);
                                }
                            } else {
                                LOGGER.error("read> error (reason: start<0 or start>=end)");
                            }


                        }
                    }
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (readedMetadataMap.size() <= 0) {
                    return null;
                }
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return readedMetadataMap;
    }

    private static BlackboxMetadata readMetadata(String input) {
        BlackboxMetadata result = new BlackboxMetadata();
        result.unflatten(input);
        return result;
    }
}
