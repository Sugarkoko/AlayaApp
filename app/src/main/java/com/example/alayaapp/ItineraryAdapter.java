package com.example.alayaapp;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
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
    private final OnItemClickListener itemClickListener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ItineraryAdapter(List<ItineraryItem> itineraryList,
                            OnItemClickListener itemClickListener) {
        this.itineraryList = itineraryList;
        this.itemClickListener = itemClickListener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ItineraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItemItineraryBinding binding = ListItemItineraryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ItineraryViewHolder(binding, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ItineraryViewHolder holder, int position) {
        ItineraryItem item = itineraryList.get(position);
        if (item == null) return;

        if (item.getTime() != null) {
            holder.binding.tvItemTime.setText(timeFormat.format(item.getTime().getTime()));
        } else {
            holder.binding.tvItemTime.setText("N/A");
        }
        holder.binding.tvItemActivity.setText(item.getActivity() != null ? item.getActivity() : "Unknown Activity");
        holder.binding.tvItemRating.setText(item.getRating() != null ? item.getRating() : "-");

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
    }

    public void recalculateTimes() {
        if (itineraryList == null || itineraryList.isEmpty()) return;

        Calendar firstTime = null;
        int firstTimeIndex = -1;
        for(int i = 0; i < itineraryList.size(); i++) {
            if(itineraryList.get(i) != null && itineraryList.get(i).getTime() != null) {
                firstTime = (Calendar) itineraryList.get(i).getTime().clone();
                firstTimeIndex = i;
                break;
            }
        }

        if (firstTime == null) {
            if (!itineraryList.isEmpty() && itineraryList.get(0) != null) {
                firstTime = Calendar.getInstance();
                firstTime.set(Calendar.HOUR_OF_DAY, 9); firstTime.set(Calendar.MINUTE, 0); firstTime.set(Calendar.SECOND, 0);
                itineraryList.get(0).setTime((Calendar)firstTime.clone());
                firstTimeIndex = 0;
                notifyItemChanged(0, "payload_time_update");
            } else {
                return;
            }
        }

        Calendar currentTime = (Calendar) firstTime.clone();
        for (int i = firstTimeIndex + 1; i < itineraryList.size(); i++) {
            ItineraryItem currentItem = itineraryList.get(i);
            if (currentItem != null) {
                currentTime.add(Calendar.HOUR_OF_DAY, 1);
                currentItem.setTime((Calendar) currentTime.clone());
                notifyItemChanged(i, "payload_time_update");
            }
        }

        currentTime = (Calendar) firstTime.clone();
        for (int i = firstTimeIndex - 1; i >= 0; i--) {
            ItineraryItem currentItem = itineraryList.get(i);
            if (currentItem != null) {
                currentTime.add(Calendar.HOUR_OF_DAY, -1);
                currentItem.setTime((Calendar) currentTime.clone());
                notifyItemChanged(i, "payload_time_update");
            }
        }
    }

    public List<ItineraryItem> getCurrentList() {
        return itineraryList;
    }

    static class ItineraryViewHolder extends RecyclerView.ViewHolder {
        private final ListItemItineraryBinding binding;

        ItineraryViewHolder(@NonNull ListItemItineraryBinding binding, OnItemClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(position);
                }
            });
        }
    }
}