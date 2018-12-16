package com.luszczyk.remotectrlclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        if(intent.hasExtra("Error")){
            Toast.makeText(getApplicationContext(), intent.getStringExtra("Error"), Toast.LENGTH_LONG).show();
        }
    }

    public void connect(View view) {
        EditText ipEdit = findViewById(R.id.ipText);
        EditText passwordEdit = findViewById(R.id.passwordText);
        String ip = ipEdit.getText().toString();
        String password = passwordEdit.getText().toString();

        Intent intent = new Intent(this, ClientActivity.class);
        intent.putExtra("IP", ip);
        intent.putExtra("Password", password);
        startActivity(intent);
        finish();
    }
}
