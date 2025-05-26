package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri; // For "View in Maps"
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast; // For user feedback

import com.bumptech.glide.Glide; // For image loading
// Assuming your XML layout file for this activity is activity_place_details.xml
// If it's still activity_burnham_details.xml, change this back.
import com.example.alayaapp.databinding.ActivityPlaceDetailsBinding; // Make sure this matches your XML file name
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Locale; // For formatting

public class PlaceDetailsActivity extends AppCompatActivity {

    private static final String TAG = "PlaceDetailsActivity";
    // This key MUST match the key used in PlaceAdapter to pass the document ID
    public static final String EXTRA_PLACE_DOCUMENT_ID = "PLACE_DOCUMENT_ID";

    // Make sure ActivityPlaceDetailsBinding matches your XML file name (e.g., activity_place_details.xml)
    private ActivityPlaceDetailsBinding binding;
    private FirebaseFirestore db;
    private Place currentPlace; // To store the fetched place data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using ViewBinding
        binding = ActivityPlaceDetailsBinding.inflate(getLayoutInflater()); // Ensure this binding class is correct
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        String placeDocumentId = getIntent().getStringExtra(EXTRA_PLACE_DOCUMENT_ID);

        if (placeDocumentId != null && !placeDocumentId.isEmpty()) {
            loadPlaceDetails(placeDocumentId);
        } else {
            Log.e(TAG, "No Place Document ID provided!");
            Toast.makeText(this, "Error: Place details not found.", Toast.LENGTH_LONG).show();
            finish();
        }

        binding.ivBackArrow.setOnClickListener(v -> finish());

        binding.tvViewTransportation.setOnClickListener(v -> {
            if (currentPlace != null && currentPlace.getName() != null) {
                Intent intent = new Intent(PlaceDetailsActivity.this, TransportationModeActivity.class);
                intent.putExtra("PARK_NAME", currentPlace.getName());
                if (currentPlace.getLatitude() != null && currentPlace.getLongitude() != null) {
                    intent.putExtra("PARK_LATITUDE", currentPlace.getLatitude());
                    intent.putExtra("PARK_LONGITUDE", currentPlace.getLongitude());
                } else {
                    Log.w(TAG, "Latitude or Longitude is null for " + currentPlace.getName());
                    Toast.makeText(this, "Location coordinates not available.", Toast.LENGTH_SHORT).show();
                }
                startActivity(intent);
            } else {
                Toast.makeText(this, "Place details not loaded yet.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvView.setOnClickListener(v -> {
            if (currentPlace != null && currentPlace.getLatitude() != null && currentPlace.getLongitude() != null) {
                Double lat = currentPlace.getLatitude();
                Double lng = currentPlace.getLongitude();
                String label = currentPlace.getName() != null ? currentPlace.getName() : "Selected Location";

                Uri gmmIntentUri = Uri.parse("geo:" + lat + "," + lng + "?q=" + Uri.encode(lat + "," + lng + "(" + label + ")"));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Uri webIntentUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
                    Intent webMapIntent = new Intent(Intent.ACTION_VIEW, webIntentUri);
                    if (webMapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(webMapIntent);
                    } else {
                        Toast.makeText(this, "No app found to handle map links.", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(this, "Location data not available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPlaceDetails(String documentId) {
        DocumentReference placeRef = db.collection("places").document(documentId);

        placeRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    currentPlace = document.toObject(Place.class);
                    if (currentPlace != null) {
                        currentPlace.setDocumentId(document.getId());
                        populateUI(currentPlace);
                    } else {
                        Log.e(TAG, "Failed to convert document to Place object. Check Place.java and Firestore field names/types.");
                        Toast.makeText(this, "Error: Could not parse place data.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.d(TAG, "No such document with ID: " + documentId);
                    Toast.makeText(this, "Error: Place details not found in database.", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "Firestore get failed with ", task.getException());
                FirebaseFirestoreException e = (FirebaseFirestoreException) task.getException();
                if (e != null && e.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Toast.makeText(this, "Error: Permission denied. Check Firestore rules.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Error: Could not load place details.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void populateUI(Place place) {
        binding.tvParkTitle.setText(place.getName() != null ? place.getName() : "N/A");
        binding.tvRatingValue.setText(String.format(Locale.getDefault(), "%.1f", place.getRating()));

        if (binding.tvReviewCount != null) {
            binding.tvReviewCount.setText(place.getReview_count_text() != null ? place.getReview_count_text() : "(N/A)");
        }

        binding.tvAboutDescription.setText(place.getAbout() != null ? place.getAbout() : "No description available.");
        binding.tvOpenDescription.setText(place.getOpen() != null ? place.getOpen() : "Hours not specified.");

        if (binding.tvBestTimeDescription != null) {
            binding.tvBestTimeDescription.setText(place.getBest_time() != null ? place.getBest_time() : "Not specified.");
        }

        if (binding.tvEntranceFeeDescription != null) {
            if (place.getPrice_range() == 0) {
                binding.tvEntranceFeeDescription.setText("Free");
            } else {
                // *** FIXED LINE HERE ***
                // Use %d for long integers
                binding.tvEntranceFeeDescription.setText(String.format(Locale.getDefault(), "PHP %d", place.getPrice_range()));
            }
        }

        if (place.getImage_url() != null && !place.getImage_url().isEmpty()) {
            Glide.with(this)
                    .load(place.getImage_url())
                    .placeholder(R.drawable.img_placeholder) // Use your generic placeholder
                    .error(R.drawable.img_error)           // Use your generic error image
                    .into(binding.ivParkImage);
        } else {
            binding.ivParkImage.setImageResource(R.drawable.img_placeholder); // Fallback to placeholder
        }
    }
}