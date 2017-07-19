package com.test.talon.picbrowse;

import java.io.Serializable;

/**
 * Created by Talon on 2017/7/13.
 */

public class PicModel implements Serializable {
    private static final long serialVersionUID = 2362986116304661714L;

    /**
     * createTime : 2017-04-08 00:00:03
     * photo : http://edutest.hzcwtech.com//res/images/video_capture/27-330104001201-1491580801-10937.jpg
     */

    private String createTime;
    private String photo;

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
