package com.nitramite.apcupsdmonitor;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nitramite.ui.FileDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UpsEditor extends AppCompatActivity {

    // Variables
    private SharedPreferences sharedPreferences;
    private String upsId = null;
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ups_editor);

        // Shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // New or existing package
        Intent intent = getIntent();
        upsId = intent.getStringExtra("UPS_ID");


        final Switch connectionTypeSwitch = findViewById(R.id.connectionTypeSwitch);

        // Editable views
        final EditText serverAddressET = findViewById(R.id.serverAddressET);
        final EditText serverPortET = findViewById(R.id.serverPortET);
        final EditText serverUsernameET = findViewById(R.id.serverUsernameET);
        final EditText serverPasswordET = findViewById(R.id.serverPasswordET);
        final Switch privateKeyAuthSwitch = findViewById(R.id.privateKeyAuthSwitch);
        final EditText privateKeyPassphraseET = findViewById(R.id.privateKeyPassphraseET);
        final EditText privateKeyLocationET = findViewById(R.id.privateKeyLocationET);
        final Switch strictHostKeyCheckingSwitch = findViewById(R.id.strictHostKeyCheckingSwitch);
        final EditText statusCommandET = findViewById(R.id.statusCommandET);
        final Switch loadUpsEventsSwitch = findViewById(R.id.loadUpsEventsSwitch);
        final EditText eventsLocationET = findViewById(R.id.eventsLocationET);


        final Spinner cmdPresetSelection = findViewById(R.id.cmdPresetSelection);
        List<String> cmdPresetOptions = new ArrayList<>();
        cmdPresetOptions.add(getString(R.string.click_to_select));
        cmdPresetOptions.add(getString(R.string.apcupsd_daemon_software));
        cmdPresetOptions.add(getString(R.string.apcupsd_daemon_software_no_sudo));
        cmdPresetOptions.add(getString(R.string.synology_upsc));
        cmdPresetOptions.add(getString(R.string.apc_network_management_card_aos));
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cmdPresetOptions);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cmdPresetSelection.setAdapter(dataAdapter);
        cmdPresetSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        break;
                    case 1:
                        statusCommandET.setText(Constants.STATUS_COMMAND_APCUPSD);
                        loadUpsEventsSwitch.setChecked(true);
                        break;
                    case 2:
                        statusCommandET.setText(Constants.STATUS_COMMAND_APCUPSD_NO_SUDO);
                        loadUpsEventsSwitch.setChecked(true);
                        break;
                    case 3:
                        statusCommandET.setText(Constants.STATUS_COMMAND_SYNOLOGY);
                        loadUpsEventsSwitch.setChecked(false);
                        break;
                    case 4:
                        statusCommandET.setText(Constants.STATUS_COMMAND_APC_NETWORK_CARD);
                        loadUpsEventsSwitch.setChecked(false);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        final LinearLayout sshOptionsLayout = findViewById(R.id.sshOptionsLayout);

        connectionTypeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sshOptionsLayout.setVisibility(View.GONE);
            } else {
                sshOptionsLayout.setVisibility(View.VISIBLE);
            }
        });


        // Set values
        if (upsId == null) {
            setTitle(getString(R.string.ups_editor_create_new));
            // Defaults
            statusCommandET.setText(Constants.STATUS_COMMAND_APCUPSD);
            eventsLocationET.setText(Constants.EVENTS_LOCATION);
        } else {
            setTitle(getString(R.string.ups_editor_update_existing));
            UPS ups = databaseHelper.getAllUps(upsId).get(0);
            connectionTypeSwitch.setChecked(ups.UPS_CONNECTION_TYPE.equals(UPS.UPS_CONNECTION_TYPE_NIS));
            serverAddressET.setText(ups.UPS_SERVER_ADDRESS);
            serverPortET.setText(ups.UPS_SERVER_PORT);
            serverUsernameET.setText(ups.UPS_SERVER_USERNAME);
            serverPasswordET.setText(ups.UPS_SERVER_PASSWORD);
            privateKeyAuthSwitch.setChecked(ups.UPS_USE_PRIVATE_KEY_AUTH.equals("1"));
            privateKeyLocationET.setText(ups.UPS_PRIVATE_KEY_PATH);
            privateKeyPassphraseET.setText(ups.UPS_PRIVATE_KEY_PASSWORD);
            strictHostKeyCheckingSwitch.setChecked(ups.UPS_SERVER_SSH_STRICT_HOST_KEY_CHECKING.equals("1"));
            statusCommandET.setText(ups.UPS_SERVER_STATUS_COMMAND);
            eventsLocationET.setText(ups.UPS_SERVER_EVENTS_LOCATION);
            loadUpsEventsSwitch.setChecked(ups.getUpsLoadEvents());
        }


        Button tutorialBtn = findViewById(R.id.tutorialBtn);
        tutorialBtn.setOnClickListener(view -> {
            String url = "http://www.nitramite.com/apcupsdmonitor.html";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });

        Button selectPrivateKeyLocationBtn = findViewById(R.id.selectPrivateKeyLocationBtn);
        selectPrivateKeyLocationBtn.setOnClickListener(view -> {

            File mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
            final FileDialog fileDialog = new FileDialog(UpsEditor.this, mPath, "");
            fileDialog.addFileListener(file -> {
                Toast.makeText(UpsEditor.this, getString(R.string.path) + ": " + file.toString(), Toast.LENGTH_SHORT).show();
                privateKeyLocationET.setText(file.toString());
            });
            fileDialog.showDialog();

        });


        Button negativeBtn = findViewById(R.id.negativeBtn);
        negativeBtn.setOnClickListener(view -> UpsEditor.this.finish());


        Button positiveBtn = findViewById(R.id.positiveBtn);
        positiveBtn.setOnClickListener(view -> {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.UPS_CONNECTION_TYPE,
                    connectionTypeSwitch.isChecked() ? UPS.UPS_CONNECTION_TYPE_NIS : UPS.UPS_CONNECTION_TYPE_SSH);
            contentValues.put(DatabaseHelper.UPS_SERVER_ADDRESS, serverAddressET.getText().toString());
            contentValues.put(DatabaseHelper.UPS_SERVER_PORT, serverPortET.getText().toString());
            contentValues.put(DatabaseHelper.UPS_SERVER_USERNAME, serverUsernameET.getText().toString());
            contentValues.put(DatabaseHelper.UPS_SERVER_PASSWORD, serverPasswordET.getText().toString());
            contentValues.put(DatabaseHelper.UPS_USE_PRIVATE_KEY_AUTH, privateKeyAuthSwitch.isChecked() ? "1" : "0");
            contentValues.put(DatabaseHelper.UPS_PRIVATE_KEY_PASSWORD, privateKeyPassphraseET.getText().toString());
            contentValues.put(DatabaseHelper.UPS_PRIVATE_KEY_PATH, privateKeyLocationET.getText().toString());
            contentValues.put(DatabaseHelper.UPS_SERVER_SSH_STRICT_HOST_KEY_CHECKING, strictHostKeyCheckingSwitch.isChecked() ? "1" : "0");
            contentValues.put(DatabaseHelper.UPS_SERVER_STATUS_COMMAND, statusCommandET.getText().toString());
            contentValues.put(DatabaseHelper.UPS_SERVER_EVENTS_LOCATION, eventsLocationET.getText().toString());
            contentValues.put(DatabaseHelper.UPS_LOAD_EVENTS, loadUpsEventsSwitch.isChecked() ? "1" : "0");
            databaseHelper.insertUpdateUps(upsId, contentValues);
            Toast.makeText(UpsEditor.this, R.string.saved, Toast.LENGTH_SHORT).show();
            UpsEditor.this.finish();
        });


        Switch autoOpenUpsSwitch = findViewById(R.id.autoOpenUpsSwitch);
        final String sharedPrefsUpsId = sharedPreferences.getString(Constants.SP_AUTO_OPEN_UPS_ID, null);
        if (upsId != null && sharedPrefsUpsId != null) {
            if (sharedPrefsUpsId.equals(upsId)) {
                autoOpenUpsSwitch.setChecked(true);
            }
        }
        autoOpenUpsSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences.Editor prefsEditor;
            prefsEditor = sharedPreferences.edit();
            if (b) {
                prefsEditor.putString(Constants.SP_AUTO_OPEN_UPS_ID, upsId);
            } else {
                prefsEditor.remove(Constants.SP_AUTO_OPEN_UPS_ID);
            }
            prefsEditor.apply();
        });

    } // End of onCreate();


}
