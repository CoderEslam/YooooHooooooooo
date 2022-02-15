package com.doubleclick.yoohooappmaster;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.doubleclick.yoohooappmaster.receivers.ConnectivityReceiver;
import com.onesignal.OneSignal;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;



public class BaseApplication extends Application {

    private static final String ONESIGNAL_APP_ID = "6c07ca3e-d3f6-46bc-b4ea-01f3ba9338bb";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        // OneSignal Initialization
//        OneSignal.initWithContext(this);
//        OneSignal.setAppId(ONESIGNAL_APP_ID);

        OneSignal.provideUserConsent(true);
        OneSignal.startInit(this)
                .setNotificationOpenedHandler(new MyOnesignalNotificationOpenedHandler(this))
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .init();
        ConnectivityReceiver.init(this);
        EmojiManager.install(new GoogleEmojiProvider());

//        String admobAppId = getString(R.string.admob_app_id);
//        if (!TextUtils.isEmpty(admobAppId))
//            MobileAds.initialize(this, admobAppId);
    }
}
