package com.example.alayaapp;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.alayaapp.databinding.DialogEditItineraryTimeBinding;
import com.google.firebase.firestore.GeoPoint;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EditItineraryTimeDialog extends DialogFragment {

    public interface EditTimeDialogListener {
        void onTimeConfirmed(int itemIndex, Calendar newStartTime, Calendar newEndTime);
    }

    private static final String ARG_ITEM_INDEX = "item_index";
    private static final String ARG_TRIP_START_MILLIS = "trip_start_millis";
    private static final String ARG_TRIP_END_MILLIS = "trip_end_millis";

    private DialogEditItineraryTimeBinding binding;
    private EditTimeDialogListener listener;

    private int itemIndex;
    private ItineraryItem currentItem;
    private List<ItineraryItem> allItems;
    private Place placeDetails;

    private Calendar tripStartCal;
    private Calendar tripEndCal;
    private Calendar itemStartCal;
    private Calendar itemEndCal;

    private Calendar minValidTime;
    private Calendar maxValidTime;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    public static EditItineraryTimeDialog newInstance(int itemIndex, long tripStartMillis, long tripEndMillis) {
        EditItineraryTimeDialog fragment = new EditItineraryTimeDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_ITEM_INDEX, itemIndex);
        args.putLong(ARG_TRIP_START_MILLIS, tripStartMillis);
        args.putLong(ARG_TRIP_END_MILLIS, tripEndMillis);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogEditItineraryTimeBinding.inflate(LayoutInflater.from(getContext()));
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot());

        try {
            listener = (EditTimeDialogListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement EditTimeDialogListener");
        }

        unpackArguments();
        findPlaceDetails();
        calculateValidTimeWindow();
        setupViews();
        setupClickListeners();

        return builder.create();
    }

    private void unpackArguments() {
        Bundle args = getArguments();
        if (args == null) {
            dismiss();
            return;
        }
        itemIndex = args.getInt(ARG_ITEM_INDEX);

        ItinerariesActivity activity = (ItinerariesActivity) getActivity();
        if (activity == null || activity.itineraryViewModel.itineraryState.getValue() == null) {
            dismiss();
            return;
        }
        allItems = activity.itineraryViewModel.itineraryState.getValue().getItineraryItems();
        currentItem = allItems.get(itemIndex);

        tripStartCal = Calendar.getInstance();
        tripStartCal.setTimeInMillis(args.getLong(ARG_TRIP_START_MILLIS));
        tripEndCal = Calendar.getInstance();
        tripEndCal.setTimeInMillis(args.getLong(ARG_TRIP_END_MILLIS));

        itemStartCal = (Calendar) currentItem.getStartTime().clone();
        itemEndCal = (Calendar) currentItem.getEndTime().clone();
    }

    private void findPlaceDetails() {
        ItinerariesActivity activity = (ItinerariesActivity) getActivity();
        if (activity == null) return;

        placeDetails = activity.allPlacesList.stream()
                .filter(p -> p.getDocumentId().equals(currentItem.getPlaceDocumentId()))
                .findFirst()
                .orElse(null);
    }

    private void calculateValidTimeWindow() {
        // 1. Absolute min/max from trip schedule
        minValidTime = (Calendar) tripStartCal.clone();
        maxValidTime = (Calendar) tripEndCal.clone();

        // 2. Factor in place opening hours
        if (placeDetails != null && placeDetails.getOpeningHours() != null) {
            String dayOfWeek = tripStartCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US).toLowerCase();
            Map<String, String> hours = placeDetails.getOpeningHours().get(dayOfWeek);
            if (hours != null) {
                try {
                    // --- FIX START ---
                    // This logic correctly applies the parsed time to the trip's date.
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
                    Calendar parsedCal = Calendar.getInstance();

                    // Handle Opening Time
                    Date openParsedTime = sdf.parse(hours.get("open"));
                    if (openParsedTime != null) {
                        parsedCal.setTime(openParsedTime);
                        Calendar placeOpen = (Calendar) tripStartCal.clone();
                        placeOpen.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY));
                        placeOpen.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE));
                        if (placeOpen.after(minValidTime)) {
                            minValidTime = placeOpen;
                        }
                    }

                    // Handle Closing Time
                    Date closeParsedTime = sdf.parse(hours.get("close"));
                    if (closeParsedTime != null) {
                        parsedCal.setTime(closeParsedTime);
                        Calendar placeClose = (Calendar) tripStartCal.clone();
                        placeClose.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY));
                        placeClose.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE));
                        if (placeClose.before(maxValidTime)) {
                            maxValidTime = placeClose;
                        }
                    }
                    // --- FIX END ---
                } catch (ParseException | NullPointerException e) {
                    Log.e("EditTimeDialog", "Error parsing place opening/closing hours", e);
                }
            }
        }

        // 3. Factor in adjacent stops (this is a simplified constraint for the dialog)
        if (itemIndex > 0) {
            Calendar prevItemEnd = allItems.get(itemIndex - 1).getEndTime();
            if (prevItemEnd.after(minValidTime)) {
                minValidTime = (Calendar) prevItemEnd.clone();
            }
        }
        if (itemIndex < allItems.size() - 1) {
            Calendar nextItemStart = allItems.get(itemIndex + 1).getStartTime();
            if (nextItemStart.before(maxValidTime)) {
                maxValidTime = (Calendar) nextItemStart.clone();
            }
        }
    }

    private void setupViews() {
        binding.tvDialogPlaceName.setText(currentItem.getActivity());
        binding.tvValidTimeRangeInfo.setText(String.format(Locale.getDefault(),
                "This stop must be between %s and %s.",
                timeFormat.format(minValidTime.getTime()),
                timeFormat.format(maxValidTime.getTime())));

        updateButtonTimes();
    }

    private void setupClickListeners() {
        binding.btnSelectStartTime.setOnClickListener(v -> showTimePicker(true));
        binding.btnSelectEndTime.setOnClickListener(v -> showTimePicker(false));
        binding.btnDialogCancel.setOnClickListener(v -> dismiss());
        binding.btnDialogConfirm.setOnClickListener(v -> onConfirm());
    }

    private void showTimePicker(boolean isStartTime) {
        Calendar calToShow = isStartTime ? itemStartCal : itemEndCal;
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minute) -> {
                    if (isStartTime) {
                        itemStartCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        itemStartCal.set(Calendar.MINUTE, minute);
                    } else {
                        itemEndCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        itemEndCal.set(Calendar.MINUTE, minute);
                    }
                    updateButtonTimes();
                },
                calToShow.get(Calendar.HOUR_OF_DAY),
                calToShow.get(Calendar.MINUTE),
                false // 12-hour format with AM/PM
        );
        timePickerDialog.show();
    }

    private void updateButtonTimes() {
        binding.btnSelectStartTime.setText(timeFormat.format(itemStartCal.getTime()));
        binding.btnSelectEndTime.setText(timeFormat.format(itemEndCal.getTime()));
    }

    private void onConfirm() {
        // Validation
        if (itemStartCal.after(itemEndCal)) {
            Toast.makeText(getContext(), "Start time must be before end time.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Add a 1-minute tolerance to handle edge cases with the picker
        Calendar minValidWithTolerance = (Calendar) minValidTime.clone();
        minValidWithTolerance.add(Calendar.MINUTE, -1);

        Calendar maxValidWithTolerance = (Calendar) maxValidTime.clone();
        maxValidWithTolerance.add(Calendar.MINUTE, 1);

        if (itemStartCal.before(minValidWithTolerance)) {
            Toast.makeText(getContext(), "Start time is too early. Must be after " + timeFormat.format(minValidTime.getTime()), Toast.LENGTH_LONG).show();
            return;
        }
        if (itemEndCal.after(maxValidWithTolerance)) {
            Toast.makeText(getContext(), "End time is too late. Must be before " + timeFormat.format(maxValidTime.getTime()), Toast.LENGTH_LONG).show();
            return;
        }

        // If valid, call listener
        listener.onTimeConfirmed(itemIndex, itemStartCal, itemEndCal);
        dismiss();
    }
}