package edu.columbia.cs.androidiotcomissioner;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

/**
 * Created by FreedomSworder on 5/22/16.
 */

// General Alert Dialog
public class ConnectDialogFragment extends DialogFragment {
    public static final String TAG = "ConnectDialogFragment";
    public MainActivity hostingActicity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String address = this.getTag();
        hostingActicity = (MainActivity) getActivity();
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
                        Log.d(TAG, "Canceling the connection");
                    }
                });

        return builder.create();
    }
}
