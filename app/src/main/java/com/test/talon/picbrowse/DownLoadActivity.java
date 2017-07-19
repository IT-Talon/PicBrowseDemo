package com.test.talon.picbrowse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.alibaba.fastjson.TypeReference;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public class DownLoadActivity extends Activity {

    private static final String TAG = DownLoadActivity.class.getSimpleName();

    private List<PicModel> data;
    private String topicId, cameraId, sort;

    private List<Observable<Boolean>> observables;

    @BindView(R.id.img_pic)
    ImageView imgPic;
    @BindView(R.id.progressbar)
    ProgressBar progressbar;

    private static int screenWidth, screenHeight;

    private ImageLoader imageLoader;

    private File dir;

    private int i = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int index = msg.what < data.size() ? msg.what : 0;
            PicModel img = data.get(index);
            // 切记跳过缓存，不然会造成卡顿
            Glide.with(DownLoadActivity.this).load("file://" + img.getPhoto()).apply(new RequestOptions().dontAnimate().placeholder(imgPic.getDrawable()).fitCenter()).into(imgPic);
//            imageLoader.displayImage("file://" + img.getPhoto(), imgPic);
            mHandler.sendEmptyMessageDelayed(++index, 100);
        }
    };

    public static void getScreenWidth(Activity activity) {
        if (screenWidth == 0) {
            WindowManager wm = activity.getWindowManager();
            Point size = new Point();
            wm.getDefaultDisplay().getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
        }

    }

    public static void start(Activity activity, String topicId, String cameraId, String sort) {
        Intent intent = new Intent(activity, DownLoadActivity.class);
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

        getScreenWidth(DownLoadActivity.this);

        setContentView(R.layout.activity_down_load);
        ButterKnife.bind(this);
        topicId = getIntent().getStringExtra(AppConstant.TOPICID);
        cameraId = getIntent().getStringExtra(AppConstant.CAMERAID);
        sort = getIntent().getStringExtra(AppConstant.SORT);

        initView();
        downloadImg();
//        getData();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .build();
        ImageLoader.getInstance().init(config);
        imageLoader = ImageLoader.getInstance(); // Get singleton instance
    }

    private void downloadImg() {
        observables = new ArrayList<>();
        OkHttpClient client = new OkHttpClient.Builder().
                connectTimeout(60, TimeUnit.SECONDS).
                readTimeout(60, TimeUnit.SECONDS).
                writeTimeout(60, TimeUnit.SECONDS).build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://edutest.hzcwtech.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        final RetrofitService service = retrofit.create(RetrofitService.class);
        service.listPics(Integer.valueOf(topicId), cameraId, sort)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<HttpResponseModel>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull HttpResponseModel httpResponseModel) {
                        Log.d(TAG, "-------------onNext");
                        List<PicModel> picList = httpResponseModel.getDataObject(new TypeReference<List<PicModel>>() {
                        });
                        for (int i = 0; i < picList.size(); i++) {
                            if (!picList.get(i).getCreateTime().substring(11, 13).equals("00")) {
//                                final String downloadUrl = "http://edutest.hzcwtech.com//res/images/video_capture/36-330108001056-1490083201-19903.jpg";
                                final String downloadUrl = picList.get(i).getPhoto();
                                observables.add(
                                        service.downloadLatestFeature(downloadUrl)
                                                .subscribeOn(Schedulers.io())
                                                .map(new Function<ResponseBody, Boolean>() {
                                                    @Override
                                                    public Boolean apply(@NonNull ResponseBody responseBody) {
                                                        return writeResponseBodyToDisk(DownLoadActivity.this, downloadUrl, responseBody);
                                                    }
                                                })
                                                .onErrorReturnItem(true) // 10块钱买的一行代码
                                );
                            }
                        }

                        Observable.merge(observables).observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Observer<Boolean>() {
                                    @Override
                                    public void onSubscribe(@NonNull Disposable d) {

                                    }

                                    @Override
                                    public void onNext(@NonNull Boolean aBoolean) {
                                        Log.d(TAG, "-------------下载onNext");
                                        i++;
                                    }

                                    @Override
                                    public void onError(@NonNull Throwable e) {
                                        Log.d(TAG, "-------------下载错误" + e.getMessage()); // 404 NOT FOUND

                                    }

                                    @Override
                                    public void onComplete() {
                                        Log.d(TAG, "-------------下载完成");
                                        showImg();
                                    }
                                });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.e(TAG, "-------------onError" + e.getMessage());

                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "-------------list onComplete");

                    }
                });

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
        progressbar.setVisibility(View.INVISIBLE);
    }


    private void initView() {
        dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "PicBrowse");
        if (!dir.exists()) {
            dir.mkdir();
        } else {
            if (dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    f.delete();
                }
            }
        }
        progressbar.setVisibility(View.VISIBLE);

    }

    // http://edutest.hzcwtech.com/api/camera/photos?topicId=27&cameraId=330104001201&orderBy=asc
    interface RetrofitService {

        @GET("api/camera/photos")
        Observable<HttpResponseModel> listPics(@Query("topicId") int topicId, @Query("cameraId") String cameraId, @Query("orderBy") String orderBy);

        @Streaming
        @GET
        Observable<ResponseBody> downloadLatestFeature(@Url String fileUrl);

    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }


    public boolean writeResponseBodyToDisk(Context context, String fileName, ResponseBody body) {
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        InputStream is = null;
        try {
            is = body.byteStream();
            File file = new File(dir, fileName.substring(fileName.lastIndexOf('/')));
            if (file.exists()) {
//                return true;
                file.delete();
                file = new File(dir, fileName.substring(fileName.lastIndexOf('/')));
            }
            fos = new FileOutputStream(file);
            bis = new BufferedInputStream(is);
            byte[] buffer = new byte[1024 * 1024];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null && bis != null && is != null) {
                try {
                    fos.close();
                    bis.close();
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
}
