package com.example.tripexpense;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ScrollView scrollView;
    private TextView tabMembers, tabExpenses, tabSettlement;
    private LinearLayout layoutMembers, layoutExpenses, layoutSettlement;
    private EditText etMemberName, etTitle, etAmount;
    private TextView tvMemberList, tvResults;
    private Spinner spinnerPayer;
    private LinearLayout llInvolvedMembers;
    private ListView lvExpenses;
    private Button btnAddExpense;
    
    private List<Member> memberList;
    private List<Expense> expenseList;
    
    private int currentTripId = -1; // Tracks which trip we are viewing
    private int editingExpenseId = -1; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fetch the Trip details passed from HomeActivity
        currentTripId = getIntent().getIntExtra("TRIP_ID", -1);
        String tripName = getIntent().getStringExtra("TRIP_NAME");

        if (currentTripId == -1) {
            Toast.makeText(this, "Error loading trip", Toast.LENGTH_SHORT).show();
            finish(); // Close this screen if no trip was passed
            return;
        }

        // Set the action bar title to the Trip Name
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(tripName);
        }

        dbHelper = new DatabaseHelper(this);

        tabMembers = findViewById(R.id.tabMembers);
        tabExpenses = findViewById(R.id.tabExpenses);
        tabSettlement = findViewById(R.id.tabSettlement);
        layoutMembers = findViewById(R.id.layoutMembers);
        layoutExpenses = findViewById(R.id.layoutExpenses);
        layoutSettlement = findViewById(R.id.layoutSettlement);
        scrollView = findViewById(R.id.scrollView);
        etMemberName = findViewById(R.id.etMemberName);
        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        tvMemberList = findViewById(R.id.tvMemberList);
        tvResults = findViewById(R.id.tvResults);
        spinnerPayer = findViewById(R.id.spinnerPayer);
        llInvolvedMembers = findViewById(R.id.llInvolvedMembers);
        lvExpenses = findViewById(R.id.lvExpenses);
        btnAddExpense = findViewById(R.id.btnAddExpense);

        tabMembers.setOnClickListener(v -> switchTab(1));
        tabExpenses.setOnClickListener(v -> switchTab(2));
        tabSettlement.setOnClickListener(v -> switchTab(3));

        refreshMembers();
        refreshExpenseLog();

        findViewById(R.id.btnAddMember).setOnClickListener(v -> {
            String name = etMemberName.getText().toString().trim();
            if (!name.isEmpty()) {
                dbHelper.insertMember(currentTripId, name); // Added currentTripId
                etMemberName.setText("");
                refreshMembers();
                Toast.makeText(this, "Member Added", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddExpense.setOnClickListener(v -> saveExpense());
        findViewById(R.id.btnCalculate).setOnClickListener(v -> calculateSplits());

        lvExpenses.setOnItemClickListener((parent, view, position, id) -> {
            Expense clickedExpense = expenseList.get(position);
            showExpenseDetailsDialog(clickedExpense);
        });
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
            calculateSplits();
        }
        scrollView.scrollTo(0, 0);
    }

    private void refreshMembers() {
        memberList = dbHelper.getAllMembers(currentTripId); // Fetches only this trip's members
        StringBuilder names = new StringBuilder("Members: ");
        llInvolvedMembers.removeAllViews(); 

        for (Member m : memberList) {
            names.append(m.getName()).append(", ");
            CheckBox cb = new CheckBox(this);
            cb.setText(m.getName());
            cb.setTag(m.getId());
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

    private void refreshExpenseLog() {
        expenseList = dbHelper.getFullExpenses(currentTripId); // Fetches only this trip's expenses
        ExpenseAdapter adapter = new ExpenseAdapter(this, expenseList);
        lvExpenses.setAdapter(adapter);
    }

    private void saveExpense() {
        if (memberList.isEmpty()) return;

        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        Member selectedPayer = (Member) spinnerPayer.getSelectedItem();

        if (title.isEmpty() || amountStr.isEmpty() || selectedPayer == null) {
            Toast.makeText(this, "Fill all details", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> involvedIds = new ArrayList<>();
        for (int i = 0; i < llInvolvedMembers.getChildCount(); i++) {
            CheckBox cb = (CheckBox) llInvolvedMembers.getChildAt(i);
            if (cb.isChecked()) involvedIds.add((Integer) cb.getTag());
        }

        if (involvedIds.isEmpty()) {
            Toast.makeText(this, "Select at least one involved member!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        if (editingExpenseId == -1) {
            dbHelper.insertExpense(currentTripId, title, amount, selectedPayer.getId(), involvedIds); // Added currentTripId
            Toast.makeText(this, "Expense Saved", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.updateExpense(editingExpenseId, title, amount, selectedPayer.getId(), involvedIds);
            Toast.makeText(this, "Expense Updated", Toast.LENGTH_SHORT).show();
            editingExpenseId = -1;
            btnAddExpense.setText("Save Expense");
        }
        
        etTitle.setText("");
        etAmount.setText("");
        refreshMembers();
        refreshExpenseLog();
    }

    private void showExpenseDetailsDialog(Expense expense) {
        StringBuilder involvedNames = new StringBuilder();
        for (Member m : expense.getInvolvedMembers()) {
            involvedNames.append("- ").append(m.getName()).append("\n");
        }

        String message = "Amount: ₹" + String.format("%.2f", expense.getAmount()) + "\n" +
                         "Paid By: " + expense.getPayerName() + "\n\n" +
                         "Split Between:\n" + involvedNames.toString();

        new AlertDialog.Builder(this)
                .setTitle(expense.getTitle())
                .setMessage(message)
                .setPositiveButton("Close", null)
                .setNeutralButton("Edit", (dialog, which) -> loadExpenseForEditing(expense))
                .setNegativeButton("Delete", (dialog, which) -> deleteExpenseConfirm(expense))
                .show();
    }

    private void deleteExpenseConfirm(Expense expense) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Expense?")
            .setMessage("Are you sure you want to delete '" + expense.getTitle() + "'?")
            .setPositiveButton("Yes", (dialog, which) -> {
                dbHelper.deleteExpense(expense.getId());
                refreshExpenseLog();
                Toast.makeText(this, "Expense Deleted", Toast.LENGTH_SHORT).show();
                
                if (editingExpenseId == expense.getId()) {
                    editingExpenseId = -1;
                    etTitle.setText("");
                    etAmount.setText("");
                    btnAddExpense.setText("Save Expense");
                }
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
            if (m.getId() == expense.getPayerId()) {
                spinnerPayer.setSelection(i);
                break;
            }
        }
        
        List<Integer> involvedIds = new ArrayList<>();
        for (Member m : expense.getInvolvedMembers()) involvedIds.add(m.getId());
        
        for (int i = 0; i < llInvolvedMembers.getChildCount(); i++) {
            CheckBox cb = (CheckBox) llInvolvedMembers.getChildAt(i);
            int memberId = (Integer) cb.getTag();
            cb.setChecked(involvedIds.contains(memberId));
        }
        
        btnAddExpense.setText("Update Expense");
        scrollView.scrollTo(0, 0);
    }

    private void calculateSplits() {
        if (memberList.isEmpty()) {
            tvResults.setText("Add members first.");
            return;
        }
        expenseList = dbHelper.getFullExpenses(currentTripId); // Updated to use currentTripId

        Map<Integer, Double> balances = new HashMap<>();
        for (Member m : memberList) balances.put(m.getId(), 0.0);

        for (Expense e : expenseList) {
            balances.put(e.getPayerId(), balances.get(e.getPayerId()) + e.getAmount());
            double splitShare = e.getAmount() / e.getInvolvedMembers().size();
            for (Member involved : e.getInvolvedMembers()) {
                balances.put(involved.getId(), balances.get(involved.getId()) - splitShare);
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
}
