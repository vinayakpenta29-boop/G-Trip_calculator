package com.example.tripexpense;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


import androidx.activity.result.ActivityResultLauncher;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

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

        // 1. The QR Scanner Launcher
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
        new ScanContract(),
        result -> {
            if (result.getContents() != null) {
                // If a QR code was successfully scanned, join the trip automatically!
                String scannedCode = result.getContents().trim().toLowerCase();
                joinTripWithCode(scannedCode);
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            }
        });

    // 2. Updated Join Dialog
    private void showJoinTripDialog() {
        // 1. Inflate the beautiful new layout
        View view = getLayoutInflater().inflate(R.layout.dialog_join_trip, null);
        EditText input = view.findViewById(R.id.etShareCode);

        // 2. Use the Material Builder instead of the old AlertDialog
        new MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton("Join", (dialog, which) -> {
                String code = input.getText().toString().trim().toLowerCase();
                if (!code.isEmpty()) {
                    joinTripWithCode(code);
                }
            })
            .setNeutralButton("Scan QR", (dialog, which) -> {
                ScanOptions options = new ScanOptions();
                options.setPrompt("Scan a Trip QR Code");
                options.setBeepEnabled(true);
                options.setOrientationLocked(false);
                options.setCaptureActivity(CaptureAct.class);
                barcodeLauncher.launch(options);
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

    // 3-DOTS MENU & DELETE TRIP LOGIC
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_trip) {
            showDeleteTripSelectionDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteTripSelectionDialog() {
        String currentUserId = myUserId;

        List<Trip> adminTrips = new ArrayList<>();
        List<String> tripNames = new ArrayList<>();

        // 1. Filter the list: Only keep trips where this user is the Admin
        for (Trip trip : tripList) {
            // NOTE: If your getter in Trip.java is named differently (like getCreatedBy()), 
            // change 'getAdminId()' below to match your exact code!
            if (trip.getAdminId() != null && trip.getAdminId().equals(currentUserId)) {
                adminTrips.add(trip);
                tripNames.add(trip.getName());
            }
        }

        // 2. If they aren't admin of anything, show a toast and stop.
        if (adminTrips.isEmpty()) {
            Toast.makeText(this, "You are not the Admin of any trips.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Show a premium list dialog of their trips
        String[] namesArray = tripNames.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
            .setTitle("Select Trip to Delete")
            .setItems(namesArray, (dialog, which) -> {
                Trip selectedTrip = adminTrips.get(which);
                confirmDeleteTrip(selectedTrip);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void confirmDeleteTrip(Trip trip) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete " + trip.getName() + "?")
            .setMessage("This will permanently remove the trip for all members. This cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                // Delete the parent trip document from Firebase
                db.collection("trips").document(trip.getId())
                  .delete()
                  .addOnSuccessListener(aVoid -> {
                      Toast.makeText(this, "Trip deleted permanently", Toast.LENGTH_SHORT).show();
                      // Your real-time listener will automatically remove the card from the screen!
                  });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

}
