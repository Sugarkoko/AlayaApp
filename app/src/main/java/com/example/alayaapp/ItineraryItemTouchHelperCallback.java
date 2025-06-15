package com.example.alayaapp;

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
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Only allow dragging if we are in edit mode AND the item is a CardViewHolder
        if (!isEditMode || !(viewHolder instanceof ItineraryAdapter.CardViewHolder)) {
            return 0; // No movement allowed
        }

        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder sourceViewHolder, @NonNull RecyclerView.ViewHolder targetViewHolder) {
        // Ensure the target is also a draggable type before allowing the move
        if (!(targetViewHolder instanceof ItineraryAdapter.CardViewHolder)) {
            return false;
        }
        return mAdapter.onItemMove(sourceViewHolder.getAdapterPosition(), targetViewHolder.getAdapterPosition());
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Not used
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            if (viewHolder != null) {
                viewHolder.itemView.setAlpha(0.8f);
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