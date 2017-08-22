
package com.pokevian.app.smartfleet.ui.driving;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.pokevian.app.smartfleet.R;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class BlackboxPreview2 extends BlackboxPreview {

    final String TAG = "bb-prev2";

    private TextureView mTextureView;
    public int width, height;
    public boolean isSurfaceCreated = false;

    public BlackboxPreview2(Context context, int contentLayoutResId) {
        super(context, contentLayoutResId);
        
        mTextureView = (TextureView)mContentView.findViewById(R.id.preview_texture);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    public SurfaceTexture getSurfaceTexture() {
        if (mTextureView != null) {
            return mTextureView.getSurfaceTexture();
        }
        
        return null;
    }
    
    @Override
    protected void setCallback() { }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable@" + width + "x" + height);
            Log.i(TAG, "blackbox preview surface created");
            isSurfaceCreated = true;
            BlackboxPreview2.this.width = width;
            BlackboxPreview2.this.height = height;
            
//            scale(mControlView);
//            mCallback.onCreated();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            Log.d(TAG, "onSurfaceTextureSizeChanged@" + width + "x" + height);
            BlackboxPreview2.this.width = width;
            BlackboxPreview2.this.height = height;
            
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            isSurfaceCreated = false;
//            mCallback.onDestroyed();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };
    
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void configureTransform() {
        configureTransform(new Size(width, height));
    }
    
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void configureTransform(Size preview) {
        Log.d(TAG, "configureTransform@" + width + "x" + height);

        int rotation = getWindow().getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, width, height);
        RectF bufferRect = new RectF(0, 0, preview.getHeight(), preview.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            
            float scale = Math.max(
                    (float) height / preview.getHeight(),
                    (float) width / preview.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        
        mTextureView.setTransform(matrix);
    }

}
