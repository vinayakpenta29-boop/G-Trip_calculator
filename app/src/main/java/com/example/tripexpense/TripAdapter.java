package com.example.tripexpense;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class TripAdapter extends ArrayAdapter<Trip> {

    public TripAdapter(Context context, List<Trip> trips) {
        super(context, 0, trips);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Trip trip = getItem(position);
        
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_trip, parent, false);
        }
        
        TextView tvTripName = convertView.findViewById(R.id.tvTripName);
        TextView tvTripMembers = convertView.findViewById(R.id.tvTripMembers);
        TextView tvTripTotal = convertView.findViewById(R.id.tvTripTotal);
        TextView tvBadge = convertView.findViewById(R.id.tvBadge);

        if (trip != null) {
            tvTripName.setText(trip.getName());
            
            if (trip.getMemberNames() != null && !trip.getMemberNames().isEmpty()) {
                tvTripMembers.setText("👤 " + trip.getMemberNames());
            } else {
                tvTripMembers.setText("👤 No members yet");
            }
            
            tvTripTotal.setText(String.format("Total ₹%.2f", trip.getTotalExpense()));

            // Now displays the Member Count
            if (trip.getMemberCount() > 0) {
                tvBadge.setVisibility(View.VISIBLE);
                tvBadge.setText(String.valueOf(trip.getMemberCount()));
            } else {
                tvBadge.setVisibility(View.GONE); // Hides badge if 0 members
            }
        }
        
        return convertView;
    }
}
