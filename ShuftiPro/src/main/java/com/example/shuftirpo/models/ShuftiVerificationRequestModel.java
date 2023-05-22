package com.example.shuftirpo.models;

import android.app.Activity;

import com.example.shuftirpo.Listeners.ShuftiVerifyListener;

import org.json.JSONObject;


public class ShuftiVerificationRequestModel {


    private Activity parentActivity;
    private ShuftiVerifyListener shuftiVerifyListener;
    private boolean asyncRequest;
    private JSONObject jsonObject;
    private JSONObject authObject;
    private JSONObject configObject;
    private boolean isCaptureEnabled = false;

    public ShuftiVerificationRequestModel() {
    }

    public Activity getParentActivity() {
        return parentActivity;
    }

    public void setParentActivity(Activity parentActivity) {
        this.parentActivity = parentActivity;
    }

    public ShuftiVerifyListener getShuftiVerifyListener() {
        return shuftiVerifyListener;
    }

    public void setShuftiVerifyListener(ShuftiVerifyListener shuftiVerifyListener) {
        this.shuftiVerifyListener = shuftiVerifyListener;
    }

    public boolean isAsyncRequest() {
        return asyncRequest;
    }

    public void setAsyncRequest(boolean asyncRequest) {
        this.asyncRequest = asyncRequest;
    }



    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JSONObject getAuthbject() {
        return authObject;
    }

    public void setAuthObject(JSONObject authObject) {
        this.authObject = authObject;
    }

    public JSONObject getConfigObject() {
        return configObject;
    }

    public void setConfigObject(JSONObject configObject) {
        this.configObject = configObject;
    }

    public boolean isCaptureEnabled() {
        return isCaptureEnabled;
    }

    public void setCaptureEnabled(boolean captureEnabled) {
        isCaptureEnabled = captureEnabled;
    }


}
