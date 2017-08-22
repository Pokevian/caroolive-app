package com.pokevian.app.smartfleet.ui.driving;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.util.DimenUtils;
import com.pokevian.app.smartfleet.util.OrientationUtils;

import org.apache.log4j.Logger;

public class BaseDrivingActivity extends BaseActivity {
	final String TAG = "scale_";
	
	protected View mView;
	private boolean mIsScalable;
	private boolean mIsPaused;

	protected int mHeight = 0;
	protected int mWidth = 0;

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		Logger.getLogger(TAG).debug("Layout Width - " + String.valueOf(mView.getWidth()));
		Logger.getLogger(TAG).debug("Layout Height - " + String.valueOf(mView.getHeight()));

//		setContentView(layoutResID, true);
	}

	@Override
	public void setContentView(final int layoutResID) {
		Logger.getLogger(TAG).debug("setContentView");
		super.setContentView(R.layout.activity_base);

		mView = findViewById(R.id.content);
		LayoutInflater.from(this).inflate(layoutResID, (ViewGroup) mView);
		mView.post(new Runnable() {
			@Override
			public void run() {
				LayoutInflater.from(getApplicationContext()).inflate(layoutResID, (ViewGroup) mView);
				mWidth = mView.getWidth();
				mHeight = mView.getHeight();

				Logger.getLogger(TAG).debug("setContentView" + String.valueOf(mWidth) + "x" + String.valueOf(mHeight));

				doScale(true);
			}
		});

	}

	public void setContentView(int layoutResID, final boolean isScalable) {
		mView = findViewById(R.id.content);
		LayoutInflater.from(this).inflate(layoutResID, (ViewGroup) mView);
		
//		mHeight = ((CarooApp) getApplication()).getHeight();
//		mHeight = getApplicationInfo
		if( mHeight == 0) {
			final View mainRoot = (View)findViewById(R.id.calc_layout);
			ViewTreeObserver vto = mainRoot.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                @SuppressWarnings("deprecation")
				@Override
			    public void onGlobalLayout() {
			    	mHeight = mainRoot.getHeight();
//			    	Log.w(TAG, "onGlobalLayout@"+ mainRoot.getWidth());
//			    	((CarooApp) getApplication()).setHeight(mHeight);
			    	doScale(isScalable);
			        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			        	mainRoot.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			        } else {
			        	mainRoot.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			        }
			    }

			});
		} else {
			doScale(isScalable);
		}

	}	
	
	/*public void setContentView(int layoutResID, final boolean isScalable) {
        setContentView(layoutResID);
    }*/
	
	protected void doScale(boolean isScalable) {
		//ADD
	    
	    DisplayMetrics dm = getResources().getDisplayMetrics();
//	    Log.w(TAG,  "1 doScale@" +mHeight );
//	    Log.e(TAG,  "2 doScale@" + mView.getWidth() + "x"+ mView.getHeight());
//	    Log.w(TAG,  "3 doScale@" + dm.widthPixels + "x"+ dm.heightPixels);
	    
	    int orientation = OrientationUtils.getOrientation(this);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && /*mHeight > 600 &&*/ orientation == Configuration.ORIENTATION_LANDSCAPE) {
			if (isScalable) {
				scale();
			}
			mIsScalable = isScalable;
		} else {
			mIsScalable = false;
		}
	}
	
	/*@Override
	protected void onPause() {
		mIsPaused = true;
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();		
		mIsPaused = false;
	}
	
	protected boolean isPaused() {
		return mIsPaused;
	}*/
	
	/*@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
//		doScale(mIsScalable);
		final View mainRoot = (View)findViewById(R.id.calc_layout);
		ViewTreeObserver vto = mainRoot.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @SuppressWarnings("deprecation")
			@Override
		    public void onGlobalLayout() {
		    	mHeight = mainRoot.getHeight();
		    	((CarooApp) getApplication()).setHeight(0);//recheck
		    	
		    	doScale(mIsScalable);
		        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
		        	mainRoot.getViewTreeObserver().removeOnGlobalLayoutListener(this);
		        } else {
		        	mainRoot.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		        }
		    }

		});
	}*/
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void scale() {
		if (mView == null) return;
		
		ActionBar bar = getSupportActionBar();
		boolean hasActionBar = bar != null;
		if (bar != null) {
			TypedValue tv = new TypedValue();
			if (bar.getThemedContext().getTheme().resolveAttribute(android.R.attr.windowActionBarOverlay, tv, true)) {
//				Log.d(TAG, "### windowActionBarOverlay");
				hasActionBar = false;
			}
		}
//		Log.w(TAG, "hasActionBar=" + hasActionBar);
		
		Resources ress = getResources();
		DisplayMetrics dm = ress.getDisplayMetrics();
		
		int bw = ress.getDimensionPixelSize(R.dimen.base_land_width_with_soft);
		int bh = ress.getDimensionPixelSize(R.dimen.base_land_height);
		if ((float)dm.widthPixels / dm.heightPixels > 1.7f) {
            bw = ress.getDimensionPixelSize(R.dimen.base_land_width);
        }

		
		/*int orientation = OrientationUtils.getOrientation(this);
		ViewGroup.LayoutParams lp = mView.getLayoutParams();
		if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			lp.width = ress.getDimensionPixelSize(R.dimen.base_land_width);
			lp.height = ress.getDimensionPixelSize(R.dimen.base_land_height);
		} else {
			lp.width = ress.getDimensionPixelSize(R.dimen.base_port_width);
			lp.height = ress.getDimensionPixelSize(R.dimen.base_port_height);
		}
//		mView.setLayoutParams(lp);
		
		bw = lp.width;
		bh = lp.height;*/
		
//		DisplayMetrics dm = ress.getDisplayMetrics();
//		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int w = dm.widthPixels;
		int h = mHeight;//dm.heightPixels;
//		h = dm.heightPixels;
		
		/*lp.width = ress.getDimensionPixelSize(R.dimen.base_land_width_with_soft);
		lp.height = ress.getDimensionPixelSize(R.dimen.base_land_height);
		if ((float)dm.widthPixels / dm.heightPixels > 1.7f) {
		    lp.width = ress.getDimensionPixelSize(R.dimen.base_land_width);
		}*/
	        
//		Log.e(TAG, "scale@" + dm.widthPixels + "x" + dm.heightPixels + "[" + mHeight + "]" + ": " + bw + "x" + bh);
//		Log.w(TAG, "DisplayMetrics@" + dm.density + "(" + dm.scaledDensity + ")" + ": " + dm.densityDpi + ": " + dm.widthPixels + "x" + dm.heightPixels);
		
//	    mView.setLayoutParams(lp);
	        
		
		
//		int sbh = DimenUtils.getStatusBarHeight(ress);
//		boolean hasTopStatusbar = ((CarooApp) getApplication()).hasTopStatusBar();
//		if(hasTopStatusbar) {
//			h -= sbh;
//		}		
		
		int abh = DimenUtils.getActionBarHeight(ress);
		if (hasActionBar) {
			h -= abh;
		}
		
		float sx = (float)w / bw;
		float sy = (float)h / bh;
		
		float scale = sx < sy ? sx : sy;
		if (scale > 1.1f) {
		    
		    mView.setPivotX(dm.widthPixels / 2);
		    mView.setPivotY(mHeight);
		    
            mView.setScaleX(scale);
            mView.setScaleY(scale);
//            Log.e(TAG,  ">>> scale@" + scale);
        }
		
//		Log.w(TAG,  "scale@" + scale);
		
		/*mView.setPivotX(dm.widthPixels/2);
		mView.setPivotY(dm.heightPixels);*/
		/*if (scale > 1f &&  Math.abs(1f - Math.abs(scale)) > 0.1f) {
		    mView.setScaleX(scale);
		    mView.setScaleY(scale);
		}*/
		

	}
}
