package com.cm.chatgpt.Class;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.cm.chatgpt.Class.ChatMessage;

import java.util.List;

@Dao
public interface ChatMessageDao {
    @Insert
    void insert(ChatMessage message);
    
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    List<ChatMessage> getAllMessages();
    
    @Query("DELETE FROM chat_messages")
    void deleteAll();

    @Delete
    void delete(ChatMessage message);
}