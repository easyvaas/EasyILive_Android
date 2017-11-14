package com.easyvaas.sdk.ilivedemo;

import com.easyvaas.sdk.core.EVSdk;
import com.easyvaas.sdk.evilive.wrapper.EVRtcWorker;
import com.easyvaas.sdk.evilive.wrapper.EVWorkerThread;
import com.uuzuche.lib_zxing.ZApplication;

/**
 * Created by liya on 16/7/8.
 */
public class EVApplication extends ZApplication {
    private static final String TAG = EVApplication.class.getSimpleName();

    private final static String APP_ID = "";
    private final static String ACCESS_KEY = "";
    private final static String SECRET_KEY = "";
    private final static String USER_ID = "";

    

    private static EVApplication app;

    @Override public void onCreate() {
        super.onCreate();

        app = this;

        EVRtcWorker.initWorker(getApplicationContext(), Constant.INTERACTIVE_LIVE_APP_ID);

        EVSdk.init(getApplicationContext(), APP_ID, ACCESS_KEY, SECRET_KEY, USER_ID);
        EVSdk.enableDebugLog();
    }

    public static EVApplication getApp() {
        return app;
    }
}
