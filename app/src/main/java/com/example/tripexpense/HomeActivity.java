package com.example.tripexpense;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String myUserId;
    private ListView lvTrips;
    private List<Trip> tripList;
    private TripAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        myUserId = UserManager.getUserId(this); // Get the device's unique ID
        
        lvTrips = findViewById(R.id.lvTrips);
        tripList = new ArrayList<>();
        adapter = new TripAdapter(this, tripList);
        lvTrips.setAdapter(adapter);

        findViewById(R.id.btnCreateTrip).setOnClickListener(v -> showCreateTripDialog());
        findViewById(R.id.btnJoinTrip).setOnClickListener(v -> showJoinTripDialog());

        lvTrips.setOnItemClickListener((parent, view, position, id) -> {
            Trip clickedTrip = tripList.get(position);
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.putExtra("TRIP_ID", clickedTrip.getId());
            intent.putExtra("TRIP_NAME", clickedTrip.getName());
            intent.putExtra("ADMIN_ID", clickedTrip.getAdminId()); // Pass Admin ID to check permissions later
            intent.putExtra("SHARE_CODE", clickedTrip.getShareCode()); 
            startActivity(intent);
        });

        listenForMyTrips();
    }

    // REALTIME CLOUD LISTENER
    private void listenForMyTrips() {
        // Only fetch trips where the userIds array contains MY user ID
        db.collection("trips")
          .whereArrayContains("userIds", myUserId)
          .addSnapshotListener((value, error) -> {
              if (error != null) {
                  Toast.makeText(this, "Error loading trips", Toast.LENGTH_SHORT).show();
                  return;
              }
              if (value != null) {
                  tripList.clear();
                  for (QueryDocumentSnapshot doc : value) {
                      Trip trip = doc.toObject(Trip.class);
                      tripList.add(trip);
                  }
                  adapter.notifyDataSetChanged();
              }
          });
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
                    createTripInCloud(tripName);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void createTripInCloud(String name) {
        // Generate a random 8-character hex code for sharing (e.g. "f445a7f8")
        String shareCode = UUID.randomUUID().toString().substring(0, 8);
        String tripId = db.collection("trips").document().getId(); // Auto-generate Firebase ID

        Trip newTrip = new Trip(tripId, name, shareCode, myUserId);

        db.collection("trips").document(tripId)
          .set(newTrip)
          .addOnSuccessListener(aVoid -> Toast.makeText(this, "Trip Created!", Toast.LENGTH_SHORT).show())
          .addOnFailureListener(e -> Toast.makeText(this, "Failed to create trip", Toast.LENGTH_SHORT).show());
    }

    private void showJoinTripDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter 8-character Share Code");

        new AlertDialog.Builder(this)
            .setTitle("Join Trip")
            .setView(input)
            .setPositiveButton("Join", (dialog, which) -> {
                String code = input.getText().toString().trim();
                if (!code.isEmpty()) {
                    joinTripWithCode(code);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void joinTripWithCode(String code) {
        // Search the cloud for a trip with this exact share code
        db.collection("trips")
          .whereEqualTo("shareCode", code)
          .get()
          .addOnSuccessListener(queryDocumentSnapshots -> {
              if (!queryDocumentSnapshots.isEmpty()) {
                  // Trip found! Get the document ID
                  String tripId = queryDocumentSnapshots.getDocuments().get(0).getId();
                  
                  // Add myUserId to the userIds array in the cloud
                  db.collection("trips").document(tripId)
                    .update("userIds", com.google.firebase.firestore.FieldValue.arrayUnion(myUserId))
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Successfully Joined!", Toast.LENGTH_SHORT).show());
              } else {
                  Toast.makeText(this, "Invalid Share Code", Toast.LENGTH_SHORT).show();
              }
          })
          .addOnFailureListener(e -> Toast.makeText(this, "Error finding trip", Toast.LENGTH_SHORT).show());
    }
}
