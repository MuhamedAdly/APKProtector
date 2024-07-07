package com.example.protectstringsexample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String str1 = "A Secured ";
        Toast.makeText(this, str1 + "Content Here", Toast.LENGTH_SHORT).show();
    }
}