package com.example.lab8;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Register extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private Button registerButton;
    private SocketHandler socketHandler;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(Register.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return; // Exit early if fields are empty
            }

            registerUser(email, password);
        });
    }

    private void registerUser(String email, String password) {
        executor.execute(() -> {
            try {
                socketHandler = new SocketHandler(email); // Initialize SocketHandler with email
                socketHandler.connect(); // Connect to the server
                socketHandler.register(email, password, new SocketHandler.RegisterCallback() {
                    @Override
                    public void onRegisterResponse(String response) {
                        runOnUiThread(() -> handleRegistrationResponse(response, email));
                    }
                });
            } catch (Exception e) {
                Log.e("Register", "Error during registration", e);
                runOnUiThread(() -> Toast.makeText(Register.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                socketHandler.disconnect();
            }
        });
    }


    private void handleRegistrationResponse(String response, String email) {
        if (response != null && response.equals("successful")) {
            Toast.makeText(Register.this, "Registration successful!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Register.this, MainActivity.class);
            intent.putExtra("email", email); // Pass email to MainActivity
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(Register.this, "Registration failed: " + response, Toast.LENGTH_SHORT).show();
        }
    }
}