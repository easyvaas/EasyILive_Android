package com.easyvaas.sdk.ilivedemo;

import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.easyvaas.sdk.evilive.wrapper.EVEventHandler;
import com.easyvaas.sdk.evilive.wrapper.EVLayoutRegion;
import com.easyvaas.sdk.evilive.wrapper.EVRtc;
import com.easyvaas.sdk.evilive.wrapper.RtcConstants;
import com.easyvaas.sdk.ilivedemo.bean.RtcOption;
import com.easyvaas.sdk.ilivedemo.utils.Logger;
import com.easyvaas.sdk.ilivedemo.utils.SingleToast;
import com.easyvaas.sdk.ilivedemo.utils.Utils;
import com.easyvaas.sdk.player.EVPlayer;
import com.easyvaas.sdk.player.PlayerConstants;
import com.easyvaas.sdk.player.base.EVPlayerBase;
import com.easyvaas.sdk.player.base.EVPlayerParameter;
import com.easyvaas.sdk.player.base.EVVideoView;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;

public class PlayerActivity extends BaseActivity implements View.OnClickListener, EVEventHandler {
    private final static String TAG = PlayerActivity.class.getSimpleName();
    public static final String EXTRA_PLAY_CONFIG_BEAN = "extra_play_config_bean";

    private final static int MAX_LINE_NUM = 3;
    private final static int GRID_ITEM_SPACE = 5;

    private EVVideoView mVideoView;
    private EVPlayer mEVPlayer;

    protected TextView mVideoTitleTv;
    protected TextView mUserIdTv;
    protected View mTopInfoAreaView;

    private CheckBox cameraSwitchCb;
    private CheckBox muteCb;
    private CheckBox audioOnlyCb;
    private ImageView shareIv;
    private CheckBox lianmai;

    private View mOptionsView;

    private PowerManager.WakeLock mWakeLock;

    private Dialog mLoadingDialog;

    private MediaController mMediaController;

    private RtcOption mRtcOption;

    private EVRtc mEVRtc;
    private String mChannelID;

    //本地大窗口view
    /*private GridVideoViewContainer mGridVideoViewContainer;*/
    private FrameLayout mFrameVideoViewContainer;

    private RelativeLayout mSmallVideoViewDock;
    private final LinkedHashMap<Integer, SurfaceView> mUidsList = new LinkedHashMap<>();

    private Dialog mConfirmStopDialog;

    public Bitmap mChannelBitmap = null;
    public Bitmap mShareBitmap = null;

    private int mRole;

    private int mMasterId;
    private int mCurrentId;
    private int mDisplayWidth;
    private int mDisplayHeight;

    private boolean mLeaveByUser = false;
    private String mShareUrl = "";

