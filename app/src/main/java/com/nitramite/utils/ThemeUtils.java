package com.nitramite.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nitramite.apcupsdmonitor.Constants;

import static android.content.res.Configuration.UI_MODE_NIGHT_YES;


public class ThemeUtils {

    @SuppressWarnings("HardCodedStringLiteral")
    public enum Theme {
        BASIC(1),
        DARK(2),
        AUTO(3);

        private int num;

        Theme(int num) {
            this.num = num;
        }

        public static Theme getCurrentTheme(Context context) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int theme = preferences.getBoolean(Constants.SP_USE_DARK_THEME, false) ? 2 : 1;
            for (Theme b : Theme.values()) {
                if (b.num == theme) {

                    return b;
                }
            }
            return BASIC;
        }

        public static boolean isDarkThemeForced(Context context) {
            Theme currentTheme = getCurrentTheme(context);
            return currentTheme == DARK;
        }

        public static boolean isDarkTheme(Context context) {
            Theme currentTheme = getCurrentTheme(context);
            int nightModeFlags =
                    context.getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;
            Log.i("TU", "Nightmode: " + nightModeFlags);
            return (nightModeFlags == UI_MODE_NIGHT_YES) || currentTheme == DARK;
        }

        public static boolean isAutoTheme(Context context) {
            Theme currentTheme = getCurrentTheme(context);
            return currentTheme == AUTO;
        }
    }

}
