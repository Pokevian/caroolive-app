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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;

public class VideoCopyProgressDialog {

    private final Context mContext;
    private CharSequence mTitle;
    private CharSequence mMessage;
    private DialogInterface.OnCancelListener mOnCancelListener;
    private DialogInterface.OnDismissListener mOnDismissListener;
    private ProgressDialog mDialog;

    private int mMaxValue;

    public VideoCopyProgressDialog(Context context) {
        mContext = context;
    }

    public VideoCopyProgressDialog setTitle(int resId) {
        return setTitle(mContext.getString(resId));
    }

    public VideoCopyProgressDialog setTitle(CharSequence title) {
        mTitle = title;
        return this;
    }

    public VideoCopyProgressDialog setMessage(int resId) {
        return setMessage(mContext.getString(resId));
    }

    public VideoCopyProgressDialog setMessage(CharSequence message) {
        mMessage = message;
        return this;
    }

    public VideoCopyProgressDialog setMax(int max) {
        mMaxValue = max;
        return this;
    }

    public VideoCopyProgressDialog setOnCancelListener(DialogInterface.OnCancelListener listener) {
        mOnCancelListener = listener;
        return this;
    }

    public VideoCopyProgressDialog setOnDismissListener(DialogInterface.OnDismissListener listener) {
        mOnDismissListener = listener;
        return this;
    }

    public void dismiss() {
        if (mDialog != null) {
            try {
                mDialog.dismiss();
            } catch (Exception e) {
            }
        }
    }

    public ProgressDialog show() {
        mDialog = new ProgressDialog(mContext);
        mDialog.setCancelable(true);
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        if (!TextUtils.isEmpty(mTitle)) {
            mDialog.setTitle(mTitle);
        }
        if (!TextUtils.isEmpty(mMessage)) {
            mDialog.setMessage(mMessage);
        }
        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (mOnCancelListener != null) {
                    mOnCancelListener.onCancel(dialog);
                }
            }
        });
        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                if (mOnDismissListener != null) {
                    mOnDismissListener.onDismiss(dialog);
                }
            }
        });
        mDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, mContext.getResources().getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (mOnCancelListener != null) {
                            mOnCancelListener.onCancel(dialog);
                        }
                    }
                });
        mDialog.setMax(mMaxValue);
        mDialog.setProgress(0);
        mDialog.show();

        return mDialog;
    }


    public VideoCopyProgressDialog setProgress(int value) {
        mDialog.setProgress(value);
        return this;
    }

}
