package com.cm.chatgpt.offlineDatabase;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.cm.chatgpt.Class.ChatMessage;
import com.cm.chatgpt.Class.ChatMessageDao;

@Database(entities = {ChatMessage.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ChatMessageDao chatMessageDao();
}