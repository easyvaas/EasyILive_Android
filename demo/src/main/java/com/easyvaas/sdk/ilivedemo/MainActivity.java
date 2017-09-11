package com.easyvaas.sdk.ilivedemo;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.easyvaas.sdk.core.EVSdk;
import com.easyvaas.sdk.evilive.wrapper.RtcConstants;
import com.easyvaas.sdk.ilivedemo.BuildConfig;
import com.easyvaas.sdk.ilivedemo.R;
import com.easyvaas.sdk.ilivedemo.bean.RtcOption;
import com.easyvaas.sdk.ilivedemo.utils.SingleToast;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;

public class MainActivity extends ActionBarActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_OPEN_SETTING = 1;
    private static final int REQUEST_SCAN_QRCODE = 2;
    private static final int MENU_SCAN = 10;

    private EditText channelIDEt;
    private EditText userIdEt;

    private Button liveGuestBtn;
    private Button masterBtn;
    private Button guestBtn;

    private TextView curVersionTv;

    private RtcOption rtcOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setTitle("");

        channelIDEt = (EditText)findViewById(R.id.channel_id);
        userIdEt = (EditText)findViewById(R.id.user_id);

        liveGuestBtn = (Button)findViewById(R.id.join_channel_btn);
        liveGuestBtn.setOnClickListener(mOnClickListener);
        masterBtn = (Button)findViewById(R.id.create_channel_btn);
        masterBtn.setOnClickListener(mOnClickListener);
        guestBtn = (Button)findViewById(R.id.watch_channel_btn);
        guestBtn.setOnClickListener(mOnClickListener);

        curVersionTv = (TextView)findViewById(R.id.current_version_tv);
        curVersionTv.setText("DEMO版本: V" + BuildConfig.VERSION_NAME);

        rtcOption = new RtcOption();
        rtcOption.setLive(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem item = menu.add(0, MENU_SCAN, 0, R.string.scan_qr_code);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setOnMenuItemClickListener(listener);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                showSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        EVSdk.destroy();
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            int rtcRole = RtcConstants.RTC_ROLE_MASTER;
            String channelID = "";
            String strUid = "";
            long uid = 0L;
            switch (v.getId()) {
                case R.id.join_channel_btn:
                    if (!TextUtils.isEmpty(channelIDEt.getText().toString())) {
                        channelID = channelIDEt.getText().toString();
                    } else {
                        SingleToast.show(getApplicationContext(), R.string.msg_channel_is_empty);
                        return;
                    }

                    strUid = userIdEt.getText().toString();
                    if (!TextUtils.isEmpty(strUid)) {
                        uid = Long.parseLong(strUid);
                    } else {
                        SingleToast.show(getApplicationContext(), R.string.msg_uid_is_empty);
                        uid = 0L;
                    }

                    rtcRole = RtcConstants.RTC_ROLE_LIVE_GUEST;
                    break;
                case R.id.create_channel_btn:
                    channelID = channelIDEt.getText().toString();
                    rtcRole = RtcConstants.RTC_ROLE_MASTER;

                    strUid = userIdEt.getText().toString();
                    if (!TextUtils.isEmpty(strUid)) {
                        uid = Long.parseLong(strUid);
                    } else {
                        SingleToast.show(getApplicationContext(), R.string.msg_uid_is_empty);
                        uid = 0L;
                    }

                    break;
                case R.id.watch_channel_btn:
                    if (!TextUtils.isEmpty(channelIDEt.getText().toString())) {
                        channelID = channelIDEt.getText().toString();
                    } else {
                        SingleToast.show(getApplicationContext(), R.string.msg_channel_is_empty);
                        return;
                    }
                    rtcRole = RtcConstants.RTC_ROLE_GUEST;
                    break;
            }

            showRtcOrPlayer(rtcRole, uid, channelID);
        }
    };

    private void showRtcOrPlayer(int rtcRole, long uid, String channelID) {
        rtcOption.setChannelID(channelID);
        rtcOption.setRole(rtcRole);
        rtcOption.setUid(uid);

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_PLAY_CONFIG_BEAN, rtcOption);
        startActivity(intent);
    }

    private void showSettings() {
        Intent openSetting = new Intent(this, RtcOptionsActivity.class);
        startActivityForResult(openSetting, REQUEST_CODE_OPEN_SETTING);
    }

    private MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case MENU_SCAN:
                    Intent intent = new Intent(getApplication(), CaptureActivity.class);
                    startActivityForResult(intent, REQUEST_SCAN_QRCODE);
                    break;
                default:
                    break;
            }
            return false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SCAN_QRCODE:
                if (null != data) {
                    Bundle bundle = data.getExtras();
                    if (bundle == null) {
                        return;
                    }
                    if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                        String result = bundle.getString(CodeUtils.RESULT_STRING);
                        channelIDEt.setText(result);
                    } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                        SingleToast.show(getApplicationContext(), R.string.msg_scan_qrcode_error);
                    }
                }
                break;
            case REQUEST_CODE_OPEN_SETTING:
                if (null != data) {
                    int resolution = data.getIntExtra(Constant.EXTRA_KEY_RESOLUTION,
                            RtcConstants.RTC_RESOLUTION_HD);
                    rtcOption.setVideoResolution(resolution);
                }
                break;
        }
    }
}