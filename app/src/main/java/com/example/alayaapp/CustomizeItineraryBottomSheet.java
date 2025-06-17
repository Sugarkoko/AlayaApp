package com.example.alayaapp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CustomizeItineraryBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "CustomizeItineraryBottomSheet";

    // Interface to send data back to the activity
    public interface CustomizeListener {
        void onCustomizeApplied(List<String> categoryPreferences);
    }

    private CustomizeListener listener;
    private Slider stopsSlider;
    private TextView stopCountTextView;
    private LinearLayout spinnersContainer;
    private Button applyButton;

    // Pre-defined categories for the dropdowns
    private final List<String> categories = new ArrayList<>(Arrays.asList(
            "Any", "Tourist Spot", "Food", "Shopping", "Park", "Museum", "Cafe"
    ));

    public static CustomizeItineraryBottomSheet newInstance() {
        return new CustomizeItineraryBottomSheet();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (CustomizeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement CustomizeListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_customize_itinerary, container, false);

        stopsSlider = view.findViewById(R.id.slider_stops);
        stopCountTextView = view.findViewById(R.id.tv_stop_count);
        spinnersContainer = view.findViewById(R.id.container_category_spinners);
        applyButton = view.findViewById(R.id.btn_apply_and_generate);

        setupSlider();
        setupApplyButton();

        // Initial setup
        updateStopCountText(Math.round(stopsSlider.getValue()));
        updateSpinners(Math.round(stopsSlider.getValue()));

        return view;
    }

    private void setupSlider() {
        stopsSlider.addOnChangeListener((slider, value, fromUser) -> {
            int numStops = Math.round(value);
            updateStopCountText(numStops);
            updateSpinners(numStops);
        });
    }

    private void updateStopCountText(int count) {
        stopCountTextView.setText(String.format(Locale.getDefault(), "%d Stops", count));
    }

    private void updateSpinners(int count) {
        spinnersContainer.removeAllViews(); // Clear existing spinners

        LayoutInflater inflater = LayoutInflater.from(getContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, categories);

        for (int i = 1; i <= count; i++) {
            // Inflate the reusable row layout
            TextInputLayout textInputLayout = (TextInputLayout) inflater.inflate(R.layout.item_customize_spinner_row, spinnersContainer, false);
            AutoCompleteTextView autoCompleteTextView = textInputLayout.findViewById(R.id.actv_category_spinner);

            textInputLayout.setHint("Stop " + i);
            autoCompleteTextView.setAdapter(adapter);
            autoCompleteTextView.setText(categories.get(0), false); // Default to "Any"

            spinnersContainer.addView(textInputLayout);
        }
    }

    private void setupApplyButton() {
        applyButton.setOnClickListener(v -> {
            List<String> preferences = new ArrayList<>();
            for (int i = 0; i < spinnersContainer.getChildCount(); i++) {
                View child = spinnersContainer.getChildAt(i);
                if (child instanceof TextInputLayout) {
                    AutoCompleteTextView actv = ((TextInputLayout) child).findViewById(R.id.actv_category_spinner);
                    preferences.add(actv.getText().toString());
                }
            }
            listener.onCustomizeApplied(preferences);
            dismiss();
        });
    }
}