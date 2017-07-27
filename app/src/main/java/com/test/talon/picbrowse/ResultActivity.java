package com.test.talon.picbrowse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.alibaba.fastjson.TypeReference;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

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
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
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

public class ResultActivity extends Activity {
    private static final String TAG = ResultActivity.class.getSimpleName();

    @BindView(R.id.img_pic)
    ImageView imgPic;
    @BindView(R.id.progressbar)
    ProgressBar progressbar;

    private String topicId, cameraId, sort;

    private File dir;

    private RetrofitService service;

    private List<Observable<String>> observables;

    private boolean mIsLoop, isDownloading;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mIsLoop) {
                int index = msg.what < mShowImgPath.size() ? msg.what : 0;
                Log.d("Talon", "--------------开始循环播放了 + index: " + index + "     size: " + mShowImgPath.size());
                String path = mShowImgPath.get(index);
                Glide.with(ResultActivity.this).load("file://" + path).apply(new RequestOptions().dontAnimate().placeholder(imgPic.getDrawable())).into(imgPic);
                mHandler.sendEmptyMessageDelayed(++index, 300);
            } else {
                int index = msg.what;
                if (index < mShowImgPath.size() - 1) {
                    Log.d("Talon", "--------------播放  index: " + index + "     size: " + mShowImgPath.size());
                    progressbar.setVisibility(View.INVISIBLE);
                    String path = mShowImgPath.get(index);
                    Glide.with(ResultActivity.this).load("file://" + path).apply(new RequestOptions().dontAnimate().placeholder(imgPic.getDrawable())).into(imgPic);
                    mHandler.sendEmptyMessageDelayed(++index, 300);
                    if (!isDownloading) {
                        mIsLoop = true;
                        Log.d("Talon", "         下载结束，改变loop状态");
                    }
                } else {
                    Glide.with(ResultActivity.this).load("file://" + mShowImgPath.get(index)).apply(new RequestOptions().dontAnimate().placeholder(imgPic.getDrawable())).into(imgPic);
                    progressbar.setVisibility(View.VISIBLE);
                    index = mShowImgPath.size() - 1;
                    Log.d("Talon", "      没东西放了，放最后一张--------index: " + index + "     size: " + mShowImgPath.size());
                    mHandler.sendEmptyMessageDelayed(index, 300);
                }
            }
        }
    };

    public static void start(Activity activity, String topicId, String cameraId, String sort) {
        Intent intent = new Intent(activity, ResultActivity.class);
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
        setContentView(R.layout.activity_result);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.get(ResultActivity.this).clearMemory();
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(ResultActivity.this).clearDiskCache();
            }
        }).start();

        ButterKnife.bind(this);
        topicId = getIntent().getStringExtra(AppConstant.TOPICID);
        cameraId = getIntent().getStringExtra(AppConstant.CAMERAID);
        sort = getIntent().getStringExtra(AppConstant.SORT);

        initView();
        downloadImg();
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

        service = retrofit.create(RetrofitService.class);
