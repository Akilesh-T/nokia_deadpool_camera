package com.hmdglobal.app.camera.settings;

import android.content.Context;
import com.android.external.ExtendKey;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.LocationManager;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;

public class Keys {
    public static final String BACK_STACK_PREFS = ":settings:prefs";
    public static final String KEY_ANTIBANDING = "pref_camera_antibanding_key";
    public static final String KEY_ANTIBANDING_VALUE = "pref_camera_antibanding_Value_key";
    public static final String KEY_ASK_STORAGE_PATH = "storage_path";
    public static final String KEY_BLURRED_BITMAP_BYTE = "blurred_bitmap";
    public static final String KEY_CAMERA_AIS = "pref_camera_ais_key";
    public static final String KEY_CAMERA_ATTENTIONSEEKER = "pref_camera_attentionseeker_key";
    public static final String KEY_CAMERA_AUTO_HDR = "pref_camera_auto_hdr_key";
    public static final String KEY_CAMERA_DEPTH = "pref_camera_depth_key";
    public static final String KEY_CAMERA_ENABLE_ALGORITHM = "pref_camera_enable_algorithm_key";
    public static final String KEY_CAMERA_FACEBEAUTY = "pref_camera_facebeauty_key";
    public static final String KEY_CAMERA_FIRST_USE_HINT_SHOWN = "pref_camera_first_use_hint_shown_key";
    public static final String KEY_CAMERA_GESTURE_DETECTION = "pref_camera_gesture_detection_key";
    public static final String KEY_CAMERA_GRID_LINES = "pref_camera_grid_lines";
    public static final String KEY_CAMERA_HDR = "pref_camera_hdr_key";
    public static final String KEY_CAMERA_HDR_PLUS = "pref_camera_hdr_plus_key";
    public static final String KEY_CAMERA_ID = "pref_camera_id_key";
    public static final String KEY_CAMERA_LOWLIGHT = "pref_camera_lowlight_key";
    public static final String KEY_CAMERA_LOW_LIGHT = "pref_camera_low_light_key";
    public static final String KEY_CAMERA_MIRROR_SELFIE = "pref_camera_mirror_key";
    public static final String KEY_CAMERA_MODULE_LAST_USED = "pref_camera_module_last_used_index";
    public static final String KEY_CAMERA_MOTION = "pref_camera_motion";
    public static final String KEY_CAMERA_PANO_ORIENTATION = "pref_camera_pano_orientation";
    public static final String KEY_CAMERA_PHOTO_RESOLUTION = "pref_photo_resolution_key";
    public static final String KEY_CAMERA_RAW_FILE = "pref_camera_raw_file_key";
    public static final String KEY_CAMERA_SAVEPATH = "pref_camera_savepath_key";
    public static final String KEY_CAMERA_SCENE_DETECTION = "pref_camera_scene_detection_key";
    public static final String KEY_CAMERA_SHUTTER_CONTROLL = "pref_camera_shutter_controller_key";
    public static final String KEY_CAMERA_VIDEO_RESOLUTION = "pref_video_resolution_key";
    public static final String KEY_CAMERA_WATER_MARK = "pref_water_mark_key";
    public static final String KEY_COUNTDOWN_DURATION = "pref_camera_countdown_duration_key";
    public static final String KEY_CUR_EXPOSURE_TIME_STATE = "curExposureTimeState";
    public static final String KEY_CUR_EXPOSURE_TIME_STATE_FRONT = "curExposureTimeStateFront";
    public static final String KEY_CUR_FOCUS_STATE = "curFocusState";
    public static final String KEY_CUR_FOCUS_STATE_FRONT = "curFocusStateFront";
    public static final String KEY_CUR_WHITE_BALANCE_STATE = "curWhiteBalanceState";
    public static final String KEY_CUR_WHITE_BALANCE_STATE_FRONT = "curWhiteBalanceStateFront";
    public static final String KEY_EXPOSURE = "pref_camera_exposure_key";
    public static final String KEY_EXPOSURE_COMPENSATION_ENABLED = "pref_camera_exposure_compensation_key";
    public static final String KEY_FACEBEAUTY_SKIN_SMOOTHING = "pref_facebeauty_skinsmoothing_key";
    public static final String KEY_FLASH_HIDDEN = "flash_hidden";
    public static final String KEY_FLASH_MODE = "pref_camera_flashmode_key";
    public static final String KEY_FLASH_MODE_BEFORE_BEAUTY = "pref_camera_flashmode_beforebeauty";
    public static final String KEY_FLASH_SUPPORTED_BACK_CAMERA = "pref_flash_supported_back_camera";
    public static final String KEY_FOCUS_MODE = "pref_camera_focusmode_key";
    public static final String KEY_GUIDE = "pref_guide_key";
    public static final String KEY_HDR_HIDDEN = "hdr_hidden";
    public static final String KEY_HDR_MODE_BEFORE_BEAUTY = "pref_camera_hdrmode_beforebeauty";
    public static final String KEY_HDR_PLUS_FLASH_MODE = "pref_hdr_plus_flash_mode";
    public static final String KEY_HELP_TIP_FRONT_CAMERA_FINISHED = "pref_help_tip_front_camera_finished_key";
    public static final String KEY_HELP_TIP_FRONT_CAMERA_OPENED_TIMES = "pref_help_tip_front_camera_opened_times_key";
    public static final String KEY_HELP_TIP_GESTURE_FINISHED = "pref_help_tip_gesture_finished_key";
    public static final String KEY_HELP_TIP_MANUAL_FINISHED = "pref_help_tip_manaul_finished_key";
    public static final String KEY_HELP_TIP_MODE_FINISHED = "pref_help_tip_mode_finished_key";
    public static final String KEY_HELP_TIP_PANO_FINISHED = "pref_help_tip_pano_finished_key";
    public static final String KEY_HELP_TIP_PINCH_ZOOM_FINISHED = "pref_help_tip_pinch_zoom_finished_key";
    public static final String KEY_HELP_TIP_QUICK_SETTINGS_FINISHED = "pref_help_tip_quick_settings_finished_key";
    public static final String KEY_HELP_TIP_RECENT_FINISHED = "pref_help_tip_recent_finished_key";
    public static final String KEY_HELP_TIP_SETTINGS_FINISHED = "pref_help_tip_settings_finished_key";
    public static final String KEY_HELP_TIP_STOP_VIDEO_FINISHED = "pref_help_tip_stop_video_finished_key";
    public static final String KEY_HELP_TIP_SYSTEM_TIME = "pref_help_tip_system_time_key";
    public static final String KEY_HELP_TIP_USER_APP_TIMES = "pref_help_tip_user_app_times_key";
    public static final String KEY_HELP_TIP_VIDEO_SNAP_FINISHED = "pref_help_tip_video_snap_finished_key";
    public static final String KEY_HELP_TIP_VIDEO_TIMES = "pref_help_tip_video_times_key";
    public static final String KEY_HELP_TIP_WELCOME_FINISHED = "pref_help_tip_welcome_finished_key";
    public static final String KEY_HELP_TIP_WELCOME_STEP = "pref_help_tip_welcome_step_key";
    public static final String KEY_JPEG_QUALITY = "pref_camera_jpegquality_key";
    public static final String KEY_MANUAL_EXPOSURE_COMPENSATION_STATE = "curExposureCompensation";
    public static final String KEY_MANUAL_EXPOSURE_COMPENSATION_STATE_FRONT = "curExposureCompensationFront";
    public static final String KEY_MANUAL_ISO_STATE = "curIsoState";
    public static final String KEY_MANUAL_ISO_STATE_FRONT = "curIsoStateFront";
    public static final String KEY_MICROVIDEO_GUIDE = "pref_microvideo_guide_key";
    public static final String KEY_MOTION_MODE_BEFORE_BEAUTY = "pref_camera_motionmode_beforebeauty";
    public static final String KEY_NEW_LAUNCHING_FOR_HDRTOAST = "pref_new_launching_for_hdrtoast_key";
    public static final String KEY_NEW_LAUNCHING_FOR_MICROGUIDE = "pref_new_launching_for_microguide_key";
    public static final String KEY_NEW_LAUNCHING_FOR_MICROTIP = "pref_new_launching_for_microtip_key";
    public static final String KEY_NEW_LAUNCHING_FOR_NIGHTTOAST = "pref_new_launching_for_nighttoast_key";
    public static final String KEY_NEW_LAUNCHING_TIMES_FOR_HDRTOAST = "pref_new_launching_times_for_hdrtoast_key";
    public static final String KEY_NEW_LAUNCHING_TIMES_FOR_MICROTIP = "pref_new_launching_times_for_microtip_key";
    public static final String KEY_NEW_LAUNCHING_TIMES_FOR_NIGHTTOAST = "pref_new_launching_times_for_nighttoast_key";
    public static final String KEY_PICTURE_SIZE_BACK = "pref_camera_picturesize_back_key";
    public static final String KEY_PICTURE_SIZE_FRONT = "pref_camera_picturesize_front_key";
    public static final String KEY_PREVIEW_AREA = "preview_area";
    public static final String KEY_PRO_CURRENT_ET_AUTO_STATES = "pro_current_et_auto_states";
    public static final String KEY_RECORD_LOCATION = "pref_camera_recordlocation_key";
    public static final String KEY_RELEASE_DIALOG_LAST_SHOWN_VERSION = "pref_release_dialog_last_shown_version";
    public static final String KEY_REQUEST_RETURN_HDR_PLUS = "pref_request_return_hdr_plus";
    public static final String KEY_RESTORE_SETTING = "pref_camera_restore_setting";
    public static final String KEY_SCENE_MODE = "pref_camera_scenemode_key";
    public static final String KEY_SECURE_CAMERA = "secure_camera";
    public static final String KEY_SECURE_MODULE_INDEX = "pref_camera_secure_module_index";
    public static final String KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING = "pref_should_show_refocus_viewer_cling";
    public static final String KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING = "pref_should_show_settings_button_cling";
    public static final String KEY_SOUND = "pref_camera_capture_sound";
    public static final String KEY_SQUARE_RETURN_TO_INDEX = "square_return_to_module";
    public static final String KEY_STARTUP_MODULE_INDEX = "camera.startup_module";
    public static final String KEY_SWITCH_CAMERA = "pref_camera2_switch_camera_key";
    public static final String KEY_THUMB_URI = "thumb_uri";
    public static final String KEY_TIPS = "pref_tips_key";
    public static final String KEY_TIZR_PROMPT = "pref_tizr_prompt_key";
    public static final String KEY_UPGRADE_VERSION = "pref_upgrade_version";
    public static final String KEY_USER_SELECTED_ASPECT_RATIO = "pref_user_selected_aspect_ratio";
    public static final String KEY_VIDEOCAMERA_FLASH_MODE = "pref_camera_video_flashmode_key";
    public static final String KEY_VIDEO_BOOM_KEY_TIP = "pref_video_boom_key_tip";
    public static final String KEY_VIDEO_EFFECT = "pref_video_effect_key";
    public static final String KEY_VIDEO_EIS = "pref_video_eis_key";
    public static final String KEY_VIDEO_FIRST_USE_HINT_SHOWN = "pref_video_first_use_hint_shown_key";
    public static final String KEY_VIDEO_QUALITY_BACK = "pref_video_quality_back_key";
    public static final String KEY_VIDEO_QUALITY_FRONT = "pref_video_quality_front_key";
    public static final String SOURCE_CAMERA_ID = "source_camera_id";
    public static final String SOURCE_MODULE_SCOPE = "source_module_scope";

