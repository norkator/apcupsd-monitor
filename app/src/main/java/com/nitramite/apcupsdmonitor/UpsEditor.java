package com.nitramite.apcupsdmonitor;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.nitramite.ui.FileDialog;

import java.io.File;

public class UpsEditor extends AppCompatActivity {

    // App camera permissions
    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 2;
    private final String permissionReadStorage = Manifest.permission.READ_EXTERNAL_STORAGE;

    // Variables
    private SharedPreferences sharedPreferences;
    private String upsId = null;
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);

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
            statusCommandET.setText(Constants.STATUS_COMMAND);
            eventsLocationET.setText(Constants.EVENTS_LOCATION);
        } else {
            setTitle(getString(R.string.ups_editor_update_existing));
            UPS ups = databaseHelper.getAllUps(upsId).get(0);
            connectionTypeSwitch.setChecked(ups.UPS_CONNECTION_TYPE.equals("1"));
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
            if (hasPermissions(UpsEditor.this, new String[]{permissionReadStorage})) {
                File mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
                final FileDialog fileDialog = new FileDialog(UpsEditor.this, mPath, "");
                fileDialog.addFileListener(file -> {
                    Toast.makeText(UpsEditor.this, getString(R.string.path) + ": " + file.toString(), Toast.LENGTH_SHORT).show();
                    privateKeyLocationET.setText(file.toString());
                });
                fileDialog.showDialog();
            } else {
                ActivityCompat.requestPermissions(UpsEditor.this, new String[]{permissionReadStorage}, READ_EXTERNAL_STORAGE_REQUEST_CODE);
            }
        });


        Button negativeBtn = findViewById(R.id.negativeBtn);
        negativeBtn.setOnClickListener(view -> UpsEditor.this.finish());


        Button positiveBtn = findViewById(R.id.positiveBtn);
        positiveBtn.setOnClickListener(view -> {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.UPS_CONNECTION_TYPE, connectionTypeSwitch.isChecked() ? "1" : "0");
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


    // Check for required permissions
    private static boolean hasPermissions(Context context, String[] permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


}
