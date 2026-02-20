package com.example.nodebotdashboard;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("node");
    }

    private static boolean nodeStarted = false;

    public native int startNodeWithArguments(String[] arguments);

    private TextView outputView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        outputView = findViewById(R.id.text_output);
        Button callNodeButton = findViewById(R.id.button_call_node);

        if (!nodeStarted) {
            nodeStarted = true;
            new Thread(() -> {
                String nodeScript =
                        "const http = require('http');" +
                                "const server = http.createServer((req, res) => {" +
                                "  res.end('Hello from Node on Android!');" +
                                "});" +
                                "server.listen(3000, '127.0.0.1');";

                startNodeWithArguments(new String[]{
                        "node",
                        "-e",
                        nodeScript
                });

                Log.i("NodeApp", "Node.js finished.");
            }).start();
        }

        callNodeButton.setOnClickListener(v -> queryNodeServer());
    }

    private void queryNodeServer() {
        new Thread(() -> {
            try {
                URL url = new URL("http://127.0.0.1:3000/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                reader.close();

                String result = builder.toString();
                runOnUiThread(() -> outputView.setText(result));
            } catch (Exception e) {
                Log.e("NodeApp", "Failed to call Node server", e);
                runOnUiThread(
                        () -> outputView.setText("Error: " + e.getMessage())
                );
            }
        }).start();
    }
}
