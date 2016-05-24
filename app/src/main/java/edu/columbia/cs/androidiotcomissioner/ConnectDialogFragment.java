package edu.columbia.cs.androidiotcomissioner;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by FreedomSworder on 5/22/16.
 */

// General Alert Dialog
public class ConnectDialogFragment extends DialogFragment {
    public static final String TAG = "ConnectDialogFragment";
    public MainActivity hostingActicity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        hostingActicity = (MainActivity) getActivity();
        if(this.getTag().contains("group")){
            String ans = getArguments().getString("message");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(ans)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothin
                        }
                    });
            return builder.create();
        }
        else if(this.getTag().contains(".")){

            // obtain the service name
            String name = getArguments().getString("servicename");
            hostingActicity.mPendingService.addLast(name);

            int separator = this.getTag().indexOf(":");
            final String addr = this.getTag().substring(0,separator);
            final int port = (new Integer(this.getTag().substring(separator+1))).intValue();
            // authorize device
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Authorize this device with "+addr+":"+port)
                    .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // perform authorization
                            hostingActicity.authorize( addr, port );
                        }
                    })
                    .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    });
            return builder.create();

        }
        else {
            final String address = this.getTag();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                    .setMessage("Connect to this device?")
                    .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "Connecting the new device");
                            hostingActicity.connectToP2PDevice(address);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "Cancelling the connection");
                        }
                    });

            return builder.create();
        }
    }


}
