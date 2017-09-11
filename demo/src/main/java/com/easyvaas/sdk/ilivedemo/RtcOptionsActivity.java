package com.easyvaas.sdk.ilivedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.easyvaas.sdk.evilive.wrapper.RtcConstants;
import com.easyvaas.sdk.ilivedemo.bean.RtcOption;
import com.easyvaas.sdk.ilivedemo.utils.Preferences;
import com.easyvaas.sdk.ilivedemo.utils.SingleToast;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;

public class RtcOptionsActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = RtcOptionsActivity.class.getSimpleName();
    public static final int REQUEST_CODE = 111;

    private RadioButton resolutionHDBtn;
    private RadioButton resolutionSDBtn;
    private RadioButton resolutionLDBtn;

    private boolean isMaster = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtc_options);

        resolutionHDBtn = (RadioButton) findViewById(R.id.radiobutton_hd);
        resolutionSDBtn = (RadioButton) findViewById(R.id.radiobutton_sd);
        resolutionLDBtn = (RadioButton) findViewById(R.id.radiobutton_ld);

        int prefResolution = Preferences.getInstance(getApplicationContext()).getInt(
                Preferences.KEY_RTC_RESOLUTION, RtcConstants.RTC_RESOLUTION_HD
        );

        switch (prefResolution) {
            case RtcConstants.RTC_RESOLUTION_HD:
                resolutionHDBtn.setChecked(true);
                break;
            case RtcConstants.RTC_RESOLUTION_SD:
                resolutionSDBtn.setChecked(true);
                break;
            case RtcConstants.RTC_RESOLUTION_LD:
                resolutionSDBtn.setChecked(true);
                break;
            default:
                resolutionHDBtn.setChecked(true);
                break;
        }

        getSupportActionBar().setTitle("设置");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_rtc_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_confirm) {
            int rtcResolution = RtcConstants.RTC_RESOLUTION_HD;
            if (resolutionHDBtn.isChecked()) {
                rtcResolution = RtcConstants.RTC_RESOLUTION_HD;
            } else if (resolutionSDBtn.isChecked()) {
                rtcResolution = RtcConstants.RTC_RESOLUTION_SD;
            } else if (resolutionLDBtn.isChecked()) {
                rtcResolution = RtcConstants.RTC_RESOLUTION_LD;
            }

            saveProfile(rtcResolution);

            Intent selectResolutionIntent = new Intent();
            selectResolutionIntent.putExtra(Constant.EXTRA_KEY_RESOLUTION, rtcResolution);
            setResult(RESULT_OK, selectResolutionIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

        }
    }

    private void saveProfile(int resolution) {
        Preferences.getInstance(getApplicationContext()).putInt(
                Preferences.KEY_RTC_RESOLUTION, resolution
        );
    }
}
