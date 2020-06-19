package com.nitramite.apcupsdmonitor;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

// Had methods to query information from ConnectorTask
@SuppressWarnings("FieldCanBeLocal")
public class ConnectorTask extends AsyncTask<String, String, String> {

    // Logging
    private final static String TAG = "ConnectorTask";

    // Command variables
    private String statusCommand = Constants.STATUS_COMMAND;
    private String eventsLocation = Constants.EVENTS_LOCATION;

    // Variables
    private ArrayList<UPS> upsArrayList = new ArrayList<>();
    private Integer arrayPosition = 0;
    private SharedPreferences sharedPreferences;
    private String upsId = null; // If provided, updates only one ups
    private TaskMode taskMode; // Activity or service task, on service skip getting events
    private String address = null;
    private Integer port = 22;
    private String sshUsername = null;
    private String sshPassword = null;
    private Boolean strictHostKeyChecking = false;
    private String sshHostName = null;
    private String sshHostFingerPrint = null;
    private String sshHostKey = null;

    // Private key variables
    private Boolean privateKeyFileEnabled = false;
    private String privateKeyFilePassphrase = "";
    private String privateKeyFileLocation = null;

    // Interface
    private ConnectorInterface apcupsdInterface;

    // SSH Library
    private Session session = null;

    // Database
    private DatabaseHelper databaseHelper;

    // APC Socket
    private Socket socket;


    // Constructor
    ConnectorTask(final ConnectorInterface apcupsdInterface, Context context, TaskMode taskMode_, final String upsId_) {
        Log.i(TAG, "Connector provided ups id: " + upsId_);
        taskMode = taskMode_;
        this.upsId = upsId_;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        databaseHelper = new DatabaseHelper(context);
        this.apcupsdInterface = apcupsdInterface;
        this.execute();
    }


    @Override
    protected String doInBackground(String... params) {
        upsArrayList.clear();
        arrayPosition = 0;
        upsArrayList = databaseHelper.getAllUps(this.upsId);
        if (upsArrayList.size() > 0) {
            upsTaskHelper();
        } else {
            apcupsdInterface.noUpsConfigured();
        }
        return null;
    }


    private void upsTaskHelper() {
        if (arrayPosition < upsArrayList.size()) {
            if (arrayPosition > 0) {
                apcupsdInterface.onRefreshList(); // Refresh list view
            }

            final String connectionType = upsArrayList.get(arrayPosition).UPS_CONNECTION_TYPE;

            this.statusCommand = upsArrayList.get(arrayPosition).UPS_SERVER_STATUS_COMMAND;
            this.eventsLocation = upsArrayList.get(arrayPosition).UPS_SERVER_EVENTS_LOCATION;
            this.address = upsArrayList.get(arrayPosition).UPS_SERVER_ADDRESS;
            this.port = portStringToInteger(upsArrayList.get(arrayPosition).UPS_SERVER_PORT);
            this.sshUsername = upsArrayList.get(arrayPosition).UPS_SERVER_USERNAME;
            this.sshPassword = upsArrayList.get(arrayPosition).UPS_SERVER_PASSWORD;
            this.strictHostKeyChecking = upsArrayList.get(arrayPosition).UPS_SERVER_SSH_STRICT_HOST_KEY_CHECKING.equals("1");

            this.sshHostName = upsArrayList.get(arrayPosition).UPS_SERVER_HOST_NAME;
            this.sshHostFingerPrint = upsArrayList.get(arrayPosition).UPS_SERVER_HOST_FINGER_PRINT;
            this.sshHostKey = upsArrayList.get(arrayPosition).UPS_SERVER_HOST_KEY;

            // Private key feature
            this.privateKeyFileEnabled = upsArrayList.get(arrayPosition).UPS_USE_PRIVATE_KEY_AUTH.equals("1");
            this.privateKeyFilePassphrase = upsArrayList.get(arrayPosition).UPS_PRIVATE_KEY_PASSWORD;
            this.privateKeyFileLocation = upsArrayList.get(arrayPosition).UPS_PRIVATE_KEY_PATH;


            // Determine connection type
            if (connectionType.equals("1")) {
                // APCUPSD SOCKET
                if (validAPCUPSDRequirements()) {
                    if (connectSocketServer(upsArrayList.get(arrayPosition).UPS_ID, this.address, this.port)) {
                        getUPSStatusAPCUPSD(upsArrayList.get(arrayPosition).UPS_ID);
                    } else {
                        apcupsdInterface.onConnectionError();
                    }
                } else {
                    apcupsdInterface.onMissingPreferences();
                }
            } else {
                // SSH
                if (validSSHRequirements()) {
                    if (connectSSHServer(upsArrayList.get(arrayPosition).UPS_ID)) {
                        getUPSStatusSSH(upsArrayList.get(arrayPosition).UPS_ID);
                    }
                } else {
                    apcupsdInterface.onMissingPreferences();
                }
            }
        } else {
            apcupsdInterface.onTaskCompleted();
        }
    }


