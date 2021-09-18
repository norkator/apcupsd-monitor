package com.nitramite.apcupsdmonitor;

import android.annotation.SuppressLint;
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
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

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
    private final TaskMode taskMode; // Activity or service task, on service skip getting events
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
    private final ConnectorInterface apcupsdInterface;

    // SSH Library
    private Session session = null;

    // Database
    private final DatabaseHelper databaseHelper;

    // APC Socket
    private Socket socket;

    @SuppressLint("StaticFieldLeak")
    private final Context context;


    // Constructor
    @SuppressWarnings("deprecation")
    ConnectorTask(final ConnectorInterface apcupsdInterface, Context context, TaskMode taskMode_, final String upsId_) {
        Log.i(TAG, "Connector provided ups id: " + upsId_ + " meaning we update " +
                (upsId_ == null ? "all ups statuses" : "one ups status"));
        this.context = context;
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

            UPS ups = upsArrayList.get(arrayPosition);
            final String connectionType = ups.UPS_CONNECTION_TYPE;

            this.statusCommand = ups.UPS_SERVER_STATUS_COMMAND;
            this.eventsLocation = ups.UPS_SERVER_EVENTS_LOCATION;
            this.address = ups.UPS_SERVER_ADDRESS;
            this.port = portStringToInteger(ups.UPS_SERVER_PORT);
            this.sshUsername = ups.UPS_SERVER_USERNAME;
            this.sshPassword = ups.UPS_SERVER_PASSWORD;
            this.strictHostKeyChecking = ups.UPS_SERVER_SSH_STRICT_HOST_KEY_CHECKING.equals("1");

            this.sshHostName = ups.UPS_SERVER_HOST_NAME;
            this.sshHostFingerPrint = ups.UPS_SERVER_HOST_FINGER_PRINT;
            this.sshHostKey = ups.UPS_SERVER_HOST_KEY;

            // Private key feature
            this.privateKeyFileEnabled = ups.UPS_USE_PRIVATE_KEY_AUTH.equals("1");
            this.privateKeyFilePassphrase = ups.UPS_PRIVATE_KEY_PASSWORD;
            this.privateKeyFileLocation = ups.UPS_PRIVATE_KEY_PATH;


            // Determine connection type
            if (connectionType.equals(ConnectionType.UPS_CONNECTION_TYPE_NIS)) {
                // APCUPSD SOCKET
                if (validAPCUPSDRequirements()) {
                    if (connectSocketServer(ups.UPS_ID, this.address, this.port)) {
                        getUPSStatusAPCUPSD(ups.UPS_ID, ups.getUpsLoadEvents());
                    } else {
                        onConnectionError(ups.UPS_ID);
                    }
                } else {
                    apcupsdInterface.onMissingPreferences();
                }
            } else if (connectionType.equals(ConnectionType.UPS_CONNECTION_TYPE_SSH)) {
                // SSH
                if (validSSHRequirements()) {
                    if (connectSSHServer(ups.UPS_ID)) {
                        if (ups.UPS_IS_APC_NMC) {
                            getUPSStatusNMC(ups.UPS_ID, ups.getUpsLoadEvents());
                        } else {
                            getUPSStatusSSH(ups.UPS_ID, ups.getUpsLoadEvents());
                        }
                    } else {
                        onConnectionError(ups.UPS_ID);
                    }
                } else {
                    apcupsdInterface.onMissingPreferences();
                }
            } else if (connectionType.equals(ConnectionType.UPS_CONNECTION_TYPE_IPM)) {
                // Eaton IPM
                IPM ipm = new IPM(
                        context, ups.UPS_SERVER_ADDRESS, ups.UPS_SERVER_PORT,
                        ups.UPS_SERVER_USERNAME, ups.UPS_SERVER_PASSWORD, ups.UPS_NODE_ID
                );
                ContentValues contentValues = new ContentValues();
                contentValues.put(DatabaseHelper.UPS_REACHABLE, UPS.UPS_REACHABLE);
                if (ipm.getNodeStatus() != null) {
                    contentValues.put(DatabaseHelper.UPS_STATUS_STR, ipm.getNodeStatus());
                }
                databaseHelper.insertUpdateUps(ups.UPS_ID, contentValues);
                if (ipm.getEvents().size() > 0) {
                    databaseHelper.insertEvents(ups.UPS_ID, ipm.getEvents());
                }
                // next
                arrayPosition++;
                upsTaskHelper();
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

    // Get ups status for APC NMC cards
    private void getUPSStatusNMC(final String upsId, final boolean loadEvents) {
        try {
            Channel channel = session.openChannel("shell");
            InputStream in = channel.getInputStream();
            OutputStream out = channel.getOutputStream();
            channel.connect();

            StringBuilder stringBuilder = new StringBuilder();
            Scanner scanner = new Scanner(in);
            //We have to send the commands one by one like this
            //Otherwise, the NMC ssh server just gives up on us ¯\_(ツ)_/¯
            scanner.useDelimiter("apc>");
            stringBuilder.append(scanner.next());
            out.write("detstatus -all\n".getBytes());
            out.flush();
            stringBuilder.append(scanner.next());
            out.write("upsabout\n".getBytes());
            out.flush();
            stringBuilder.append(scanner.next());
            out.write("exit\n".getBytes());
            out.flush();
            while (scanner.hasNext()) {
                stringBuilder.append(scanner.next());
            }
            scanner.close();
            channel.disconnect();

            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.UPS_REACHABLE, UPS.UPS_REACHABLE);
            String output = stringBuilder.toString();
            contentValues.put(DatabaseHelper.UPS_STATUS_STR, output);
            databaseHelper.insertUpdateUps(upsId, contentValues);

            if (this.taskMode == TaskMode.MODE_ACTIVITY) {
                getNMCEvents(upsId, loadEvents);
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
                    // Mock.APCNetworkCardMockDataAP9630()
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
            contentValues.put(DatabaseHelper.UPS_REACHABLE, UPS.UPS_REACHABLE);
            contentValues.put(DatabaseHelper.UPS_STATUS_STR, stringBuilder.toString());
            databaseHelper.insertUpdateUps(upsId, contentValues);

            getUPSEventsAPCUPSD(upsId, loadEvents);

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

    private void getNMCEvents(final String upsId, final boolean loadEvents) {
        ArrayList<String> events = new ArrayList<>();
        if (loadEvents) {
            Log.i(TAG, "Loading events...");
            try {
                this.connectSSHServer(upsId);

                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand("apc-scp -f event.txt");
                channel.connect();

                InputStream in = channel.getInputStream();
                Scanner scanner = new Scanner(in);
                scanner.useDelimiter("\r");
                StringBuilder stringBuilder = new StringBuilder();
                while (scanner.hasNext()) {
                    stringBuilder.append(scanner.next());
                }

                String eventsString = stringBuilder.toString();
                if (eventsString.contains("Date,Time,User,Event,Code")) {
                    String eventString = eventsString.split("Date,Time,User,Event,Code")[1];
                    events.addAll(Arrays.asList(eventString.split("\n")));
                }

                channel.disconnect();
                sessionDisconnect();
                databaseHelper.insertEvents(upsId, events);
            } catch (JSchException | IOException e) {
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
