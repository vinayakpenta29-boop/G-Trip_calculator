package com.example.tripexpense;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class ExpenseAdapter extends ArrayAdapter<Expense> {

    public ExpenseAdapter(Context context, List<Expense> expenses) {
        super(context, 0, expenses);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_expense, parent, false);
        }

        Expense expense = getItem(position);
        
        TextView tvIcon = convertView.findViewById(R.id.tvCategoryIcon);
        TextView tvTitle = convertView.findViewById(R.id.tvExpenseTitle);
        TextView tvPayer = convertView.findViewById(R.id.tvExpensePayer);
        TextView tvAmount = convertView.findViewById(R.id.tvExpenseAmount);

        if (expense != null) {
            tvTitle.setText(expense.getTitle());
            tvPayer.setText("Paid by " + expense.getPayerName());
            tvAmount.setText(String.format("₹%.2f", expense.getAmount()));

            // 🛑 NEW: Handle the category emoji
            String category = expense.getCategory();
            if (category != null && !category.isEmpty()) {
                // Splits "🍔 Food & Drinks" at the space and grabs just the "🍔"
                String emoji = category.split(" ")[0];
                tvIcon.setText(emoji);
            } else {
                // Default fallback icon for any older expenses saved before we added categories
                tvIcon.setText("💸"); 
            }
        }

        return convertView;
    }
}