    @Override
    protected void onPostExecute(String param) {
    }


    private Boolean validAPCUPSDRequirements() {
        return this.address != null && this.port != -1;
    }

    private Boolean validSSHRequirements() {
        return this.address != null && this.port != -1 && this.sshUsername != null && (this.sshPassword != null || this.privateKeyFileLocation != null);
    }


    // ---------------------------------------------------------------------------------------------

    // Connect ssh server
    private Boolean connectSSHServer(final String upsId) {
        JSch sshClient = null;
        try {
            sshClient = new JSch();

            if (this.privateKeyFileEnabled && privateKeyFileLocation != null) {
                // Use private key
                sshClient.addIdentity(privateKeyFileLocation, privateKeyFilePassphrase);
                session = sshClient.getSession(this.sshUsername, this.address, this.port);
            } else {
                // Use username and password
                session = sshClient.getSession(this.sshUsername, this.address, this.port);
                session.setPassword(this.sshPassword);
            }


            if (!this.strictHostKeyChecking) {
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect(5000);
                return true;
            } else {
                // https://stackoverflow.com/questions/43646043/jsch-how-to-let-user-confirm-host-fingerprint
                session.setConfig("StrictHostKeyChecking", "yes");
                if (this.sshHostKey != null) {
                    byte[] keyBytes = Base64.decode(this.sshHostKey, Base64.DEFAULT);
                    sshClient.getHostKeyRepository().add(new HostKey(this.sshHostName, keyBytes), null);
                }
                session.connect();
                return true;
            }
        } catch (JSchException e) {
            if (e.toString().contains("reject HostKey")) {
                apcupsdInterface.onAskToTrustKey(
                        upsId,
                        session.getHostKey().getHost(),
                        session.getHostKey().getFingerPrint(sshClient),
                        session.getHostKey().getKey()
                );
            } else {
                apcupsdInterface.onConnectionError();
            }
            return false;
        } catch (NullPointerException e) {
            apcupsdInterface.onConnectionError();
            return false;
        }
    }


    // ---------------------------------------------------------------------------------------------

