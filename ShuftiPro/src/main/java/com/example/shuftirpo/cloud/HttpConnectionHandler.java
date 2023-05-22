package com.example.shuftirpo.cloud;

import static com.example.shuftirpo.Utils.Constants.VERIFICATION_REQUEST_PENDING;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Base64;
import android.util.Log;


import com.example.shuftirpo.Fragments.ResultFragment;
import com.example.shuftirpo.Listeners.NetworkListener;
import com.example.shuftirpo.Listeners.ReferenceResponseListener;
import com.example.shuftirpo.Singleton.SetAndGetData;
import com.example.shuftirpo.Utils.Constants;
import com.example.shuftirpo.Utils.IntentHelper;
import com.example.shuftirpo.Utils.Utils;
import com.example.shuftirpo.models.ShuftiVerificationRequestModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

public class HttpConnectionHandler {

    private static HttpConnectionHandler instance = null;
    private boolean errorOccured = true;
    private String TAG = HttpConnectionHandler.class.getSimpleName();
    private static final String SHUFTIPRO_API_URL = "https://api.shuftipro.com/";
    //Staging URL
//    private static final String SHUFTIPRO_API_URL = "https://st-api.shuftiapps.com/";
    private static final String SHUFTIPRO_STATUS_API_URL = "https://api.shuftipro.com/sdk/request/status/";
//    Production URL
    private static final String SHUFTIPRO_ERROR_API_URL = "https://api.shuftipro.com/v3/sdk/error/report";
    //    Testing URL
//    private static final String SHUFTIPRO_ERROR_API_URL = "https://wi.shuftiapps.com/api/webhooks/62e75de700e59e187206ae74";


    private String CLIENT_ID;
    private String SECRET_KEY;
    private String ACCESS_TOKEN;
    private InputStream inputStream = null;
    private boolean asyncRequest;
    private ShuftiVerificationRequestModel shuftiVerificationRequestModel;
    HashMap<String, String> responseSet;

//    public HttpConnectionHandler(String clientId, String secretKey, String accessToken) {
//        this.CLIENT_ID = clientId;
//        this.SECRET_KEY = secretKey;
//        this.ACCESS_TOKEN = accessToken;
//    }
//
//
//    public static HttpConnectionHandler getInstance(String clientId, String secretKey, String accessToken) {
//
//        instance = new HttpConnectionHandler(clientId, secretKey, accessToken);
//        return instance;
//    }

