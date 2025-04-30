package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

// Use SuppressLint for Handler leak warning if not using lifecycle-aware components yet
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2500; // 2.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Theme is set in Manifest now
        setContentView(R.layout.activity_splash);

        // Handler to wait for SPLASH_DELAY milliseconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Intent to start LoginActivity
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);

            // Finish SplashActivity so user can't navigate back to it
            finish();
        }, SPLASH_DELAY);
    }
}