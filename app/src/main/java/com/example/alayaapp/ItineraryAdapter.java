package com.example.alayaapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_itinerary, parent, false);
        return new ItineraryViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ItineraryViewHolder holder, int position) {
        ItineraryItem item = itineraryList.get(position);
        holder.tvTime.setText(timeFormat.format(item.getTime().getTime()));
        holder.tvActivity.setText(item.getActivity());
        holder.tvRating.setText(item.getRating());

        // Show/hide drag handle based on edit mode
        holder.ivDragHandle.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        // Start drag on touching the handle *only* in edit mode
        if (isEditMode) {
            holder.ivDragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    dragStartListener.onStartDrag(holder);
                }
                return false; // Let the touch event propagate if needed
            });
        } else {
            holder.ivDragHandle.setOnTouchListener(null); // Remove listener when not editing
        }
    }

    @Override
    public int getItemCount() {
        return itineraryList.size();
    }

    @Override
    public long getItemId(int position) {
        // Return a unique and stable ID for the item
        return itineraryList.get(position).getId();
    }

    public void setEditMode(boolean editMode) {
        boolean needsUpdate = isEditMode != editMode; // Check if mode actually changed
        isEditMode = editMode;
        if (needsUpdate) {
            // Use notifyItemRangeChanged instead of notifyDataSetChanged()
            // This rebinds existing views without fully recreating them.
            notifyItemRangeChanged(0, getItemCount());
        }
    }
    // Method called by ItemTouchHelper when an item is moved
    public boolean onItemMove(int fromPosition, int toPosition) {
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
        // IMPORTANT: Recalculate times after moving
        recalculateTimes();
        return true;
    }

    // Method to update times based on order
    public void recalculateTimes() {
        if (itineraryList.isEmpty()) return;

        Calendar currentTime = (Calendar) itineraryList.get(0).getTime().clone(); // Start with first item's time or a fixed start time
        // Or set a fixed start time:
        // Calendar currentTime = Calendar.getInstance();
        // currentTime.set(Calendar.HOUR_OF_DAY, 9);
        // currentTime.set(Calendar.MINUTE, 0);
        // currentTime.set(Calendar.SECOND, 0);
        // itineraryList.get(0).setTime(currentTime); // Set first item's time

        for (int i = 0; i < itineraryList.size(); i++) {
            ItineraryItem currentItem = itineraryList.get(i);
            // Clone calendar to avoid modifying previous item's time reference
            Calendar itemTime = (Calendar) currentTime.clone();
            currentItem.setTime(itemTime);

            // Notify change for this specific item to update view
            notifyItemChanged(i, "payload_time_update"); // Use payload to avoid full rebind if possible

            // Increment time for the *next* item (e.g., add 1 hour)
            currentTime.add(Calendar.HOUR_OF_DAY, 1); // Adjust interval as needed
        }
        // No need to call notifyDataSetChanged() if using notifyItemChanged()
    }

    public List<ItineraryItem> getCurrentList() {
        return itineraryList;
    }


    // ViewHolder Class
    static class ItineraryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDragHandle;
        TextView tvTime, tvActivity, tvRating;

        ItineraryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            tvTime = itemView.findViewById(R.id.tv_item_time);
            tvActivity = itemView.findViewById(R.id.tv_item_activity);
            tvRating = itemView.findViewById(R.id.tv_item_rating);
        }
    }
}