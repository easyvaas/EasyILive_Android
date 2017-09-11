package com.easyvaas.sdk.ilivedemo.bean;

import java.io.Serializable;

/**
 * @author liya
 * @version V1.0
 * @ClassName:
 * @Package com.easyvaas.sdk.ilivedemo.bean
 * @Description:
 * @date 2017-07-13 15:46
 */

public class RtcOption implements Serializable {
    private int role;
    private int videoResolution;
    private String channelID;
    private long uid;
    private boolean isLive;

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public int getVideoResolution() {
        return videoResolution;
    }

    public void setVideoResolution(int videoResolution) {
        this.videoResolution = videoResolution;
    }

    public String getChannelID() {
        return channelID;
    }

    public void setChannelID(String channelID) {
        this.channelID = channelID;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }
}
