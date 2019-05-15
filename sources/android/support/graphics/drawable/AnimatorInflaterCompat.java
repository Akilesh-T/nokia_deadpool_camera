package android.support.graphics.drawable;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.Build.VERSION;
import android.support.annotation.AnimatorRes;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.graphics.PathParser;
import android.support.v4.graphics.PathParser.PathDataNode;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.InflateException;
import com.hmdglobal.app.camera.provider.InfoTable;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

@RestrictTo({Scope.LIBRARY_GROUP})
public class AnimatorInflaterCompat {
    private static final boolean DBG_ANIMATOR_INFLATER = false;
    private static final int MAX_NUM_POINTS = 100;
    private static final String TAG = "AnimatorInflater";
    private static final int TOGETHER = 0;
    private static final int VALUE_TYPE_COLOR = 3;
    private static final int VALUE_TYPE_FLOAT = 0;
    private static final int VALUE_TYPE_INT = 1;
    private static final int VALUE_TYPE_PATH = 2;
    private static final int VALUE_TYPE_UNDEFINED = 4;

    private static class PathDataEvaluator implements TypeEvaluator<PathDataNode[]> {
        private PathDataNode[] mNodeArray;

        private PathDataEvaluator() {
        }

        PathDataEvaluator(PathDataNode[] nodeArray) {
            this.mNodeArray = nodeArray;
        }

        public PathDataNode[] evaluate(float fraction, PathDataNode[] startPathData, PathDataNode[] endPathData) {
            if (PathParser.canMorph(startPathData, endPathData)) {
                if (this.mNodeArray == null || !PathParser.canMorph(this.mNodeArray, startPathData)) {
                    this.mNodeArray = PathParser.deepCopyNodes(startPathData);
                }
                for (int i = 0; i < startPathData.length; i++) {
                    this.mNodeArray[i].interpolatePathDataNode(startPathData[i], endPathData[i], fraction);
                }
                return this.mNodeArray;
            }
            throw new IllegalArgumentException("Can't interpolate between two incompatible pathData");
        }
    }

    public static Animator loadAnimator(Context context, @AnimatorRes int id) throws NotFoundException {
        if (VERSION.SDK_INT >= 24) {
            return AnimatorInflater.loadAnimator(context, id);
        }
        return loadAnimator(context, context.getResources(), context.getTheme(), id);
    }

    public static Animator loadAnimator(Context context, Resources resources, Theme theme, @AnimatorRes int id) throws NotFoundException {
        return loadAnimator(context, resources, theme, id, 1.0f);
    }

