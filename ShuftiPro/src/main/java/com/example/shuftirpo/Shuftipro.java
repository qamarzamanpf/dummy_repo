package com.example.shuftirpo;


import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.example.shuftirpo.Listeners.ShuftiVerifyListener;
import com.example.shuftirpo.Singleton.SetAndGetData;
import com.example.shuftirpo.Utils.Constants;
import com.example.shuftirpo.Utils.IntentHelper;
import com.example.shuftirpo.Utils.SocketConnection;
import com.example.shuftirpo.Utils.Utils;
import com.example.shuftirpo.activities.ShuftiVerifyActivity;
import com.example.shuftirpo.models.ShuftiVerificationRequestModel;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class Shuftipro {
    private static Shuftipro shuftipro = null;

    public Shuftipro() {

    }

    public static Shuftipro getInstance() {
        shuftipro = new Shuftipro();
        return shuftipro;
    }

    public void shuftiproVerification(JSONObject requestedObject,JSONObject authObject,JSONObject configObject, Activity parentActivity, ShuftiVerifyListener listener)
    {

        if(configObject == null || configObject.length() == 0){
            try {
                JSONObject configObj = new JSONObject();
                configObj.put("open_webview",false);
                configObj.put("asyncRequest",false);
                configObj.put("captureEnabled",false);
                configObj.put("encryptRequest",false);
                configObj.put("autoCapture",true);
                configObject = configObj;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(requestedObject == null || requestedObject.length() == 0)
        {
            HashMap<String, String> responseSet = new HashMap<>();
            responseSet.put("error", "Request Object cannot be empty");
            listener.verificationStatus(responseSet);
        }
        else if(authObject == null || authObject.length() == 0)
        {
            HashMap<String, String> responseSet = new HashMap<>();
            responseSet.put("error", "Auth Object cannot be empty");
            listener.verificationStatus(responseSet);
        }else
        {

            if (requestedObject.has("reference")) {
                try {
                    SetAndGetData.getInstance().setReference(requestedObject.getString("reference"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (SetAndGetData.getInstance().getmSocket() != null && SetAndGetData.getInstance().getmSocket().connected()) {
                SetAndGetData.getInstance().getmSocket().disconnect();
            }
            new SocketConnection();

            SetAndGetData.getInstance().setConfigObject(configObject);
            SetAndGetData.getInstance().setRequestObject(requestedObject);
            SetAndGetData.getInstance().setAuthObject(authObject);
            SetAndGetData.getInstance().setShuftiVerifyListener(listener);

            try
            {
                if (requestedObject.has("show_results"))
                {
                    SetAndGetData.getInstance().setShowResults(requestedObject.getString("show_results"));
                }

            }catch(Exception e)
            {
                e.printStackTrace();
            }


            JSONObject clientObjects = new JSONObject();
            try {
                clientObjects.put("Auth Object", authObject);
                clientObjects.put("Config Object", configObject);
                clientObjects.put("Request Object", requestedObject);
                Utils.sendLog("SPMOB0", clientObjects.toString(), null);
                Log.i("SPMOB0", clientObjects.toString());
            }catch (Exception e) {
                e.printStackTrace();
            }

            ShuftiVerificationRequestModel verificationRequestModel = new ShuftiVerificationRequestModel();
            verificationRequestModel.setJsonObject(requestedObject);
            verificationRequestModel.setAuthObject(authObject);
            verificationRequestModel.setConfigObject(configObject);
            verificationRequestModel.setParentActivity(parentActivity);
            verificationRequestModel.setShuftiVerifyListener(listener);
            IntentHelper.getInstance().insertObject(Constants.KEY_DATA_MODEL,verificationRequestModel);

            Utils.sendLog("SPMOB1", null, null);
            Intent intent = new Intent(parentActivity, ShuftiVerifyActivity.class);
            parentActivity.startActivity(intent);
        }
        }


}
