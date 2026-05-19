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
    private static final int DATABASE_VERSION = 4; // Upgraded for Multiple Trips!

    public static final String TABLE_TRIPS = "trips_table";
    public static final String TABLE_MEMBERS = "members_table";
    public static final String TABLE_EXPENSES = "expense_table";
    public static final String TABLE_EXPENSE_MEMBERS = "expense_members_table";

    public static final String COL_ID = "ID";
    public static final String COL_NAME = "NAME";
    public static final String COL_TITLE = "TITLE";
    public static final String COL_AMOUNT = "AMOUNT";
    public static final String COL_PAYER_ID = "PAYER_ID";
    public static final String COL_TRIP_ID = "TRIP_ID"; // New column to link data!
    public static final String COL_EXP_ID_FK = "EXPENSE_ID";
    public static final String COL_MEM_ID_FK = "MEMBER_ID";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TRIPS + " (" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_NAME + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_MEMBERS + " (" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_TRIP_ID + " INTEGER, " + COL_NAME + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_EXPENSES + " (" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_TRIP_ID + " INTEGER, " + COL_TITLE + " TEXT, " + COL_AMOUNT + " REAL, " + COL_PAYER_ID + " INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_EXPENSE_MEMBERS + " (" + COL_EXP_ID_FK + " INTEGER, " + COL_MEM_ID_FK + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMBERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSE_MEMBERS);
        onCreate(db);
    }

    // --- TRIPS ---
    public void insertTrip(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME, name);
        db.insert(TABLE_TRIPS, null, cv);
    }

    public List<Trip> getAllTrips() {
        List<Trip> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TRIPS, null);
        if (cursor.moveToFirst()) {
            do { list.add(new Trip(cursor.getInt(0), cursor.getString(1))); } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

            public List<Trip> getTripsWithDetails() {
        List<Trip> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // The query now counts members instead of expenses
        String query = "SELECT t." + COL_ID + ", t." + COL_NAME + ", " +
                       "(SELECT GROUP_CONCAT(" + COL_NAME + ", ', ') FROM " + TABLE_MEMBERS + " WHERE " + COL_TRIP_ID + " = t." + COL_ID + ") as Members, " +
                       "(SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES + " WHERE " + COL_TRIP_ID + " = t." + COL_ID + ") as TotalExpense, " +
                       "(SELECT COUNT(" + COL_ID + ") FROM " + TABLE_MEMBERS + " WHERE " + COL_TRIP_ID + " = t." + COL_ID + ") as MemberCount " +
                       "FROM " + TABLE_TRIPS + " t";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String members = cursor.getString(2); 
                double total = cursor.getDouble(3);   
                int count = cursor.getInt(4); // This now holds the Member Count
                
                list.add(new Trip(id, name, members, total, count));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // --- MEMBERS (Now filtered by TRIP_ID) ---
    public void insertMember(int tripId, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TRIP_ID, tripId);
        cv.put(COL_NAME, name);
        db.insert(TABLE_MEMBERS, null, cv);
    }

    public List<Member> getAllMembers(int tripId) {
        List<Member> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MEMBERS + " WHERE " + COL_TRIP_ID + " = ?", new String[]{String.valueOf(tripId)});
        if (cursor.moveToFirst()) {
            do { list.add(new Member(cursor.getInt(0), cursor.getString(2))); } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // --- EXPENSES (Now filtered by TRIP_ID) ---
    public long insertExpense(int tripId, String title, double amount, int payerId, List<Integer> involvedMemberIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TRIP_ID, tripId);
        cv.put(COL_TITLE, title);
        cv.put(COL_AMOUNT, amount);
        cv.put(COL_PAYER_ID, payerId);
        long expenseId = db.insert(TABLE_EXPENSES, null, cv);

        for (int memberId : involvedMemberIds) {
            ContentValues linkCv = new ContentValues();
            linkCv.put(COL_EXP_ID_FK, expenseId);
            linkCv.put(COL_MEM_ID_FK, memberId);
            db.insert(TABLE_EXPENSE_MEMBERS, null, linkCv);
        }
        return expenseId;
    }

    public List<Expense> getFullExpenses(int tripId) {
        List<Expense> expenseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT e.ID, e.TITLE, e.AMOUNT, e.PAYER_ID, m.NAME " +
                       "FROM " + TABLE_EXPENSES + " e " +
                       "JOIN " + TABLE_MEMBERS + " m ON e.PAYER_ID = m.ID " +
                       "WHERE e." + COL_TRIP_ID + " = ?";
        Cursor eCursor = db.rawQuery(query, new String[]{String.valueOf(tripId)});

        if (eCursor.moveToFirst()) {
            do {
                int expId = eCursor.getInt(0);
                String title = eCursor.getString(1);
                double amount = eCursor.getDouble(2);
                int payerId = eCursor.getInt(3);
                String payerName = eCursor.getString(4);

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

    public void deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, COL_ID + " = ?", new String[]{String.valueOf(expenseId)});
        db.delete(TABLE_EXPENSE_MEMBERS, COL_EXP_ID_FK + " = ?", new String[]{String.valueOf(expenseId)});
        db.close();
    }

    public void updateExpense(int expenseId, String title, double amount, int payerId, List<Integer> involvedMemberIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, title);
        cv.put(COL_AMOUNT, amount);
        cv.put(COL_PAYER_ID, payerId);
        db.update(TABLE_EXPENSES, cv, COL_ID + " = ?", new String[]{String.valueOf(expenseId)});
        
        db.delete(TABLE_EXPENSE_MEMBERS, COL_EXP_ID_FK + " = ?", new String[]{String.valueOf(expenseId)});
        for (int memberId : involvedMemberIds) {
            ContentValues linkCv = new ContentValues();
            linkCv.put(COL_EXP_ID_FK, expenseId);
            linkCv.put(COL_MEM_ID_FK, memberId);
            db.insert(TABLE_EXPENSE_MEMBERS, null, linkCv);
        }
        db.close();
    }
}
