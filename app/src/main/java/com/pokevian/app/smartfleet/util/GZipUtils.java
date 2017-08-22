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

package com.pokevian.app.smartfleet.util;

import com.google.api.client.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by dg.kim on 2015-05-18.
 */
public class GZipUtils {

    public static String compress(String str) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(str.length());
        GZIPOutputStream gzip = new GZIPOutputStream(output);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        output.close();
        return Base64.encodeBase64String(output.toByteArray());
    }

    public static String uncompress(String str) throws IOException {
        byte[] bytes = Base64.decodeBase64(str);
        GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes));

        StringBuilder result = new StringBuilder();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = gzip.read(buffer)) > 0) {
            result.append(new String(buffer, 0, length, "UTF-8"));
        }
        return result.toString();
    }
}