    public HttpConnectionHandler() {
        try {
            if (SetAndGetData.getInstance().getAuthObject().has("client_Id"))
                CLIENT_ID = SetAndGetData.getInstance().getAuthObject().getString("client_Id");
            if (SetAndGetData.getInstance().getAuthObject().has("secret_key"))
                SECRET_KEY = SetAndGetData.getInstance().getAuthObject().getString("secret_key");
            if (SetAndGetData.getInstance().getAuthObject().has("access_token"))
                ACCESS_TOKEN = SetAndGetData.getInstance().getAuthObject().getString("access_token");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static HttpConnectionHandler getInstance() {
        instance = new HttpConnectionHandler();
        return instance;
    }

    public static HttpConnectionHandler getInstance(String clientId, String secretKey,String accessToken , boolean asyncRequest) {
        instance = new HttpConnectionHandler(clientId, secretKey, accessToken , asyncRequest);
        return instance;
    }

    public HttpConnectionHandler(String clientId, String secretKey,String accessToken , boolean asyncRequest) {
        this.CLIENT_ID = clientId;
        this.SECRET_KEY = secretKey;
        this.ACCESS_TOKEN = accessToken;
        this.asyncRequest = asyncRequest;
    }

    @SuppressLint("StaticFieldLeak")
    public boolean executeVerificationRequest(final ResultFragment resultFragment, final JSONObject requestedObject,
                                              final NetworkListener networkListener, final Context context) {
        Log.e(TAG, requestedObject.toString());
        if (networkAvailable(context) && Utils.isNetworkOnline(context)) {
            new AsyncTask<Void, Integer, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    String resultResponse = "";
                    try {
                        URL url = new URL(SHUFTIPRO_API_URL);
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setAllowUserInteraction(false);
                        connection.setRequestProperty("Connection", "Keep-Alive");
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                        connection.setRequestProperty("Accept", "application/json");
                        connection.setChunkedStreamingMode(1024);

                        String cred = "";
                        if (CLIENT_ID == null || CLIENT_ID.isEmpty() || SECRET_KEY == null || SECRET_KEY.isEmpty()) {
                            cred = bearer(ACCESS_TOKEN);
                        } else {
                            cred = basic(CLIENT_ID, SECRET_KEY);

                        }
                        Utils.sendLog("SPMOB5", null);

                        connection.setRequestProperty("Authorization", cred);
                        connection.connect();

                        DataOutputStream os = new DataOutputStream(connection.getOutputStream());

                        byte[] payload = requestedObject.toString().getBytes("UTF-8");

                        int progressPercent = 0;
                        int offset = 0;
                        int bufferLength = payload.length / 100;
                        while (progressPercent < 100) {
                            os.write(payload, offset, bufferLength);
                            offset += bufferLength;
                            ++progressPercent;
                            this.publishProgress(new Integer[]{progressPercent});
                        }
                        os.write(payload, offset, payload.length % 100);
                        os.flush();
                        os.close();

                        int responseCode = ((HttpsURLConnection) connection).getResponseCode();
                        if ((responseCode >= HttpsURLConnection.HTTP_OK)
                                && responseCode < 300) {
                            inputStream = connection.getInputStream();
                            errorOccured = false;
                            resultResponse = inputStreamToString(inputStream);
                            Log.d(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 400) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 401) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 402) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 403) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 404) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 409) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 503) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        }
                        Utils.sendLog("SPMOB6", "Response code:" + responseCode);

                    } catch (Exception e) {
                        Log.e("TAG1", e.toString());
                        e.printStackTrace();
                        resultResponse = e.getMessage();

                        if (resultResponse != null) {
                            if (resultResponse.contains("error during system call, Software caused connection abort")) {
                                resultResponse = "Internet Connection Failed";
                            }
                        }
                        return resultResponse;
                    }
                    return resultResponse;
                }
                protected void onProgressUpdate(Integer... values) {
                    super.onProgressUpdate(values);
                    resultFragment.handleProgress(values[0]);
                    if (values[0] == 100) {
                        resultFragment.handleProgress(100);
                    }
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    try {
                        if (result == null) {
                            result = "";
                        }
                        if (result.isEmpty()) {
                            Utils.sendLog("SPMOB16", "");
                            networkListener.errorResponse(result);
                        } else {
                            JSONObject jsonObj = new JSONObject(result);
                            if (jsonObj.has("event")) {
                                if (SetAndGetData.getInstance().getActivity() != null && !asyncRequest) {

                                    if (networkListener != null) {
                                        if (!errorOccured) {
                                            networkListener.successResponse(result);
                                        } else {
                                            networkListener.errorResponse(result);
                                        }
                                    }
                                } else {

                                    String event = "";
                                    try {
                                        JSONObject jsonObject1 = new JSONObject(result);
                                        event = jsonObject1.getString("event");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    if (event.equalsIgnoreCase(VERIFICATION_REQUEST_PENDING)) {
                                        if (networkListener != null) {
                                            if (!errorOccured) {
                                                networkListener.successResponse(result);
                                            } else {
                                                networkListener.errorResponse(result);
                                            }
                                        }
                                    } else {
                                        if (IntentHelper.getInstance().containsKey(Constants.KEY_DATA_MODEL)) {
                                            shuftiVerificationRequestModel = (ShuftiVerificationRequestModel) IntentHelper.getInstance().getObject(Constants.KEY_DATA_MODEL);
                                            sendCallbackToCallerActivity(result);
                                        }
                                    }

                                    //This is an async request check the VerificationListener class listener and return response


                                }
                            } else if (jsonObj.has("e_data")) {
                                Utils.callJsDecrypt(context, SetAndGetData.getInstance().getWebView(), jsonObj.getString("e_data"));
                            } else {
                                networkListener.errorResponse(result);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }.execute();
            return true;

        } else {
            Log.e("TAG1", "Network not available");
            return false;
        }
    }

    private void sendCallbackToCallerActivity(String response) {

        responseSet = new HashMap<>();
        responseSet.clear();
        if (response == null) {

            Log.e(TAG, "Response is null");
        }

        populateMap(response);
    }

    private void populateMap(String response) {

        //Putting response in hash map
        try {

            JSONObject jsonObject = new JSONObject(response);

            //Starting api response parsing
            String reference = "";
            String event = "";
            String error = "";
            String verification_url = "";
            String verification_result = "";
            String verification_data = "";
            String declined_reason = "";

            if (jsonObject.has("reference")) {
                try {
                    reference = jsonObject.getString("reference");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("event")) {
                try {
                    event = jsonObject.getString("event");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("error")) {
                try {
                    JSONObject errorObject = new JSONObject(jsonObject.getString("error"));
                    error = errorObject.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("verification_url")) {
                try {
                    verification_url = jsonObject.getString("verification_url");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("verification_result")) {
                try {
                    verification_result = jsonObject.getString("verification_result");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("verification_data")) {
                try {
                    verification_data = jsonObject.getString("verification_data");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("declined_reason")) {
                try {
                    declined_reason = jsonObject.getString("declined_reason");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            //Putting response in hash map
            responseSet.put("reference", reference);
            responseSet.put("event", event);
            responseSet.put("error", error);
            responseSet.put("verification_url", verification_url);
            responseSet.put("verification_result", verification_result);
            responseSet.put("verification_data", verification_data);
            responseSet.put("declined_reason", declined_reason);

            if (shuftiVerificationRequestModel != null && shuftiVerificationRequestModel.getShuftiVerifyListener() != null) {
                shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public static boolean networkAvailable(Context context) {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;

            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }

        if (!haveConnectedWifi)
            Log.e("NetworkInfo", "No WIFI Available");
        else
            Log.e("NetworkInfo", "WIFI Available");
        if (!haveConnectedMobile)
            Log.e("NetworkInfo", "No Mobile Network Available");
        else
            Log.e("NetworkInfo", "Mobile Network Available");

        return haveConnectedWifi || haveConnectedMobile;
    }

    private static String basic(String username, String password) {
        String usernameAndPassword = username + ":" + password;
        String encoded = Base64.encodeToString((usernameAndPassword).getBytes(), Base64.NO_WRAP);
        return "Basic " + encoded;
    }

    private static String bearer(String access_token) {
        return "Bearer " + access_token;
    }

    private static String inputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder out = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        reader.close();
        return out.toString();
    }

    // send crash reports to whistle it channel
    @SuppressLint("StaticFieldLeak")
    public void sendStacktraceReport(final Context context, final String threadName, final Exception stackTrace,
                                     final String message, final String deviceInformation, final String timeStamp, final String sdkVersion,
                                     final String exceptionClassname, final String stackTraceMessage, final String configObject,
                                     final String requestObject) {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                String resultResponse = "";
                try {
                    URL url = new URL(SHUFTIPRO_ERROR_API_URL);
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setAllowUserInteraction(false);
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setConnectTimeout(90000);
                    connection.setReadTimeout(90000);

                    connection.setRequestMethod("POST");
                    String crashPath = "";
                    for (int i = 0; i < stackTrace.getStackTrace().length; i++) {
                        crashPath = crashPath + stackTrace.getStackTrace()[i] + "\n";
                    }

                    Uri.Builder builder = new Uri.Builder()
                            .appendQueryParameter("configObject", configObject)
                            .appendQueryParameter("requestObject", requestObject)
                            .appendQueryParameter("threadName", threadName)
                            .appendQueryParameter("stackTrace", crashPath)
                            .appendQueryParameter("message", message)
                            .appendQueryParameter("deviceInformation", deviceInformation)
                            .appendQueryParameter("SDKVersion", sdkVersion)
                            .appendQueryParameter("timeStamp", timeStamp)
                            .appendQueryParameter("ExceptionClassName", exceptionClassname)
                            .appendQueryParameter("stackTraceMessage", stackTraceMessage);

                    Log.e("REQUEST", builder.toString());
                    String query = builder.build().getEncodedQuery();

                    byte[] outputBytes = query.getBytes("UTF-8");

                    OutputStream os = connection.getOutputStream();
                    os.write(outputBytes);
                    os.close();

                    connection.connect();
                    int responseCode = ((HttpsURLConnection) connection).getResponseCode();
                    if ((responseCode >= HttpsURLConnection.HTTP_OK)
                            && responseCode < 300) {
                        Log.e("REQUEST", "Response: " + resultResponse);
                        inputStream = connection.getInputStream();
                        ((Activity) context).finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return resultResponse;
                }
                return resultResponse;
            }

            protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                try {
                    SetAndGetData.getInstance().getActivity().finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    public boolean decryption(NetworkListener networkListener) {

        String result = SetAndGetData.getInstance().getDecryptResponse().substring(16);

        if (SetAndGetData.getInstance().getActivity() != null && !asyncRequest) {

            if (networkListener != null) {
                if (!SetAndGetData.getInstance().getError_occured()) {
                    networkListener.successResponse(result);
                } else {
                    networkListener.errorResponse(result);
                }
            }
        } else {

            String event = "";
            try {
                JSONObject jsonObject = new JSONObject(result);
                event = jsonObject.getString("event");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (event.equalsIgnoreCase(VERIFICATION_REQUEST_PENDING)) {
                if (networkListener != null) {
                    if (!errorOccured) {
                        networkListener.successResponse(result);
                    } else {
                        networkListener.errorResponse(result);
                    }
                }
            } else {
                if (IntentHelper.getInstance().containsKey(Constants.KEY_DATA_MODEL)) {
                    shuftiVerificationRequestModel = (ShuftiVerificationRequestModel) IntentHelper.getInstance().getObject(Constants.KEY_DATA_MODEL);
                    sendCallbackToCallerActivity(result);
                }
            }
        }
        return true;
    }

    @SuppressLint("StaticFieldLeak")
    public void sendAutoCaptureWebhook(final String doctype, final String country,
                                       final String deviceInformation, final String timeStamp, final String reference) {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                String resultResponse = "";
                try {
                    URL url = new URL(SHUFTIPRO_ERROR_API_URL);
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setAllowUserInteraction(false);
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setConnectTimeout(90000);
                    connection.setReadTimeout(90000);


                    Uri.Builder builder = new Uri.Builder()
                            .appendQueryParameter("platform", "Android")
                            .appendQueryParameter("reference", reference)
                            .appendQueryParameter("deviceInformation", deviceInformation)
                            .appendQueryParameter("documentType", doctype)
                            .appendQueryParameter("country", country)
                            .appendQueryParameter("timeStamp", timeStamp);

                    Log.e("REQUEST", builder.toString());
                    String query = builder.build().getEncodedQuery();

                    byte[] outputBytes = query.getBytes("UTF-8");

                    OutputStream os = connection.getOutputStream();
                    os.write(outputBytes);
                    os.close();

                    connection.connect();
                    int responseCode = ((HttpsURLConnection) connection).getResponseCode();
                } catch (Exception e) {
                    e.printStackTrace();
                    return resultResponse;
                }
                return resultResponse;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
            }
        }.execute();
    }

}