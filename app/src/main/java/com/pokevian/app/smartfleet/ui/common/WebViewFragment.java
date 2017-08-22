package com.pokevian.app.smartfleet.ui.common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;


import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.util.NetworkUtils;

import org.apache.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class WebViewFragment extends Fragment {

    public static final String TAG = "BaseWebViewFragment";
    public static final String EXTRA_URL = "extra.URL";
    public static final String EXTRA_INTERFACE = "extra.INTERFACE";

    private WebView mBrowser;
    private String mUrl;
    private Boolean mInterfaceEnabled;
    private LinearLayout mProgressLayout;
    private OnSessionClosedListener mListener;

    public interface OnSessionClosedListener {
        public void sessionCloased();
    }

    public static WebViewFragment newInstance(String url, boolean interfaceEnabled) {
        WebViewFragment instance = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_URL, url);
        args.putBoolean(EXTRA_INTERFACE, interfaceEnabled);
        instance.setArguments(args);
        return instance;
    }

    public static WebViewFragment newInstance(String url) {
        return newInstance(url, false);
    }

//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
////        try {
////            mListener = (OnSessionClosedListener) activity;
////        } catch (ClassCastException e) {
////        }
//
//        mListener = (OnSessionClosedListener) activity;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mUrl = args.getString(EXTRA_URL);
            mInterfaceEnabled = args.getBoolean(EXTRA_INTERFACE);
        } else {
            mUrl = savedInstanceState.getString(EXTRA_URL);
            mInterfaceEnabled = savedInstanceState.getBoolean(EXTRA_INTERFACE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_URL, mUrl);
        outState.putBoolean(EXTRA_INTERFACE, mInterfaceEnabled);
    }

    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_webview, container, false);
        }

        ViewGroup parent = (ViewGroup) rootView.getParent();
        if (parent != null) {
            parent.removeView(rootView);

            return rootView;
        }

        mBrowser = (WebView) rootView.findViewById(R.id.web_view);
        mBrowser.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    String url = mBrowser.getUrl();
                    if (ServerUrl.UNREACHABLE_URL.equals(url)) {
                        Logger.getLogger(TAG).debug("# reload url=" + mLastUrl);
                        loadUrl(mLastUrl);
                    }
                }
                return false;
            }
        });
        mProgressLayout = (LinearLayout) rootView.findViewById(R.id.progress_layout);
        mProgressLayout.setVisibility(View.VISIBLE);

        initBrowser();

        loadUrl(mUrl);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        cancelLoadingTimer();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mBrowser.destroy();
        super.onDestroy();
    }

    public boolean canGoBack() {
        if (mBrowser.canGoBack()) {
            mBrowser.goBack();
            return true;
        }
        return false;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initBrowser() {
        if (mBrowser != null) {
//            mBrowser.setBackgroundColor(getResources().getColor(R.color.main_bg));
            mBrowser.getSettings().setJavaScriptEnabled(true);
            mBrowser.getSettings().setNeedInitialFocus(true);
            mBrowser.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
            mBrowser.getSettings().setSupportMultipleWindows(false);
            mBrowser.addJavascriptInterface(new JavaScriptInterface(), "androidInterface");
            mBrowser.getSettings().setTextZoom(100);
//            mBrowser.getSettings().setTextSize(WebSettings.TextSize.NORMAL);
            mBrowser.setWebViewClient(mWebViewClient);
            mBrowser.setWebChromeClient(webChromeClient);
        }
    }

    private Timer mLoadingTimer;
    private TimerTask mLoadingTimerTask;
    private String mLastUrl;
    private static final long LOADING_TIMEOUT = 20000;

    private void startLoadingTimer(String url) {
        if (mLoadingTimer == null) {
            Logger.getLogger(TAG).debug("# start loading timer: url=" + url);
            mLastUrl = url;

            mLoadingTimer = new Timer();
            mLoadingTimerTask = new TimerTask() {
                public void run() {
                    Logger.getLogger(TAG).debug("# url loading timed out: url=" + mLastUrl);

                    if (!isAdded() || isRemoving() || isDetached()) {
                        return;
                    }

                    mLoadingTimerTask = null;
                    mLoadingTimer = null;

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                mProgressLayout.setVisibility(View.INVISIBLE);

                                mBrowser.stopLoading();
                                mBrowser.loadUrl(ServerUrl.UNREACHABLE_URL);
                            }
                        });
                    }
                }
            };
            mLoadingTimer.schedule(mLoadingTimerTask, LOADING_TIMEOUT);
            mProgressLayout.setVisibility(View.VISIBLE);
        }
    }

    private void cancelLoadingTimer() {
        if (mLoadingTimer != null) {
            Logger.getLogger(TAG).debug("# cancel loading timer");

            mLoadingTimer.cancel();
            mLoadingTimerTask.cancel();

            mLoadingTimerTask = null;
            mLoadingTimer = null;

            mProgressLayout.setVisibility(View.INVISIBLE);
        }
    }

    private void loadUrl(String url) {
        cancelLoadingTimer();

        if (NetworkUtils.isConnected(getActivity())) {
            mBrowser.loadUrl(url);
        } else {
            mBrowser.loadUrl(ServerUrl.UNREACHABLE_URL);
            DialogFragment dialog = (DialogFragment) getActivity().getSupportFragmentManager().findFragmentByTag("network_error_dialog");
            if (dialog == null) {
                dialog = new NetworkErrorDialogFragment();
                dialog.show(getActivity().getSupportFragmentManager(), "network_error_dialog");
            }
        }

        mProgressLayout.setVisibility(View.VISIBLE);

        startLoadingTimer(url);
    }

    public static class NetworkErrorDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.notice)
                    .setMessage(R.string.network_disconnect)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .create();
        }
    }

    private final WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
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
            Logger.getLogger(TAG).debug("shouldOverrideUrlLoading() url:" + url);
            if (!NetworkUtils.isConnected(getActivity())) {
                DialogFragment dialog = (DialogFragment) getActivity().getSupportFragmentManager().findFragmentByTag("network_error_dialog");
                if (dialog == null) {
                    dialog = new NetworkErrorDialogFragment();
                    dialog.show(getActivity().getSupportFragmentManager(), "network_error_dialog");
                }
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            startLoadingTimer(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Logger.getLogger(TAG).debug("# onPageFinished(): url=" + url);
            super.onPageFinished(view, url);

            if (!mInterfaceEnabled) {
                cancelLoadingTimer();
            }

            if (ServerUrl.UNREACHABLE_URL.equals(url)) {
                cancelLoadingTimer();
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            Logger.getLogger(TAG).debug("# onReceivedError(): errorCode=" + errorCode);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    };

    private JsResult mLastJsResult;

    private final WebChromeClient webChromeClient = new WebChromeClient() {
        @Override
        public boolean onJsConfirm(WebView view, final String url, String message, JsResult result) {
            if (isDetached() || isRemoving() || !isAdded()) {
                return false;
            }

            mLastJsResult = result;

            DialogFragment fragment = JsDialogFragment.newInstance(true, message);
            fragment.show(getChildFragmentManager(), "js-dialog");

            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, final String url, String message, JsResult result) {
            if (isDetached() || isRemoving() || !isAdded()) {
                return false;
            }

            mLastJsResult = result;

            DialogFragment fragment = JsDialogFragment.newInstance(false, message);
            fragment.show(getChildFragmentManager(), "js-dialog");

            return true;
        }
    };

    public void cancelJs() {
        if (mLastJsResult != null) {
            mLastJsResult.cancel();
            mLastJsResult = null;
        }
    }

    public void confirmJs() {
        if (mLastJsResult != null) {
            mLastJsResult.confirm();
            mLastJsResult = null;
        }
    }

    public static class JsDialogFragment extends DialogFragment {

        private boolean mConfirm;
        private String mMessage;

        public static JsDialogFragment newInstance(boolean confirm, String message) {
            JsDialogFragment fragment = new JsDialogFragment();
            Bundle args = new Bundle();
            args.putBoolean("confirm", confirm);
            args.putString("message", message);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mConfirm = args.getBoolean("confirm");
                mMessage = args.getString("message");
            } else {
                mConfirm = savedInstanceState.getBoolean("confirm");
                mMessage = savedInstanceState.getString("message");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putBoolean("confirm", mConfirm);
            outState.putString("message", mMessage);
            super.onSaveInstanceState(outState);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.app_name)
                    .setMessage(mMessage)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((WebViewFragment) getParentFragment()).confirmJs();
                        }
                    });
            if (mConfirm) {
                builder
                        .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ((WebViewFragment) getParentFragment()).cancelJs();
                            }
                        });
            }
            return builder.create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            ((WebViewFragment) getParentFragment()).cancelJs();
        }
    }

    public class JavaScriptInterface {
        @JavascriptInterface
        public void selectDate(final String date) {
            Logger.getLogger(TAG).debug("selectDate(): date=" + date);
        }

        @JavascriptInterface
        public void sessionClosed() {
            Logger.getLogger(TAG).debug("sessionClosed()");

            if (isDetached() || isRemoving() || !isAdded()) {
                return;
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListener.sessionCloased();
                }
            });
        }

        @JavascriptInterface
        public void onReady(final String url) {
            Logger.getLogger(TAG).debug("onReady() url=" + url);
        }

        @JavascriptInterface
        public void onDataReady(String from, String to) {
            Logger.getLogger(TAG).debug("onDataReady()");
        }

        @JavascriptInterface
        public void onUserEcoDataFinished() {
            Logger.getLogger(TAG).debug("onUserEcoDataFinished()");

            if (isDetached() || isRemoving() || !isAdded()) {
                return;
            }

            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    cancelLoadingTimer();
                }
            });
        }

        @JavascriptInterface
        public void onConfirm() {
            Logger.getLogger(TAG).debug("onConfirm()");
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();

                }
            });
        }
    }
}