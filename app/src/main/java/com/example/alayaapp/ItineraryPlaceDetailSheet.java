package com.example.alayaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;

public class ItineraryPlaceDetailSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ItineraryPlaceDetailSheet";
    private static final String ARG_PLACE_DOC_ID = "place_doc_id";

    private Place currentPlace;

    private ImageView ivPlaceImage;
    private TextView tvPlaceName, tvPlaceCategory, tvPlaceAbout;
    private Button btnGetDirections, btnViewOnMap;
    private ImageButton btnCloseSheet;

    public static ItineraryPlaceDetailSheet newInstance(String placeDocumentId) {
        ItineraryPlaceDetailSheet fragment = new ItineraryPlaceDetailSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PLACE_DOC_ID, placeDocumentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_itinerary_place_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        ivPlaceImage = view.findViewById(R.id.iv_place_image);
        tvPlaceName = view.findViewById(R.id.tv_place_name);
        tvPlaceCategory = view.findViewById(R.id.tv_place_category);
        tvPlaceAbout = view.findViewById(R.id.tv_place_about);
        btnGetDirections = view.findViewById(R.id.btn_get_directions);
        btnViewOnMap = view.findViewById(R.id.btn_view_on_map);
        btnCloseSheet = view.findViewById(R.id.btn_close_sheet);

        String placeDocId = getArguments() != null ? getArguments().getString(ARG_PLACE_DOC_ID) : null;
        ItinerariesActivity activity = (ItinerariesActivity) getActivity();

        if (activity != null && placeDocId != null) {
            // Find the place from the activity's master list
            activity.allPlacesList.stream()
                    .filter(p -> placeDocId.equals(p.getDocumentId()))
                    .findFirst()
                    .ifPresent(place -> {
                        this.currentPlace = place;
                        populateUI();
                    });
        }

        if (currentPlace == null) {
            Toast.makeText(getContext(), "Could not load place details.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        setupClickListeners();
    }

    private void populateUI() {
        if (currentPlace == null) return;

        tvPlaceName.setText(currentPlace.getName());
        tvPlaceAbout.setText(currentPlace.getAbout());

        if (currentPlace.getCategory() != null && !currentPlace.getCategory().isEmpty()) {
            tvPlaceCategory.setText(currentPlace.getCategory().toUpperCase(Locale.ROOT));
            tvPlaceCategory.setVisibility(View.VISIBLE);
        } else {
            tvPlaceCategory.setVisibility(View.GONE);
        }

        if (currentPlace.getImage_url() != null && !currentPlace.getImage_url().isEmpty()) {
            Glide.with(this)
                    .load(currentPlace.getImage_url())
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_error)
                    .into(ivPlaceImage);
        } else {
            ivPlaceImage.setImageResource(R.drawable.img_placeholder);
        }
    }

    private void setupClickListeners() {
        btnCloseSheet.setOnClickListener(v -> dismiss());

        btnViewOnMap.setOnClickListener(v -> {
            if (isPlaceDataValid()) {
                Intent mapIntent = new Intent(getActivity(), MapsActivity.class);
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LATITUDE, currentPlace.getLatitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LONGITUDE, currentPlace.getLongitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_NAME, currentPlace.getName());
                // Do NOT set EXTRA_DRAW_ROUTE, so it just centers on the pin
                startActivity(mapIntent);
            }
        });

        btnGetDirections.setOnClickListener(v -> {
            if (isPlaceDataValid()) {
                Intent mapIntent = new Intent(getActivity(), MapsActivity.class);
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LATITUDE, currentPlace.getLatitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LONGITUDE, currentPlace.getLongitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_NAME, currentPlace.getName());
                mapIntent.putExtra(MapsActivity.EXTRA_DRAW_ROUTE, true); // Signal to draw a route
                startActivity(mapIntent);
            }
        });
    }

    private boolean isPlaceDataValid() {
        if (currentPlace != null && currentPlace.getLatitude() != null && currentPlace.getLongitude() != null) {
            return true;
        } else {
            Toast.makeText(getContext(), "Location data not available for this place.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Attempted map action, but currentPlace or its coordinates are null.");
            return false;
        }
    }
}