    // Socket connection
    private Boolean connectSocketServer(final String upsId, final String ip, final int port) {
        try {
            InetAddress serverAddress = InetAddress.getByName(ip);
            socket = new Socket(serverAddress, port);
            return true;
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ---------------------------------------------------------------------------------------------


    // Get ups status
    private void getUPSStatusSSH(final String upsId) {
        try {

            StringBuilder stringBuilder = new StringBuilder();
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(this.statusCommand);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream input = channel.getInputStream();
            channel.connect();

            /* APCUPSD TEST INPUT STARTS FROM HERE */
            /*
            String input_test =
                "APC      : 001,037,0940\n" +
                "DATE     : 2019-09-20 09:10:34 +0200\n" +
                "HOSTNAME : nebula\n" +
                "VERSION  : 3.14.14 (31 May 2016) debian\n" +
                "UPSNAME  : HomeUPS\n" +
                "CABLE    : USB Cable\n" +
                "DRIVER   : USB UPS Driver\n" +
                "UPSMODE  : Stand Alone\n" +
                "STARTTIME: 2019-09-04 21:52:07 +0200\n" +
                "MODEL    : Back-UPS XS 950U\n" +
                "STATUS   : ONLINE\n" +
                "LINEV    : 232.0 Volts\n" +
                "LOADPCT  : 12.0 Percent\n" +
                "BCHARGE  : 100.0 Percent\n" +
                "TIMELEFT : 48.0 Minutes\n" +
                "MBATTCHG : 5 Percent\n" +
                "MINTIMEL : 3 Minutes\n" +
                "MAXTIME  : 0 Seconds\n" +
                "SENSE    : Medium\n" +
                "LOTRANS  : 155.0 Volts\n" +
                "HITRANS  : 280.0 Volts\n" +
                "ALARMDEL : 30 Seconds\n" +
                "BATTV    : 13.4 Volts\n" +
                "LASTXFER : Unacceptable line voltage changes\n" +
                "NUMXFERS : 2\n" +
                "XONBATT  : 2019-09-12 09:32:19 +0200\n" +
                "TONBATT  : 0 Seconds\n" +
                "CUMONBATT: 117 Seconds\n" +
                "XOFFBATT : 2019-09-12 09:34:13 +0200\n" +
                "SELFTEST : NO\n" +
                "STATFLAG : 0x05000008\n" +
                "SERIALNO : 3B1739X28322\n" +
                "BATTDATE : 2017-10-01\n" +
                "NOMINV   : 230 Volts\n" +
                "NOMBATTV : 12.0 Volts\n" +
                "NOMPOWER : 480 Watts\n" +
                "FIRMWARE : 925.T2 .I USB FW:T2\n" +
                "END APC  : 2019-09-20 09:10:35 +0200\n";
            InputStream testInputStream = new ByteArrayInputStream(input_test.getBytes(StandardCharsets.UTF_8));
            */

            /* SYNOLOGY NAS  TEST INPUT STARTS FROM HERE */
            String input_test =
                "battery.charge: 100\n" + 
                "battery.charge.low: 10\n" + 
                "battery.charge.warning: 50\n" + 
                "battery.runtime: 5580\n" + 
                "battery.runtime.low: 120\n" + 
                "battery.type: PbAc\n" + 
                "battery.voltage: 27.0\n" + 
                "battery.voltage.nominal: 24.0\n" + 
                "device.mfr: American Power Conversion\n" + 
                "device.model: Smart-UPS 750\n" + 
                "device.serial: AS1244114679 \n" + 
                "device.type: ups\n" + 
                "driver.name: usbhid-ups\n" + 
                "driver.parameter.pollfreq: 30\n" + 
                "driver.parameter.pollinterval: 5\n" + 
                "driver.parameter.port: auto\n" + 
                "driver.version: DSM6-2-25364-191230\n" + 
                "driver.version.data: APC HID 0.95\n" + 
                "driver.version.internal: 0.38\n" + 
                "ups.beeper.status: disabled\n" + 
                "ups.delay.shutdown: 20\n" + 
                "ups.firmware: UPS 08.3 / ID=18\n" + 
                "ups.mfr: American Power Conversion\n" + 
                "ups.mfr.date: 2012/11/01\n" + 
                "ups.model: Smart-UPS 750\n" + 
                "ups.productid: 0003\n" + 
                "ups.serial: AS1244114679 \n" + 
                "ups.status: OL\n" + 
                "ups.timer.reboot: -1\n" + 
                "ups.timer.shutdown: -1\n" + 
                "ups.vendorid: 051d\n";
            InputStream testInputStream = new ByteArrayInputStream(input_test.getBytes(StandardCharsets.UTF_8));



            /* TEST INPUT ENDS HERE */

            InputStreamReader inputReader = new InputStreamReader(/*input*/ testInputStream); // Can be replaced by test string (/*input*/ testInputStream)
            BufferedReader bufferedReader = new BufferedReader(inputReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            bufferedReader.close();
            inputReader.close();
            channel.disconnect();

            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.UPS_STATUS_STR, stringBuilder.toString());
            databaseHelper.insertUpdateUps(upsId, contentValues);

            if (this.taskMode == TaskMode.MODE_ACTIVITY) {
                getUPSEvents(upsId);
            } else {
                arrayPosition++;
                upsTaskHelper();
            }
        } catch (JSchException | IOException e) {
            e.printStackTrace();
            apcupsdInterface.onCommandError(e.toString());
            sessionDisconnect();
        }
    }


    private void getUPSStatusAPCUPSD(final String upsId) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            byte[] message = {0x00, 0x06, 0x73, 0x74, 0x61, 0x74, 0x75, 0x73};
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

            dOut.write(message);           // write the message
            // dOut.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.length() > 3) {
                    final String line_ = line.substring(2, line.length()) + "\n";
                    stringBuilder.append(line_);
                    // Log.i(TAG, line_);
                    if (line_.contains("END APC")) {
                        break;
                    }
                }
            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.UPS_STATUS_STR, stringBuilder.toString());
            databaseHelper.insertUpdateUps(upsId, contentValues);

