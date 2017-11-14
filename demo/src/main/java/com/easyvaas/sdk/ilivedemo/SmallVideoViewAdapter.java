package com.easyvaas.sdk.ilivedemo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.easyvaas.sdk.ilivedemo.utils.Logger;

import java.util.HashMap;

public class SmallVideoViewAdapter extends VideoViewAdapter {
    private static final String TAG = SmallVideoViewAdapter.class.getSimpleName();

    public SmallVideoViewAdapter(Context context, int exceptedUid, int masterUid,
                                 HashMap<Integer, SurfaceView> uids,
                                 int lines, int space, VideoViewEventListener listener) {
        super(context, exceptedUid, masterUid, uids, lines, space, listener);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d("VideoViewAdapter", "onCreateViewHolder " + viewType);

        View v = mInflater.inflate(R.layout.small_video_view_container, parent, false);
        v.getLayoutParams().width = mItemWidth;
        v.getLayoutParams().height = mItemHeight;
        //Log.e("simonwLog", "mItemWidth:" + mItemWidth + ",mItemHeight:" + mItemHeight);
        return new SmallVideoUserStatusHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SmallVideoUserStatusHolder myHolder = ((SmallVideoUserStatusHolder) holder);

        final VideoStatusData user = mUsers.get(position);

        Logger.d(TAG, "onBindViewHolder " + position + " " + user + " " + myHolder + " " + myHolder.itemView);

        FrameLayout holderView = myHolder.videoContainerFl;

        long curUid = user.mUid & 0xffffffffL;
        myHolder.userIdTv.setText("用户id: " + curUid + ", 身份: " +
                (curUid == (mMasterUid & 0xffffffffL) ? "主播" : "连麦观众"));

        if (holderView.getChildCount() == 0) {
            SurfaceView target = user.mView;
            stripSurfaceView(target);
            holderView.addView(target, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        holderView.setOnTouchListener(new OnDoubleTapListener(mContext) {
            @Override
            public void onDoubleTap(View view, MotionEvent e) {
                if (mListener != null) {
                    mListener.onItemDoubleClick(view, user);
                }
            }

            @Override
            public void onSingleTapUp() {
            }
        });
    }

    @Override
    protected void customizedInit(HashMap<Integer, SurfaceView> uids, boolean force) {
        for (HashMap.Entry<Integer, SurfaceView> entry : uids.entrySet()) {
            if (entry.getKey() != exceptedUid) {
                entry.getValue().setZOrderOnTop(true);
                entry.getValue().setZOrderMediaOverlay(true);
                mUsers.add(new VideoStatusData(entry.getKey(), entry.getValue(), VideoStatusData.DEFAULT_STATUS, VideoStatusData.DEFAULT_VOLUME));
            }
        }

        if (force || mItemWidth == 0 || mItemHeight == 0) {
            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(outMetrics);
            //int outWidth = outMetrics.widthPixels - (mLineNum - 1)*mSpace;
            int outHeight = outMetrics.heightPixels - (mLineNum - 1) * mSpace;
            //mItemWidth = outWidth / mLineNum;
            mItemHeight = outHeight / mLineNum;
            mItemWidth = mItemHeight * 16 / 9;
        }
    }

    @Override
    public void notifyUiChanged(HashMap<Integer, SurfaceView> uids, int uidExcluded, HashMap<Integer, Integer> status, HashMap<Integer, Integer> volume) {
        mUsers.clear();

        for (HashMap.Entry<Integer, SurfaceView> entry : uids.entrySet()) {
            Logger.d(TAG, "notifyUiChanged " + entry.getKey() + " " + uidExcluded);

            if (entry.getKey() != uidExcluded) {
                entry.getValue().setZOrderOnTop(true);
                entry.getValue().setZOrderMediaOverlay(true);
                mUsers.add(new VideoStatusData(entry.getKey(), entry.getValue(), VideoStatusData.DEFAULT_STATUS, VideoStatusData.DEFAULT_VOLUME));
            }
        }

        notifyDataSetChanged();
    }

    public int getExceptedUid() {
        return exceptedUid;
    }
}
