# How to Build?

Prerequisites: Android SDK with following packages installed
Android SDK Tools - 25.1.1 or newer
Android SDK Platform Tools - 23.0.3 or newer
Android API 23 SDK Platform - 23 or newer
Android Support Repository - 30 or newer
Android Support Library - 23.2.1 or newer

Build the APK for debug:
run 
./autobuild.sh <SDK Directory>

# How to run?

1. Launch the commissioner application.

2. In SCAN tab, toggle the button to turn on the discovery mode. If this is successful, no message will show up and a moment later nearby WiFi P2P device will show up in the list. If this step fails, then a pop up toast will indicate what type of error occured. In this case, please turn off the discovery mode, go to menu, click "Reset WiFi" and try again.

3. Click the device you want to connect to. The application will create a P2P group and invite the device to join. When the device is joined, a double arrow icon will indicate that it's connected.

4. (Optional) If you want to import another CA certificate, please click "Set CA Certificate" in the menu. Enter the URL of the CA and the device will prompt for confirmation.

5. Switch to AUTHENTICATE TAB, toggle the button to turn on the scanner for available services. In about half a minute available services will show up. Click the service to initiate the authentication process. When there's an incoming connection from the device, the commissioner app will present its certificate to you and ask for user's consent to transfer the network credential. Simply click yes and the network credential will be transferred securely.

6. If any of the previous steps fails and you want to reset the application. Toggle off both switches in SCAN and AUTHENTICATE tabs and reset WiFi, or simply quit the application and relaunch it.

# Known limitations

1. In a lot of devices, WiFi P2P can only work when the mobile/tablet is disconnected to any WiFi networks. So if the application fails to work with popup always saying "BUSY", please remove all saved networks in Android Settings - WiFi so that the application can function correctly.

2. Sometimes it might take a while for a service to show up in the AUTHENTICATE tab. It should not exceed 5 minutes in all cases. If it still fails to show up after a while, please retoggle (off and then on) the switch in AUTHENTICATE tab to reinitiate the discovery process.

