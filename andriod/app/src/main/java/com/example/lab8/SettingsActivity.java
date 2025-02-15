package com.example.lab8;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.lab8.databinding.ActivitySettingsBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private BottomNavigationView bottomNavigationView;
    private SocketHandler socketHandler;
    private String currentEmail;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bottomNavigationView = binding.navViewSettings;

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            currentEmail = intent.getStringExtra("email");
            socketHandler = new SocketHandler(currentEmail);
            socketHandler.connect(); // 在 onCreate 中连接
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                navigateToMain();
                return true;
            } else if (item.getItemId() == R.id.navigation_settings) {
                loadSettingsFragment();
                return true;
            }
            return false;
        });

        loadSettingsFragment();
    }

    private void navigateToMain() {
        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
        intent.putExtra("email", currentEmail);
        startActivity(intent);
        finish();
    }

    private void loadSettingsFragment() {
        SettingsFragment settingsFragment = new SettingsFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container_settings, settingsFragment);
        transaction.commit();
    }

    public static class SettingsFragment extends Fragment {
        private com.example.lab8.databinding.FragmentSettingsBinding binding;

        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
            binding = com.example.lab8.databinding.FragmentSettingsBinding.inflate(inflater, container, false);
            View view = binding.getRoot();

            binding.logoutButton.setOnClickListener(v -> ((SettingsActivity)getActivity()).logoutUser());
            binding.languageButton.setOnClickListener(v -> ((SettingsActivity)getActivity()).showLanguageDialog());
            return view;
        }
    }

    private void showLanguageDialog() {
        final String[] languages = {"English", "中文", "日本語"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setItems(languages, (dialog, which) -> {
                    String languageCode;
                    switch (which) {
                        case 0:
                            languageCode = "en";
                            break;
                        case 1:
                            languageCode = "zh-Hant";
                            break;
                        case 2:
                            languageCode = "ja";
                            break;
                        default:
                            languageCode = "en";
                            break;
                    }
                    setLocale(languageCode);
                })
                .show();
    }

    private void setLocale(String languageCode) {
        android.content.res.Resources resources = getResources();
        android.util.DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        android.content.res.Configuration configuration = resources.getConfiguration();
        configuration.setLocale(new java.util.Locale(languageCode));
        resources.updateConfiguration(configuration, displayMetrics);
        onConfigurationChanged(configuration);
        recreate();
        uploadLanguageToServer(languageCode);
    }

    private void uploadLanguageToServer(String languageCode) {
        executor.execute(() -> {
            while (!socketHandler.isConnected()) {
                try {
                    Thread.sleep(100); // 等待 100 毫秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            try {
                String response = socketHandler.setLanguage(languageCode);
                if (response != null && response.equals("Language set successfully")) {
                    handler.post(() -> Toast.makeText(SettingsActivity.this, "Language set successfully", Toast.LENGTH_SHORT).show());
                } else {
                    handler.post(() -> Toast.makeText(SettingsActivity.this, "Failed to set language", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("SettingsActivity", "Error uploading language", e);
                handler.post(() -> Toast.makeText(SettingsActivity.this, "Failed to set language: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void logoutUser() {
        executor.execute(() -> {
            while (!socketHandler.isConnected()) {
                try {
                    Thread.sleep(100); // 等待 100 毫秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            try {
                socketHandler.disconnect();
                handler.post(() -> {
                    Toast.makeText(SettingsActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SettingsActivity.this, Login.class);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(SettingsActivity.this, "Logout failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}