    public static Animator loadAnimator(Context context, Resources resources, Theme theme, @AnimatorRes int id, float pathErrorScale) throws NotFoundException {
        StringBuilder stringBuilder;
        NotFoundException rnf;
        XmlResourceParser parser = null;
        try {
            parser = resources.getAnimation(id);
            Animator animator = createAnimatorFromXml(context, resources, theme, parser, pathErrorScale);
            if (parser != null) {
                parser.close();
            }
            return animator;
        } catch (XmlPullParserException ex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't load animation resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            rnf = new NotFoundException(stringBuilder.toString());
            rnf.initCause(ex);
            throw rnf;
        } catch (IOException ex2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can't load animation resource ID #0x");
            stringBuilder.append(Integer.toHexString(id));
            rnf = new NotFoundException(stringBuilder.toString());
            rnf.initCause(ex2);
            throw rnf;
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private static PropertyValuesHolder getPVH(TypedArray styledAttributes, int valueType, int valueFromId, int valueToId, String propertyName) {
        int valueType2;
        PropertyValuesHolder returnValue;
        TypedArray typedArray = styledAttributes;
        int i = valueFromId;
        int i2 = valueToId;
        String str = propertyName;
        TypedValue tvFrom = typedArray.peekValue(i);
        boolean hasFrom = tvFrom != null;
        int fromType = hasFrom ? tvFrom.type : 0;
        TypedValue tvTo = typedArray.peekValue(i2);
        boolean hasTo = tvTo != null;
        int toType = hasTo ? tvTo.type : 0;
        int i3 = valueType;
        if (i3 != 4) {
            valueType2 = i3;
        } else if ((hasFrom && isColorType(fromType)) || (hasTo && isColorType(toType))) {
            valueType2 = 3;
        } else {
            valueType2 = 0;
        }
        boolean getFloats = valueType2 == 0;
        TypedValue typedValue;
        int toType2;
        PropertyValuesHolder propertyValuesHolder;
        if (valueType2 == 2) {
            String fromString = typedArray.getString(i);
            String toString = typedArray.getString(i2);
            PathDataNode[] nodesFrom = PathParser.createNodesFromPathData(fromString);
            tvFrom = PathParser.createNodesFromPathData(toString);
            if (nodesFrom == null && tvFrom == null) {
                typedValue = tvTo;
                toType2 = toType;
                propertyValuesHolder = null;
            } else {
                if (nodesFrom != null) {
                    propertyValuesHolder = null;
                    PathDataEvaluator evaluator = new PathDataEvaluator();
                    if (tvFrom == null) {
                        toType2 = toType;
                        returnValue = PropertyValuesHolder.ofObject(str, evaluator, new Object[]{nodesFrom});
                    } else if (PathParser.canMorph(nodesFrom, tvFrom)) {
                        returnValue = PropertyValuesHolder.ofObject(str, evaluator, new Object[]{nodesFrom, tvFrom});
                        toType2 = toType;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(" Can't morph from ");
                        stringBuilder.append(fromString);
                        stringBuilder.append(" to ");
                        stringBuilder.append(toString);
                        throw new InflateException(stringBuilder.toString());
                    }
                }
                toType2 = toType;
                propertyValuesHolder = null;
                if (tvFrom != null) {
                    returnValue = PropertyValuesHolder.ofObject(str, new PathDataEvaluator(), new Object[]{tvFrom});
                }
                tvTo = toType2;
                toType = valueToId;
            }
            returnValue = propertyValuesHolder;
            tvTo = toType2;
            toType = valueToId;
        } else {
            typedValue = tvTo;
            toType2 = toType;
            propertyValuesHolder = null;
            TypeEvaluator evaluator2 = null;
            if (valueType2 == 3) {
                evaluator2 = ArgbEvaluator.getInstance();
            }
            if (getFloats) {
                float valueTo;
                if (hasFrom) {
                    float valueFrom;
                    if (fromType == 5) {
                        valueFrom = typedArray.getDimension(i, 0.0f);
                    } else {
                        valueFrom = typedArray.getFloat(i, 0.0f);
                    }
                    if (hasTo) {
                        if (toType2 == 5) {
                            valueTo = typedArray.getDimension(valueToId, 0.0f);
                        } else {
                            valueTo = typedArray.getFloat(valueToId, 0.0f);
                        }
                        returnValue = PropertyValuesHolder.ofFloat(str, new float[]{valueFrom, valueTo});
                    } else {
                        toType = valueToId;
                        valueTo = PropertyValuesHolder.ofFloat(str, new float[]{valueFrom});
                    }
                } else {
                    toType = valueToId;
                    if (toType2 == 5) {
                        valueTo = typedArray.getDimension(toType, 0.0f);
                    } else {
                        valueTo = typedArray.getFloat(toType, 0.0f);
                    }
                    valueTo = PropertyValuesHolder.ofFloat(str, new float[]{valueTo});
                }
                returnValue = valueTo;
            } else {
                int toType3 = toType2;
                toType = valueToId;
                int valueTo2;
                int i4;
                if (hasFrom) {
                    int valueFrom2;
                    if (fromType == 5) {
                        valueFrom2 = (int) typedArray.getDimension(i, 0.0f);
                    } else if (isColorType(fromType)) {
                        valueFrom2 = typedArray.getColor(i, 0);
                    } else {
                        valueFrom2 = typedArray.getInt(i, 0);
                    }
                    if (hasTo) {
                        if (toType3 == 5) {
                            valueTo2 = (int) typedArray.getDimension(toType, 0.0f);
                            i4 = 0;
                        } else if (isColorType(toType3)) {
                            i4 = 0;
                            valueTo2 = typedArray.getColor(toType, 0);
                        } else {
                            i4 = 0;
                            valueTo2 = typedArray.getInt(toType, 0);
                        }
                        returnValue = PropertyValuesHolder.ofInt(str, new int[]{valueFrom, valueTo2});
                    } else {
                        returnValue = PropertyValuesHolder.ofInt(str, new int[]{valueFrom});
                    }
                } else if (hasTo) {
                    if (toType3 == 5) {
                        valueTo2 = (int) typedArray.getDimension(toType, 0.0f);
                        i4 = 0;
                    } else if (isColorType(toType3)) {
                        i4 = 0;
                        valueTo2 = typedArray.getColor(toType, 0);
                    } else {
                        i4 = 0;
                        valueTo2 = typedArray.getInt(toType, 0);
                    }
                    returnValue = PropertyValuesHolder.ofInt(str, new int[]{valueTo2});
                } else {
                    returnValue = propertyValuesHolder;
                }
            }
            if (!(returnValue == null || evaluator2 == null)) {
                returnValue.setEvaluator(evaluator2);
            }
        }
        return returnValue;
    }

    private static void parseAnimatorFromTypeArray(ValueAnimator anim, TypedArray arrayAnimator, TypedArray arrayObjectAnimator, float pixelSize, XmlPullParser parser) {
        long duration = (long) TypedArrayUtils.getNamedInt(arrayAnimator, parser, InfoTable.DURATION, 1, 300);
        long startDelay = (long) TypedArrayUtils.getNamedInt(arrayAnimator, parser, "startOffset", 2, 0);
        int valueType = TypedArrayUtils.getNamedInt(arrayAnimator, parser, "valueType", 7, 4);
        if (TypedArrayUtils.hasAttribute(parser, "valueFrom") && TypedArrayUtils.hasAttribute(parser, "valueTo")) {
            if (valueType == 4) {
                valueType = inferValueTypeFromValues(arrayAnimator, 5, 6);
            }
            if (getPVH(arrayAnimator, valueType, 5, 6, "") != null) {
                anim.setValues(new PropertyValuesHolder[]{getPVH(arrayAnimator, valueType, 5, 6, "")});
            }
        }
        anim.setDuration(duration);
        anim.setStartDelay(startDelay);
        anim.setRepeatCount(TypedArrayUtils.getNamedInt(arrayAnimator, parser, "repeatCount", 3, 0));
        anim.setRepeatMode(TypedArrayUtils.getNamedInt(arrayAnimator, parser, "repeatMode", 4, 1));
        if (arrayObjectAnimator != null) {
            setupObjectAnimator(anim, arrayObjectAnimator, valueType, pixelSize, parser);
        }
    }

    private static void setupObjectAnimator(ValueAnimator anim, TypedArray arrayObjectAnimator, int valueType, float pixelSize, XmlPullParser parser) {
        ObjectAnimator oa = (ObjectAnimator) anim;
        String pathData = TypedArrayUtils.getNamedString(arrayObjectAnimator, parser, "pathData", 1);
        if (pathData != null) {
            String propertyXName = TypedArrayUtils.getNamedString(arrayObjectAnimator, parser, "propertyXName", 2);
            String propertyYName = TypedArrayUtils.getNamedString(arrayObjectAnimator, parser, "propertyYName", 3);
            if (valueType == 2 || valueType == 4) {
            }
            if (propertyXName == null && propertyYName == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(arrayObjectAnimator.getPositionDescription());
                stringBuilder.append(" propertyXName or propertyYName is needed for PathData");
                throw new InflateException(stringBuilder.toString());
            }
            setupPathMotion(PathParser.createPathFromPathData(pathData), oa, 0.5f * pixelSize, propertyXName, propertyYName);
            return;
        }
        oa.setPropertyName(TypedArrayUtils.getNamedString(arrayObjectAnimator, parser, "propertyName", 0));
    }

    private static void setupPathMotion(Path path, ObjectAnimator oa, float precision, String propertyXName, String propertyYName) {
        Path path2 = path;
        ObjectAnimator objectAnimator = oa;
        String str = propertyXName;
        String str2 = propertyYName;
        PathMeasure measureForTotalLength = new PathMeasure(path2, false);
        float totalLength = 0.0f;
        ArrayList<Float> contourLengths = new ArrayList();
        contourLengths.add(Float.valueOf(0.0f));
        do {
            totalLength += measureForTotalLength.getLength();
            contourLengths.add(Float.valueOf(totalLength));
        } while (measureForTotalLength.nextContour());
        PathMeasure pathMeasure = new PathMeasure(path2, false);
        int numPoints = Math.min(100, ((int) (totalLength / precision)) + 1);
        float[] mX = new float[numPoints];
        float[] mY = new float[numPoints];
        float[] position = new float[2];
        float step = totalLength / ((float) (numPoints - 1));
        float currentDistance = 0.0f;
        int contourIndex = 0;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= numPoints) {
                break;
            }
            PathMeasure measureForTotalLength2 = measureForTotalLength;
            pathMeasure.getPosTan(currentDistance - ((Float) contourLengths.get(contourIndex)).floatValue(), position, null);
            mX[i2] = position[0];
            mY[i2] = position[1];
            currentDistance += step;
            if (contourIndex + 1 < contourLengths.size() && currentDistance > ((Float) contourLengths.get(contourIndex + 1)).floatValue()) {
                contourIndex++;
                pathMeasure.nextContour();
            }
            i = i2 + 1;
            measureForTotalLength = measureForTotalLength2;
            path2 = path;
        }
        PropertyValuesHolder x = null;
        PropertyValuesHolder y = null;
        if (str != null) {
            x = PropertyValuesHolder.ofFloat(str, mX);
        }
        if (str2 != null) {
            y = PropertyValuesHolder.ofFloat(str2, mY);
        }
        if (x == null) {
            objectAnimator.setValues(new PropertyValuesHolder[]{y});
        } else if (y == null) {
            objectAnimator.setValues(new PropertyValuesHolder[]{x});
        } else {
            objectAnimator.setValues(new PropertyValuesHolder[]{x, y});
        }
    }

