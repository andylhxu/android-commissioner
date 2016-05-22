package edu.columbia.cs.androidiotcomissioner;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class WiFiP2PFragment extends Fragment {

    public static final String TAG = "WiFiP2PFragment";

    // for the Lists
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private SwitchCompat mSwitchCompat;

    public static WiFiP2PFragment newInstance(){
        return new WiFiP2PFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_wifip2p, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.wifip2p_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mSwitchCompat = (SwitchCompat) rootView.findViewById(R.id.fragment_wifip2p_switch);
        mSwitchCompat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSwitchCompat.isChecked())
                {
                    Log.d(TAG, "Toggle On");
                }
                else{
                    Log.d(TAG, "Toggle Off");
                }
            }
        });

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public class WiFiDeviceAdaptor extends RecyclerView.Adapter<WiFiDeviceHolder> {

        private List<WifiP2pDevice> mAdaptorDevices;

        public WiFiDeviceAdaptor (List<WifiP2pDevice> devices)
        {
            mAdaptorDevices = devices;
        }

        @Override
        public WiFiDeviceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(R.layout.list_item_device, parent, false);
            return new WiFiDeviceHolder(v);
        }

        @Override
        public void onBindViewHolder(WiFiDeviceHolder holder, int position) {
            WifiP2pDevice thisDevice = mAdaptorDevices.get(position);
            holder.bindDevice(thisDevice);
        }

        @Override
        public int getItemCount() {
            return mAdaptorDevices.size();
        }

        public void updateDeviceList(List<WifiP2pDevice> devices)
        {
            mAdaptorDevices = devices;
        }
    }

    public class WiFiDeviceHolder extends RecyclerView.ViewHolder{
        TextView mDeviceNameTextView;
        TextView mDeviceMacTextView;
        WifiP2pDevice mDevice;

        public WiFiDeviceHolder(View itemView) {
            super(itemView);
            mDeviceNameTextView = (TextView) itemView.findViewById(R.id.list_item_device_name);
            mDeviceMacTextView = (TextView) itemView.findViewById(R.id.list_item_device_address);
        }

        public void bindDevice(WifiP2pDevice device){
            mDevice = device;
            mDeviceNameTextView.setText(device.deviceName);
            mDeviceMacTextView.setText(device.deviceAddress);
        }

    }
}
