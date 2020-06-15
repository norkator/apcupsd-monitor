package com.nitramite.apcupsdmonitor;

import android.os.Bundle;

public class Preferences extends com.fnp.materialpreferences.PreferenceActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyPreferenceFragment myPreferenceFragment = new MyPreferenceFragment();
        setPreferenceFragment(myPreferenceFragment);

        // Activity result back to menu
        setResult(RESULT_OK, null);

        // Exec pending transactions
        myPreferenceFragment.getFragmentManager().executePendingTransactions();
    }

    public static class MyPreferenceFragment extends com.fnp.materialpreferences.PreferenceFragment {
        @Override
        public int addPreferencesFromResource() {
            return R.xml.preferences; // Your preference file
        }
    }


} // End of class