package com.android.ex.camera2.portability;

public class CameraActions {
    public static final int ABORT_PRE_ALLOC_BURST_SHOT = 605;
    public static final int ABORT_SHOT = 603;
    public static final int ADD_CALLBACK_BUFFER = 105;
    public static final int APPLY_PREVIEW_RELATED_SETTINGS = 205;
    public static final int APPLY_SETTINGS = 204;
    public static final int AUTO_FOCUS = 301;
    public static final int BURST_SHOT = 602;
    public static final int CANCEL_AUTO_FOCUS = 302;
    public static final int CANCEL_AUTO_FOCUS_FINISH = 305;
    public static final int CAPTURE_PHOTO = 601;
    public static final int ENABLE_SHUTTER_SOUND = 501;
    public static final int FAKE_SET_PREVIEW_TEXTURE_ASYNC = 701;
    public static final int FAKE_START_PREVIEW_ASYNC = 702;
    public static final int FAKE_STOP_PREVIEW = 703;
    public static final int GET_PARAMETERS = 202;
    public static final int LOCK = 5;
    public static final int OPEN_CAMERA = 1;
    public static final int PRE_ALLOC_BURST_SHOT = 604;
    public static final int RECONNECT = 3;
    public static final int REFRESH_PARAMETERS = 203;
    public static final int RELEASE = 2;
    public static final int SET_AUTO_FOCUS_MOVE_CALLBACK = 303;
    public static final int SET_BOLKEN_LEVEL = 800;
    public static final int SET_DISPLAY_ORIENTATION = 502;
    public static final int SET_FACE_DETECTION_LISTENER = 461;
    public static final int SET_JPEG_ORIENTATION = 503;
    public static final int SET_ONE_SHOT_PREVIEW_CALLBACK = 108;
    public static final int SET_PARAMETERS = 201;
    public static final int SET_PREVIEW_CALLBACK = 107;
    public static final int SET_PREVIEW_CALLBACK_WITH_BUFFER = 104;
    public static final int SET_PREVIEW_DISPLAY_ASYNC = 106;
    public static final int SET_PREVIEW_TEXTURE_ASYNC = 101;
    public static final int SET_ZOOM_CHANGE_LISTENER = 304;
    public static final int START_FACE_DETECTION = 462;
    public static final int START_PREVIEW_ASYNC = 102;
    public static final int STOP_FACE_DETECTION = 463;
    public static final int STOP_PREVIEW = 103;
    public static final int UNLOCK = 4;
    public static final int WAIT_PREVIEW_ASYNC = 109;

    public static String stringify(int action) {
        if (action == CAPTURE_PHOTO) {
            return "CAPTURE_PHOTO";
        }
        switch (action) {
            case 1:
                return "OPEN_CAMERA";
            case 2:
                return "RELEASE";
            case 3:
                return "RECONNECT";
            case 4:
                return "UNLOCK";
            case 5:
                return "LOCK";
            default:
                switch (action) {
                    case 101:
                        return "SET_PREVIEW_TEXTURE_ASYNC";
                    case 102:
                        return "START_PREVIEW_ASYNC";
                    case 103:
                        return "STOP_PREVIEW";
                    case 104:
                        return "SET_PREVIEW_CALLBACK_WITH_BUFFER";
                    case 105:
                        return "ADD_CALLBACK_BUFFER";
                    case 106:
                        return "SET_PREVIEW_DISPLAY_ASYNC";
                    case 107:
                        return "SET_PREVIEW_CALLBACK";
                    case 108:
                        return "SET_ONE_SHOT_PREVIEW_CALLBACK";
                    default:
                        switch (action) {
                            case 201:
                                return "SET_PARAMETERS";
                            case 202:
                                return "GET_PARAMETERS";
                            case 203:
                                return "REFRESH_PARAMETERS";
                            case 204:
                                return "APPLY_SETTINGS";
                            default:
                                switch (action) {
                                    case AUTO_FOCUS /*301*/:
                                        return "AUTO_FOCUS";
                                    case CANCEL_AUTO_FOCUS /*302*/:
                                        return "CANCEL_AUTO_FOCUS";
                                    case SET_AUTO_FOCUS_MOVE_CALLBACK /*303*/:
                                        return "SET_AUTO_FOCUS_MOVE_CALLBACK";
                                    case SET_ZOOM_CHANGE_LISTENER /*304*/:
                                        return "SET_ZOOM_CHANGE_LISTENER";
                                    case CANCEL_AUTO_FOCUS_FINISH /*305*/:
                                        return "CANCEL_AUTO_FOCUS_FINISH";
                                    default:
                                        switch (action) {
                                            case SET_FACE_DETECTION_LISTENER /*461*/:
                                                return "SET_FACE_DETECTION_LISTENER";
                                            case START_FACE_DETECTION /*462*/:
                                                return "START_FACE_DETECTION";
                                            case STOP_FACE_DETECTION /*463*/:
                                                return "STOP_FACE_DETECTION";
                                            default:
                                                switch (action) {
                                                    case ENABLE_SHUTTER_SOUND /*501*/:
                                                        return "ENABLE_SHUTTER_SOUND";
                                                    case SET_DISPLAY_ORIENTATION /*502*/:
                                                        return "SET_DISPLAY_ORIENTATION";
                                                    default:
                                                        StringBuilder stringBuilder = new StringBuilder();
                                                        stringBuilder.append("UNKNOWN(");
                                                        stringBuilder.append(action);
                                                        stringBuilder.append(")");
                                                        return stringBuilder.toString();
                                                }
                                        }
                                }
                        }
                }
        }
    }

    private CameraActions() {
        throw new AssertionError();
    }
}
