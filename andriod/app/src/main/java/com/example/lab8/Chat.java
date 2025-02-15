package com.example.lab8;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Chat extends AppCompatActivity {
    private LinearLayout chatLayout;
    private EditText messageArea;
    private Button sendButton;
    private String currentEmail;
    private String selectedUser;
    private SocketHandler socketHandler;
    private Toolbar toolbar;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    private ImageView iv_mic;
    private TextView tv_Speech_to_text;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> navigateToMain());

        chatLayout = findViewById(R.id.chatLayout);
        messageArea = findViewById(R.id.messageArea);
        sendButton = findViewById(R.id.buttonChat);
        iv_mic = findViewById(R.id.iv_mic);
        tv_Speech_to_text = findViewById(R.id.tv_speech_to_text);

        iv_mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent
                        = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                        Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text");

                try {
                    startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
                }
                catch (Exception e) {
                    Toast
                            .makeText(Chat.this, " " + e.getMessage(),
                                    Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        // Get data from intent
        Intent intent = getIntent();
        if (intent != null) {
            currentEmail = intent.getStringExtra("currentEmail");
            selectedUser = intent.getStringExtra("selectedUser");
        }

        if (currentEmail != null) {
            socketHandler = new SocketHandler(currentEmail);
            socketHandler.connect();
            waitForConnectionAndLoadMessages(); // 等待连接完成并加载消息
        }

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void waitForConnectionAndLoadMessages() {
        executor.execute(() -> {
            while (!socketHandler.isConnected()) {
                try {
                    Thread.sleep(100); // 等待 100 毫秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            loadMessages(); // 连接成功后加载消息
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(Chat.this, MainActivity.class);
        // Use FLAG_ACTIVITY_CLEAR_TOP to avoid creating a new instance of MainActivity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void sendMessage() {
        String messageText = messageArea.getText().toString().trim();
        if (!messageText.isEmpty()) {
            String user = currentEmail;
            displayMessage(user, messageText);
            executor.execute(() -> {
                socketHandler.sendMessage(selectedUser, messageText);
                handler.post(() -> messageArea.setText(""));
                loadMessages();
            });
        }
    }

    private void loadMessages() {
        executor.execute(() -> {
            String response = socketHandler.loadMessage(selectedUser);
            handler.post(() -> {
                if (response != null) {
                    displayMessages(response);
                } else {
                    Toast.makeText(Chat.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void displayMessages(String response) {
        chatLayout.removeAllViews(); // Clear existing messages
        String[] messages = response.split("\n");
        for (String message : messages) {
            if (!message.isEmpty()) {
                String[] parts = message.split(" ", 2);
                if (parts.length == 2) {
                    String user = parts[0];
                    String messageText = parts[1];
                    if (user.startsWith("from_")) {
                        user = user.substring(5); // Remove "from_" prefix
                    }
                    displayMessage(user, messageText);
                    Log.d("Chat", "user: " + user);
                } else {
                    Log.e("Chat", "Invalid message format: " + message);
                }
            }
        }
    }

    private void displayMessage(String user, String message) {
        boolean isCurrentUser = user.equals(currentEmail);

        // Inflate the appropriate layout
        View messageView;
        if (isCurrentUser) {
            messageView = LayoutInflater.from(this).inflate(R.layout.chat_message_right, chatLayout, false);
        } else {
            messageView = LayoutInflater.from(this).inflate(R.layout.chat_message_left, chatLayout, false);
        }

        // Set message text
        TextView messageTextView = messageView.findViewById(R.id.messageTextView);
        messageTextView.setText(message);

        // Set username text
        TextView userTextView = messageView.findViewById(R.id.userTextView);
        userTextView.setText(user);

        // Add message view to chat layout
        chatLayout.addView(messageView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                tv_Speech_to_text.setText(
                        Objects.requireNonNull(result).get(0));
            }
        }
    }
}