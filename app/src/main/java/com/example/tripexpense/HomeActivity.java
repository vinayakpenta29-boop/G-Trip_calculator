package com.example.tripexpense;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView lvTrips;
    private List<Trip> tripList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        dbHelper = new DatabaseHelper(this);
        lvTrips = findViewById(R.id.lvTrips);

        findViewById(R.id.btnCreateTrip).setOnClickListener(v -> showCreateTripDialog());

        lvTrips.setOnItemClickListener((parent, view, position, id) -> {
            Trip clickedTrip = tripList.get(position);
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            // Pass the Trip ID and Name to MainActivity
            intent.putExtra("TRIP_ID", clickedTrip.getId());
            intent.putExtra("TRIP_NAME", clickedTrip.getName());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTrips();
    }

    private void refreshTrips() {
        tripList = dbHelper.getAllTrips();
        TripAdapter adapter = new TripAdapter(this, tripList);
        lvTrips.setAdapter(adapter);
    }

    private void showCreateTripDialog() {
        EditText input = new EditText(this);
        input.setHint("e.g., Goa Trip");

        new AlertDialog.Builder(this)
            .setTitle("New Trip")
            .setView(input)
            .setPositiveButton("Create", (dialog, which) -> {
                String tripName = input.getText().toString().trim();
                if (!tripName.isEmpty()) {
                    dbHelper.insertTrip(tripName);
                    refreshTrips();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
