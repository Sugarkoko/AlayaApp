package com.example.alayaapp;

import android.graphics.Color; // Import for optional visual feedback

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

// Extend ItemTouchHelper.Callback to handle drag/swipe events
public class ItineraryItemTouchHelperCallback extends ItemTouchHelper.Callback {

    // Need a reference to the adapter to notify it of moves
    private final ItineraryAdapter mAdapter;
    private boolean isEditMode = false; // Track if editing is active

    // Constructor to receive the adapter
    public ItineraryItemTouchHelperCallback(ItineraryAdapter adapter) {
        mAdapter = adapter;
    }

    // Public method for the Activity to enable/disable dragging
    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
    }

    // --- Configuration Methods ---

    @Override
    public boolean isLongPressDragEnabled() {
        // Disable default long press drag because we will use a specific drag handle
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        // Disable swipe functionality for this RecyclerView
        return false;
    }

    // --- Movement Flags ---

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Only allow movement if we are in edit mode
        if (!isEditMode) {
            return 0; // No movement allowed (dragFlags = 0, swipeFlags = 0)
        }

        // Define allowed drag directions (up and down)
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        // Define allowed swipe directions (none in this case)
        final int swipeFlags = 0;

        // Combine flags and return
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    // --- Handling Movement ---

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder sourceViewHolder, @NonNull RecyclerView.ViewHolder targetViewHolder) {
        // This is called when an item is dragged over another item's position.
        // We delegate the actual list update and notification to the adapter.
        // viewHolder.getAdapterPosition() gives the current position during the drag.
        return mAdapter.onItemMove(sourceViewHolder.getAdapterPosition(), targetViewHolder.getAdapterPosition());
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // This method is called when an item is swiped off the screen.
        // Since isItemViewSwipeEnabled() returns false, this will not be called.
    }

    // --- Optional Visual Feedback Callbacks ---

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        // Called when the state of an item changes (e.g., starts being dragged, finishes dragging)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // Item is being actively dragged
            if (viewHolder != null) {
                // Apply visual feedback (e.g., change background, alpha, elevation)
                viewHolder.itemView.setAlpha(0.8f); // Make it slightly transparent
                // viewHolder.itemView.setBackgroundColor(Color.LTGRAY); // Example background change
            }
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Called when the drag is released or cancelled
        super.clearView(recyclerView, viewHolder);
        // Reset any visual changes applied in onSelectedChanged
        viewHolder.itemView.setAlpha(1.0f); // Reset transparency
        // Reset background if you changed it
        // viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT); // Or set back to original
        // viewHolder.itemView.setBackgroundResource(R.drawable.your_original_item_background);
    }
}