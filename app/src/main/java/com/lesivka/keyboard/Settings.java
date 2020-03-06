package com.lesivka.keyboard;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class Settings extends PreferenceFragment {
    public static final String PREF_NAME = "lesivkaPreferences";
    public static final int PREF_MODE = Context.MODE_PRIVATE;
    public static final String AUTO_ACUTE = "auto_acute";
    public static final boolean AUTO_ACUTE_DEFAULT = true;
    public static final String DOUBLE_TAP_DELAY = "double_tap_delay";
    public static final long DOUBLE_TAP_DELAY_DEFAULT = 300_000_000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesName(PREF_NAME);
        pm.setSharedPreferencesMode(PREF_MODE);

        addPreferencesFromResource(R.xml.preferences);
    }

}
