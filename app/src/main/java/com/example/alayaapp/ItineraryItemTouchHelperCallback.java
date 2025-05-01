package com.example.alayaapp;

import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ItineraryItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final ItineraryAdapter mAdapter;
    private boolean isEditMode = false;

    public ItineraryItemTouchHelperCallback(ItineraryAdapter adapter) {
        mAdapter = adapter;
    }

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false; // We use a specific handle
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false; // No swipe
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (!isEditMode) {
            return 0;
        }
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder sourceViewHolder, @NonNull RecyclerView.ViewHolder targetViewHolder) {
        if (!isEditMode) return false; // Only allow moving in edit mode
        // Check bounds before calling adapter method
        int fromPosition = sourceViewHolder.getAdapterPosition();
        int toPosition = targetViewHolder.getAdapterPosition();
        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
            return false;
        }
        return mAdapter.onItemMove(fromPosition, toPosition);
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Not used
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isEditMode) {
            if (viewHolder != null) {
                viewHolder.itemView.setAlpha(0.8f);
                // Optional: Add elevation or background change for drag visual
                // viewHolder.itemView.setBackgroundColor(Color.LTGRAY);
                viewHolder.itemView.setElevation(8f); // Example elevation
            }
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setAlpha(1.0f);
        // Reset visual changes
        // viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT); // Or restore original if needed
        viewHolder.itemView.setElevation(0f); // Reset elevation

        // Important: After dropping, ensure the background is correctly set based on edit mode
        // This might require access to the adapter's isEditMode state or passing it somehow,
        // but often the adapter's subsequent bind call handles this.
        // If flickering occurs, you might need to explicitly reset background here too.
    }
}