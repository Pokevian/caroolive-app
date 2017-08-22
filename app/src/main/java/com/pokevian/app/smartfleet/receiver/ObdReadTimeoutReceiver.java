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

package com.pokevian.app.smartfleet.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.pokevian.app.smartfleet.BuildConfig;

import org.apache.log4j.Logger;

/**
 * The type Obd read timeout receiver.
 */
public class ObdReadTimeoutReceiver extends BroadcastReceiver {

    final Logger logger = Logger.getLogger("ObdReadTimeoutReceiver");

    public static final String ACTION_OBD_EXCEPTION = "com.pokevian.lib.obd2.action.IO_EXCEPTION";
    public static final String EXTRA_OBD_EXCEPTION = "com.pokevian.lib.obd2.extra.EXCEPTION";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (BuildConfig.DEBUG) {
            String action = intent.getAction();

            if (ACTION_OBD_EXCEPTION.equals(action)) {
                Exception ex = (Exception) intent.getSerializableExtra(EXTRA_OBD_EXCEPTION);
                String text = "OBD IO Exception!\n- message=" + ex.getMessage();
                logger.warn(text);

                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            }
        }
    }

}
