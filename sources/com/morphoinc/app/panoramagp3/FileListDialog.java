package com.morphoinc.app.panoramagp3;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import com.hmdglobal.app.camera.R;
import com.morphoinc.app.LogFilter;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

public class FileListDialog extends DialogFragment implements OnClickListener {
    private static final String TAG = " FileListDialog";
    private boolean mAddList = false;
    private File[] mDialogFileList;
    private onFileListDialogListener mListener = null;
    private String mPath = null;

    public interface onFileListDialogListener {
        void onClickCancelButtonFileListDialog();

        void onClickOkButtonFileListDialog(String str);
    }

    private void setPath(String path) {
        this.mPath = path;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPath : ");
        stringBuilder.append(this.mPath);
        LogFilter.d(str, stringBuilder.toString());
    }

    public void setOnFileListDialogListener(onFileListDialogListener listener) {
        this.mListener = listener;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogFilter.d(TAG, "onCreateDialog");
        AlertDialog dialog = null;
        try {
            FileFilter filter = new FileFilter() {
                public boolean accept(File pathname) {
                    if (pathname.isDirectory()) {
                        return true;
                    }
                    String path = pathname.getName();
                    int index = path.lastIndexOf(".");
                    if (index < 0) {
                        return false;
                    }
                    return ".yuv".equalsIgnoreCase(path.substring(index));
                }
            };
            this.mDialogFileList = new File(this.mPath).listFiles(filter);
            File[] mDialogFileList2 = new File(this.mPath).listFiles(filter);
            if (this.mDialogFileList != null) {
                if (mDialogFileList2 != null) {
                    int i;
                    Arrays.sort(mDialogFileList2);
                    int list_size = mDialogFileList2.length;
                    for (i = 1; i < list_size + 1; i++) {
                        this.mDialogFileList[i - 1] = mDialogFileList2[list_size - i];
                    }
                    int i2 = 0;
                    if (this.mPath.lastIndexOf("/") != 0) {
                        list_size++;
                        this.mAddList = true;
                    } else {
                        this.mAddList = false;
                    }
                    String[] list = new String[list_size];
                    i = 0;
                    if (this.mAddList) {
                        list[0] = "..";
                        i = 0 + 1;
                    }
                    File[] fileArr = this.mDialogFileList;
                    int length = fileArr.length;
                    while (i2 < length) {
                        String name;
                        File file = fileArr[i2];
                        if (file.isDirectory()) {
                            name = new StringBuilder();
                            name.append(file.getName());
                            name.append("/");
                            name = name.toString();
                        } else {
                            name = file.getName();
                        }
                        list[i] = name;
                        i++;
                        i2++;
                    }
                    Builder builder = new Builder(getActivity());
                    builder.setPositiveButton(ButtonsFragment.ACTION_START, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (FileListDialog.this.mListener != null) {
                                FileListDialog.this.mListener.onClickOkButtonFileListDialog(FileListDialog.this.mPath);
                            }
                        }
                    });
                    builder.setNegativeButton("cancel", new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (FileListDialog.this.mListener != null) {
                                FileListDialog.this.mListener.onClickCancelButtonFileListDialog();
                            }
                        }
                    });
                    builder.setTitle(this.mPath).setItems(list, this);
                    dialog = builder.create();
                    dialog.getButton(-1).setTextColor(getActivity().getResources().getColor(R.color.dialog_button_font_color));
                    dialog.getButton(-2).setTextColor(getActivity().getResources().getColor(R.color.dialog_button_font_color));
                    return dialog;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onClick : ");
        stringBuilder.append(which);
        LogFilter.d(str, stringBuilder.toString());
        if (this.mDialogFileList == null) {
            return;
        }
        if (which == 0 && this.mAddList) {
            int cnt = this.mPath.lastIndexOf("/");
            if (cnt != 0) {
                setPath(this.mPath.substring(0, cnt));
            }
            dismiss();
            show(getFragmentManager(), "");
            return;
        }
        if (this.mAddList) {
            which--;
        }
        File file = this.mDialogFileList[which];
        dismiss();
        if (file.isDirectory()) {
            setPath(file.getAbsolutePath());
        }
        show(getFragmentManager(), "");
    }
}
