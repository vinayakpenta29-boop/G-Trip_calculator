package com.example.tripexpense;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etMemberName, etTitle, etAmount;
    private TextView tvMemberList, tvResults;
    private Spinner spinnerPayer;
    private List<Member> memberList;

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

        Button btnAddMember = findViewById(R.id.btnAddMember);
        Button btnAddExpense = findViewById(R.id.btnAddExpense);
        Button btnCalculate = findViewById(R.id.btnCalculate);

        refreshMembers();

        btnAddMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etMemberName.getText().toString().trim();
                if (!name.isEmpty()) {
                    dbHelper.insertMember(name);
                    etMemberName.setText("");
                    refreshMembers();
                    Toast.makeText(MainActivity.this, "Member Added", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (memberList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Add members first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String title = etTitle.getText().toString().trim();
                String amountStr = etAmount.getText().toString().trim();
                Member selectedMember = (Member) spinnerPayer.getSelectedItem();

                if (title.isEmpty() || amountStr.isEmpty() || selectedMember == null) {
                    Toast.makeText(MainActivity.this, "Fill all expense details", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amountStr);
                dbHelper.insertExpense(title, amount, selectedMember.getId());
                
                etTitle.setText("");
                etAmount.setText("");
                Toast.makeText(MainActivity.this, "Expense Saved", Toast.LENGTH_SHORT).show();
            }
        });

        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateSplits();
            }
        });
    }

    private void refreshMembers() {
        memberList = dbHelper.getAllMembers();
        
        // Update text view showing members
        StringBuilder names = new StringBuilder("Members: ");
        for (Member m : memberList) {
            names.append(m.getName()).append(", ");
        }
        if (memberList.isEmpty()) {
            tvMemberList.setText("Members: None");
        } else {
            // remove last comma
            tvMemberList.setText(names.substring(0, names.length() - 2)); 
        }

        // Update Spinner
        ArrayAdapter<Member> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, memberList);
        spinnerPayer.setAdapter(adapter);
    }

    private void calculateSplits() {
        if (memberList.isEmpty()) {
            tvResults.setText("Add members and expenses first.");
            return;
        }

        List<Expense> expenses = dbHelper.getAllExpenses();
        double totalSpent = 0;
        
        // Map to track how much each member has paid so far
        Map<Integer, Double> paidMap = new HashMap<>();
        for (Member m : memberList) {
            paidMap.put(m.getId(), 0.0);
        }

        for (Expense e : expenses) {
            totalSpent += e.getAmount();
            double currentPaid = paidMap.get(e.getPayerId());
            paidMap.put(e.getPayerId(), currentPaid + e.getAmount());
        }

        if (totalSpent == 0) {
            tvResults.setText("No expenses logged yet.");
            return;
        }

        double perPersonShare = totalSpent / memberList.size();
        
        StringBuilder results = new StringBuilder();
        results.append(String.format("Total Trip Cost: $%.2f\n", totalSpent));
        results.append(String.format("Cost Per Person: $%.2f\n\n", perPersonShare));

        // Calculate Net Balances
        for (Member m : memberList) {
            double paidByMember = paidMap.get(m.getId());
            double balance = paidByMember - perPersonShare;
            
            if (balance > 0.01) {
                results.append(m.getName()).append(" gets back $").append(String.format("%.2f", balance)).append("\n");
            } else if (balance < -0.01) {
                results.append(m.getName()).append(" owes $").append(String.format("%.2f", Math.abs(balance))).append("\n");
            } else {
                results.append(m.getName()).append(" is settled up.\n");
            }
        }

        tvResults.setText(results.toString());
    }
}
