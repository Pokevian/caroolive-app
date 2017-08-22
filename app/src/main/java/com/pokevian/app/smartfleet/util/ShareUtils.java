package com.pokevian.app.smartfleet.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.pokevian.app.smartfleet.R;

import org.apache.log4j.Logger;

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

    public static void doShare(Activity activity) throws FileNotFoundException {
        View view = activity.getWindow().getDecorView();
//        view.buildDrawingCache();
//        view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);


        Bitmap b = Bitmap.createBitmap(1080, 2496, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);




        Logger.getLogger("share").warn("doShare#1 " + view.getWidth() + "x" + view.getHeight());
        Logger.getLogger("share").warn("doShare#2 " + view.getLayoutParams().width + "x" + view.getLayoutParams().height);
//        view.setDrawingCacheEnabled(true);
        Logger.getLogger("share").warn("doShare#3 " + view.getWidth() + "x" + view.getHeight());
//        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
//        view.measure(/*View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY)*/1080, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        Logger.getLogger("share").warn("doShare#4 " + view.getWidth() + "x" + view.getHeight());
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
//        view.layout(0, 0, /*view.getMeasuredWidth()*/1080, view.getMeasuredHeight());
        Logger.getLogger("share").warn("doShare#5 " + view.getWidth() + "x" + view.getHeight());

        view.buildDrawingCache();
        Logger.getLogger("share").warn("doShare#6 " + view.getWidth() + "x" + view.getHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());

//        view.setDrawingCacheEnabled(false);

        Logger.getLogger("share").warn("doShare#7 " + view.getWidth() + "x" + view.getHeight());

        File imageFile = getShareImageFile();
        FileOutputStream fos = new FileOutputStream(imageFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        close(fos);
        activity.startActivity(Intent.createChooser(createShareImageIntent(imageFile), activity.getString(R.string.action_share)));

//        Rect frame = new Rect();
//        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
//        int statusBarHeight = frame.top;
//
//        Rect statusBar = new Rect();
//        view.getWindowVisibleDisplayFrame(statusBar);
//
//        Bitmap snapshot = Bitmap.createBitmap(bitmap, 0, statusBar.top, bitmap.getWidth(), bitmap.getHeight() - statusBar.top);
//
//        File imageFile = getShareImageFile();
//        FileOutputStream fos = new FileOutputStream(imageFile);
//        snapshot.compress(Bitmap.CompressFormat.PNG, 100, fos);
//        close(fos);
//        activity.startActivity(createShareImageIntent(imageFile));
    }

    public static void doShare(Context context, View view) throws FileNotFoundException {
        File imageFile = getShareImageFile();
        FileOutputStream fos = new FileOutputStream(imageFile);
        snapshot(view).compress(Bitmap.CompressFormat.PNG, 100, fos);
        close(fos);
        context.startActivity(Intent.createChooser(createShareImageIntent(imageFile), context.getString(R.string.action_share)));
    }

    public static void doShare(Context context, Bitmap bitmap) throws FileNotFoundException {
        File imageFile = getShareImageFile();
        FileOutputStream fos = new FileOutputStream(imageFile);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        close(fos);
        context.startActivity(Intent.createChooser(createShareImageIntent(imageFile), context.getString(R.string.action_share)));
    }

    private static void snapshotWithoutStatusBar() {

    }

    public static void saveImage(Bitmap bitmap) {

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
