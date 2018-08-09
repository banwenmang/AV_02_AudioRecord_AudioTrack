package com.wenmag.av_02_audiorecord_audiotrack.contant;

import android.media.AudioFormat;

/**
 * desc: 音频常量
 * author: created by zhoujx on 2018/8/8 09:21
 */
public class AudioConstants {

    public static final int RECODE_AUDIO_BUFFER_TIMES = 1;
    public static final int PLAY_AUDIO_BUFFER_TIMES = 1;

    /**
     * 音频数据的采样率
     */
    public static final int AUDIO_FREQUENCY = 44100;

    /**
     * 输入声道为双声道立体声
     */
    public static final int RECORD_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;

    /**
     * 输出声道为双声道立体声
     */
    public static final int PLAY_CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;

    /**
     * 音频数据格式 ：PCM 每采样16位
     */
    public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
}
