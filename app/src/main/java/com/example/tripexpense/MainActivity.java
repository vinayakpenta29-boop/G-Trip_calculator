package com.example.tripexpense;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etTitle, etAmount;
    private TextView tvTotalSpent;
    private ListView lvExpenses;
    private ExpenseAdapter adapter;
    private List<Expense> expenseList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        etTitle = findViewById(R.id.etTitle);
        etAmount = findViewById(R.id.etAmount);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        lvExpenses = findViewById(R.id.lvExpenses);

        refreshExpenseList();

        Button btnAddExpense = findViewById(R.id.btnAddExpense);
        btnAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTitle.getText().toString().trim();
                String amountStr = etAmount.getText().toString().trim();

                if (title.isEmpty() || amountStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter all details", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amountStr);
                boolean isInserted = dbHelper.insertExpense(title, amount);

                if (isInserted) {
                    Toast.makeText(MainActivity.this, "Expense Added", Toast.LENGTH_SHORT).show();
                    etTitle.setText("");
                    etAmount.setText("");
                    refreshExpenseList();
                } else {
                    Toast.makeText(MainActivity.this, "Error saving expense", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void refreshExpenseList() {
        expenseList = dbHelper.getAllExpenses();
        adapter = new ExpenseAdapter(this, expenseList);
        lvExpenses.setAdapter(adapter);

        double total = 0;
        for (Expense e : expenseList) {
            total += e.getAmount();
        }
        tvTotalSpent.setText(String.format("Total Spent: $%.2f", total));
    }
}
