package com.wenmag.av_02_audiorecord_audiotrack.audio;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.wenmag.av_02_audiorecord_audiotrack.utils.AudioUtil;
import com.wenmag.av_02_audiorecord_audiotrack.utils.LogUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * desc: 音频录制器
 * author: created by zhoujx on 2018/8/7 17:37
 */
public class WDAudio {
    private static final String TAG = WDAudio.class.getSimpleName();
    private static final String TMP_FOLDER_NAME = "WDAudio";

    private AudioRecordThread mRecordThread;
    private AudioTrackPlayThread mPalyThread;
    volatile WindState mState = WindState.IDLE;

    File mTmpPCMFile;
    File mTmpWAVFile;

    private OnStateListener mOnStateListener;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    /**
     * PCM 缓存目录
     */
    private static String sCachePCMFolder;

    /**
     * WAV 缓存目录
     */
    private static String sCacheWAVFolder;

    private static WDAudio sInstance;

    private WDAudio() {

    }

    public static WDAudio getInstance() {
        if (null == sInstance) {
            synchronized (WDAudio.class) {
                if (null == sInstance) {
                    sInstance = new WDAudio();
                }
            }
        }

        return sInstance;
    }

    /**
     * 设置状态变化回调
     *
     * @param onStateListener
     */
    public void setOnStateListener(OnStateListener onStateListener) {
        mOnStateListener = onStateListener;
    }

    /**
     * 初始化 录制文件夹
     */
    public static void init() {
        LogUtil.d(TAG, "初始化录制文件");
        initPCMFolder();
        initWAVFolder();
    }

    /**
     * 初始化 pcm 文件夹
     */
    private static void initPCMFolder() {
        sCachePCMFolder = Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + TMP_FOLDER_NAME;

        makeFolder(sCachePCMFolder, "PCM目录", true);
    }

    /**
     * 初始化 wav 文件夹
     */
    private static void initWAVFolder() {
        sCacheWAVFolder = Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + TMP_FOLDER_NAME;

        makeFolder(sCacheWAVFolder, "WAV目录", false);
    }

