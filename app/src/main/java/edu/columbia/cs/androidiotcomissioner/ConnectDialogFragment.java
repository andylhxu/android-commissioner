package edu.columbia.cs.androidiotcomissioner;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.net.InetAddress;


// General Alert Dialog
public class ConnectDialogFragment extends DialogFragment {
    public static final String TAG = "ConnectDialogFragment";
    public MainActivity hostingActivity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        hostingActivity = (MainActivity) getActivity();
        if(this.getTag().contains("certificate")){
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View v = (View) inflater.inflate(R.layout.fragment_dialog_certificate,null);
            builder
                    .setTitle("Set Customized CA Certificate")
                    .setView(v)
                    .setNeutralButton("Default", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            hostingActivity.setCertificate("");
                        }
                    })
                    .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText editText = (EditText) v.findViewById(R.id.fragment_dialog_certificate_url);
                            String url = editText.getText().toString();
                            hostingActivity.setCertificate(url);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothin
                        }
                    });
            return builder.create();
        }
        else if(this.getTag().contains("group")){
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
        else if(this.getTag().contains("service")){

            // obtain the service name
            Bundle args = getArguments();
            final String name = args.getString("name");

            final InetAddress address = (InetAddress) args.getSerializable("address");

            final int port = args.getInt("port");
            // authorize device
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Authorize <"+name+">")
                    .setMessage(address.toString().substring(1)+":"+port)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // perform authorization
                            hostingActivity.mPendingService.addLast(name);
                            hostingActivity.authorize(address, port );
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    });
            return builder.create();

        }
        else if(getTag().equals("quit")){
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Closing the Commissioner")
                    .setMessage("Are you sure you want to quit?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().finish();
                        }

                    })
                    .setNegativeButton("No", null)
                    .create();
            return dialog;
        }
        else {
            Bundle args = getArguments();
            final String address = args.getString("address");
            final String name = args.getString("name");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                    .setTitle("Connect to?")
                    .setMessage(name+" ("+address+")")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "Connecting the new device");
                            hostingActivity.connectToP2PDevice(address);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "Cancelling the connection");
                        }
                    });

            return builder.create();
        }
    }


}
