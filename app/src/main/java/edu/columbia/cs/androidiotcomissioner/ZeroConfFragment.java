package edu.columbia.cs.androidiotcomissioner;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;



public class ZeroConfFragment extends Fragment {

    private MainActivity callingActivity;

    private SwitchCompat mServerSwitch;
    private TextView mTextView;

    public static ZeroConfFragment newInstance(){
        return new ZeroConfFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callingActivity = (MainActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_zeroconf, container, false);

        mServerSwitch = (SwitchCompat) rootView.findViewById(R.id.fragment_zeroconf_switch);
        mTextView = (TextView) rootView.findViewById(R.id.fragment_zeroconf_text);

        mServerSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mServerSwitch.isChecked()){
                    callingActivity.setServerOn();
                }
                else
                    callingActivity.setServerOff();
            }

        });
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }



    // additional functions

    public void setTextView(String content){
        mTextView.setText(content);
    }
}
