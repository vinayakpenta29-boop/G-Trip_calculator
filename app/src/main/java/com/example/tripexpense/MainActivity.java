package com.example.tripexpense;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;


import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    private ListView lvExpenses;
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

        // 5. Apply Admin vs Viewer UI restrictions
        enforcePermissions();

        // 6. Setup Tabs
        tabMembers.setOnClickListener(v -> switchTab(1));
        tabExpenses.setOnClickListener(v -> switchTab(2));
        tabSettlement.setOnClickListener(v -> switchTab(3));

        // 7. Button Listeners (Only functional if Admin)
        btnAddMember.setOnClickListener(v -> saveMemberToCloud());
        btnAddExpense.setOnClickListener(v -> saveExpenseToCloud());
        findViewById(R.id.btnCalculate).setOnClickListener(v -> calculateSplits());

        lvExpenses.setOnItemClickListener((parent, view, position, id) -> {
            Expense clickedExpense = expenseList.get(position);
            showExpenseDetailsDialog(clickedExpense);
        });

        // 8. Start Real-time Listeners
        listenForMembers();
        listenForExpenses();
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
                  
                  ExpenseAdapter adapter = new ExpenseAdapter(this, expenseList);
                  lvExpenses.setAdapter(adapter);
                  
                  // Auto-update calculations when new data comes in
                  calculateSplits();
                  
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

        if (title.isEmpty() || amountStr.isEmpty() || selectedPayer == null) {
            Toast.makeText(this, "Fill all details", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Member> involved = new ArrayList<>();
        for (int i = 0; i < llInvolvedMembers.getChildCount(); i++) {
            CheckBox cb = (CheckBox) llInvolvedMembers.getChildAt(i);
            if (cb.isChecked()) {
                // Find the member object by ID
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

        // If editingExpenseId is "-1", generate a new ID. Otherwise, use the existing one to overwrite it.
        String expenseId = editingExpenseId.equals("-1") ? 
            db.collection("trips").document(currentTripId).collection("expenses").document().getId() : editingExpenseId;

        Expense newExpense = new Expense(expenseId, title, amount, selectedPayer.getId(), selectedPayer.getName(), involved);

        db.collection("trips").document(currentTripId).collection("expenses").document(expenseId)
          .set(newExpense)
          .addOnSuccessListener(aVoid -> {
              etTitle.setText("");
              etAmount.setText("");
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

        ArrayAdapter<Member> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, memberList);
        spinnerPayer.setAdapter(adapter);
    }

    private void showExpenseDetailsDialog(Expense expense) {
        StringBuilder involvedNames = new StringBuilder();
        for (Member m : expense.getInvolvedMembers()) {
            involvedNames.append("- ").append(m.getName()).append("\n");
        }

        String message = "Amount: ₹" + String.format("%.2f", expense.getAmount()) + "\n" +
                         "Paid By: " + expense.getPayerName() + "\n\n" +
                         "Split Between:\n" + involvedNames.toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(expense.getTitle())
               .setMessage(message)
               .setPositiveButton("Close", null);

        // Only show Edit/Delete if Admin
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
            results.append("➔ ").append(debtor.name).append(" pays ").append(creditor.name)
                   .append(String.format(" ₹%.2f\n", settlement));

            debtor.amount -= settlement;
            creditor.amount -= settlement;

            if (debtor.amount < 0.01) i++;
            if (creditor.amount < 0.01) j++;
        }

        if (!needsSettlement) results.append("Everyone is completely settled up! 🎉\n");
        tvResults.setText(results.toString());
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

}
