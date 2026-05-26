package com.example.tripexpense;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.ListAdapter;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableLayout;
import android.widget.TableRow;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


import android.graphics.Bitmap;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;


import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentTripId;
    private String adminId;
    private String myUserId;
    private String shareCode;
    private boolean isAdmin;

    private ScrollView scrollView;
    private TextView tabMembers, tabExpenses, tabSettlement;
    private LinearLayout layoutMembers, layoutExpenses, layoutSettlement;
    private EditText etMemberName, etTitle, etAmount;
    private TextView tvMemberList, tvResults;
    private Spinner spinnerPayer;
    private LinearLayout llInvolvedMembers;
    private NonScrollListView lvExpenses;
    private Button btnAddExpense, btnAddMember;

    private List<Member> memberList = new ArrayList<>();
    private List<Expense> expenseList = new ArrayList<>();
    
    private String editingExpenseId = "-1"; // Strings for Firebase IDs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Setup Firebase & User Identity
        db = FirebaseFirestore.getInstance();
        myUserId = UserManager.getUserId(this);

        // 2. Get passed data
        currentTripId = getIntent().getStringExtra("TRIP_ID");
        String tripName = getIntent().getStringExtra("TRIP_NAME");
        adminId = getIntent().getStringExtra("ADMIN_ID");
        shareCode = getIntent().getStringExtra("SHARE_CODE");

        if (currentTripId == null) {
            Toast.makeText(this, "Error loading trip", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(tripName);
        }

        // 3. Determine if this user is the Admin
        isAdmin = myUserId.equals(adminId);

        // 4. Bind UI
        scrollView = findViewById(R.id.scrollView);
        tabMembers = findViewById(R.id.tabMembers);
        tabExpenses = findViewById(R.id.tabExpenses);
        tabSettlement = findViewById(R.id.tabSettlement);
        layoutMembers = findViewById(R.id.layoutMembers);
        layoutExpenses = findViewById(R.id.layoutExpenses);
        layoutSettlement = findViewById(R.id.layoutSettlement);
        etMemberName = findViewById(R.id.etMemberName);
        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        tvMemberList = findViewById(R.id.tvMemberList);
        tvResults = findViewById(R.id.tvResults);
        spinnerPayer = findViewById(R.id.spinnerPayer);
        llInvolvedMembers = findViewById(R.id.llInvolvedMembers);
        lvExpenses = findViewById(R.id.lvExpenses);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnAddMember = findViewById(R.id.btnAddMember);

        Spinner spinnerCategory = findViewById(R.id.spinnerCategory);
        String[] categories = {"🍔 Food & Drinks", "🚕 Transport", "🏨 Accommodation", "🎢 Activities", "🛒 Groceries", "💡 Other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(categoryAdapter);
        
        
        // 5. Apply Admin vs Viewer UI restrictions
        enforcePermissions();

        // 6. Setup Tabs
        tabMembers.setOnClickListener(v -> switchTab(1));
        tabExpenses.setOnClickListener(v -> switchTab(2));
        tabSettlement.setOnClickListener(v -> switchTab(3));

        // 7. Button Listeners (Only functional if Admin)
        btnAddMember.setOnClickListener(v -> saveMemberToCloud());
        btnAddExpense.setOnClickListener(v -> saveExpenseToCloud());
        
        findViewById(R.id.btnOpenNotes).setOnClickListener(v -> showNotesDialog());
        
        findViewById(R.id.btnCalculate).setOnClickListener(v -> calculateSplits());

        findViewById(R.id.btnSeeIndividualExpenses).setOnClickListener(v -> showSelectMemberDialog());

        findViewById(R.id.btnShareReport).setOnClickListener(v -> shareReportToWhatsApp());
        
        
        lvExpenses.setOnItemClickListener((parent, view, position, id) -> {
            Expense clickedExpense = expenseList.get(position);
            showExpenseDetailsDialog(clickedExpense);
        });

        // 8. Start Real-time Listeners
        listenForMembers();
        listenForExpenses();

        switchTab(2);
    }

    private void enforcePermissions() {
        if (!isAdmin) {
            // 1. Hide the input fields and buttons
            etMemberName.setVisibility(View.GONE);
            btnAddMember.setVisibility(View.GONE);
            
            findViewById(R.id.spinnerPayer).setVisibility(View.GONE);
            etTitle.setVisibility(View.GONE);
            etAmount.setVisibility(View.GONE);
            findViewById(R.id.llInvolvedMembers).setVisibility(View.GONE);
            btnAddExpense.setVisibility(View.GONE);
            
            // 2. Safely change/hide the section titles without crashing!
            try {
                View memberTitle = layoutMembers.getChildAt(0);
                if (memberTitle instanceof TextView) {
                    ((TextView) memberTitle).setText("Trip Travelers");
                }
                
                View expenseTitle = layoutExpenses.getChildAt(0);
                if (expenseTitle instanceof TextView) {
                    expenseTitle.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // --- FIREBASE REAL-TIME LISTENERS ---

    private void listenForMembers() {
        db.collection("trips").document(currentTripId).collection("members")
          .addSnapshotListener((value, error) -> {
              if (value != null) {
                  memberList.clear();
                  StringBuilder names = new StringBuilder();
                  
                  for (QueryDocumentSnapshot doc : value) {
                      Member m = doc.toObject(Member.class);
                      memberList.add(m);
                      names.append(m.getName()).append(", ");
                  }
                  
                  refreshMembersUI();
                  
                  // Update the Root Trip document so the Home Screen stays accurate
                  if (isAdmin) {
                      String joinedNames = names.length() > 0 ? names.substring(0, names.length() - 2) : "";
                      db.collection("trips").document(currentTripId).update(
                          "memberCount", memberList.size(),
                          "memberNames", joinedNames
                      );
                  }
              }
          });
    }

    private void listenForExpenses() {
        db.collection("trips").document(currentTripId).collection("expenses")
          .addSnapshotListener((value, error) -> {
              if (value != null) {
                  expenseList.clear();
                  double total = 0;
                  
                  for (QueryDocumentSnapshot doc : value) {
                      Expense e = doc.toObject(Expense.class);
                      expenseList.add(e);
                      total += e.getAmount();
                  }

                  Collections.sort(expenseList, (e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()));
                  
                  ExpenseAdapter adapter = new ExpenseAdapter(this, expenseList);
                  lvExpenses.setAdapter(adapter);
                  
                  // Auto-update calculations when new data comes in
                  calculateSplits();
                  updateSummaryTable();
                  
                  // Update the Root Trip document so Home Screen total stays accurate
                  if (isAdmin) {
                      db.collection("trips").document(currentTripId).update("totalExpense", total);
                  }
              }
          });
    }

    // --- CLOUD SAVE METHODS ---

    private void saveMemberToCloud() {
        String name = etMemberName.getText().toString().trim();
        if (name.isEmpty()) return;

        for (Member m : memberList) {
        // equalsIgnoreCase makes sure "Alice" and "alice" are treated as the same
        if (m.getName().equalsIgnoreCase(name)) {
            Toast.makeText(this, "This member already exists!", Toast.LENGTH_SHORT).show();
            return; // Stop the code here, do not save to Firebase
            }
        }

        // Auto-generate a Firebase ID
        String memberId = db.collection("trips").document(currentTripId).collection("members").document().getId();
        Member newMember = new Member(memberId, name);

        db.collection("trips").document(currentTripId).collection("members").document(memberId)
          .set(newMember)
          .addOnSuccessListener(aVoid -> {
              etMemberName.setText("");
              Toast.makeText(this, "Member Added", Toast.LENGTH_SHORT).show();
          });
    }

    private void saveExpenseToCloud() {
        if (memberList.isEmpty()) return;

        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        Member selectedPayer = (Member) spinnerPayer.getSelectedItem();

        // 🛑 NEW CHECK: Prevent saving if "Select Payer" is still selected
        if (selectedPayer == null || selectedPayer.getId().equals("-1")) {
            Toast.makeText(this, "Please select who paid!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Fill all details", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Member> involved = new ArrayList<>();
        for (int i = 0; i < llInvolvedMembers.getChildCount(); i++) {
            CheckBox cb = (CheckBox) llInvolvedMembers.getChildAt(i);
            if (cb.isChecked()) {
                String mId = (String) cb.getTag();
                for (Member m : memberList) {
                    if (m.getId().equals(mId)) involved.add(m);
                }
            }
        }

        if (involved.isEmpty()) {
            Toast.makeText(this, "Select at least one involved member!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        String expenseId = editingExpenseId.equals("-1") ? 
            db.collection("trips").document(currentTripId).collection("expenses").document().getId() : editingExpenseId;

        selected category from the UI
        Spinner spinnerCategory = findViewById(R.id.spinnerCategory);
        String selectedCategory = spinnerCategory.getSelectedItem().toString();

        Expense newExpense = new Expense(expenseId, title, amount, selectedPayer.getId(), selectedPayer.getName(), involved, selectedCategory);
        
        newExpense.setTimestamp(System.currentTimeMillis());

        db.collection("trips").document(currentTripId).collection("expenses").document(expenseId)
          .set(newExpense)
          .addOnSuccessListener(aVoid -> {
              // 🛑 RESET THE UI AFTER SAVING
              etTitle.setText("");
              etAmount.setText("");
              spinnerPayer.setSelection(0); // Resets Spinner back to "Select Payer"
              editingExpenseId = "-1";
              btnAddExpense.setText("Save Expense");
              Toast.makeText(this, "Expense Saved", Toast.LENGTH_SHORT).show();
          });
    }


    // --- UI HELPERS ---

    private void refreshMembersUI() {
        StringBuilder names = new StringBuilder("Members: ");
        llInvolvedMembers.removeAllViews();

        for (Member m : memberList) {
            names.append(m.getName()).append(", ");
            CheckBox cb = new CheckBox(this);
            cb.setText(m.getName());
            cb.setTag(m.getId()); // Store string ID
            cb.setChecked(true);
            llInvolvedMembers.addView(cb);
        }

        if (memberList.isEmpty()) {
            tvMemberList.setText("Members: None");
        } else {
            tvMemberList.setText(names.substring(0, names.length() - 2));
        }

        // 🛑 THE FIX: Create a separate list just for the Spinner
        List<Member> spinnerList = new ArrayList<>();
        // Add our dummy "Select Payer" item at the very top (Index 0)
        spinnerList.add(new Member("-1", "-- Select Payer --")); 
        // Then add all the real members below it
        spinnerList.addAll(memberList);

        ArrayAdapter<Member> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerList);
        spinnerPayer.setAdapter(adapter);
    }


    private void showExpenseDetailsDialog(Expense expense) {
        // 1. Inflate our custom premium layout
        View view = getLayoutInflater().inflate(R.layout.dialog_expense_details, null);

        // 2. Link the XML elements to Java
        TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvDialogAmount = view.findViewById(R.id.tvDialogAmount);
        TextView tvDialogPayer = view.findViewById(R.id.tvDialogPayer);
        TextView tvDialogSplitMembers = view.findViewById(R.id.tvDialogSplitMembers);

        // 3. Populate the data
        tvDialogTitle.setText(expense.getTitle());
        tvDialogAmount.setText("₹" + String.format("%.2f", expense.getAmount()));
        tvDialogPayer.setText("Paid by " + expense.getPayerName());

        StringBuilder involvedNames = new StringBuilder();
        for (Member m : expense.getInvolvedMembers()) {
            involvedNames.append("• ").append(m.getName()).append("\n");
        }
        tvDialogSplitMembers.setText(involvedNames.toString().trim());

        // 4. Build the premium Material Dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
               .setView(view)
               .setPositiveButton("Close", null);

        // Only show Edit/Delete if the user is the Admin
        if (isAdmin) {
            builder.setNeutralButton("Edit", (dialog, which) -> loadExpenseForEditing(expense))
                   .setNegativeButton("Delete", (dialog, which) -> deleteExpenseConfirm(expense));
        }
        
        builder.show();
    }


    private void deleteExpenseConfirm(Expense expense) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Expense?")
            .setMessage("Are you sure?")
            .setPositiveButton("Yes", (dialog, which) -> {
                db.collection("trips").document(currentTripId).collection("expenses").document(expense.getId()).delete();
                Toast.makeText(this, "Expense Deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void loadExpenseForEditing(Expense expense) {
        editingExpenseId = expense.getId();
        etTitle.setText(expense.getTitle());
        etAmount.setText(String.valueOf(expense.getAmount()));
        
        for (int i = 0; i < spinnerPayer.getCount(); i++) {
            Member m = (Member) spinnerPayer.getItemAtPosition(i);
            if (m.getId().equals(expense.getPayerId())) {
                spinnerPayer.setSelection(i);
                break;
            }
        }
        
        List<String> involvedIds = new ArrayList<>();
        for (Member m : expense.getInvolvedMembers()) involvedIds.add(m.getId());
        
        for (int i = 0; i < llInvolvedMembers.getChildCount(); i++) {
            CheckBox cb = (CheckBox) llInvolvedMembers.getChildAt(i);
            String mId = (String) cb.getTag();
            cb.setChecked(involvedIds.contains(mId));
        }
        
        btnAddExpense.setText("Update Expense");
        scrollView.scrollTo(0, 0);
    }

    // --- MATH & CALCULATIONS (Updated for String IDs) ---

    private void calculateSplits() {
        if (memberList.isEmpty()) {
            tvResults.setText("Add members first.");
            return;
        }

        Map<String, Double> balances = new HashMap<>();
        for (Member m : memberList) balances.put(m.getId(), 0.0);

        for (Expense e : expenseList) {
            // Give payer positive balance
            double currentPayerBal = balances.containsKey(e.getPayerId()) ? balances.get(e.getPayerId()) : 0.0;
            balances.put(e.getPayerId(), currentPayerBal + e.getAmount());

            // Subtract split share from everyone involved
            double splitShare = e.getAmount() / e.getInvolvedMembers().size();
            for (Member involved : e.getInvolvedMembers()) {
                if (balances.containsKey(involved.getId())) {
                    balances.put(involved.getId(), balances.get(involved.getId()) - splitShare);
                }
            }
        }

        StringBuilder results = new StringBuilder("💸 HOW TO SETTLE UP:\n\n");
        
        class Balance {
            String name; double amount;
            Balance(String name, double amount) { this.name = name; this.amount = amount; }
        }

        List<Balance> debtors = new ArrayList<>();
        List<Balance> creditors = new ArrayList<>();

        for (Member m : memberList) {
            double bal = balances.get(m.getId());
            if (bal < -0.01) debtors.add(new Balance(m.getName(), Math.abs(bal)));
            else if (bal > 0.01) creditors.add(new Balance(m.getName(), bal));
        }

        boolean needsSettlement = false;
        int i = 0, j = 0;

        while (i < debtors.size() && j < creditors.size()) {
            needsSettlement = true;
            Balance debtor = debtors.get(i);
            Balance creditor = creditors.get(j);

            double settlement = Math.min(debtor.amount, creditor.amount);
            results.append(debtor.name).append(" Pays ").append(" ➔ ").append(creditor.name)
                   .append(String.format(" ₹%.2f\n", settlement)); 

            debtor.amount -= settlement;
            creditor.amount -= settlement; 

            if (debtor.amount < 0.01) i++;
            if (creditor.amount < 0.01) j++;
        }

        if (!needsSettlement) results.append("Everyone is completely settled up! 🎉\n");
        tvResults.setText(results.toString());

        updateSummaryTable();
    }

    private void switchTab(int tabIndex) {
        tabMembers.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabMembers.setTextColor(Color.parseColor("#333333"));
        tabExpenses.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabExpenses.setTextColor(Color.parseColor("#333333"));
        tabSettlement.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabSettlement.setTextColor(Color.parseColor("#333333"));

        layoutMembers.setVisibility(View.GONE);
        layoutExpenses.setVisibility(View.GONE);
        layoutSettlement.setVisibility(View.GONE);

        if (tabIndex == 1) {
            tabMembers.setBackgroundResource(R.drawable.bg_tab_selected);
            tabMembers.setTextColor(Color.WHITE);
            layoutMembers.setVisibility(View.VISIBLE);
        } else if (tabIndex == 2) {
            tabExpenses.setBackgroundResource(R.drawable.bg_tab_selected);
            tabExpenses.setTextColor(Color.WHITE);
            layoutExpenses.setVisibility(View.VISIBLE);
        } else if (tabIndex == 3) {
            tabSettlement.setBackgroundResource(R.drawable.bg_tab_selected);
            tabSettlement.setTextColor(Color.WHITE);
            layoutSettlement.setVisibility(View.VISIBLE);
        }
        scrollView.scrollTo(0, 0);
    }

    // --- SHARE CODE MENU BAR ---
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Create a "Share" button in the top right Action Bar
        menu.add(0, 1, 0, "Share Code").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            // Inflate our custom QR layout
            View view = getLayoutInflater().inflate(R.layout.dialog_share_code, null);
            ImageView ivQrCode = view.findViewById(R.id.ivQrCode);
            TextView tvShareCode = view.findViewById(R.id.tvShareCode);

            tvShareCode.setText(shareCode);

            // Generate the QR Code Bitmap
            try {
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.encodeBitmap(shareCode, BarcodeFormat.QR_CODE, 600, 600);
                ivQrCode.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Show the Dialog
            new AlertDialog.Builder(this)
                .setTitle("Share Code")
                .setView(view)
                .setPositiveButton("Close", null)
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateSummaryTable() {
        TableLayout tableSummary = findViewById(R.id.tableSummary);
        
        // 1. Clear old data rows (but keep the Header at index 0)
        int count = tableSummary.getChildCount();
        for (int i = count - 1; i > 0; i--) {
            tableSummary.removeViewAt(i);
        }

        // 2. Set up variables for the math
        Map<String, Double> totalPaid = new HashMap<>();
        Map<String, Double> totalShare = new HashMap<>();

        for (Member m : memberList) {
            totalPaid.put(m.getId(), 0.0);
            totalShare.put(m.getId(), 0.0);
        }

        // 3. Calculate Paid & Share for every expense
        for (Expense e : expenseList) {
            // Add to the Payer's total
            if (totalPaid.containsKey(e.getPayerId())) {
                totalPaid.put(e.getPayerId(), totalPaid.get(e.getPayerId()) + e.getAmount());
            }

            // Split the share among involved members
            if (e.getInvolvedMembers() != null && !e.getInvolvedMembers().isEmpty()) {
                double splitAmount = e.getAmount() / e.getInvolvedMembers().size();
                for (Member m : e.getInvolvedMembers()) {
                    if (totalShare.containsKey(m.getId())) {
                        totalShare.put(m.getId(), totalShare.get(m.getId()) + splitAmount);
                    }
                }
            }
        }

        // 4. Build the UI Rows dynamically
        for (Member m : memberList) {
            double paid = totalPaid.get(m.getId());
            double share = totalShare.get(m.getId());
            double balance = paid - share;

            TableRow row = new TableRow(this);
            row.setPadding(0, 16, 0, 16); // Premium vertical spacing

            // Column 1: Member Name
            TextView tvName = new TextView(this);
            tvName.setText(m.getName());
            tvName.setTextColor(ContextCompat.getColor(this, R.color.purple_primary));
            tvName.setTypeface(Typeface.create("monospace", Typeface.BOLD));
            tvName.setTextSize(14f);

            // Column 2: Total Paid
            TextView tvPaid = new TextView(this);
            tvPaid.setText(String.format("₹%.2f", paid));
            tvPaid.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            tvPaid.setGravity(Gravity.CENTER);
            tvPaid.setTypeface(Typeface.create("monospace", Typeface.NORMAL)); 
            tvPaid.setTextSize(12f);

            // Column 3: Total Share
            TextView tvShare = new TextView(this);
            tvShare.setText(String.format("₹%.2f", share));
            tvShare.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            tvShare.setGravity(Gravity.CENTER);
            tvShare.setTypeface(Typeface.create("monospace", Typeface.NORMAL)); 
            tvShare.setTextSize(12f);

            // Column 4: Balance (Color Coded!)
            TextView tvBalance = new TextView(this);
            tvBalance.setGravity(Gravity.END);
            tvBalance.setTypeface(Typeface.create("monospace", Typeface.NORMAL)); 
            tvBalance.setTextSize(12f);

            if (balance > 0.01) {
                // They paid more than their share: They get money BACK (Green)
                tvBalance.setTextColor(Color.parseColor("#388E3C")); 
                tvBalance.setText("+" + String.format("₹%.2f", balance));
            } else if (balance < -0.01) {
                // They used more than they paid: They OWE money (Red)
                tvBalance.setTextColor(Color.parseColor("#D32F2F")); 
                tvBalance.setText(String.format("₹%.2f", balance)); 
            } else {
                // Perfect zero balance
                tvBalance.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
                tvBalance.setText("₹0.00");
            }

            // Add all columns to the row
            row.addView(tvName);
            row.addView(tvPaid);
            row.addView(tvShare);
            row.addView(tvBalance);

            // Add row to table
            tableSummary.addView(row);

            // Add a thin grey divider line under the row
            View divider = new View(this);
            TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1);
            params.span = 4;
            divider.setLayoutParams(params);
            divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider_color));
            tableSummary.addView(divider);
        }
    }

    private void showNotesDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_notes, null);
        ListView lvNotes = view.findViewById(R.id.lvNotes);
        
        com.google.android.material.textfield.TextInputEditText etNewNote = view.findViewById(R.id.etNewNote);
        com.google.android.material.button.MaterialButton btnSaveNote = view.findViewById(R.id.btnSaveNote);

        List<Note> noteList = new ArrayList<>();
        NoteAdapter noteAdapter = new NoteAdapter(this, noteList);
        lvNotes.setAdapter(noteAdapter);

                // 🛑 ADD THIS: Listen for a Long-Press on any note
        lvNotes.setOnItemLongClickListener((parent, view1, position, id) -> {
            Note selectedNote = noteList.get(position);
            showNoteOptionsDialog(selectedNote);
            return true; // Tells Android we handled the long-click
        });
        

        // 1. Build and show the pop-up
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .show();

        // 2. Fetch Notes from Firebase instantly (Oldest at top, Newest at bottom like a chat)
        db.collection("trips").document(currentTripId).collection("notes")
          .orderBy("timestamp") 
          .addSnapshotListener((value, error) -> {
              if (value != null) {
                  noteList.clear();
                  for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                      noteList.add(doc.toObject(Note.class));
                  }
                  noteAdapter.notifyDataSetChanged();
                  
                  // Auto-scroll to the newest note!
                  if(noteAdapter.getCount() > 0) {
                      lvNotes.setSelection(noteAdapter.getCount() - 1);
                  }
              }
          });

        // 3. Save a new Note
        btnSaveNote.setOnClickListener(v -> {
            String text = etNewNote.getText().toString().trim();
            if (!text.isEmpty()) {
                String noteId = db.collection("trips").document(currentTripId).collection("notes").document().getId();
                
                // Note: We use "Member" here. If you track names in SharedPreferences, replace "Member" with their real name!
                Note newNote = new Note(noteId, text, "Member", System.currentTimeMillis());

                db.collection("trips").document(currentTripId).collection("notes").document(noteId)
                  .set(newNote)
                  .addOnSuccessListener(aVoid -> {
                      etNewNote.setText(""); // Clear the input box after sending
                  });
            }
        });
    }

    // INDIVIDUAL EXPENSE BREAKDOWN LOGIC

    private void showSelectMemberDialog() {
        if (memberList.isEmpty()) {
            Toast.makeText(this, "No members available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert Member List to an Array of Names for the pop-up
        String[] namesArray = new String[memberList.size()];
        for (int i = 0; i < memberList.size(); i++) {
            namesArray[i] = memberList.get(i).getName();
        }

        // Show a premium Material list of names
        new MaterialAlertDialogBuilder(this)
            .setTitle("Whose expenses do you want to see?")
            .setItems(namesArray, (dialog, which) -> {
                Member selectedMember = memberList.get(which);
                showMemberExpensesDialog(selectedMember);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showMemberExpensesDialog(Member member) {
        View view = getLayoutInflater().inflate(R.layout.dialog_member_expenses, null);
        
        TextView tvName = view.findViewById(R.id.tvDialogMemberName);
        LinearLayout llContainer = view.findViewById(R.id.llExpenseListContainer);
        TextView tvTotal = view.findViewById(R.id.tvDialogTotalShare);

        // Force uppercase for an extra premium monospace look
        tvName.setText(member.getName().toUpperCase() + "'S SHARE");

        double totalShare = 0.0;

        // Loop through ALL expenses to find which ones this member was a part of
        for (Expense e : expenseList) {
            boolean isMemberInvolved = false;
            
            if (e.getInvolvedMembers() != null) {
                for (Member m : e.getInvolvedMembers()) {
                    if (m.getId().equals(member.getId())) {
                        isMemberInvolved = true;
                        break;
                    }
                }
            }

            // If they are part of this expense, create a beautiful row for it!
            if (isMemberInvolved) {
                double myShare = e.getAmount() / e.getInvolvedMembers().size();
                totalShare += myShare;

                // 1. Create the Row Layout
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 20, 0, 20);

                // 2. The Expense Name (Left side)
                TextView tvExpenseTitle = new TextView(this);
                tvExpenseTitle.setText(e.getTitle());
                tvExpenseTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
                tvExpenseTitle.setTextSize(16f);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tvExpenseTitle.setLayoutParams(titleParams);

                // 3. The Amount (Right side in Monospace)
                TextView tvExpenseShare = new TextView(this);
                tvExpenseShare.setText(String.format("₹%.2f", myShare));
                tvExpenseShare.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
                tvExpenseShare.setTextSize(16f);
                tvExpenseShare.setTypeface(Typeface.create("monospace", Typeface.NORMAL)); 

                // Add texts to the row, and the row to the container
                row.addView(tvExpenseTitle);
                row.addView(tvExpenseShare);
                llContainer.addView(row);

                // 4. Add a clean grey divider under the row
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider_color));
                llContainer.addView(divider);
            }
        }

        // If they haven't been added to any expenses yet
        if (llContainer.getChildCount() == 0) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No expenses recorded for this member.");
            tvEmpty.setPadding(0, 16, 0, 16);
            llContainer.addView(tvEmpty);
        }

        // Set the final bottom total
        tvTotal.setText(String.format("₹%.2f", totalShare));

        // Show the beautiful receipt dialog
        new MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton("Close", null)
            .show();
    }

    // ==========================================
    // NOTE EDIT & DELETE LOGIC
    // ==========================================

    private void showNoteOptionsDialog(Note note) {
        String[] options = {"Edit Note", "Delete Note"};

        new MaterialAlertDialogBuilder(this)
            .setTitle("Note Options")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    showEditNoteDialog(note);
                } else if (which == 1) {
                    deleteNoteFromCloud(note);
                }
            })
            .show();
    }

    private void showEditNoteDialog(Note note) {
        // Build a premium text box programmatically (no new XML file needed!)
        com.google.android.material.textfield.TextInputLayout layout = new com.google.android.material.textfield.TextInputLayout(this);
        int padding = (int) (20 * getResources().getDisplayMetrics().density); // 20dp padding
        layout.setPadding(padding, padding / 2, padding, 0);
        
        com.google.android.material.textfield.TextInputEditText input = new com.google.android.material.textfield.TextInputEditText(this);
        input.setText(note.getText());
        input.setSelection(input.getText().length()); // Put cursor at the end
        layout.addView(input);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Edit Note")
            .setView(layout)
            .setPositiveButton("Save", (dialog, which) -> {
                String newText = input.getText().toString().trim();
                if (!newText.isEmpty() && !newText.equals(note.getText())) {
                    // Update the note in Firebase
                    db.collection("trips").document(currentTripId)
                      .collection("notes").document(note.getId())
                      .update("text", newText)
                      .addOnSuccessListener(aVoid -> Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show());
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteNoteFromCloud(Note note) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Note?")
            .setMessage("Are you sure you want to delete this note? This cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                // Remove the note from Firebase
                db.collection("trips").document(currentTripId)
                  .collection("notes").document(note.getId())
                  .delete()
                  .addOnSuccessListener(aVoid -> Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ==========================================
    // EXPORT TO WHATSAPP LOGIC
    // ==========================================

    private void shareReportToWhatsApp() {
        TextView tvResults = findViewById(R.id.tvResults);
        String resultsText = tvResults.getText().toString();

        // 1. Make sure they actually calculated the splits first!
        if (resultsText.equals("Press calculate to see who owes whom.") || resultsText.isEmpty()) {
            Toast.makeText(this, "Please calculate the splits first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Get the Trip Name to use in the title
        String tripName = getIntent().getStringExtra("TRIP_NAME");
        if (tripName == null) {
            tripName = "Our Trip";
        }

        // 3. Build a beautifully formatted WhatsApp message using asterisks for bolding
        String shareBody = "✈️ *" + tripName + " - Final Settlement* ✈️\n\n" +
                           "Here is the final breakdown of who owes whom:\n\n" +
                           resultsText + "\n\n" +
                           "📊 _Calculated instantly with Trip Expense Calculator_";

        // 4. Create the Android Share Intent
        android.content.Intent sharingIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        
        // 5. Try to force WhatsApp to open. If they don't have it installed, open the normal share menu!
        sharingIntent.setPackage("com.whatsapp");
        try {
            startActivity(sharingIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            sharingIntent.setPackage(null);
            startActivity(android.content.Intent.createChooser(sharingIntent, "Share Report via..."));
        }
    }

}
