package com.wenmag.av_02_audiorecord_audiotrack.audio;

import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.wenmag.av_02_audiorecord_audiotrack.contant.AudioConstants;
import com.wenmag.av_02_audiorecord_audiotrack.utils.AudioUtil;
import com.wenmag.av_02_audiorecord_audiotrack.utils.LogUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * desc: AudioRecord 音频录制
 * author: created by zhoujx on 2018/8/8 10:21
 */
class AudioRecordThread extends Thread {
    private static final String TAG = "AudioRecordThread";
    private AudioRecord mAudioRecord;
    private int mBufferSize = 10240;
    private boolean mIsCreateWav;
    private WDAudio mWDAudio;

    AudioRecordThread(WDAudio WDAudio, boolean isCreateWav) {
        mWDAudio = WDAudio;
        mIsCreateWav = isCreateWav;
        int bufferSize = AudioRecord.getMinBufferSize(AudioConstants.AUDIO_FREQUENCY,
                AudioConstants.PLAY_CHANNEL_CONFIG,
                AudioConstants.AUDIO_ENCODING) * AudioConstants.RECODE_AUDIO_BUFFER_TIMES;

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioConstants.AUDIO_FREQUENCY,
                AudioConstants.RECORD_CHANNEL_CONFIG, AudioConstants.AUDIO_ENCODING, bufferSize);
    }

    @Override
    public void run() {
        super.run();
        notifyState(WindState.RECORDING);
        LogUtil.d(TAG, "开始录制");

        writeFile();

        notifyState(WindState.STOP_RECORD);
        notifyState(WindState.IDLE);
        LogUtil.d(TAG, "录制结束");
    }

    /**
     * 创建输出流写入文件
     */
    private void writeFile() {
        FileOutputStream pcmFos = null;
        FileOutputStream wavFos = null;
        try {
            pcmFos = new FileOutputStream(mWDAudio.mTmpPCMFile);
            wavFos = new FileOutputStream(mWDAudio.mTmpWAVFile);
            audioRecordRecording(pcmFos, wavFos);
        } catch (IOException e) {
            recordError(e);
        } finally {
            closeStream(pcmFos, wavFos);
        }
    }

    /**
     * audioRecord 开始录制
     *
     * @param pcmFos
     * @param wavFos
     * @throws IOException
     */
    private void audioRecordRecording(FileOutputStream pcmFos, FileOutputStream wavFos) throws IOException {
        if (mIsCreateWav) {
            mWDAudio.writeWavFileHeader(wavFos, mBufferSize, AudioConstants.AUDIO_FREQUENCY, mAudioRecord.getChannelCount());
        }
        // 开始录制
        mAudioRecord.startRecording();
        // 录音写入文件
        startWriteRecording(pcmFos, wavFos);
        // 录制结束
        mAudioRecord.stop();
        LogUtil.d(TAG, "tmpPCMFile.length:" + mWDAudio.mTmpPCMFile.length());
        if (mIsCreateWav) {
            // 修改header
            modifyWavHeader();
        }
    }

    /**
     * 录音写入文件
     *
     * @param pcmFos
     * @param wavFos
     * @throws IOException 写入文件异常
     */
    private void startWriteRecording(FileOutputStream pcmFos, FileOutputStream wavFos) throws IOException {
        byte[] byteBuffer = new byte[mBufferSize];
        while (mWDAudio.mState.equals(WindState.RECORDING) && !isInterrupted()) {
            int end = mAudioRecord.read(byteBuffer, 0, byteBuffer.length);
            pcmFos.write(byteBuffer, 0, end); // 写入pcm文件
            pcmFos.flush();

            if (mIsCreateWav) {
                wavFos.write(byteBuffer, 0, end); // 写入wav文件
                wavFos.flush();
            }
        }
    }

    /**
     * 修改 wav 头文件
     */
    private void modifyWavHeader() {
        RandomAccessFile wavRaf = null;
        try {
            wavRaf = new RandomAccessFile(mWDAudio.mTmpWAVFile, "rw");
            modifyHeader(wavRaf);
        } catch (IOException e) {
            recordError(e);
        } finally {
            closeAccessFile(wavRaf);
        }
    }

    /**
     * 修改 文件头部
     *
     * @param raf RandomAccessFile 文件写入
     * @throws IOException 文件写入异常
     */
    private void modifyHeader(RandomAccessFile raf) throws IOException {
        byte[] header = mWDAudio.generateWavFileHeader(mWDAudio.mTmpPCMFile.length(),
                AudioConstants.AUDIO_FREQUENCY,
                mAudioRecord.getChannelCount());
        LogUtil.d(TAG, AudioUtil.getHexString(header));
        LogUtil.d(TAG, AudioUtil.getNormalString(header));
        raf.seek(0);
        raf.write(header);
        LogUtil.d(TAG, "tmpWAVFile.length:" + mWDAudio.mTmpWAVFile.length());
    }

    /**
     * 关流
     *
     * @param wavFaf RandomAccessFile
     */
    private void closeAccessFile(RandomAccessFile wavFaf) {
        try {
            if (wavFaf != null) {
                wavFaf.close();
            }
        } catch (IOException e) {
            recordError(e);
        }
    }

    /**
     * 关流 FileOutputStream
     *
     * @param pcmFos pcm输出流
     * @param wavFos wav输出流
     */
    private void closeStream(FileOutputStream pcmFos, FileOutputStream wavFos) {
        try {
            if (pcmFos != null) {
                pcmFos.close();
            }
            if (wavFos != null) {
                wavFos.close();
            }
        } catch (IOException e) {
            recordError(e);
        }
    }

    /**
     * 打印异常
     *
     * @param error
     */
    private void recordError(Exception error) {
        LogUtil.e(TAG, error.getMessage());
        notifyState(WindState.ERROR);
    }

    /**
     * 更新状态
     *
     * @param state {@link WindState}
     */
    private void notifyState(WindState state) {
        mWDAudio.mState = state;
        mWDAudio.notifyState(mWDAudio.mState);
    }
}
