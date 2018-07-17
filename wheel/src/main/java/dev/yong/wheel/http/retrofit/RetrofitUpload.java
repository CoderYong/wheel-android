package dev.yong.wheel.http.retrofit;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

import dev.yong.wheel.http.UploadFile;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;

/**
 * @author CoderYong
 */

public class RetrofitUpload extends RetrofitCall {

    private UploadService mService;
    private String mUrl;
    private Map<String, String> mParameters;
    private List<UploadFile> mFiles;

    RetrofitUpload(UploadService service, String url, Map<String, String> parameters, UploadFile... files) {
        this.mService = service;
        this.mUrl = url;
        this.mParameters = parameters;
        this.mFiles = Arrays.asList(files);
    }

    @Override
    protected Call<ResponseBody> getCall() {
        return null;
    }

    @Override
    protected Call<ResponseBody> postCall() {
        MultipartBody multipartBody = filesToMultipartBody(mFiles);
        return mParameters != null && mParameters.size() > 0 ?
                mService.upload(mUrl, mParameters, multipartBody) : mService.upload(mUrl, multipartBody);
    }

    private MultipartBody filesToMultipartBody(List<UploadFile> files) {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        if (files != null && files.size() > 0) {
            for (UploadFile file : files) {
                RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file.file);
                builder.addFormDataPart(file.partName, file.fileName, requestBody);
            }
        } else {
            builder.addFormDataPart("", "");
        }
        builder.setType(MultipartBody.FORM);
        return builder.build();
    }
}