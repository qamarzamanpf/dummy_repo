package com.example.shuftirpo.Fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.shuftirpo.Listeners.CameraListener;
import com.example.shuftirpo.Listeners.NetworkListener;
import com.example.shuftirpo.R;
import com.example.shuftirpo.Singleton.SetAndGetData;
import com.example.shuftirpo.Utils.Constants;
import com.example.shuftirpo.Utils.IntentHelper;
import com.example.shuftirpo.Utils.Utils;
import com.example.shuftirpo.cloud.HttpConnectionHandler;
import com.example.shuftirpo.databinding.FragmentResultBinding;
import com.example.shuftirpo.models.ShuftiVerificationRequestModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class ResultFragment extends Fragment implements NetworkListener {
    private FragmentResultBinding fragmentResultBinding;
    private HashMap<String, String> responseSet = new HashMap<>();
    private String requestReference = "";
    private ShuftiVerificationRequestModel shuftiVerificationRequestModel;
    Utils util;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentResultBinding = FragmentResultBinding.inflate(inflater, container, false);
        initialization();
        return fragmentResultBinding.getRoot();
    }

    /**
     * Method to initialize variables
     * and setting camera related values
     */
    private void initialization() {
        requireActivity().getOnBackPressedDispatcher().addCallback(this.getActivity(), backPressCallBack);
        util = new Utils();
        SetAndGetData.getInstance().setCurrentScreen("Uploading_" + "Result_Screen");
        if (IntentHelper.getInstance().containsKey(Constants.KEY_DATA_MODEL)) {
            shuftiVerificationRequestModel = (ShuftiVerificationRequestModel) IntentHelper.getInstance().getObject(Constants.KEY_DATA_MODEL);
        }
        letsGoButton();
        handleResultClick();
        setResultUi(View.VISIBLE, View.GONE, View.GONE);
        if (!SetAndGetData.getInstance().isApiCalled()) {
            apiRequestForVerification();
        }
    }

    /**
     * method to send log to backend regarding result
     */
    private void sendResultLog(String message){
        if (message != null) {
            Utils.sendLog("SPMOB7", "Result:" + message);
        }else {
            Utils.sendLog("SPMOB7", null);
        }
    }

    /**
     * method to se result screen UI's visibilities
     *
     * @param progressUiVisibility  progress screen visibility
     * @param successUiVisibility   success screen visibility
     * @param unSuccessUiVisibility UnSuccess screen visibility
     */
    private void setResultUi(int progressUiVisibility, int successUiVisibility, int unSuccessUiVisibility) {
            fragmentResultBinding.uploadingLayout.setVisibility(progressUiVisibility);
            fragmentResultBinding.successVerifLayout.setVisibility(successUiVisibility);
            fragmentResultBinding.unsuccessVerifLayout.setVisibility(unSuccessUiVisibility);
        if (progressUiVisibility == View.GONE){
            SetAndGetData.getInstance().setCurrentScreen("Result_Shown");
        }
    }

    /**
     * method to call api to verify
     */
    private void apiRequestForVerification() {
        SetAndGetData.getInstance().setApiCalled(true);
        boolean isSubmitted = HttpConnectionHandler.getInstance().executeVerificationRequest(this,
                SetAndGetData.getInstance().getRequestObject(), this, SetAndGetData.getInstance().getContext());

        if (!isSubmitted) {
            SetAndGetData.getInstance().setApiCalled(false);

            responseSet.put("reference", SetAndGetData.getInstance().getReference());
            responseSet.put("event", "");
            responseSet.put("error", "No Internet Connection");

            //show dialog
        }
    }


    //If WebService/Api Response is Success than this CallBack Call
    @Override
    public void successResponse(String result) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SetAndGetData.getInstance().setApiCalled(false);
                responseSet.clear();
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    Log.e("XYZ", jsonObject.toString());
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
                            requestReference = reference;
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
                            error = jsonObject.getString("error");
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
                    responseSet.put("reference", reference);
                    responseSet.put("event", event);
                    responseSet.put("error", error);
                    responseSet.put("verification_url", verification_url);
                    responseSet.put("verification_result", verification_result);
                    responseSet.put("verification_data", verification_data);
                    responseSet.put("declined_reason", declined_reason);
                    responseSet.put("body", result);

