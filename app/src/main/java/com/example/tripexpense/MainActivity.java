package com.example.tripexpense;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
    private EditText etMemberName, etTitle, etAmount;
    private TextView tvMemberList, tvResults;
    private Spinner spinnerPayer;
    private LinearLayout llInvolvedMembers;
    private ListView lvExpenses;
    private List<Member> memberList;
    private List<Expense> expenseList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        etMemberName = findViewById(R.id.etMemberName);
        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        tvMemberList = findViewById(R.id.tvMemberList);
        tvResults = findViewById(R.id.tvResults);
        spinnerPayer = findViewById(R.id.spinnerPayer);
        llInvolvedMembers = findViewById(R.id.llInvolvedMembers);
        lvExpenses = findViewById(R.id.lvExpenses);

        refreshMembers();
        refreshExpenseLog();

        findViewById(R.id.btnAddMember).setOnClickListener(v -> {
            String name = etMemberName.getText().toString().trim();
            if (!name.isEmpty()) {
                dbHelper.insertMember(name);
                etMemberName.setText("");
                refreshMembers();
                Toast.makeText(this, "Member Added", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnAddExpense).setOnClickListener(v -> saveExpense());
        findViewById(R.id.btnCalculate).setOnClickListener(v -> calculateSplits());

        // Handle Click on Expense Log Item
        lvExpenses.setOnItemClickListener((parent, view, position, id) -> {
            Expense clickedExpense = expenseList.get(position);
            showExpenseDetailsDialog(clickedExpense);
        });
    }

    private void refreshMembers() {
        memberList = dbHelper.getAllMembers();
        
        StringBuilder names = new StringBuilder("Members: ");
        llInvolvedMembers.removeAllViews(); // Clear old checkboxes

        for (Member m : memberList) {
            names.append(m.getName()).append(", ");
            
            // Create a checkbox for each member
            CheckBox cb = new CheckBox(this);
            cb.setText(m.getName());
            cb.setTag(m.getId());
            cb.setChecked(true); // Default to everyone involved
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
        expenseList = dbHelper.getFullExpenses();
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

        // Find out who is checked
        List<Integer> involvedIds = new ArrayList<>();
        for (int i = 0; i < llInvolvedMembers.getChildCount(); i++) {
            CheckBox cb = (CheckBox) llInvolvedMembers.getChildAt(i);
            if (cb.isChecked()) {
                involvedIds.add((Integer) cb.getTag());
            }
        }

        if (involvedIds.isEmpty()) {
            Toast.makeText(this, "Select at least one involved member!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        dbHelper.insertExpense(title, amount, selectedPayer.getId(), involvedIds);
        
        etTitle.setText("");
        etAmount.setText("");
        refreshExpenseLog();
        Toast.makeText(this, "Expense Saved", Toast.LENGTH_SHORT).show();
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
                .show();
    }

    private void calculateSplits() {
        if (memberList.isEmpty()) return;
        expenseList = dbHelper.getFullExpenses();

        // Balances Map: Positive means owed money, Negative means they owe money
        Map<Integer, Double> balances = new HashMap<>();
        for (Member m : memberList) balances.put(m.getId(), 0.0);

        for (Expense e : expenseList) {
            // Payer gets credited the full amount
            balances.put(e.getPayerId(), balances.get(e.getPayerId()) + e.getAmount());

            // Everyone involved gets debited their share
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
