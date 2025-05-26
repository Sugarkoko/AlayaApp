package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    BottomNavigationView bottomNavigationView;
    RecyclerView rvPlacesList;
    PlaceAdapter placeAdapter;
    List<Place> placesList; // This list instance is shared with the adapter
    FirebaseFirestore db;

    ProgressBar progressBarHome;
    TextView tvEmptyPlaces;

    final int CURRENT_ITEM_ID = R.id.navigation_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        rvPlacesList = findViewById(R.id.rv_places_list);
        progressBarHome = findViewById(R.id.progressBar_home);
        tvEmptyPlaces = findViewById(R.id.tv_empty_places);

        db = FirebaseFirestore.getInstance();
        placesList = new ArrayList<>(); // Create the list

        rvPlacesList.setLayoutManager(new LinearLayoutManager(this));
        placeAdapter = new PlaceAdapter(this, placesList); // Pass the list reference to the adapter
        rvPlacesList.setAdapter(placeAdapter);

        bottomNavigationView.setSelectedItemId(CURRENT_ITEM_ID);
        fetchPlacesFromFirestore();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == CURRENT_ITEM_ID) {
                return true;
            }
            Intent intent = null;
            if (itemId == R.id.navigation_itineraries) {
                intent = new Intent(getApplicationContext(), ItinerariesActivity.class);
            } else if (itemId == R.id.navigation_map) {
                intent = new Intent(getApplicationContext(), MapsActivity.class);
            } else if (itemId == R.id.navigation_profile) {
                intent = new Intent(getApplicationContext(), ProfileActivity.class);
            }
            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            }
            return false;
        });
    }

    private void fetchPlacesFromFirestore() {
        Log.d(TAG, "fetchPlacesFromFirestore: Method entered. Showing ProgressBar.");
        if (progressBarHome != null) progressBarHome.setVisibility(View.VISIBLE);
        if (rvPlacesList != null) rvPlacesList.setVisibility(View.GONE);
        if (tvEmptyPlaces != null) tvEmptyPlaces.setVisibility(View.GONE);

        db.collection("places")
                .get()
                .addOnCompleteListener(task -> {
                    if (progressBarHome != null) progressBarHome.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            placesList.clear(); // Clear the existing items IN THE SHARED LIST
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    Place place = document.toObject(Place.class);
                                    if (place != null) {
                                        place.setDocumentId(document.getId());
                                        placesList.add(place); // Add to THE SHARED LIST
                                        Log.d(TAG, "Fetched and converted: " + (place.getName() != null ? place.getName() : "Unnamed Place") + " (ID: " + document.getId() + ")");
                                    } else {
                                        Log.e(TAG, "Failed to convert document to Place object (was null): " + document.getId());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting document: " + document.getId() + ". Check POJO and Firestore data types/field names.", e);
                                }
                            }

                            if (!placesList.isEmpty()) {
                                placeAdapter.notifyDataSetChanged(); // <<<<< MODIFIED HERE
                                if (rvPlacesList != null) rvPlacesList.setVisibility(View.VISIBLE);
                                if (tvEmptyPlaces != null) tvEmptyPlaces.setVisibility(View.GONE);
                                Log.d(TAG, "Places loaded and RecyclerView updated. Count: " + placesList.size());
                            } else {
                                Log.d(TAG, "No valid places to display after processing documents. Displaying empty state.");
                                if (rvPlacesList != null) rvPlacesList.setVisibility(View.GONE);
                                if (tvEmptyPlaces != null) {
                                    tvEmptyPlaces.setText("No places to display or error in data.");
                                    tvEmptyPlaces.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            Log.d(TAG, "No places found in Firestore collection. Displaying empty state.");
                            if (rvPlacesList != null) rvPlacesList.setVisibility(View.GONE);
                            if (tvEmptyPlaces != null) {
                                tvEmptyPlaces.setText("No places found nearby.");
                                tvEmptyPlaces.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
                        Log.w(TAG, "Error getting documents from Firestore.", task.getException());
                        if (rvPlacesList != null) rvPlacesList.setVisibility(View.GONE);
                        if (tvEmptyPlaces != null) {
                            tvEmptyPlaces.setText("Failed to load places. Please check connection.");
                            tvEmptyPlaces.setVisibility(View.VISIBLE);
                        }
                        Toast.makeText(HomeActivity.this, "Failed to load places.", Toast.LENGTH_LONG).show();
                        if (task.getException() instanceof FirebaseFirestoreException) {
                            FirebaseFirestoreException e = (FirebaseFirestoreException) task.getException();
                            Log.e(TAG, "Firestore Exception Code: " + e.getCode().name());
                        }
                    }
                });
    }
}