            getUPSEventsAPCUPSD(upsId);

        } catch (UnknownHostException e) {
            Log.i(TAG, e.toString());
            apcupsdInterface.onCommandError(e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.i(TAG, e.toString());
            apcupsdInterface.onCommandError(e.toString());
            e.printStackTrace();
        } catch (Exception e) {
            Log.i(TAG, e.toString());
            apcupsdInterface.onCommandError(e.toString());
            e.printStackTrace();
        }
    }


    // ---------------------------------------------------------------------------------------------


    // Get ups events
    private void getUPSEvents(final String upsId) {
        ArrayList<String> events = new ArrayList<>();
        if (sharedPreferences.getBoolean(Constants.SP_LOAD_EVENTS, true)) {
            Log.i(TAG, "Loading events...");
            try {
                ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                channelSftp.get(this.eventsLocation, byteArrayOutputStream);
                BufferedReader bufferedReader = new BufferedReader(new StringReader(byteArrayOutputStream.toString()));
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    events.add(line);
                }
                bufferedReader.close();
                channelSftp.disconnect();
                sessionDisconnect();
                databaseHelper.insertEvents(upsId, events);
            } catch (JSchException | IOException | SftpException e) {
                e.printStackTrace();
                apcupsdInterface.onCommandError(e.toString());
                sessionDisconnect();
            }
        }
        arrayPosition++;
        upsTaskHelper();
    }



    private void getUPSEventsAPCUPSD(final String upsId) {
        ArrayList<String> events = new ArrayList<>();
        try {

            if (sharedPreferences.getBoolean(Constants.SP_LOAD_EVENTS, true)) {

                byte[] message = {0x00, 0x06, 0x65, 0x76, 0x65, 0x6e, 0x74, 0x73};
                DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

                dOut.write(message);           // write the message
                // dOut.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                socket.setSoTimeout(1500);
                String line = null;
                try {
                    while ((line = in.readLine()) != null) {
                        if (line.length() > 3) {
                            final String line_ = line.substring(2, line.length());
                            events.add(line_);
                            Log.i(TAG, line_);
                        }
                    }
                } catch (SocketTimeoutException ignored) {
                }

                in.close();
                dOut.close();

                databaseHelper.insertEvents(upsId, events);

            }

            arrayPosition++;
            upsTaskHelper();

        } catch (UnknownHostException e) {
            Log.i(TAG, e.toString());
            apcupsdInterface.onCommandError(e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.i(TAG, e.toString());
            apcupsdInterface.onCommandError(e.toString());
            e.printStackTrace();
        } catch (Exception e) {
            Log.i(TAG, e.toString());
            apcupsdInterface.onCommandError(e.toString());
            e.printStackTrace();
        }
    }


    // ---------------------------------------------------------------------------------------------

    // Disconnect ssh session
    private void sessionDisconnect() {
        session.disconnect();
    }


    /**
     * Helps to turn integer to string
     *
     * @param port port
     * @return integer port
     */
    private Integer portStringToInteger(String port) {
        try {
            return Integer.valueOf(port);
        } catch (NumberFormatException e) {
            return 0;
        }
    }


} // End of class