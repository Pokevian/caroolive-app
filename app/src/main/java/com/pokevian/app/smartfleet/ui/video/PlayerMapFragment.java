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

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.BlackboxMetadata;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;


@SuppressLint("ValidFragment")
public class PlayerMapFragment extends Fragment {

    static final String TAG = "PlayerMapFragment";
    final Logger log = Logger.getLogger(TAG);

    private static final float DEFAULT_ZOOM_LEVEL = 16;

    private static final int MSG_MOVE_CAMERA = 0;
    private static final int MSG_ANIMATE_CAMERA = 1;
    private static final int MSG_SET_MARKERPOS = 2;
    private static final int MSG_DRAW_PATH = 3;
    private static final int MSG_ADD_MARKER = 4;

    private View mFragmentView;
    private FrameLayout mTouchView;

    private MapView mMapView;
    private GoogleMap mGoogleMap;
    private GestureDetector mGestureDetector;
    private List<OnScrollListener> mOnScrollListeners;

    private MarkerOptions mMyMarkerOptions;
    private Marker mMyMarker;
    private LinkedHashMap<Long, BlackboxMetadata> mReadedMetadataMap;

    private boolean mIsUserCenter = false;
    private float mCurrentZoom;

    private Polyline mAddedMapPolyline;
    private UpdatePolyline mUpdatePathThread;

    private View mMapControlButtonView;
    private ImageView mTrackingLocationBtn;
    private ImageView mShowTrackBtn;

    public PlayerMapFragment() {
        mCurrentZoom = DEFAULT_ZOOM_LEVEL;

        mGestureDetector = new GestureDetector(getActivity(), new MapGestureListener());
        mOnScrollListeners = new ArrayList<OnScrollListener>(0);

        mIsUserCenter = false;
    }

    public static Fragment newInstance() {
        return new PlayerMapFragment();
    }

    public PlayerMapFragment(float zoomlevel) {
        mGestureDetector = new GestureDetector(getActivity(), new MapGestureListener());
        mOnScrollListeners = new ArrayList<OnScrollListener>(0);

        mIsUserCenter = false;

        mCurrentZoom = zoomlevel;
    }

    public void registerOnScrollListener(OnScrollListener listener) {
        mOnScrollListeners.add(listener);
    }

