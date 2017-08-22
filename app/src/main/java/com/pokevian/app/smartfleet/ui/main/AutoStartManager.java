package com.pokevian.app.smartfleet.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.pokevian.app.smartfleet.BuildConfig;
import com.pokevian.app.smartfleet.service.AutoConnectService;
import com.pokevian.app.smartfleet.service.ImpactDetector;
import com.pokevian.app.smartfleet.service.ScreenMonitorService;
import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.app.smartfleet.setting.SettingsStore;

/**
 * Created by ian on 2016-12-16.
 */

public class AutoStartManager {

    private static final int AUTO_START_DISABLE = 0x00;
    private static final int AUTO_START_MODE_HIGH = 0x01;
    private static final int AUTO_START_MODE_NORMAL = 0x02;
    private static final int AUTO_START_MODE_POWER_SAVE = 0X04;

    public static int getAutoStartMode(String mode) {
        if ("HIGH".equals(mode)) {
            return AUTO_START_MODE_HIGH;
        } else if ("NORMAL".equals(mode)) {
            return AUTO_START_MODE_NORMAL;
        } else if ("LOW".equals(mode)) {
            return AUTO_START_MODE_POWER_SAVE;
        }

        return AUTO_START_DISABLE;
    }

    public static boolean isPowerSaveMode() {
        return "LOW".equals(SettingsStore.getInstance().getAutoStartMode());
    }

    public static void startAutoStartService(Context context) {
        SettingsStore settingsStore = SettingsStore.getInstance();
        if (settingsStore.isValidAccount() && settingsStore.isValidVehicle()
                && !settingsStore.isBlackboxEnabled()) {
            int mode = getAutoStartMode(settingsStore.getAutoStartMode());
            switch(mode) {
                case AUTO_START_MODE_HIGH:
                    context.startService(new Intent(context, ScreenMonitorService.class));
                    break;
                case AUTO_START_MODE_NORMAL:
                case AUTO_START_MODE_POWER_SAVE:
                    AutoConnectService.setAlarm(context, Consts.AUTO_CONNECT_WAKEUP_DELAY);
                    break;
            }
        }
    }

    public static void stopAutoStartService(Context context) {
//        int mode = getAutoStartMode();
//        if (AUTO_START_MODE_NORMAL == mode) {
//            AutoConnectService.cancelAlarm(context);
//        } else if (AUTO_START_MODE_POWER_SAVE == mode) {
//            context.stopService(new Intent(context, ScreenMonitorService.class));
//        }

        AutoConnectService.cancelAlarm(context);
        context.stopService(new Intent(context, ScreenMonitorService.class));
    }
}
