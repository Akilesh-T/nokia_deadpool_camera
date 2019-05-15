package com.morphoinc.app.panoramagp3;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.hmdglobal.app.camera.R;

public class CameraInfoViewFragment extends Fragment {
    private TextView mTextView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_info_view_fragment, container, false);
        this.mTextView = (TextView) view.findViewById(R.id.tv_result_text_view);
        return view;
    }

    public void update(String text) {
        this.mTextView.setText(text);
    }
}
