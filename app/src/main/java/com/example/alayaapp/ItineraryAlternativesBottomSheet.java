package com.example.alayaapp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.GeoPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ItineraryAlternativesBottomSheet extends BottomSheetDialogFragment implements ItineraryAlternativesAdapter.OnAlternativeClickListener {
    public static final String TAG = "ItineraryAlternativesBottomSheet";
    private static final String ARG_ITEM_TO_REPLACE_INDEX = "item_to_replace_index";

    private RecyclerView rvAlternatives;
    private TextView tvTitle, tvSubtitle, tvNoAlternatives;
    private ItineraryAlternativesAdapter adapter;
    private ItineraryViewModel viewModel;
    private int itemIndexToReplace;
    private ItineraryItem itemToReplace;
    private List<Place> allPlaces;
    private List<ItineraryItem> currentItinerary;

    // Views for the new choice buttons
    private LinearLayout llChoiceContainer;
    private Button btnShowBest, btnShowAll;

    public interface AlternativesListener {
        void onPlaceSelectedForReplacement(int indexToReplace, Place newPlace);
    }

    private AlternativesListener listener;

    public static ItineraryAlternativesBottomSheet newInstance(int indexToReplace) {
        ItineraryAlternativesBottomSheet fragment = new ItineraryAlternativesBottomSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_ITEM_TO_REPLACE_INDEX, indexToReplace);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (AlternativesListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement AlternativesListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_itinerary_alternatives, container, false);

        // Find all views
        rvAlternatives = view.findViewById(R.id.rv_alternatives);
        tvTitle = view.findViewById(R.id.tv_alternatives_title);
        tvSubtitle = view.findViewById(R.id.tv_alternatives_subtitle);
        tvNoAlternatives = view.findViewById(R.id.tv_no_alternatives);
        llChoiceContainer = view.findViewById(R.id.ll_choice_container);
        btnShowBest = view.findViewById(R.id.btn_show_best_alternatives);
        btnShowAll = view.findViewById(R.id.btn_show_all_places);

        if (getArguments() != null) {
            itemIndexToReplace = getArguments().getInt(ARG_ITEM_TO_REPLACE_INDEX);
        }

        viewModel = ((ItinerariesActivity) requireActivity()).itineraryViewModel;
        ItineraryState currentState = viewModel.itineraryState.getValue();
        if (currentState == null || currentState.getItineraryItems() == null || currentState.getItineraryItems().isEmpty()) {
            dismiss();
            return view;
        }

        currentItinerary = currentState.getItineraryItems();
        itemToReplace = currentItinerary.get(itemIndexToReplace);
        allPlaces = ((ItinerariesActivity) requireActivity()).allPlacesList;

        tvTitle.setText("Replace " + itemToReplace.getActivity());

        // Set click listeners for the embedded buttons
        btnShowBest.setOnClickListener(v -> {
            llChoiceContainer.setVisibility(View.GONE);
            tvSubtitle.setVisibility(View.VISIBLE);
            displayAlternatives(true);
        });
        btnShowAll.setOnClickListener(v -> {
            llChoiceContainer.setVisibility(View.GONE);
            tvSubtitle.setVisibility(View.VISIBLE);
            displayAlternatives(false);
        });

        return view;
    }



    private void displayAlternatives(boolean findBest) {
        // MODIFIED: Set the subtitle text here
        if (findBest) {
            tvSubtitle.setText("Showing nearby places in the same category.");
        } else {
            tvSubtitle.setText("Showing all available places, sorted by category.");
        }

        List<String> existingPlaceIds = currentItinerary.stream()
                .map(ItineraryItem::getPlaceDocumentId)
                .collect(Collectors.toList());

        List<Place> potentialReplacements = allPlaces.stream()
                .filter(p -> !existingPlaceIds.contains(p.getDocumentId()))
                .collect(Collectors.toList());

        List<Place> finalDisplayList;
        if (findBest) {
            finalDisplayList = potentialReplacements.stream()
                    .filter(p -> itemToReplace.getCategory().equalsIgnoreCase(p.getCategory()))
                    .collect(Collectors.toList());
            GeoPoint originalLocation = itemToReplace.getCoordinates();
            for (Place p : finalDisplayList) {
                p.setDistance(calculateDistance(originalLocation, p.getCoordinates()));
            }
            Collections.sort(finalDisplayList, Comparator.comparingDouble(Place::getDistance));
        } else {
            finalDisplayList = new ArrayList<>(potentialReplacements);
            for (Place p : finalDisplayList) {
                p.setDistance(-1);
            }
            Collections.sort(finalDisplayList, Comparator.comparing(Place::getCategory, Comparator.nullsLast(String::compareTo)).thenComparing(Place::getName));
        }

        if (finalDisplayList.isEmpty()) {
            rvAlternatives.setVisibility(View.GONE);
            tvNoAlternatives.setVisibility(View.VISIBLE);
        } else {
            rvAlternatives.setVisibility(View.VISIBLE);
            tvNoAlternatives.setVisibility(View.GONE);
            adapter = new ItineraryAlternativesAdapter(getContext(), finalDisplayList, this);
            rvAlternatives.setAdapter(adapter);
        }
    }

    @Override
    public void onAlternativeClick(Place selectedPlace) {
        listener.onPlaceSelectedForReplacement(itemIndexToReplace, selectedPlace);
        dismiss();
    }

    private double calculateDistance(GeoPoint start, GeoPoint end) {
        if (start == null || end == null) return Double.MAX_VALUE;
        final int R = 6371;
        double latDistance = Math.toRadians(end.getLatitude() - start.getLatitude());
        double lonDistance = Math.toRadians(end.getLongitude() - start.getLongitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}