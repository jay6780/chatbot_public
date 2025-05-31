package com.cm.chatgpt.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cm.chatgpt.AppConstant;
import com.cm.chatgpt.Class.ChatMessageDao;
import com.cm.chatgpt.Class.DeleteAll_listerner;
import com.cm.chatgpt.R;
import com.cm.chatgpt.SPUtils;
import com.cm.chatgpt.Class.ChatMessage;
import com.daasuu.bl.BubbleLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import app.m4ntis.blinkingloader.BlinkingLoader;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private Context context;
    private List<ChatMessage> messageList = new ArrayList<>();
    private boolean showLoading = true;
    private ChatMessageDao chatMessageDao;
    private Executor databaseExecutor;
    private DeleteAll_listerner showdefault;

    public ChatAdapter(Context context, ChatMessageDao chatMessageDao,Executor databaseExecutor,DeleteAll_listerner showdefault) {
        this.context = context;
        this.chatMessageDao = chatMessageDao;
        this.databaseExecutor = databaseExecutor;
        this.showdefault = showdefault;
    }

    public void addMessage(ChatMessage message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }
    public void loadMessages(List<ChatMessage> messages) {
        messageList.clear();
        messageList.addAll(messages);
        notifyDataSetChanged();
    }

    public void isShow(boolean b) {
        showLoading = b;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userMessage, botMessage;
        BubbleLayout bot_bubble,user_bubble;
        ImageView userAvatar,aiavatar;
        BlinkingLoader dotLoading1;
        public ViewHolder(View view) {
            super(view);
            userMessage = view.findViewById(R.id.userMessage);
            botMessage = view.findViewById(R.id.botMessage);
            bot_bubble = view.findViewById(R.id.bot_bubble);
            user_bubble = view.findViewById(R.id.user_bubble);
            userAvatar = view.findViewById(R.id.userAvatar);
            aiavatar = view.findViewById(R.id.aiavatar);
            dotLoading1 = view.findViewById(R.id.dotLoading1);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ChatMessage chatMessage = messageList.get(position);
        String gender = SPUtils.getInstance().getString(AppConstant.Mygender);
        int mipmap = SPUtils.getInstance().getInt(AppConstant.AvatarValue);

        if(gender.equalsIgnoreCase("Male")){
            Glide.with(context)
                    .load(mipmap)
                    .circleCrop()
                    .into(holder.userAvatar);
        }else if (gender.equalsIgnoreCase("Female")){
            Glide.with(context)
                    .load(mipmap)
                    .circleCrop()
                    .into(holder.userAvatar);
        }
        holder.user_bubble.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showDialog(context,chatMessage,position);
                return true;
            }
        });

        holder.bot_bubble.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showDialog(context,chatMessage,position);
                return true;
            }
        });



        if (showLoading && position == messageList.size() - 1) {
            holder.dotLoading1.setVisibility(View.VISIBLE);
            holder.aiavatar.setVisibility(View.VISIBLE);
        }else{
            holder.aiavatar.setVisibility(View.GONE);
            holder.dotLoading1.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(R.mipmap.robot_img)
                .circleCrop()
                .into(holder.aiavatar);

        if (chatMessage.isUser()) {
            holder.userAvatar.setVisibility(View.VISIBLE);
            holder.user_bubble.setVisibility(View.VISIBLE);
            holder.userMessage.setText(chatMessage.getMessage());
            holder.bot_bubble.setVisibility(View.GONE);
        } else {
            holder.aiavatar.setVisibility(View.VISIBLE);
            holder.userAvatar.setVisibility(View.GONE);
            holder.bot_bubble.setVisibility(View.VISIBLE);
            holder.botMessage.setText(chatMessage.getMessage());
            holder.user_bubble.setVisibility(View.GONE);
        }
    }

    private void showDialog(Context context,ChatMessage chatMessage,int position) {
        String[] colors = {"Copy", "Delete","Delete all"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Option");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(null,chatMessage.getMessage());
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context,"Copied success",Toast.LENGTH_SHORT).show();
                        break;

                    case 1:
                        removeMessage(position,false);
                        Toast.makeText(context, "Delete success", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        removeMessage(position,true);
                        Toast.makeText(context, "Delete All success", Toast.LENGTH_SHORT).show();
                        break;

                }
            }
        });
        builder.show();
    }
    public void removeMessage(int position,boolean isDeleteAll) {
        Log.d("DeleteAll","VALUE: "+isDeleteAll);
        if (position >= 0 && position < messageList.size()) {
            ChatMessage message = messageList.get(position);
            if(isDeleteAll){
                databaseExecutor.execute(() -> {
                    chatMessageDao.deleteAll();
                    if(context instanceof Activity){
                        (((Activity)context)).runOnUiThread(() -> {
                            messageList.clear();
                            notifyDataSetChanged();
                            showdefault.showBot();
                        });
                    }
                });
            }else{
                databaseExecutor.execute(() -> {
                    chatMessageDao.delete(message);
                });
                messageList.remove(position);
                notifyItemRemoved(position);
            }

        }
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }
}
