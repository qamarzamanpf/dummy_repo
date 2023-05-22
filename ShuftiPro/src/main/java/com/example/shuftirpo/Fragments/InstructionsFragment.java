package com.example.shuftirpo.Fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.shuftirpo.R;
import com.example.shuftirpo.Singleton.SetAndGetData;
import com.example.shuftirpo.Utils.Constants;
import com.example.shuftirpo.Utils.IntentHelper;
import com.example.shuftirpo.Utils.Utils;
import com.example.shuftirpo.activities.ShuftiVerifyActivity;
import com.example.shuftirpo.databinding.FragmentInstructionsBinding;
import com.example.shuftirpo.models.ShuftiVerificationRequestModel;


import java.util.HashMap;

public class InstructionsFragment extends Fragment {

    private FragmentInstructionsBinding fragmentInstructionsBinding;
    Utils util;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        fragmentInstructionsBinding = FragmentInstructionsBinding.inflate(inflater, container, false);
        initialization();
        return fragmentInstructionsBinding.getRoot();
    }

    /**
     * method to setting activity related values
     */
    private void initialization() {
        SetAndGetData.getInstance().setCurrentScreen("Instruction_Screen");

        util = new Utils();
        getStartListener();
        skipListener();
        requireActivity().getOnBackPressedDispatcher().addCallback(this.getActivity(), backPressCallBack);

    }

    /**
     * method to handle screen's back press click
     */
    OnBackPressedCallback backPressCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed()
        {
            util.showExitDialog();
        }
    };


    /**
     * method to handle click of get started
     * to initiate verification process
     * will open camera screen
     */
    private void getStartListener(){
        fragmentInstructionsBinding.startedBtn.setOnClickListener(view -> {
            if (ShuftiVerifyActivity.checkPermissions()) {
                Utils.sendLog("SPMOB27", "", "Camera Screen");
                Navigation.findNavController(view).navigate(InstructionsFragmentDirections.navigateFromInstructionsToCamera("doc_front",
                        Utils.getUniqueReference(SetAndGetData.getInstance().getContext()), SetAndGetData.getInstance().getCountryCode(), "id_card"));
            }
        });
    }

    /**
     * method to handle click of skip button
     * to terminate verification process
     * will close the sdk
     */
    private void skipListener(){
        fragmentInstructionsBinding.skipBtn.setOnClickListener(view -> {
            util.showExitDialog();
        });

        fragmentInstructionsBinding.exitBtn.setOnClickListener(view -> {
            util.showExitDialog();
        });
    }



}
