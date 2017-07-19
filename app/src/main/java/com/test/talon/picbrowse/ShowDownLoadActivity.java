package com.test.talon.picbrowse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ShowDownLoadActivity extends Activity {

    private List<PicModel> data;
    @BindView(R.id.img_pic)
    ImageView imgPic;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, ShowDownLoadActivity.class));
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int index = msg.what < data.size() ? msg.what : 0;
            PicModel pic = data.get(index);
            // 2017-04-08 08:00:03
            Glide.with(ShowDownLoadActivity.this).load(data.get(index).getPhoto()).apply(new RequestOptions().dontAnimate().placeholder(imgPic.getDrawable()).skipMemoryCache(true)).into(imgPic);
            mHandler.sendEmptyMessageDelayed(++index, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_show_down_load);
        ButterKnife.bind(this);
        showImg();
    }

    private void showImg() {
        data = new ArrayList<>();
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "PicBrowse");
        if (dir.isDirectory()) {
            List<File> imgList = orderByName(dir.getAbsolutePath());
            for (File file : imgList) {
                if (file.getName().endsWith("jpg")) {
                    PicModel picModel = new PicModel();
                    picModel.setPhoto(file.getAbsolutePath());
                    data.add(picModel);
                }
            }
        }
        mHandler.sendEmptyMessageDelayed(0, 1000);
    }

    public static List<File> orderByName(String fliePath) {
        List<File> files = Arrays.asList(new File(fliePath).listFiles());
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;
                return o1.getName().compareTo(o2.getName());
            }
        });
        return files;
//        for (File f : files) {
//            System.out.println(f.getName());
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }
}
