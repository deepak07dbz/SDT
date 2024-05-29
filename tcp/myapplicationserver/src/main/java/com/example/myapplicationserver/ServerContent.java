package com.example.myapplicationserver;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import androidx.annotation.Nullable;

public class ServerContent extends ContentProvider {
    public ServerContent() {
    }
    //database
    SQLiteDatabase db;
    private static final String DB_NAME = "SERVER_DATA";
    private static final String TABLE_NAME = "TEXT";
    private static final int DB_VER = 1;

    //columns
    private static final String ID = "ID";
    private static final String MESSAGE = "MESSAGE";
    public static final String AUTHORITY = "com.example.myapplicationserver";
    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+TABLE_NAME);

    //uri matcher
    static int TEXT = 1;
    static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY,TABLE_NAME,TEXT);
    }

    private class Helper extends SQLiteOpenHelper{

        public Helper(Context context) {
            super(context, DB_NAME, null, DB_VER);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + MESSAGE + " TEXT)";
            sqLiteDatabase.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(sqLiteDatabase);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        db.delete(TABLE_NAME, null, null);
        getContext().getContentResolver().notifyChange(uri, null);
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long row = db.insert(TABLE_NAME, null, values);
        if(row > 0) {
            uri = ContentUris.withAppendedId(CONTENT_URI, row);
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        uriMatcher.addURI(AUTHORITY,TABLE_NAME,TEXT);
        Helper helper = new Helper(getContext());
        db = helper.getWritableDatabase();
        if(db != null)
            return true;
        else
            return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cr = null;
        switch(uriMatcher.match(uri)){
            case 1:
                SQLiteQueryBuilder myQuery = new SQLiteQueryBuilder();
                myQuery.setTables(TABLE_NAME);
                cr = myQuery.query(db, null,null,null,null, null, ID);
                cr.setNotificationUri(getContext().getContentResolver(),uri);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI:" + uri.toString());
        }
        return cr;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}