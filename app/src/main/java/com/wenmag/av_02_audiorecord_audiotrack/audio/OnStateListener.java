package com.wenmag.av_02_audiorecord_audiotrack.audio;

/**
 * desc: 状态变化接口
 * author: created by zhoujx on 2018/8/7 18:24
 */
public interface OnStateListener {

    /**
     * 状态变化
     *
     * @param currentState
     */
    void onStateChanged(WindState currentState);
}
