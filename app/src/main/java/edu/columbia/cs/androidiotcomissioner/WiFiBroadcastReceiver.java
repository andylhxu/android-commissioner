package edu.columbia.cs.androidiotcomissioner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by FreedomSworder on 5/22/16.
 */
public class WiFiBroadcastReceiver extends BroadcastReceiver{

    private final String TAG = "WiFiBroadcastReceiver";
    private MainActivity activity;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            peers.clear();
            peers.addAll(peerList.getDeviceList());
            activity.updateDeviceList(peers);/*
           StringBuilder sb = new StringBuilder("Debug Info: \n\n");
           for(int i = 0 ; i < peers.size();i++)
           {
               sb.append(peers.get(i).toString());
               sb.append("\n");
           }
           activity.updatePeersInfo(sb.toString());
           */
        }
    };
    public WiFiBroadcastReceiver(WifiP2pManager manager,WifiP2pManager.Channel channel,MainActivity act)
    {
        activity = act;
        mChannel = channel;
        mManager = manager;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Determine if Wifi P2P mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // activity.setIsWifiP2pEnabled(true);
                // do nothin
            } else {
                activity.setIsWifiP2pEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // The peer list has changed!  We should probably do something about
            // that.
            if(mManager != null)
                mManager.requestPeers(mChannel,mPeerListListener);
            Log.d(TAG,"Finished fetching devices");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Connection state changed!  We should probably do something about
            // that.
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // do something about the device configuration change.
        }
    }



}
