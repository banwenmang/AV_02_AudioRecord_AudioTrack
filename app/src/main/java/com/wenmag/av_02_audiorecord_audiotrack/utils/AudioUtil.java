package com.wenmag.av_02_audiorecord_audiotrack.utils;

/**
 * desc: WDAudio 工具类
 * author: created by zhoujx on 2018/8/8 14:02
 */
public class AudioUtil {
    public static String getHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append(Integer.toHexString(b)).append(",");
        }

        return sb.toString();
    }

    public static String getNormalString(byte[] bytes){
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append(String.valueOf(b)).append(",");
        }

        return sb.toString();
    }
}
