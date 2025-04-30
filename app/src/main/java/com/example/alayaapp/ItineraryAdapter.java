package com.example.alayaapp;

import android.annotation.SuppressLint;
// Removed unused Context import
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
// Removed unused ImageView, TextView imports
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alayaapp.databinding.ListItemItineraryBinding; // Import Item Binding

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ItineraryViewHolder> {

    private List<ItineraryItem> itineraryList;
    private boolean isEditMode = false;
    private final OnStartDragListener dragStartListener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    // Interface to signal drag start to the Activity/Fragment
    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public ItineraryAdapter(List<ItineraryItem> itineraryList, OnStartDragListener dragStartListener) {
        this.itineraryList = itineraryList;
        this.dragStartListener = dragStartListener;
        setHasStableIds(true); // Important for drag/drop performance/correctness
    }

    @NonNull
    @Override
    public ItineraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate using ViewBinding
        ListItemItineraryBinding binding = ListItemItineraryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ItineraryViewHolder(binding); // Pass binding to ViewHolder
    }

    @SuppressLint("ClickableViewAccessibility") // Keep suppression for setOnTouchListener lambda
    @Override
    public void onBindViewHolder(@NonNull ItineraryViewHolder holder, int position) {
        ItineraryItem item = itineraryList.get(position);
        // Bind data using holder's binding object
        holder.binding.tvItemTime.setText(timeFormat.format(item.getTime().getTime()));
        holder.binding.tvItemActivity.setText(item.getActivity());
        holder.binding.tvItemRating.setText(item.getRating());

        // Show/hide drag handle based on edit mode using binding
        holder.binding.ivDragHandle.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        // Start drag on touching the handle *only* in edit mode
        if (isEditMode) {
            holder.binding.ivDragHandle.setOnTouchListener((v, event) -> {
                // Check if the specific event is ACTION_DOWN
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    // Check listener isn't null before calling
                    if (dragStartListener != null) {
                        dragStartListener.onStartDrag(holder);
                    }
                }
                // Return false: Let touch event propagate further if needed by system/parent views
                // Return true: Consume the touch event here (usually not needed for simple drag handle)
                return false;
            });
        } else {
            // Important: Remove the listener when not in edit mode to prevent accidental drags
            // and potential memory leaks if the listener holds references.
            holder.binding.ivDragHandle.setOnTouchListener(null);
        }
    }


    @Override
    public int getItemCount() {
        // Check if list is null before returning size
        return itineraryList != null ? itineraryList.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        // Add bounds checking
        if (itineraryList != null && position >= 0 && position < itineraryList.size()) {
            return itineraryList.get(position).getId();
        }
        // Return a default value or handle error appropriately if out of bounds
        return RecyclerView.NO_ID; // Standard practice for invalid ID
    }

    public void setEditMode(boolean editMode) {
        boolean needsUpdate = isEditMode != editMode; // Check if mode actually changed
        isEditMode = editMode;
        if (needsUpdate) {
            // Use notifyItemRangeChanged for better performance than notifyDataSetChanged()
            // This rebinds existing views without fully recreating them.
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    // Method called by ItemTouchHelper when an item is moved
    public boolean onItemMove(int fromPosition, int toPosition) {
        // Add bounds checking for safety
        if (fromPosition < 0 || fromPosition >= getItemCount() || toPosition < 0 || toPosition >= getItemCount()) {
            return false;
        }

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(itineraryList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(itineraryList, i, i - 1);
            }
        }
        // Notify adapter about the move for animation
        notifyItemMoved(fromPosition, toPosition);

        // IMPORTANT: Recalculate times after moving
        recalculateTimes(); // Call the recalculate method
        return true;
    }

    // Method to update times based on order - ensure list isn't empty
    public void recalculateTimes() {
        if (itineraryList == null || itineraryList.isEmpty()) return;

        // Ensure the first item has a valid time to start from
        if (itineraryList.get(0).getTime() == null) {
            // Handle error: Maybe set a default start time or log an issue
            // For now, let's set a default if null
            Calendar defaultStart = Calendar.getInstance();
            defaultStart.set(Calendar.HOUR_OF_DAY, 9); defaultStart.set(Calendar.MINUTE, 0); defaultStart.set(Calendar.SECOND, 0);
            itineraryList.get(0).setTime(defaultStart);
        }

        Calendar currentTime = (Calendar) itineraryList.get(0).getTime().clone(); // Start with first item's time

        for (int i = 0; i < itineraryList.size(); i++) {
            ItineraryItem currentItem = itineraryList.get(i);

            // Clone calendar to avoid modifying previous item's time reference unintentionally
            Calendar itemTime = (Calendar) currentTime.clone();
            currentItem.setTime(itemTime); // Set the calculated time

            // Notify change for this specific item to update its view *efficiently*
            // Using a payload helps avoid full rebind if only time changed
            notifyItemChanged(i, "payload_time_update");

            // Increment time for the *next* item (e.g., add 1 hour - adjust as needed)
            // Make sure to handle potential nulls if times could be invalid
            if (currentTime != null) {
                currentTime.add(Calendar.HOUR_OF_DAY, 1);
            } else {
                // Handle error - what should happen if current time becomes null?
                break; // Stop recalculating if time becomes invalid
            }
        }
    }


    public List<ItineraryItem> getCurrentList() {
        return itineraryList;
    }

    // ViewHolder Class uses ViewBinding
    static class ItineraryViewHolder extends RecyclerView.ViewHolder {
        // Hold the binding object instead of individual views
        private final ListItemItineraryBinding binding;

        ItineraryViewHolder(@NonNull ListItemItineraryBinding binding) {
            super(binding.getRoot()); // Pass the root view to the superclass
            this.binding = binding;   // Store the binding
        }
    }
}