    public interface OnScrollListener {
        void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);
    }

    private class MapGestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            log.error("[MapGestureListener] disX=" + distanceX + ", disY=" + distanceY);
            if (Math.abs(distanceX) > 10 || Math.abs(distanceY) > 10) {
                mTrackingLocationBtn.setVisibility(View.VISIBLE);
                mIsUserCenter = true;

                if (mGoogleMap != null) {
                    mCurrentZoom = mGoogleMap.getCameraPosition().zoom;
                }

            }
            return false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFragmentView = inflater.inflate(R.layout.fragment_player_map, container, false);

        mTouchView = new TouchableWrapper(getActivity());
        mTouchView.addView(mFragmentView);

        MapsInitializer.initialize(getActivity());

        mMapView = (MapView) mFragmentView.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mGoogleMap = mMapView.getMap();

        if (mGoogleMap != null) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mGoogleMap.getUiSettings().setCompassEnabled(false);
            mGoogleMap.getUiSettings().setZoomControlsEnabled(false);
            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
            mGoogleMap.setMyLocationEnabled(true);

            float u = (float) 0.5;
            float v = (float) 0.5;
            mMyMarkerOptions = new MarkerOptions().title("me")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location))
                    .anchor(u, v);


            //set last location
            LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location != null) {
                Message msg = mHandler.obtainMessage(MSG_MOVE_CAMERA);
                Bundle bundle = new Bundle();
                bundle.putDouble("lat", location.getLatitude());
                bundle.putDouble("lng", location.getLongitude());
                bundle.putFloat("zoomlevel", mCurrentZoom);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }

        }

        mMapControlButtonView = mFragmentView.findViewById(R.id.map_control);
        mTrackingLocationBtn = (ImageView) mFragmentView.findViewById(R.id.tracking_location);
        mTrackingLocationBtn.setOnClickListener(clickListener);
        mTrackingLocationBtn.setVisibility(View.INVISIBLE);

        mShowTrackBtn = (ImageView) mFragmentView.findViewById(R.id.show_track);
        mShowTrackBtn.setOnClickListener(clickListener);

        return mTouchView;
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            double lat = -1;
            double lng = -1;
            float zoomlevel = -1;
            switch (msg.what) {
                case MSG_MOVE_CAMERA:
                    if (mGoogleMap != null) {
                        lat = bundle.getDouble("lat");
                        lng = bundle.getDouble("lng");
                        zoomlevel = bundle.getFloat("zoomlevel");
                        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), zoomlevel));
                    }
                    break;
                case MSG_ANIMATE_CAMERA:
                    if (mGoogleMap != null) {
                        lat = bundle.getDouble("lat");
                        lng = bundle.getDouble("lng");
                        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));
                    }
                    break;
                case MSG_SET_MARKERPOS:
                    lat = bundle.getDouble("lat");
                    lng = bundle.getDouble("lng");
                    if (mMyMarker != null) {
                        mMyMarker.setPosition(new LatLng(lat, lng));
                    }
                    break;
                case MSG_DRAW_PATH:
                    if (mAddedMapPolyline != null) {
                        mAddedMapPolyline.remove();
                    }
                    if (mGoogleMap != null) {
                        mAddedMapPolyline = mGoogleMap.addPolyline((PolylineOptions) msg.obj);
                    }
                    break;
                case MSG_ADD_MARKER:
                    if (mGoogleMap != null && mMyMarkerOptions != null && mMyMarker == null) {
                        lat = bundle.getDouble("lat");
                        lng = bundle.getDouble("lng");
                        mMyMarkerOptions.position(new LatLng(lat, lng));
                        mMyMarker = mGoogleMap.addMarker(mMyMarkerOptions);
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    @Override
    public void onDestroy() {
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null) {
            mMapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onPause() {
        if (mMapView != null) {
            mMapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
        }
    }

    @Override
    public void onLowMemory() {

        if (mMapView != null) {
            mMapView.onLowMemory();
        }
        super.onLowMemory();
    }

    public void updateLocation(final BlackboxMetadata meta) {
        if (meta != null && meta.locationData.isValid) {
            LatLng latLng = new LatLng(meta.locationData.latitude, meta.locationData.longitude);
            if (mMyMarker == null) {
                Message msg = mHandler.obtainMessage(MSG_ADD_MARKER);
                Bundle bundle = new Bundle();
                bundle.putDouble("lat", latLng.latitude);
                bundle.putDouble("lng", latLng.longitude);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            } else {
                Message msg = mHandler.obtainMessage(MSG_SET_MARKERPOS);
                Bundle bundle = new Bundle();
                bundle.putDouble("lat", latLng.latitude);
                bundle.putDouble("lng", latLng.longitude);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }

            if (!mIsUserCenter) {
                Message msg = mHandler.obtainMessage(MSG_MOVE_CAMERA);
                Bundle bundle = new Bundle();
                bundle.putDouble("lat", latLng.latitude);
                bundle.putDouble("lng", latLng.longitude);
                bundle.putFloat("zoomlevel", mCurrentZoom);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
        }
    }

    public void updatePath(LinkedHashMap<Long, BlackboxMetadata> readedMap) {
        mReadedMetadataMap = readedMap;
        startUpdate();
        mIsUserCenter = false;
    }

    public float getCurrentZoomLevel() {
        return mCurrentZoom;
    }

    public ViewGroup.LayoutParams getLayoutParams() {
        return mMapView.getLayoutParams();
    }

    public void addTopMarginOfMapControlView(int topMargin) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = topMargin + pixelFromDip(15);
        params.rightMargin = pixelFromDip(10);
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        mMapControlButtonView.setLayoutParams(params);
    }

    private int pixelFromDip(int dip) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dip * scale);
    }

    private void startUpdate() {
        stopUpdate();
        if (mUpdatePathThread == null) {
            mUpdatePathThread = new UpdatePolyline();
            mUpdatePathThread.setName("update_path");
            mUpdatePathThread.start();
        }
    }


    private void stopUpdate() {
        if (mUpdatePathThread != null) {
            mUpdatePathThread.interrupt();
            mUpdatePathThread = null;
        }
    }

    class UpdatePolyline extends Thread {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            PolylineOptions videoPolylineOptions = new PolylineOptions();
            videoPolylineOptions.color(Color.RED);
            if (mReadedMetadataMap != null) {
                LinkedHashMap<Long, BlackboxMetadata> clone = ((LinkedHashMap<Long, BlackboxMetadata>) mReadedMetadataMap.clone());
                Iterator<BlackboxMetadata> iter = clone.values().iterator();
                while (iter.hasNext()) {
                    if (interrupted()) {
                        break;
                    }
                    BlackboxMetadata meta = iter.next();
                    if (meta != null && meta.locationData.isValid) {
                        LatLng latLng = new LatLng(meta.locationData.latitude,
                                meta.locationData.longitude);
                        videoPolylineOptions.add(latLng);
                    }
                }
            }
            drawPath(videoPolylineOptions);
        }
    }

    private void drawPath(final PolylineOptions videoPolylineOptions) {
        Message msg = mHandler.obtainMessage(MSG_DRAW_PATH);
        msg.obj = videoPolylineOptions;
        mHandler.sendMessage(msg);
    }

    public void setTrackingLocation() {
        if (mGoogleMap != null) {
            if (mMyMarker != null) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(mMyMarker.getPosition()));
            }

            mIsUserCenter = false;
        }
    }


    @SuppressWarnings("unchecked")
    public void showTrack() {
        if (mGoogleMap != null && mReadedMetadataMap != null) {
            LatLngBounds.Builder bc = new LatLngBounds.Builder();
            boolean isIncluded = false;
            LinkedHashMap<Long, BlackboxMetadata> clone = ((LinkedHashMap<Long, BlackboxMetadata>) mReadedMetadataMap.clone());
            Iterator<BlackboxMetadata> iter = clone.values().iterator();
            while (iter.hasNext()) {
                BlackboxMetadata meta = iter.next();
                if (meta != null && meta.locationData.isValid) {
                    bc.include(new LatLng(meta.locationData.latitude, meta.locationData.longitude));
                    isIncluded = true;
                }
            }

            if (isIncluded) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bc.build(), 100));
            }

            mIsUserCenter = true;
        }
    }


    class TouchableWrapper extends FrameLayout {
        public TouchableWrapper(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            mGestureDetector.onTouchEvent(ev);
            return super.dispatchTouchEvent(ev);
        }
    }


    private void trackingLocationBtnClicked() {
        setTrackingLocation();
        mTrackingLocationBtn.setVisibility(View.INVISIBLE);

    }

    private void showTrackBtnClicked() {
        showTrack();
        mTrackingLocationBtn.setVisibility(View.VISIBLE);

    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.tracking_location) {
                trackingLocationBtnClicked();
            } else if (id == R.id.show_track) {
                showTrackBtnClicked();
            }
        }
    };
}
