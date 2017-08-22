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

package com.pokevian.app.smartfleet.ui.tripmonitor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.tripmonitor.DrivingDate;
import com.pokevian.app.smartfleet.model.tripmonitor.DrivingDate.UserEcoDate;
import com.pokevian.app.smartfleet.model.tripmonitor.NoticeCategory;
import com.pokevian.app.smartfleet.model.tripmonitor.NoticeCategory.CategoryData;
import com.pokevian.app.smartfleet.request.GetDrivingDateRequest;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.util.NetworkUtils;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripMonitorFragment extends Fragment implements OnClickListener, RadioGroup.OnCheckedChangeListener {

    static final String TAG = "TripMonitorFragment";
    final Logger logger = Logger.getLogger(TAG);

    private static final String ARG_SECTION_NUMBER = "section_number";

    public static final int REQUEST_WEB_POPUP = 1;

    private static final int CHART_PERIOD_DAY = 0;
    private static final int CHART_PERIOD_WEEK = 1;
    private static final int CHART_PERIOD_MONTH = 2;

    private static final int VIDEO_PERIOD_DAY = 0;
    private static final int VIDEO_PERIOD_WEEK = 1;
    private static final int VIDEO_PERIOD_MONTH = 2;
    private static final int VIDEO_PERIOD_ALL = 100;

    public static final int MENU_MAIN = 0;
    public static final int MENU_TRIP = 0;
    public static final int MENU_CHART = 1;
    public static final int MENU_VIDEO = 2;
    public static final int MENU_NOTICE = 3;
    private static final int MENU_SETTING = 4;

    private ControlBarLayout mControlBarLayout;

    private WebView mBrowser;
    private ProgressBar mProgressBar;
    private String mLastUrl;
    private boolean mIsReRequest;

    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueueForCookie();

    private CaldroidFragment mDialogCalendarFragment;
    private ChartTypeFragment mDialogChartTypeFragment;

    private List<String> mDrivingDateList = null;
    private ArrayList<CategoryData> mNoticeCategoryList = null;
    private Date mCurrentDate;

    final SimpleDateFormat mFormatterDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    final SimpleDateFormat mFormatterDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    final SimpleDateFormat mFormatterDateTimeForVideo = new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss", Locale.getDefault());

    private TextView mTvCurrentDate;
    private Button mBtnChartList;
    private RadioGroup mRadioGroupChart;
    private RadioButton mRadioChartDay;
    private RadioButton mRadioChartWeek;
    private RadioButton mRadioChartMonth;
    private RadioGroup mRadioGroupVideo;
    private RadioButton mRadioVideoDay;
    private RadioButton mRadioVideoWeek;
    private RadioButton mRadioVideoMonth;
    private RadioButton mRadioVideoAll;
    private RadioGroup mRadioGroupNotice;

    private int mSelectedMenu = -1;

    public static TripMonitorFragment newInstance(int sectionNumber) {
        TripMonitorFragment fragment = new TripMonitorFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_trip_monitor, container, false);

        mControlBarLayout = (ControlBarLayout) rootView.findViewById(R.id.custom_control_frame);
        mTvCurrentDate = mControlBarLayout.getTextDate();
        mBtnChartList = mControlBarLayout.getBtnChartList();

        mRadioGroupChart = mControlBarLayout.getRadioGroupChart();
        mRadioChartDay = mControlBarLayout.getRadioChartDay();
        mRadioChartWeek = mControlBarLayout.getRadioChartWeek();
        mRadioChartMonth = mControlBarLayout.getRadioChartMonth();

        mRadioGroupVideo = mControlBarLayout.getRadioGroupVideo();
        mRadioVideoDay = mControlBarLayout.getRadioVideoDay();
        mRadioVideoWeek = mControlBarLayout.getRadioVideoWeek();
        mRadioVideoMonth = mControlBarLayout.getRadioVideoMonth();
        mRadioVideoAll = mControlBarLayout.getRadioVideoAll();

        mRadioGroupNotice = mControlBarLayout.getRadioGroupNotice();

        mRadioGroupChart.setOnCheckedChangeListener(this);
        mRadioGroupVideo.setOnCheckedChangeListener(this);
        mRadioGroupNotice.setOnCheckedChangeListener(this);

        mDialogCalendarFragment = new CaldroidFragment();
        mDialogCalendarFragment.setCaldroidListener(mCalendarListener);

        mDialogChartTypeFragment = new ChartTypeFragment();
        mDialogChartTypeFragment.setChartTypeListener(mChartTypeListener);

        mBrowser = (WebView) rootView.findViewById(R.id.web_view);
        mBrowser.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    String url = mBrowser.getUrl();
                    if (ServerUrl.UNREACHABLE_URL.equals(url)) {
                        if (mIsReRequest) {
                            getDrivingDate();
                        }
                        loadUrl(mLastUrl);
                    }
                }
                return false;
            }
        });
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progress);

        mProgressBar.setVisibility(View.INVISIBLE);
        mTvCurrentDate.setOnClickListener(this);

        mBtnChartList.setOnClickListener(this);

        mNoticeCategoryList = new ArrayList<CategoryData>();

        initBrowser();

        mCurrentDate = new Date(); // set date to today
        Calendar cal = Calendar.getInstance();
        cal.clear(Calendar.HOUR);
        cal.clear(Calendar.HOUR_OF_DAY);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        mCurrentDate.setTime(cal.getTimeInMillis());

        mDrivingDateList = new ArrayList<String>();

        if (savedInstanceState != null) {
            mSelectedMenu = savedInstanceState.getInt("selected_menu");
        } else {
            Bundle bundle = getArguments();
            if (bundle != null) {
                mSelectedMenu = bundle.getInt(ARG_SECTION_NUMBER);
            } else {
                mSelectedMenu = MENU_MAIN;
            }
        }

        logger.debug("[+] loadServiceUrl");
        loadServiceUrl(mSelectedMenu);
        logger.debug("[-] loadServiceUrl");
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        GetDrivingDateRequest request = new GetDrivingDateRequest(
//                new GetDrivingDateListener());
//        request.setTag(TAG);
//        mRequestQueue.add(request);
        getDrivingDate();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_WEB_POPUP) {
            if (resultCode == PopupActivity.RESULT_LOGOUT) {
                ((TripMonitorActivity) getActivity()).onSessionClosed();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        mRequestQueue.cancelAll(TAG);

        mBrowser.destroy();

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("current_date", mCurrentDate);
        outState.putInt("selected_menu", mSelectedMenu);
        super.onSaveInstanceState(outState);
    }

//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//
//        int selectecMenu = getArguments().getInt(ARG_SECTION_NUMBER);
//        ((MainActivity) activity).onSectionAttached(selectecMenu);
//    }

    @Override
    public void onCheckedChanged(RadioGroup group, int id) {
        if (group == mRadioGroupChart) {
            if (mRadioGroupChart.getCheckedRadioButtonId() == ((View) mRadioChartDay).getId()) {
                setChartPeriod(CHART_PERIOD_DAY);
            } else if (mRadioGroupChart.getCheckedRadioButtonId() == ((View) mRadioChartWeek).getId()) {
                setChartPeriod(CHART_PERIOD_WEEK);
            } else if (mRadioGroupChart.getCheckedRadioButtonId() == ((View) mRadioChartMonth).getId()) {
                setChartPeriod(CHART_PERIOD_MONTH);
            }
        } else if (group == mRadioGroupVideo) {
            if (mRadioGroupVideo.getCheckedRadioButtonId() == ((View) mRadioVideoDay).getId()) {
                Settings.setVideoPeriod(getActivity(), VIDEO_PERIOD_DAY);
            } else if (mRadioGroupVideo.getCheckedRadioButtonId() == ((View) mRadioVideoWeek).getId()) {
                Settings.setVideoPeriod(getActivity(), VIDEO_PERIOD_WEEK);
            } else if (mRadioGroupVideo.getCheckedRadioButtonId() == ((View) mRadioVideoMonth).getId()) {
                Settings.setVideoPeriod(getActivity(), VIDEO_PERIOD_MONTH);
            } else if (mRadioGroupVideo.getCheckedRadioButtonId() == ((View) mRadioVideoAll).getId()) {
                Settings.setVideoPeriod(getActivity(), VIDEO_PERIOD_ALL);
            }
            logger.debug("[+] loadVideoUrl@onCheckedChanged");
            loadVideoUrl();
            logger.debug("[-] loadVideoUrl@onCheckedChanged");
        } else if (group == mRadioGroupNotice) {
            for (int i = 0; i < mRadioGroupNotice.getChildCount(); i++) {

                if (i % 2 != 0) { // divider
                    continue;
                }
                if (mRadioGroupNotice.getChildAt(i).getId() == id) {
                    setNoticeCategory((i / 2) - 1); // -1:all, 0 ~ : from server
                    break;

                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mTvCurrentDate) {
            showCalendarPopup();
        } else if (v == mBtnChartList) {
            if (!mDialogChartTypeFragment.isVisible()) {
                showChartTypePopup();
            } else {
                hideChartTypePopup();

            }
        }
    }

    private void getDrivingDate() {
        GetDrivingDateRequest request = new GetDrivingDateRequest(
                new GetDrivingDateListener());
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initBrowser() {
        if (mBrowser != null) {
            mBrowser.getSettings().setJavaScriptEnabled(true);
            mBrowser.getSettings().setNeedInitialFocus(true);
            mBrowser.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
            mBrowser.getSettings().setSupportMultipleWindows(false);

            mBrowser.getSettings().setUseWideViewPort(true);

            mBrowser.addJavascriptInterface(new JavaScriptInterface(), "androidInterface");
            mBrowser.setWebViewClient(mWebViewClient);
            mBrowser.setWebChromeClient(webChromeClient);
        }
    }


    private void loadUrl(String url) {
        Logger.getLogger(TAG).trace("loadUrl#" + url);
        if (!ServerUrl.UNREACHABLE_URL.equals(url) && !url.contains("javascript:")) {
            mLastUrl = url;
        }

        if (NetworkUtils.isConnected(getActivity())) {
            mBrowser.loadUrl(url);
        } else {
            mBrowser.loadUrl(ServerUrl.UNREACHABLE_URL);
        }
    }


    private WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler,
                                       SslError error) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.ssl_error_title);
            builder.setMessage(R.string.ssl_error_message);
            builder.setPositiveButton(R.string.ssl_error_continue, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.proceed();
                }
            });
            builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.cancel();
                }
            });

            builder.create().show();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Logger.getLogger(TAG).trace("shouldOverrideUrlLoading#" + url);
            if (isPopup(url)) {
                Intent intent = new Intent(getActivity(), PopupActivity.class);

                intent.putExtra("open_date", mCurrentDate);
                intent.putExtra("open_url", url);

                startActivityForResult(intent, REQUEST_WEB_POPUP);

                return true;

            }
            loadUrl(url);
            return true;
