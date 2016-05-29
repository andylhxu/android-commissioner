package edu.columbia.cs.androidiotcomissioner;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSocket;


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
                    .setTitle("Download CA Certificate")
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
            builder
                    .setMessage("Authenticate <"+name+"> ("+address.toString().substring(1)+":"+port+")")
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
        else if (getTag().equals("client")){
            final X509Certificate cacert = (X509Certificate) getArguments().getSerializable("ca");
            final X509Certificate clientcert = (X509Certificate) getArguments().getSerializable("public");
            final SSLSocket c = ((MainActivity) getActivity()).mCurrentClient;

            String msg =
                    "Common Name: "+clientcert.getSubjectX500Principal().getName()+"\n\n"+
                    "Issuer Name: "+clientcert.getIssuerX500Principal().getName()+"\n\n"+
                    "Certificate Serial: "+clientcert.getSerialNumber().toString();

            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle("Authorize?")
                    .setMessage(msg)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        // preparing the file
                                        InputStream ins = getResources().openRawResource(R.raw.networkconfig);
                                        ByteArrayOutputStream ous = new ByteArrayOutputStream();
                                        int size = 0;
                                        byte[] buffer = new byte[1024];
                                        try {
                                            while ((size = ins.read(buffer, 0, 1024)) >= 0) {
                                                ous.write(buffer, 0, size);
                                            }
                                            ins.close();
                                        }
                                        catch (IOException ioe){
                                            Log.e(TAG, ioe.toString());
                                        }
                                        byte [] bufferAll = ous.toByteArray();
                                        OutputStream out = c.getOutputStream();
                                        InputStream in = c.getInputStream();
                                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                                        DataOutputStream dataOutputStream = new DataOutputStream(out);
                                        dataOutputStream.writeInt(bufferAll.length);
                                        dataOutputStream.write(bufferAll);
                                        reader.close();
                                        dataOutputStream.close();
                                        in.close();
                                        out.close();
                                        c.close();
                                        Log.d(TAG, "Closed a client, sent: " + bufferAll.length + " bytes");
                                        if (!hostingActivity.mPendingService.isEmpty())
                                            hostingActivity.mEnrolledService.addLast(hostingActivity.mPendingService.pollFirst());
                                    }
                                    catch (Exception ex){
                                        Log.e(TAG, ex.toString());
                                    }
                                }
                            };
                            new Thread(r).start();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        c.close();
                                    } catch (Exception ex){
                                        Log.e(TAG,ex.toString());
                                    }
                                }
                            };
                            new Thread(r).start();
                            // remove from the pending list
                            if(!hostingActivity.mPendingService.isEmpty())
                                hostingActivity.mPendingService.removeFirst();
                        }
                    })
                    .create();
            return dialog;

        }
        else if(getTag().equals("certdetail")){
            X509Certificate ca = (X509Certificate) getArguments().getSerializable("ca");
            X509Certificate client = (X509Certificate) getArguments().getSerializable("ca");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            AlertDialog dialog = builder
                    .setTitle("Certificate Details")
                    .setMessage(ca.toString()+"\n"+client.toString())
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothin
                        }
                    })
                    .create();
            return dialog;
        }
        else if(getTag().equals("importca")){

            final X509Certificate ca = (X509Certificate) getArguments().getSerializable("ca");

            String msg =
                    "Issuer Name: "+ca.getIssuerX500Principal().getName()+"\n\n"+
                    "Certificate Serial: "+ca.getSerialNumber().toString();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Import this CA")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            hostingActivity.caInputCert = ca;
                            Log.d(TAG, "Loaded CA");
                            Toast.makeText(getActivity().getApplicationContext(), "Loaded CA", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothin
                            hostingActivity.caInputCert = null;
                        }
                    })
                    .setMessage(msg);
            return builder.create();

        }
        else {
            Bundle args = getArguments();
            final String address = args.getString("address");
            final String name = args.getString("name");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                    .setMessage("Connect to "+"<"+name+"> ("+address+")")
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
