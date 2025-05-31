package com.cm.chatgpt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cm.chatgpt.Adapter.ChatAdapter;
import com.cm.chatgpt.Class.DeleteAll_listerner;
import com.cm.chatgpt.offlineDatabase.AppDatabase;
import com.cm.chatgpt.Class.Avatar;
import com.cm.chatgpt.Adapter.AvatarAdapter;
import com.cm.chatgpt.Class.ChatMessage;
import com.cm.chatgpt.Class.ChatMessageDao;
import com.cm.chatgpt.Class.GenderListener;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ViewHolder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements GenderListener, DeleteAll_listerner {

    private static final String TAG = "MainActivity";
    private static final String API_KEY = "Your api key on api market";
    private RecyclerView chatRecycler;
    private EditText chatEditText;
    private ImageView send;
    private String promptString;
    private ChatAdapter chatAdapter;
    private TextView chatbotTxt;
    private ImageView chatbot_icon;
    private AvatarAdapter avatarAdapter;
    private String Mygender;
    private int AvatarValue;
    private AppDatabase db;
    private ChatMessageDao chatMessageDao;
    private Executor databaseExecutor = Executors.newSingleThreadExecutor();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        changeStatusBarColor(Color.parseColor("#EAEFEF"));
        chatRecycler = findViewById(R.id.chatRecycler);
        chatEditText = findViewById(R.id.chatEditText);
        chatbot_icon = findViewById(R.id.chatbot_icon);
        chatbotTxt = findViewById(R.id.chatbotTxt);
        send = findViewById(R.id.send);
        send.setOnClickListener(view -> sendChatRequest());
        chatRecycler.setLayoutManager(new LinearLayoutManager(this));

        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "chat-database")
                .fallbackToDestructiveMigration()
                .build();
        chatMessageDao = db.chatMessageDao();

        chatAdapter = new ChatAdapter(this,chatMessageDao,databaseExecutor,this);
        chatRecycler.setAdapter(chatAdapter);


        if(SPUtils.getInstance().getString(AppConstant.Mygender).isEmpty()){
            avatarDialog();
        }
        loadSavedMessages();

    }

    private void loadSavedMessages() {
        databaseExecutor.execute(() -> {
            List<ChatMessage> savedMessages = chatMessageDao.getAllMessages();
            runOnUiThread(() -> {
                chatAdapter.loadMessages(savedMessages);
                if (!savedMessages.isEmpty()) {
                    chatRecycler.scrollToPosition(savedMessages.size() - 1);
                    chatRecycler.setVisibility(View.VISIBLE);
                    chatbotTxt.setVisibility(View.GONE);
                    chatbot_icon.setVisibility(View.GONE);
                    chatAdapter.isShow(false);
                }
            });
        });
    }

    private void sendChatRequest() {
        promptString = chatEditText.getText().toString().trim();
        if (promptString.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please enter prompt", Toast.LENGTH_SHORT).show();
            return;
        }
        chatRecycler.setVisibility(View.VISIBLE);
        chatbotTxt.setVisibility(View.GONE);
        chatbot_icon.setVisibility(View.GONE);
        ChatMessage userMessage = new ChatMessage(promptString, true);

        runOnUiThread(() -> {
            chatAdapter.addMessage(userMessage);
            chatAdapter.isShow(true);
            chatRecycler.scrollToPosition(chatAdapter.getItemCount() - 1);
            chatEditText.setText("");
        });
        databaseExecutor.execute(() -> {
            chatMessageDao.insert(userMessage);
            new Thread(() -> {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();

                MediaType mediaType = MediaType.parse("application/json");
                String jsonBody = "{"
                        + "\"model\":\"gpt-4o\","
                        + "\"messages\":[{\"role\":\"user\",\"content\":\"" + promptString.replace("\"", "\\\"") + "\"}]"
                        + "}";

                RequestBody body = RequestBody.create(jsonBody, mediaType);
                Request request = new Request.Builder()
                        .url("https://prod.api.market/api/v1/swift-api/gpt4o/chat/completions")
                        .post(body)
                        .addHeader("x-magicapi-key", API_KEY)
                        .addHeader("content-type", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Response: " + responseBody);

                        JSONObject json = new JSONObject(responseBody);
                        JSONArray choices = json.getJSONArray("choices");
                        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                        String content = message.getString("content").trim();
                        ChatMessage botMessage = new ChatMessage(content, false);

                        runOnUiThread(() -> {
                            chatAdapter.addMessage(botMessage);
                            chatAdapter.isShow(false);
                            chatRecycler.scrollToPosition(chatAdapter.getItemCount() - 1);
                        });

                        databaseExecutor.execute(() -> chatMessageDao.insert(botMessage));
                    } else {
                        Log.e(TAG, "Request failed: " + response.code());
                        runOnUiThread(() -> {
                            chatAdapter.isShow(false);
                            Toast.makeText(getApplicationContext(), "Request failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Network error: ", e);
                    runOnUiThread(() -> {
                        chatAdapter.isShow(false);
                        Toast.makeText(getApplicationContext(), "Network error. Try again.", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
    }

    public void avatarDialog() {
        List<Avatar> avatarList = new ArrayList<>();
        avatarList.add(new Avatar(R.mipmap.male,"Male"));
        avatarList.add(new Avatar(R.mipmap.female,"Female"));
        avatarList.add(new Avatar(R.mipmap.avatar1,"Male"));
        avatarList.add(new Avatar(R.mipmap.avatar3,"Female"));
        avatarList.add(new Avatar(R.mipmap.avatar2,"Male"));
        avatarList.add(new Avatar(R.mipmap.avatar4,"Female"));
        DialogPlus dialog = DialogPlus.newDialog(MainActivity.this)
                .setContentHolder(new ViewHolder(R.layout.gender))
                .setContentWidth(ViewGroup.LayoutParams.MATCH_PARENT)
                .setContentHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
                .setGravity(Gravity.CENTER)
                .setCancelable(false)
                .create();

        View dialogView = dialog.getHolderView();
        Button yes = dialogView.findViewById(R.id.Yes);
        RecyclerView avatarRecycler = dialogView.findViewById(R.id.avatarRecycler);
        avatarRecycler.setLayoutManager(new LinearLayoutManager(this,RecyclerView.HORIZONTAL,false));
        avatarAdapter = new AvatarAdapter(this,avatarList,this);
        avatarRecycler.setAdapter(avatarAdapter);
        yes.setOnClickListener(view -> {
            savedGender(Mygender, dialog, AvatarValue);
        });

        dialog.show();
    }


    private void savedGender(String mygender, DialogPlus dialog,int AvatarValue) {
        if (!mygender.equalsIgnoreCase("Male") && !mygender.equalsIgnoreCase("Female")) {
            Toast.makeText(getApplicationContext(), "Only accepts Male & Female", Toast.LENGTH_SHORT).show();
            return;
        }
        SPUtils.getInstance().put(AppConstant.AvatarValue,AvatarValue);
        SPUtils.getInstance().put(AppConstant.Mygender,mygender);
        dialog.dismiss();
    }

    private void changeStatusBarColor(int color) {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(color);
    }

    @Override
    public void getGender(String gender,int mipmap) {
        Mygender = gender;
        AvatarValue = mipmap;
//        Log.d(TAG,"value: "+Mygender+" "+AvatarValue);
    }

    @Override
    public void showBot() {
        chatbotTxt.setVisibility(View.VISIBLE);
        chatbot_icon.setVisibility(View.VISIBLE);
        chatRecycler.setVisibility(View.GONE);
    }
}