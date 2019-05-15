package com.hmdglobal.app.camera.ui.camera2;

import android.hardware.Camera.Face;
import java.lang.reflect.Method;

public class ExtendedFaceWrapper extends Wrapper {
    private static final String CLASS_NAME = "com.qualcomm.qti.camera.ExtendedFace";
    private static Class<?> mExtendFaceClass;
    private static Method method_getBlinkDetected = null;
    private static Method method_getFaceRecognized = null;
    private static Method method_getGazeAngle = null;
    private static Method method_getLeftEyeBlinkDegree = null;
    private static Method method_getLeftRightDirection = null;
    private static Method method_getLeftRightGazeDegree = null;
    private static Method method_getRightEyeBlinkDegree = null;
    private static Method method_getRollDirection = null;
    private static Method method_getSmileDegree = null;
    private static Method method_getSmileScore = null;
    private static Method method_getTopBottomGazeDegree = null;
    private static Method method_getUpDownDirection = null;

    public static boolean isExtendedFaceInstance(Object object) {
        if (mExtendFaceClass == null) {
            try {
                mExtendFaceClass = Class.forName(CLASS_NAME);
            } catch (Exception exception) {
                exception.printStackTrace();
                return false;
            }
        }
        return mExtendFaceClass.isInstance(object);
    }

    public static int getSmileDegree(Face face) {
        int degree = 0;
        try {
            if (method_getSmileDegree == null) {
                method_getSmileDegree = getMethod("getSmileDegree");
            }
            return ((Integer) method_getSmileDegree.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return degree;
        }
    }

    public static int getSmileScore(Face face) {
        int score = 0;
        try {
            if (method_getSmileScore == null) {
                method_getSmileScore = getMethod("getSmileScore");
            }
            return ((Integer) method_getSmileScore.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return score;
        }
    }

    public static int getBlinkDetected(Face face) {
        int blink = 0;
        try {
            if (method_getBlinkDetected == null) {
                method_getBlinkDetected = getMethod("getBlinkDetected");
            }
            return ((Integer) method_getBlinkDetected.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return blink;
        }
    }

    public static int getFaceRecognized(Face face) {
        int faces = 0;
        try {
            if (method_getFaceRecognized == null) {
                method_getFaceRecognized = getMethod("getFaceRecognized");
            }
            return ((Integer) method_getFaceRecognized.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return faces;
        }
    }

    public static int getGazeAngle(Face face) {
        int angle = 0;
        try {
            if (method_getGazeAngle == null) {
                method_getGazeAngle = getMethod("getGazeAngle");
            }
            return ((Integer) method_getGazeAngle.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return angle;
        }
    }

    public static int getUpDownDirection(Face face) {
        int direction = 0;
        try {
            if (method_getUpDownDirection == null) {
                method_getUpDownDirection = getMethod("getUpDownDirection");
            }
            return ((Integer) method_getUpDownDirection.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return direction;
        }
    }

    public static int getLeftRightDirection(Face face) {
        int direction = 0;
        try {
            if (method_getLeftRightDirection == null) {
                method_getLeftRightDirection = getMethod("getLeftRightDirection");
            }
            return ((Integer) method_getLeftRightDirection.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return direction;
        }
    }

    public static int getRollDirection(Face face) {
        int direction = 0;
        try {
            if (method_getRollDirection == null) {
                method_getRollDirection = getMethod("getRollDirection");
            }
            return ((Integer) method_getRollDirection.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return direction;
        }
    }

    public static int getLeftEyeBlinkDegree(Face face) {
        int degree = 0;
        try {
            if (method_getLeftEyeBlinkDegree == null) {
                method_getLeftEyeBlinkDegree = getMethod("getLeftEyeBlinkDegree");
            }
            return ((Integer) method_getLeftEyeBlinkDegree.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return degree;
        }
    }

    public static int getRightEyeBlinkDegree(Face face) {
        int degree = 0;
        try {
            if (method_getRightEyeBlinkDegree == null) {
                method_getRightEyeBlinkDegree = getMethod("getRightEyeBlinkDegree");
            }
            return ((Integer) method_getRightEyeBlinkDegree.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return degree;
        }
    }

    public static int getLeftRightGazeDegree(Face face) {
        int degree = 0;
        try {
            if (method_getLeftRightGazeDegree == null) {
                method_getLeftRightGazeDegree = getMethod("getLeftRightGazeDegree");
            }
            return ((Integer) method_getLeftRightGazeDegree.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return degree;
        }
    }

    public static int getTopBottomGazeDegree(Face face) {
        int degree = 0;
        try {
            if (method_getTopBottomGazeDegree == null) {
                method_getTopBottomGazeDegree = getMethod("getTopBottomGazeDegree");
            }
            return ((Integer) method_getTopBottomGazeDegree.invoke(face, new Object[0])).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return degree;
        }
    }

    private static Method getMethod(String name) throws Exception {
        if (mExtendFaceClass == null) {
            mExtendFaceClass = Class.forName(CLASS_NAME);
        }
        return mExtendFaceClass.getDeclaredMethod(name, new Class[0]);
    }
}
