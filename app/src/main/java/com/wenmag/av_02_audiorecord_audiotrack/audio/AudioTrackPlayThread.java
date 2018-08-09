package com.wenmag.av_02_audiorecord_audiotrack.audio;

import android.media.AudioManager;
import android.media.AudioTrack;

import com.wenmag.av_02_audiorecord_audiotrack.contant.AudioConstants;
import com.wenmag.av_02_audiorecord_audiotrack.utils.LogUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * desc: 播放线程
 * author: created by zhoujx on 2018/8/8 09:15
 */
class AudioTrackPlayThread extends Thread {
    private static final String TAG = "AudioTrackPlayThread";
    private AudioTrack mAudioTrack;
    private int mBufferSize = 10240;
    private File mAudioFile;

    AudioTrackPlayThread(File audioFile) {
        setPriority(Thread.MAX_PRIORITY);
        mAudioFile = audioFile;

        int bufferSize = AudioTrack.getMinBufferSize(AudioConstants.AUDIO_FREQUENCY,
                AudioConstants.PLAY_CHANNEL_CONFIG,
                AudioConstants.AUDIO_ENCODING) * AudioConstants.PLAY_AUDIO_BUFFER_TIMES;

        try {
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    AudioConstants.AUDIO_FREQUENCY,
                    AudioConstants.PLAY_CHANNEL_CONFIG,
                    AudioConstants.AUDIO_ENCODING,
                    bufferSize,
                    AudioTrack.MODE_STREAM);

        } catch (IllegalArgumentException e) {
            LogUtil.e(TAG, e.getMessage());
        }
    }

    @Override
    public void run() {
        super.run();
        notifyState(WindState.PLAYING);
        LogUtil.d(TAG, "开始播放音频:" + (mAudioFile == null ? "音频文件不存在" : mAudioFile.getName()));
        readFile();
        notifyState(WindState.STOP_PLAY);
        notifyState(WindState.IDLE);
        LogUtil.d(TAG, "音频播放停止");
    }

    /**
     * 创建输入流读取文件
     */
    private void readFile() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(mAudioFile);
            audioTrackPlaying(fis);
        } catch (IOException e) {
            runError(e);
        } finally {
            closeStream(fis);
        }
    }

    /**
     * audioTrack 开始播放
     *
     * @param fis 输入流
     * @throws IOException 文件读取异常
     */
    private void audioTrackPlaying(FileInputStream fis) throws IOException {
        mAudioTrack.play();
        byte[] byteBuffer = new byte[mBufferSize];
        while (WDAudio.getInstance().mState.equals(WindState.PLAYING) && fis.read(byteBuffer) >= 0) {
            mAudioTrack.write(byteBuffer, 0, byteBuffer.length);
        }
        mAudioTrack.stop();
        mAudioTrack.release();
    }

    /**
     * 关流 FileInputStream
     *
     * @param fis 输入流
     */
    private void closeStream(FileInputStream fis) {
        try {
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e) {
            runError(e);
        }
    }

    /**
     * 打印异常
     *
     * @param error
     */
    private void runError(Exception error) {
        LogUtil.e(TAG, error.getMessage());
        notifyState(WindState.ERROR);
    }

    /**
     * 更新状态
     *
     * @param state {@link WindState}
     */
    private void notifyState(WindState state) {
        WDAudio.getInstance().mState = state;
        WDAudio.getInstance().notifyState(WDAudio.getInstance().mState);
    }
}
