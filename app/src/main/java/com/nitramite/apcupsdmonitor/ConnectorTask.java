package com.nitramite.apcupsdmonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
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
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;


public class ConnectorTask {

    // Logging
    private final static String TAG = ConnectorTask.class.getSimpleName();

    // Variables
    private ArrayList<Thread> threads;
    private ArrayList<UPS> upsArrayList = new ArrayList<>();
    private Integer completed = 0;
    private final TaskMode taskMode; // Activity or service task, on service skip getting events

    // Interface
    private final ConnectorInterface apcupsdInterface;

    // Database
    private final DatabaseHelper databaseHelper;


    // Constructor
    ConnectorTask(final ConnectorInterface apcupsdInterface, Context context, TaskMode taskMode_, final String upsId) {
        Log.i(TAG, "Connector provided ups id: " + upsId + " meaning we update " +
                (upsId == null ? "all ups statuses" : "one ups status"));
        taskMode = taskMode_;
        databaseHelper = new DatabaseHelper(context);
        this.apcupsdInterface = apcupsdInterface;
        this.doInBackground(upsId);
    }


    void doInBackground(String upsId) {
        try {
            threads = new ArrayList<>();
            upsArrayList.clear();
            this.completed = 0;
            upsArrayList = databaseHelper.getAllUps(upsId, true);
            SQLiteDatabase writablePool = databaseHelper.getWritablePool();

            if (upsArrayList.size() > 0) {
                Thread thread = new Thread() {
                    @SuppressWarnings("BusyWait")
                    @Override
                    public void run() {
                        for (int i = 0; i < upsArrayList.size(); i++) {
                            threads.add(new UPSTaskThread(writablePool, upsArrayList.get(i)));
                        }
                        for (int t = 0; t < threads.size(); t++) {
                            try {
                                threads.get(t).start();
                                sleep(100);
                            } catch (InterruptedException | IllegalThreadStateException e) {
                                e.printStackTrace();
                                Log.e(TAG, e.toString());
                            }
                        }
                    }
                };
                thread.start();
            } else {
                apcupsdInterface.noUpsConfigured();
            }
        } catch (RuntimeException e) {
            this.apcupsdInterface.onTaskError(e.toString());
        }
    }


    /**
     * Called when any of UPS updates is finished
     */
    private void oneReady(SQLiteDatabase writablePool) {
        apcupsdInterface.onRefreshList(); // Refresh list view
        this.completed++;
        if (completed == threads.size()) {
            databaseHelper.closePool(writablePool);
            apcupsdInterface.onTaskCompleted();
        }
    }


    /**
     * Thread task goes through added ups devices and load statuses and events
     */
    private class UPSTaskThread extends Thread {

        private final SQLiteDatabase writablePool;
        private final UPS ups;

        UPSTaskThread(SQLiteDatabase writablePool, UPS ups) {
            this.writablePool = writablePool;
            this.ups = ups;
        }