    /**
     * 创建文件夹
     *
     * @param folderPath 文件夹路径
     * @param desc       文件夹描述
     * @param isRemove   是否删除文件夹中已存在文件
     */
    private static void makeFolder(String folderPath, String desc, boolean isRemove) {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            boolean f = folder.mkdirs();
            Log.d(TAG, String.format(Locale.CHINA, "%s ：%s -> %b", desc, folderPath, f));
        } else {
            if (isRemove) {
                for (File file : folder.listFiles()) {
                    boolean d = file.delete();
                    LogUtil.d(TAG, String.format(Locale.CHINA, "删除PCM文件：%s -> %b", file.getName(), d));
                }
            }

            LogUtil.d(TAG, String.format(Locale.CHINA, "%s ：%s", desc, folderPath));
        }
    }

    /**
     * 开始录制
     *
     * @param createWav 是否录制 wav文件
     */
    public synchronized void startRecord(boolean createWav) {
        LogUtil.i(TAG, "startRecord enter...");
        if (!mState.equals(WindState.IDLE)) {
            LogUtil.w(TAG, "无法开始录制，当前状态为：" + mState);
            return;
        }

        try {
            createFile(createWav);
        } catch (IOException e) {
            LogUtil.e(TAG, e.getMessage());
        }

        if (null != mRecordThread) {
            mRecordThread.interrupt();
            mRecordThread = null;
        }

        mRecordThread = new AudioRecordThread(this, createWav);
        mRecordThread.start();
    }

    /**
     * 停止 录制
     */
    public synchronized void stopRecord() {
        LogUtil.i(TAG, "stopRecord enter...");
        if (!mState.equals(WindState.RECORDING)) {
            LogUtil.w(TAG, "当前没有进行录制，当前状态为：" + mState);
            return;
        }

        mState = WindState.STOP_RECORD;
        notifyState(mState);
    }

    /**
     * 播放 pcm 文件
     */
    public synchronized void startPlayPCM() {
        LogUtil.i(TAG, "startPlayPCM enter...");
        if (!isIdle()) {
            LogUtil.d(TAG, "非空闲状态：当前状态为：" + mState);
            return;
        }

        if (mTmpPCMFile == null) {
            LogUtil.d(TAG, "尚未录制pcm文件...");
            return;
        }

        new AudioTrackPlayThread(mTmpPCMFile).start();
    }

    /**
     * 播放 wav 文件
     */
    public synchronized void startPlayWAV() {
        LogUtil.i(TAG, "startPlayWAV enter...");
        if (!isIdle()) {
            LogUtil.d(TAG, "非空闲状态：当前状态为：" + mState);
            return;
        }

        if (mTmpWAVFile == null) {
            LogUtil.d(TAG, "尚未录制wav文件...");
            return;
        }

        new AudioTrackPlayThread(mTmpWAVFile).start();
    }

    /**
     * 停止播放
     */
    public synchronized void stopPlay() {
        LogUtil.i(TAG, "stopPlay enter...");
        if (!mState.equals(WindState.PLAYING)) {
            LogUtil.d(TAG, "非播放状态-当前状态为：" + mState);
            return;
        }

        mState = WindState.STOP_PLAY;
        notifyState(mState);
    }

    /**
     * 是否空闲状态
     *
     * @return
     */
    public synchronized boolean isIdle() {
        return WindState.IDLE.equals(mState);
    }

    /**
     * 创建 pcm wav 文件
     *
     * @param createWav 是否创建wav文件
     * @throws IOException
     */
    private void createFile(boolean createWav) throws IOException {
        mTmpPCMFile = File.createTempFile("recording", ".pcm", new File(sCachePCMFolder));
        if (createWav) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss", Locale.CHINA);
            mTmpWAVFile = new File(sCacheWAVFolder + File.separator + "r" + sdf.format(new Date()));
        }
    }

    /**
     * @param out            wav音频文件流
     * @param totalAudioLen  不包括header的音频数据总长度
     * @param longSampleRate 采样率：录制时的频率
     * @param channels       audioRecord的频道数量
     * @throws IOException 写文件错误
     */
    void writeWavFileHeader(FileOutputStream out, long totalAudioLen, long longSampleRate, int channels) throws IOException {
        byte[] header = generateWavFileHeader(totalAudioLen, longSampleRate, channels);
        LogUtil.d(TAG, AudioUtil.getHexString(header));
        LogUtil.d(TAG, AudioUtil.getNormalString(header));
        out.write(header, 0, header.length);
    }

    /**
     * 任何一种文件在头部添加相应的头文件才能够确定的表示这种文件的格式，
     * wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE Chunk,
     * FMT Chunk,Fact chunk,Data chunk,其中Fact chunk是可以选择的
     *
     * @param totalAudioLen  不包括header的音频数据总长度
     * @param longSampleRate 采样率：录制时的频率
     * @param channels       audioRecord的频道数量
     * @return wav 头文件
     */
    byte[] generateWavFileHeader(long totalAudioLen, long longSampleRate, int channels) {
        // 不包含前8个字节的WAV文件总长度
        long totalDataLen = totalAudioLen + 36;
        long byteRate = longSampleRate * 2 * channels;
        LogUtil.d(TAG, "totalAudioLen:" + totalAudioLen + "\n totalDataLen:" + totalDataLen +
                "\n longSampleRate:" + longSampleRate + "\n byteRate:" + byteRate);

        byte[] header = new byte[44];
        // RIFF
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';

        // 数据大小
        header[4] = (byte) (totalDataLen & 0XFF);
        header[5] = (byte) ((totalDataLen >> 8) & 0XFF);
        header[6] = (byte) ((totalDataLen >> 16) & 0XFF);
        header[7] = (byte) ((totalDataLen >> 24) & 0XFF);

        // WAVE
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';

        // FMT Chunk 'fmt '
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' '; // 过渡字节

        // 数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;

        // 编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;

        // 通道数
        header[22] = (byte) channels;
        header[23] = 0;

        // 采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0XFF);
        header[25] = (byte) ((longSampleRate >> 8) & 0XFF);
        header[26] = (byte) ((longSampleRate >> 16) & 0XFF);
        header[27] = (byte) ((longSampleRate >> 24) & 0XFF);

        // 音频数据传送速率，采样率 * 通道数 * 采样深度 / 8
        header[28] = (byte) (byteRate & 0XFF);
        header[29] = (byte) ((byteRate >> 8) & 0XFF);
        header[30] = (byte) ((byteRate >> 16) & 0XFF);
        header[31] = (byte) ((byteRate >> 24) & 0XFF);

        // 确定系统一次处理多少个这样的数据，确定缓冲区，通道数 * 采样位数 TODO ?
        header[32] = (byte) (2 * channels);
        header[33] = 0;

        // 每个样本的数据位数
        header[34] = 16;
        header[35] = 0;

        // Data chunk
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0XFF);
        header[41] = (byte) ((totalAudioLen >> 8) & 0XFF);
        header[42] = (byte) ((totalAudioLen >> 16) & 0XFF);
        header[43] = (byte) ((totalAudioLen >> 24) & 0XFF);

        return header;
    }

    /**
     * 更新状态
     *
     * @param currentState 当前状态
     */
    synchronized void notifyState(final WindState currentState) {
        if (null != mOnStateListener) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOnStateListener.onStateChanged(currentState);
                }
            });
        }
    }
}
