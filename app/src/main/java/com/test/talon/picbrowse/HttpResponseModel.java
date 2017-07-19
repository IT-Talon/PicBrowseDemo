package com.test.talon.picbrowse;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;

import java.io.Serializable;

/**
 * Created by Talon on 2017/7/13.
 */

public class HttpResponseModel implements Serializable {
    private static final long serialVersionUID = 7141541043982367855L;

    /**
     * errcode : 0
     * errmsg : OK
     */

    private int errcode;
    private String errmsg;
    private Object data;

    public int getErrcode() {
        return errcode;
    }

    public void setErrcode(int errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public final <T> T getDataObject(TypeReference<T> typeReference) {
        T obj = null;
        String dataStr = data == null ? null : JSON.toJSONString(data);

        if (!TextUtils.isEmpty(dataStr)) {
            try {
                obj = JSON.parseObject(dataStr, typeReference, new Feature[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return obj;
    }

    public final <T> T getDataObject(Class<T> clazz) {
        T obj = null;
        String dataStr = data == null ? null : JSON.toJSONString(data);

        if (!TextUtils.isEmpty(dataStr)) {
            try {
                obj = JSON.parseObject(dataStr, clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return obj;
    }
}
