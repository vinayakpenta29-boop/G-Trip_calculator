package com.example.tripexpense;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class UserManager {
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_USER_ID = "device_user_id";

    // Generates and remembers a unique ID for this specific phone
    public static String getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String id = prefs.getString(KEY_USER_ID, null);
        
        if (id == null) {
            id = UUID.randomUUID().toString(); // Creates a random string like "550e8400-e29b..."
            prefs.edit().putString(KEY_USER_ID, id).apply();
        }
        return id;
    }
}
