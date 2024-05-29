package com.example.myapplicationserver;

import android.content.ContentValues;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.goterl.lazysodium.LazySodiumAndroid;
import com.goterl.lazysodium.SodiumAndroid;
import com.goterl.lazysodium.interfaces.Box;
import com.goterl.lazysodium.interfaces.SecretBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivityServer extends AppCompatActivity {

    public static final int msgSize = 8;
    TextView tvIP, tvPort, tvStatus, heading;
    private int serverPort = 1234;
    ContentValues values = new ContentValues();
    private ServerThread serverThread;

    //sodium initialization
    SodiumAndroid sodiumAndroid = new SodiumAndroid();
    LazySodiumAndroid lazySodiumAndroid = new LazySodiumAndroid(sodiumAndroid, StandardCharsets.UTF_8);
    SecretBox.Native secretBoxNative = lazySodiumAndroid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_server);

        tvIP = findViewById(R.id.textView);
        tvPort = findViewById(R.id.textView2);
        tvStatus = findViewById(R.id.textView3);
        heading = findViewById(R.id.textView4);

        WifiManager wifiMan = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));

        tvIP.setText(ip);
        tvPort.setText(String.valueOf(serverPort));
    }

    private byte[] generateKey (){
        //secret key generation
        byte[] secretKey = new byte[SecretBox.KEYBYTES];
        secretBoxNative.cryptoSecretBoxKeygen(secretKey);
        return secretKey;
    }
    public boolean decryption(byte[] message, byte[] cipherText, long cipherTextLen, byte[] nonce, byte[] key){
        return lazySodiumAndroid.cryptoSecretBoxOpenEasy(message, cipherText, cipherTextLen, nonce, key);
    }
    public void onClickStartServer(View view) {
        serverThread = new ServerThread();
        serverThread.startServer();
    }
    public void onClickStopServer(View view) {
        serverThread.stopServer();
    }
    class ServerThread extends Thread implements Runnable{
        private boolean serverRunning;
        private ServerSocket serverSocket;
        public void startServer(){
            serverRunning = true;
            start();
        }
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(serverPort));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Waiting for clients");
                    }
                });
                while (serverRunning){
                    Socket socket = serverSocket.accept();
                    //send signal to sender
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.write("READY".getBytes());
                    out.flush();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText("Connected to: " + socket.getInetAddress() + " : " + socket.getLocalPort());
                        }
                    });

                    //secret key generation
                    byte[] secretKey = generateKey();

                    //receive public key
                    byte[] publicKey = new byte[SecretBox.KEYBYTES];
                    InputStream is = new DataInputStream(socket.getInputStream());
                    long count;
                    is.read(publicKey);

                    //encrypt secret key
                    byte[] cipherSK = new byte[Box.SEALBYTES + SecretBox.KEYBYTES];
                    if(!lazySodiumAndroid.cryptoBoxSeal(cipherSK, secretKey, SecretBox.KEYBYTES, publicKey))
                        Log.d("encryption", "secret key encryption failed");

                    //send secret key
                    out.write(cipherSK);
                    out.flush();

                    // receive nonce + size of message
                    byte[] context = new byte[SecretBox.NONCEBYTES + msgSize];
                    is.read(context);

                    byte[] nonce = new byte[SecretBox.NONCEBYTES];
                    System.arraycopy(context, 0, nonce, 0, SecretBox.NONCEBYTES);
                    byte[] sizeBytes = new byte[msgSize];
                    System.arraycopy(context, SecretBox.NONCEBYTES, sizeBytes, 0, msgSize);
                    long size = ByteBuffer.wrap(sizeBytes).getLong();

                    //receive encrypted message
                    List<Byte> data = new ArrayList<>();
                    while((count = is.read()) != -1){
                        data.add((byte) count);
                    }
                    byte[] dataBytes = new byte[data.size()];
                    for (int i = 0; i<data.size(); i++){
                        dataBytes[i] = data.get(i);
                    }

                    //decrypt and store in database in string format
                    byte[] output = new byte[data.size()];

                    if(!decryption(output, dataBytes, dataBytes.length, nonce, secretKey))
                        Log.d("decryption", "message decryption failed");

                    String txtFromClient = new String(output, StandardCharsets.UTF_8);
                    values.put("MESSAGE", txtFromClient);
                    getContentResolver().insert(ServerContent.CONTENT_URI, values);

                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void stopServer(){
            serverRunning = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(serverSocket!=null){
                        try {
                            serverSocket.close();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tvStatus.setText("Server stopped");
                                }
                            });
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                    }

                }
            }).start();
        }
    }
}
