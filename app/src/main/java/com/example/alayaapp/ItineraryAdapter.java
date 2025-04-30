package com.example.alayaapp;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alayaapp.databinding.ListItemItineraryBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ItineraryViewHolder> {

    private List<ItineraryItem> itineraryList;
    private boolean isEditMode = false;
    private final OnStartDragListener dragStartListener;
    private final OnItemClickListener itemClickListener; // Added click listener
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    // Interface for drag start
    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    // Interface for item click
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    // Updated Constructor
    public ItineraryAdapter(List<ItineraryItem> itineraryList,
                            OnStartDragListener dragStartListener,
                            OnItemClickListener itemClickListener) { // Added click listener
        this.itineraryList = itineraryList;
        this.dragStartListener = dragStartListener;
        this.itemClickListener = itemClickListener; // Store click listener
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ItineraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItemItineraryBinding binding = ListItemItineraryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ItineraryViewHolder(binding, itemClickListener); // Pass click listener to ViewHolder
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ItineraryViewHolder holder, int position) {
        ItineraryItem item = itineraryList.get(position);

        // Check for null before accessing item properties
        if (item == null) return;

        // Bind data using holder's binding object
        if (item.getTime() != null) {
            holder.binding.tvItemTime.setText(timeFormat.format(item.getTime().getTime()));
        } else {
            holder.binding.tvItemTime.setText("N/A"); // Handle null time
        }
        holder.binding.tvItemActivity.setText(item.getActivity() != null ? item.getActivity() : "Unknown Activity");
        holder.binding.tvItemRating.setText(item.getRating() != null ? item.getRating() : "-");

        holder.binding.ivDragHandle.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        if (isEditMode) {
            holder.binding.ivDragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (dragStartListener != null) {
                        dragStartListener.onStartDrag(holder);
                    }
                }
                return false;
            });
        } else {
            holder.binding.ivDragHandle.setOnTouchListener(null);
        }
        // Click listener is set in ViewHolder constructor now
    }


    @Override
    public int getItemCount() {
        return itineraryList != null ? itineraryList.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        if (itineraryList != null && position >= 0 && position < itineraryList.size() && itineraryList.get(position) != null) {
            return itineraryList.get(position).getId();
        }
        return RecyclerView.NO_ID;
    }

    public void setEditMode(boolean editMode) {
        boolean needsUpdate = isEditMode != editMode;
        isEditMode = editMode;
        if (needsUpdate) {
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        if (itineraryList == null || fromPosition < 0 || fromPosition >= itineraryList.size() || toPosition < 0 || toPosition >= itineraryList.size()) {
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
        notifyItemMoved(fromPosition, toPosition);
        recalculateTimes();
        return true;
    }

    public void recalculateTimes() {
        if (itineraryList == null || itineraryList.isEmpty()) return;

        // Find the first item with a non-null time to start calculation
        Calendar firstTime = null;
        int firstTimeIndex = -1;
        for(int i = 0; i < itineraryList.size(); i++) {
            if(itineraryList.get(i) != null && itineraryList.get(i).getTime() != null) {
                firstTime = (Calendar) itineraryList.get(i).getTime().clone();
                firstTimeIndex = i;
                break;
            }
        }

        // If no item has a time, set a default start for the first item
        if (firstTime == null) {
            if (itineraryList.get(0) != null) {
                firstTime = Calendar.getInstance();
                firstTime.set(Calendar.HOUR_OF_DAY, 9); firstTime.set(Calendar.MINUTE, 0); firstTime.set(Calendar.SECOND, 0);
                itineraryList.get(0).setTime((Calendar)firstTime.clone());
                firstTimeIndex = 0;
                notifyItemChanged(0, "payload_time_update"); // Update the first item view
            } else {
                return; // Cannot proceed if first item is null
            }
        }


        Calendar currentTime = (Calendar) firstTime.clone();

        // Calculate times for items *after* the first one with a time
        for (int i = firstTimeIndex + 1; i < itineraryList.size(); i++) {
            ItineraryItem currentItem = itineraryList.get(i);
            if (currentItem != null) {
                currentTime.add(Calendar.HOUR_OF_DAY, 1); // Add 1 hour (adjust as needed)
                currentItem.setTime((Calendar) currentTime.clone());
                notifyItemChanged(i, "payload_time_update");
            }
        }

        // Calculate times for items *before* the first one with a time (if any)
        currentTime = (Calendar) firstTime.clone(); // Reset to the known start time
        for (int i = firstTimeIndex - 1; i >= 0; i--) {
            ItineraryItem currentItem = itineraryList.get(i);
            if (currentItem != null) {
                currentTime.add(Calendar.HOUR_OF_DAY, -1); // Subtract 1 hour
                currentItem.setTime((Calendar) currentTime.clone());
                notifyItemChanged(i, "payload_time_update");
            }
        }
    }

    public List<ItineraryItem> getCurrentList() {
        return itineraryList;
    }

    // ViewHolder Class
    static class ItineraryViewHolder extends RecyclerView.ViewHolder {
        private final ListItemItineraryBinding binding;

        // Updated constructor to accept click listener
        ItineraryViewHolder(@NonNull ListItemItineraryBinding binding, OnItemClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            // Set click listener on the entire item view
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                // Ensure position is valid and listener exists
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(position);
                }
            });
        }
    }
}