    private static Animator createAnimatorFromXml(Context context, Resources res, Theme theme, XmlPullParser parser, float pixelSize) throws XmlPullParserException, IOException {
        return createAnimatorFromXml(context, res, theme, parser, Xml.asAttributeSet(parser), null, 0, pixelSize);
    }

    /* JADX WARNING: Removed duplicated region for block: B:33:0x00cf  */
    private static android.animation.Animator createAnimatorFromXml(android.content.Context r20, android.content.res.Resources r21, android.content.res.Resources.Theme r22, org.xmlpull.v1.XmlPullParser r23, android.util.AttributeSet r24, android.animation.AnimatorSet r25, int r26, float r27) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        /*
        r8 = r21;
        r9 = r22;
        r10 = r23;
        r11 = r25;
        r0 = 0;
        r1 = 0;
        r2 = r23.getDepth();
        r7 = r0;
        r12 = r1;
    L_0x0010:
        r13 = r2;
        r0 = r23.next();
        r14 = r0;
        r1 = 3;
        if (r0 != r1) goto L_0x0026;
    L_0x0019:
        r0 = r23.getDepth();
        if (r0 <= r13) goto L_0x0020;
    L_0x001f:
        goto L_0x0026;
    L_0x0020:
        r1 = r20;
        r19 = r13;
        goto L_0x00fe;
    L_0x0026:
        r0 = 1;
        if (r14 == r0) goto L_0x00fa;
    L_0x0029:
        r0 = 2;
        if (r14 == r0) goto L_0x002f;
        r2 = r13;
        goto L_0x0010;
    L_0x002f:
        r15 = r23.getName();
        r16 = 0;
        r0 = "objectAnimator";
        r0 = r15.equals(r0);
        if (r0 == 0) goto L_0x0051;
    L_0x003d:
        r0 = r20;
        r1 = r8;
        r2 = r9;
        r3 = r24;
        r4 = r27;
        r5 = r10;
        r0 = loadObjectAnimator(r0, r1, r2, r3, r4, r5);
    L_0x004a:
        r1 = r20;
        r7 = r0;
        r19 = r13;
        goto L_0x00c9;
    L_0x0051:
        r0 = "animator";
        r0 = r15.equals(r0);
        if (r0 == 0) goto L_0x0068;
    L_0x0059:
        r4 = 0;
        r0 = r20;
        r1 = r8;
        r2 = r9;
        r3 = r24;
        r5 = r27;
        r6 = r10;
        r0 = loadAnimator(r0, r1, r2, r3, r4, r5, r6);
        goto L_0x004a;
    L_0x0068:
        r0 = "set";
        r0 = r15.equals(r0);
        if (r0 == 0) goto L_0x00a3;
    L_0x0070:
        r0 = new android.animation.AnimatorSet;
        r0.<init>();
        r17 = r0;
        r0 = android.support.graphics.drawable.AndroidResources.STYLEABLE_ANIMATOR_SET;
        r7 = r24;
        r6 = android.support.v4.content.res.TypedArrayUtils.obtainAttributes(r8, r9, r7, r0);
        r0 = "ordering";
        r1 = 0;
        r18 = android.support.v4.content.res.TypedArrayUtils.getNamedInt(r6, r10, r0, r1, r1);
        r5 = r17;
        r5 = (android.animation.AnimatorSet) r5;
        r0 = r20;
        r1 = r8;
        r2 = r9;
        r3 = r10;
        r4 = r7;
        r19 = r13;
        r13 = r6;
        r6 = r18;
        r7 = r27;
        createAnimatorFromXml(r0, r1, r2, r3, r4, r5, r6, r7);
        r13.recycle();
        r1 = r20;
        r7 = r17;
        goto L_0x00c9;
    L_0x00a3:
        r19 = r13;
        r0 = "propertyValuesHolder";
        r0 = r15.equals(r0);
        if (r0 == 0) goto L_0x00dd;
        r0 = android.util.Xml.asAttributeSet(r23);
        r1 = r20;
        r0 = loadValues(r1, r8, r9, r10, r0);
        if (r0 == 0) goto L_0x00c6;
    L_0x00ba:
        if (r7 == 0) goto L_0x00c6;
    L_0x00bc:
        r2 = r7 instanceof android.animation.ValueAnimator;
        if (r2 == 0) goto L_0x00c6;
    L_0x00c0:
        r2 = r7;
        r2 = (android.animation.ValueAnimator) r2;
        r2.setValues(r0);
    L_0x00c6:
        r16 = 1;
    L_0x00c9:
        if (r11 == 0) goto L_0x00d8;
    L_0x00cb:
        if (r16 != 0) goto L_0x00d8;
    L_0x00cd:
        if (r12 != 0) goto L_0x00d5;
    L_0x00cf:
        r0 = new java.util.ArrayList;
        r0.<init>();
        r12 = r0;
    L_0x00d5:
        r12.add(r7);
        r2 = r19;
        goto L_0x0010;
    L_0x00dd:
        r1 = r20;
        r0 = new java.lang.RuntimeException;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "Unknown animator name: ";
        r2.append(r3);
        r3 = r23.getName();
        r2.append(r3);
        r2 = r2.toString();
        r0.<init>(r2);
        throw r0;
    L_0x00fa:
        r1 = r20;
        r19 = r13;
    L_0x00fe:
        if (r11 == 0) goto L_0x0129;
    L_0x0100:
        if (r12 == 0) goto L_0x0129;
    L_0x0102:
        r0 = r12.size();
        r0 = new android.animation.Animator[r0];
        r2 = 0;
        r3 = r12.iterator();
    L_0x010d:
        r4 = r3.hasNext();
        if (r4 == 0) goto L_0x0120;
    L_0x0113:
        r4 = r3.next();
        r4 = (android.animation.Animator) r4;
        r5 = r2 + 1;
        r0[r2] = r4;
        r2 = r5;
        goto L_0x010d;
    L_0x0120:
        if (r26 != 0) goto L_0x0126;
    L_0x0122:
        r11.playTogether(r0);
        goto L_0x0129;
    L_0x0126:
        r11.playSequentially(r0);
    L_0x0129:
        return r7;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.graphics.drawable.AnimatorInflaterCompat.createAnimatorFromXml(android.content.Context, android.content.res.Resources, android.content.res.Resources$Theme, org.xmlpull.v1.XmlPullParser, android.util.AttributeSet, android.animation.AnimatorSet, int, float):android.animation.Animator");
    }

    private static PropertyValuesHolder[] loadValues(Context context, Resources res, Theme theme, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
        ArrayList<PropertyValuesHolder> values;
        Resources resources;
        Theme theme2;
        AttributeSet attributeSet;
        PropertyValuesHolder[] valuesArray;
        XmlPullParser xmlPullParser = parser;
        ArrayList<PropertyValuesHolder> values2 = null;
        while (true) {
            values = values2;
            int eventType = parser.getEventType();
            int type = eventType;
            if (eventType == 3 || type == 1) {
                resources = res;
                theme2 = theme;
                attributeSet = attrs;
                valuesArray = null;
            } else if (type != 2) {
                parser.next();
                values2 = values;
            } else {
                if (parser.getName().equals("propertyValuesHolder")) {
                    resources = res;
                    theme2 = theme;
                    TypedArray a = TypedArrayUtils.obtainAttributes(resources, theme2, attrs, AndroidResources.STYLEABLE_PROPERTY_VALUES_HOLDER);
                    String propertyName = TypedArrayUtils.getNamedString(a, xmlPullParser, "propertyName", 3);
                    int valueType = TypedArrayUtils.getNamedInt(a, xmlPullParser, "valueType", 2, 4);
                    String propertyName2 = propertyName;
                    PropertyValuesHolder pvh = loadPvh(context, resources, theme2, xmlPullParser, propertyName, valueType);
                    if (pvh == null) {
                        pvh = getPVH(a, valueType, 0, 1, propertyName2);
                    }
                    if (pvh != null) {
                        if (values == null) {
                            values = new ArrayList();
                        }
                        values.add(pvh);
                    }
                    a.recycle();
                } else {
                    resources = res;
                    theme2 = theme;
                    attributeSet = attrs;
                }
                values2 = values;
                parser.next();
            }
        }
        resources = res;
        theme2 = theme;
        attributeSet = attrs;
        valuesArray = null;
        if (values != null) {
            int count = values.size();
            valuesArray = new PropertyValuesHolder[count];
            for (int i = 0; i < count; i++) {
                valuesArray[i] = (PropertyValuesHolder) values.get(i);
            }
        }
        return valuesArray;
    }

    private static int inferValueTypeOfKeyframe(Resources res, Theme theme, AttributeSet attrs, XmlPullParser parser) {
        TypedArray a = TypedArrayUtils.obtainAttributes(res, theme, attrs, AndroidResources.STYLEABLE_KEYFRAME);
        int valueType = 0;
        TypedValue keyframeValue = TypedArrayUtils.peekNamedValue(a, parser, "value", 0);
        if ((keyframeValue != null) && isColorType(keyframeValue.type)) {
            valueType = 3;
        }
        a.recycle();
        return valueType;
    }

    private static int inferValueTypeFromValues(TypedArray styledAttributes, int valueFromId, int valueToId) {
        TypedValue tvFrom = styledAttributes.peekValue(valueFromId);
        boolean hasTo = true;
        boolean hasFrom = tvFrom != null;
        int fromType = hasFrom ? tvFrom.type : 0;
        TypedValue tvTo = styledAttributes.peekValue(valueToId);
        if (tvTo == null) {
            hasTo = false;
        }
        int toType = hasTo ? tvTo.type : 0;
        if ((hasFrom && isColorType(fromType)) || (hasTo && isColorType(toType))) {
            return 3;
        }
        return 0;
    }

    private static void dumpKeyframes(Object[] keyframes, String header) {
        if (keyframes != null && keyframes.length != 0) {
            Log.d(TAG, header);
            int count = keyframes.length;
            for (int i = 0; i < count; i++) {
                Keyframe keyframe = keyframes[i];
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Keyframe ");
                stringBuilder.append(i);
                stringBuilder.append(": fraction ");
                stringBuilder.append(keyframe.getFraction() < 0.0f ? "null" : Float.valueOf(keyframe.getFraction()));
                stringBuilder.append(", ");
                stringBuilder.append(", value : ");
                stringBuilder.append(keyframe.hasValue() ? keyframe.getValue() : "null");
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private static PropertyValuesHolder loadPvh(Context context, Resources res, Theme theme, XmlPullParser parser, String propertyName, int valueType) throws XmlPullParserException, IOException {
        int next;
        int type;
        Resources resources;
        Theme theme2;
        XmlPullParser xmlPullParser;
        ArrayList<Keyframe> keyframes;
        int type2;
        PropertyValuesHolder value = null;
        ArrayList<Keyframe> keyframes2 = null;
        int valueType2 = valueType;
        while (true) {
            next = parser.next();
            type = next;
            if (next == 3 || type == 1) {
                resources = res;
                theme2 = theme;
                xmlPullParser = parser;
            } else if (parser.getName().equals("keyframe")) {
                if (valueType2 == 4) {
                    resources = res;
                    theme2 = theme;
                    xmlPullParser = parser;
                    valueType2 = inferValueTypeOfKeyframe(resources, theme2, Xml.asAttributeSet(parser), xmlPullParser);
                } else {
                    resources = res;
                    theme2 = theme;
                    xmlPullParser = parser;
                }
                Keyframe keyframe = loadKeyframe(context, resources, theme2, Xml.asAttributeSet(parser), valueType2, xmlPullParser);
                if (keyframe != null) {
                    if (keyframes2 == null) {
                        keyframes2 = new ArrayList();
                    }
                    keyframes2.add(keyframe);
                }
                parser.next();
            } else {
                resources = res;
                theme2 = theme;
                xmlPullParser = parser;
            }
        }
        resources = res;
        theme2 = theme;
        xmlPullParser = parser;
        if (keyframes2 != null) {
            next = keyframes2.size();
            int count = next;
            if (next > 0) {
                next = 0;
                Keyframe firstKeyframe = (Keyframe) keyframes2.get(0);
                Keyframe lastKeyframe = (Keyframe) keyframes2.get(count - 1);
                float endFraction = lastKeyframe.getFraction();
                float f = 0.0f;
                if (endFraction < 1.0f) {
                    if (endFraction < 0.0f) {
                        lastKeyframe.setFraction(1.0f);
                    } else {
                        keyframes2.add(keyframes2.size(), createNewKeyframe(lastKeyframe, 1.0f));
                        count++;
                    }
                }
                float startFraction = firstKeyframe.getFraction();
                if (startFraction != 0.0f) {
                    if (startFraction < 0.0f) {
                        firstKeyframe.setFraction(0.0f);
                    } else {
                        keyframes2.add(0, createNewKeyframe(firstKeyframe, 0.0f));
                        count++;
                    }
                }
                Keyframe[] keyframeArray = new Keyframe[count];
                keyframes2.toArray(keyframeArray);
                while (next < count) {
                    PropertyValuesHolder value2;
                    float f2;
                    Keyframe keyframe2 = keyframeArray[next];
                    if (keyframe2.getFraction() >= f) {
                        value2 = value;
                        keyframes = keyframes2;
                        type2 = type;
                        f2 = f;
                    } else if (next == 0) {
                        keyframe2.setFraction(f);
                        value2 = value;
                        keyframes = keyframes2;
                        type2 = type;
                        f2 = f;
                    } else if (next == count - 1) {
                        keyframe2.setFraction(1.0f);
                        value2 = value;
                        keyframes = keyframes2;
                        type2 = type;
                        f2 = 0.0f;
                    } else {
                        int startIndex = next;
                        int j = startIndex + 1;
                        value2 = value;
                        value = next;
                        while (true) {
                            keyframes = keyframes2;
                            type2 = type;
                            type = j;
                            if (type >= count - 1) {
                                f2 = 0.0f;
                                break;
                            }
                            f2 = 0.0f;
                            if (keyframeArray[type].getFraction() >= 0.0f) {
                                break;
                            }
                            value = type;
                            j = type + 1;
                            keyframes2 = keyframes;
                            type = type2;
                        }
                        distributeKeyframes(keyframeArray, keyframeArray[value + 1].getFraction() - keyframeArray[startIndex - 1].getFraction(), startIndex, value);
                    }
                    next++;
                    f = f2;
                    value = value2;
                    keyframes2 = keyframes;
                    type = type2;
                }
                keyframes = keyframes2;
                type2 = type;
                PropertyValuesHolder value3 = PropertyValuesHolder.ofKeyframe(propertyName, keyframeArray);
                if (valueType2 != 3) {
                    return value3;
                }
                value3.setEvaluator(ArgbEvaluator.getInstance());
                return value3;
            }
        }
        keyframes = keyframes2;
        type2 = type;
        String value4 = propertyName;
        return null;
    }

    private static Keyframe createNewKeyframe(Keyframe sampleKeyframe, float fraction) {
        if (sampleKeyframe.getType() == Float.TYPE) {
            return Keyframe.ofFloat(fraction);
        }
        if (sampleKeyframe.getType() == Integer.TYPE) {
            return Keyframe.ofInt(fraction);
        }
        return Keyframe.ofObject(fraction);
    }

    private static void distributeKeyframes(Keyframe[] keyframes, float gap, int startIndex, int endIndex) {
        float increment = gap / ((float) ((endIndex - startIndex) + 2));
        for (int i = startIndex; i <= endIndex; i++) {
            keyframes[i].setFraction(keyframes[i - 1].getFraction() + increment);
        }
    }

    private static Keyframe loadKeyframe(Context context, Resources res, Theme theme, AttributeSet attrs, int valueType, XmlPullParser parser) throws XmlPullParserException, IOException {
        TypedArray a = TypedArrayUtils.obtainAttributes(res, theme, attrs, AndroidResources.STYLEABLE_KEYFRAME);
        Keyframe keyframe = null;
        float fraction = TypedArrayUtils.getNamedFloat(a, parser, "fraction", 3, -1.0f);
        TypedValue keyframeValue = TypedArrayUtils.peekNamedValue(a, parser, "value", 0);
        boolean hasValue = keyframeValue != null;
        if (valueType == 4) {
            if (hasValue && isColorType(keyframeValue.type)) {
                valueType = 3;
            } else {
                valueType = 0;
            }
        }
        if (hasValue) {
            if (valueType != 3) {
                switch (valueType) {
                    case 0:
                        keyframe = Keyframe.ofFloat(fraction, TypedArrayUtils.getNamedFloat(a, parser, "value", 0, 0.0f));
                        break;
                    case 1:
                        break;
                }
            }
            keyframe = Keyframe.ofInt(fraction, TypedArrayUtils.getNamedInt(a, parser, "value", 0, 0));
        } else {
            Keyframe ofFloat;
            if (valueType == 0) {
                ofFloat = Keyframe.ofFloat(fraction);
            } else {
                ofFloat = Keyframe.ofInt(fraction);
            }
            keyframe = ofFloat;
        }
        int resID = TypedArrayUtils.getNamedResourceId(a, parser, "interpolator", 1, 0);
        if (resID > 0) {
            keyframe.setInterpolator(AnimationUtilsCompat.loadInterpolator(context, resID));
        }
        a.recycle();
        return keyframe;
    }

    private static ObjectAnimator loadObjectAnimator(Context context, Resources res, Theme theme, AttributeSet attrs, float pathErrorScale, XmlPullParser parser) throws NotFoundException {
        ValueAnimator anim = new ObjectAnimator();
        loadAnimator(context, res, theme, attrs, anim, pathErrorScale, parser);
        return anim;
    }

    private static ValueAnimator loadAnimator(Context context, Resources res, Theme theme, AttributeSet attrs, ValueAnimator anim, float pathErrorScale, XmlPullParser parser) throws NotFoundException {
        TypedArray arrayAnimator = TypedArrayUtils.obtainAttributes(res, theme, attrs, AndroidResources.STYLEABLE_ANIMATOR);
        TypedArray arrayObjectAnimator = TypedArrayUtils.obtainAttributes(res, theme, attrs, AndroidResources.STYLEABLE_PROPERTY_ANIMATOR);
        if (anim == null) {
            anim = new ValueAnimator();
        }
        parseAnimatorFromTypeArray(anim, arrayAnimator, arrayObjectAnimator, pathErrorScale, parser);
        int resID = TypedArrayUtils.getNamedResourceId(arrayAnimator, parser, "interpolator", 0, 0);
        if (resID > 0) {
            anim.setInterpolator(AnimationUtilsCompat.loadInterpolator(context, resID));
        }
        arrayAnimator.recycle();
        if (arrayObjectAnimator != null) {
            arrayObjectAnimator.recycle();
        }
        return anim;
    }

    private static boolean isColorType(int type) {
        return type >= 28 && type <= 31;
    }

    private AnimatorInflaterCompat() {
    }
}
