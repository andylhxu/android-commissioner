package edu.columbia.cs.androidiotcomissioner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.nsd.NsdManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MainActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private static final String TAG = "MainActivity";


    // WiFiP2P related variables
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private NsdManager mNsdManager;
    protected static final int[] CHANNEL_LIST = { 1, 3, 6, 11};
    private List<WifiP2pDevice> mWifiP2pDevices;
    private WiFiBroadcastReceiver mBroadcastReceiver;
    protected static final int OWNER_INTENT_HIGH = 15;


    // Server and Zeroconf related stuff
    private ClientHandler runningHandler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);



        // WiFiP2P related setup
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


        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        if (mNsdManager == null){
            Log.e(TAG,"Failed to obtain the NsdManager");
        }
        final int channelNum = CHANNEL_LIST[new Random().nextInt(CHANNEL_LIST.length)];
        try {
            Method setWifiP2pChannels = mManager.getClass().getMethod("setWifiP2pChannels", WifiP2pManager.Channel.class, int.class, int.class, WifiP2pManager.ActionListener.class);
            setWifiP2pChannels.invoke(mManager, mChannel, 0, channelNum, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                    Log.d(TAG, "Changed channel (" + channelNum + ") succeeded");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Changed channel (" + channelNum + ")  failed");
                }
            });
        } catch (Exception ex)
        {
            Log.e(TAG,ex.toString());
        }
        mWifiP2pDevices = new ArrayList<>();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBroadcastReceiver = new WiFiBroadcastReceiver(mManager,mChannel,this);
        registerReceiver(mBroadcastReceiver,intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private WiFiP2PFragment hostingWiFiP2PFragment;
        private ZeroConfFragment hostingZeroConfFragment;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            hostingWiFiP2PFragment = null;
            hostingZeroConfFragment = null;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position){
                case 0:
                    if(hostingWiFiP2PFragment == null)
                        hostingWiFiP2PFragment = WiFiP2PFragment.newInstance();
                    return hostingWiFiP2PFragment;
                case 1:
                    if(hostingZeroConfFragment == null)
                        hostingZeroConfFragment = ZeroConfFragment.newInstance();
                    return hostingZeroConfFragment;
            }
            Log.e(TAG, "getItem() error");
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SCAN";
                case 1:
                    return "AUTHENTICATE";
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }
    }



    // additional callbacks

    public void setDiscoveryOn()
    {
        // create a wifi p2p group
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"Wifip2pgroup successfully created");
            }

            @Override
            public void onFailure(int reason) {

                Toast.makeText(getApplicationContext(),R.string.create_group_fail,Toast.LENGTH_SHORT).show();
                if(reason == WifiP2pManager.BUSY)
                    Log.e(TAG,"Wifip2pgroup failed to create: BUSY");
                else if (reason == WifiP2pManager.ERROR)
                    Log.e(TAG,"Wifip2pgroup failed to create: ERROR");
                else
                    Log.e(TAG,"Wifip2pgroup failed to create: P2P Unsupported");
                finish();
            }
        });
        try {
            Thread.sleep(200, 0);
        }
        catch(Exception ex){
            Log.e(TAG,ex.toString());
        }
        // discover peers
        mManager.discoverPeers(mChannel,new WifiP2pManager.ActionListener(){
            @Override
            public void onSuccess() {
                Log.d(TAG,"WiFiBroadcastReceiver handled peer discovery request");
            }
            @Override
            public void onFailure(int reason) {
                Log.e(TAG,"Failed to initiate the peer discovery process.");
                Toast.makeText(MainActivity.this, "Failed to initiate peer discovery", Toast.LENGTH_SHORT).show();
                if(reason == WifiP2pManager.P2P_UNSUPPORTED)
                    Log.e(TAG,"Unsupported P2P");
                else if(reason == WifiP2pManager.BUSY)
                    Log.e(TAG,"Busy");
                else
                    Log.e(TAG,"Internal Error");
            }
        });
    }

    public void setDiscoveryOff()
    {
        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully stopped peer discovery");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failed to stop peer discovery");
            }
        });
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"Successfully destroyed the group");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG,"Failed to destroy the P2P group upon exit!");

            }
        });
        updateDeviceList(new ArrayList<WifiP2pDevice>());
    }

    public void setServerOn(){
        // start the server
        startServer();
    }
    public void setServerOff(){
        if (runningHandler != null) {
            runningHandler.cancel(true);
            Toast.makeText(this, "Stopping the auth server", Toast.LENGTH_SHORT).show();
            mSectionsPagerAdapter.hostingZeroConfFragment.setTextView("Off");
        }
    }

    private void startServer(){
        try {
            // obtain key information
            Resources resources = getResources();
            InputStream caInput = resources.openRawResource(R.raw.cacert);
            InputStream keyStoreInput = resources.openRawResource(R.raw.keystore);


            SSLContext sslContext; // the sslContext of our keystore
            KeyManagerFactory keyManagerFactory; // the Factory that creates a KeyManager
            KeyStore keyStore; // used to store our certificate-private key pair

            char[] storePass = "123456".toCharArray();
            char[] keyPass = "123456".toCharArray();

            sslContext = SSLContext.getInstance("TLS");

            Log.d(TAG,"Default algorithm:"+KeyManagerFactory.getDefaultAlgorithm()+" Default type:"+KeyStore.getDefaultType());
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); //Sun format of the certificate
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType()); // BKS store type
            keyStore.load(keyStoreInput,storePass);
            keyManagerFactory.init(keyStore,keyPass);


            // Import the organizational CA
            Log.d(TAG,"creating the factory");
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509"); // standard X.509 type cert
            Certificate certificate = certificateFactory.generateCertificate(caInput); // must be DER or PKCS #7
            Log.d(TAG,"adding cert to keystore");
            KeyStore keyStoreCA = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStoreCA.load(null, null); // empty keystore
            Log.d(TAG,"setting the certificate entry");
            keyStoreCA.setCertificateEntry("IRTca",certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStoreCA); // trust the CA keystore

            sslContext.init(keyManagerFactory.getKeyManagers(),trustManagerFactory.getTrustManagers(),null);
            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

            Random random = new Random(Calendar.getInstance().getTimeInMillis());
            int port = 8000 + random.nextInt(1000);
            mSectionsPagerAdapter.hostingZeroConfFragment.setTextView("On: "+port);

            SSLServerSocket currentServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);

            Toast.makeText(this,"Running on port:"+ port,Toast.LENGTH_SHORT).show();


            StringBuilder usefulInfo = new StringBuilder();
            usefulInfo.append("1:");
            for (String str: currentServerSocket.getSupportedCipherSuites()){
                usefulInfo.append(str);
            }
            usefulInfo.append("=====\n2:");
            for (String str: currentServerSocket.getSupportedProtocols()){
                usefulInfo.append(str);
            }
            currentServerSocket.setEnabledCipherSuites(currentServerSocket.getSupportedCipherSuites());
            currentServerSocket.setEnabledProtocols(currentServerSocket.getSupportedProtocols());
            currentServerSocket.setEnableSessionCreation(true);
            currentServerSocket.setNeedClientAuth(true);
            currentServerSocket.setWantClientAuth(true);
            currentServerSocket.setReuseAddress(true);
            Log.d(TAG,"Information"+usefulInfo.toString());

            runningHandler = new ClientHandler();
            runningHandler.execute((SSLServerSocket)currentServerSocket);

        } catch (Exception ex)
        {
            Log.e(TAG, ex.toString());
        }
    }


    private class ClientHandler extends AsyncTask<SSLServerSocket,String,Integer> {
        SSLServerSocket mServerSocket = null;
        // input Socket, status post and final result

        protected Integer doInBackground(SSLServerSocket... currentServerSocket) {
            mServerSocket = currentServerSocket[0];
            Log.d(TAG,"Client Handler Running");
            try {
                mServerSocket.setSoTimeout(500);
            }
            catch (Exception ex){
                Log.d(TAG,"failed to set the timeout");
            }
            Log.d(TAG,"Waiting for a new client");
            while(!isCancelled()) {
                try {
                    Socket c = mServerSocket.accept();
                    Log.d(TAG, "Accepted a new client");
                    OutputStream out = c.getOutputStream();
                    InputStream in = c.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    DataOutputStream dataOutputStream =new DataOutputStream(out);
                    dataOutputStream.writeBytes("network={\n" +
                            "  scan_ssid=1\n" +
                            "  ssid=\"SECE\"\n" +
                            "  key_mgmt=WPA-PSK\n" +
                            "  proto=WPA2\n" +
                            "  psk=\"irtlab7lw2\"\n" +
                            "}");
                    while(!isCancelled())
                    {
                        String str = reader.readLine();
                        if (str == null || str.length() == 0)
                            break;
                        publishProgress("Received:"+str);
                        dataOutputStream.writeBytes(str.toUpperCase()+"\n");
                        if(str.length() >= 4 && str.substring(0,4).equals("exit"))
                            break;
                    }
                    reader.close();
                    dataOutputStream.close();
                    in.close();
                    out.close();
                    c.close();
                    Log.d(TAG,"Closed a client");

                } catch (SocketTimeoutException ste)
                {
                    //
                } catch (IOException io) {
                    Log.e(TAG, io.toString());
                }
            }
            return 0;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Toast.makeText(getApplicationContext(),"Get update:"+values[0],Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            if(integer == 0)
                Toast.makeText(getApplicationContext(),"Finished handling",Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(),"Some error occurred",Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onCancelled() {

            Log.d(TAG,"Thread canceling");
            if(mServerSocket != null){
                try {
                    mServerSocket.close();
                    Log.d(TAG,"Socket closed");
                } catch (IOException io){
                    Log.e(TAG,"failed to close the server socket"+io.toString());
                }
            }
        }

    }

    public void updateDeviceList(List<WifiP2pDevice> list){
        mWifiP2pDevices.clear();
        mWifiP2pDevices.addAll(list);
        Log.d(TAG, "Update List gets called: "+mWifiP2pDevices.size()+"added");
        WiFiP2PFragment.WiFiDeviceAdaptor adaptor = mSectionsPagerAdapter.hostingWiFiP2PFragment.getDeviceAdaptor();
        if(adaptor != null)
            adaptor.notifyDataSetChanged();
    }

    public List<WifiP2pDevice> getWifiP2pDevices()
    {
        return mWifiP2pDevices;
    }


    // additional methods
    public void setIsWifiP2pEnabled(boolean flag)
    {
        WiFiP2PFragment f = mSectionsPagerAdapter.hostingWiFiP2PFragment;
        if(f != null)
            f.toggleSwitch(flag);
    }

    public void connectToP2PDevice(final String address){
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = address;
        config.groupOwnerIntent = OWNER_INTENT_HIGH;
        WpsInfo wpsInfo = new WpsInfo();
        wpsInfo.setup = WpsInfo.PBC;
        config.wps = wpsInfo;
        Log.d(TAG,"Attempted to connect to"+config.deviceAddress);
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"Invitation sent",Toast.LENGTH_SHORT).show();
                // register this connection!!!
            }

            @Override
            public void onFailure(int reason) {

                if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                    Log.e(TAG, "Unsupported P2P");

                } else if (reason == WifiP2pManager.BUSY) {
                    Log.e(TAG, "Busy");

                } else {
                    Log.e(TAG, "Internal Error");
                }
            }
        });
    }
}