        public void run() {
            Thread.currentThread().setName(ups.UPS_ID);

            if (ups.UPS_CONNECTION_TYPE == null) {
                ups.UPS_CONNECTION_TYPE = ConnectionType.UPS_CONNECTION_TYPE_NA;
            }

            // Determine connection type
            switch (ups.UPS_CONNECTION_TYPE) {
                case ConnectionType.UPS_CONNECTION_TYPE_NIS:
                    // APCUPSD SOCKET
                    if (validAPCUPSDRequirements(ups.UPS_SERVER_ADDRESS, portStringToInteger(ups.UPS_SERVER_PORT))) {
                        try {
                            Socket socket = connectSocketServer(ups.UPS_SERVER_ADDRESS, portStringToInteger(ups.UPS_SERVER_PORT));
                            getUPSStatusAPCUPSD(writablePool, socket, ups.UPS_ID, ups.getUpsLoadEvents());
                        } catch (IOError e) {
                            apcupsdInterface.onCommandError(e.toString());
                        } catch (IOException e) {
                            onConnectionError(writablePool, ups.UPS_ID);
                        }
                    } else {
                        apcupsdInterface.onMissingPreferences();
                    }
                    break;
                case ConnectionType.UPS_CONNECTION_TYPE_SSH:
                    // SSH
                    if (validSSHRequirements(
                            ups.UPS_SERVER_ADDRESS, portStringToInteger(ups.UPS_SERVER_PORT),
                            ups.UPS_SERVER_USERNAME, ups.UPS_SERVER_PASSWORD, ups.UPS_PRIVATE_KEY_PATH
                    )) {
                        try {
                            Session session = connectSSHServer(ups);
                            if (session != null && session.isConnected()) {
                                if (ups.UPS_IS_APC_NMC) {
                                    getUPSStatusNMC(writablePool, session, ups, ups.getUpsLoadEvents());
                                } else {
                                    getUPSStatusSSH(writablePool, session, ups, ups.getUpsLoadEvents());
                                }
                            }
                        } catch (NullPointerException e) {
                            onConnectionError(writablePool, ups.UPS_ID);
                        }
                    } else {
                        apcupsdInterface.onMissingPreferences();
                    }
                    break;
                case ConnectionType.UPS_CONNECTION_TYPE_IPM:
                    // Eaton IPM
                    IPM ipm = new IPM(
                            true, ups.UPS_SERVER_ADDRESS, ups.UPS_SERVER_PORT,
                            ups.UPS_SERVER_USERNAME, ups.UPS_SERVER_PASSWORD, ups.UPS_NODE_ID
                    );
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DatabaseHelper.UPS_REACHABLE, UPS.UPS_REACHABLE);
                    if (ipm.getNodeStatus() != null) {
                        contentValues.put(DatabaseHelper.UPS_STATUS_STR, ipm.getNodeStatus());
                    }
                    databaseHelper.insertUpdateUps(writablePool, ups.UPS_ID, contentValues);
                    if (ipm.getEvents().size() > 0) {
                        databaseHelper.insertEvents(writablePool, ups.UPS_ID, ipm.getEvents());
                    }
                    break;
                default:
                    Log.w(TAG, "Unsupported UPS connection type");
                    break;
            }
            oneReady(writablePool);
        }
    }


    private void onConnectionError(SQLiteDatabase writablePool, final String upsId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.UPS_REACHABLE, UPS.UPS_NOT_REACHABLE);
        databaseHelper.insertUpdateUps(writablePool, upsId, contentValues);
        apcupsdInterface.onConnectionError(upsId);
    }


    private Boolean validAPCUPSDRequirements(String address, Integer port) {
        return address != null && port != -1;
    }

    private Boolean validSSHRequirements(String address, Integer port, String sshUsername, String sshPassword, String privateKeyFileLocation) {
        return address != null && port != -1 && sshUsername != null && (sshPassword != null || privateKeyFileLocation != null);
    }


    // ---------------------------------------------------------------------------------------------

    /**
     * Connect ssh server and return session
     *
     * @param ups objects
     * @return ssh session
     */
    private Session connectSSHServer(UPS ups) {
        JSch sshClient = null;
        Session session = null;
        try {
            sshClient = new JSch();

            if (ups.UPS_USE_PRIVATE_KEY_AUTH.equals("1") && ups.UPS_PRIVATE_KEY_PATH != null) {
                // Use private key
                sshClient.addIdentity(ups.UPS_PRIVATE_KEY_PATH, ups.UPS_PRIVATE_KEY_PASSWORD);
                session = sshClient.getSession(ups.UPS_SERVER_USERNAME, ups.UPS_SERVER_ADDRESS, portStringToInteger(ups.UPS_SERVER_PORT));
            } else {
                // Use username and password
                session = sshClient.getSession(ups.UPS_SERVER_USERNAME, ups.UPS_SERVER_ADDRESS, portStringToInteger(ups.UPS_SERVER_PORT));
                session.setPassword(ups.UPS_SERVER_PASSWORD);
            }


            if (!ups.UPS_SERVER_SSH_STRICT_HOST_KEY_CHECKING.equals("1")) {
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect(5000);
                return session;
            } else {
                // https://stackoverflow.com/questions/43646043/jsch-how-to-let-user-confirm-host-fingerprint
                session.setConfig("StrictHostKeyChecking", "yes");
                if (ups.UPS_SERVER_HOST_KEY != null) {
                    byte[] keyBytes = Base64.decode(ups.UPS_SERVER_HOST_KEY, Base64.DEFAULT);
                    sshClient.getHostKeyRepository().add(new HostKey(ups.UPS_SERVER_HOST_NAME, keyBytes), null);
                }
                session.connect();
                return session;
            }
        } catch (JSchException e) {
            if (e.toString().contains("reject HostKey")) {
                apcupsdInterface.onAskToTrustKey(
                        ups.UPS_ID,
                        session.getHostKey().getHost(),
                        session.getHostKey().getFingerPrint(sshClient),
                        session.getHostKey().getKey()
                );
            }
            return session;
        }
    }


    // ---------------------------------------------------------------------------------------------

    // Socket connection
    private Socket connectSocketServer(final String ip, final int port) throws IOException {
        InetAddress serverAddress = InetAddress.getByName(ip);
        Socket socket = new Socket(serverAddress, port);
        socket.setSoTimeout(10 * 1000);
        return socket;
    }

    // ---------------------------------------------------------------------------------------------

    // Get ups status for APC NMC cards
    private void getUPSStatusNMC(
            SQLiteDatabase writablePool, Session session, final UPS ups, final boolean loadEvents
    ) {
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
            databaseHelper.insertUpdateUps(writablePool, ups.UPS_ID, contentValues);

            if (this.taskMode == TaskMode.MODE_ACTIVITY) {
                getNMCEvents(writablePool, session, ups, loadEvents);
            }
        } catch (JSchException | IOException e) {
            e.printStackTrace();
            apcupsdInterface.onCommandError(e.toString());
            sessionDisconnect(session);
        }
    }

    // Get ups status
    private void getUPSStatusSSH(
            SQLiteDatabase writablePool, Session session, final UPS ups, final boolean loadEvents
    ) {
        try {

            StringBuilder stringBuilder = new StringBuilder();
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(Constants.STATUS_COMMAND_APCUPSD);
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
            databaseHelper.insertUpdateUps(writablePool, ups.UPS_ID, contentValues);

            if (this.taskMode == TaskMode.MODE_ACTIVITY) {
                getUPSEvents(writablePool, session, ups.UPS_ID, loadEvents);
            }
        } catch (JSchException | IOException e) {
            e.printStackTrace();
            apcupsdInterface.onCommandError(e.toString());
            sessionDisconnect(session);
        }
    }


    private void getUPSStatusAPCUPSD(
            SQLiteDatabase writablePool, Socket socket, final String upsId, final boolean loadEvents
    ) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
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
        databaseHelper.insertUpdateUps(writablePool, upsId, contentValues);

        getUPSEventsAPCUPSD(writablePool, socket, upsId, loadEvents);
    }


    // ---------------------------------------------------------------------------------------------


    // Get ups events
    private void getUPSEvents(
            SQLiteDatabase writablePool, Session session, final String upsId, final boolean loadEvents
    ) {
        ArrayList<String> events = new ArrayList<>();
        if (loadEvents) {
            Log.i(TAG, "Loading events...");
            try {
                ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                channelSftp.get(Constants.EVENTS_LOCATION, byteArrayOutputStream);
                BufferedReader bufferedReader = new BufferedReader(new StringReader(byteArrayOutputStream.toString()));
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    events.add(line);
                }
                bufferedReader.close();
                channelSftp.disconnect();
                sessionDisconnect(session);
                databaseHelper.insertEvents(writablePool, upsId, events);
            } catch (JSchException | IOException | SftpException e) {
                e.printStackTrace();
                apcupsdInterface.onCommandError(e.toString());
                sessionDisconnect(session);
            }
        }
    }

    private void getNMCEvents(
            SQLiteDatabase writablePool, Session session, final UPS ups, final boolean loadEvents
    ) {
        ArrayList<String> events = new ArrayList<>();
        if (loadEvents) {
            Log.i(TAG, "Loading events...");
            try {
                this.connectSSHServer(ups);

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
                sessionDisconnect(session);
                databaseHelper.insertEvents(writablePool, ups.UPS_ID, events);
            } catch (JSchException | IOException e) {
                e.printStackTrace();
                apcupsdInterface.onCommandError(e.toString());
                sessionDisconnect(session);
            }
        }
    }

    private void getUPSEventsAPCUPSD(
            SQLiteDatabase writablePool, Socket socket, final String upsId, final Boolean loadEvents
    ) {
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

                databaseHelper.insertEvents(writablePool, upsId, events);

            }

        } catch (Exception e) {
            Log.i(TAG, e.toString());
            apcupsdInterface.onCommandError(e.toString());
            e.printStackTrace();
        }
    }


    // ---------------------------------------------------------------------------------------------

    // Disconnect ssh session
    private void sessionDisconnect(Session session) {
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
