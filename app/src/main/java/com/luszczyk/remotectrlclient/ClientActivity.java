package com.luszczyk.remotectrlclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class ClientActivity extends AppCompatActivity {
    private BufferedReader in;
    private PrintWriter out;
    private SecretKey secretKey;
    private TextView serverView;
    private EditText editText;
    private InetAddress inetAddress;
    private Intent errorIntent;
    private String IP;
    private Socket clientSocket;
    private Boolean interrupted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        errorIntent = new Intent(this, MainActivity.class);
        serverView = findViewById(R.id.serverView);
        serverView.setMovementMethod(new ScrollingMovementMethod());
        editText = findViewById(R.id.editText);
        Intent intent = getIntent();
        IP = intent.getStringExtra("IP");
        String password = intent.getStringExtra("Password");

        byte[] key = password.getBytes();
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert sha != null;
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // use only first 128 bit
        secretKey = new SecretKeySpec(key, "AES");

        new Reciever().start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        startActivity(errorIntent);
        finish();
    }
    @Override
    public void onDestroy() {
        interrupted = true;
        super.onDestroy();
    }

    public void send(View view) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        String message = MSGEncrypt.AESEncrypt(editText.getText().toString(), secretKey);
        new Sender(message).start();
    }

    private class Reciever extends Thread{
        public void run(){
            String inputLine;
            ProgressBar pDialog = findViewById(R.id.progressBar);
            pDialog.setIndeterminate(true);
            pDialog.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            try {
                inetAddress = InetAddress.getByName(IP);
            } catch (Exception e) {
                returnWithError("Error: Unable to parse IP address");
            }
            try {
                clientSocket = new Socket(inetAddress, 24771);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (Exception e) {
                e.printStackTrace();
                returnWithError("Error: Connection refused");
            }
            try {
                out.println(MSGEncrypt.AESEncrypt("echo Welcome",secretKey));
            } catch (Exception e) {
                e.printStackTrace();
            }

            try{
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                pDialog.setVisibility(View.GONE);
                findViewById(R.id.connectText).setVisibility(View.GONE);
            } catch(Exception e){
                returnWithError("Error: Connection refused");
            }
            try{
                while (!interrupted){
                    inputLine = in.readLine();
                    try{
                        inputLine = MSGEncrypt.AESDecrypt(inputLine,secretKey);
                        serverView.setText(serverView.getText().toString() + inputLine + "\n");
                    } catch(Exception e){
                        returnWithError("Error: Bad password");
                        break;
                    }
                }
            } catch (Exception e){
                returnWithError("Error: Server disconnected");
            }
        }
    }

    private class Sender extends Thread{
        private String message;
        Sender(String message){
            this.message = message;
        }

        public void run(){
            out.println(message);
        }
    }

    private void returnWithError(String error){
        if(!interrupted){
            errorIntent.putExtra("Error", error);
            startActivity(errorIntent);
        }
        try {
            if(clientSocket!=null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        interrupted = true;
        finish();
    }
}
