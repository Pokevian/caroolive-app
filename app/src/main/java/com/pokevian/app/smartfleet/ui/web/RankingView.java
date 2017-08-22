package com.pokevian.app.smartfleet.ui.web;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.http.SslError;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.ServerUrl;

import org.apache.log4j.Logger;

/**
 * Created by ian on 2016-09-27.
 */

public class RankingView extends WebViewFragment {


    @Override
    protected WebViewClient getWebViewClient() {
        return new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Logger.getLogger(TAG).trace("shouldOverrideUrlLoading#" + url + "#" + mShouldOverrideUrlLoading);
                loadUrl(url);
                return true;
            }

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
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                Logger.getLogger(TAG).debug("errorCode#" + errorCode + "@onReceivedError#" + description);
                super.onReceivedError(view, errorCode, description, failingUrl);
                loadUrl(ServerUrl.UNREACHABLE_URL);
                mProgressLayout.setVisibility(View.INVISIBLE);

            }
        };
    }

}
