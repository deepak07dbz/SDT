package com.example.senderclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.goterl.lazysodium.LazySodiumAndroid;
import com.goterl.lazysodium.SodiumAndroid;
import com.goterl.lazysodium.interfaces.Box;
import com.goterl.lazysodium.interfaces.Random;
import com.goterl.lazysodium.interfaces.SecretBox;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    EditText port, ip, msgToSend;
    private String text, serverIP;
    private int serverPort;
    private boolean isStarted;

    //sodium initialization
    SodiumAndroid sodiumAndroid = new SodiumAndroid();
    LazySodiumAndroid lazySodiumAndroid = new LazySodiumAndroid(sodiumAndroid, StandardCharsets.UTF_8);
    SecretBox.Native secretBoxNative = lazySodiumAndroid;
    Box.Native boxNative = lazySodiumAndroid;
    Random random = lazySodiumAndroid;
    byte[] privateKey;
    byte[] publicKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ip = findViewById(R.id.edtTextIP);
        port = findViewById(R.id.edtTextPort);
        msgToSend = findViewById(R.id.edtTextMsg);

        //public and private key generation
        publicKey = new byte[Box.PUBLICKEYBYTES];
        privateKey = new byte[Box.SECRETKEYBYTES];
        boxNative.cryptoBoxKeypair(publicKey, privateKey);

        isStarted = false;
    }
    private boolean decryption(byte[] message, byte[] cipher, long cipherLen, byte[] publicKey, byte[] privateKey) {
        return boxNative.cryptoBoxSealOpen(message, cipher, cipherLen, publicKey, privateKey);
    }

    private boolean encryption(byte[] cipher, byte[] message, long messageLen, byte[] nonce, byte[] key) {
        return secretBoxNative.cryptoSecretBoxEasy(cipher, message, messageLen, nonce, key);
    }

    //sender thread to periodically check whether server is listening or not
    private class SenderThread extends Thread {
        private Socket socket;
        public SenderThread() {
            try {
                socket = new Socket(serverIP, serverPort);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                boolean receiverListening = checkReceiverListening();

                if (receiverListening) {
                    //query messages with SENT == 0
                    sendMessage("fetched from db");
                    //update SENT = 1 of that message entry
                } else {
                    Log.d("Sender", "Receiver is not listening.");
                }
            }
        }

        private void sendMessage(String toSend) {

            try {
                //send public key
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.write(publicKey);
                out.flush();

                //receive secret key
                byte[] secretKeyEnc = new byte[SecretBox.KEYBYTES + Box.SEALBYTES];
                InputStream in = socket.getInputStream();
                int count = in.read(secretKeyEnc);

                //decrypt secret key
                byte[] secretKey = new byte[SecretBox.KEYBYTES];
                if (!decryption(secretKey, secretKeyEnc, secretKeyEnc.length, publicKey, privateKey))
                    Log.d("decryption", "secret key decryption failed");

                //generate nonce
                byte[] nonce = new byte[SecretBox.NONCEBYTES];
                nonce = random.nonce(nonce.length);

                //encrypt msg with secret key
                byte[] msg = toSend.getBytes();
                byte[] msgEnc = new byte[SecretBox.MACBYTES + msg.length];
                if (!encryption(msgEnc, msg, msg.length, nonce, secretKey))
                    Log.d("encryption", "message encryption failed");

                //send nonce + msg size
                long size = msgEnc.length;
                byte[] sizeBytes = ByteBuffer.allocate(Long.BYTES).putLong(size).array();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                os.write(nonce);
                os.write(sizeBytes);
                byte[] context = os.toByteArray();

                out.write(context);
                out.flush();

                //send encrypted msg
                out.write(msgEnc);
                out.flush();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "message sent", Toast.LENGTH_SHORT).show();
                    }
                });
                socket.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        public boolean checkReceiverListening() {
            boolean listening = false;
            try {
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                byte[] signal = new byte[5];
                dataInputStream.read(signal);
                if(Arrays.toString(signal).equals("READY"))
                    listening = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return listening;
        }
    }
    public void btnConnect(View view) {
        serverIP = ip.getText().toString();
        serverPort = Integer.valueOf(port.getText().toString());
        text = msgToSend.getText().toString();

        //insert in db
        ContentValues cv = new ContentValues();
        cv.put("MESSAGE", text);
        cv.put("TIME", new SimpleDateFormat("HH.mm.ss.dd.MM").format(new java.util.Date()));
        cv.put("SENT", 0);
        getContentResolver().insert(ToSend.CONTENT_URI, cv);

        //start sender thread after insertion, if it is not started
        (new Thread(() -> {
            if (!isStarted) {
                SenderThread senderThread = new SenderThread();
                senderThread.start();
                isStarted = true;
            }
        })).start();
    }
}
