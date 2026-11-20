package com.example.tripexpense;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyTripsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String myUserId;

    private TextView tvVisitedCount, tvUnvisitedCount, tabToVisit, tabVisited, tvEmptyPlaces;
    private ListView lvMyPlaces;

    private final List<MyPlace> allPlaces = new ArrayList<>();
    private boolean showingVisitedTab = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_trips);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Trips");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        myUserId = UserManager.getUserId(this);

        tvVisitedCount = findViewById(R.id.tvVisitedCount);
        tvUnvisitedCount = findViewById(R.id.tvUnvisitedCount);
        tabToVisit = findViewById(R.id.tabToVisit);
        tabVisited = findViewById(R.id.tabVisited);
        tvEmptyPlaces = findViewById(R.id.tvEmptyPlaces);
        lvMyPlaces = findViewById(R.id.lvMyPlaces);

        tabToVisit.setOnClickListener(v -> switchTab(false));
        tabVisited.setOnClickListener(v -> switchTab(true));

        findViewById(R.id.btnAddNewPlace).setOnClickListener(v -> showAddPlaceDialog());

        listenForPlaces();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void switchTab(boolean showVisited) {
        showingVisitedTab = showVisited;

        if (showingVisitedTab) {
            tabToVisit.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabToVisit.setTextColor(Color.parseColor("#333333"));
            tabVisited.setBackgroundResource(R.drawable.bg_tab_selected);
            tabVisited.setTextColor(Color.WHITE);
        } else {
            tabToVisit.setBackgroundResource(R.drawable.bg_tab_selected);
            tabToVisit.setTextColor(Color.WHITE);
            tabVisited.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabVisited.setTextColor(Color.parseColor("#333333"));
        }

        updatePlacesListUI();
    }

    private void listenForPlaces() {
        db.collection("users").document(myUserId).collection("my_trips_places")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        allPlaces.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            MyPlace place = doc.toObject(MyPlace.class);
                            allPlaces.add(place);
                        }
                        Collections.sort(allPlaces, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        updatePlacesListUI();
                    }
                });
    }

    private void updatePlacesListUI() {
        int visitedCount = 0;
        int unvisitedCount = 0;
        List<MyPlace> filtered = new ArrayList<>();

        for (MyPlace p : allPlaces) {
            if (p.isVisited()) visitedCount++;
            else unvisitedCount++;

            if (p.isVisited() == showingVisitedTab) {
                filtered.add(p);
            }
        }

        tvVisitedCount.setText(String.valueOf(visitedCount));
        tvUnvisitedCount.setText(String.valueOf(unvisitedCount));

        if (filtered.isEmpty()) {
            tvEmptyPlaces.setVisibility(View.VISIBLE);
            tvEmptyPlaces.setText(showingVisitedTab
                    ? "No visited places yet.\nMark a place as visited once you complete your trip!"
                    : "No upcoming places yet.\nTap '+ Add New Place' to add your dream destinations!");
        } else {
            tvEmptyPlaces.setVisibility(View.GONE);
        }

        MyPlaceAdapter adapter = new MyPlaceAdapter(this, filtered, new MyPlaceAdapter.PlaceActionListener() {
            @Override
            public void onToggleVisited(MyPlace place) {
                boolean newStatus = !place.isVisited();
                db.collection("users").document(myUserId)
                        .collection("my_trips_places").document(place.getId())
                        .update("visited", newStatus)
                        .addOnSuccessListener(aVoid -> Toast.makeText(
                                MyTripsActivity.this,
                                newStatus ? "Marked as Visited!" : "Moved to Yet to Visit",
                                Toast.LENGTH_SHORT
                        ).show());
            }

            @Override
            public void onDeletePlace(MyPlace place) {
                new MaterialAlertDialogBuilder(MyTripsActivity.this)
                        .setTitle("Delete Place?")
                        .setMessage("Remove \"" + place.getPlaceName() + "\" from your list?")
                        .setPositiveButton("Delete", (d, w) -> db.collection("users")
                                .document(myUserId)
                                .collection("my_trips_places")
                                .document(place.getId())
                                .delete())
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        lvMyPlaces.setAdapter(adapter);
    }

    // ==========================================
    // ALL-INDIA TOURIST PLACE SEARCH & ADD DIALOG
    // ==========================================

    private void showAddPlaceDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_place, null);

        EditText etSearchPlace = view.findViewById(R.id.etSearchPlace);
        MaterialButton btnSearchIndia = view.findViewById(R.id.btnSearchIndia);
        ProgressBar progressSearch = view.findViewById(R.id.progressSearch);
        LinearLayout llSearchResults = view.findViewById(R.id.llSearchResults);

        EditText etPlaceName = view.findViewById(R.id.etPlaceName);
        EditText etPlaceState = view.findViewById(R.id.etPlaceState);
        EditText etPlaceCity = view.findViewById(R.id.etPlaceCity);
        EditText etPlaceVillage = view.findViewById(R.id.etPlaceVillage);
        CheckBox cbAlreadyVisited = view.findViewById(R.id.cbAlreadyVisited);

        Runnable runSearch = () -> {
            String query = etSearchPlace.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Enter a tourist place, city, or village", Toast.LENGTH_SHORT).show();
                return;
            }
            searchIndiaPlaces(query, progressSearch, llSearchResults, etPlaceName, etPlaceState, etPlaceCity, etPlaceVillage);
        };

        btnSearchIndia.setOnClickListener(v -> runSearch.run());
        etSearchPlace.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch.run();
                return true;
            }
            return false;
        });

        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setPositiveButton("Save Place", (dialog, which) -> {
                    String name = etPlaceName.getText().toString().trim();
                    String state = etPlaceState.getText().toString().trim();
                    String city = etPlaceCity.getText().toString().trim();
                    String village = etPlaceVillage.getText().toString().trim();
                    boolean visited = cbAlreadyVisited.isChecked();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Please enter or select a place name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String placeId = db.collection("users").document(myUserId)
                            .collection("my_trips_places").document().getId();

                    MyPlace newPlace = new MyPlace(placeId, name, state, city, village, visited, System.currentTimeMillis());

                    db.collection("users").document(myUserId)
                            .collection("my_trips_places").document(placeId)
                            .set(newPlace)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Place saved to My Trips!", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void searchIndiaPlaces(
            String query,
            ProgressBar progressSearch,
            LinearLayout llSearchResults,
            EditText etPlaceName,
            EditText etPlaceState,
            EditText etPlaceCity,
            EditText etPlaceVillage
    ) {
        progressSearch.setVisibility(View.VISIBLE);
        llSearchResults.removeAllViews();
        llSearchResults.setVisibility(View.GONE);

        executor.execute(() -> {
            List<MyPlace> foundPlaces = new ArrayList<>();
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                String apiUrl = "https://nominatim.openstreetmap.org/search?q=" + encoded
                        + "&countrycodes=in&format=json&addressdetails=1&limit=6";

                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "TripExpenseAndroidApp/1.0");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONArray arr = new JSONArray(sb.toString());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.getJSONObject(i);
                        JSONObject addr = item.optJSONObject("address");

                        String name = item.optString("name", "");
                        if (name.isEmpty()) {
                            String display = item.optString("display_name", query);
                            name = display.split(",")[0].trim();
                        }

                        String state = "";
                        String city = "";
                        String villageOrArea = "";

                        if (addr != null) {
                            state = addr.optString("state", addr.optString("state_district", ""));

                            city = addr.optString("city", "");
                            if (city.isEmpty()) city = addr.optString("town", "");
                            if (city.isEmpty()) city = addr.optString("county", "");
                            if (city.isEmpty()) city = addr.optString("state_district", "");

                            villageOrArea = addr.optString("village", "");
                            if (villageOrArea.isEmpty()) villageOrArea = addr.optString("suburb", "");
                            if (villageOrArea.isEmpty()) villageOrArea = addr.optString("hamlet", "");
                            if (villageOrArea.isEmpty()) villageOrArea = addr.optString("neighbourhood", "");
                            if (villageOrArea.isEmpty()) villageOrArea = addr.optString("road", "");
                        }

                        foundPlaces.add(new MyPlace("", name, state, city, villageOrArea, false, 0));
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            mainHandler.post(() -> {
                progressSearch.setVisibility(View.GONE);
                llSearchResults.removeAllViews();

                if (foundPlaces.isEmpty()) {
                    Toast.makeText(MyTripsActivity.this, "No matches found. You can enter details manually below.", Toast.LENGTH_SHORT).show();
                    etPlaceName.setText(query);
                    return;
                }

                llSearchResults.setVisibility(View.VISIBLE);
                for (MyPlace result : foundPlaces) {
                    MaterialCardView card = new MaterialCardView(MyTripsActivity.this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 0, 0, 12);
                    card.setLayoutParams(params);
                    card.setCardBackgroundColor(Color.parseColor("#EDE7F6"));
                    card.setRadius(24f);
                    card.setCardElevation(0f);

                    LinearLayout inner = new LinearLayout(MyTripsActivity.this);
                    inner.setOrientation(LinearLayout.VERTICAL);
                    inner.setPadding(28, 20, 28, 20);

                    TextView tvTitle = new TextView(MyTripsActivity.this);
                    tvTitle.setText(result.getPlaceName());
                    tvTitle.setTextColor(Color.parseColor("#6200EE"));
                    tvTitle.setTextSize(15f);
                    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

                    String subtitle = "State: " + (result.getState().isEmpty() ? "—" : result.getState())
                            + "  |  City: " + (result.getCity().isEmpty() ? "—" : result.getCity());
                    if (!result.getVillageOrArea().isEmpty()) {
                        subtitle += "\nArea/Village: " + result.getVillageOrArea();
                    }

                    TextView tvSub = new TextView(MyTripsActivity.this);
                    tvSub.setText(subtitle);
                    tvSub.setTextColor(Color.parseColor("#444444"));
                    tvSub.setTextSize(12f);

                    inner.addView(tvTitle);
                    inner.addView(tvSub);
                    card.addView(inner);

                    card.setOnClickListener(v -> {
                        etPlaceName.setText(result.getPlaceName());
                        etPlaceState.setText(result.getState());
                        etPlaceCity.setText(result.getCity());
                        etPlaceVillage.setText(result.getVillageOrArea());
                        llSearchResults.setVisibility(View.GONE);
                        Toast.makeText(MyTripsActivity.this, "Location auto-filled!", Toast.LENGTH_SHORT).show();
                    });

                    llSearchResults.addView(card);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
