package com.example.shuftirpo.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Base64;
import android.util.Size;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.Navigation;

import com.example.shuftirpo.Listeners.CameraListener;
import com.example.shuftirpo.R;
import com.example.shuftirpo.Singleton.SetAndGetData;
import com.example.shuftirpo.Utils.Constants;
import com.example.shuftirpo.Utils.Utils;
import com.example.shuftirpo.databinding.FragmentCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class CameraFragment extends Fragment {

    private Camera camera;
    private Preview preview;
    private WebSocket webSocket;
    private JSONObject reqResponse;
    private Boolean isToCapture = true, isToShowError = true;
    private static AlertDialog autoCaptureDialog;
    private ProcessCameraProvider cameraProvider;
    private static CountDownTimer autoCapTimer = null;
    private FragmentCameraBinding fragmentCameraBinding;
    private long current, previous = System.currentTimeMillis();
    private double delay = TimeUnit.MILLISECONDS.toMillis(500);
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private String requestStatus = "idle", responseStatus = "idle", socketResponseMessage,
            captureState = "manual", capturedImgBase64 = null, verificationType, image;
    private Bitmap originalBitmap, fullImage, previousFrame;
    Utils util;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false);
        initialization(null);
        return fragmentCameraBinding.getRoot();

    }


    /**
     * Method to initialize variables
     * and setting camera related values
     */
    private void initialization(String verType) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(SetAndGetData.getInstance().getContext());
        util = new Utils();
        isToShowError = true;

        requireActivity().getOnBackPressedDispatcher().addCallback(this.getActivity(), backPressCallBack);

        if (verType != null) {
            verificationType = verType;
        } else {
            verificationType = CameraFragmentArgs.fromBundle(getArguments()).getVerificationType();
        }
        SetAndGetData.getInstance().setCurrentScreen(verificationType + "_Camera_Screen");

        captureButtonListener();
        handlePreviewClicks();
        backButtonListener();
        settingCamera();
        setCameraUi();
        autoCaptureTimer();
        initiateSocketConnection();
    }

    /**
     * method to handle screen's back press click
     */
    private void backButtonListener() {
        fragmentCameraBinding.backImg.setOnClickListener(view -> {
            if (fragmentCameraBinding.previewParentLayout.getVisibility() == View.VISIBLE) {
                backPressSettings();

            } else {
                if (Constants.CODE_DOC_FRONT.equals(verificationType))
                {
                    Navigation.findNavController(view).popBackStack();
                }
            }
        });
    }

    /**
     * method to handle screen's back press click
     */
    OnBackPressedCallback backPressCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (fragmentCameraBinding.previewParentLayout.getVisibility() == View.VISIBLE) {
                backPressSettings();

            } else {
                switch (verificationType) {
                    case Constants.CODE_DOC_FRONT:
                        Navigation.findNavController(getActivity(),R.id.nav_host_fragment).popBackStack();
                        break;
                    case Constants.CODE_DOC_BACK:
                    case Constants.CODE_FACE:
                        util.showExitDialog();
                        break;
                }
            }
        }
    };

    /**
     * this mehotd is used to implement some setting on backpress
     */
    private void backPressSettings() {
        autoCapTimer.start();
        fragmentCameraBinding.captureBtn.setEnabled(true);
        isToCapture = true;
        setCameraUi();
        initiateSocketConnection();
        fragmentCameraBinding.cameraParentLayout.setVisibility(View.VISIBLE);
        fragmentCameraBinding.previewParentLayout.setVisibility(View.GONE);
    }


    /**
     * method to start timer for auto capture
     */
    private void autoCaptureTimer() {
        autoCapTimer = new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                errorLayoutGone();
                isToShowError = false;
                webSocket.close(1000, "sp_sdk_timeout");
            }
        };
        autoCapTimer.start();
    }

    /**
     * method to handle preview screen clicks
     * handling confirm button click
     * handling retake button click
     */
    private void handlePreviewClicks() {
        fragmentCameraBinding.confirmBtn.setOnClickListener(view -> {

            CameraListener cameraListener = (CameraListener) SetAndGetData.getInstance().getActivity();
            cameraListener.capturedImage(capturedImgBase64, verificationType, captureState);

            switch (verificationType) {
                case Constants.CODE_DOC_FRONT:
                    verificationType = Constants.CODE_DOC_BACK;
                    fragmentCameraBinding.backImg.setVisibility(View.GONE);
                    restartCameraSettings();
                    break;
                case Constants.CODE_DOC_BACK:
                    verificationType = Constants.CODE_FACE;
                    fragmentCameraBinding.backImg.setVisibility(View.GONE);
                    restartCameraSettings();
                    break;
                case Constants.CODE_FACE:
                    break;
            }


        });
        fragmentCameraBinding.retakeBtn.setOnClickListener(view -> {
            initiateSocketConnection();
            autoCapTimer.start();
            isToCapture = isToShowError = true;
            fragmentCameraBinding.cameraParentLayout.setVisibility(View.VISIBLE);
            fragmentCameraBinding.previewParentLayout.setVisibility(View.GONE);
            setCameraUi();

        });
    }

    /**
     * This method is used to initialize some settings when restarting camera
     */
    private void restartCameraSettings() {
        cameraProvider.unbindAll();
        fragmentCameraBinding.previewView.getSurfaceProvider();
        autoCapTimer.start();
        isToCapture = true;
        fragmentCameraBinding.cameraParentLayout.setVisibility(View.VISIBLE);
        fragmentCameraBinding.previewParentLayout.setVisibility(View.GONE);
        settingCamera();
        setCameraUi();
        initiateSocketConnection();
        autoCapTimer.start();


    }


    /**
     * This listener is here to perform action when capture button is pressed
     */
    private void captureButtonListener() {
        fragmentCameraBinding.captureBtn.setOnClickListener(view -> {

            isToCapture = false;
            errorLayoutGone();

            webSocket.close(1000, "sp_sdk_captured");
            cancelDialog();
            cancelTimer();
            camera.getCameraControl().enableTorch(false);
            try {
                captureState = captureState + socketResponseMessage;
            } catch (Exception e) {
                e.printStackTrace();
            }
            previewImage(originalBitmap);
        });
    }

    /* *//**
     * method to show dialog
     * when there is a timeout of auto capture
     * invoking further method to handle listeners
     *//*
    private void showTimeoutDialog() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(SetAndGetData.getInstance().getActivity());
        builder.setTitle(R.string.timeout);
        builder.setMessage(R.string.timeout_Subheading);
        builder.setCancelable(false);
        listenersTimeoutDialog(builder);
    }*/

    /**
     * method to handle click listeners of timeout dialog
     * creating alertDialog
     *
     * @param builder
     */
    private void listenersTimeoutDialog(AlertDialog.Builder builder) {
        builder.setNegativeButton(R.string.capture_manually, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                autoCaptureDialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                initiateSocketConnection();
                autoCapTimer.start();
            }
        });
        autoCaptureDialog = builder.create();
    }

    /**
     * method to set camera
     * starting camera preview
     * invoking further method to handle camera frames
     */
    private void settingCamera() {
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(SetAndGetData.getInstance().getContext()));
    }

    /**
     * Method to set camera screen UI
     * setting instruction on camera screen
     * invoking further method to enable capture button
     * invoking further method to handle camera overlays' visibility
     */
    private void setCameraUi() {
        switch (verificationType) {
            case Constants.CODE_DOC_FRONT:
                fragmentCameraBinding.cameraInstructionTop.setText(getString(R.string.top_front_id));
                fragmentCameraBinding.captureInstHeading.setText(getString(R.string.front_of_id));
                fragmentCameraBinding.captureInst.setText(getString(R.string.front_id_inst));
                fragmentCameraBinding.backImg.setVisibility(View.VISIBLE);
                errorLayoutGone();
                setCameraOverlays(View.VISIBLE, View.GONE);
                break;
            case Constants.CODE_DOC_BACK:
                fragmentCameraBinding.cameraInstructionTop.setText(getString(R.string.top_back_id));
                fragmentCameraBinding.captureInstHeading.setText(getString(R.string.back_of_id));
                fragmentCameraBinding.captureInst.setText(getString(R.string.back_id_inst));
                setCameraOverlays(View.VISIBLE, View.GONE);
                fragmentCameraBinding.backImg.setVisibility(View.GONE);
                errorLayoutGone();
                break;
            case Constants.CODE_FACE:
                fragmentCameraBinding.cameraInstructionTop.setText(getString(R.string.face_photo));
                fragmentCameraBinding.captureInstHeading.setText(getString(R.string.face_verification));
                fragmentCameraBinding.captureInst.setText(getString(R.string.face_verif_inst));
                setCameraOverlays(View.GONE, View.VISIBLE);
                fragmentCameraBinding.backImg.setVisibility(View.GONE);
                errorLayoutGone();
                break;
        }
        enableCapturingAfterAWhile();
    }

    /**
     * method to disable capture button on opening camera
     * and enabling it after one second
     */
    private void enableCapturingAfterAWhile() {
        fragmentCameraBinding.captureBtn.setEnabled(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fragmentCameraBinding.captureBtn.setEnabled(true);
            }
        }, 1000);
    }

    /**
     * method to set camera overlays visibility
     *
     * @param cardLayoutVisibility visibility of card layout
     * @param faceLayoutVisibility visibility of face layout
     */
    private void setCameraOverlays(int cardLayoutVisibility, int faceLayoutVisibility) {
        fragmentCameraBinding.cardParentLayout.setVisibility(cardLayoutVisibility);
        fragmentCameraBinding.faceParentLayout.setVisibility(faceLayoutVisibility);
    }

    /**
     * method to handle frame processing
     */
    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        @SuppressLint("RestrictedApi") ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setDefaultResolution(new Size(1024, 1024))
                        .setTargetRotation(Surface.ROTATION_0)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(SetAndGetData.getInstance().getContext()), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                current = System.currentTimeMillis();
                if (current - previous >= delay) {
                    @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
                    Image image1 = image.getImage();
                    try {
                        toBitmap(image1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    image.close();
                    previous = System.currentTimeMillis();
                } else {
                    image.close();
                }
            }
        });
        previewCamera(imageAnalysis);


    }

    /**
     * method to decide whether to open front camera or back
     * and starting camera preview
     *
     * @param imageAnalysis to process frames
     */
    private void previewCamera(ImageAnalysis imageAnalysis) {
        preview = new Preview.Builder().build();
        CameraSelector cameraSelector;
        if (verificationType.equals(Constants.CODE_FACE)) {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        } else {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        }
        preview.setSurfaceProvider(fragmentCameraBinding.previewView.getSurfaceProvider());
        camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,
                imageAnalysis, preview);
    }

    /**
     * method to convert frame to bitmap
     * invoking further methods for conversion processing
     *
     * @param image to which we have to convert to bitmap
     */
    private void toBitmap(Image image) throws IOException {
        byte[] jpegByteArray = toJpegImage(image, 100);
//        BitmapFactory bitmapFactory = new BitmapFactory();
        originalBitmap = BitmapFactory.decodeByteArray(
                jpegByteArray, 0, jpegByteArray.length);
        cropAndProcessFrame(originalBitmap);
    }

    /**
     * method to get image in byte form
     */
    private byte[] toJpegImage(Image image, int imageQuality) {
        YuvImage yuvImage = toYuvImage(image);
        int width = image.getWidth();
        int height = image.getHeight();

        // Convert to jpeg
        byte[] jpegImage = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), imageQuality, out);
            jpegImage = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jpegImage;
    }

    /**
     * method to get image in YuvImage form
     */
    private YuvImage toYuvImage(Image image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Invalid image format");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane yPlane = image.getPlanes()[0];
        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();

        // Full size Y channel and quarter size U+V channels.
        int numPixels = (int) (width * height * 1.5f);
        byte[] nv21 = new byte[numPixels];
        int index = 0;

        // Copy Y channel.
        int yRowStride = yPlane.getRowStride();
        int yPixelStride = yPlane.getPixelStride();
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                nv21[index++] = yBuffer.get(y * yRowStride + x * yPixelStride);
            }
        }

        // Copy VU data; NV21 format is expected to have YYYYVU packaging.
        // The U/V planes are guaranteed to have the same row stride and pixel stride.
        int uvRowStride = uPlane.getRowStride();
        int uvPixelStride = uPlane.getPixelStride();
        int uvWidth = width / 2;
        int uvHeight = height / 2;

        for (int y = 0; y < uvHeight; ++y) {
            for (int x = 0; x < uvWidth; ++x) {
                int bufferIndex = (y * uvRowStride) + (x * uvPixelStride);
                // V channel.
                nv21[index++] = vBuffer.get(bufferIndex);
                // U channel.
                nv21[index++] = uBuffer.get(bufferIndex);
            }
        }
        return new YuvImage(
                nv21, ImageFormat.NV21, width, height, /* strides= */ null);
    }

    /**
     * method to rotate, crop and process frame
     * invoking further methods for:
     * rotation
     * cropping
     * and processing
     */
    private void cropAndProcessFrame(Bitmap bitmap) {
        Bitmap croppedBitmap;
        if (verificationType.equals(Constants.CODE_FACE)) {
            croppedBitmap = cropFrame(bitmap);
            croppedBitmap = rotateBitmap(croppedBitmap, 270);
            originalBitmap = rotateBitmap(originalBitmap, 270);
        } else {
            croppedBitmap = rotateBitmap(bitmap, 90);
            croppedBitmap = cropFrame(croppedBitmap);
            originalBitmap = rotateBitmap(originalBitmap, 90);
        }

        new ProcessFrame(croppedBitmap).execute();
    }

    /**
     * method to crop bitmap/frame
     */
    private Bitmap cropFrame(Bitmap bmpToCrop) {
        int widthFinal, heightFinal, leftFinal, topFinal;

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        int heightOriginal = display.getHeight();
        int widthOriginal = display.getWidth();
        int heightFrame, widthFrame, leftFrame, topFrame;

        if (verificationType.equals(Constants.CODE_FACE)) {
            heightFrame = fragmentCameraBinding.faceBorder.getHeight();
            widthFrame = fragmentCameraBinding.faceBorder.getWidth();
            leftFrame = fragmentCameraBinding.faceBorder.getLeft();
            topFrame = fragmentCameraBinding.faceBorder.getTop();
        } else {
            heightFrame = fragmentCameraBinding.cardBorder.getHeight();
            widthFrame = fragmentCameraBinding.cardBorder.getWidth();
            leftFrame = fragmentCameraBinding.cardBorder.getLeft();
            topFrame = fragmentCameraBinding.cardBorder.getTop();
        }

        int heightReal = bmpToCrop.getHeight();
        int widthReal = bmpToCrop.getWidth();
        widthFinal = widthFrame * widthReal / widthOriginal;
        heightFinal = heightFrame * heightReal / heightOriginal;
        leftFinal = leftFrame * widthReal / widthOriginal;
        topFinal = topFrame * heightReal / heightOriginal;

        if (verificationType.equals(Constants.CODE_FACE)) {
            return Bitmap.createBitmap(bmpToCrop, leftFinal + 360, topFinal + 90, widthFinal - 190, heightFinal + 250);
        } else {
            return Bitmap.createBitmap(bmpToCrop, leftFinal + 130, topFinal + 20, widthFinal - 250, heightFinal + 20);
        }
    }

    /**
     * method to rotate bitmap
     */
    private static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * method to check sockets and connect them if required
     */
    private void checkAndConnectSockets(){
        if (!SetAndGetData.getInstance().isAutoCapSocketsConn() && !SetAndGetData.getInstance().getSocketDisconnectReason().equals(
                "sp_sdk_timeout") && !SetAndGetData.getInstance().getSocketDisconnectReason().equals("sp_sdk_captured")){
            if (Utils.networkAvailable(SetAndGetData.getInstance().getContext()) &&
                    Utils.isNetworkOnline(SetAndGetData.getInstance().getContext())){
                initiateSocketConnection();
            }
        }
    }

    /**
     * A complete Async task to process different frames every 500milliseconds
     */
    @SuppressLint("StaticFieldLeak")
    class ProcessFrame extends AsyncTask<String, String, Bitmap> {
        Bitmap croppedBitmap;

        public ProcessFrame(Bitmap croppedBitmap) {
            this.croppedBitmap = croppedBitmap;
        }

        @Override
        protected Bitmap doInBackground(String... f_url) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            image = Base64.encodeToString(byteArray, Base64.NO_WRAP);
//            String image = Base64.encodeToString(byteArray, Base64.NO_WRAP);
            String token = null;
            try {
                token = image + CameraFragmentArgs.fromBundle(getArguments()).getReference() + "MOB12VERIFA09QWKOPUHFBN12";
            } catch (Exception e) {
                e.printStackTrace();
            }
            String sha256 = Utils.sha256(token);

            if (isToCapture) {
                if (verificationType.equals(Constants.CODE_FACE)) {
                    JSONObject faceObject = new JSONObject();
                    try {
                        faceObject.put("reference", CameraFragmentArgs.fromBundle(getArguments()).getReference());
                        faceObject.put("token", sha256);
                        faceObject.put("source", "SDK");
                        faceObject.put("frame", image);
                        checkAndConnectSockets();
                        if (requestStatus.equalsIgnoreCase("idle")) {
                            fullImage = originalBitmap;
                            webSocket.send(faceObject.toString().replaceAll("\\\\", ""));
                            requestStatus = "isSent";
                        }
                        try {
                            if (responseStatus.equalsIgnoreCase("received")) {
                                socketResponseMessage = reqResponse.getString("error_key");
                                if (reqResponse.getString("result").equalsIgnoreCase("true")) {
                                    return fullImage;
                                } else {
                                    requireActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (isToShowError) {
                                                        handleFaceDetectError();
                                                    }
                                                }
                                            }, 2000);
                                        }
                                    });
                                    responseStatus = "idle";
                                    requestStatus = "idle";
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("reference", CameraFragmentArgs.fromBundle(getArguments()).getReference());
                        jsonObject.put("token", sha256);
                        jsonObject.put("frame", image);
                        jsonObject.put("source", "SDK");
                        if (verificationType.equals(
                                Constants.CODE_DOC_FRONT)) {
                            jsonObject.put("proof_name", "proof");
                        } else {
                            jsonObject.put("proof_name", "additional_proof");
                        }
                        jsonObject.put("supported_types", CameraFragmentArgs.fromBundle(getArguments())
                                .getSupportedType().replace("_", ""));
                        jsonObject.put("country", CameraFragmentArgs.fromBundle(getArguments()).getCountry());
                        checkAndConnectSockets();
                        if (requestStatus.equalsIgnoreCase("idle")) {
                            fullImage = originalBitmap;
                            webSocket.send(jsonObject.toString().replaceAll("\\\\", ""));
                            requestStatus = "isSent";
                        }

                        try {
                            if (responseStatus.equalsIgnoreCase("received")) {
                                socketResponseMessage = reqResponse.getString("error_key");
                                if (reqResponse.getString("result").equalsIgnoreCase("true")) {
                                    return fullImage;
                                } else {
                                    requireActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (isToShowError) {
                                                        handleDocDetectError();
                                                    }

                                                }
                                            }, 2000);
                                        }
                                    });
                                    responseStatus = "idle";
                                    requestStatus = "idle";
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bmp) {
            postDetection(bmp);
        }
    }

    /**
     * method to set and display face detection related issue
     */
    private void handleFaceDetectError() {

        try {
            if (getActivity() != null && reqResponse != null && !reqResponse.getString("message").contains("accepted")) {
                requireActivity().runOnUiThread(() -> {
                    try {
                        fragmentCameraBinding.faceDetectError.setVisibility(View.VISIBLE);
                        fragmentCameraBinding.faceBorder.setBackgroundResource(R.drawable.face_border_bg_red);
                        String errorText = reqResponse.getString("error_key");
                        switch (errorText) {
                            case "face_not_in_oval":
                                fragmentCameraBinding.faceDetectError.setText(getString(R.string.face_not_in_oval));
                                break;
                            case "blurred_face":
                                fragmentCameraBinding.faceDetectError.setText(getString(R.string.blurred_face));
                                break;
                            case "face_too_far":
                                fragmentCameraBinding.faceDetectError.setText(getString(R.string.face_too_far));
                                break;
                            case "face_forward":
                                fragmentCameraBinding.faceDetectError.setText(getString(R.string.face_forward));
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * method to set and display doc detection related issue
     */
    private void handleDocDetectError() {
        try {
            if (getActivity() != null && reqResponse != null && !reqResponse.getString("error_msg").contains("accepted")) {
                requireActivity().runOnUiThread(() -> {
                    try {
                        fragmentCameraBinding.docDetectError.setVisibility(View.VISIBLE);
                        String errorText = reqResponse.getString("error_key");
                        fragmentCameraBinding.cardBorder.setBackgroundResource(R.drawable.card_bg_red);
                        switch (errorText) {
                            case "missing_corners":
                                fragmentCameraBinding.docDetectError.setText(getString(R.string.missing_corners));
                                break;
                            case "blurred_document":
                                fragmentCameraBinding.docDetectError.setText(getString(R.string.blurred_document));
                                break;
                            case "document_too_far":
                                fragmentCameraBinding.docDetectError.setText(getString(R.string.document_too_far));
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * method to initiate socket connection
     */
    private void initiateSocketConnection() {
        try {
            requestStatus = "idle";
            responseStatus = "idle";
            reqResponse = null;
            webSocket = null;
            OkHttpClient client = new OkHttpClient().newBuilder().cache(null).build();
            Request request = new Request.Builder()
                    .addHeader("Cache-Control", "no-cache")
                    .cacheControl(new CacheControl.Builder().noCache().build())
//                .CacheControl(CacheControl.FORCE_NETWORK)
                    .url(verificationType.equals(Constants.CODE_FACE) ?
                            Constants.FACE_ML_PATH : Constants.DOC_ML_PATH)
                    .build();
            webSocket = client.newWebSocket(request, new SocketListener());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * to handle socket listeners
     */
    private class SocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            SetAndGetData.getInstance().setAutoCapSocketsConn(true);
            SetAndGetData.getInstance().setSocketDisconnectReason("");
        }

        /**
         * to handle new emit/message
         */
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            try {
                reqResponse = new JSONObject(text);
                socketResponseMessage = reqResponse.getString("error_key");
                responseStatus = "received";
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
            SetAndGetData.getInstance().setSocketDisconnectReason(reason);
            SetAndGetData.getInstance().setAutoCapSocketsConn(false);
            if (!reason.equals("sp_sdk_timeout") && !reason.equals("sp_sdk_captured")) {
                if (Utils.isNetworkOnline(SetAndGetData.getInstance().getContext()) &&
                        Utils.networkAvailable(SetAndGetData.getInstance().getContext())) {
                    initiateSocketConnection();
                }
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
            SetAndGetData.getInstance().setAutoCapSocketsConn(false);
            if (Utils.isNetworkOnline(SetAndGetData.getInstance().getContext()) &&
                    Utils.networkAvailable(SetAndGetData.getInstance().getContext())) {
                initiateSocketConnection();
            }
        }
    }

    /**
     * method to handle further flow after face/doc detection
     */
    private void postDetection(final Bitmap bmp) {
        if (bmp != null) {
            cancelDialog();
            cancelTimer();
            isToCapture = false;
            captureState = "AutoCapture";
            getActivity().runOnUiThread(() -> {
                errorLayoutGone();
            });
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    previewImage(bmp);
                }
            }, 500);

        }
    }

    /**
     * method to cancel auto capture timer
     */
    public static void cancelTimer() {
        if (autoCapTimer != null)
            autoCapTimer.cancel();
    }

    /**
     * method to dismiss auto capture dialog
     */
    public static void cancelDialog() {
        if (autoCaptureDialog != null) {
            autoCaptureDialog.dismiss();
        }
    }

    /**
     * method to display captured image
     */
    private void previewImage(Bitmap bmp) {
        fragmentCameraBinding.backImg.setVisibility(View.VISIBLE);
        previousFrame = bmp;
        webSocket.close(1000, "sp_sdk_captured");
        imageToBase64(bmp);
        handleCapturedImage(bmp);
    }

    /**
     * mehtod to convert image to base64
     */
    public void imageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        capturedImgBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    /**
     * method to flip bitmap
     */
    public static Bitmap flip(Bitmap src) {
        // create new matrix for transformation
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        // return transformed image
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    /**
     * method to handle captured image
     */
    public void handleCapturedImage(Bitmap bitmap) {
        if (!isToCapture) {
            fragmentCameraBinding.previewParentLayout.setVisibility(View.VISIBLE);
            fragmentCameraBinding.cameraParentLayout.setVisibility(View.GONE);

            switch (verificationType) {
                case Constants.CODE_DOC_FRONT:
                    fragmentCameraBinding.cameraInstructionTop.setText(getString(R.string.front_doc_preview));
                    fragmentCameraBinding.previewInstHeading.setText(getString(R.string.is_front_readable));
                    fragmentCameraBinding.capturedImage.setImageBitmap(bitmap);
                    break;
                case Constants.CODE_DOC_BACK:
                    fragmentCameraBinding.cameraInstructionTop.setText(getString(R.string.back_doc_preview));
                    fragmentCameraBinding.previewInstHeading.setText(getString(R.string.is_back_readable));
                    fragmentCameraBinding.capturedImage.setImageBitmap(bitmap);
                    break;
                case Constants.CODE_FACE:
                    fragmentCameraBinding.cameraInstructionTop.setText(getString(R.string.face_preview));
                    fragmentCameraBinding.previewInstHeading.setText(getString(R.string.is_face_visible));
                    fragmentCameraBinding.previewInst.setText(getString(R.string.is_face_lit));
                    fragmentCameraBinding.capturedImage.setImageBitmap(flip(bitmap));
                    break;
            }
        }

    }


    /**
     * This method is used to hide error layout and set white sources for face and doc overlays
     */
    private void errorLayoutGone() {
        fragmentCameraBinding.cardBorder.setBackgroundResource(R.drawable.card_bg_white);
        fragmentCameraBinding.docDetectError.setVisibility(View.GONE);
        fragmentCameraBinding.faceBorder.setBackgroundResource(R.drawable.face_border_bg);
        fragmentCameraBinding.faceDetectError.setVisibility(View.GONE);
    }

    /**
     * This method is created to go back to the preview screen of front side when pressed backPress on backside camera
     */
    private void showPreviousPreview() {
        isToCapture = false;
        errorLayoutGone();
        webSocket.close(1000, "sp_sdk_captured");
        cancelDialog();
        cancelTimer();
        camera.getCameraControl().enableTorch(false);
        try {
            captureState = captureState + socketResponseMessage;
        } catch (Exception e) {
            e.printStackTrace();
        }
        previewImage(previousFrame);
    }


}