    private SmallVideoViewAdapter mSmallVideoViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(outMetrics);
        mDisplayWidth = outMetrics.widthPixels;
        mDisplayHeight = outMetrics.heightPixels;
    }

    @Override
    protected void initUIandEvent() {
        mRtcOption = (RtcOption) getIntent().getSerializableExtra(EXTRA_PLAY_CONFIG_BEAN);
        if (mRtcOption != null) {
            mChannelID = mRtcOption.getChannelID();
            mRole = mRtcOption.getRole();

            mEVPlayer = new EVPlayer(this);
            mEVRtc = new EVRtc(this, mRole == RtcConstants.RTC_ROLE_MASTER);
            mEVRtc.setVideoProfile(mRtcOption.getVideoResolution());
            mEVRtc.setRtcEventHandler(this);

            mOptionsView = findViewById(R.id.rtc_options_left_ll);
            mVideoView = (EVVideoView) findViewById(R.id.player_surface_view);
            cameraSwitchCb = (CheckBox) mOptionsView.findViewById(R.id.live_switch_camera_cb);
            muteCb = (CheckBox) mOptionsView.findViewById(R.id.live_mute_cb);
            audioOnlyCb = (CheckBox) mOptionsView.findViewById(R.id.live_audio_only_cb);
            shareIv = (ImageView) mOptionsView.findViewById(R.id.live_share_iv);
            lianmai = (CheckBox) findViewById(R.id.interactive_live_cb);
            lianmai.setOnCheckedChangeListener(mOnCheckedChangeListener);
            findViewById(R.id.live_close_iv).setOnClickListener(this);

            if (mRole == RtcConstants.RTC_ROLE_GUEST) {
                //普通观众
                guestUIAndEvent(muteCb, cameraSwitchCb, audioOnlyCb, shareIv);
                initEVPlayer();
                startPlay();
            } else {
                lianmai.setVisibility(View.GONE);
                //主播或连麦观众
                broadcasterUIAndEvent(muteCb, cameraSwitchCb, audioOnlyCb, shareIv);
                if (mRole == RtcConstants.RTC_ROLE_MASTER) {
                    startBroadCaster(mRtcOption.getUid(), mRtcOption.getChannelID());
                } else {
                    startRtc(mRtcOption.getUid(), mRtcOption.getChannelID());
                }
            }

            mEVPlayer.onCreate();
        }
    }

    @Override
    protected void deInitUIandEvent() {
        if (mEVRtc != null) {
            doLeaveChannel();
            mEVRtc.removeRtcEventHandler(this);
            mEVRtc.release();
        }

        mUidsList.clear();
        System.gc();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (null != mEVPlayer) {
            mEVPlayer.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != mEVPlayer) {
            mEVPlayer.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (null != mEVPlayer) {
            mEVPlayer.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mEVPlayer) {
            mEVPlayer.onDestroy();
        }

        dismissLoadingDialog();
        //releaseWakeLock();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startBroadCaster(long uid, String channel) {
        if (mEVRtc != null) {
            mEVRtc.createAndJoinChannel(channel, uid, true, true);
        }
    }

    private void startRtc(long uid, String channel) {
        if (mEVRtc != null) {
            mEVRtc.joinChannel(channel, uid);
        }
    }

    private void guestUIAndEvent(CheckBox muteCb, CheckBox switchCb, CheckBox audioOnlyCb,
                                 ImageView shareIv) {
        mTopInfoAreaView = this.findViewById(R.id.play_info_rl);
        mVideoTitleTv = (TextView) findViewById(R.id.player_title_tv);
        mUserIdTv = (TextView) findViewById(R.id.user_id_tv);
        mVideoTitleTv.setOnClickListener(this);

        muteCb.setVisibility(View.GONE);
        switchCb.setVisibility(View.GONE);
        audioOnlyCb.setVisibility(View.GONE);
        shareIv.setVisibility(View.GONE);

        mMediaController = new MediaController(this);
        mMediaController.setMediaPlayer(mediaControl);
        mMediaController.setAnchorView(mVideoView);

        if (mRtcOption.isLive()) {
            findViewById(R.id.player_bottom_progress_btn).setVisibility(View.GONE);
        } else {
            findViewById(R.id.player_bottom_progress_btn).setOnClickListener(this);
        }
    }

    private void broadcasterUIAndEvent(CheckBox muteCb, CheckBox switchCb, CheckBox audioOnlyCb,
                                       ImageView shareIv) {
        mVideoTitleTv = (TextView) findViewById(R.id.player_title_tv);
        mUserIdTv = (TextView) findViewById(R.id.user_id_tv);
        //mQRCodeImageView = (ImageView) findViewById(R.id.qrcode_image);
        mVideoTitleTv.setOnClickListener(this);
        findViewById(R.id.player_bottom_progress_btn).setVisibility(View.GONE);
        muteCb.setOnCheckedChangeListener(mOnCheckedChangeListener);
        switchCb.setOnCheckedChangeListener(mOnCheckedChangeListener);
        audioOnlyCb.setOnCheckedChangeListener(mOnCheckedChangeListener);
        shareIv.setOnClickListener(this);

        /*mGridVideoViewContainer = (GridVideoViewContainer) findViewById(R.id.grid_video_view_container);
        mGridVideoViewContainer.setItemEventHandler(new VideoViewEventListener() {
            @Override
            public void onItemDoubleClick(View v, Object item) {
                //处理双击小窗口改变布局的逻辑
            }
        });*/
        mFrameVideoViewContainer = (FrameLayout) findViewById(R.id.frame_video_view_container);

        mVideoView.setLayoutParams(new RelativeLayout.LayoutParams(1, 1));
    }

    private void initEVPlayer() {
        EVPlayerParameter.Builder builder = new EVPlayerParameter.Builder();
        builder.setLive(mRtcOption.isLive());
        mEVPlayer.setParameter(builder.build());
        mEVPlayer.setVideoView(mVideoView);
        mEVPlayer.setEnableAutoReconnect(true);
        mEVPlayer.setOnPreparedListener(mOnPreparedListener);
        mEVPlayer.setOnCompletionListener(mOnCompletionListener);
        mEVPlayer.setOnInfoListener(mOnInfoListener);
        mEVPlayer.setOnErrorListener(mOnErrorListener);
    }

    private void startPlay() {
        //mEVPlayer.watchStart(mPlayOption.getLid(), mPlayOption.isLive());
        if (mEVRtc != null) {
            mEVRtc.watchLive(mChannelID);
        }

        if (!isFinishing()) {
            showLoadingDialog(R.string.loading_data, true, true);
        }
    }

    private void doLeaveChannel() {
        if (mRole == RtcConstants.RTC_ROLE_LIVE_GUEST || mRole == RtcConstants.RTC_ROLE_MASTER) {
            mEVRtc.startPreview(false, null, mCurrentId);

            if (!mLeaveByUser && mRole == RtcConstants.RTC_ROLE_MASTER) {
                return;
            }

            mEVRtc.leaveChannel(mChannelID);
        }
    }

    private void acquireWakeLock() {
        if (null == mWakeLock) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
        }

        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (null != mWakeLock) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private void showLoadingDialog(int resId, boolean dismissTouchOutside, boolean cancelable) {
        showLoadingDialog(getString(resId), dismissTouchOutside, cancelable);
    }

    private void showLoadingDialog(String message, boolean dismissTouchOutside, boolean cancelable) {
        if (mLoadingDialog == null) {
            mLoadingDialog = Utils.getProcessDialog(this, message, dismissTouchOutside, cancelable);
        }
        mLoadingDialog.setCancelable(cancelable);
        mLoadingDialog.setCanceledOnTouchOutside(dismissTouchOutside);
        mLoadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.interactive_live_cb:
                    if (TextUtils.isEmpty(Constant.INTERACTIVE_LIVE_APP_ID)) {
                        SingleToast.show(getApplicationContext(), R.string.interactive_id_empty);
                    } else {
                        if (isChecked) {
                            startInteractive();

                        } else {
                            stopInteractive();
                        }
                    }

                    break;
                case R.id.live_mute_cb:
                    if (mEVRtc != null) {
                        mEVRtc.muteLocalAudioStream(isChecked);
                    }
                    break;
                case R.id.live_switch_camera_cb:
                    if (mEVRtc != null) {
                        mEVRtc.switchCamera();
                    }
                    break;
                case R.id.live_audio_only_cb:
                    if (mEVRtc != null) {
                        mEVRtc.muteLocalVideoStream(isChecked);
                    }
                    break;
            }
        }
    };

    private void startInteractive() {
        /*mEVPlayer.stopPlay();
        mVideoView.setVisibility(View.GONE);*/
        mEVPlayer.stopPlay();
        mRole = RtcConstants.RTC_ROLE_LIVE_GUEST;
        broadcasterUIAndEvent(muteCb, cameraSwitchCb, audioOnlyCb, shareIv);
        startRtc(mRtcOption.getUid(), mRtcOption.getChannelID());
        if (mSmallRecycler != null) {
            mSmallRecycler.setLayoutParams(new RelativeLayout.LayoutParams(mDisplayWidth, mDisplayHeight));
        }
    }

    private void stopInteractive() {
        /*mVideoView.setVisibility(View.VISIBLE);
        mEVPlayer.onCreate();
        startPlay();*/
        mEVPlayer.onCreate();
        startPlay();
        mRole = RtcConstants.RTC_ROLE_GUEST;
        guestUIAndEvent(muteCb, cameraSwitchCb, audioOnlyCb, shareIv);
        mFrameVideoViewContainer.removeAllViews();
        mEVRtc.leaveChannel(mChannelID);
        mUidsList.clear();
        mSmallVideoViewAdapter.customizedInit(new HashMap<Integer, SurfaceView>(0), true);
        mSmallRecycler.setLayoutParams(new RelativeLayout.LayoutParams(0, 0));
        mVideoView.setLayoutParams(new RelativeLayout.LayoutParams(mDisplayWidth, mDisplayHeight));
        mUserIdTv.setText("身份: 观众");
    }

    private void onInteractiveStart() {

    }

    private void onInteractiveEnd() {

    }

    private EVPlayerBase.OnPreparedListener mOnPreparedListener = new EVPlayerBase.OnPreparedListener() {
        @Override
        public boolean onPrepared() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //acquireWakeLock();
                    dismissLoadingDialog();
                }
            });
            return true;
        }
    };

    private EVPlayerBase.OnCompletionListener mOnCompletionListener = new EVPlayerBase.OnCompletionListener() {
        @Override
        public boolean onCompletion() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.getOneButtonDialog(PlayerActivity.this,
                            getString(R.string.msg_play_complete), false, false,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    finish();
                                    //releaseWakeLock();
                                }
                            }).show();
                    if (null != mEVPlayer) {
                        mEVPlayer.watchStop();
                    }
                }
            });
            return true;
        }
    };

    private EVPlayerBase.OnInfoListener mOnInfoListener = new EVPlayerBase.OnInfoListener() {
        @Override
        public boolean onInfo(int what, final int extra) {
            switch (what) {
                case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()) {
                                showLoadingDialog(R.string.loading_data, true, true);
                            }
                        }
                    });
                    break;
                case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dismissLoadingDialog();
                        }
                    });
                    break;
                case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    Logger.w(TAG, "open first video time(ms): " + extra);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mVideoTitleTv.setVisibility(View.VISIBLE);
                            mVideoTitleTv.setText("首屏打开时间: " + extra + " ms" + ", 房间号: " + mChannelID);

                            mUserIdTv.setVisibility(View.VISIBLE);
                            mUserIdTv.setText("身份: 观众");
                        }
                    });
                    break;
            }
            return true;
        }
    };

    private EVPlayerBase.OnErrorListener mOnErrorListener = new EVPlayerBase.OnErrorListener() {
        @Override
        public boolean onError(int what, int extra) {
            switch (what) {
                case PlayerConstants.EV_PLAYER_ERROR_SDK_INIT:
                    showToastOnUiThread(R.string.msg_sdk_init_error);
                    break;
                case PlayerConstants.EV_PLAYER_ERROR_GET_RESOURCE:
                    showToastOnUiThread(R.string.msg_live_show_error);
                    break;
                case PlayerConstants.EV_PLAYER_ERROR_GET_URL:
                    showToastOnUiThread(R.string.msg_live_redirect_error);
                    break;
                case PlayerConstants.EV_PLAYER_ERROR_LIVE_EXCEPTION:
                    showToastOnUiThread(R.string.msg_play_live_error);
                    break;
                case PlayerConstants.EV_PLAYER_ERROR_NONE_STREAM:
                    showToastOnUiThread(R.string.msg_play_none_stream_error);
                    break;
                case PlayerConstants.EV_PLAYER_ERROR_NOT_ACCEPTABLE:
                    showToastOnUiThread(R.string.msg_play_not_support_error);
                    break;
                case PlayerConstants.EV_PLAYER_ERROR_PARAMETER:
                    showToastOnUiThread(R.string.msg_play_parameter_error);
                    break;
                case PlayerConstants.EV_PLAYER_ERROR_RECORD_EXCEPTION:
                    showToastOnUiThread(R.string.msg_play_record_error);
                    break;
                case PlayerConstants.EV_PLAYER_ERROR_FILE_EXCEPTION:
                    showToastOnUiThread(R.string.msg_play_file_error);
                    break;
                default:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.getOneButtonDialog(PlayerActivity.this,
                                    getString(R.string.msg_play_error), false, false,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            dismissLoadingDialog();
                                            //releaseWakeLock();
                                            if (null != mEVPlayer) {
                                                mEVPlayer.onStop();
                                            }
                                            finish();
                                        }
                                    }).show();
                        }
                    });
                    break;
            }

            return true;
        }
    };

    private void showToastOnUiThread(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                SingleToast.show(getApplicationContext(), resId);
                finish();
            }
        });
    }

    private void showErrorDialog(final int code, final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int resid = R.string.msg_rtc_error;
                switch (code) {
                    case RtcConstants.RTC_ERROR_LIVE_SHOW:
                        resid = R.string.msg_rtc_live_show;
                        break;
                    case RtcConstants.RTC_ERROR_RECORD_SHOW:
                        resid = R.string.msg_rtc_record_show;
                        break;
                    case RtcConstants.RTC_ERROR_CHANNEL_NOT_EXIST:
                        resid = R.string.msg_rtc_channel_not_exist;
                        break;
                    case RtcConstants.RTC_ERROR_INVALID_APPID:
                        resid = R.string.msg_rtc_invalid_app_id;
                        break;
                    case RtcConstants.RTC_ERROR_CREATE_CHANNEL:
                        resid = R.string.msg_rtc_create_channel;
                        break;
                    case RtcConstants.RTC_ERROR_JOIN_CHANNEL:
                    case RtcConstants.RTC_ERROR_USER_ALREADY_JOINED:
                        resid = R.string.msg_rtc_join_channel;
                        break;
                    case RtcConstants.RTC_ERROR_CHANNEL_CONFLICT:
                        resid = R.string.msg_rtc_channel_conflict;
                        break;
                    case RtcConstants.RTC_ERROR_USER_AUTH:
                        resid = R.string.msg_rtc_user_auth;
                        break;
                    case RtcConstants.RTC_ERROR_PERMISSION_KEY:
                        resid = R.string.msg_rtc_permission_key;
                        break;
                    case RtcConstants.RTC_ERROR_CHANNEL_EMPTY:
                        resid = R.string.msg_rtc_channel_empty;
                        break;
                    case RtcConstants.RTC_ERROR_CHANNEL_MAX_USERS:
                        resid = R.string.msg_rtc_channel_max_users;
                        break;
                    case RtcConstants.RTC_ERROR_MASTER_OFFLINE:
                        resid = R.string.title_master_stop_rtc;
                        break;
                }

                Utils.getOneButtonDialog(PlayerActivity.this,
                        getString(resid) + ", msg: " + msg, false, false,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        }).show();
            }
        });
    }

    private void showMasterExitDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utils.getOneButtonDialog(PlayerActivity.this,
                        getString(R.string.title_master_stop_rtc), false, false,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        }).show();
            }
        });
    }

    private void showConfirmStopDialog() {
        if (mConfirmStopDialog != null) {
            mConfirmStopDialog.show();
            return;
        }
        mConfirmStopDialog = new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.title_confirm_stop_rtc)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mLeaveByUser = true;
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                            dialog.dismiss();
                            return true;
                        }
                        return false;
                    }
                })
                .setCancelable(false)
                .create();
        mConfirmStopDialog.show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.live_close_iv:
                if (mRole == RtcConstants.RTC_ROLE_GUEST) {
                    if (null != mEVPlayer) {
                        mEVPlayer.watchStop();
                    }

                    finish();
                } else {
                    showConfirmStopDialog();
                }
                break;
            case R.id.player_bottom_progress_btn:
                toggleProgressBar(true);
                break;
            case R.id.player_title_tv:
                showQRCodeView(view);
                break;
            case R.id.live_share_iv:
                getShareUrl(view);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.player_bottom_action_bar).getVisibility() != View.VISIBLE) {
            toggleProgressBar(false);
        } else {
            super.onBackPressed();
        }
    }

    private void getShareUrl(View parentView) {
        if (TextUtils.isEmpty(mShareUrl)) {
            showInfoToast("获取分享地址失败");
        } else {
            showShareView(parentView, mShareUrl);
        }
    }

    private void showChannelTitleTv(final String channel) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoTitleTv.setVisibility(View.VISIBLE);
                mVideoTitleTv.setText("房间号: " + channel + ", 点击弹出二维码");
            }
        });
    }

    private void showShareView(View parentView, final String url) {
        View contentView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.dialog_share, null);
        final PopupWindow popupWindow = new PopupWindow(contentView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setBackgroundDrawable(new ColorDrawable(0x55000000));//支持点击Back虚拟键退出
        popupWindow.setOutsideTouchable(true);
        popupWindow.setClippingEnabled(false);
        // 设置popWindow的显示和消失动画
        popupWindow.setAnimationStyle(R.style.mypopwindow_anim_style);

        ImageView qrCodeImg;
        EditText urlEt;
        Button copyBtn;
        qrCodeImg = (ImageView) contentView.findViewById(R.id.qrcode_image_url);
        urlEt = (EditText) contentView.findViewById(R.id.live_url_et);
        urlEt.setText(url);
        copyBtn = (Button) contentView.findViewById(R.id.copy_url_btn);
        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager cmb = (ClipboardManager) getApplicationContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                cmb.setText(url);
                showInfoToast("分享地址已经复制到剪贴板!");
            }
        });

        if (mShareBitmap == null) {
            mShareBitmap = CodeUtils.createImage(url, 400, 400, null);
        }
        qrCodeImg.setImageBitmap(mShareBitmap);

        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0);
    }

    private void showQRCodeView(View parentView) {
        View contentView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.dialog_qr_code, null);
        final PopupWindow popupWindow = new PopupWindow(contentView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setBackgroundDrawable(new ColorDrawable(0xffffffff));//支持点击Back虚拟键退出
        popupWindow.setOutsideTouchable(true);
        popupWindow.setClippingEnabled(false);
        // 设置popWindow的显示和消失动画
        popupWindow.setAnimationStyle(R.style.mypopwindow_anim_style);

        ImageView qrCodeImg;
        qrCodeImg = (ImageView) contentView.findViewById(R.id.qrcode_image);
        if (mChannelBitmap == null) {
            mChannelBitmap = CodeUtils.createImage(mChannelID, 400, 400, null);
        }
        qrCodeImg.setImageBitmap(mChannelBitmap);

        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = contentView.getMeasuredWidth();
        int popupHeight = contentView.getMeasuredHeight();

        int[] location = new int[2];
        parentView.getLocationOnScreen(location);
        popupWindow.showAtLocation(parentView, Gravity.NO_GRAVITY, (location[0] + parentView.getWidth() / 2) - popupWidth / 2,
                location[1] + popupHeight / 4);
    }

    private void showErrorToast(int resId) {
        if (isFinishing()) {
            return;
        }

        SingleToast.show(getApplicationContext(), resId);
        finish();
    }

    private void showInfoToast(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                SingleToast.show(getApplicationContext(), resId);
            }
        });
    }

    private void showUserId(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long curId = uid;
                mUserIdTv.setVisibility(View.VISIBLE);
                mUserIdTv.setText("用户id: " + curId + ", 身份: "
                        + (mRole == RtcConstants.RTC_ROLE_MASTER ? "主播" : "连麦观众"));
            }
        });
    }

    private void showInfoToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                SingleToast.show(getApplicationContext(), text);
            }
        });
    }

    private void toggleProgressBar(boolean show) {
        if (mMediaController == null || mVideoView == null) {
            return;
        }
        View actionBar = findViewById(R.id.player_bottom_action_bar);
        if (show) {
            actionBar.setVisibility(View.INVISIBLE);
            mMediaController.show();
        } else {
            actionBar.setVisibility(View.VISIBLE);
            mMediaController.hide();
        }
    }

    private MediaController.MediaPlayerControl mediaControl = new MediaController.MediaPlayerControl() {
        @Override
        public void start() {
            if (mVideoView != null) {
                mVideoView.start();
            }
        }

        @Override
        public void pause() {
            if (mVideoView != null) {
                mVideoView.pause();
            }
        }

        @Override
        public int getDuration() {
            return mVideoView != null ? (int) mVideoView.getDuration() : 0;
        }

        @Override
        public int getCurrentPosition() {
            return mVideoView != null ? (int) mVideoView.getCurrentPosition() : 0;
        }

        @Override
        public void seekTo(long pos) {
            if (mVideoView != null) {
                mVideoView.seekTo(pos);
            }
        }

        @Override
        public boolean isPlaying() {
            return mVideoView != null && mVideoView.isPlaying();
        }

        @Override
        public int getBufferPercentage() {
            return mVideoView != null ? mVideoView.getBufferPercentage() : 0;
        }

        @Override
        public boolean canPause() {
            return mVideoView != null && mVideoView.canPause();
        }

        @Override
        public boolean canSeekBackward() {
            return true;
        }

        @Override
        public boolean canSeekForward() {
            return true;
        }
    };

    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        //收到远程视频第一帧数据，可以开始渲染连麦小窗口了
        renderRemoteView(uid);
    }

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
        //加入连麦房间成功
        mCurrentId = uid;

        if (mEVRtc != null) {
            mEVRtc.getShareUrl(channel);
        }

        showInfoToast(R.string.msg_rtc_join_success);
        showUserId(uid);
        showChannelTitleTv(channel);
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        //有用户离开，处理界面变化
        long offUserId = uid;
        if (mMasterId == offUserId) {
            //如果主播退出,给出提示
            showInfoToast(R.string.msg_rtc_master_leave_temp);
        }

        removeRemoteView(uid);
    }

    @Override
    public void onUserMuteVideo(int uid, boolean muted) {
        //有用户开启/关闭了视频显示
        long muteId = uid;
        int resId = muted ? R.string.msg_rtc_user_mute_video_on
                : R.string.msg_rtc_user_mute_video_off;
        showInfoToast(getString(resId, muteId + " "));
    }

    @Override
    public void onUserMuteAudio(int uid, boolean muted) {
        //有用户开启/关闭了音频传输
        long muteId = uid;
        int resId = muted ? R.string.msg_rtc_user_mute_audio_on
                : R.string.msg_rtc_user_mute_audio_off;
        showInfoToast(getString(resId, muteId + " "));
    }

    @Override
    public void onPlayUrl(String url) {
        //获取到旁路直播的播放地址
        if (mEVPlayer != null) {
            mEVPlayer.startPlay(url);
        }
    }

    @Override
    public void onChannelCreate(String channel) {
        //互动房间创建成功
        mChannelID = channel;
    }

    @Override
    public void onShareUrl(String url) {
        //获取到直播观看h5地址
        mShareUrl = url;
    }

    private SurfaceView localView;
    //private long localUid;

    @Override
    public void onAuthSuccess(String channel, long masterId, long uid) {
        //连麦鉴权成功,开启本地大窗口视频预览
        if (masterId == 0) {
            mMasterId = new Long(uid).intValue();
        } else {
            mMasterId = new Long(masterId).intValue();
        }
        localUid = Long.valueOf(uid).intValue();
        bigScreenUid = Long.valueOf(uid).intValue();
        //previewAtSmallPosUid = Long.valueOf(masterId).intValue();
        Logger.d(TAG, "user auth success, channel: " + channel + ", owner id: " + masterId);

        if (mEVRtc != null) {
            localView = mEVRtc.createRendererView(getApplicationContext());
            mEVRtc.setupLocalView(localView, Long.valueOf(uid).intValue());
            localView.setZOrderOnTop(false);
            localView.setZOrderMediaOverlay(false);
            localView.getHolder().setFormat(PixelFormat.TRANSPARENT);

            mUidsList.put(Long.valueOf(uid).intValue(), localView); // get first surface view
            /*mGridVideoViewContainer.initViewContainer(getApplicationContext(), 0, mUidsList); // first is now full view*/
            mFrameVideoViewContainer.addView(localView, new FrameLayout.LayoutParams(mDisplayWidth, mDisplayHeight));

            mEVRtc.startPreview(true, localView, Long.valueOf(uid).intValue());
        }

        showInfoToast(R.string.msg_rtc_auth_success);
    }

    @Override
    public void onError(int error, String msg) {
        //连麦错误信息回调
        showErrorDialog(error, msg);
    }

    private void renderRemoteView(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                if (mEVRtc != null) {
                    SurfaceView surfaceV = mEVRtc.createRendererView(getApplicationContext());
                    surfaceV.setZOrderOnTop(true);
                    surfaceV.setZOrderMediaOverlay(true);
                    surfaceV.getHolder().setFormat(PixelFormat.TRANSPARENT);
                    mUidsList.put(uid, surfaceV);
                    mEVRtc.setupRemoteView(surfaceV, uid);

                    //调整合图布局
                    if (mRole == RtcConstants.RTC_ROLE_MASTER) {
                        updateCompositingLayout(mUidsList, bigScreenUid);
                    }
                }

                addOverlapVideoView();
            }
        });
    }

    private void addOverlapVideoView() {
        /*HashMap<Integer, SurfaceView> slice = new HashMap<>(1);
        slice.put(0, mUidsList.get(0));
        mGridVideoViewContainer.initViewContainer(getApplicationContext(), 0, slice);*/

        bindToSmallVideoView(bigScreenUid, MAX_LINE_NUM, GRID_ITEM_SPACE);
    }

    private void removeRemoteView(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                /*int bigBgUid = -1;
                if (mSmallVideoViewAdapter != null) {
                    bigBgUid = mSmallVideoViewAdapter.getExceptedUid();
                }
                Logger.d(TAG, "doRemoveRemoteUi " + (uid & 0xFFFFFFFFL) + " " + (bigBgUid & 0xFFFFFFFFL));*/
                //调整合图布局
                //if (mRole == RtcConstants.RTC_ROLE_MASTER) {
                if (uid == bigScreenUid) {
                    toggleBigAndSmall(new VideoStatusData(localUid, mUidsList.get(localUid),
                            VideoStatusData.DEFAULT_STATUS, VideoStatusData.DEFAULT_VOLUME));
                    mUidsList.remove(uid);
                    updateCompositingLayout(mUidsList, localUid);
                } else {
                    mUidsList.remove(uid);
                    updateCompositingLayout(mUidsList, bigScreenUid);
                }
                addOverlapVideoView();
            }
        });
    }

    private void updateCompositingLayout(HashMap<Integer, SurfaceView> uids, int bigScrennUid) {
        double smallViewHeight = (mDisplayHeight - (MAX_LINE_NUM - 1) * GRID_ITEM_SPACE) / MAX_LINE_NUM;
        double smallViewWidth = smallViewHeight * 16 / 9;

        double layoutW = smallViewWidth / mDisplayWidth;
        double layoutH = smallViewHeight / mDisplayHeight;

        List<EVLayoutRegion> regions = new ArrayList<>();
        ArrayList<Integer> users = new ArrayList<>();
        for (HashMap.Entry<Integer, SurfaceView> entry : uids.entrySet()) {
            int uid = entry.getKey();

            if (uid == bigScrennUid) {
                EVLayoutRegion region = new EVLayoutRegion();
                region.setUid(mCurrentId);
                region.setMaster(true);
                region.setX(0);
                region.setY(0);
                region.setW(1.0);
                region.setH(1.0);

                regions.add(region);
            } else {
                users.add(uid);
            }
        }

        int usersize = users.size();
        for (int i = 0; i < usersize; i++) {
            int column = i / MAX_LINE_NUM;
            int row = i % MAX_LINE_NUM;
            double xpos = (mDisplayWidth - (smallViewWidth * (column + 1)) - column * GRID_ITEM_SPACE) / mDisplayWidth;
            double ypos = (row * (smallViewHeight + GRID_ITEM_SPACE)) / mDisplayHeight;
            EVLayoutRegion region = new EVLayoutRegion();

            region.setUid(users.get(i));
            region.setMaster(false);
            region.setX(xpos);
            region.setY(ypos);
            region.setW(layoutW);
            region.setH(layoutH);

            regions.add(region);
        }

        if (mEVRtc != null) {
            mEVRtc.configCompositiongLayout(regions);
        }
    }

    private RecyclerView mSmallRecycler;
    private boolean isPreviewInMainscreen = true;
    private int bigScreenUid;
    private int localUid;

    private void toggleBigAndSmall(VideoStatusData item) {
        if (mEVRtc != null) {
            SurfaceView oldBig = (SurfaceView) mFrameVideoViewContainer.getChildAt(0);
            oldBig.setZOrderOnTop(true);
            oldBig.setZOrderMediaOverlay(true);
            mFrameVideoViewContainer.removeAllViews();
            SurfaceView oldSmall = item.mView;
            ViewParent parent = oldSmall.getParent();
            if (parent != null) {
                ((FrameLayout) parent).removeView(oldSmall);
            }
            oldSmall.setZOrderOnTop(false);
            oldSmall.setZOrderMediaOverlay(false);
            mFrameVideoViewContainer.addView(oldSmall, new FrameLayout.LayoutParams(mDisplayWidth, mDisplayHeight));
            if (mRole == RtcConstants.RTC_ROLE_MASTER) {
                if (isPreviewInMainscreen) {
                    mUidsList.put(bigScreenUid, oldBig);
                    mUidsList.put(item.mUid, oldSmall);
                    mUserIdTv.setText("用户id: " + item.mUid + ", 身份: 连麦观众");
                    mEVRtc.setupRemoteView(oldSmall, item.mUid);
                    mEVRtc.setupLocalView(oldBig, Long.valueOf(bigScreenUid).intValue());
                    mEVRtc.startPreview(true, oldBig, Long.valueOf(bigScreenUid).intValue());
                    isPreviewInMainscreen = false;
                } else if (item.mUid == mMasterId) {
                    mUidsList.put(bigScreenUid, oldBig);
                    mUidsList.put(item.mUid, oldSmall);
                    mUserIdTv.setText("用户id: " + mMasterId + ", 身份: 主播");
                    mEVRtc.setupRemoteView(oldBig, bigScreenUid);
                    mEVRtc.setupLocalView(oldSmall, item.mUid);
                    mEVRtc.startPreview(true, oldSmall, item.mUid);
                    isPreviewInMainscreen = true;
                } else {
                    mUidsList.put(bigScreenUid, oldBig);
                    mUidsList.put(item.mUid, oldSmall);
                    mUserIdTv.setText("用户id: " + item.mUid + ", 身份: 连麦观众");
                    mEVRtc.setupRemoteView(oldBig, bigScreenUid);
                    mEVRtc.setupRemoteView(oldSmall, item.mUid);
                }
            } else if (mRole == RtcConstants.RTC_ROLE_LIVE_GUEST) {
                if (isPreviewInMainscreen) {
                    mUidsList.put(bigScreenUid, oldBig);
                    mUidsList.put(item.mUid, oldSmall);
                    if (item.mUid == mMasterId) {
                        mUserIdTv.setText("用户id: " + mMasterId + ", 身份: 主播");
                    } else {
                        mUserIdTv.setText("用户id: " + item.mUid + ", 身份: 连麦观众");
                    }
                    mEVRtc.setupRemoteView(oldSmall, item.mUid);
                    mEVRtc.setupLocalView(oldBig, bigScreenUid);
                    mEVRtc.startPreview(true, oldBig, bigScreenUid);
                    isPreviewInMainscreen = false;
                } else if (item.mUid == localUid) {
                    mUidsList.put(bigScreenUid, oldBig);
                    mUidsList.put(item.mUid, oldSmall);
                    mUserIdTv.setText("用户id: " + item.mUid + ", 身份: 连麦观众");
                    mEVRtc.setupRemoteView(oldBig, Long.valueOf(bigScreenUid).intValue());
                    mEVRtc.setupLocalView(oldSmall, item.mUid);
                    mEVRtc.startPreview(true, oldSmall, item.mUid);
                    isPreviewInMainscreen = true;
                } else {
                    mUidsList.put(bigScreenUid, oldBig);
                    mUidsList.put(item.mUid, oldSmall);
                    if (item.mUid == mMasterId) {
                        mUserIdTv.setText("用户id: " + mMasterId + ", 身份: 主播");
                    } else {
                        mUserIdTv.setText("用户id: " + item.mUid  + ", 身份: 连麦观众");
                    }
                    mEVRtc.setupRemoteView(oldBig, bigScreenUid);
                    mEVRtc.setupRemoteView(oldSmall, item.mUid);
                }
            }
            updateCompositingLayout(mUidsList, item.mUid);
            bindToSmallVideoView(item.mUid, MAX_LINE_NUM, GRID_ITEM_SPACE);
            bigScreenUid = item.mUid;
        }
    }

    private void bindToSmallVideoView(int exceptUid, int lineNum, int space) {
        if (mSmallVideoViewDock == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.small_video_view_dock);
            mSmallVideoViewDock = (RelativeLayout) stub.inflate();
        }

        mSmallRecycler = (RecyclerView) findViewById(R.id.small_video_view_container);

        boolean create = false;

        if (mSmallVideoViewAdapter == null) {
            create = true;
            mSmallVideoViewAdapter = new SmallVideoViewAdapter(this, exceptUid, (int) mMasterId, mUidsList, lineNum,
                    space, new VideoViewEventListener() {
                @Override
                public void onItemDoubleClick(View v, Object item) {
                    toggleBigAndSmall((VideoStatusData) item);
                }
            });
            mSmallVideoViewAdapter.setHasStableIds(true);
            mSmallRecycler.addItemDecoration(new SpaceItemDecoration(space, lineNum, GridLayoutManager.HORIZONTAL));
        }
        mSmallRecycler.setHasFixedSize(true);

        mSmallRecycler.setLayoutManager(new GridLayoutManager(this, lineNum, GridLayoutManager.HORIZONTAL, true));
        mSmallRecycler.setAdapter(mSmallVideoViewAdapter);

        mSmallRecycler.setDrawingCacheEnabled(true);
        mSmallRecycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

        if (!create) {
            mSmallVideoViewAdapter.notifyUiChanged(mUidsList, exceptUid, null, null);
        }
        mSmallRecycler.setVisibility(View.VISIBLE);
        mSmallVideoViewDock.setVisibility(View.VISIBLE);
    }
}
