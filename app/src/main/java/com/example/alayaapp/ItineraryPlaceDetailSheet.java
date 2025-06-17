package com.example.alayaapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils; // Import TextUtils for ellipsize
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class ItineraryPlaceDetailSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ItineraryPlaceDetailSheet";
    private static final String ARG_PLACE_DOC_ID = "place_doc_id";
    private static final String ARG_TRIP_DATE_MILLIS = "trip_date_millis";

    private Place currentPlace;

    private ImageView ivPlaceImage;
    private TextView tvPlaceName, tvPlaceCategory, tvPlaceAbout, tvPlaceOpeningHours;
    private LinearLayout llOpeningHoursContainer;
    private Button btnGetDirections, btnViewOnMap;
    private ImageButton btnCloseSheet;

    // NEW: Member variable to track the expanded/collapsed state of the description
    private boolean isDescriptionExpanded = false;

    public static ItineraryPlaceDetailSheet newInstance(String placeDocumentId, long tripDateMillis) {
        ItineraryPlaceDetailSheet fragment = new ItineraryPlaceDetailSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PLACE_DOC_ID, placeDocumentId);
        args.putLong(ARG_TRIP_DATE_MILLIS, tripDateMillis);
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
        llOpeningHoursContainer = view.findViewById(R.id.ll_opening_hours_container);
        tvPlaceOpeningHours = view.findViewById(R.id.tv_place_opening_hours);

        Bundle args = getArguments();
        if (args == null) {
            Toast.makeText(getContext(), "Error: Missing place information.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        final String placeDocId = args.getString(ARG_PLACE_DOC_ID);
        final long tripDateMillis = args.getLong(ARG_TRIP_DATE_MILLIS, 0);

        if (placeDocId == null || placeDocId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Invalid place ID.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        ItinerariesActivity activity = (ItinerariesActivity) getActivity();

        if (activity != null) {
            activity.allPlacesList.stream()
                    .filter(p -> placeDocId.equals(p.getDocumentId()))
                    .findFirst()
                    .ifPresent(place -> {
                        this.currentPlace = place;
                        populateUI(tripDateMillis);
                    });
        }

        if (currentPlace == null) {
            Toast.makeText(getContext(), "Could not load place details.", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        setupClickListeners();
    }

    private void populateUI(long tripDateMillis) {
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

        if (tripDateMillis > 0 && currentPlace.getOpeningHours() != null) {
            Calendar tripCal = Calendar.getInstance();
            tripCal.setTimeInMillis(tripDateMillis);
            String dayOfWeek = tripCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US).toLowerCase();

            Map<String, String> hoursForDay = currentPlace.getOpeningHours().get(dayOfWeek);
            if (hoursForDay != null) {
                String openTime = formatTime(hoursForDay.get("open"));
                String closeTime = formatTime(hoursForDay.get("close"));
                if (openTime != null && closeTime != null) {
                    tvPlaceOpeningHours.setText(String.format("%s - %s", openTime, closeTime));
                    llOpeningHoursContainer.setVisibility(View.VISIBLE);
                } else {
                    llOpeningHoursContainer.setVisibility(View.GONE);
                }
            } else {
                tvPlaceOpeningHours.setText("Closed on this day");
                llOpeningHoursContainer.setVisibility(View.VISIBLE);
            }
        } else {
            llOpeningHoursContainer.setVisibility(View.GONE);
        }
    }

    private String formatTime(String time24h) {
        if (time24h == null || time24h.isEmpty()) return null;
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.US);
            SimpleDateFormat sdf12 = new SimpleDateFormat("h:mm a", Locale.US);
            return sdf12.format(sdf24.parse(time24h));
        } catch (ParseException e) {
            Log.e(TAG, "Could not parse time: " + time24h, e);
            return time24h;
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
                startActivity(mapIntent);
            }
        });

        btnGetDirections.setOnClickListener(v -> {
            if (isPlaceDataValid()) {
                Intent mapIntent = new Intent(getActivity(), MapsActivity.class);
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LATITUDE, currentPlace.getLatitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_LONGITUDE, currentPlace.getLongitude());
                mapIntent.putExtra(MapsActivity.EXTRA_TARGET_NAME, currentPlace.getName());
                mapIntent.putExtra(MapsActivity.EXTRA_DRAW_ROUTE, true);
                startActivity(mapIntent);
            }
        });

        // --- NEWLY ADDED CLICK LISTENER FOR THE DESCRIPTION ---
        tvPlaceAbout.setOnClickListener(v -> {
            // Toggle the boolean flag
            isDescriptionExpanded = !isDescriptionExpanded;

            if (isDescriptionExpanded) {
                // If it's now expanded, show all lines
                tvPlaceAbout.setMaxLines(Integer.MAX_VALUE);
                tvPlaceAbout.setEllipsize(null);
            } else {
                // If it's now collapsed, set max lines back to 3
                tvPlaceAbout.setMaxLines(3);
                tvPlaceAbout.setEllipsize(TextUtils.TruncateAt.END);
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