package com.example.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBhelper extends SQLiteOpenHelper {

    static final int db_version = 1;

    public static final String MESSAGE = "MESSAGE";

    public static final String ID = "ID";
    public static final String MSG = "MSG";

    public DBhelper(Context context) {
        super(context, "Records", null, db_version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_TABLE = "CREATE TABLE " + MESSAGE + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + MSG + " TEXT)";
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
         sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MESSAGE);
         onCreate(sqLiteDatabase);
    }
    public void addOne(MsgModel msgModel){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(MSG, msgModel.getMsg());

        long insert = db.insert(MESSAGE, null, contentValues);
        if (insert == -1){
            Log.d("DB_ERROR", "INSERT");
        }
        else
            Log.d("DB_success", "addOne: true");
        db.close();
    }
    public List<MsgModel> listMsg(){
        SQLiteDatabase db = this.getReadableDatabase();
        List<MsgModel> messages = new ArrayList<>();
        String selectQuery = "SELECT * " + "FROM " + MESSAGE;

        Cursor cursor = db.rawQuery(selectQuery, null);
        if(cursor.moveToFirst()){
            do {
                int ID = cursor.getInt(0);
                String MSG = cursor.getString(1);

                MsgModel msgModel = new MsgModel(ID, MSG);
                messages.add(msgModel);
            }while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return messages;
    }
}
