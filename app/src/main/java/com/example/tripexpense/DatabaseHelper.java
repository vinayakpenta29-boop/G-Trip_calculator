package com.example.tripexpense;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "expenses.db";
    private static final int DATABASE_VERSION = 2; // Upgraded version!

    // Member Table
    public static final String TABLE_MEMBERS = "members_table";
    public static final String COL_MEMBER_ID = "ID";
    public static final String COL_MEMBER_NAME = "NAME";

    // Expense Table
    public static final String TABLE_EXPENSES = "expense_table";
    public static final String COL_EXP_ID = "ID";
    public static final String COL_EXP_TITLE = "TITLE";
    public static final String COL_EXP_AMOUNT = "AMOUNT";
    public static final String COL_EXP_PAYER_ID = "PAYER_ID";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createMembersTable = "CREATE TABLE " + TABLE_MEMBERS + " (" +
                COL_MEMBER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MEMBER_NAME + " TEXT)";
        
        String createExpensesTable = "CREATE TABLE " + TABLE_EXPENSES + " (" +
                COL_EXP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EXP_TITLE + " TEXT, " +
                COL_EXP_AMOUNT + " REAL, " +
                COL_EXP_PAYER_ID + " INTEGER)";
                
        db.execSQL(createMembersTable);
        db.execSQL(createExpensesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMBERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        // Also drop the old v1 table name just in case
        db.execSQL("DROP TABLE IF EXISTS expense_table"); 
        onCreate(db);
    }

    // --- Member Methods ---
    public boolean insertMember(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_MEMBER_NAME, name);
        long result = db.insert(TABLE_MEMBERS, null, contentValues);
        return result != -1;
    }

    public List<Member> getAllMembers() {
        List<Member> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MEMBERS, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new Member(cursor.getInt(0), cursor.getString(1)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // --- Expense Methods ---
    public boolean insertExpense(String title, double amount, int payerId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_EXP_TITLE, title);
        contentValues.put(COL_EXP_AMOUNT, amount);
        contentValues.put(COL_EXP_PAYER_ID, payerId);
        long result = db.insert(TABLE_EXPENSES, null, contentValues);
        return result != -1;
    }

    public List<Expense> getAllExpenses() {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EXPENSES, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new Expense(cursor.getInt(0), cursor.getString(1), cursor.getDouble(2), cursor.getInt(3)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}
