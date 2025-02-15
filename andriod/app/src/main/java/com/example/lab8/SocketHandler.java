package com.example.lab8;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketHandler {
    private static final String SERVER_IP = "192.168.68.115"; // Replace with your server IP
    private static final int SERVER_PORT = 65432;
    private static final int READ_TIMEOUT_MS = 5000; // 读取超时时间
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private String currentEmail;
    private boolean isConnected = false; // 添加连接状态标志

    public SocketHandler(String currentEmail) {
        this.currentEmail = currentEmail;
    }

    public void connect() {
        executor.execute(() -> {
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                socket.setSoTimeout(READ_TIMEOUT_MS); // 设置读取超时
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                isConnected = true;
                Log.d("SocketHandler", "Connected to server");
            } catch (IOException e) {
                Log.e("SocketHandler", "Error connecting to server", e);
                disconnect();
            }
        });
    }

    public void disconnect() {
        executor.execute(() -> {
            try {
                if (out != null) {
                    out.print("exit\n");
                    out.flush();
                }
                if (socket != null) {
                    socket.close();
                    Log.d("SocketHandler", "Disconnected from server");
                }
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Log.e("SocketHandler", "Error disconnecting from server", e);
            } finally {
                socket = null;
                in = null;
                out = null;
                isConnected = false; // 断开连接后重置标志
            }
        });
    }

    public void register(String email, String password, RegisterCallback callback) {
        executor.execute(() -> {
            try {
                String message = "new_user " + email + " " + password;
                if (out != null) {
                    out.print(message + "\n");
                    out.flush();
                }
                String response = readResponse();
                Log.d("SocketHandler", "Registration response: " + response);
                handler.post(() -> callback.onRegisterResponse(response));
            } catch (IOException e) {
                Log.e("SocketHandler", "Error sending registration message", e);
                disconnect();
                handler.post(() -> callback.onRegisterResponse("Registration failed: " + e.getMessage()));
            }
        });
    }


    public void login(String email, String password, LoginCallback callback) {
        executor.execute(() -> {
            try {
                String message = "login " + email + " " + password;
                if (out != null) {
                    out.print(message + "\n");
                    out.flush();
                }
                String response = readResponse();
                Log.d("SocketHandler", "Login response: " + response);
                handler.post(() -> callback.onLoginResponse(response));
            } catch (IOException e) {
                Log.e("SocketHandler", "Error sending login message", e);
                disconnect();
                handler.post(() -> callback.onLoginResponse("Login failed: " + e.getMessage()));
            }
        });
    }


    public String getFriends() {
        try {
            String message = "get_friends " + currentEmail;
            if (out != null) {
                out.print(message + "\n");
                out.flush();
                Log.d("SocketHandler", "Sent get_friends message: " + message); // 添加日志
            } else {
                Log.e("SocketHandler", "Output stream is null, cannot send message");
                return null;
            }
            String response = readResponse();
            Log.d("SocketHandler", "Received get_friends response: " + response); // 添加日志
            return response;
        } catch (IOException e) {
            Log.e("SocketHandler", "Error getting friends", e);
            disconnect();
            return null;
        }
    }

    public void addFriend(String selectedUser) {
        executor.execute(() -> {
            try {
                String message = "add_friend " + currentEmail + " " + selectedUser;
                if (out != null) {
                    out.print(message + "\n");
                    out.flush();
                }
                String response = readResponse();
                Log.d("SocketHandler", "Add friend response: " + response);
            } catch (IOException e) {
                Log.e("SocketHandler", "Error adding friend", e);
                disconnect();
            }
        });
    }

    public void sendMessage(String toUser, String message) {
        executor.execute(() -> {
            try {
                String serverMessage = "send_message " + currentEmail + " " + toUser + " " + message;
                if (out != null) {
                    out.print(serverMessage + "\n");
                    out.flush();
                }
                String response = readResponse();
                Log.d("SocketHandler", "Send message response: " + response);
            } catch (IOException e) {
                Log.e("SocketHandler", "Error sending message", e);
                disconnect();
            }
        });
    }

    public String loadMessage(String toUser) {
        try {
            String message = "load_message " + currentEmail + " " + toUser;
            if (out != null) {
                out.print(message + "\n");
                out.flush();
                Log.d("SocketHandler", "Sent load_message message: " + message); // 添加日志
            } else {
                Log.e("SocketHandler", "Output stream is null, cannot send load_message");
                return null;
            }
            String response = readResponse();
            Log.d("SocketHandler", "Received load_message response: " + response); // 添加日志
            return response;
        } catch (IOException e) {
            Log.e("SocketHandler", "Error loading message", e);
            disconnect();
            return null;
        }
    }

    public String setLanguage(String language) {
        try {
            String message = "language " + currentEmail + " " + language;
            if (out != null) {
                out.print(message + "\n");
                out.flush();
            }
            return readResponse();
        } catch (IOException e) {
            Log.e("SocketHandler", "Error loading message", e);
            disconnect();
            return null;
        }
    }

    private String readResponse() throws IOException {
        if (in == null) {
            return null;
        }
        StringBuilder response = new StringBuilder();
        char[] buffer = new char[1024];
        int bytesRead;
        try {
            bytesRead = in.read(buffer);
            if (bytesRead != -1) {
                response.append(buffer, 0, bytesRead);
            }
        } catch (SocketTimeoutException e) {
            Log.e("SocketHandler", "Read timed out", e);
            return null;
        }
        return response.toString().trim();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public interface LoginCallback {
        void onLoginResponse(String response);
    }
    public interface RegisterCallback {
        void onRegisterResponse(String response);
    }
}