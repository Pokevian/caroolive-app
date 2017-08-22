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

package com.pokevian.app.smartfleet.ui.version;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.pokevian.app.smartfleet.R;

public class VersionInfoDialogFragment extends DialogFragment {

    public static final String TAG = "VersionInfoDialogFragment";

    public static VersionInfoDialogFragment newInstance() {
        VersionInfoDialogFragment fragment = new VersionInfoDialogFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        PackageInfo pkgInfo = getPackageInfo();
        if (pkgInfo != null) {
            PackageManager pm = getActivity().getPackageManager();
            final Drawable icon = pkgInfo.applicationInfo.loadIcon(pm);
            final CharSequence label = pkgInfo.applicationInfo.loadLabel(pm);
            final String versionName = pkgInfo.versionName;

            return new AlertDialog.Builder(getActivity())
                    .setIcon(icon)
                    .setTitle(label)
                    .setMessage(versionName)
                    .setPositiveButton(R.string.btn_ok, null)
                    .create();
        } else {
            return null;
        }
    }

    private PackageInfo getPackageInfo() {
        PackageInfo pkgInfo = null;
        try {
            PackageManager pm = getActivity().getPackageManager();
            pkgInfo = pm.getPackageInfo(getActivity().getPackageName(), 0);
        } catch (NameNotFoundException e) {
        }
        return pkgInfo;
    }

}
