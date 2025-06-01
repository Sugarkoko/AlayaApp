package com.example.alayaapp;

import android.graphics.Color; // Import for optional visual feedback

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

// Extend ItemTouchHelper.Callback to handle drag/swipe events
public class ItineraryItemTouchHelperCallback extends ItemTouchHelper.Callback {


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



    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Only allow movement if we are in edit mode
        if (!isEditMode) {
            return 0; // No movement allowed (dragFlags = 0, swipeFlags = 0)
        }

        // Define allowed drag directions (up and down)
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;

        final int swipeFlags = 0;

        // Combine flags and return
        return makeMovementFlags(dragFlags, swipeFlags);
    }



    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder sourceViewHolder, @NonNull RecyclerView.ViewHolder targetViewHolder) {

        return mAdapter.onItemMove(sourceViewHolder.getAdapterPosition(), targetViewHolder.getAdapterPosition());
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }



    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {

            if (viewHolder != null) {

                viewHolder.itemView.setAlpha(0.8f); // Make it slightly transparent

            }
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

        super.clearView(recyclerView, viewHolder);

        viewHolder.itemView.setAlpha(1.0f);

    }
}