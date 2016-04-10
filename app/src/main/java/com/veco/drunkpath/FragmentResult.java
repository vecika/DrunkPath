package com.veco.drunkpath;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.veco.drunkpath.utils.Constants;


/**
 * Created by veco on 21.3.2016..
 */

public class FragmentResult extends Fragment {
    TextView tvDistance, tvSpeed, tvTime;
    Button btnShare, btnBack, btnReset;

    ToggleButton btnToggle;
    TextView tvDistanceTraveled;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View  rootView = inflater.inflate(R.layout.fragment_score, container, false);

        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();

        rootView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        getFragmentManager().popBackStackImmediate();
                        btnToggle.setVisibility(View.VISIBLE);
                        tvDistanceTraveled.setVisibility(View.VISIBLE);
                        btnReset.setVisibility(View.VISIBLE);
                        return true;
                    }
                }
                return false;
            }
        });

        Bundle bundle = getArguments();
        String bundleDistance = bundle.getString("distance");
        long bundleTime = bundle.getLong("time");
        String bundleSpeed = bundle.getString("speed");

        tvDistance = (TextView)rootView.findViewById(R.id.tv_distance);
        tvSpeed = (TextView)rootView.findViewById(R.id.tv_speed);
        tvTime = (TextView)rootView.findViewById(R.id.tv_time);

        btnBack = (Button)rootView.findViewById(R.id.btn_back);
        btnShare = (Button)rootView.findViewById(R.id.btn_share);

        btnToggle = (ToggleButton)getActivity().findViewById(R.id.toggleButton);
        tvDistanceTraveled = (TextView)getActivity().findViewById(R.id.textview_distance);
        btnReset = (Button)getActivity().findViewById(R.id.btn_reset);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStackImmediate();

                btnToggle.setVisibility(View.VISIBLE);
                tvDistanceTraveled.setVisibility(View.VISIBLE);
                btnReset.setVisibility(View.VISIBLE);
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), R.string.sending_data, Toast.LENGTH_SHORT).show();
            }
        });

        long second = (bundleTime / 1000) % 60;
        long minute = (bundleTime / (1000 * 60)) % 60;
        long hour = (bundleTime / (1000 * 60 * 60)) % 24;

        String time = String.format(Constants.RESULT_TIME_STAMP, hour, minute, second);

        tvDistance.setText(bundleDistance + " m");
        tvSpeed.setText(bundleSpeed + " km/h");
        tvTime.setText(time);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        btnToggle.setVisibility(View.INVISIBLE);
        tvDistanceTraveled.setVisibility(View.INVISIBLE);
        btnReset.setVisibility(View.INVISIBLE);
    }
}

