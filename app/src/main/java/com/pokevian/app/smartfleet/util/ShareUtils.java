package com.pokevian.app.smartfleet.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.view.View;

import com.pokevian.app.smartfleet.R;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ian on 2016-05-04.
 */
public class ShareUtils {

    public static void doShare(Context context, View view) throws FileNotFoundException {
        File imageFile = getShareImageFile();
        FileOutputStream fos = new FileOutputStream(imageFile);
        snapshot(view).compress(Bitmap.CompressFormat.PNG, 100, fos);
        close(fos);
        context.startActivity(Intent.createChooser(createShareImageIntent(imageFile), context.getString(R.string.action_share)));
    }

    private static Intent createShareImageIntent(File file) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

        return shareIntent;
    }

    public static Bitmap snapshot(View view) {
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);

        return bitmap;
    }

    private static File getShareImageFile() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (dir.mkdirs()) {

        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        return new File(dir, simpleDateFormat.format(new Date()) + ".png");
    }

    private static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