    public static void setDefaults(SettingsManager settingsManager, Context context) {
        boolean defMirrorSelfieOn = false;
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_VDF_COUNT_TIMER, false)) {
            settingsManager.setDefaults(KEY_COUNTDOWN_DURATION, 0, context.getResources().getIntArray(R.array.vdf_pref_countdown_duration));
        } else {
            settingsManager.setDefaults(KEY_COUNTDOWN_DURATION, 0, context.getResources().getIntArray(R.array.pref_countdown_duration));
        }
        settingsManager.setDefaults(KEY_CAMERA_ID, context.getString(R.string.pref_camera_id_default), context.getResources().getStringArray(R.array.camera_id_entryvalues));
        settingsManager.setDefaults(KEY_SCENE_MODE, context.getString(R.string.pref_camera_scenemode_default), context.getResources().getStringArray(R.array.pref_camera_scenemode_entryvalues));
        settingsManager.setDefaults(KEY_SWITCH_CAMERA, context.getString(R.string.pref_switch_camera_default), context.getResources().getStringArray(R.array.camera_switch_entryvalues));
        settingsManager.setDefaults(KEY_FLASH_MODE, context.getString(R.string.pref_camera_flashmode_default), context.getResources().getStringArray(R.array.pref_camera_flashmode_entryvalues));
        settingsManager.setDefaults(KEY_CAMERA_HDR, "auto", context.getResources().getStringArray(R.array.pref_camera_hdr_entryvalues));
        settingsManager.setDefaults(KEY_CAMERA_HDR_PLUS, false);
        settingsManager.setDefaults(KEY_ASK_STORAGE_PATH, false);
        settingsManager.setDefaults(KEY_CAMERA_FIRST_USE_HINT_SHOWN, true);
        settingsManager.setDefaults(KEY_FOCUS_MODE, context.getString(R.string.pref_camera_focusmode_default), context.getResources().getStringArray(R.array.pref_camera_focusmode_entryvalues));
        String videoQualityBackDefaultValue = context.getString(R.string.pref_video_quality_large);
        if (ApiHelper.IS_NEXUS_6) {
            videoQualityBackDefaultValue = context.getString(R.string.pref_video_quality_medium);
        }
        settingsManager.setDefaults(KEY_JPEG_QUALITY, context.getString(R.string.pref_camera_jpeg_quality_normal), context.getResources().getStringArray(R.array.pref_camera_jpeg_quality_entryvalues));
        settingsManager.setDefaults(KEY_VIDEOCAMERA_FLASH_MODE, context.getString(R.string.pref_camera_video_flashmode_default), context.getResources().getStringArray(R.array.pref_camera_video_flashmode_entryvalues));
        settingsManager.setDefaults(KEY_VIDEO_EFFECT, context.getString(R.string.pref_video_effect_default), context.getResources().getStringArray(R.array.pref_video_effect_entryvalues));
        settingsManager.setDefaults(KEY_VIDEO_FIRST_USE_HINT_SHOWN, true);
        settingsManager.setDefaults(KEY_STARTUP_MODULE_INDEX, context.getResources().getInteger(R.integer.camera_mode_photo), context.getResources().getIntArray(R.array.camera_modes));
        settingsManager.setDefaults(KEY_CAMERA_MODULE_LAST_USED, context.getResources().getInteger(R.integer.camera_mode_photo), context.getResources().getIntArray(R.array.camera_modes));
        settingsManager.setDefaults(KEY_CAMERA_PANO_ORIENTATION, context.getString(R.string.pano_orientation_horizontal), context.getResources().getStringArray(R.array.pref_camera_pano_orientation_entryvalues));
        settingsManager.setDefaults(KEY_CAMERA_GRID_LINES, false);
        settingsManager.setDefaults(KEY_CAMERA_MOTION, false);
        settingsManager.setDefaults(KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING, true);
        settingsManager.setDefaults(KEY_HDR_PLUS_FLASH_MODE, context.getString(R.string.pref_camera_hdr_plus_flashmode_default), context.getResources().getStringArray(R.array.pref_camera_hdr_plus_flashmode_entryvalues));
        settingsManager.setDefaults(KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING, true);
        settingsManager.setDefaults(KEY_CAMERA_LOWLIGHT, false);
        settingsManager.setDefaults(KEY_CAMERA_FACEBEAUTY, true);
        settingsManager.setDefaults(KEY_RECORD_LOCATION, true);
        settingsManager.setDefaults(KEY_MICROVIDEO_GUIDE, true);
        settingsManager.setDefaults(KEY_NEW_LAUNCHING_TIMES_FOR_MICROTIP, 0, context.getResources().getIntArray(R.array.camera_launching_times_for_micro_tip));
        settingsManager.setDefaults(KEY_NEW_LAUNCHING_TIMES_FOR_HDRTOAST, 0, context.getResources().getIntArray(R.array.camera_launching_times_for_hdr_toast));
        settingsManager.setDefaults(KEY_NEW_LAUNCHING_TIMES_FOR_NIGHTTOAST, 0, context.getResources().getIntArray(R.array.camera_launching_times_for_night_toast));
        settingsManager.setDefaults(KEY_NEW_LAUNCHING_FOR_MICROTIP, false);
        settingsManager.setDefaults(KEY_NEW_LAUNCHING_FOR_MICROGUIDE, false);
        settingsManager.setDefaults(KEY_NEW_LAUNCHING_FOR_HDRTOAST, false);
        settingsManager.setDefaults(KEY_NEW_LAUNCHING_FOR_NIGHTTOAST, false);
        settingsManager.setDefaults(KEY_CAMERA_LOW_LIGHT, true);
        settingsManager.setDefaults(KEY_CAMERA_ENABLE_ALGORITHM, true);
        CustomUtil util = CustomUtil.getInstance();
        settingsManager.setDefaults(KEY_CAMERA_SHUTTER_CONTROLL, util.getBoolean(CustomFields.DEF_CAMERA_SHUTTER_CONTROL_ON, true));
        settingsManager.setDefaults(KEY_SOUND, util.getBoolean(CustomFields.DEF_CAMERA_SHUTTER_SOUND_ON, true));
        boolean defGestureDetectionEnabled = util.getBoolean(CustomFields.DEF_CAMERA_SUPPORT_GESTURE_SHOT, false) && util.getBoolean(CustomFields.DEF_CAMERA_GESTURE_SHOT_ON, false);
        settingsManager.setDefaults(KEY_CAMERA_GESTURE_DETECTION, defGestureDetectionEnabled);
        settingsManager.setDefaults(KEY_CAMERA_DEPTH, util.getBoolean(CustomFields.DEF_CAMERA_DEPTH_ON, false));
        boolean defAttentionSeekerOn = util.getBoolean(CustomFields.DEF_CAMERA_SUPPORT_ATTENTION_SEEKER, false) && util.getBoolean(CustomFields.DEF_CAMERA_ATTENTION_SEEKER_ON, false);
        settingsManager.setDefaults(KEY_CAMERA_ATTENTIONSEEKER, defAttentionSeekerOn);
        boolean defEisOn = util.getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HMD_EIS, false) && util.getBoolean(CustomFields.DEF_CAMERA_HMD_EIS_ON, false);
        settingsManager.setDefaults(KEY_VIDEO_EIS, defEisOn);
        if (util.getBoolean(CustomFields.DEF_CAMERA_SUPPORT_MIRROR_SELFIE, true) && util.getBoolean(CustomFields.DEF_CAMERA_MIRROR_SELFIE_ON, true)) {
            defMirrorSelfieOn = true;
        }
        settingsManager.setDefaults(KEY_CAMERA_MIRROR_SELFIE, defMirrorSelfieOn);
        settingsManager.setDefaults(KEY_ANTIBANDING, util.getString(CustomFields.DEF_ANTIBAND_DEFAULT, context.getResources().getString(R.string.pref_camera_antibanding_default)), context.getResources().getStringArray(R.array.pref_camera_antibanding_entryvalues));
        settingsManager.setDefaults(KEY_PRO_CURRENT_ET_AUTO_STATES, true);
        setToDefaults(settingsManager, context);
    }

    public static void setToDefaults(SettingsManager settingsManager, Context context) {
        if (!settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_GESTURE_DETECTION)) {
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_GESTURE_DETECTION);
        }
        if (!settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_ATTENTIONSEEKER)) {
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_ATTENTIONSEEKER);
        }
        if (!settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, KEY_VIDEO_EIS)) {
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, KEY_VIDEO_EIS);
        }
        if (!settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_MIRROR_SELFIE)) {
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_MIRROR_SELFIE);
        }
        if (!settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, KEY_ANTIBANDING)) {
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, KEY_ANTIBANDING);
        }
        if (!settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, KEY_SOUND)) {
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, KEY_SOUND);
        }
        if (!settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_DEPTH)) {
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_DEPTH);
        }
        if (!settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_SHUTTER_CONTROLL)) {
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_SHUTTER_CONTROLL);
        }
    }

    public static boolean isCameraBackFacing(SettingsManager settingsManager, String moduleScope) {
        return settingsManager.isDefault(moduleScope, KEY_CAMERA_ID);
    }

    public static boolean isHdrPlusOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_HDR_PLUS);
    }

    public static boolean isHdrOn(SettingsManager settingsManager) {
        return ExtendKey.FLIP_MODE_OFF.equals(getHdrMode(settingsManager)) ^ 1;
    }

    public static String getHdrMode(SettingsManager settingsManager) {
        return settingsManager.getString(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_HDR);
    }

    public static boolean requestsReturnToHdrPlus(SettingsManager settingsManager, String moduleScope) {
        return settingsManager.getBoolean(moduleScope, KEY_REQUEST_RETURN_HDR_PLUS);
    }

    public static boolean areGridLinesOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_GRID_LINES);
    }

    public static boolean isMotionOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_MOTION);
    }

    public static boolean isPanoOrientationHorizontal(SettingsManager settingsManager) {
        return settingsManager.isDefault(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_PANO_ORIENTATION);
    }

    public static void setLocation(SettingsManager settingsManager, boolean on, LocationManager locationManager) {
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_RECORD_LOCATION, on);
        locationManager.recordLocation(on);
    }

    public static void setAspectRatioSelected(SettingsManager settingsManager) {
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_USER_SELECTED_ASPECT_RATIO, true);
    }

    public static void setManualExposureCompensation(SettingsManager settingsManager, boolean on) {
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_EXPOSURE_COMPENSATION_ENABLED, on);
    }

    public static void syncLocationManager(SettingsManager settingsManager, LocationManager locationManager) {
        locationManager.recordLocation(settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_RECORD_LOCATION));
    }

    public static void pauseLocationManager(LocationManager locationManager) {
        locationManager.recordLocation(false);
    }

    public static boolean isShutterSoundOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_SOUND);
    }

    public static boolean isShutterControlOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_SHUTTER_CONTROLL);
    }

    public static boolean isVideoStabilizationEnabled(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_VIDEO_EIS);
    }

    public static boolean isLowlightOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_LOWLIGHT);
    }

    public static boolean isGestureDetectionOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_GESTURE_DETECTION);
    }

    public static boolean isFacebeautyOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_FACEBEAUTY);
    }

    public static boolean isAttentionseekerOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_ATTENTIONSEEKER);
    }

    public static void setMicroGuide(SettingsManager settingsManager, boolean show) {
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_MICROVIDEO_GUIDE, show);
    }

    public static boolean isShowMicroGuide(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_MICROVIDEO_GUIDE);
    }

    public static void setNewLaunchingForMicrotip(SettingsManager settingsManager, boolean isNew) {
        if (isNewLaunchingForMicrotip(settingsManager) != isNew) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_NEW_LAUNCHING_FOR_MICROTIP, isNew);
        }
    }

    public static boolean isNewLaunchingForMicrotip(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_NEW_LAUNCHING_FOR_MICROTIP);
    }

    public static void setNewLaunchingForMicroguide(SettingsManager settingsManager, boolean isNew) {
        if (isNewLaunchingForMicroguide(settingsManager) != isNew) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_NEW_LAUNCHING_FOR_MICROGUIDE, isNew);
        }
    }

    public static boolean isNewLaunchingForMicroguide(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_NEW_LAUNCHING_FOR_MICROGUIDE);
    }

    public static void setNewLaunchingForHdrtoast(SettingsManager settingsManager, boolean isNew) {
        if (isNewLaunchingForHdrtoast(settingsManager) != isNew) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_NEW_LAUNCHING_FOR_HDRTOAST, isNew);
        }
    }

    public static boolean isNewLaunchingForHdrtoast(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_NEW_LAUNCHING_FOR_HDRTOAST);
    }

    public static void setNewLaunchingForNighttoast(SettingsManager settingsManager, boolean isNew) {
        if (isNewLaunchingForNighttoast(settingsManager) != isNew) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_NEW_LAUNCHING_FOR_NIGHTTOAST, isNew);
        }
    }

    public static boolean isNewLaunchingForNighttoast(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_NEW_LAUNCHING_FOR_NIGHTTOAST);
    }

    public static boolean isMirrorSelfieOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_MIRROR_SELFIE);
    }

    public static boolean isLowLightModeOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_LOW_LIGHT);
    }

    public static boolean isAlgorithmsOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_ENABLE_ALGORITHM);
    }

    public static boolean isDepthOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_DEPTH);
    }

    public static String getAntibandingValue(SettingsManager settingsManager) {
        return settingsManager.getString(SettingsManager.SCOPE_GLOBAL, KEY_ANTIBANDING_VALUE);
    }

    public static void resetSecureModuleIndex(SettingsManager settingsManager, int modeIndex) {
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_SECURE_MODULE_INDEX, modeIndex);
    }
}
