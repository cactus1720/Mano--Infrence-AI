package com.example.mano;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etMessage;
    private FloatingActionButton fabSend;
    private ImageView ivSettings;
    private ChatAdapter adapter;
    private List<ChatMessage> messages;
    private OkHttpClient httpClient;
    private DatabaseHelper dbHelper;

    // Groq OpenAI-compatible API Configuration
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String API_KEY = "Bearer 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";
    private static final String MODEL_NAME = "llama3-8b-8192";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        httpClient = new OkHttpClient();
        dbHelper = new DatabaseHelper(this);

        // Bind UI Elements
        rvChat = findViewById(R.id.rv_chat);
        etMessage = findViewById(R.id.et_message);
        fabSend = findViewById(R.id.fab_send);
        ivSettings = findViewById(R.id.iv_settings);


        messages = dbHelper.getAllMessages();

        // Welcome greeting fallback if history table is blank
        if (messages.isEmpty()) {
            messages.add(new ChatMessage("Hi! I'm mano. How can I help you today?", false));
        }

        // Bind Chat Adapter to RecyclerView
        adapter = new ChatAdapter(messages);
        rvChat.setAdapter(adapter);
        rvChat.scrollToPosition(messages.size() - 1);

        // Send Button Click Handler
        fabSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userText = etMessage.getText().toString().trim();
                if (!TextUtils.isEmpty(userText)) {
                    sendMessageToMano(userText);
                }
            }
        });

        // Settings Button Click Handler
        ivSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });
    }

    private void showSettingsDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvFaq = sheetView.findViewById(R.id.tv_faq);
        TextView tvContactSupport = sheetView.findViewById(R.id.tv_contact_support);
        TextView tvClearHistory = sheetView.findViewById(R.id.tv_clear_history);

        // FAQ Popup Alert
        tvFaq.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Frequently Asked Questions")
                    .setMessage("Q: Is my information private?\n" +
                            "A: Yes, conversation histories stay directly on your local device platform.\n\n" +
                            "Q: Does this require internet?\n" +
                            "A: Yes, it targets remote API pathways to serve lightning-fast SLM edge operations.")
                    .setPositiveButton("Close", null)
                    .show();
        });

        // Email Support Intent Gateway
        tvContactSupport.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@manoapp.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Mano App Support Feedback");

            try {
                startActivity(Intent.createChooser(emailIntent, "Open with..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(MainActivity.this, "No email client software found.", Toast.LENGTH_SHORT).show();
            }
        });

        // Clear Chat History SQL Command Handler
        tvClearHistory.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to clear all chat histories?")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        dbHelper.clearChatHistory();
                        messages.clear();
                        messages.add(new ChatMessage("Hi! I'm mano. How can I help you today?", false));
                        adapter.notifyDataSetChanged();
                        bottomSheetDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Chat history deleted.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        bottomSheetDialog.show();
    }

    private void sendMessageToMano(String userText) {
        // 1. Save and Render User Bubble
        dbHelper.insertMessage(userText, true);
        messages.add(new ChatMessage(userText, true));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
        etMessage.setText("");

        // 2. Add Loading State/Indicator
        messages.add(new ChatMessage(true));
        final int loadingIndex = messages.size() - 1;
        adapter.notifyItemInserted(loadingIndex);
        rvChat.scrollToPosition(loadingIndex);

        //  API Request Payload
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", MODEL_NAME);

            JSONArray messagesArray = new JSONArray();
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            messageObject.put("content", userText);
            messagesArray.put(messageObject);
            jsonBody.put("messages", messagesArray);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", API_KEY)
                    .post(body)
                    .build();

            // 4.  Network Call Execution
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> handleError(loadingIndex, "Network connection failure. Try again."));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            String replyText = jsonResponse.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");

                            runOnUiThread(() -> {
                                // Save Assistant response to the history table
                                dbHelper.insertMessage(replyText.trim(), false);

                                // Erase loading animation and substitute text response
                                messages.remove(loadingIndex);
                                messages.add(new ChatMessage(replyText.trim(), false));
                                adapter.notifyDataSetChanged();
                                rvChat.scrollToPosition(messages.size() - 1);
                            });
                        } else {
                            runOnUiThread(() -> handleError(loadingIndex, "Inference server error: " + response.code()));
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> handleError(loadingIndex, "Parsing failure. Check format patterns."));
                    }
                }
            });

        } catch (Exception e) {
            handleError(loadingIndex, "Payload initialization exception occurred.");
        }
    }

    private void handleError(int targetIndex, String errorMessage) {
        if (targetIndex < messages.size()) {
            messages.remove(targetIndex);
        }
        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        adapter.notifyDataSetChanged();
    }
}