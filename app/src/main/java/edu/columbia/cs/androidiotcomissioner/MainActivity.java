package edu.columbia.cs.androidiotcomissioner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
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

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
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
    private static final String SERVICE_TYPE = "_enrolled._udp";
    //private static final String SERVICE_TYPE = "_workstation._tcp";
    int port_final;


    // WiFiP2P related variables
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    public NsdManager mNsdManager;
    // protected static final int[] CHANNEL_LIST = { 1, 3, 6, 11};
    protected static final int[] CHANNEL_LIST = { 11};
    private List<WifiP2pDevice> mWifiP2pDevices;
    private List<String> mConnectedDevices;
    private WiFiBroadcastReceiver mBroadcastReceiver;
    protected static final int OWNER_INTENT_HIGH = -1; // doesn't matter
    private boolean ifGroupCreated = false;


    // Server and Zeroconf related stuff
    private ClientHandler runningHandler;
    public SSLSocket mCurrentClient;

    public List<NsdServiceInfo> mServiceList;
    public Deque<String> mPendingService;
    public Deque<String> mEnrolledService;
    private NsdManager.DiscoveryListener mServiceDiscoveryListener;

    private static final int RESOLVING_INTERVAL = 5000;

    public String cacert_customize_url;
    public Certificate caInputCert;
    private static final String keyStore_pass = "123456";




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
        mServiceList = new ArrayList<>();
        mPendingService = new ArrayDeque<>();
        mEnrolledService = new ArrayDeque<>();


        mWifiP2pDevices = new ArrayList<>();
        mConnectedDevices = new ArrayList<>();
        cacert_customize_url = "";
        caInputCert = null;

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
        if (id == R.id.action_refresh) {

            // rediscover the services
            /*
            if (mServiceDiscoveryListener != null) {
                Log.d(TAG, "stop and restart the service discovery listener");
                mNsdManager.stopServiceDiscovery(mServiceDiscoveryListener);
                mServiceList.clear();
                mNsdManager.discoverServices(SERVICE_TYPE,NsdManager.PROTOCOL_DNS_SD, mServiceDiscoveryListener);
                mSectionsPagerAdapter.hostingZeroConfFragment.getServiceAdaptor().notifyDataSetChanged();

            }
            */

            mSectionsPagerAdapter.hostingWiFiP2PFragment.getDeviceAdaptor().notifyDataSetChanged();
            mSectionsPagerAdapter.hostingZeroConfFragment.getServiceAdaptor().notifyDataSetChanged();
            Log.d(TAG,"Refresh called");
            return true;
        }

        if (id == R.id.action_group_info) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    // Snackbar.make(findViewById(R.id.main_content),group.toString(),Snackbar.LENGTH_LONG).show();
                    if (group == null)
                        return;
                    Bundle args = new Bundle();
                    args.putString("message", group.toString());

                    ConnectDialogFragment f = new ConnectDialogFragment();
                    f.setArguments(args);

                    f.show(getSupportFragmentManager(),"group");

                }
            });
        }
        if (id == R.id.action_certificate) {
            // pop window
            ConnectDialogFragment f = new ConnectDialogFragment();
            f.show(getSupportFragmentManager(), "certificate");
        }
        if (id == R.id.action_reset_wifi){
            WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
            if(manager == null){
                Toast.makeText(getApplicationContext(), "WiFi control permission denied by the system", Toast.LENGTH_SHORT).show();
                return false;
            }


            if(manager.disconnect())
                Log.d(TAG, "Successfully disconnect from current network");

            if(manager.setWifiEnabled(false))
                Log.d(TAG, "Successfully disable wifi");

            if(manager.setWifiEnabled(true))
                Log.d(TAG, "Successfully enabled wifi");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mManager == null || mChannel == null || !ifGroupCreated)
        {
            return;
        }
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"Successfully destroyed the group");
                ifGroupCreated = false;
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG,"Failed to destroy the P2P group upon exit!");

            }
        });

    }

    @Override
    public void onBackPressed() {
        ConnectDialogFragment fragment = new ConnectDialogFragment();
        fragment.show(getSupportFragmentManager(),"quit");
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
        // pick a random channel
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

        // discover peers
        mManager.discoverPeers(mChannel,new WifiP2pManager.ActionListener(){
            @Override
            public void onSuccess() {
                Log.d(TAG,"WiFiBroadcastReceiver handled peer discovery request");
                mSectionsPagerAdapter.hostingWiFiP2PFragment.setMessage("Discovery Mode");
            }
            @Override
            public void onFailure(int reason) {
                Log.e(TAG,"Failed to initiate the peer discovery process.");
                Toast.makeText(MainActivity.this, "Failed to initiate peer discovery, please reset WiFi configuration and disconnect from all APs", Toast.LENGTH_SHORT).show();
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
        if(ifGroupCreated) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Successfully destroyed the group");
                    ifGroupCreated = false;
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Failed to destroy the P2P group upon exit!");

                }
            });
        }
        updateDeviceList(new ArrayList<WifiP2pDevice>());
        mConnectedDevices = new ArrayList<>();
    }

    public void setServerOn(){
        // start the server
        startServer();

        mServiceDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "start service discovery failed");
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {

            }

            @Override
            public void onDiscoveryStarted(String serviceType) {

            }

            @Override
            public void onDiscoveryStopped(String serviceType) {

            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service found:"+serviceInfo.getServiceName());

                boolean ifAdd = true;
                for(NsdServiceInfo info: mServiceList){
                    if(info.getServiceName().equals(serviceInfo.getServiceName())) {
                        ifAdd = false;
                        break;
                    }
                }
                if(ifAdd) {
                    mServiceList.add(serviceInfo);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                /*
                for(NsdServiceInfo info:mServiceList){
                    if(serviceInfo.getServiceName().equals(info.getServiceName())) {
                        mServiceList.remove(info);
                        break;
                    }
                }
                */
            }
        };
        // discover the services
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mServiceDiscoveryListener);

        // for each service discovered, resolve the address
        final Handler handler = new Handler();
        // Define the code block to be executed
        final Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread
                // Repeat this the same runnable code block again another 5 seconds
                for(int i = 0 ; i < mServiceList.size(); i++){
                    final NsdServiceInfo info = mServiceList.get(i);
                    mNsdManager.resolveService(info, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            // Toast.makeText(getApplicationContext(),"Failed to resolve "+serviceInfo.getServiceName(),Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            info.setHost(serviceInfo.getHost());
                            info.setPort(serviceInfo.getPort());
                            Log.d(TAG, "Resolved auto: "+serviceInfo.getHost().toString()+" and "+serviceInfo.getPort());
                        }
                    });

                }
                handler.postDelayed(this, RESOLVING_INTERVAL);
                mSectionsPagerAdapter.hostingZeroConfFragment.getServiceAdaptor().notifyDataSetChanged();
            }
        };
        // Start the initial runnable task by posting through the handler
        handler.postDelayed(runnableCode,RESOLVING_INTERVAL);

    }
    public void setServerOff(){
        if (runningHandler != null) {
            runningHandler.cancel(true);
        }

        mNsdManager.stopServiceDiscovery(mServiceDiscoveryListener);
        // turn zeroconf off

        mServiceList.clear();
        mPendingService.clear();
        mEnrolledService.clear();


        mSectionsPagerAdapter.hostingZeroConfFragment.setTextView("Off");
        mServiceDiscoveryListener = null;
        mSectionsPagerAdapter.hostingZeroConfFragment.getServiceAdaptor().notifyDataSetChanged();
    }

    private void startServer(){
        try {
            // obtain key information
            Resources resources = getResources();
            InputStream caInput;
            InputStream keyStoreInput = resources.openRawResource(R.raw.keystore); // default keystore
            caInput = resources.openRawResource(R.raw.cacert); // default ca



            SSLContext sslContext; // the sslContext of our keystore
            KeyManagerFactory keyManagerFactory; // the Factory that creates a KeyManager
            KeyStore keyStore; // used to store our certificate-private key pair

            char[] storePass = keyStore_pass.toCharArray();
            char[] keyPass = keyStore_pass.toCharArray();

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

            if(caInputCert != null){
                certificate = caInputCert;
                Log.d(TAG, "Using customized CA");
            }

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
            port_final = 8000 + random.nextInt(1000);
            mSectionsPagerAdapter.hostingZeroConfFragment.setTextView("On: "+port_final);

            SSLServerSocket currentServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port_final);



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


    private class ClientHandler extends AsyncTask<SSLServerSocket,Certificate,Integer> {
        SSLServerSocket mServerSocket = null;
        // input Socket, status post and final result

        byte[] bufferAll = null;

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

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
                    SSLSocket c = (SSLSocket) mServerSocket.accept();
                    mCurrentClient = c;
                    Log.d(TAG, "Accepted a new client");
                    // handling certs
                    Certificate client_ca = null;
                    Certificate client_public = null;
                    try {
                    SSLSession session = c.getSession();

                    Certificate[] certs = session.getPeerCertificates();

                        client_ca = certs[0];
                        client_public = certs[1];
                    }
                    catch (Exception ex){
                        Log.e(TAG, ex.getMessage());
                        continue;
                    }
                    if(client_ca == null || client_public == null){
                        Log.e(TAG, "Client certificate empty");
                        continue;
                    }

                    publishProgress(client_ca, client_public);


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
        protected void onProgressUpdate(Certificate... values) {
            super.onProgressUpdate(values);
            ConnectDialogFragment clientCertDialog = new ConnectDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("ca",values[0]);
            args.putSerializable("public",values[1]);
            clientCertDialog.setArguments(args);
            clientCertDialog.show(getSupportFragmentManager(), "client");
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

    public class AuthorizeExecutor implements Runnable{
        InetAddress address;
        int port;
        public AuthorizeExecutor(InetAddress addr, int p) {
            address = addr;
            port = p;
        }

        @Override
        public void run() {
            try {
                DatagramSocket socket = new DatagramSocket();

                String msg = "HelloReq:tcp:192.168.49.1:"+port_final;
                DatagramPacket packet = new DatagramPacket(msg.getBytes(),msg.length(), address, port);

                socket.send(packet);
                Log.d(TAG, "sent --- "+ msg + " to "+address.toString().substring(1)+":"+port);

            }
            catch(Exception ex){
                Log.e(TAG, ex.toString());
            }
        }
    }

    public void authorize(InetAddress address, int port){

        AuthorizeExecutor exec = new AuthorizeExecutor(address,port);
        new Thread(exec).start();
    }

    public void updateDeviceList(List<WifiP2pDevice> list){
        mWifiP2pDevices.clear();
        mWifiP2pDevices.addAll(list);
        Log.d(TAG, "Update List gets called: "+mWifiP2pDevices.size()+"added");
        final WiFiP2PFragment.WiFiDeviceAdaptor adaptor = mSectionsPagerAdapter.hostingWiFiP2PFragment.getDeviceAdaptor();
        if(adaptor != null)
            adaptor.notifyDataSetChanged();

        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if(group == null)
                    return;
                if(group.getClientList().size() != 0){
                    for(WifiP2pDevice device:group.getClientList())
                    {
                        if(mConnectedDevices.contains(device.deviceAddress) == false)
                        {
                            mConnectedDevices.add(device.deviceAddress);
                        }
                    }
                }
            }
        });
    }


    public List<WifiP2pDevice> getWifiP2pDevices()
    {
        return mWifiP2pDevices;
    }

    public List<String> getConnectedP2pDevices(){
        return mConnectedDevices;
    }
    public List<NsdServiceInfo> getZeroConfServices(){
        return mServiceList;
    }


    // additional methods
    public void setIsWifiP2pEnabled(boolean flag)
    {
        WiFiP2PFragment f = mSectionsPagerAdapter.hostingWiFiP2PFragment;
        if(f != null) {
            f.toggleSwitch(flag);
        }
    }

    public void connectToP2PDevice(final String address){


        // create a wifi p2p group if a group is not formed
        if(!ifGroupCreated) {
            mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    ifGroupCreated = true;
                    Log.d(TAG, "Wifip2pgroup successfully created");


                    // connect to the device
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ie){
                        Log.e(TAG,ie.getMessage());
                    }

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
                            mSectionsPagerAdapter.hostingWiFiP2PFragment.setMessage("AP Mode");
                        }

                        @Override
                        public void onFailure(int reason) {

                            if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                                Log.e(TAG, "Unsupported P2P");

                            } else if (reason == WifiP2pManager.BUSY) {
                                Log.e(TAG, "Busy");
                                Toast.makeText(getApplicationContext(), "Busy, please reset WiFi and disconnect from all APs", Toast.LENGTH_SHORT).show();

                            } else {
                                Log.e(TAG, "Internal Error");
                            }
                        }
                    });

                }

                @Override
                public void onFailure(int reason) {

                    Toast.makeText(getApplicationContext(), R.string.create_group_fail, Toast.LENGTH_SHORT).show();
                    if (reason == WifiP2pManager.BUSY)
                        Log.e(TAG, "Wifip2pgroup failed to create: BUSY");
                    else if (reason == WifiP2pManager.ERROR)
                        Log.e(TAG, "Wifip2pgroup failed to create: ERROR");
                    else
                        Log.e(TAG, "Wifip2pgroup failed to create: P2P Unsupported");

                    mSectionsPagerAdapter.hostingWiFiP2PFragment.toggleSwitch(false);
                    setDiscoveryOff();

                }
            });
        }


        else{
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
                    mSectionsPagerAdapter.hostingWiFiP2PFragment.setMessage("AP Mode");
                }

                @Override
                public void onFailure(int reason) {

                    if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                        Log.e(TAG, "Unsupported P2P");

                    } else if (reason == WifiP2pManager.BUSY) {
                        Log.e(TAG, "Busy");
                        Toast.makeText(getApplicationContext(), "Busy, please reset WiFi and disconnect from all APs", Toast.LENGTH_SHORT).show();

                    } else {
                        Log.e(TAG, "Internal Error");
                    }
                }
            });
        }


    }

    public void setCertificate(String url){
        // do something
        cacert_customize_url = url;
        if(cacert_customize_url.length() != 0) {
            CertDownloader downloader = new CertDownloader();
            downloader.run();
        }
        else{
            Toast.makeText(getApplicationContext(), "CA set to default", Toast.LENGTH_SHORT).show();
        }

    }

    class CertDownloader implements Runnable{

        @Override
        public void run() {
            Ion.with(getApplicationContext())
                    .load(cacert_customize_url)
                    .asByteArray()
                    .setCallback(new FutureCallback<byte[]>() {
                        @Override
                        public void onCompleted(Exception e, byte[] result) {


                            X509Certificate certificate;
                            // now we want to verify that this is a certificate
                            try {
                                ByteArrayInputStream stream = new ByteArrayInputStream(result);
                                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509"); // standard X.509 type cert
                                certificate = (X509Certificate) certificateFactory.generateCertificate(stream); // must be DER or PKCS #7
                            }
                            catch (CertificateException ce)
                            {
                                Toast.makeText(getApplicationContext(), "Not a valid certificate file", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            catch (Exception ex){
                                Toast.makeText(getApplicationContext(), "Download error", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if(certificate == null) {
                                Toast.makeText(getApplicationContext(), "Empty CA", Toast.LENGTH_SHORT).show();
                                return; //
                            }

                            try{
                                certificate.checkValidity();
                            } catch (CertificateExpiredException expired){
                                Toast.makeText(getApplicationContext(), "Expired CA", Toast.LENGTH_SHORT).show();
                                return; //
                            } catch (CertificateNotYetValidException notValid){
                                Toast.makeText(getApplicationContext(), "Not yet valid CA", Toast.LENGTH_SHORT).show();
                            }

                            try{
                                certificate.verify(certificate.getPublicKey());
                            } catch (Exception ex){
                                Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Pop window asking whether we should accept certificate or not
                            ConnectDialogFragment f = new ConnectDialogFragment();
                            Bundle args = new Bundle();
                            args.putSerializable("ca",certificate);
                            f.setArguments(args);
                            f.show(getSupportFragmentManager(),"importca");
                        }
                    });

        }
    }
}
