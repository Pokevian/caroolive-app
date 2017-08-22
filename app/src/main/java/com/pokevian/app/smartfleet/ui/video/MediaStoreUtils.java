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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

public final class MediaStoreUtils {

    private MediaStoreUtils() {
    }

    public static Uri retrieveVideoContentUri(ContentResolver resolver, File file) {
        Uri contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Cursor c = resolver.query(contentUri,
                new String[]{MediaStore.Video.Media._ID},
                MediaStore.Video.Media.DATA + "=?",
                new String[]{file.getAbsolutePath()},
                null);
        Uri uri = null;
        if (c.moveToFirst()) {
            String id = c.getString(0);
            uri = Uri.withAppendedPath(contentUri, id);
        }
        c.close();
        return uri;
    }

    public static Uri insertVideo(ContentResolver resolver, File file, String mimeType) {
        ContentValues content = new ContentValues();
        content.put(MediaStore.Video.VideoColumns.TITLE, file.getName());
        content.put(MediaStore.Video.VideoColumns.DATE_ADDED, file.lastModified() / 1000);
        content.put(MediaStore.Video.Media.MIME_TYPE, mimeType);
        content.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());
        content.put(MediaStore.Video.VideoColumns.SIZE, file.length());
        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, content);
    }

}
