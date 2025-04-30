package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.example.alayaapp.databinding.ActivitySplashBinding; // Import ViewBinding class

// Use SuppressLint for Handler leak warning if not using lifecycle-aware components yet
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2500; // 2.5 seconds
    private ActivitySplashBinding binding; // Declare binding variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using ViewBinding
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        // Set the content view from the binding's root
        setContentView(binding.getRoot());
        // Theme is set in Manifest now

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