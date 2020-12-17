package com.nitramite.apcupsdmonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Logging
    private static final String TAG = "DatabaseHelper";

    // Variables
    private Context context;

    // For database updating
    private List<String> columnsUps;
    private List<String> columnsEventsData;
    private boolean upgrade = false;

    // DATABASE VERSION
    private static final int DATABASE_VERSION = 3;
    // 1 = v1.1.5
    // 2 = v1.2.2, added ups load events boolean
    // 3 = v1.8.7, added ups_reachable boolean


    // DATABASE NAME
    private static final String DATABASE_NAME = "UPS.db";

    // TABLE NAME'S
    private static final String UPS_TABLE = "Ups";
    private static final String EVENTS_TABLE = "Events";

    // -------------------------------------------------------------------

    // TABLE ROWS NAMES - For ups data
    private static final String UPS_ID = "id"; // Ups id
    static final String UPS_CONNECTION_TYPE = "ups_connection_type";
    static final String UPS_SERVER_ADDRESS = "server_address";
    static final String UPS_SERVER_PORT = "server_port";
    static final String UPS_SERVER_USERNAME = "server_username";
    static final String UPS_SERVER_PASSWORD = "server_password";
    static final String UPS_USE_PRIVATE_KEY_AUTH = "server_use_private_key_auth";
    static final String UPS_PRIVATE_KEY_PASSWORD = "server_private_key_password";
    static final String UPS_PRIVATE_KEY_PATH = "server_private_key_path";
    static final String UPS_SERVER_SSH_STRICT_HOST_KEY_CHECKING = "server_strict_host_key_checking";
    static final String UPS_SERVER_STATUS_COMMAND = "server_status_command";
    static final String UPS_SERVER_EVENTS_LOCATION = "server_events_location";
    static final String UPS_SERVER_HOST_NAME = "server_host_name";
    static final String UPS_SERVER_HOST_FINGER_PRINT = "server_host_finger_print";
    static final String UPS_SERVER_HOST_KEY = "server_host_key";
    static final String UPS_STATUS_STR = "ups_status_str"; // status message containing all lines
    static final String UPS_LOAD_EVENTS = "ups_load_events";
    static final String UPS_REACHABLE = "ups_reachable";

    // -------------------------------------------------------------------

    /* Database for events data */
    private static final String EVENT_ID = "id";
    private static final String EVENT_UPS_ID = "ups_id";
    private static final String EVENT_STR = "event_str"; // one event line string

    // -------------------------------------------------------------------

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        // ***BACKUP*** Backup old database stuff
        if (upgrade) {

            // Ups table alter
            columnsUps = GetColumns(db, UPS_TABLE);
            db.execSQL("ALTER TABLE " + UPS_TABLE + " RENAME TO TEMP_" + UPS_TABLE);

            // Create events table if not exists
            db.execSQL("CREATE TABLE IF NOT EXISTS " + EVENTS_TABLE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, ups_id TEXT, event_str TEXT)");
        }

        // ***CREATE***  Create new Ups table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + UPS_TABLE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, ups_connection_type TEXT, server_address TEXT, server_port TEXT, server_username TEXT, " +
                "server_password TEXT, server_use_private_key_auth TEXT, server_private_key_password TEXT, server_private_key_path TEXT, server_strict_host_key_checking TEXT, " +
                "server_status_command TEXT, server_events_location TEXT, server_host_name TEXT, server_host_finger_print TEXT, server_host_key TEXT, " +
                "ups_status_str TEXT, ups_load_events TEXT, ups_reachable TEXT)");

        // Create events data table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + EVENTS_TABLE + "(id INTEGER PRIMARY KEY AUTOINCREMENT, ups_id TEXT, event_str TEXT)");


        // ***RESTORE***  Restore from old
        if (upgrade) {
            // Ups
            columnsUps.retainAll(GetColumns(db, UPS_TABLE));
            String parcelCols = join(columnsUps, ",");
            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s FROM TEMP_%s", UPS_TABLE, parcelCols, parcelCols, UPS_TABLE));
            db.execSQL("DROP TABLE TEMP_" + UPS_TABLE);

            // Events
            // columnsEventsData.retainAll(GetColumns(db, EVENTS_TABLE));
            // String trackingCols = join(columnsEventsData, ",");
            // db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s FROM TEMP_%s", EVENTS_TABLE, trackingCols, trackingCols, EVENTS_TABLE));
            // db.execSQL("DROP TABLE TEMP_" + EVENTS_TABLE);
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        upgrade = true;
        onCreate(db);
    }


    // Returns this database as readable for exported
    public SQLiteDatabase getReadableDatabaseObject() {
        return this.getReadableDatabase();
    }


    // ---------------------------------------------------------------------------------------------


    /**
     * Insert or update ups
     *
     * @param upsId         String
     * @param contentValues Values
     * @return db id
     */
    Long insertUpdateUps(String upsId, ContentValues contentValues) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result;
        if (upsId == null) {
            result = db.insert(UPS_TABLE, null, contentValues);
        } else {
            result = db.update(UPS_TABLE, contentValues, " id = ?", new String[]{upsId});
        }
        db.close();
        return result;
    }


    ArrayList<UPS> getAllUps(String upsId) {
        ArrayList<UPS> upsArrayList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res;
        if (upsId == null) {
            res = db.rawQuery("SELECT * FROM " + UPS_TABLE + " ORDER BY " + UPS_ID + " ASC", null);
        } else {
            res = db.rawQuery("SELECT * FROM " + UPS_TABLE + " WHERE " + UPS_ID + " = ? ORDER BY " + UPS_ID + " ASC", new String[]{upsId});
        }
        while (res.moveToNext()) {
            UPS ups = new UPS();
            ups.UPS_ID = res.getString(0);
            ups.UPS_CONNECTION_TYPE = res.getString(1);
            ups.UPS_SERVER_ADDRESS = res.getString(2);
            ups.UPS_SERVER_PORT = res.getString(3);
            ups.UPS_SERVER_USERNAME = res.getString(4);
            ups.UPS_SERVER_PASSWORD = res.getString(5);
            ups.UPS_USE_PRIVATE_KEY_AUTH = res.getString(6);
            ups.UPS_PRIVATE_KEY_PASSWORD = res.getString(7);
            ups.UPS_PRIVATE_KEY_PATH = res.getString(8);
            ups.UPS_SERVER_SSH_STRICT_HOST_KEY_CHECKING = res.getString(9);
            ups.UPS_SERVER_STATUS_COMMAND = res.getString(10);
            ups.UPS_SERVER_EVENTS_LOCATION = res.getString(11);
            ups.UPS_SERVER_HOST_NAME = res.getString(12);
            ups.UPS_SERVER_HOST_FINGER_PRINT = res.getString(13);
            ups.UPS_SERVER_HOST_KEY = res.getString(14);
            ups.setUPS_STATUS_STR(res.getString(15));
            ups.UPS_LOAD_EVENTS = res.getString(16);
            ups.setUPS_REACHABLE_STATUS(res.getString(17));
            upsArrayList.add(ups);
        }
        res.close();
        db.close();
        return upsArrayList;
    }


    // Delete data with id
    void deleteUps(String upsId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(UPS_TABLE, UPS_ID + " = ?", new String[]{upsId});
        db.delete(EVENTS_TABLE, EVENT_UPS_ID + " = ?", new String[]{upsId});
        db.close();
    }


    void insertEvents(String upsId, ArrayList<String> events) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(EVENTS_TABLE, EVENT_UPS_ID + " = ?", new String[]{upsId});
        for (int i = 0; i < events.size(); i++) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(EVENT_UPS_ID, upsId);
            contentValues.put(EVENT_STR, events.get(i));
            db.insert(EVENTS_TABLE, null, contentValues);
        }
        db.close();
    }


    ArrayList<String> getAllEvents(final String upsId) {
        ArrayList<String> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + EVENTS_TABLE + " WHERE " + EVENT_UPS_ID + " = ?" +
                " ORDER BY " + EVENT_ID + " ASC", new String[]{upsId});
        while (res.moveToNext()) {
            events.add(res.getString(2));
        }
        res.close();
        db.close();
        return events;
    }


    Boolean isAnyUpsDown() {
        SQLiteDatabase db = this.getReadableDatabase();
        boolean isUpsDown = false;
        Cursor res;
        res = db.rawQuery("SELECT * FROM " + UPS_TABLE + " ORDER BY " + UPS_ID + " ASC", null);
        while (res.moveToNext()) {
            UPS ups = new UPS();
            ups.setUPS_STATUS_STR(res.getString(15));
            Log.i(TAG, "DatabaseHelper isAnyUpsDown: " + ups.getSTATUS());
            if (!ups.getSTATUS().contains("ONLINE")) {
                isUpsDown = true;
            }
        }
        res.close();
        db.close();
        return isUpsDown;
    }


    // ---------------------------------------------------------------------------------------------
    /* Database upgrade script */

    private static List<String> GetColumns(SQLiteDatabase db, String tableName) {
        List<String> ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 1", null);
            if (c != null) {
                ar = new ArrayList<>(Arrays.asList(c.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return ar;
    }

    public static String join(List<String> list, String delim) {
        StringBuilder buf = new StringBuilder();
        int num = list.size();
        for (int i = 0; i < num; i++) {
            if (i != 0)
                buf.append(delim);
            buf.append((String) list.get(i));
        }
        return buf.toString();
    }

    // ---------------------------------------------------------------------------------------------

} // End of class