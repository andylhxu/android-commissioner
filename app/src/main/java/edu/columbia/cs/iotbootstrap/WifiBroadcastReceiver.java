package edu.columbia.cs.iotbootstrap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WifiBroadcastReceiver extends BroadcastReceiver {

    private final String TAG = "WifiBroadcastReceiver";

    private WifiDevActivity activity;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;

    private List<WifiP2pDevice> peers = new ArrayList<>();
    private WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            peers.clear();
            peers.addAll(peerList.getDeviceList());
            StringBuilder sb = new StringBuilder("Number of device found: " + peers.size()+"\n\n");
            for(int i = 0 ; i < peers.size();i++)
            {
                sb.append(peers.get(i).toString());
                sb.append('\n');
            }
            activity.updateStatus(sb.toString());

        }
    };

    public WifiBroadcastReceiver(WifiP2pManager manager,WifiP2pManager.Channel channel,WifiDevActivity act)
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
                activity.setIsWifiP2pEnabled(true);
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
