package com.pokevian.app.smartfleet.ui.web;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.util.NetworkUtils;
import com.pokevian.app.smartfleet.util.TelephonyUtils;

import org.apache.log4j.Logger;

/**
 * Created by ian on 2016-09-27.
 */

public class WebViewFragment extends Fragment {
    public static final String TAG = "web-view";

    interface OnSessionCloseListener {
        public void onSessionClosed();
    }

    private WebView mWebView;
    private String mLastUrl;
    protected LinearLayout mProgressLayout;
    protected boolean mShouldOverrideUrlLoading = false;
    private OnSessionCloseListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (OnSessionCloseListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement OnSessionCloseListener");
        }

        /*if (mListener == null) {
            try {
                mListener = (OnSessionCloseListener) getParentFragment();
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement OnSessionCloseListener");
            }
        }*/
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_webview, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mWebView = (WebView) view.findViewById(R.id.web_view);
        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    String url = mWebView.getUrl();
                    if (ServerUrl.UNREACHABLE_URL.equals(url)) {
                        loadUrl(mLastUrl);
                    }
                }
                return false;
            }
        });
        mProgressLayout = (LinearLayout) view.findViewById(R.id.progress_layout);

//        initBrowser();
        if (mWebView != null) {
            init(mWebView);
        }
    }

    @Override
    public void onDestroy() {
        mWebView.destroy();
        super.onDestroy();
    }

    public boolean canGoBack() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return false;
    }

    public WebView getWebView() {
        return mWebView;
    }

    protected void showProgress() {
        mProgressLayout.setVisibility(View.VISIBLE);
    }

    protected void hideProgress() {
        mProgressLayout.setVisibility(View.INVISIBLE);
    }

    /*@SuppressLint("SetJavaScriptEnabled")
    private void initBrowser() {
        if (mWebView != null) {
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            mWebView.getSettings().setSupportMultipleWindows(false);

            mWebView.getSettings().setNeedInitialFocus(true);
            mWebView.getSettings().setUseWideViewPort(true);

            mWebView.addJavascriptInterface(new JavaScriptInterface(), "androidInterface");
            mWebView.getSettings().setTextZoom(100);
//            mWebView.getSettings().setTextSize(WebSettings.TextSize.NORMAL);
            mWebView.setWebViewClient(newWebViewClient());
            mWebView.setWebChromeClient(newWebChromeClient());
        }
    }*/

    @SuppressLint("SetJavaScriptEnabled")
    protected void init(WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(false);

        webView.getSettings().setNeedInitialFocus(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.addJavascriptInterface(new JavaScriptInterface(), "androidInterface");
        webView.getSettings().setTextZoom(100);
//            mWebView.getSettings().setTextSize(WebSettings.TextSize.NORMAL);
        webView.setWebViewClient(getWebViewClient());
        webView.setWebChromeClient(getWebChromeClient());
    }

    protected WebViewClient getWebViewClient() {
        return newWebViewClient();
    }

    protected WebChromeClient getWebChromeClient() {
        return newWebChromeClient();
    }

    public void loadUrl(String url) {
        Logger.getLogger(TAG).debug("loadUrl#" + mLastUrl + " > " + url);
        if (!ServerUrl.UNREACHABLE_URL.equals(url)) {
            mLastUrl = url;
        }
        if (NetworkUtils.isConnected(getActivity())) {
            mWebView.loadUrl(url);
            showProgress();
        } else {
            mWebView.loadUrl(ServerUrl.UNREACHABLE_URL);

//            DialogFragment dialog = (DialogFragment) getActivity().getSupportFragmentManager().findFragmentByTag("network_error_dialog");
//            if (dialog == null) {
//                dialog = new NetworkErrorDialogFragment();
//                dialog.show(getActivity().getSupportFragmentManager(), "network_error_dialog");
//            }
        }
    }

    private WebViewClient newWebViewClient() {
        return new WebViewClient() {

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
                Logger.getLogger(TAG).info("shouldOverrideUrlLoading#" + url + "#" + mShouldOverrideUrlLoading);
//                if (!NetworkUtils.isConnected(getActivity())) {
//                    DialogFragment dialog = (DialogFragment) getActivity().getSupportFragmentManager().findFragmentByTag("network_error_dialog");
//                    if (dialog == null) {
//                        dialog = new NetworkErrorDialogFragment();
//                        dialog.show(getActivity().getSupportFragmentManager(), "network_error_dialog");
//                    }
//                }

                if (mShouldOverrideUrlLoading) {
                    if (url.contains("web/event") || url.contains("view.do?cmd=vObdPurchase")) {
                        if (url.contains("view.do?cmd=vObdPurchase")) {
                            ((WebViewActivity) getActivity()).getSupportActionBar().setTitle("OBD 구매");
                        }
                        loadUrl(url);
                        return true;
                    }

                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(uri);
                    view.getContext().startActivity(intent);
                } else {
                    loadUrl(url);
                    return true;
                }

                return mShouldOverrideUrlLoading;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Logger.getLogger(TAG).trace("onPageStarted#" + url);
                super.onPageStarted(view, url, favicon);
                mProgressLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Logger.getLogger(TAG).trace("onPageFinished#" + url);
                super.onPageFinished(view, url);
                mProgressLayout.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                Logger.getLogger(TAG).debug("errorCode#" + errorCode + "@onReceivedError#" + description);
                super.onReceivedError(view, errorCode, description, failingUrl);
                loadUrl(ServerUrl.UNREACHABLE_URL);
                mProgressLayout.setVisibility(View.INVISIBLE);

            }
        };
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

    private void popupPointUse(String number) {
        mWebView.loadUrl("javascript:popupPointUse('" + number + "')");
    }

    private void onDialogConfirm() {
        mWebView.loadUrl("javascript:onConfirm()");
    }

    private void onDialogCancel() {
        mWebView.loadUrl("javascript:onCancel()");
    }

    protected class JavaScriptInterface {
        @JavascriptInterface
        public void sessionClosed() {
            Logger.getLogger(TAG).debug("seesionClosed");
            mListener.onSessionClosed();
        }

        @JavascriptInterface
        public void onInputPhoneNumber(final String title, final String point) {
            DialogFragment fragment = PhoneNumberInputDialog.newInstance(title, point);
            fragment.show(getChildFragmentManager(), PhoneNumberInputDialog.TAG);
        }

        @JavascriptInterface
        public void onDialog(final String title, final String message, boolean isConfirm) {
            DialogFragment fragment = JsDialogFragment.newInstance(title, message, isConfirm);
            fragment.show(getChildFragmentManager(), JsDialogFragment.TAG);
        }

        @JavascriptInterface
        public void onReady(final String url) {
            Logger.getLogger(TAG).debug("onReady#" + url);

            getActivity().runOnUiThread(new Runnable() {
                public void run() {

//                    try {
//                        mWebView.clearHistory();
//
//                    } catch (Exception e) {
//                    }

                    showProgress();
                }
            });
        }

        @JavascriptInterface
        public void onStart(final String url) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showProgress();
                }
            });
        }

        @JavascriptInterface
        public void onComplete(final String url) {
            Logger.getLogger(TAG).debug("onComplete#" + url);
            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    hideProgress();
                }
            });
        }

    }

    private JsResult mLastJsResult;
    protected WebChromeClient newWebChromeClient() {
        return new WebChromeClient() {

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                mLastJsResult = result;

                DialogFragment fragment = WebChromeDialogFragment.newInstance("알림", message, false);
                fragment.show(getChildFragmentManager(), WebChromeDialogFragment.TAG);

                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                mLastJsResult = result;

                DialogFragment fragment = WebChromeDialogFragment.newInstance("확인", message, true);
                fragment.show(getChildFragmentManager(), WebChromeDialogFragment.TAG);

                return true;
            }
        };
    }

    public static class WebChromeDialogFragment extends DialogFragment {

        public static final String TAG = "WebChromeDialogFragment";

        protected String mTitle;
        protected String mMessage;
        protected Boolean mIsConfirm;

        public static WebChromeDialogFragment newInstance(String title, String message, boolean isConfirm) {
            WebChromeDialogFragment fragment = new WebChromeDialogFragment();
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putString("message", message);
            args.putBoolean("is-confirm", isConfirm);
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mTitle = args.getString("title");
                mMessage = args.getString("message");
                mIsConfirm = args.getBoolean("is-confirm");
            } else {
                mTitle = savedInstanceState.getString("title");
                mMessage = savedInstanceState.getString("message");
                mIsConfirm = savedInstanceState.getBoolean("is-confirm");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("title", mTitle);
            outState.putString("message", mMessage);
            outState.putBoolean("is-confirm", mIsConfirm);
            super.onSaveInstanceState(outState);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(mTitle)
                    .setMessage(mMessage)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            onDialogConfirm();
                        }
                    });

            if (mIsConfirm) {
                builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onDialogCancel();
                    }
                });
            }
            Dialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);

            return dialog;
        }

        protected void onDialogConfirm() {
            ((WebViewFragment) getParentFragment()).onJsConfirm();
        }

        protected void onDialogCancel() {
            ((WebViewFragment) getParentFragment()).onJsCancel();
        }
    }

    public static class PhoneNumberInputDialog extends DialogFragment {

        public static final String TAG = "PhoneNumberInputDialog";

        private String mTitle;
        private String mPoint;
        private EditText mPhoneNumber;

        public static PhoneNumberInputDialog newInstance(String title, String point) {
            PhoneNumberInputDialog fragment = new PhoneNumberInputDialog();
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putString("point", point);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mTitle = args.getString("title");
                mPoint = args.getString("point");
            } else {
                mTitle = savedInstanceState.getString("title");
                mPoint = savedInstanceState.getString("point");
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            setEnabledButton();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("title", mTitle);
            outState.putString("point", mPoint);
            super.onSaveInstanceState(outState);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_input_phone_number, null);
            ((TextView) view.findViewById(R.id.title)).setText(mTitle);
            ((TextView) view.findViewById(R.id.point)).setText(mPoint);

            mPhoneNumber = (EditText) view.findViewById(R.id.edit);
            mPhoneNumber.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    setEnabledButton();
                }
            });

            mPhoneNumber.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

            String phoneNumber = TelephonyUtils.getPhoneNumber(getActivity());
            if (!TextUtils.isEmpty(phoneNumber)) {
                phoneNumber = TelephonyUtils.formatPhoneNumber(phoneNumber);
            }
            mPhoneNumber.setText(phoneNumber);
            mPhoneNumber.setSelection(mPhoneNumber.length());

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.dialog_title_buy))
                    .setView(view)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            DialogFragment fragment = PhoneNumberConfirmDialog.newInstance(mTitle, mPoint, mPhoneNumber.getText().toString());
                            fragment.show(getFragmentManager(), PhoneNumberConfirmDialog.TAG);
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            Dialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        private void setEnabledButton(boolean enabled) {
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enabled);
        }

        private void setEnabledButton() {
            try {
                setEnabledButton(isCellPhone(mPhoneNumber.getText().toString()));
            } catch (NullPointerException e) {

            }
        }

        private boolean isCellPhone(String number) {
            if (TextUtils.isEmpty(number)) {
                return false;
            }
            String str = TelephonyUtils.normalizePhoneNumber(number);
//                return number.matches("(01[016789])-(\\d{3,4})-(\\d{4})");
            return str.matches("(01[016789])(\\d{3,4})(\\d{4})");
        }
    }

    public static class PhoneNumberConfirmDialog extends DialogFragment {

        public static final String TAG = "PhoneNumberConfirmDialog";

        private String mTitle;
        private String mPoint;
        private String mPhoneNumber;

        public static PhoneNumberConfirmDialog newInstance(String title, String point, String number) {
            PhoneNumberConfirmDialog fragment = new PhoneNumberConfirmDialog();
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putString("point", point);
            args.putString("phone-number", number);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mTitle = args.getString("title");
                mPoint = args.getString("point");
                mPhoneNumber = args.getString("phone-number");
            } else {
                mTitle = savedInstanceState.getString("title");
                mPoint = savedInstanceState.getString("point");
                mPhoneNumber = savedInstanceState.getString("phone-number");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("title", mTitle);
            outState.putString("point", mPoint);
            outState.putString("phone-number", mPhoneNumber);
            super.onSaveInstanceState(outState);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_confirm_phone_number, null);
            ((TextView) view.findViewById(R.id.title)).setText(mTitle);
            ((TextView) view.findViewById(R.id.point)).setText(mPoint);
            ((TextView) view.findViewById(R.id.phone_number)).setText(mPhoneNumber);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.dialog_title_buy_confirm))
                    .setView(view)
                    .setPositiveButton(R.string.btn_buy, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((WebViewFragment) getParentFragment()).popupPointUse(mPhoneNumber);
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

            Dialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);

            return dialog;
        }
    }

    public static class JsDialogFragment extends WebChromeDialogFragment {
        public static final String TAG = "JsDialogFragment";

        public static JsDialogFragment newInstance(String title, String message, boolean isConfirm) {
            JsDialogFragment fragment = new JsDialogFragment();
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putString("message", message);
            args.putBoolean("is-confirm", isConfirm);
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        protected void onDialogConfirm() {
            ((WebViewFragment) getParentFragment()).onDialogConfirm();
        }

        @Override
        protected void onDialogCancel() {
            ((WebViewFragment) getParentFragment()).onDialogCancel();
        }
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
}
