package com.example.alayaapp;

import androidx.appcompat.app.AlertDialog;
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

// Implement the new listener interface
public class TripHistoryActivity extends AppCompatActivity implements TripHistoryAdapter.OnTripInteractionListener {
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
        // Pass 'this' as the listener when creating the adapter
        adapter = new TripHistoryAdapter(this, tripList, this);
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

    // --- NEW METHODS FOR DELETION ---

    @Override
    public void onTripLongPressed(Trip trip, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Trip")
                .setMessage("Are you sure you want to permanently delete your '" + trip.getTripTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteTripFromFirestore(trip, position);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteTripFromFirestore(Trip trip, int position) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || trip.getDocumentId() == null || trip.getDocumentId().isEmpty()) {
            Toast.makeText(this, "Error: Could not delete trip.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .collection("tripHistory").document(trip.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // On success, update the UI
                    tripList.remove(position);
                    adapter.notifyItemRemoved(position);
                    // This is important to fix headers if the first item of a month is deleted
                    adapter.notifyItemRangeChanged(position, tripList.size());

                    Toast.makeText(this, "Trip deleted successfully.", Toast.LENGTH_SHORT).show();

                    // Check if the list is now empty
                    if (tripList.isEmpty()) {
                        binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
                        binding.rvTripHistory.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete trip. Please try again.", Toast.LENGTH_SHORT).show();
                    Log.e("TripHistoryActivity", "Error deleting trip", e);
                });
    }
}