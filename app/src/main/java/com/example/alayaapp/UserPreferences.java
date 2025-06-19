package com.example.alayaapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserPreferences {

    private static final String BASE_PREFS_NAME = "AlayaAppPrefs";
    private static final String GUEST_PREFS_SUFFIX = "_guest";


    public static SharedPreferences get(Context context) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String prefsName;

        if (currentUser != null) {
            // User is logged in, create a unique filename using their UID
            prefsName = BASE_PREFS_NAME + "_" + currentUser.getUid();
        } else {
            // User is not logged in, use a generic guest file
            prefsName = BASE_PREFS_NAME + GUEST_PREFS_SUFFIX;
        }

        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }
}