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
    private static final int DATABASE_VERSION = 3; // Upgraded for relation tables!

    // Tables
    public static final String TABLE_MEMBERS = "members_table";
    public static final String TABLE_EXPENSES = "expense_table";
    public static final String TABLE_EXPENSE_MEMBERS = "expense_members_table";

    // Columns
    public static final String COL_ID = "ID";
    public static final String COL_NAME = "NAME";
    public static final String COL_TITLE = "TITLE";
    public static final String COL_AMOUNT = "AMOUNT";
    public static final String COL_PAYER_ID = "PAYER_ID";
    public static final String COL_EXP_ID_FK = "EXPENSE_ID";
    public static final String COL_MEM_ID_FK = "MEMBER_ID";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_MEMBERS + " (" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_NAME + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_EXPENSES + " (" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_TITLE + " TEXT, " + COL_AMOUNT + " REAL, " + COL_PAYER_ID + " INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_EXPENSE_MEMBERS + " (" + COL_EXP_ID_FK + " INTEGER, " + COL_MEM_ID_FK + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMBERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSE_MEMBERS);
        onCreate(db);
    }

    public void insertMember(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME, name);
        db.insert(TABLE_MEMBERS, null, cv);
    }

    public List<Member> getAllMembers() {
        List<Member> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MEMBERS, null);
        if (cursor.moveToFirst()) {
            do { list.add(new Member(cursor.getInt(0), cursor.getString(1))); } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public long insertExpense(String title, double amount, int payerId, List<Integer> involvedMemberIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, title);
        cv.put(COL_AMOUNT, amount);
        cv.put(COL_PAYER_ID, payerId);
        
        long expenseId = db.insert(TABLE_EXPENSES, null, cv); // Returns the new ID

        // Save who was involved
        for (int memberId : involvedMemberIds) {
            ContentValues linkCv = new ContentValues();
            linkCv.put(COL_EXP_ID_FK, expenseId);
            linkCv.put(COL_MEM_ID_FK, memberId);
            db.insert(TABLE_EXPENSE_MEMBERS, null, linkCv);
        }
        return expenseId;
    }

    public List<Expense> getFullExpenses() {
        List<Expense> expenseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT e.ID, e.TITLE, e.AMOUNT, e.PAYER_ID, m.NAME " +
                       "FROM " + TABLE_EXPENSES + " e " +
                       "JOIN " + TABLE_MEMBERS + " m ON e.PAYER_ID = m.ID";
        
        Cursor eCursor = db.rawQuery(query, null);

        if (eCursor.moveToFirst()) {
            do {
                int expId = eCursor.getInt(0);
                String title = eCursor.getString(1);
                double amount = eCursor.getDouble(2);
                int payerId = eCursor.getInt(3);
                String payerName = eCursor.getString(4);

                // Fetch involved members for this expense
                List<Member> involved = new ArrayList<>();
                String memQuery = "SELECT m.ID, m.NAME FROM " + TABLE_EXPENSE_MEMBERS + " em " +
                                  "JOIN " + TABLE_MEMBERS + " m ON em.MEMBER_ID = m.ID " +
                                  "WHERE em.EXPENSE_ID = ?";
                Cursor mCursor = db.rawQuery(memQuery, new String[]{String.valueOf(expId)});
                if (mCursor.moveToFirst()) {
                    do { involved.add(new Member(mCursor.getInt(0), mCursor.getString(1))); } while (mCursor.moveToNext());
                }
                mCursor.close();

                expenseList.add(new Expense(expId, title, amount, payerId, payerName, involved));
            } while (eCursor.moveToNext());
        }
        eCursor.close();
        return expenseList;
    }
}
