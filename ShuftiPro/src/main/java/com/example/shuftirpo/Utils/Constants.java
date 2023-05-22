package com.example.shuftirpo.Utils;

public class Constants {
    public static final String CODE_FACE = "face";
    public static final String CODE_DOC_FRONT = "doc_front";
    public static final String CODE_DOC_BACK = "doc_back";
    public static final String FACE_ML_PATH = "wss://ac.shuftipro.com/face";
    public static final String DOC_ML_PATH = "wss://ac.shuftipro.com/document";

    public static final String KEY_DATA_MODEL = "key_data";
    public static final String KEY_DATA_MODELS = "key_datas";
    public static final String KEY_VERIFICATION_TYPE = "verification_type";
    public static final String OUTPUT_IMAGE_PREFIX = "SHUFTIPRO_IMG_";
    public static final String IMAGE_EXTENSION = ".jpg";
    public static final String KEY_USERSUPPORTED = "user_supported_type";

    public static final String TAG_TUTORIAL_FRAGMENT = "tag_tut_frag";
    public static final String TAG_Ready_FRAGMENT = "tag_red_frag";
    public static final String TAG_Result_FRAGMENT = "tag_res_frag";
    public static final String TAG_CAMERA_FRAGMENT = "tag_camera_frag";
    public static final String TAG_CONSENT_FRAGMENT = "tag_consent_frag";
    public static final String TAG_SUPPORTED_TYPE_FRAGMENT = "tag_supported_type_frag";
    public static final String TAG_COUNTRY_FRAGMENT = "tag_country_frag";
    public static final String TAG_Upload_FRAGMENT = "tag_upload_frag";
    public static final String TAG_Proof_Select_FRAGMENT = "tag_prof_sel_frag";

    public static final String IS_TO_OPEN_FRONT_CAM = "is_to_open_front_cam";
    public static final String VIDEO_PREFIX_FOR_SERVER = "data:video/mp4;base64,";
    public static final String IMAGE_PREFIX_FOR_SERVER = "data:image/jpg;base64,";
    public static final String PDF_PREFIX_FOR_SERVER = "data:application/pdf;base64,";
    public static final String VERIFICATION_REQUEST_DECLINED = "verification.declined";
    public static final String VERIFICATION_REQUEST_ACCEPTED = "verification.accepted";
    public static final String VERIFICATION_REQUEST_PENDING = "request.pending";
    public static final String VERIFICATION_REQUEST_RECEIVED = "request.received";
    public static final String VERIFICATION_REQUEST_INVALID="request.invalid";
    public static final String VERIFICATION_REQUEST_UNAUTHORIZED="request.unauthorized";
    public static final String VERIFICATION_REQUEST_TIMEOUT = "request.timeout";

    public static final int CODE_SELFIE = 0;
    public static final int CODE_DOCUMENT = 1;
    public static final int CODE_ADDRESS = 2;
    public static final int CODE_CONSENT = 3;
    public static final int CODE_DOCUMENT_TWO = 4;
    public static final String KEY_AUTOCAPTURE = "auto_capture";


    /**
     * Link for production server
     */
//    public static final String Base_URL = "https://app.whistleit.io/";

    /**
     * Link for staging server
     */
    public static final String Base_URL = "https://st-app.whistleit.io/";

    /**
     * Links for production web hooks
     */
//    public static final String CRASH_REPORT_URL = "https://app.whistleit.io/api/webhooks/62f778e7faa05a5a0f0497f8";
//    public static final String TRY_CATCH_URL = "https://app.whistleit.io/api/webhooks/62f778bab206ca4aa45f1ebc";
//    public static final String LOGS_URL = "https://app.whistleit.io/api/webhooks/61f2419b13e01f600627c7d2";

    /**
     * Links for Staging web hooks
     */
    public static final String CRASH_REPORT_URL = "https://app.whistleit.io/api/webhooks/62f779800c080e729c5915e0";
    public static final String TRY_CATCH_URL = "https://app.whistleit.io/api/webhooks/62f779800c080e729c5915e0";
    public static final String LOGS_URL = "https://app.whistleit.io/api/webhooks/62f779800c080e729c5915e0";


    public static final String[] REQUIRED_PERMISSIONS_CAMERA = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    public static final String[] REQUIRED_PERMISSIONS_STORAGE = new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"};
    public static final String[] REQUIRED_PERMISSIONS_AUDIO = new String[]{"android.permission.RECORD_AUDIO"};

}

