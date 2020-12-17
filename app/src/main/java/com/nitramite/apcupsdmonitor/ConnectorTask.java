package com.nitramite.apcupsdmonitor;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Array;
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
    private final static String TAG = ConnectorTask.class.getSimpleName();

    // Command variables
    private String statusCommand = Constants.STATUS_COMMAND_APCUPSD;
    private String eventsLocation = Constants.EVENTS_LOCATION;

    // Variables
    private ArrayList<UPS> upsArrayList = new ArrayList<>();
    private Integer arrayPosition = 0;
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
        Log.i(TAG, "Connector provided ups id: " + upsId_ + " meaning we update " +
                (upsId_ == null ? "all ups statuses" : "one ups status"));
        taskMode = taskMode_;
        this.upsId = upsId_;
        databaseHelper = new DatabaseHelper(context);
        this.apcupsdInterface = apcupsdInterface;
        this.execute();
    }


    @Override
    protected String doInBackground(String... params) {
        try {
            upsArrayList.clear();
            arrayPosition = 0;
            upsArrayList = databaseHelper.getAllUps(this.upsId);
            if (upsArrayList.size() > 0) {
                upsTaskHelper();
            } else {
                apcupsdInterface.noUpsConfigured();
            }
        } catch (RuntimeException e) {
            Log.i(TAG, e.toString());
        }
        return null;
    }


    /**
     * Goes thru added ups devices and load statuses and events
     */
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
            if (connectionType.equals(UPS.UPS_CONNECTION_TYPE_NIS)) {
                // APCUPSD SOCKET
                if (validAPCUPSDRequirements()) {
                    if (connectSocketServer(upsArrayList.get(arrayPosition).UPS_ID, this.address, this.port)) {
                        getUPSStatusAPCUPSD(upsArrayList.get(arrayPosition).UPS_ID,
                                upsArrayList.get(arrayPosition).getUpsLoadEvents());
                    } else {
                        onConnectionError(upsArrayList.get(arrayPosition).UPS_ID);
                    }
                } else {
                    apcupsdInterface.onMissingPreferences();
                }
            } else if (connectionType.equals(UPS.UPS_CONNECTION_TYPE_SSH)) {
                // SSH
                if (validSSHRequirements()) {
                    if (connectSSHServer(upsArrayList.get(arrayPosition).UPS_ID)) {
                        getUPSStatusSSH(upsArrayList.get(arrayPosition).UPS_ID,
                                upsArrayList.get(arrayPosition).getUpsLoadEvents());
                    } else {
                        onConnectionError(upsArrayList.get(arrayPosition).UPS_ID);
                    }
                } else {
                    apcupsdInterface.onMissingPreferences();
                }
            } else {
                Log.w(TAG, "Unsupported UPS connection type");
            }
        } else {
            apcupsdInterface.onTaskCompleted();
        }
    }


    private void onConnectionError(final String upsId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.UPS_REACHABLE, UPS.UPS_NOT_REACHABLE);
        databaseHelper.insertUpdateUps(upsId, contentValues);
        apcupsdInterface.onConnectionError(upsId);
        arrayPosition++;
        upsTaskHelper();
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
            }
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }


    // ---------------------------------------------------------------------------------------------

    // Socket connection
    private Boolean connectSocketServer(final String upsId, final String ip, final int port) {
        try {
            InetAddress serverAddress = InetAddress.getByName(ip);
            socket = new Socket(serverAddress, port);
            socket.setSoTimeout(10 * 1000);
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
    private void getUPSStatusSSH(final String upsId, final boolean loadEvents) {
        try {

            StringBuilder stringBuilder = new StringBuilder();
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(this.statusCommand);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream input = channel.getInputStream();
            channel.connect();


            // Can be replaced by test string (/*input*/ testInputStream)
            InputStreamReader inputReader = new InputStreamReader(
                    input
                    // Mock.ApcupsdMockData()
                    // Mock.SynologyMockData()
                    // Mock.APCNetworkCardMockData()
            );


            BufferedReader bufferedReader = new BufferedReader(inputReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            bufferedReader.close();
            inputReader.close();
            channel.disconnect();

            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.UPS_REACHABLE, UPS.UPS_REACHABLE);
            contentValues.put(DatabaseHelper.UPS_STATUS_STR, stringBuilder.toString());
            databaseHelper.insertUpdateUps(upsId, contentValues);

            if (this.taskMode == TaskMode.MODE_ACTIVITY) {
                getUPSEvents(upsId, loadEvents);
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


    private void getUPSStatusAPCUPSD(final String upsId, final boolean loadEvents) {
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

            getUPSEventsAPCUPSD(upsId, loadEvents);

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
    private void getUPSEvents(final String upsId, final boolean loadEvents) {
        ArrayList<String> events = new ArrayList<>();
        if (loadEvents) {
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


    private void getUPSEventsAPCUPSD(final String upsId, final Boolean loadEvents) {
        ArrayList<String> events = new ArrayList<>();
        try {

            if (loadEvents) {

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