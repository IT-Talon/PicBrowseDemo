package com.test.talon.picbrowse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.alibaba.fastjson.TypeReference;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private List<PicModel> data;
    private String topicId, cameraId, sort;

    @BindView(R.id.img_pic)
    ImageView imgPic;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case 1:
            int index = msg.what < data.size() ? msg.what : 0;
            PicModel pic = data.get(index);
            // 2017-04-08 08:00:03
            if (!pic.getCreateTime().substring(11, 13).equals("00")) {
                Glide.with(MainActivity.this).load(data.get(index).getPhoto()).apply(new RequestOptions().dontAnimate().placeholder(imgPic.getDrawable()).skipMemoryCache(true)).into(imgPic);
                mHandler.sendEmptyMessageDelayed(++index, 100);
            } else {
                mHandler.sendEmptyMessage(++index);
//                    }
//                case 2:
//                    String localPath = msg.getData().getString("LocalPath");
//                    Glide.with(MainActivity.this).load(localPath).apply(new RequestOptions().dontAnimate().placeholder(imgPic.getDrawable())).into(imgPic);
//                    break;
            }
        }
    };

    public static void start(Activity activity, String topicId, String cameraId, String sort) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra(AppConstant.TOPICID, topicId);
        intent.putExtra(AppConstant.CAMERAID, cameraId);
        intent.putExtra(AppConstant.SORT, sort);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        topicId = getIntent().getStringExtra(AppConstant.TOPICID);
        cameraId = getIntent().getStringExtra(AppConstant.CAMERAID);
        sort = getIntent().getStringExtra(AppConstant.SORT);
        getData();
        initView();
    }

    private void getData() {
        data = new ArrayList<>();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://edutest.hzcwtech.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitService service = retrofit.create(RetrofitService.class);
//        final Call<HttpResponseModel> repos = service.listPics(27, "330104001201", "asc");
        final Call<HttpResponseModel> repos = service.listPics(Integer.valueOf(topicId), cameraId, sort);
        repos.enqueue(new Callback<HttpResponseModel>() {
            @Override
            public void onResponse(Call<HttpResponseModel> call, Response<HttpResponseModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PicModel> picList = response.body().getDataObject(new TypeReference<List<PicModel>>() {
                    });
                    data.clear();
                    data.addAll(picList);
                    mHandler.sendEmptyMessage(0);
//                    downloadImg(data.get(0).getPhoto());
                }
            }

            @Override
            public void onFailure(Call<HttpResponseModel> call, Throwable t) {

            }
        });
    }


    private void initView() {
    }

    // http://edutest.hzcwtech.com/api/camera/photos?topicId=27&cameraId=330104001201&orderBy=asc
    interface RetrofitService {
        @GET("api/camera/photos")
        Call<HttpResponseModel> listPics(@Query("topicId") int topicId, @Query("cameraId") String cameraId, @Query("orderBy") String orderBy);

        @GET("api/camera/photos")
        Call<HttpResponseModel> listPics2();

        @Streaming
        @GET
        Call<ResponseBody> downloadLatestFeature(@Url String fileUrl);
    }

    private void downloadImg(final String imgUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://edutest.hzcwtech.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitService service = retrofit.create(RetrofitService.class);
//        final Call<HttpResponseModel> repos = service.listPics(27, "330104001201", "asc");
        final Call<ResponseBody> repos = service.downloadLatestFeature(imgUrl);
        repos.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                writeResponseBodyToDisk(MainActivity.this, imgUrl, response.body());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });


    }

    public void writeResponseBodyToDisk(Context context, String url, ResponseBody body) {
        try {
            InputStream is = body.byteStream();
//            File file = new File(path, "download.jpg");
            String suffix = url.substring(url.lastIndexOf("."));
            String path = context.getFilesDir() + File.separator + "loadingImg";
            File fileDr = new File(path);
            if (!fileDr.exists()) {
                fileDr.mkdir();
            }
            File file = new File(path, "loadingImg" + suffix);
            if (file.exists()) {
                file.delete();
                file = new File(path, "loadingImg" + suffix);
            }
            FileOutputStream fos = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            Toast.makeText(context, "下载完成", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "下载完成" + file.getAbsolutePath());
            Message msg = mHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("LocalPath", file.getAbsolutePath());
            msg.setData(bundle);
            msg.what = 2;
            mHandler.sendMessage(msg);
            fos.flush();
            fos.close();
            bis.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }
}
