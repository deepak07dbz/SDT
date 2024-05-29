package com.example.tcp;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ListView records;
    ArrayAdapter<StringBuilder> readingArrayAdapter;
    Button list, clear;
    Uri CONTENT_URI = Uri.parse("content://com.example.myapplicationserver/TEXT");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        list = findViewById(R.id.button3);
        clear = findViewById(R.id.button2);
        records = findViewById(R.id.Listview);

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContentResolver().delete(CONTENT_URI, null, null);
            }
        });

        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Cursor cr = getContentResolver().query(CONTENT_URI, null, null, null, "ID");
                if(cr != null) {
                    List<StringBuilder> readings = new ArrayList<>();
                    StringBuilder stringBuilder = new StringBuilder();
                    if (cr.moveToFirst()) {
                        do {
                            int id = cr.getInt(0);
                            String s1 = cr.getString(1);
                            readings.add(new StringBuilder(id + "       " + s1));
                        } while (cr.moveToNext());
                    }
                    readingArrayAdapter = new ArrayAdapter<StringBuilder>(MainActivity.this, android.R.layout.simple_list_item_1, readings);
                    records.setAdapter(readingArrayAdapter);
                    Log.d("DB", "onClick: LIST");
                }
                assert cr != null;
                cr.close();
            }
        });
    }
}
