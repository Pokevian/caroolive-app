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
import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.util.NetworkUtils;

import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Stack;

public class PopupActivity extends BaseActivity {

    static final String TAG = "PopupActivity";
    final Logger logger = Logger.getLogger(TAG);

    public static final int RESULT_LOGOUT = 100;

    private FrameLayout mContainer;
    private WebView mBrowser;
    private String mLastUrl;
    private Stack<WebView> mStackPopup;
    private ProgressBar mProgressBar;

    private final SimpleDateFormat mFormatterDateTime = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private Date mCurrentDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logger.debug("onCreate");

        ActionBar bar = getSupportActionBar();
        //		bar.hide();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayUseLogoEnabled(false);
        bar.setDisplayShowHomeEnabled(false);
        bar.setTitle(R.string.tripmon_popup_route_detail);

        setContentView(R.layout.activity_popup);

        mContainer = (FrameLayout) findViewById(R.id.container);
        mBrowser = (WebView) findViewById(R.id.web_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);

        mProgressBar.setVisibility(View.INVISIBLE);

        mStackPopup = new Stack<WebView>();

        initBrowser();

        Intent intent = getIntent();
        if (intent != null) {
            mCurrentDate = (Date) intent.getSerializableExtra("open_date");
            String url = intent.getStringExtra("open_url");
            String title = intent.getStringExtra("open_title");

            if (url != null) {

                bar.setTitle(title);
                mBrowser.loadUrl(url);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        mBrowser.destroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!mStackPopup.empty()) {    // is popup
            WebView wv = mStackPopup.peek();
            wv.loadUrl("javascript:closePopup()");
            return;
        } else {
            if (mBrowser.canGoBack()) {
                mBrowser.goBack();
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {

            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initBrowser() {
        if (mBrowser != null) {
            mBrowser.getSettings().setJavaScriptEnabled(true);
            mBrowser.getSettings().setNeedInitialFocus(true);
            mBrowser.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
            mBrowser.getSettings().setSupportMultipleWindows(true);
            mBrowser.addJavascriptInterface(new JavaScriptInterface(), "androidInterface");
            mBrowser.setWebViewClient(webViewClient);
            mBrowser.setWebChromeClient(webChromeClient);
        }
    }

    private void loadUrl(String url) {
        if (!ServerUrl.UNREACHABLE_URL.equals(url)) {
            mLastUrl = url;
        }

        if (NetworkUtils.isConnected(this)) {
            loadUrl(url);
        } else {
            loadUrl(ServerUrl.UNREACHABLE_URL);
        }
    }
    private WebViewClient webViewClient = new WebViewClient() {

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler,
                                       SslError error) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PopupActivity.this);
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
            logger.debug("shouldOverrideUrlLoading(): url=" + url);
//            return super.shouldOverrideUrlLoading(view, url);
            loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            //logger.debug("onPageStarted(): url=" + url);

            if (url.contains(ServerUrl.SESSION_CLOSED_API)) {
                logger.debug("Session Closed!!! Please sign-in again!!!");
                return;
            }

            mProgressBar.setVisibility(View.VISIBLE);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
//            String text = getResources().getString(R.string.web_loading_error) + description;
//            Toast.makeText(PopupActivity.this, text, Toast.LENGTH_SHORT).show();
            super.onReceivedError(view, errorCode, description, failingUrl);
            loadUrl(ServerUrl.UNREACHABLE_URL);
        }

    };

    private JsResult mLastJsResult;

    private WebChromeClient webChromeClient = new WebChromeClient() {

        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog,
                                      boolean isUserGesture, Message resultMsg) {

            logger.debug("onCreateWindow()");

            WebView wv = new WebView(PopupActivity.this);
            wv.getSettings().setJavaScriptEnabled(true);
            wv.setWebChromeClient(this);
            wv.setWebViewClient(webViewClient);
            wv.getSettings().setSupportMultipleWindows(true);
            wv.addJavascriptInterface(new JavaScriptInterface(), "androidInterface");
            wv.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            mContainer.addView(wv);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(wv);
            resultMsg.sendToTarget();

            mStackPopup.push(wv);

            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            logger.debug("onCloseWindow(): window=" + window);

            mContainer.removeView(window);
            mStackPopup.pop();
            super.onCloseWindow(window);
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            mLastJsResult = result;

            DialogFragment fragment = JsAlertDialogFramgment.newInstance(message);
            fragment.show(getSupportFragmentManager(), JsAlertDialogFramgment.TAG);
            return true;
        }

    };

    public static class JsAlertDialogFramgment extends DialogFragment {

        public static final String TAG = "WebChromeDialogFragment";

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
                            ((PopupActivity) getActivity()).onJsConfirm();
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((PopupActivity) getActivity()).onJsCancel();
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

            runOnUiThread(new Runnable() {
                public void run() {
                    logger.debug("Session Closed!!! Please signin again!!!");
                    setResult(PopupActivity.RESULT_LOGOUT);
                    finish();
                }
            });
        }

        @JavascriptInterface
        public void onReady(final String url) {
            logger.debug("onReady() url=" + url);

            runOnUiThread(new Runnable() {
                public void run() {
                    if (url.contains("vInfoMaximizeMap")) {
                        getSupportActionBar().setTitle(R.string.tripmon_popup_route_detail);
                    } else if (url.contains("drvYtVideo")) {
                        getSupportActionBar().setTitle(R.string.tripmon_popup_route_video);
                    } else if (url.contains("vEcoChart")) {
                        getSupportActionBar().setTitle(R.string.popup_chart);
                        mBrowser.loadUrl(String.format("javascript:getUserEcoData('%s', '%s')",
                                mFormatterDateTime.format(DateUtils.LocalTimeToUTCTime(mCurrentDate)), "day"));
                    }

                    mProgressBar.setVisibility(View.INVISIBLE);
                }
            });
        }

    }

}
