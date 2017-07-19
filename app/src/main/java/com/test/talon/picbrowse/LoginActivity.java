package com.test.talon.picbrowse;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.topicID)
    EditText topicID;
    @BindView(R.id.cameraID)
    EditText cameraID;
    @BindView(R.id.sort)
    EditText sort;
    @BindView(R.id.btn_show)
    Button btnShow;
    @BindView(R.id.btn_download)
    Button btnDownload;
    @BindView(R.id.btn_showDownload)
    Button btnShowDownload;

    private String mTopicId, mCameraId, mSort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
    }

    private void getData() {
        mTopicId = topicID.getText().toString().trim();
        mCameraId = cameraID.getText().toString().trim();
        mSort = sort.getText().toString().trim();
    }

    @OnClick({R.id.btn_show, R.id.btn_download, R.id.btn_showDownload})
    public void onViewClicked(View view) {
        getData();
        switch (view.getId()) {
            case R.id.btn_show:
                MainActivity.start(LoginActivity.this, mTopicId, mCameraId, mSort);
                break;
            case R.id.btn_download:
                DownLoadActivity.start(LoginActivity.this, mTopicId, mCameraId, mSort);
                break;
            case R.id.btn_showDownload:
                ShowDownLoadActivity.start(LoginActivity.this);
                break;
        }
    }
}
