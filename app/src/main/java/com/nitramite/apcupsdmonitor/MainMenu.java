package com.nitramite.apcupsdmonitor;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nitramite.apcupsdmonitor.notifier.PushUtils;
import com.wdullaer.swipeactionadapter.SwipeActionAdapter;
import com.wdullaer.swipeactionadapter.SwipeDirection;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class MainMenu extends AppCompatActivity implements ConnectorInterface, PurchasesUpdatedListener, SwipeActionAdapter.SwipeActionListener, SwipeRefreshLayout.OnRefreshListener {

    // http://www.apcupsd.org/manual/manual.html

    // Logging
    private final static String TAG = "MainMenu";

    // In app billing
    private BillingClient mBillingClient;

    // Variables
    protected SwipeActionAdapter mAdapter;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper databaseHelper = new DatabaseHelper(this);
    private ArrayList<UPS> upsArrayList = new ArrayList<>();
    private ListView upsListView;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Activity request codes
    public static final int ACTIVITY_RESULT_NEW_UPS_ADDED = 1;


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setAppActivityRunning(false);
        try {
            if (sharedPreferences != null) {
                if (sharedPreferences.getBoolean(Constants.SP_NOTIFICATIONS_ENABLED, true)) {
                    PushUtils.subscribeToTopic(PushUtils.TOPIC_UPDATE);
                } else
                    PushUtils.unsubscribeFromTopic(PushUtils.TOPIC_UPDATE);

            }
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set theme
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // Override thread policy
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setAppActivityRunning(true);

        // Floating action buttons
        FloatingActionButton floatingAddUpsBtn = findViewById(R.id.floatingAddNewUpsBtn);
        floatingAddUpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenu.this, UpsEditor.class);
                startActivityForResult(intent, ACTIVITY_RESULT_NEW_UPS_ADDED);
            }
        });

        upsListView = findViewById(R.id.upsListView);

        upsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainMenu.this, UpsViewer.class);
                intent.putExtra("UPS_ID", upsArrayList.get(i).UPS_ID);
                startActivity(intent);
            }
        });

        upsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                directionRightEdit(position);
                return true;
            }
        });

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);


        // Init in app billing
        initInAppBilling();

        // Initial ups data load
        getUpsData();

        // Get status data
        startConnectorTask();
    } // End of onCreate()


    // ---------------------------------------------------------------------------------------------

    // Get's UPS data
    private void startConnectorTask() {
        progressBar = findViewById(R.id.progressBar);
        progressBar.setScaleY(3f);
        progressBar.setVisibility(View.VISIBLE);
        new ConnectorTask(this, this, TaskMode.MODE_ACTIVITY, null); // Update all
    }


    private void getUpsData() {
        upsArrayList = databaseHelper.getAllUps(null);
        CustomUpsAdapter customUpsAdapter = new CustomUpsAdapter(this, upsArrayList);

        mAdapter = new SwipeActionAdapter(customUpsAdapter);
        mAdapter.setSwipeActionListener(this)
                .setDimBackgrounds(true)
                .setListView(upsListView);
        mAdapter.addBackground(SwipeDirection.DIRECTION_NORMAL_RIGHT, R.layout.swipe_right_edit)
                .addBackground(SwipeDirection.DIRECTION_NORMAL_LEFT, R.layout.swipe_left_delete);

        upsListView.setAdapter(mAdapter);
    }

    @Override
    public boolean hasActions(int position, SwipeDirection direction) {
        if (direction.isLeft()) return true;
        if (direction.isRight()) return true;
        return false;
    }

    @Override
    public boolean shouldDismiss(int position, SwipeDirection direction) {
        return direction == SwipeDirection.DIRECTION_NORMAL_RIGHT;
    }

    @Override
    public void onSwipe(int[] positionList, SwipeDirection[] directionList) {
        for (int i = 0; i < positionList.length; i++) {
            SwipeDirection direction = directionList[i];
            int onSwipePosition = positionList[i];
            switch (direction) {
                case DIRECTION_NORMAL_RIGHT:
                    directionRightEdit(onSwipePosition);
                    break;
                case DIRECTION_NORMAL_LEFT:
                    deleteItemConfirmationDialog(onSwipePosition);
                    break;
                case DIRECTION_FAR_RIGHT:
                    directionRightEdit(onSwipePosition);
                    break;
                case DIRECTION_FAR_LEFT:
                    deleteItemConfirmationDialog(onSwipePosition);
                    break;
            }
        }
    }

    // Swipe from left to right
    private void directionRightEdit(final int swipePosition) {
        Intent intent = new Intent(MainMenu.this, UpsEditor.class);
        intent.putExtra("UPS_ID", upsArrayList.get(swipePosition).UPS_ID);
        startActivity(intent);
    }


    private void deleteItemConfirmationDialog(final int swipePosition) {
        new AlertDialog.Builder(MainMenu.this)
                .setTitle(R.string.delete_item)
                .setMessage(R.string.delete_item_message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    databaseHelper.deleteUps(upsArrayList.get(swipePosition).UPS_ID);
                    MainMenu.this.getUpsData();
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    // Return
                })
                .setIcon(R.mipmap.logo)
                .show();
    }


    // ---------------------------------------------------------------------------------------------


    @Override
    public void noUpsConfigured() {
        runOnUiThread(() -> {
            closeProgressDialog();
            new AlertDialog.Builder(MainMenu.this)
                    .setTitle(R.string.no_ups_devices_title)
                    .setMessage(R.string.no_ups_devices_message)
                    .setNegativeButton(R.string.close, (dialogInterface, i) -> {
                    })
                    .setIcon(R.mipmap.logo)
                    .show();
        });
    }

    @Override
    public void onAskToTrustKey(final String upsId, final String hostName, final String hostFingerPrint, final String hostKey) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closeProgressDialog();
                new AlertDialog.Builder(MainMenu.this)
                        .setIcon(R.mipmap.logo)
                        .setMessage(getString(R.string.trust_host) + " " + hostName + " " + getString(R.string.with_following_key_fingerprint) + "\n\n" + hostFingerPrint)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, (dialog, id) -> {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(DatabaseHelper.UPS_SERVER_HOST_NAME, hostName);
                            contentValues.put(DatabaseHelper.UPS_SERVER_HOST_FINGER_PRINT, hostFingerPrint);
                            contentValues.put(DatabaseHelper.UPS_SERVER_HOST_KEY, hostKey);
                            databaseHelper.insertUpdateUps(upsId, contentValues);
                            startConnectorTask(); // Load again
                        })
                        .setNegativeButton(R.string.no, (dialog, id) ->
                                genericSuccessDialog(getString(R.string.note), getString(R.string.host_fingerprint_message)))
                        .show();
            }
        });
    }


    @Override
    public void onRefreshList() {
        runOnUiThread(this::getUpsData);
    }


    @Override
    public void onTaskCompleted() {
        runOnUiThread(() -> {
            closeProgressDialog();
            getUpsData();
            final String autoOpenUpsId = sharedPreferences.getString(Constants.SP_AUTO_OPEN_UPS_ID, null);
            if (autoOpenUpsId != null) {
                Intent intent = new Intent(MainMenu.this, UpsViewer.class);
                intent.putExtra("UPS_ID", autoOpenUpsId);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onMissingPreferences() {
        runOnUiThread(() -> {
            closeProgressDialog();
            checkYourPreferencesDialog();
        });
    }


    // Generic use success dialog
    private void checkYourPreferencesDialog() {
        new AlertDialog.Builder(MainMenu.this)
                .setTitle(R.string.missing_settings)
                .setMessage(R.string.ssh_connection_properties_not_set_message)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> startActivityForResult(new Intent(MainMenu.this, Preferences.class), 200))
                .setNegativeButton(R.string.close, (dialogInterface, i) -> {
                })
                .setIcon(R.mipmap.logo)
                .show();
    }


    // Call back result from other activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            startConnectorTask();
        }
        if (requestCode == ACTIVITY_RESULT_NEW_UPS_ADDED) {
            getUpsData();
            startConnectorTask();
        }
    }

    @Override
    public void onConnectionError() {
        runOnUiThread(() -> {
            closeProgressDialog();
            genericErrorDialog(getString(R.string.error),
                    getString(R.string.connection_error_one) + "\n\n" +
                            getString(R.string.connection_error_two) + "\n\n" +
                            getString(R.string.connection_error_three) + " '" + sharedPreferences.getString(Constants.SP_STATUS_COMMAND, "sudo apcaccess status") + "' " +
                            getString(R.string.connection_error_four) +
                            getString(R.string.connection_error_five)
            );
        });
    }

    @Override
    public void onCommandError(final String errorStr) {
        runOnUiThread(() -> {
            closeProgressDialog();
            genericErrorDialog(getString(R.string.error), getString(R.string.command_error_result) + " " + errorStr);
        });
    }


    // ---------------------------------------------------------------------------------------------
    // Helpers


    // Update activity running bit
    private void setAppActivityRunning(Boolean running) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.SP_ACTIVITY_RUNNING, running);
        editor.apply();
    }


    // Close progressbar in controlled way
    private void closeProgressDialog() {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }


    // Generic use success dialog
    private void genericSuccessDialog(final String title, final String description) {
        new AlertDialog.Builder(MainMenu.this)
                .setTitle(title)
                .setMessage(description)
                .setPositiveButton(R.string.close, (dialog, which) -> {
                })
                .setIcon(R.mipmap.ic_launcher_round)
                .show();
    }


    // Generic use error dialog
    private void genericErrorDialog(final String title, final String description) {
        try {
            new AlertDialog.Builder(MainMenu.this)
                    .setTitle(title)
                    .setMessage(description)
                    .setPositiveButton(R.string.close, (dialog, which) -> {
                    })
                    .setNeutralButton(R.string.copy_content, (dialog, which) -> {
                        try {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("", description);
                            assert clipboard != null;
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(MainMenu.this, R.string.content_copied, Toast.LENGTH_SHORT).show();
                        } catch (IndexOutOfBoundsException e) {
                            Toast.makeText(MainMenu.this, R.string.nothing_to_copy, Toast.LENGTH_LONG).show();
                        }
                    })
                    .setIcon(R.drawable.ic_error_small)
                    .show();
        } catch (WindowManager.BadTokenException e) {
            Log.e(TAG, e.toString());
        }
    }


    // Check if service is running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void onRefresh() {
        startConnectorTask();
    }

    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            startConnectorTask();
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainMenu.this, Preferences.class));
            return true;
        }
        if (id == R.id.action_rate_app) {
            String url = "https://play.google.com/store/apps/details?id=com.nitramite.apcupsdmonitor";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
            return true;
        }
        if (id == R.id.action_project_github) {
            String url = "https://github.com/norkator/apcupsd-monitor";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
            return true;
        }
        if (id == R.id.donateBtn) {
            donateDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------------------------------------------------------------------
    /* In app billing features */


    // Initialize in app billing feature
    private void initInAppBilling() {
        // In app billing
        mBillingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The billing client is ready. You can query purchases here.
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }


    private void donateDialog() {
        new AlertDialog.Builder(this)
                .setIcon(R.mipmap.logo)
                .setTitle("Donate")
                .setMessage(
                        "Donate only if you have find this application useful so far.\n\n" +
                                "Via donating you help future development. " +
                                "For example I have plan to implement multi UPS support as a next big step."
                )
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        inAppPurchase(Constants.IAP_ITEM_SKU_DONATE_MEDIUM);
                    }
                })
                .setNegativeButton("Return", null)
                .show();
    }


    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                /*if (purchase.getSku().equals(Constants.IAP_ITEM_SKU_DONATE_MEDIUM)) {
                    acknowledgePurchase(purchase);
                }*/
                // Also run acknowledgePurchase here
                if (!purchase.isAcknowledged()) {
                    acknowledgePurchase(purchase);
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED && purchases != null) {
            for (Purchase purchase : purchases) {
                if (!purchase.isAcknowledged()) {
                    acknowledgePurchase(purchase);
                }
            }
        } else {
            // Handle any other error codes.
        }
    }


    /**
     * Acknowledge purchase required by billing lib 2.x++
     *
     * @param purchase billing purchase
     */
    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams acknowledgePurchaseParams =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
    }


    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
        @Override
        public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
            Toast.makeText(MainMenu.this, "Purchase acknowledged!", Toast.LENGTH_SHORT).show();
        }
    };


    public void inAppPurchase(final String IAP_ITEM_SKU) {
        if (mBillingClient.isReady()) {

            List<String> skuList = new ArrayList<>();
            skuList.add(IAP_ITEM_SKU);

            SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
                    .setSkusList(skuList).setType(BillingClient.SkuType.INAPP).build();

            mBillingClient.querySkuDetailsAsync(skuDetailsParams, new SkuDetailsResponseListener() {
                @Override
                public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                    try {
                        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetailsList.get(0))
                                .build();
                        mBillingClient.launchBillingFlow(MainMenu.this, flowParams);
                    } catch (IndexOutOfBoundsException e) {
                        genericErrorDialog(getString(R.string.error), e.toString());
                    }
                }
            });
        } else {
            genericErrorDialog("Billing service", "Billing service is not initialized yet. Please try again later.");
            initInAppBilling();
        }
    }


    /* In app Restore purchases */
    public void restorePurchases() {
        mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, new PurchaseHistoryResponseListener() {
            @Override
            public void onPurchaseHistoryResponse(BillingResult billingResult, List<PurchaseHistoryRecord> purchaseHistoryRecordList) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchaseHistoryRecordList != null) {
                    if (purchaseHistoryRecordList.size() > 0) {
                        for (PurchaseHistoryRecord purchase : purchaseHistoryRecordList) {
                        }
                    } else {
                        genericErrorDialog(getString(R.string.error), "No purchases made");
                    }
                } else {
                    genericErrorDialog(getString(R.string.error), "Error querying purchased items");
                }
            }
        });
    }


} // End of class