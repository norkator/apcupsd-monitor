package com.nitramite.apcupsdmonitor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Locale;

public class Preferences extends AppCompatActivity {
    private static final String TAG = Preferences.class.getSimpleName();


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }


    protected <T extends Fragment> T initFragment(@IdRes int target,
                                                  @NonNull T fragment) {
        return initFragment(target, fragment, null);
    }

    protected <T extends Fragment> T initFragment(@IdRes int target,
                                                  @NonNull T fragment,
                                                  @Nullable Locale locale) {
        return initFragment(target, fragment, locale, null);
    }

    protected <T extends Fragment> T initFragment(@IdRes int target,
                                                  @NonNull T fragment,
                                                  @Nullable Locale locale,
                                                  @Nullable Bundle extras) {
        Bundle args = new Bundle();

        if (extras != null) {
            args.putAll(extras);
        }

        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(target, fragment)
                .commitAllowingStateLoss();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        MyPreferenceFragment myPreferenceFragment = new MyPreferenceFragment();
        initFragment(R.id.content, myPreferenceFragment);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    public static class MyPreferenceFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                Preferences.this.onBackPressed();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
