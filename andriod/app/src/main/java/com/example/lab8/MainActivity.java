package com.example.lab8;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.lab8.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private ArrayAdapter<String> userAdapter;
    private List<String> userList;
    private ActivityMainBinding binding;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private HomeFragment homeFragment;
    private SocketHandler socketHandler; // 使用自定义的 SocketHandler
    private String currentEmail;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userList = new ArrayList<>();
        userAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);

        bottomNavigationView = binding.navView;
        toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            currentEmail = intent.getStringExtra("email");
            // 使用传递的 email 初始化 SocketHandler
        } else {
            Intent loginIntent = new Intent(MainActivity.this, Login.class);
            startActivity(loginIntent);
            finish();
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                loadHomeFragment();
                return true;
            } else if (item.getItemId() == R.id.navigation_settings) {
                openSettings();
                return true;
            }
            return false;
        });

        loadHomeFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            currentEmail = intent.getStringExtra("email");
        }
        socketHandler = new SocketHandler(currentEmail); // 在 onResume 中重新初始化 SocketHandler
        socketHandler.connect();
        waitForConnectionAndLoadUsers(); // 在 onResume 中加载用户
    }

    private void waitForConnectionAndLoadUsers() {
        executor.execute(() -> {
            while (!socketHandler.isConnected()) {
                try {
                    Thread.sleep(100); // 等待 100 毫秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            loadUsers(); // 连接成功后加载用户
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketHandler != null) {
            socketHandler.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsers(newText);
                return true;
            }
        });
        return true;
    }

    private void loadHomeFragment() {
        homeFragment = new HomeFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, homeFragment);
        transaction.commit();
    }

    private void loadUsers() {
        executor.execute(() -> {
            String response = socketHandler.getFriends();
            handler.post(() -> {
                if (response != null) {
                    userList.clear();
                    String[] friends = response.split(" ");
                    for (String friend : friends) {
                        if (!friend.isEmpty()) {
                            userList.add(friend);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                    if (homeFragment != null) {
                        homeFragment.updateUserList(userList);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Error loading users", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void openSettings() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        intent.putExtra("email", currentEmail); // 传递 email
        startActivity(intent);
    }

    private void filterUsers(String query) {
        if (homeFragment != null) {
            homeFragment.filterUsers(query);
        }
    }

    public static class HomeFragment extends Fragment {
        private com.example.lab8.databinding.FragmentHomeBinding binding;
        private UserAdapter userAdapter;
        private List<String> originalUserList;
        private SocketHandler socketHandler; // 使用自定义的 SocketHandler
        private ExecutorService executor = Executors.newSingleThreadExecutor();
        private Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
            binding = com.example.lab8.databinding.FragmentHomeBinding.inflate(inflater, container, false);
            View view = binding.getRoot();

            originalUserList = new ArrayList<>();
            userAdapter = new UserAdapter(getContext(), new ArrayList<>());
            binding.userListView.setAdapter(userAdapter);

            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                socketHandler = mainActivity.socketHandler;
            }

            binding.userListView.setOnItemClickListener((parent, view1, position, id) -> {
                String selectedUser = userAdapter.getItem(position);
                startChat(selectedUser);
            });
            return view;
        }

        public void updateUserList(List<String> userList) {
            originalUserList.clear();
            originalUserList.addAll(userList);
            userAdapter.clear();
            userAdapter.addAll(userList);
            userAdapter.notifyDataSetChanged();
        }

        public void filterUsers(String query) {
            List<String> filteredList = new ArrayList<>();
            if (query.isEmpty()) {
                filteredList.addAll(originalUserList);
            } else {
                for (String user : originalUserList) {
                    if (user.toLowerCase().contains(query.toLowerCase())) {
                        filteredList.add(user);
                    }
                }
            }
            userAdapter.clear();
            userAdapter.addAll(filteredList);
            userAdapter.setQuery(query);
            userAdapter.notifyDataSetChanged();
        }

        private void startChat(String selectedUser) {
            executor.execute(() -> {
                if (socketHandler != null) {
                    socketHandler.addFriend(selectedUser);
                }
                handler.post(() -> {
                    Intent intent = new Intent(getContext(), Chat.class);
                    intent.putExtra("selectedUser", selectedUser);
                    intent.putExtra("currentEmail", ((MainActivity)getActivity()).currentEmail);
                    startActivity(intent);
                });
            });
        }

        private static class UserAdapter extends ArrayAdapter<String> {
            private String query = "";

            public UserAdapter(@NonNull Context context, @NonNull List<String> objects) {
                super(context, android.R.layout.simple_list_item_1, objects);
            }

            public void setQuery(String query) {
                this.query = query;
            }

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                String item = getItem(position);
                if (item != null) {
                    SpannableString spannableString = new SpannableString(item);
                    if (!query.isEmpty() && item.toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))) {
                        int startIndex = item.toLowerCase(Locale.getDefault()).indexOf(query.toLowerCase(Locale.getDefault()));
                        int endIndex = startIndex + query.length();
                        spannableString.setSpan(new BackgroundColorSpan(Color.YELLOW), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    textView.setText(spannableString);
                }
                return view;
            }
        }
    }
}