package com.example.shuftirpo.Singleton;

import android.app.Activity;
import android.content.Context;
import android.webkit.WebView;

import com.example.shuftirpo.Listeners.ShuftiVerifyListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;

public class SetAndGetData {
    private static SetAndGetData data;
    static String consentSup, SECRET_KEY, decryptResponse, decline_reasons, reference,show_results = "1";
    private JSONObject authObject,configObject, RequestObject;
    private ShuftiVerifyListener shuftiVerifyListener;
    private Boolean isWebViewRequest = false;
    static WebView webView;
    static boolean error_occured = true, isConnected = false;
    private io.socket.client.Socket mSocket;
    private List<JSONObject> logsList = new ArrayList<>();
    private String currentScreen = "", sdkKilledTime = "", country, countryCode, socketDisconnectReason = "";
    private boolean isApiCalled = false;
    private Activity activity;
    private Context context;
    private Boolean isAutoCapSocketsConn = false;

    private SetAndGetData() {
    }

    public static SetAndGetData getInstance() {
        if (data == null) {
            data = new SetAndGetData();
        }
        return data;
    }

    public String getSocketDisconnectReason() {
        return socketDisconnectReason;
    }

    public void setSocketDisconnectReason(String reason) {
        if (reason != null) {
            this.socketDisconnectReason = reason;
        }
    }

    public Boolean isAutoCapSocketsConn() {
        return isAutoCapSocketsConn;
    }

    public void setAutoCapSocketsConn(Boolean autoCapSocketsConn) {
        isAutoCapSocketsConn = autoCapSocketsConn;
    }

    public Socket getmSocket() {
        return mSocket;
    }

    public void setmSocket(Socket mSocket) {
        this.mSocket = mSocket;
    }

    public List<JSONObject> getLogsList() {
        return logsList;
    }

    public void setLogsList(List<JSONObject> logsList) {
        this.logsList = logsList;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public ShuftiVerifyListener getShuftiVerifyListener() {
        return shuftiVerifyListener;
    }

    public void setShuftiVerifyListener(ShuftiVerifyListener shuftiVerifyListener) {
        this.shuftiVerifyListener = shuftiVerifyListener;
    }

    public void setAuthObject(JSONObject authObject) {
        this.authObject = authObject;
    }

    public JSONObject getAuthObject() {
        return authObject;
    }

    public void setConfigObject(JSONObject configObject) {
        this.configObject = configObject;
    }

    public JSONObject getConfigObject() {
        return configObject;
    }

    public void setRequestObject(JSONObject requestObject) {
        RequestObject = requestObject;
    }

    public JSONObject getRequestObject() {
        return RequestObject;
    }

    public void setShowResults(String show_results) {
        this.show_results = show_results;
    }

    public String getShowResults() {
        return show_results;
    }

    public Boolean isWebViewRequest() {
        return isWebViewRequest;
    }

    public void setWebViewRequest(Boolean webViewRequest) {
        isWebViewRequest = webViewRequest;
    }

    public WebView getWebView() {
        return webView;
    }
    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public String getSdkKilledTime() {
        return sdkKilledTime;
    }

    public void setSdkKilledTime(String sdkKilledTime) {
        this.sdkKilledTime = sdkKilledTime;
    }

    public boolean getError_occured() {
        return error_occured;
    }

    public void setError_occured(boolean error_occured) {
        this.error_occured = error_occured;
    }

    public String getDecryptResponse() {
        return decryptResponse;
    }

    public void setDecryptResponse(String decryptResponse) {
        this.decryptResponse = decryptResponse;
    }

    public String getCurrentScreen() {
        return currentScreen;
    }

    public void setCurrentScreen(String currentScreen) {
        this.currentScreen = currentScreen;
    }

    public boolean isApiCalled() {
        return isApiCalled;
    }

    public void setApiCalled(boolean apiCalled) {
        isApiCalled = apiCalled;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