//                View.OnClickListener onClickListener = new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//
//                        if (shuftiVerificationRequestModel != null && shuftiVerificationRequestModel.getShuftiVerifyListener() != null) {
//                            shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
//                        }
//                        if (alertDialog.isShowing()) {
//                            alertDialog.dismiss();
//                        }
//                        ShuftiVerifyActivity.this.finish();
//                    }
//                };

                    sendResultLog(event);
                    if(SetAndGetData.getInstance().getShowResults().equalsIgnoreCase("0"))
                    {
                        fragmentResultBinding.successVerifHeading.setText(getString(R.string.successfully_submitted));
                        fragmentResultBinding.successVerifInfo.setText(getString(R.string.forwarded_to_merchant));
                        fragmentResultBinding.successSubHeading.setVisibility(View.GONE);
                        setResultUi(View.GONE, View.VISIBLE, View.GONE);
                    }else if (event.equalsIgnoreCase(Constants.VERIFICATION_REQUEST_ACCEPTED)) {
                        setResultUi(View.GONE, View.VISIBLE, View.GONE);
                    } else if (event.equalsIgnoreCase(Constants.VERIFICATION_REQUEST_DECLINED)) {
                        setResultUi(View.GONE, View.GONE, View.VISIBLE);
                        fragmentResultBinding.unsuccessInfo.setText(declined_reason);

                    } else if (event.equalsIgnoreCase(Constants.VERIFICATION_REQUEST_RECEIVED)) {
                        fragmentResultBinding.successVerifHeading.setText(getString(R.string.successfully_submitted));
                        fragmentResultBinding.successVerifInfo.setVisibility(View.GONE);
                        setResultUi(View.GONE, View.VISIBLE, View.GONE);
                    } else if (event.equalsIgnoreCase(Constants.VERIFICATION_REQUEST_INVALID)) {
                        fragmentResultBinding.unsuccessVerifHeading.setText(getString(R.string.invalid_request));
                        fragmentResultBinding.unsuccessInfo.setText(getString(R.string.invalid_request_msg));
                        fragmentResultBinding.unsuccessDetailMsg.setVisibility(View.GONE);
                        setResultUi(View.GONE, View.GONE, View.VISIBLE);
                    } else if (event.equalsIgnoreCase(Constants.VERIFICATION_REQUEST_TIMEOUT)) {
                        fragmentResultBinding.unsuccessVerifHeading.setText(getString(R.string.request_timeout));
                        fragmentResultBinding.unsuccessInfo.setText(getString(R.string.timeout_msg));
                        fragmentResultBinding.unsuccessDetailMsg.setVisibility(View.GONE);
                        setResultUi(View.GONE, View.GONE, View.VISIBLE);
                    } else {
                        fragmentResultBinding.unsuccessInfo.setVisibility(View.GONE);
                        fragmentResultBinding.unsuccessDetailMsg.setVisibility(View.GONE);
                        setResultUi(View.GONE, View.GONE, View.VISIBLE);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, 1000);
    }

    //If WebService/Api Response return any issue or error than this CallBack Call
    @Override
    public void errorResponse(String response) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SetAndGetData.getInstance().setApiCalled(false);
                responseSet.clear();
                if (response == null) {
                    return;
                }
                if (response.equalsIgnoreCase("Internet Connection Failed")) {
                    responseSet.clear();

                    responseSet.put("reference", "");
                    responseSet.put("event", "");
                    responseSet.put("error", "No Internet Connection");

                    fragmentResultBinding.unsuccessVerifHeading.setText(getString(R.string.no_internet_connection));
                    fragmentResultBinding.unsuccessInfo.setText(getString(R.string.make_sure_internet_connection));
                    fragmentResultBinding.unsuccessDetailMsg.setVisibility(View.GONE);
                    setResultUi(View.GONE, View.GONE, View.VISIBLE);
                    return;
                }

                if (response.equalsIgnoreCase("timeout")) {
                    responseSet.clear();

                    responseSet.put("reference", "");
                    responseSet.put("event", "");
                    responseSet.put("error", "Request Timeout");

                    fragmentResultBinding.unsuccessVerifHeading.setText(getString(R.string.request_timeout));
                    fragmentResultBinding.unsuccessInfo.setText(getString(R.string.timeout_msg));
                    fragmentResultBinding.unsuccessDetailMsg.setVisibility(View.GONE);
                    setResultUi(View.GONE, View.GONE, View.VISIBLE);
                    return;
                }

                if (response.isEmpty()) {
                    try {
                        responseSet.put("error", "Error Occurred");
                        responseSet.put("reference", SetAndGetData.getInstance().getReference());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    if (!response.isEmpty()) {
                        JSONObject jsonObject = new JSONObject(response);
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
                        responseSet.put("reference", reference);
                        responseSet.put("event", event);
                        responseSet.put("error", error);
                        responseSet.put("verification_url", verification_url);
                        responseSet.put("verification_result", verification_result);
                        responseSet.put("verification_data", verification_data);
                        responseSet.put("declined_reason", declined_reason);
                        responseSet.put("body", response);

                        sendResultLog(event);
                        if(SetAndGetData.getInstance().getShowResults().equalsIgnoreCase("0"))
                        {
                            fragmentResultBinding.successVerifHeading.setText(getString(R.string.successfully_submitted));
                            fragmentResultBinding.successVerifInfo.setText(getString(R.string.forwarded_to_merchant));
                            fragmentResultBinding.successSubHeading.setVisibility(View.GONE);
                            setResultUi(View.GONE, View.VISIBLE, View.GONE);

                        }else if (event.equalsIgnoreCase(Constants.VERIFICATION_REQUEST_INVALID)) {
                            fragmentResultBinding.unsuccessVerifHeading.setText(getString(R.string.invalid_request));
                            fragmentResultBinding.unsuccessInfo.setText(getString(R.string.invalid_request_msg));
                            fragmentResultBinding.unsuccessDetailMsg.setVisibility(View.GONE);
                            setResultUi(View.GONE, View.GONE, View.VISIBLE);
                        } else if (event.equalsIgnoreCase(Constants.VERIFICATION_REQUEST_UNAUTHORIZED)) {
                            fragmentResultBinding.unsuccessVerifHeading.setText(getString(R.string.request_unauthorized));
                            fragmentResultBinding.unsuccessInfo.setText(getString(R.string.request_unauthorized_msg));
                            fragmentResultBinding.unsuccessDetailMsg.setVisibility(View.GONE);
                            setResultUi(View.GONE, View.GONE, View.VISIBLE);
                        } else {
                            fragmentResultBinding.unsuccessInfo.setVisibility(View.GONE);
                            fragmentResultBinding.unsuccessDetailMsg.setVisibility(View.GONE);
                            setResultUi(View.GONE, View.GONE, View.VISIBLE);
                        }
                    } else {
                        fragmentResultBinding.unsuccessInfo.setVisibility(View.GONE);
                        fragmentResultBinding.unsuccessDetailMsg.setVisibility(View.GONE);
                        setResultUi(View.GONE, View.GONE, View.VISIBLE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, 1000);
    }

    /**
     * method to handle screen's back press click
     */
    OnBackPressedCallback backPressCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if(fragmentResultBinding.uploadingLayout.getVisibility() == View.VISIBLE)
            {
                util.showExitDialog();
            } else {
                ShowExitDialog();
            }
        }
    };

    /**
     * method to handle result screen clicks
     * handling try Again button click
     * handling Later Button click
     */
    private void handleResultClick() {

        fragmentResultBinding.proceedBtn.setOnClickListener(view -> {
            shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
            SetAndGetData.getInstance().getActivity().finish();
        });
    }

    /**
     * method to handle true result screen clicks
     * handling try Again button click
     */
    private void letsGoButton() {

        fragmentResultBinding.letsGoBtn.setOnClickListener(view -> {

            if (shuftiVerificationRequestModel != null && shuftiVerificationRequestModel.getShuftiVerifyListener() != null) {
                shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
            }
            SetAndGetData.getInstance().getActivity().finish();
        });
    }


    /**
     * method to handle the UI of progress according to the progress value
     */
    public void handleProgress(int progress) {
        if (progress == 100){
            fragmentResultBinding.docUploading.setText(getString(R.string.identity_verify));
        }
        SetAndGetData.getInstance().setCurrentScreen("Verifying_" + "Result_Screen");
    }


    /**
     * This method is used to show exit dialog
     */
    private void ShowExitDialog(){

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
    private void terminateSDK() {
        shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
        SetAndGetData.getInstance().getActivity().finish();
    }

}