//        service.listPics(Integer.valueOf(topicId), cameraId, sort)
        service.listAllPics()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<HttpResponseModel>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull HttpResponseModel httpResponseModel) {
                        List<PicModel> picList = httpResponseModel.getDataObject(new TypeReference<List<PicModel>>() {
                        });
                        downloadAllImg(picList.subList(800, 1100));
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                    }
                });

    }


    private List<List<PicModel>> mAllDownLists; // 下载分组 <330 4组 100,100,100,30>
    private int downListsIndex = 0; // 当前下载分组下标 <0,1,2,3>
    private List<String> mShowImgPath; // 播放图片地址list
    private List<String> mAllImgPath;

    private void downloadAllImg(List<PicModel> picList) {
        mAllImgPath = new ArrayList<>();
        mAllDownLists = new ArrayList<>();
        mShowImgPath = new ArrayList<>();
        if (picList.size() > 100) {
            int allSize = picList.size();
            int childNum = allSize / 100 + 1;
            for (int i = 0; i < childNum; i++) {
                List<PicModel> l = new ArrayList<>();
                if (i == childNum - 1) {
                    l.addAll(picList.subList(i * 100, picList.size()));
                } else {
                    l.addAll(picList.subList(i * 100, (i + 1) * 100));
                }
                mAllDownLists.add(l);
            }

            downLoadChildList(mAllDownLists.get(0)); // 开始下载第一组

        } else {
            for (int i = 0; i < picList.size(); i++) {
                if (!picList.get(i).getCreateTime().substring(11, 13).equals("00")) {
//                                final String downloadUrl = "http://edutest.hzcwtech.com//res/images/video_capture/36-330108001056-1490083201-19903.jpg";
                    final String downloadUrl = picList.get(i).getPhoto();
                    observables.add(
                            service.downloadLatestFeature(downloadUrl)
                                    .subscribeOn(Schedulers.io())
                                    .map(new Function<ResponseBody, String>() {
                                        @Override
                                        public String apply(@NonNull ResponseBody responseBody) {
                                            return writeResponseBodyToDisk(downloadUrl, responseBody);
                                        }
                                    })
                                    .onErrorReturnItem("") // 10块钱买的一行代码
                    );
                }
            }

            Observable.merge(observables).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<String>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull String s) {
                            Log.d(TAG, "-------------下载onNext");
                            if (!TextUtils.isEmpty(s))
                                mShowImgPath.add(s);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.d(TAG, "-------------下载错误" + e.getMessage()); // 404 NOT FOUND
                        }

                        @Override
                        public void onComplete() {
                            Log.d(TAG, "-------------下载完成");
                            Collections.sort(mShowImgPath);
                            showImg(true);
                        }
                    });
        }

    }

    private void downLoadChildList(final List<PicModel> downLists) {
        isDownloading = true;
        List<Observable<String>> observables = new ArrayList<>();   // 子list下载队列
        final List<String> childImgPath = new ArrayList<>();  // 子list下载完成后的地址
        for (int i = 0; i < downLists.size(); i++) {
            if (!downLists.get(i).getCreateTime().substring(11, 13).equals("00")) {
//                                final String downloadUrl = "http://edutest.hzcwtech.com//res/images/video_capture/36-330108001056-1490083201-19903.jpg";
                final String downloadUrl = downLists.get(i).getPhoto();
                observables.add(
                        service.downloadLatestFeature(downloadUrl)
                                .subscribeOn(Schedulers.io())
                                .map(new Function<ResponseBody, String>() {
                                    @Override
                                    public String apply(@NonNull ResponseBody responseBody) {
                                        return writeResponseBodyToDisk(downloadUrl, responseBody);
                                    }
                                })
                                .onErrorReturnItem("") // 10块钱买的一行代码
                );
            }
        }

        Observable.merge(observables).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull String s) {
                        Log.d(TAG, "-------------下载onNext");
                        if (!TextUtils.isEmpty(s))  // 排除地址为“ null ”情况
                            childImgPath.add(s);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "-------------下载错误" + e.getMessage()); // 404 NOT FOUND
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "-------------下载完成");
                        Collections.sort(childImgPath);
                        mShowImgPath.addAll(childImgPath);
                        if (downListsIndex == 0) {
                            showImg(false);
                        }
                        downListsIndex++;
                        if (downListsIndex < mAllDownLists.size()) {
                            Log.d("Talon", "-------------index" + downListsIndex + "------------size" + mAllDownLists.size());
//                            showImg(childImgPath, false);
                            downLoadChildList(mAllDownLists.get(downListsIndex));
                        } else {
                            Log.d("Talon", "下载结束");
                            isDownloading = false;
                        }
                    }
                });
    }


    private void showImg(boolean loop) {
        mIsLoop = loop;
        progressbar.setVisibility(View.INVISIBLE);
        mHandler.sendEmptyMessageDelayed(0, 1000);
    }

    public String writeResponseBodyToDisk(String fileName, ResponseBody body) {
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
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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

    private void initView() {
        dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "PicBrowse2");
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

        @GET("api/camera/photos")
        Observable<HttpResponseModel> listAllPics();

        @Streaming
        @GET
        Observable<ResponseBody> downloadLatestFeature(@Url String fileUrl);

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }
}
