package com.hmdglobal.app.camera.ui;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.data.MediaDetails;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

public class DetailsDialog {

    private static class DetailsAdapter extends BaseAdapter {
        private final Context mContext;
        private final DecimalFormat mDecimalFormat = new DecimalFormat(".####");
        private final Locale mDefaultLocale = Locale.getDefault();
        private int mHeightIndex = -1;
        private final ArrayList<String> mItems;
        private final MediaDetails mMediaDetails;
        private int mWidthIndex = -1;

        public DetailsAdapter(Context context, MediaDetails details) {
            this.mContext = context;
            this.mMediaDetails = details;
            this.mItems = new ArrayList(details.size());
            setDetails(context, details);
        }

        private void setDetails(android.content.Context r25, com.hmdglobal.app.camera.data.MediaDetails r26) {
            /*
            r24 = this;
            r0 = r24;
            r1 = r25;
            r2 = r26;
            r3 = 1;
            r4 = 0;
            r5 = r26.iterator();
        L_0x000c:
            r6 = r5.hasNext();
            if (r6 == 0) goto L_0x0229;
        L_0x0012:
            r6 = r5.next();
            r6 = (java.util.Map.Entry) r6;
            r7 = r6.getKey();
            r7 = (java.lang.Integer) r7;
            r7 = r7.intValue();
            r8 = 2131690400; // 0x7f0f03a0 float:1.9009843E38 double:1.053194994E-314;
            r9 = 2;
            r10 = 0;
            r11 = 1;
            switch(r7) {
                case 5: goto L_0x01af;
                case 6: goto L_0x0186;
                case 7: goto L_0x017b;
                case 10: goto L_0x016a;
                case 102: goto L_0x014a;
                case 103: goto L_0x0134;
                case 104: goto L_0x0114;
                case 107: goto L_0x0083;
                case 108: goto L_0x0070;
                case 200: goto L_0x004e;
                default: goto L_0x002b;
            };
        L_0x002b:
            r21 = r5;
            r5 = r6.getValue();
            if (r5 != 0) goto L_0x01d8;
        L_0x0033:
            r7 = "%s's value is Null";
            r8 = 1;
            r9 = new java.lang.Object[r8];
            r8 = r6.getKey();
            r8 = (java.lang.Integer) r8;
            r8 = r8.intValue();
            r8 = com.hmdglobal.app.camera.ui.DetailsDialog.getDetailsName(r1, r8);
            r10 = 0;
            r9[r10] = r8;
            com.hmdglobal.app.camera.ui.DetailsDialog.fail(r7, r9);
            goto L_0x01d8;
        L_0x004e:
            r7 = new java.lang.StringBuilder;
            r7.<init>();
            r8 = "\n";
            r7.append(r8);
            r8 = r6.getValue();
            r8 = r8.toString();
            r7.append(r8);
            r7 = r7.toString();
            r8 = r6.getValue();
            r4 = r8.toString();
            goto L_0x007f;
        L_0x0070:
            r7 = r6.getValue();
            r7 = (java.lang.String) r7;
            r7 = java.lang.Integer.parseInt(r7);
            r7 = r0.toLocalNumber(r7);
        L_0x007f:
            r21 = r5;
            goto L_0x01dc;
        L_0x0083:
            r7 = r6.getValue();
            r7 = (java.lang.String) r7;
            r8 = java.lang.Double.valueOf(r7);
            r12 = r8.doubleValue();
            r14 = 4607182418800017408; // 0x3ff0000000000000 float:0.0 double:1.0;
            r8 = (r12 > r14 ? 1 : (r12 == r14 ? 0 : -1));
            r16 = 4602678819172646912; // 0x3fe0000000000000 float:0.0 double:0.5;
            if (r8 >= 0) goto L_0x00b7;
        L_0x0099:
            r8 = r0.mDefaultLocale;
            r14 = "%d/%d";
            r15 = new java.lang.Object[r9];
            r20 = java.lang.Integer.valueOf(r11);
            r15[r10] = r20;
            r18 = 4607182418800017408; // 0x3ff0000000000000 float:0.0 double:1.0;
            r18 = r18 / r12;
            r9 = r16 + r18;
            r9 = (int) r9;
            r9 = java.lang.Integer.valueOf(r9);
            r15[r11] = r9;
            r7 = java.lang.String.format(r8, r14, r15);
            goto L_0x007f;
        L_0x00b7:
            r8 = (int) r12;
            r9 = (double) r8;
            r12 = r12 - r9;
            r9 = new java.lang.StringBuilder;
            r9.<init>();
            r10 = java.lang.String.valueOf(r8);
            r9.append(r10);
            r10 = "''";
            r9.append(r10);
            r7 = r9.toString();
            r9 = 4547007122018943789; // 0x3f1a36e2eb1c432d float:-1.8890966E26 double:1.0E-4;
            r9 = (r12 > r9 ? 1 : (r12 == r9 ? 0 : -1));
            if (r9 <= 0) goto L_0x010e;
        L_0x00d8:
            r9 = new java.lang.StringBuilder;
            r9.<init>();
            r9.append(r7);
            r10 = r0.mDefaultLocale;
            r14 = " %d/%d";
            r21 = r5;
            r15 = 2;
            r5 = new java.lang.Object[r15];
            r15 = java.lang.Integer.valueOf(r11);
            r20 = 0;
            r5[r20] = r15;
            r18 = 4607182418800017408; // 0x3ff0000000000000 float:0.0 double:1.0;
            r18 = r18 / r12;
            r22 = r12;
            r11 = r16 + r18;
            r11 = (int) r11;
            r11 = java.lang.Integer.valueOf(r11);
            r12 = 1;
            r5[r12] = r11;
            r5 = java.lang.String.format(r10, r14, r5);
            r9.append(r5);
            r5 = r9.toString();
            r7 = r5;
            goto L_0x0112;
        L_0x010e:
            r21 = r5;
            r22 = r12;
        L_0x0112:
            goto L_0x01dc;
        L_0x0114:
            r21 = r5;
            r5 = "1";
            r7 = r6.getValue();
            r5 = r5.equals(r7);
            if (r5 == 0) goto L_0x012a;
        L_0x0122:
            r5 = 2131689804; // 0x7f0f014c float:1.9008634E38 double:1.0531946997E-314;
            r5 = r1.getString(r5);
            goto L_0x0131;
        L_0x012a:
            r5 = 2131689596; // 0x7f0f007c float:1.9008212E38 double:1.053194597E-314;
            r5 = r1.getString(r5);
        L_0x0131:
            r7 = r5;
            goto L_0x01dc;
        L_0x0134:
            r21 = r5;
            r5 = r6.getValue();
            r5 = r5.toString();
            r7 = java.lang.Double.parseDouble(r5);
            r5 = r0.toLocalNumber(r7);
            r7 = r5;
            goto L_0x01dc;
        L_0x014a:
            r21 = r5;
            r5 = r6.getValue();
            r5 = (com.hmdglobal.app.camera.data.MediaDetails.FlashState) r5;
            r7 = r5.isFlashFired();
            if (r7 == 0) goto L_0x0161;
        L_0x0158:
            r7 = 2131689733; // 0x7f0f0105 float:1.900849E38 double:1.0531946647E-314;
            r7 = r1.getString(r7);
            goto L_0x01dc;
        L_0x0161:
            r7 = 2131689731; // 0x7f0f0103 float:1.9008486E38 double:1.0531946637E-314;
            r7 = r1.getString(r7);
            goto L_0x01dc;
        L_0x016a:
            r21 = r5;
            r5 = r6.getValue();
            r5 = (java.lang.Long) r5;
            r7 = r5.longValue();
            r7 = android.text.format.Formatter.formatFileSize(r1, r7);
            goto L_0x01dc;
        L_0x017b:
            r21 = r5;
            r5 = r6.getValue();
            r7 = r0.toLocalInteger(r5);
            goto L_0x01dc;
        L_0x0186:
            r21 = r5;
            r5 = r0.mItems;
            r5 = r5.size();
            r0.mHeightIndex = r5;
            r5 = r6.getValue();
            r5 = r5.toString();
            r7 = "0";
            r5 = r5.equalsIgnoreCase(r7);
            if (r5 == 0) goto L_0x01a6;
        L_0x01a0:
            r7 = r1.getString(r8);
            r3 = 0;
            goto L_0x01dc;
        L_0x01a6:
            r5 = r6.getValue();
            r7 = r0.toLocalInteger(r5);
            goto L_0x01dc;
        L_0x01af:
            r21 = r5;
            r5 = r0.mItems;
            r5 = r5.size();
            r0.mWidthIndex = r5;
            r5 = r6.getValue();
            r5 = r5.toString();
            r7 = "0";
            r5 = r5.equalsIgnoreCase(r7);
            if (r5 == 0) goto L_0x01cf;
        L_0x01c9:
            r7 = r1.getString(r8);
            r3 = 0;
            goto L_0x01dc;
        L_0x01cf:
            r5 = r6.getValue();
            r7 = r0.toLocalInteger(r5);
            goto L_0x01dc;
        L_0x01d8:
            r7 = r5.toString();
        L_0x01dc:
            r5 = r7;
            r7 = r6.getKey();
            r7 = (java.lang.Integer) r7;
            r7 = r7.intValue();
            r8 = r2.hasUnit(r7);
            if (r8 == 0) goto L_0x020c;
        L_0x01ed:
            r8 = "%s: %s %s";
            r9 = 3;
            r9 = new java.lang.Object[r9];
            r10 = com.hmdglobal.app.camera.ui.DetailsDialog.getDetailsName(r1, r7);
            r11 = 0;
            r9[r11] = r10;
            r10 = 1;
            r9[r10] = r5;
            r10 = r2.getUnit(r7);
            r10 = r1.getString(r10);
            r11 = 2;
            r9[r11] = r10;
            r5 = java.lang.String.format(r8, r9);
            goto L_0x021f;
        L_0x020c:
            r11 = 2;
            r8 = "%s: %s";
            r9 = new java.lang.Object[r11];
            r10 = com.hmdglobal.app.camera.ui.DetailsDialog.getDetailsName(r1, r7);
            r11 = 0;
            r9[r11] = r10;
            r10 = 1;
            r9[r10] = r5;
            r5 = java.lang.String.format(r8, r9);
        L_0x021f:
            r8 = r0.mItems;
            r8.add(r5);
            r5 = r21;
            goto L_0x000c;
        L_0x0229:
            if (r3 != 0) goto L_0x022e;
        L_0x022b:
            r0.resolveResolution(r4);
        L_0x022e:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.DetailsDialog$DetailsAdapter.setDetails(android.content.Context, com.hmdglobal.app.camera.data.MediaDetails):void");
        }

        public void resolveResolution(String path) {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap != null) {
                onResolutionAvailable(bitmap.getWidth(), bitmap.getHeight());
            }
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            return false;
        }

        public int getCount() {
            return this.mItems.size();
        }

        public Object getItem(int position) {
            return this.mMediaDetails.getDetail(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView == null) {
                tv = (TextView) LayoutInflater.from(this.mContext).inflate(R.layout.details, parent, false);
            } else {
                tv = (TextView) convertView;
            }
            tv.setText((CharSequence) this.mItems.get(position));
            return tv;
        }

        public void onResolutionAvailable(int width, int height) {
            if (width != 0 && height != 0) {
                String widthString = String.format(this.mDefaultLocale, "%s: %d", new Object[]{DetailsDialog.getDetailsName(this.mContext, 5), Integer.valueOf(width)});
                String heightString = String.format(this.mDefaultLocale, "%s: %d", new Object[]{DetailsDialog.getDetailsName(this.mContext, 6), Integer.valueOf(height)});
                this.mItems.set(this.mWidthIndex, String.valueOf(widthString));
                this.mItems.set(this.mHeightIndex, String.valueOf(heightString));
                notifyDataSetChanged();
            }
        }

        private String toLocalInteger(Object valueObj) {
            if (valueObj instanceof Integer) {
                return toLocalNumber(((Integer) valueObj).intValue());
            }
            String value = valueObj.toString();
            try {
                value = toLocalNumber(Integer.parseInt(value));
            } catch (NumberFormatException e) {
            }
            return value;
        }

        private String toLocalNumber(int n) {
            return String.format(this.mDefaultLocale, "%d", new Object[]{Integer.valueOf(n)});
        }

        private String toLocalNumber(double n) {
            return this.mDecimalFormat.format(n);
        }
    }

    public static Dialog create(Context context, MediaDetails mediaDetails) {
        ListView detailsList = (ListView) LayoutInflater.from(context).inflate(R.layout.details_list, null, false);
        detailsList.setAdapter(new DetailsAdapter(context, mediaDetails));
        return new Builder(context).setTitle(R.string.details).setView(detailsList).setPositiveButton(R.string.close, new OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        }).create();
    }

    public static String getDetailsName(Context context, int key) {
        if (key == 200) {
            return context.getString(R.string.path);
        }
        switch (key) {
            case 1:
                return context.getString(R.string.title);
            case 2:
                return context.getString(R.string.description);
            case 3:
                return context.getString(R.string.time);
            case 4:
                return context.getString(R.string.location);
            case 5:
                return context.getString(R.string.width);
            case 6:
                return context.getString(R.string.height);
            case 7:
                return context.getString(R.string.orientation);
            case 8:
                return context.getString(R.string.duration);
            case 9:
                return context.getString(R.string.mimetype);
            case 10:
                return context.getString(R.string.file_size);
            default:
                switch (key) {
                    case 100:
                        return context.getString(R.string.maker);
                    case 101:
                        return context.getString(R.string.model);
                    case 102:
                        return context.getString(R.string.flash);
                    case 103:
                        return context.getString(R.string.focal_length);
                    case 104:
                        return context.getString(R.string.white_balance);
                    case 105:
                        return context.getString(R.string.aperture);
                    default:
                        switch (key) {
                            case 107:
                                return context.getString(R.string.exposure_time);
                            case 108:
                                return context.getString(R.string.iso);
                            default:
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown key");
                                stringBuilder.append(key);
                                return stringBuilder.toString();
                        }
                }
        }
    }

    private static void fail(String message, Object... args) {
        throw new AssertionError(args.length == 0 ? message : String.format(message, args));
    }
}
