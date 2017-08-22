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

package com.pokevian.app.smartfleet.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {

    private ImageUtils() {
    }

    public static Bitmap masking(Resources res, int originalResId, int maskResId) {
        Bitmap original = BitmapFactory.decodeResource(res, originalResId);
        Bitmap mask = BitmapFactory.decodeResource(res, maskResId);

        int width = Math.max(original.getWidth(), mask.getWidth());
        int height = Math.max(original.getHeight(), mask.getHeight());

        Bitmap result = Bitmap.createBitmap(width, height, Config.ARGB_8888);

        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        float left = (float) (width - original.getWidth()) / 2;
        float top = (float) (height - original.getHeight()) / 2;

        canvas.drawBitmap(original, left, top, null);

        left = (float) (width - mask.getWidth()) / 2;
        top = (float) (height - mask.getHeight()) / 2;

        canvas.drawBitmap(mask, left, top, paint);

        return result;
    }

    public static Bitmap masking(Resources res, BitmapDrawable orginalDrawable, int maskResId) {
        Bitmap mask = BitmapFactory.decodeResource(res, maskResId);

        int width = Math.max(orginalDrawable.getIntrinsicWidth(), mask.getWidth());
        int height = Math.max(orginalDrawable.getIntrinsicHeight(), mask.getHeight());

        Bitmap result = Bitmap.createBitmap(width, height, Config.ARGB_8888);

        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        float left = (float) (width - orginalDrawable.getIntrinsicWidth()) / 2;
        float top = (float) (height - orginalDrawable.getIntrinsicHeight()) / 2;

        orginalDrawable.setBounds(0, 0, orginalDrawable.getIntrinsicWidth(),
                orginalDrawable.getIntrinsicHeight());
        canvas.drawBitmap(orginalDrawable.getBitmap(), left, top, null);

        left = (float) (width - mask.getWidth()) / 2;
        top = (float) (height - mask.getHeight()) / 2;

        canvas.drawBitmap(mask, left, top, paint);

        return result;
    }

    public static File saveTo(Bitmap bitmap, File to) {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(to);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            return to;
        } catch (IOException e) {
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
        }

        return null;
    }

}
