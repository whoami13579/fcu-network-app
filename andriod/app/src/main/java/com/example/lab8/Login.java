package com.example.lab8;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Login extends AppCompatActivity implements SocketHandler.LoginCallback {

    private EditText emailEditText, passwordEditText;
    private SocketHandler socketHandler;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);

        findViewById(R.id.loginButton).setOnClickListener(v -> loginUser());
        findViewById(R.id.registerButton).setOnClickListener(v -> registerUser());
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        } else if (!email.contains("@")) {
            Toast.makeText(this, "invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                socketHandler = new SocketHandler(email); // Initialize SocketHandler with email
                socketHandler.connect(); // Connect to the server
                socketHandler.login(email, password, Login.this); // Call login with callback
            } catch (Exception e) {
                Log.e("Login", "Error during login", e);
                runOnUiThread(() -> Toast.makeText(Login.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onLoginResponse(String response) {
        if (response != null && response.equals("successful")) {
            Toast.makeText(Login.this, "Login successful!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Login.this, MainActivity.class);
            intent.putExtra("email", emailEditText.getText().toString().trim()); // Pass email to MainActivity
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(Login.this, "Login failed: " + response, Toast.LENGTH_SHORT).show();
        }
    }

    //Jump to registration page
    private void registerUser() {
        startActivity(new Intent(Login.this, Register.class));
    }
}