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

import com.pokevian.app.smartfleet.service.AutoConnectService;
import com.pokevian.app.smartfleet.service.DataUploadService;
import com.pokevian.app.smartfleet.service.ScreenMonitorService;
import com.pokevian.app.smartfleet.service.YoutubeUploadService;
import com.pokevian.app.smartfleet.ui.main.AutoStartManager;

import org.apache.log4j.Logger;

/**
 * The type Boot complete receiver.
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    final Logger logger = Logger.getLogger("boot-complete-receiver");

    @Override
    public void onReceive(Context context, Intent intent) {
        // Start youtube upload service to resume pending requests
//        logger.debug("# resume pending youtube upload...");
//        Intent service = new Intent(context, YoutubeUploadService.class);
//        service.putExtra(YoutubeUploadService.EXTRA_WAKED_UP, true);
//        context.startService(service);

        // Start data upload service
//        logger.debug("# resume pending data upload...");
//        service = new Intent(context, DataUploadService.class);
//        service.putExtra(DataUploadServiceOld.EXTRA_WAKED_UP, true);
//        context.startService(service);

//        AutoConnectService.setAlarm(context, 30000);
//        context.startService(new Intent(context, ScreenMonitorService.class));
        AutoStartManager.startAutoStartService(context);
        DataUploadService.setAlarm(context, 30000);
    }

}
