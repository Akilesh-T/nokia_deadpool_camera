package com.hmdglobal.app.camera.beauty.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.BitmapFactory;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DialogUtil {
    private Activity activity;

    public DialogUtil(Activity activity) {
        this.activity = activity;
    }

    public void showDialog(String message) {
        new Builder(this.activity).setTitle(message).setNegativeButton("确认", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                DialogUtil.this.activity.finish();
            }
        }).setCancelable(false).create().show();
    }

    public void showImageDialog(String message, String path, boolean isCompare) {
        Builder builder = new Builder(this.activity);
        if (isCompare) {
            builder.setTitle("比对完成");
        } else {
            builder.setTitle("注册成功");
        }
        builder.setIcon(17301514);
        LayoutParams linearLp = new LayoutParams(-1, -1);
        LayoutParams textLp = new LayoutParams(-1, -2);
        LayoutParams imageLp = new LayoutParams(-1, -1);
        LinearLayout linear = new LinearLayout(this.activity);
        linear.setGravity(17);
        linear.setOrientation(1);
        TextView text = new TextView(this.activity);
        text.setText(message);
        text.setTextColor(-1);
        if (isCompare) {
            text.setTextSize(15.0f);
        } else {
            text.setTextSize(35.0f);
        }
        text.setGravity(17);
        linear.addView(text, textLp);
        ImageView image = new ImageView(this.activity);
        image.setImageBitmap(BitmapFactory.decodeFile(path));
        linear.addView(image, imageLp);
        linear.setLayoutParams(linearLp);
        builder.setView(linear);
        builder.setPositiveButton("确认", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void showNameEditText() {
        Builder builder = new Builder(this.activity);
        builder.setTitle("请输入姓名");
        builder.setIcon(17301514);
        LayoutParams tvLp = new LayoutParams(-1, -1);
        final EditText edit = new EditText(this.activity);
        edit.setLayoutParams(tvLp);
        final InputMethodManager imm = (InputMethodManager) edit.getContext().getSystemService("input_method");
        imm.toggleSoftInput(2, 2);
        builder.setView(edit);
        builder.setPositiveButton("确认", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String name = edit.getText().toString();
                if (name == null || name.length() == 0) {
                    ConUtil.showToast(DialogUtil.this.activity, "姓名不能为空！");
                }
                imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
            }
        });
        builder.setNegativeButton("取消", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
            }
        });
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void showEditText(TextView text, int index) {
        Builder builder = new Builder(this.activity);
        builder.setTitle(getTitle(index));
        builder.setIcon(17301514);
        LayoutParams tvLp = new LayoutParams(-1, -1);
        final EditText edit = new EditText(this.activity);
        edit.setLayoutParams(tvLp);
        edit.setText(text.getText().toString());
        edit.setSelection(text.getText().toString().length());
        final InputMethodManager imm = (InputMethodManager) edit.getContext().getSystemService("input_method");
        imm.toggleSoftInput(2, 2);
        builder.setView(edit);
        final EditText editText = edit;
        final int i = index;
        final TextView textView = text;
        final InputMethodManager inputMethodManager = imm;
        builder.setPositiveButton("确认", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String str = editText.getText().toString();
                if (DialogUtil.this.isNum(str)) {
                    ConUtil.showToast(DialogUtil.this.activity, "请输入数字！");
                    return;
                }
                try {
                    String value = DialogUtil.this.getContent(str, i);
                    DialogUtil.setTextSzie(textView, value.length());
                    textView.setText(value);
                } catch (Exception e) {
                    ConUtil.showToast(DialogUtil.this.activity, "请输入数字！");
                }
                inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
        });
        builder.setNegativeButton("取消", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
            }
        });
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public static void setTextSzie(TextView text, int num) {
        if (num < 3) {
            text.setTextSize(20.0f);
        } else if (num < 5) {
            text.setTextSize(18.0f);
        } else if (num < 7) {
            text.setTextSize(16.0f);
        } else if (num < 9) {
            text.setTextSize(14.0f);
        } else if (num >= 9) {
            text.setTextSize(12.0f);
        }
    }

    public String getTitle(int index) {
        String title = "请输入";
        switch (index) {
            case 0:
                return "最小值是33\n最大值是2147483647";
            case 1:
                return "最小值是1\n最大值是2147483647";
            case 2:
                return "最小值是0\n最大值是1          ";
            case 3:
                return "最小值是0\n最大值是1          ";
            case 4:
                return "最小值是0\n最大值是1          ";
            default:
                return title;
        }
    }

    public String getContent(String str, int index) {
        String content = str;
        long faceSize;
        StringBuilder stringBuilder;
        switch (index) {
            case 0:
                faceSize = (long) Float.parseFloat(content);
                if (faceSize < 33) {
                    faceSize = 33;
                } else if (faceSize > 2147483647L) {
                    faceSize = 2147483647L;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append(faceSize);
                stringBuilder.append("");
                return stringBuilder.toString();
            case 1:
                faceSize = (long) Float.parseFloat(content);
                if (faceSize < 1) {
                    faceSize = 1;
                } else if (faceSize > 2147483647L) {
                    faceSize = 2147483647L;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append(faceSize);
                stringBuilder.append("");
                return stringBuilder.toString();
            case 2:
            case 3:
            case 4:
                float vlaue = Float.parseFloat(content);
                if (vlaue < 0.0f) {
                    vlaue = 0.0f;
                } else if (vlaue > 1.0f) {
                    vlaue = 1.0f;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(vlaue);
                stringBuilder2.append("");
                return stringBuilder2.toString();
            default:
                return content;
        }
    }

    public boolean isNum(String str) {
        return str.matches("[a-zA-Z]+");
    }

    public void onDestory() {
        this.activity = null;
    }
}
