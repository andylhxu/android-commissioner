package edu.columbia.cs.iotbootstrap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class WifiDevActivity extends AppCompatActivity {

    private final String TAG =  "WifiDevActivity";
    private boolean isWifiP2pEnabled;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mBroadcastReceiver;


    private TextView mTextView;
    private Button mDiscoveryButton;

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_dev);
        mTextView = (TextView) findViewById(R.id.wifi_dev_text_view);
        if(mTextView != null)
            mTextView.setText("This dev page is used to display wifi info");
        mDiscoveryButton = (Button) findViewById(R.id.wifi_dev_discover_button);

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);


        //register the listener when pressed the discovery button
        mDiscoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.discoverPeers(mChannel,new WifiP2pManager.ActionListener(){
                    @Override
                    public void onSuccess() {
                        Log.d(TAG,"Successfully handled peer discovery request");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.e(TAG,"Failed to initiate the peer discovery process.");
                        if(reason == WifiP2pManager.P2P_UNSUPPORTED)
                            Log.e(TAG,"Unsupported P2P");
                        else if(reason == WifiP2pManager.BUSY)
                            Log.e(TAG,"Busy");
                        else
                            Log.e(TAG,"Internal Error");
                    }
                });
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiver = new WifiBroadcastReceiver(mManager,mChannel,this);
        registerReceiver(mBroadcastReceiver,intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    public void updateStatus(String str){
        mTextView.setText(str);
    }
}
