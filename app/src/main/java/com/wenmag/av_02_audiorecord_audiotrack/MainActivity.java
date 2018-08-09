package com.wenmag.av_02_audiorecord_audiotrack;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.wenmag.av_02_audiorecord_audiotrack.audio.OnStateListener;
import com.wenmag.av_02_audiorecord_audiotrack.audio.WDAudio;
import com.wenmag.av_02_audiorecord_audiotrack.audio.WindState;
import com.wenmag.av_02_audiorecord_audiotrack.utils.LogUtil;

public class MainActivity extends AppCompatActivity implements OnStateListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private TextView mTvState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
        requestPermission();
    }

    private void initView() {
        mTvState = (TextView) findViewById(R.id.tv_state);
    }

    private void initData() {
        WDAudio.getInstance().setOnStateListener(this);
    }

    public void doStartRecord(View view) {
        if (checkHasPermission(Manifest.permission.RECORD_AUDIO)) {
            WDAudio.getInstance().startRecord(true);
        }
    }

    public void doStopRecord(View view) {
        WDAudio.getInstance().stopRecord();
    }

    public void doPlayPCM(View view) {
        WDAudio.getInstance().startPlayPCM();
    }

    public void doPlayWAV(View view) {
        WDAudio.getInstance().startPlayWAV();
    }

    public void doStopPlay(View view) {
        WDAudio.getInstance().stopPlay();
    }

    @Override
    public void onStateChanged(WindState currentState) {
        mTvState.setText(currentState.toString());
    }

    private void requestPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL)) {
            LogUtil.d(TAG, "存在权限");
            init();
        }
    }

    private void init() {
        WDAudio.init();
    }

    private boolean checkHasPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        LogUtil.d(TAG, "不存在权限：" + permission);
        return false;
    }

    /**
     * 动态检测权限
     *
     * @param permission
     * @param requestCode
     * @return
     */
    @TargetApi(23)
    private boolean checkSelfPermission(String permission, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            LogUtil.d(TAG, "低版本 无需请求权限");
            return true;
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LogUtil.d(TAG, "获取到权限:" + Manifest.permission.RECORD_AUDIO);
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL);
                } else {
                    LogUtil.d(TAG, "No permission for " + Manifest.permission.RECORD_AUDIO);
                }
                break;
            case PERMISSION_REQ_ID_WRITE_EXTERNAL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        init();
                    }
                    LogUtil.d(TAG, "Environment.getExternalStorageState() ：" + Environment.getExternalStorageState());
                } else {
                    LogUtil.d(TAG, "No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
