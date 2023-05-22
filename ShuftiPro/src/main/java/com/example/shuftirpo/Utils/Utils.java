package com.example.shuftirpo.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Button;


import com.example.shuftirpo.R;
import com.example.shuftirpo.Singleton.SetAndGetData;
import com.example.shuftirpo.cloud.HttpConnectionHandler;
import com.example.shuftirpo.models.ShuftiVerificationRequestModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Utils {

    private HashMap<String, String> responseSet = new HashMap<>();
    private static String uniqueID = null;
    private ShuftiVerificationRequestModel shuftiVerificationRequestModel;


    /**
     * method to get unique reference
     */
    public synchronized static String getUniqueReference(Context context) {
        long milliseconds = System.currentTimeMillis();
        String timeStamp = Long.toString(milliseconds);
        uniqueID = timeStamp + "-" + getSDKVersion();
        return uniqueID;
    }

    /**
     * This method is used to check limited internet even when internet is connected
     *
     * @param context
     * @return
     */
    public static boolean isNetworkOnline(Context context) {
        boolean isOnline = false;
        try {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());  // need ACCESS_NETWORK_STATE permission
            isOnline = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isOnline;
    }

    /**
     * This method is used to check the mobile data and wifi connectivity
     *
     * @param context points to the context of the activity which calls this method
     * @return return boolean value regarding internet connection
     */
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

    /**
     * method to get sdk version
     *
     * @return returning sdk version
     */
    public static String getSDKVersion() {
        return "1.0.2-android_core";
    }


    /**
     * initiate source of the sdk
     * @return returns the version of the sdk
     */
    public static String initiatedSdkVersion(){
        return  "1.0.2";
    }

    /**
     * initiate source of the sdk
     * @return returns the name of the sdk
     */
    public static String initiatedSdkType(){
        return  "android_core";
    }

    /**
     * converting token to SHA 256
     */
    public static String sha256(String token) {
        try {
            // Create SHA-256 Hash
            MessageDigest digest = MessageDigest
                    .getInstance("SHA-256");
            digest.update(token.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * This method is used to check the device information when sending stackTraceReport
     *
     * @return This method returns information regarding manufacturer and model
     */
    public static String getDeviceInformation() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    /**
     * This mehtod is used to return the string in capitalize format
     *
     * @param str String received in parameter (One you want to capitalize)
     * @return returns capitalized string
     */
    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }
        return phrase.toString();
    }

    /**
     * This method returns current time to be used in reference
     *
     * @return returns time in format of yyyy-MM-dd HH:mm:ss
     */
    public static String getCurrentTimeStamp() {
        long timeStamp = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return formatter.format(new Date(timeStamp));
    }

    /**
     * method to send log to backend if sockets are connected
     * else connecting the sockets
     * and trying to send the log again (will try once)
     *
     * @param statusCode log's code
     * @param data       log's data/message
     */
    public static void sendLog(String statusCode, String data) {
        JSONObject spLogObject = new JSONObject();
        JSONObject dataObject = new JSONObject();
        JSONObject infoObject = new JSONObject();
        try {
            if (statusCode.equals("SPMOB9") || statusCode.equals("SPMOB10")) {
                infoObject.put("time", SetAndGetData.getInstance().getSdkKilledTime());
            } else {
                infoObject.put("time", Utils.getCurrentTimeStamp());
            }
            infoObject.put("data", data);
            dataObject.put("status_code", statusCode);
            dataObject.put("info", infoObject);
            spLogObject.put("request_id", SetAndGetData.getInstance().getReference());
            spLogObject.put("data", dataObject);
            spLogObject.put("source", "android");
            if (SetAndGetData.getInstance().getmSocket() != null && SetAndGetData.getInstance().getmSocket().connected()) {
                SetAndGetData.getInstance().getmSocket().emit("save-log", spLogObject);
            } else {
                SetAndGetData.getInstance().getLogsList().add(spLogObject);
                new SocketConnection();
            }
        } catch (JSONException e) {
            Log.w("LogsSocket", e.getMessage());
        }
    }

    /**
     * Method to get verification type name from state code
     *
     * @param state verification type state code
     * @return returning name against state code
     */
    public static String getType(int state) {
        switch (state) {
            case Constants.CODE_SELFIE:
                return "Face ";
            case Constants.CODE_DOCUMENT:
                return "Doc ";
            case Constants.CODE_DOCUMENT_TWO:
                return "Doc2 ";
            case Constants.CODE_ADDRESS:
                return "Address ";
            case Constants.CODE_CONSENT:
                return "Consent ";
        }
        return String.valueOf(state);
    }

    /**
     * This method is used to decrypt the data received in APi calling using executeVerificationRequest() method
     *
     * @param context points to the context of the activity which calls this method
     * @param webView
     * @param data
     */
    public static void callJsDecrypt(Context context, final WebView webView, final String data) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript("javascript:decrypt('" + data + "')", null);
                } else {
                    webView.loadUrl("javascript:decrypt('" + data + "')");
                }
            }
        });
    }


    /**
     * This method is used to show exit dialog
     */
    public void showExitDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(SetAndGetData.getInstance().getActivity());
        builder.setTitle(R.string.confirm_cancellation);
        builder.setMessage(R.string.are_you_sure_to_cancel_verification_process);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                terminateSDK();
            }
        });
        builder.setNegativeButton(R.string.not_now, null);


        AlertDialog dialog = builder.create();
        dialog.show();
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        int color = SetAndGetData.getInstance().getActivity().getResources().getColor(R.color.light_button_color);
        positiveButton.setTextColor(color);
        negativeButton.setTextColor(color);
    }


    /**
     * this method is used to terminate the sdk
     * closing the sdk returns a callback that can be used in parent activity
     */
    public void terminateSDK() {
        if (IntentHelper.getInstance().containsKey(Constants.KEY_DATA_MODEL)) {
            shuftiVerificationRequestModel = (ShuftiVerificationRequestModel) IntentHelper.getInstance().getObject(Constants.KEY_DATA_MODEL);
        }
        responseSet.clear();
        responseSet.put("verification_process_closed", "1");
        responseSet.put("message", "User cancel the verification process");
        shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
        SetAndGetData.getInstance().getActivity().finish();
    }



    /**
     * This function is used to get nationality of the country given in the request object
     *
     * @param context points to the context of the activity which calls this method
     * @param ISOCode ISOCode 2-letter code which represents the country
     * @return returns the name of the country from valid isoCode
     */
    public static String getNationality(Context context, String ISOCode) {

        String nationality = null;
        try {
            JSONArray jsonArray = new JSONArray(Utils.loadJSONFromAsset(context));
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                if (jsonObject.getString("iso_code").equalsIgnoreCase(ISOCode)) {
                    nationality = jsonObject.getString("nationality");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return nationality;
    }


    /**
     * This method is used to load json files fro the assets
     *
     * @param context points to the context of the activity which calls this method
     * @return returns the loaded json file
     */
    public static String loadJSONFromAsset(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("countries.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }


    /**
     * This method is used to get name of the country code given by user
     *
     * @param context points to the context of the activity which calls this method
     * @param ISOCode ISOCode 2-letter code which represents the country
     * @return
     */
    public static String getName(Context context, String ISOCode) {

        String nationality = null;
        try {
            JSONArray jsonArray = new JSONArray(Utils.loadJSONFromAsset(context));
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                if (jsonObject.getString("iso_code").equalsIgnoreCase(ISOCode)) {
                    nationality = jsonObject.getString("name");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return nationality;
    }

}

