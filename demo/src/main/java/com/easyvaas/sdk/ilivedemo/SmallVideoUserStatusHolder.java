package com.easyvaas.sdk.ilivedemo;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class SmallVideoUserStatusHolder extends RecyclerView.ViewHolder {
    TextView userIdTv;
    FrameLayout videoContainerFl;

    public SmallVideoUserStatusHolder(View v) {
        super(v);
        userIdTv = (TextView) v.findViewById(R.id.small_user_id_tv);
        videoContainerFl = (FrameLayout) v.findViewById(R.id.small_video_view_container);
    }
}
