package com.pokevian.app.fingerpush;

import android.content.Context;
import android.text.TextUtils;

import com.pokevian.app.smartfleet.model.Account;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.setting.SettingsStore;

import org.apache.log4j.Logger;

import kr.co.fingerpush.android.GCMConstants;

/**
 * Created by ian on 2016-04-20.
 */
public class PushManagerHelper {
    private static final String TAG = "fingerpush";

    public static boolean setDeviceCompat(final Context context, final Account account) {
        if (isValidDevice(context)) {
            return false;
        }

        Logger.getLogger(TAG).trace(">>> setDeviceCompat");

        PushManager manager = PushManager.getInstance(context);
        manager.setDevice(new PushManager.PushManagerCallback() {
            @Override
            public void onError(String code, String errorMessage) {
                Logger.getLogger(TAG).trace("onError@setDeviceCompat#" + code + ": " + errorMessage);
            }

            @Override
            public void onComplete() {
                Logger.getLogger(TAG).trace("onComplete@setDeviceCompat");
                setIdentity(context, account.getAccountId());
                updateMemberTag(context, account);
                updateVehicleTag(context, SettingsStore.getInstance().getVehicle());
            }
        });

        return true;
    }

    public static void setDevice(final Context context) {
        if (isValidDevice(context)) {
            return;
        }

        Logger.getLogger(TAG).trace(">>> setDevice");

        PushManager manager = PushManager.getInstance(context);
        manager.setDevice(new PushManager.PushManagerCallback() {
            @Override
            public void onError(String code, String errorMessage) {
                Logger.getLogger(TAG).trace("onError@setDevice#" + code + ": " + errorMessage);
            }

            @Override
            public void onComplete() {
                Logger.getLogger(TAG).trace("onComplete@setDevice");
            }
        });
    }

    public static void checkPush(Context context, String messageIdx, String pushType) {
        if (!isValidDevice(context) || TextUtils.isEmpty(messageIdx) || TextUtils.isEmpty(pushType)) {
            Logger.getLogger(TAG).trace("" + messageIdx + "@checkPush" );
            return;
        }

        PushManager manager = PushManager.getInstance(context);
        manager.checkPush(messageIdx, pushType, new PushManager.PushManagerCallback() {
            @Override
            public void onError(String code, String errorMessage) {
                Logger.getLogger(TAG).warn("onError@checkPush#" + code + ": " + errorMessage);
            }

            @Override
            public void onComplete() {
                Logger.getLogger(TAG).trace("onComplete@checkPush");
            }
        });
    }

    private static  boolean isValidDevice(Context context) {
        Logger.getLogger(TAG).trace("" + GCMConstants.getProjectToken(context) + "@isValidDevice");
        return GCMConstants.getProjectToken(context) != null && !GCMConstants.getProjectToken(context).equals("");
    }

    private static void setDevice(final PushManager pushManager, final PushManager.PushManagerCallback callback) {
//        new Handler().post(new Runnable() {
//            @Override
//            public void run() {
//                pushManager.setDevice(callback);
//            }
//        });

        pushManager.setDevice(callback);
    }


    public static void updateVehicleTag(Context context, Vehicle vehicle) {
        if (vehicle == null || !isValidDevice(context)) {
            Logger.getLogger(TAG).trace("" + vehicle + "@updateVehicleTag");
            return;
        }

        Logger.getLogger(TAG).trace(">>> updateVehicleTag");

        final String makerTag = "MAKER_" + vehicle.getModel().getMakerCode();
        final String fuelTag = "FUEL_" + vehicle.getModel().getEngineCode();

        final PushManager manager = PushManager.getInstance(context);

        SettingsStore settingsStore = SettingsStore.getInstance();

        if (makerTag != null && !makerTag.equals(settingsStore.getVehicleMakerTag())) {
            String oldTag = settingsStore.getVehicleMakerTag();
            if (oldTag != null) {
                removeTag(manager, oldTag, new PushManager.PushManagerCallback() {
                    @Override
                    public void onError(String code, String errorMessage) {
                    }

                    @Override
                    public void onComplete() {
                        SettingsStore.getInstance().storeVehicleMakerTag(null);
                        setVehicleMakerTag(manager, makerTag);
                    }
                });

            } else {
                setVehicleMakerTag(manager, makerTag);
            }
        }

        if (fuelTag != null && !fuelTag.equals(settingsStore.getVehicleFuelTag())) {
            String oldName = settingsStore.getVehicleFuelTag();
            if (oldName != null) {
                removeTag(manager, oldName, new PushManager.PushManagerCallback() {
                    @Override
                    public void onError(String code, String errorMessage) {
                    }

                    @Override
                    public void onComplete() {
                        SettingsStore.getInstance().storeVehicleFuelTag(null);
                        setVehicleFuelTag(manager, fuelTag);
                    }
                });
            } else {
                setVehicleFuelTag(manager, fuelTag);
            }
        }
    }

