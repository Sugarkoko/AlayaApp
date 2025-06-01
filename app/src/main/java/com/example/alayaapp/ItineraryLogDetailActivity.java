package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.example.alayaapp.databinding.ActivityItineraryLogDetailBinding;


public class ItineraryLogDetailActivity extends AppCompatActivity {

    private ActivityItineraryLogDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItineraryLogDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        binding.ivBackArrowLogDetail.setOnClickListener(v -> finish());




        binding.bottomNavigationLogDetail.setSelectedItemId(R.id.navigation_profile);

    }
}