//            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Logger.getLogger(TAG).trace("onPageStarted#" + url);
            mProgressBar.setVisibility(View.VISIBLE);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Logger.getLogger(TAG).trace("onPageFinished#" + url);
            if (url.contains("/error_webview.html")) {
                mProgressBar.setVisibility(View.INVISIBLE);
            }
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
//            Toast.makeText(getActivity(), getString(R.string.web_loading_error) + description, Toast.LENGTH_SHORT).show();
            Logger.getLogger(TAG).debug("onReceivedError#" + failingUrl);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

    };

    private JsResult mLastJsResult;

    private WebChromeClient webChromeClient = new WebChromeClient() {

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            mLastJsResult = result;

            DialogFragment fragment = JsAlertDialogFramgment.newInstance(message);
            fragment.show(getChildFragmentManager(), JsAlertDialogFramgment.TAG);
            return true;
        }

    };

    public static class JsAlertDialogFramgment extends DialogFragment {

        public static final String TAG = "JsDialogFragment";

        private String mMessage;

        public static JsAlertDialogFramgment newInstance(String message) {
            JsAlertDialogFramgment fragment = new JsAlertDialogFramgment();
            Bundle args = new Bundle();
            args.putString("message", message);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mMessage = args.getString("message");
            } else {
                mMessage = savedInstanceState.getString("message");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("message", mMessage);
            super.onSaveInstanceState(outState);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.app_name)
                    .setMessage(mMessage)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((TripMonitorFragment) getParentFragment()).onJsConfirm();
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((TripMonitorFragment) getParentFragment()).onJsCancel();
                        }
                    })
                    .create();
        }
    }

    private void onJsConfirm() {
        if (mLastJsResult != null) {
            mLastJsResult.confirm();
        }
    }

    private void onJsCancel() {
        if (mLastJsResult != null) {
            mLastJsResult.cancel();
        }
    }

    public class JavaScriptInterface {

        @JavascriptInterface
        public void sessionClosed() {
            logger.debug("sessionClosed()");

            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    ((TripMonitorActivity) getActivity()).onSessionClosed();
                }
            });
        }

        @JavascriptInterface
        public void onReady(final String url) {
            logger.debug("onReady() url=" + url);

            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (url.contains(ServerUrl.HOME_URL) || url.contains(ServerUrl.VIDEO_LIST_URL)) {
                        mTvCurrentDate.setVisibility(View.VISIBLE);
                        setCurrentDate(mCurrentDate);
                    }

                    if (isChartView(url)) {
//                        setChartPeriod(Settings.getChartPeriod(getActivity()));
                        setChartPeriod(CHART_PERIOD_WEEK);
                    }
                    if (isNoticeView(url)) {
//                        setNoticeCategory(Settings.getNoticeIndex(getActivity()));
                        mBrowser.loadUrl("javascript:setCategory()");
                    }

                    try {
                        mBrowser.clearHistory();
                    } catch (Exception e) {
                    }

                    mProgressBar.setVisibility(View.INVISIBLE);
                }
            });
        }

        @JavascriptInterface
        public void onStart(final String url) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
            });
        }

        @JavascriptInterface
        public void onComplete(final String url) {
            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mProgressBar.setVisibility(View.INVISIBLE);
                }
            });
        }

    }

    private void setCurrentDate(Date date) {
        mCurrentDate = date;

        mTvCurrentDate.setText(mFormatterDate.format(mCurrentDate));
//        mBrowser.loadUrl(String.format("javascript:getUserEcoData('%s')",
//                mFormatterDateTime.format(DateUtils.LocalTimeToUTCTime(mCurrentDate))));

        loadUrl(String.format("javascript:getUserEcoData('%s')",
                mFormatterDateTime.format(DateUtils.LocalTimeToUTCTime(mCurrentDate))));
    }

    private void setChartPeriod(int period) {
        String strPeriod = "day";

        switch (period) {
            case CHART_PERIOD_DAY:
                strPeriod = "day";
                break;

            case CHART_PERIOD_WEEK:
                strPeriod = "week";
                break;

            case CHART_PERIOD_MONTH:
                strPeriod = "month";
                break;

            default:
                break;
        }

        logger.debug("setChartPeriod#" + strPeriod);

        Settings.setChartPeriod(getActivity(), period);

        mBrowser.loadUrl(String.format("javascript:getUserEcoData('%s', '%s')",
                mFormatterDateTime.format(DateUtils.LocalTimeToUTCTime(mCurrentDate)), strPeriod));
    }

    private void setNoticeCategory(int index) {
        if (mNoticeCategoryList == null || mNoticeCategoryList.size() == 0) {
            return;
        }

        Settings.setNoticeIndex(getActivity(), index);

        String url;
        if (index == -1) {
            url = String.format("javascript:setCategory()");
        } else {
            url = String.format("javascript:setCategory(%s)", mNoticeCategoryList.get(index).getCATEGORY_NO());
            //mBrowser.loadUrl(String.format("javascript:setCategory(%s)", mNoticeCategoryList.get(index).getCATEGORY_NO()));
        }

        logger.debug("setNoticeCategory(): url=" + url);
        mBrowser.loadUrl(url);
    }

    private boolean isDrivingDate(Date date) {
        if (mDrivingDateList == null || mDrivingDateList.size() == 0) {
            return false;
        }

//        for (String strDate : mDrivingDateList) {
//            logger.debug("DATE:" + strDate);
//        }

        if (!mDrivingDateList.contains(mFormatterDate.format(date))) {
            return false;
        }

        return true;
    }

    private boolean isToday(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar targetDate = Calendar.getInstance();
        targetDate.setTime(date);

        if (today.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR)
                && today.get(Calendar.MONTH) == targetDate.get(Calendar.MONTH)
                && today.get(Calendar.DAY_OF_MONTH) == targetDate.get(Calendar.DAY_OF_MONTH)) {
            return true;
        }

        return false;
    }

    private void showCalendarPopup() {
        if (mDialogCalendarFragment.isVisible()) {
            return;
        }

        final String dialogTag = "CALDROID_DIALOG_FRAGMENT";

        Bundle bundle = new Bundle();

        bundle.putString(CaldroidFragment.DIALOG_TITLE, getResources().getString(R.string.popup_title_calendar));

        mDialogCalendarFragment.setArguments(bundle);
        mDialogCalendarFragment.show(getFragmentManager(), dialogTag);
    }

    private CaldroidListener mCalendarListener = new CaldroidListener() {

        @Override
        public void onSelectDate(Date date, View view) {
            if (mSelectedMenu == MENU_VIDEO) {
                setCurrentDate(date);
                mDialogCalendarFragment.dismiss();
                logger.debug("loadVideoUrl@onSelectDate");
                loadVideoUrl();
            } else if (mSelectedMenu == MENU_CHART) {
                setCurrentDate(date);
                mDialogCalendarFragment.dismiss();
                setChartPeriod(Settings.getChartPeriod(getActivity()));

            } else {
                if (isDrivingDate(date) || isToday(date)) {
                    logger.debug("loadDrivingDate@onSelectDate");
                    setCurrentDate(date);
                    mDialogCalendarFragment.dismiss();
                    mProgressBar.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void onChangeMonth(int month, int year) {
            logger.debug("onChangeMonth(): month=" + month + ", year=" + year);
        }

        @Override
        public void onLongClickDate(Date date, View view) {
        }

        @Override
        public void onCaldroidViewCreated() {
            logger.debug("onCaldroidViewCreated()");
            //mDialogCalendarFragment.setCalendarDate(mCurrentDate);
        }

    };

    private void showChartTypePopup() {
        if (mDialogChartTypeFragment.isVisible()) {
            return;
        }

        final String dialogTag = "SmartFMS:ChartType";
        mDialogChartTypeFragment.show(getFragmentManager(), dialogTag);
    }

    private void hideChartTypePopup() {
        if (mDialogChartTypeFragment.isVisible()) {
            mDialogChartTypeFragment.dismiss();
        }
    }

    final ChartTypeListener mChartTypeListener = new ChartTypeListener() {

        @Override
        public void onItemSelected(int position) {
            loadChartUrl(position);

            if (mDialogChartTypeFragment.isVisible()) {
                mDialogChartTypeFragment.dismiss();
            }
        }

    };

    public void loadServiceUrl(int menuId) {
        if (mControlBarLayout == null) return;

        mSelectedMenu = menuId;
//        ((MainActivity) getActivity()).onSectionAttached(mSelectedMenu);

        switch (menuId) {
            case MENU_MAIN:
                mControlBarLayout.enableDateContoller(true);
                mControlBarLayout.enableChartContoller(false);
                mControlBarLayout.enableVideoContoller(false);
                mControlBarLayout.enableNoticeContoller(false);
//                mBrowser.loadUrl(ServerUrl.HOME_URL);
                loadUrl(ServerUrl.HOME_URL);
                break;

            case MENU_CHART:
                mControlBarLayout.enableDateContoller(true);
                mControlBarLayout.enableChartContoller(true);
                mControlBarLayout.enableVideoContoller(false);
                mControlBarLayout.enableNoticeContoller(false);
                mRadioGroupChart.check(((View) mRadioChartDay).getId());
                mControlBarLayout.setVisibility(View.GONE);
                loadChartUrl(0);
                break;

            case MENU_VIDEO:
                mControlBarLayout.enableDateContoller(true);
                mControlBarLayout.enableChartContoller(false);
                mControlBarLayout.enableVideoContoller(true);
                mControlBarLayout.enableNoticeContoller(false);
                logger.debug("[+] check@loadServiceUrl");
                mRadioGroupVideo.check(((View) mRadioVideoAll).getId());
                logger.debug("[-] check@loadServiceUrl");

                logger.debug("[+] loadVideoUrl@loadServiceUrl");
                loadVideoUrl();
                logger.debug("[-] loadVideoUrl@loadServiceUrl");

                break;

            case MENU_NOTICE:
                mControlBarLayout.enableDateContoller(false);
                mControlBarLayout.enableChartContoller(false);
                mControlBarLayout.enableVideoContoller(false);
                mControlBarLayout.enableNoticeContoller(true);
                mControlBarLayout.setVisibility(View.GONE);

                if (mRadioGroupNotice.getChildCount() > 0) {
                    mRadioGroupNotice.check(mRadioGroupNotice.getChildAt(0).getId());
                }

//                mBrowser.loadUrl(ServerUrl.NOTICE_LIST_URL);
                loadUrl(ServerUrl.NOTICE_LIST_URL);
                break;

            case MENU_SETTING:
                break;

            default:
                break;
        }
    }

    private void loadChartUrl(int chartType) {
        switch (chartType) {
            case 0:
//                mBrowser.loadUrl(ServerUrl.CHART_DRIVING_STATISTICS_URL);
                loadUrl(ServerUrl.CHART_DRIVING_STATISTICS_URL);
                break;

            case 1:
                mBrowser.loadUrl(ServerUrl.CHART_ECOPOINT_URL);
                break;

            case 2:
                mBrowser.loadUrl(ServerUrl.CHART_FUELECONOMY_URL);
                break;

            case 3:
                mBrowser.loadUrl(ServerUrl.CHART_FUELCOST_URL);
                break;

            case 4:
                mBrowser.loadUrl(ServerUrl.CHART_DRIVINGRATE_URL);
                break;

            default:
                break;
        }
    }

    private void loadVideoUrl() {
        Calendar endDate = Calendar.getInstance();
        endDate.setTime(mCurrentDate);

        Calendar startDate = Calendar.getInstance();
        startDate.setTime(mCurrentDate);

        String url = ServerUrl.VIDEO_LIST_URL;

        int period = Settings.getVideoPeriod(getActivity());
        if (period != VIDEO_PERIOD_ALL) {
            switch (period) {
                case VIDEO_PERIOD_DAY:
                    startDate.add(Calendar.DAY_OF_MONTH, 0);
                    break;

                case VIDEO_PERIOD_WEEK:
                    startDate.add(Calendar.WEEK_OF_YEAR, -1);
                    break;

                case VIDEO_PERIOD_MONTH:
                    startDate.add(Calendar.MONTH, -1);
                    break;

                case VIDEO_PERIOD_ALL:
                    break;

                default:
                    break;
            }

            Date date = startDate.getTime();
            url += String.format("&startDate=%s&endDate=%s",
                    mFormatterDateTimeForVideo.format(DateUtils.LocalTimeToUTCTime(date)),
                    mFormatterDateTimeForVideo.format(DateUtils.LocalTimeToUTCTime(mCurrentDate)));
        }

        logger.debug("VIDEO URL=" + url);

        mBrowser.loadUrl(url);
    }

    private boolean isPopup(String url) {
        if (url.contains("/view.do?cmd=vInfoMaximizeMap")
                || url.contains("/drvYtVideo.do?cmd=searchVideo")
                /*|| url.contains("/chart.do?cmd=vEcoChart")*/) {

            return true;
        }
        return false;
    }

    private boolean isChartView(String url) {
        if (url.contains(ServerUrl.CHART_DRIVING_STATISTICS_URL)
                || url.contains(ServerUrl.CHART_ECOPOINT_URL)
                || url.contains(ServerUrl.CHART_FUELECONOMY_URL)
                || url.contains(ServerUrl.CHART_FUELCOST_URL)
                || url.contains(ServerUrl.CHART_DRIVINGRATE_URL)) {
            return true;
        }
        return false;
    }

    private boolean isNoticeView(String url) {
        if (url.contains(ServerUrl.NOTICE_LIST_URL)) {
            return true;
        }
        return false;
    }

    private class GetDrivingDateListener extends VolleyListener<DrivingDate> {

        @Override
        public void onResponse(DrivingDate date) {
            logger.debug("DrivingDateListener::onResponse()");
            mIsReRequest = false;

            if (date == null || date.getData() == null
                    || date.getData().getIsSelectUserEcoDate() == null
                    || date.getData().getIsSelectUserEcoDate().size() == 0) {
                logger.error("DrivingDateListener::onResponse(): invalid response");
                return;
            }

            if (mDrivingDateList != null) {
                mDrivingDateList.clear();
            }

            for (UserEcoDate ecoDate : date.getData().getIsSelectUserEcoDate()) {
                Date dateUTC = DateUtils.StringDateToDate(ecoDate.getDate());
                Date dateLocal = DateUtils.UTCTimeToLocalTime(dateUTC);

//                logger.debug("Driving Date=" + mFormatterDate.format(dateLocal));
                mDrivingDateList.add(mFormatterDate.format(dateLocal));
            }

            mDialogCalendarFragment.clearSelectedDates();

            try {
                if (mDrivingDateList != null && mDrivingDateList.size() > 0) {
                    mDialogCalendarFragment.setSelectedDateStrings(mDrivingDateList, "yyyy-M-d");
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // Request notice category
//            GetNoticeCategoryRequest request = new GetNoticeCategoryRequest(new GetNoticeCategoryListener());
//            request.setTag(TAG);
//            mRequestQueue.add(request);
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            mIsReRequest = true;
            Logger.getLogger(TAG).warn("onErrorResponse#" + error);
        }

    }

    private class GetNoticeCategoryListener extends VolleyListener<NoticeCategory> {

        @Override
        public void onResponse(NoticeCategory date) {
            logger.debug("NoticeCategoryListener::onResponse()");

            if (date == null || date.getCategoryList() == null) {
                logger.error("NoticeCategoryListener::onRequestSuccess(): response is null:");
                return;
            }

            List<CategoryData> list = date.getCategoryList();

            mNoticeCategoryList.clear();

            mNoticeCategoryList.addAll(list);

            mControlBarLayout.makeNoticeCategory(mNoticeCategoryList);
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            logger.error("NoticeCategoryListener::onErrorResponse(): " + error);
        }

    }

}