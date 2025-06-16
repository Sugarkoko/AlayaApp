package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.example.alayaapp.databinding.ActivityTripHistoryBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class TripHistoryActivity extends AppCompatActivity {

    private ActivityTripHistoryBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TripHistoryAdapter adapter;
    private List<Trip> tripList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTripHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();

        binding.ivBackArrowTripHistory.setOnClickListener(v -> finish());
        binding.bottomNavigationTripHistory.setSelectedItemId(R.id.navigation_profile);

        binding.bottomNavigationTripHistory.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                Intent intent = new Intent(this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            }
            // Add other navigation cases if needed
            return false;
        });

        loadTripHistory();
    }

    private void setupRecyclerView() {
        tripList = new ArrayList<>();
        adapter = new TripHistoryAdapter(this, tripList);
        binding.rvTripHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTripHistory.setAdapter(adapter);
    }

    private void loadTripHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            binding.tvNoHistoryMessage.setText("Please sign in to view your trip history.");
            binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
            binding.rvTripHistory.setVisibility(View.GONE);
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .collection("tripHistory")
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        tripList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Trip trip = document.toObject(Trip.class);
                            trip.setDocumentId(document.getId());
                            tripList.add(trip);
                        }

                        if (tripList.isEmpty()) {
                            binding.tvNoHistoryMessage.setText("You haven't saved any trips yet.");
                            binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
                            binding.rvTripHistory.setVisibility(View.GONE);
                        } else {
                            binding.tvNoHistoryMessage.setVisibility(View.GONE);
                            binding.rvTripHistory.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.w("TripHistoryActivity", "Error getting documents.", task.getException());
                        Toast.makeText(this, "Failed to load trip history.", Toast.LENGTH_SHORT).show();
                        binding.tvNoHistoryMessage.setText("Could not load trip history.");
                        binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
                    }
                });
    }
}