    public static void updateMemberTag(Context context, Account account) {
        if (account == null || !isValidDevice(context)) {
            Logger.getLogger(TAG).trace("" + account + "@updateMemberTag" );
            return;
        }
        Logger.getLogger(TAG).trace(">>> updateMemberTag");

        SettingsStore settingsStore = SettingsStore.getInstance();
        final String sexTag = "SEX_" + account.getSexCode();
        final String regionTag = "REGION_" + account.getRegionCode();

        final PushManager manager = PushManager.getInstance(context);
        if (sexTag != null && !sexTag.equals(settingsStore.getAccountSexTag())) {
            String oldTag = settingsStore.getAccountSexTag();
            if (oldTag != null) {
                removeTag(manager, oldTag, new PushManager.PushManagerCallback() {
                    @Override
                    public void onError(String code, String errorMessage) {
                    }

                    @Override
                    public void onComplete() {
                        SettingsStore.getInstance().storeAccountSexTag(null);
                        setAccountSexTag(manager, sexTag);
                    }
                });
            } else {
                setAccountSexTag(manager, sexTag);
            }
        }

        if (regionTag != null && !regionTag.equals(settingsStore.getAccountRegionTag())) {
            String oldTag = settingsStore.getAccountRegionTag();
            if (oldTag != null) {
                removeTag(manager, oldTag, new PushManager.PushManagerCallback() {
                    @Override
                    public void onError(String code, String errorMessage) {
                    }

                    @Override
                    public void onComplete() {
                        SettingsStore.getInstance().storeAccountRegionTag(null);
                        setAccountRegionTag(manager, regionTag);
                    }
                });
            } else {
                setAccountRegionTag(manager, regionTag);
            }
        }
    }

    public static void setIdentity(Context context, final String identity) {
        if (identity == null && !isValidDevice(context)) {
            Logger.getLogger(TAG).trace("" + identity + "@setIdentity" );
            return;
        }

        Logger.getLogger(TAG).trace(">>> setIdentity#" + identity);
        if (!identity.equals(SettingsStore.getInstance().getIdentity())) {
            // removeIdentity

            PushManager manager = PushManager.getInstance(context);
            manager.setIdentity(identity, new PushManager.PushManagerCallback() {
                @Override
                public void onError(String code, String errorMessage) {
                    Logger.getLogger(TAG).warn("onError@setIdentity#" + code + ": " + errorMessage);
                }

                @Override
                public void onComplete() {
                    Logger.getLogger(TAG).trace("onComplete@setIdentity");
                    SettingsStore.getInstance().storeIdentity(identity);
                }
            });
        }

    }

    private static void setVehicleMakerTag(final PushManager pushManager, final String makerTag) {

        setTag(pushManager, makerTag, new PushManager.PushManagerCallback() {
            @Override
            public void onError(String code, String errorMessage) {
            }

            @Override
            public void onComplete() {
                Logger.getLogger(TAG).trace("onComplete@setVehicleMakerTag#" + makerTag);
                SettingsStore.getInstance().storeVehicleMakerTag(makerTag);
            }
        });
    }

    private static void setVehicleFuelTag(final PushManager pushManager, final String fuelTag) {
        setTag(pushManager, fuelTag, new PushManager.PushManagerCallback() {
            @Override
            public void onError(String code, String errorMessage) {
            }

            @Override
            public void onComplete() {
                Logger.getLogger(TAG).trace("onComplete@setVehicleFuelTag#" + fuelTag);
                SettingsStore.getInstance().storeVehicleFuelTag(fuelTag);
            }
        });
    }

    private static void setAccountSexTag(final PushManager pushManager, final String sexTag) {
        setTag(pushManager, sexTag, new PushManager.PushManagerCallback() {
            @Override
            public void onError(String code, String errorMessage) {
            }

            @Override
            public void onComplete() {
                Logger.getLogger(TAG).trace("onComplete@setAccountSexTag#" + sexTag);
                SettingsStore.getInstance().storeAccountSexTag(sexTag);
            }
        });
    }

    private static void setAccountRegionTag(final PushManager pushManager, final String regionTag) {
        setTag(pushManager, regionTag, new PushManager.PushManagerCallback() {
            @Override
            public void onError(String code, String errorMessage) {
            }

            @Override
            public void onComplete() {
                Logger.getLogger(TAG).trace("onComplete@setAccountRegionTag#" + regionTag);
                SettingsStore.getInstance().storeAccountRegionTag(regionTag);
            }
        });
    }

    private static void removeTag(final PushManager pushManager, final String tag, final PushManager.PushManagerCallback callback) {
        Logger.getLogger(TAG).trace("removeTag#" + tag);
//        new Handler().post(new Runnable() {
//            @Override
//            public void run() {
//                pushManager.removeTag(tag, new PushManager.PushManagerCallback() {
//                    @Override
//                    public void onError(String code, String errorMessage) {
//                        Logger.getLogger(TAG).warn("onError@removeTag#" + code + ": " + errorMessage);
//                    }
//
//                    @Override
//                    public void onComplete() {
//                        Logger.getLogger(TAG).trace("onComplete@removeTag#" + tag);
//                        if (callback != null) {
//                            callback.onComplete();
//                        }
//                    }
//                });
//            }
//        });

        pushManager.removeTag(tag, new PushManager.PushManagerCallback() {
            @Override
            public void onError(String code, String errorMessage) {
                Logger.getLogger(TAG).warn("onError@removeTag#" + code + ": " + errorMessage);
            }

            @Override
            public void onComplete() {
                Logger.getLogger(TAG).trace("onComplete@removeTag#" + tag);
                if (callback != null) {
                    callback.onComplete();
                }
            }
        });
    }

    private static void setTag(final PushManager pushManager, final String tag, final PushManager.PushManagerCallback callback) {
        Logger.getLogger(TAG).trace("setTag#" + tag);
//        new Handler().post(new Runnable() {
//            @Override
//            public void run() {
//                pushManager.setTag(tag, callback);
//            }
//        });

        pushManager.setTag(tag, callback);
    }


}
