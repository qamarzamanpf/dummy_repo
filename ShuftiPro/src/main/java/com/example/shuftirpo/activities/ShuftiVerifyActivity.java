package com.example.shuftirpo.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;

import com.example.shuftirpo.Fragments.CameraFragment;
import com.example.shuftirpo.Fragments.CameraFragmentDirections;
import com.example.shuftirpo.Fragments.InstructionsFragmentDirections;
import com.example.shuftirpo.Listeners.CameraListener;
import com.example.shuftirpo.R;
import com.example.shuftirpo.Singleton.SetAndGetData;
import com.example.shuftirpo.Utils.Constants;
import com.example.shuftirpo.Utils.IntentHelper;
import com.example.shuftirpo.Utils.Utils;
import com.example.shuftirpo.cloud.HttpConnectionHandler;
import com.example.shuftirpo.models.ShuftiVerificationRequestModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class ShuftiVerifyActivity extends AppCompatActivity implements CameraListener {

    private ShuftiVerificationRequestModel shuftiVerificationRequestModel;
    private HashMap<String, String> responseSet;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 100;
    private static ShuftiVerifyActivity instance = null;
    private JSONObject requestedObject;
    private String clientId = null, secretKey = null, accessToken = null, reference = "", currentImageType;

    /**
     * This is android's default callback method called everytime when activity is started
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        This is used to make sure that app is independent of device's theme mode
        This Disable the device dark mode only for a single Activity instead if whole activity
         */
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_shufti_verify);
        crashReport();
        instance = this;
        initialization();

    }

    /**
     * method to setting activity related values
     */
    private void initialization() {
        SetAndGetData.getInstance().setContext(getApplicationContext());
        SetAndGetData.getInstance().setActivity(this);
        internetInitiateSDK();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        responseSet = new HashMap<>();
        requestedObject = new JSONObject();
        if (IntentHelper.getInstance().containsKey(Constants.KEY_DATA_MODEL)) {
            shuftiVerificationRequestModel = (ShuftiVerificationRequestModel) IntentHelper.getInstance().getObject(Constants.KEY_DATA_MODEL);
            requestedObject = shuftiVerificationRequestModel.getJsonObject();
        }


        //Get Client ID from auth Object
        if (fetchClientId() != null) {
            clientId = fetchClientId();
        }

        //Get Secret key from auth Object
        if (fetchSecretKey() != null) {
            secretKey = fetchSecretKey();
        }

        //Get Access Token from auth Object
        if (fetchAccessToken() != null) {
            accessToken = fetchAccessToken();
        }

        //set country in the singleton from Request Object
        setCountry();
        checkAuthentication();

        try {
            if (shuftiVerificationRequestModel != null) {
                requestedObject.put("initiated_source", Utils.initiatedSdkType());
                requestedObject.put("initiated_source_version", Utils.initiatedSdkVersion());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     * This method is used to check all the required authentication data members
     * In case of missing value a error callback is generated
     */
    private void checkAuthentication() {
        //Return callbacks in case of wrong parameters sending
        if (shuftiVerificationRequestModel != null) {
            try {
                if(requestedObject.has("reference") && !requestedObject.getString("reference").isEmpty())
                {
                    SetAndGetData.getInstance().setReference(requestedObject.getString("reference"));
                }
                else
                {
                    returnErrorCallback(getString(R.string.empty_reference));
                    return;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (accessToken == null || accessToken.isEmpty()) {

                //Return callbacks in case of empty or null Clint id
                if (clientId == null || clientId.isEmpty()) {
                    returnErrorCallback(getString(R.string.empty_client_id));
                    return;
                }

                //Return callbacks in case of empty or null SecretKey
                if (secretKey == null || secretKey.isEmpty()) {
                    returnErrorCallback(getString(R.string.empty_secret_key));
                    return;
                }
            }
        }
    }


    /**
     *  This method fetches Clint ID Function from Auth Object
     * @return  method returns Clint ID
     */
    public String fetchClientId() {
        if (shuftiVerificationRequestModel.getAuthbject().has("client_Id")) {
            try {
                return shuftiVerificationRequestModel.getAuthbject().getString("client_Id");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    /**
     *  This method fetches Secret Key from Auth Object
     * @return  method returns Secret Key
     */
    public String fetchSecretKey() {
        if (shuftiVerificationRequestModel.getAuthbject().has("secret_key")) {
            try {
                return shuftiVerificationRequestModel.getAuthbject().getString("secret_key");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    /**
     * This method fetches access token from Auth Object
     * @return  method returns access token
     */
    public String fetchAccessToken() {
        if (shuftiVerificationRequestModel.getAuthbject().has("access_token")) {
            try {
                return shuftiVerificationRequestModel.getAuthbject().getString("access_token");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    /**
     *  This method fetches country from request Object
     */
    public void setCountry(){
        if (shuftiVerificationRequestModel.getJsonObject().has("country")) {
            try {
                String countryCode = shuftiVerificationRequestModel.getJsonObject().getString("country");
                if (!TextUtils.isEmpty(countryCode) && countryCode.length() == 2) {
                    SetAndGetData.getInstance().setCountryCode(countryCode);
                    SetAndGetData.getInstance().setCountry(Utils.getName(ShuftiVerifyActivity.this, countryCode));
                }
                else if(TextUtils.isEmpty(countryCode))
                {
                    SetAndGetData.getInstance().setCountryCode("");
                    SetAndGetData.getInstance().setCountry("");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else
        {
            SetAndGetData.getInstance().setCountryCode("");
            SetAndGetData.getInstance().setCountry("");
        }
    }


    /**
     * This method is used to return error callback to the parent activity
     * @param error
     */
    private void returnErrorCallback(String error) {
        responseSet.put("error", error);
        if (shuftiVerificationRequestModel != null && shuftiVerificationRequestModel.getShuftiVerifyListener() != null) {
            shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
        }

        try {
            if (responseSet.get("event") != null) {
                if (Objects.requireNonNull(responseSet.get("event")).equalsIgnoreCase("request.pending")) {
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ShuftiVerifyActivity.this.finish();
                        }
                    }, 2000);
                } else {
                    ShuftiVerifyActivity.this.finish();
                }
            } else {
                ShuftiVerifyActivity.this.finish();
            }

        } catch (Exception e) {

        }
    }

    /**
     * This callback is registered for navigation from camera
     * @param imageBase64           This is the captured proof in base64 string format
     * @param verificationType      This is the captured proof type
     * @param autoCapResponse       This is the captured state (weather it was auto captured or captured manually)
     */
    @Override
    public void capturedImage(String imageBase64, String verificationType, String autoCapResponse) {
        if (autoCapResponse != null){
            Utils.sendLog("SPMOB13", autoCapResponse);
        }

        switch (verificationType) {
            case Constants.CODE_DOC_FRONT:
                makeDocumentObject(imageBase64, Constants.CODE_DOC_FRONT);
                Utils.sendLog("SPMOB13", Constants.CODE_DOC_FRONT + autoCapResponse);
                break;
            case Constants.CODE_DOC_BACK:
                makeDocumentObject(imageBase64, Constants.CODE_DOC_BACK);
                Utils.sendLog("SPMOB13", Constants.CODE_DOC_BACK + autoCapResponse);
                break;
            case Constants.CODE_FACE:
                makeFaceObject(imageBase64);
                Utils.sendLog("SPMOB13", Constants.CODE_FACE + autoCapResponse);
                break;

        }

    }


    /**
     * This method is used to add the proof of face to the request object
     * @param encodedBase64String   The base64 image we received in capturedImage method
     */
    private void makeFaceObject(String encodedBase64String) {
        JSONObject faceObject = new JSONObject();
        try {
            faceObject.put("proof", Constants.IMAGE_PREFIX_FOR_SERVER + encodedBase64String);
            requestedObject.put("face", faceObject);
            SetAndGetData.getInstance().setRequestObject(requestedObject);

            Navigation.findNavController(this, R.id.nav_host_fragment).navigate(
                    CameraFragmentDirections.navigateFromCameraToResult());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method is used to add the proof of document to the request object
     *
     * @param encodedBase64String    The base64 image we received in capturedImage method
     * @param currentSide
     */
    private void makeDocumentObject(String encodedBase64String, String currentSide) {
        if (!requestedObject.has("document"))
        {
            JSONObject docObject = new JSONObject();
            try {
                requestedObject.put("document", docObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
            //If isDocBackSide true mean proof store in "additional_proof" parameter
            if (currentSide.equalsIgnoreCase(Constants.CODE_DOC_BACK)) {
                try {
                    if (requestedObject.getJSONObject("document").has("additional_proof")) {
                        requestedObject.getJSONObject("document").remove("additional_proof");
                        requestedObject.getJSONObject("document").put("additional_proof", Constants.IMAGE_PREFIX_FOR_SERVER + encodedBase64String);
                    } else {
                        requestedObject.getJSONObject("document").put("additional_proof", Constants.IMAGE_PREFIX_FOR_SERVER + encodedBase64String);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            //If isDocBackSide false mean proof store in "proof" parameter
            else {
                try {
                    if (requestedObject.getJSONObject("document").has("proof")) {
                        requestedObject.getJSONObject("document").remove("proof");
                        requestedObject.getJSONObject("document").put("proof", Constants.IMAGE_PREFIX_FOR_SERVER + encodedBase64String);
                    } else {
                        requestedObject.getJSONObject("document").put("proof", Constants.IMAGE_PREFIX_FOR_SERVER + encodedBase64String);
                    }

                    if (requestedObject.getJSONObject("document").has("supported_types")) {
                            ArrayList<String> doc_supported_types = new ArrayList<String>();
                            doc_supported_types.add("id_card");
                            requestedObject.getJSONObject("document").remove("supported_types");
                            requestedObject.getJSONObject("document").put("supported_types", new JSONArray(doc_supported_types));
                        }
                    else
                    {
                        ArrayList<String> doc_supported_types = new ArrayList<String>();
                        doc_supported_types.add("id_card");
                        requestedObject.getJSONObject("document").put("supported_types", new JSONArray(doc_supported_types));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
    }

    /**
     * TThis is android's default callback method called when app is killed
     */
    @Override
    protected void onDestroy() {
        Utils.sendLog("SPMOB10", "UI:" + SetAndGetData.getInstance().getCurrentScreen());
        super.onDestroy();

        while (requestedObject.length() > 0)
            requestedObject.remove(requestedObject.keys().next());
        instance = null;
        try {
            trimCache(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * this method is being used to Trim Cache
     * @param context
     */
    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is being used to delete directory if created any
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        assert dir != null;
        return dir.delete();
    }


    /**
     * This is android's default callback method called everytime activity is started or restarted
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            checkConnectivity();
        }
    }

    /**
     * This callback is used immediately in case internet connection is lost
     *
     */
    private ConnectivityManager.NetworkCallback connectivityCallback
            = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
        }

        @Override
        public void onLost(Network network) {
            runOnUiThread(() ->
            {
                try {
                    NoInternetDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    };

    /**
     * This method is used to sense internet connectivity all the time
     */
    private void checkConnectivity() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // here we are getting the connectivity service from connectivity manager
                final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                connectivityManager.registerNetworkCallback(new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), connectivityCallback);
            }
        });
    }

    /**
     * This method is used to check internet connection
     * It is invoked immediately when sdk is initiated
     */
    void internetInitiateSDK() {
        if (!Utils.networkAvailable(this) && !Utils.isNetworkOnline(this)) {
            NoInternetDialog();
        }
    }

    /**
     * This method is to show dialog box
     * The dialog box is shown in case of no internet connection or limited connection
     *
     */
    public void NoInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.no_internet_connection);
        builder.setMessage(R.string.make_sure_internet_connection);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                returnErrorCallback(getString(R.string.no_internet_connection));
            }
        });
        builder.setNegativeButton(R.string.retry, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                internetInitiateSDK();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        int color = SetAndGetData.getInstance().getActivity().getResources().getColor(R.color.light_button_color);
        positiveButton.setTextColor(color);
        negativeButton.setTextColor(color);

    }

    /**
     * This function checks if the required permissions are given to application or not
     * If user does not provide permission then SDK is terminated
     *
     * @return
     */
    public static boolean checkPermissions() {
        int permissionCamera = ContextCompat.checkSelfPermission(SetAndGetData.getInstance().getActivity(), Manifest.permission.CAMERA);
        int permissionStorage = ContextCompat.checkSelfPermission(SetAndGetData.getInstance().getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionAudio = ContextCompat.checkSelfPermission(SetAndGetData.getInstance().getActivity(), Manifest.permission.RECORD_AUDIO);

        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (permissionStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (permissionAudio != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions( SetAndGetData.getInstance().getActivity(), listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }


    /**
     * This overridden method is android default method
     * This method is invoked in result of user's action with permission access request
     * @param requestCode       Request code is an indicator declared in the activity by developer himself/herself
     * @param permissions       Required Permission Array List
     * @param grantResults      Result obtained by user's action (weatehr permissions are granted or not)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    if (perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                    {
                        Navigation.findNavController(this,R.id.nav_host_fragment).navigate(InstructionsFragmentDirections.navigateFromInstructionsToCamera("doc_front",
                                Utils.getUniqueReference(SetAndGetData.getInstance().getContext()), SetAndGetData.getInstance().getCountryCode(), "id_card"));

                    } else {

                        responseSet.clear();
                        JSONArray array = new JSONArray();
                        for (String permission : permissions) {
                            array.put(permission);
                        }
                        responseSet.put("permission_denied", "1");
                        responseSet.put("required_permissions", array.toString());
                        showPermissionRejectionDialog();
                    }
                }
            }
        }
    }

    /**
     * If User Denied Permission than Show Dialog and move back from Sdk
     *
     */
    public void showPermissionRejectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("App won't work without permissions. Please, restart app and give" +
                " access to the permissions.");
        builder.setPositiveButton("Finish", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (shuftiVerificationRequestModel != null && shuftiVerificationRequestModel.getShuftiVerifyListener() != null) {
                    shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
                }
                ShuftiVerifyActivity.this.finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setTextColor(Color.parseColor("#2B6AD8"));
    }



    /**
     * This method is used to handle Stack trace on an exception
     */
    private void crashReport() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                String exceptionClassname = isExceptionFromLibrary(paramThrowable);
                String threadName = paramThread.getName();
                String stackTrace = Arrays.toString(paramThread.getStackTrace());
                String message = paramThrowable.getMessage();
                String deviceInformation = Utils.getDeviceInformation();
                String timeStamp = Utils.getCurrentTimeStamp();
                String sdkVersion = Utils.getSDKVersion();
                HttpConnectionHandler.getInstance(clientId, "", "", false)
                        .sendStacktraceReport(ShuftiVerifyActivity.this, threadName, (Exception) paramThrowable,
                                message, deviceInformation, timeStamp, sdkVersion, exceptionClassname, stackTrace,
                                SetAndGetData.getInstance().getConfigObject().toString(), SetAndGetData.getInstance().getRequestObject().toString());
            }
        });
    }

    /**
     * This method is used to handle Stack trace if any exception occur in Application
     */
    private String isExceptionFromLibrary(Throwable paramThrowable) {
        String exceptionClassname = "";
        String causeClassName = "";
        try {
            exceptionClassname = paramThrowable.getStackTrace()[0].getClassName();
        } catch (Exception e) {
        }
        try {
            causeClassName = paramThrowable.getCause().getStackTrace()[0].getClassName();
        } catch (Exception e) {
        }
        if (!causeClassName.isEmpty() && causeClassName.contains("com.shutipro.sdk")) {
            exceptionClassname = causeClassName;
        }
        return exceptionClassname;
    }

}