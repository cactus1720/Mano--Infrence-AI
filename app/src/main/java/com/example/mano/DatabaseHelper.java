package com.example.mano;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ManoUser.db";
    private static final int DATABASE_VERSION = 2;

    // 1. User table
    private static final String TABLE_USERS = "users";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";

    // 2. Chat History
    private static final String TABLE_CHAT = "chat_history";
    private static final String COL_CHAT_ID = "chat_id";
    private static final String COL_MESSAGE = "message_text";
    private static final String COL_IS_USER = "is_user";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // hii User Authentication Table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_NAME + " TEXT,"
                + COL_EMAIL + " TEXT UNIQUE,"
                + COL_PASSWORD + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // hapa Chat History Table
        String CREATE_CHAT_TABLE = "CREATE TABLE " + TABLE_CHAT + "("
                + COL_CHAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_MESSAGE + " TEXT,"
                + COL_IS_USER + " INTEGER" + ")";
        db.execSQL(CREATE_CHAT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drops older tables structure during development upgrades
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHAT);
        onCreate(db);
    }



    // (Signup)
    public boolean registerUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_EMAIL, email.trim().toLowerCase());
        values.put(COL_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }


    public boolean checkUserCredentials(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COL_EMAIL + " = ?" + " AND " + COL_PASSWORD + " = ?";
        String[] selectionArgs = { email.trim().toLowerCase(), password };

        Cursor cursor = db.query(TABLE_USERS, null, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();

        return count > 0;
    }



    // Save a single message bubble to database
    public void insertMessage(String text, boolean isUser) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MESSAGE, text);
        values.put(COL_IS_USER, isUser ? 1 : 0);
        db.insert(TABLE_CHAT, null, values);
        db.close();
    }

    // Load all conversations out of disk memory sequentially
    public List<ChatMessage> getAllMessages() {
        List<ChatMessage> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CHAT + " ORDER BY " + COL_CHAT_ID + " ASC", null);

        if (cursor.moveToFirst()) {
            do {
                String text = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE));
                int isUserInt = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_USER));

                // Reconstruct ChatMessage objects using text string and boolean status flag
                historyList.add(new ChatMessage(text, isUserInt == 1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return historyList;
    }


    public void clearChatHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_CHAT);
        db.close();
    }
}