package com.example.tripexpense;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class MyPlaceAdapter extends ArrayAdapter<MyPlace> {

    public interface PlaceActionListener {
        void onToggleVisited(MyPlace place);
        void onDeletePlace(MyPlace place);
    }

    private final PlaceActionListener listener;

    public MyPlaceAdapter(Context context, List<MyPlace> places, PlaceActionListener listener) {
        super(context, 0, places);
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_my_place, parent, false);
        }

        MyPlace place = getItem(position);
        if (place == null) return convertView;

        TextView tvPlaceName = convertView.findViewById(R.id.tvPlaceName);
        MaterialCardView cardStatusBadge = convertView.findViewById(R.id.cardStatusBadge);
        TextView tvStatusBadge = convertView.findViewById(R.id.tvStatusBadge);
        TextView tvPlaceState = convertView.findViewById(R.id.tvPlaceState);
        TextView tvPlaceCity = convertView.findViewById(R.id.tvPlaceCity);
        TextView tvPlaceVillage = convertView.findViewById(R.id.tvPlaceVillage);
        MaterialButton btnToggleVisited = convertView.findViewById(R.id.btnToggleVisited);
        MaterialButton btnDeletePlace = convertView.findViewById(R.id.btnDeletePlace);

        tvPlaceName.setText(place.getPlaceName());
        tvPlaceState.setText("State: " + formatField(place.getState()));
        tvPlaceCity.setText("City / Town: " + formatField(place.getCity()));
        tvPlaceVillage.setText("Village / Area: " + formatField(place.getVillageOrArea()));

        if (place.isVisited()) {
            cardStatusBadge.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            tvStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
            tvStatusBadge.setText("Visited");
            btnToggleVisited.setText("Move to To-Visit");
            btnToggleVisited.setBackgroundColor(Color.parseColor("#757575"));
        } else {
            cardStatusBadge.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            tvStatusBadge.setTextColor(Color.parseColor("#E65100"));
            tvStatusBadge.setText("To Visit");
            btnToggleVisited.setText("Mark Visited");
            btnToggleVisited.setBackgroundColor(Color.parseColor("#6200EE"));
        }

        btnToggleVisited.setOnClickListener(v -> {
            if (listener != null) listener.onToggleVisited(place);
        });

        btnDeletePlace.setOnClickListener(v -> {
            if (listener != null) listener.onDeletePlace(place);
        });

        return convertView;
    }

    private String formatField(String value) {
        return (value == null || value.trim().isEmpty()) ? "—" : value.trim();
    }
}
