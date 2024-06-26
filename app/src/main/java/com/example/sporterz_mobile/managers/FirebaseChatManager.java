package com.example.sporterz_mobile.managers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.sporterz_mobile.models.Chat;
import com.example.sporterz_mobile.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FirebaseChatManager {

    private static final String TAG = "FirebaseChatManager";
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private String cFullName;
    private String LastMessage;
    private static StorageReference storageReference;
    private ImageView chatImage;

    public FirebaseChatManager() {
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }


    private String getKeyOfLatestMessage(DataSnapshot dataSnapshot) {
        String latestKey = null;
        long latestTimestamp = Long.MIN_VALUE;

        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
            Message message = messageSnapshot.getValue(Message.class);
            if (message != null && message.getTimestamp() > latestTimestamp) {
                latestTimestamp = message.getTimestamp();
                latestKey = messageSnapshot.getKey();
            }
        }

        return latestKey;
    }

    // Get other user image
    private void getChatImage(Chat chat) throws IOException {
        // get the list of participants in the chat, the other user is the one who is not the current user
        // get the user id of the other user
        final String userId = chat.getParticipants().keySet().stream().filter(id -> !id.equals(mCurrentUser.getUid())).findFirst().orElse(null);
        storageReference = FirebaseStorage.getInstance().getReference().child("images/" + userId);
        File localfile = File.createTempFile("tempImage", "jpeg");
        storageReference.getFile(localfile).addOnSuccessListener(taskSnapshot -> {
            Bitmap bitmap = BitmapFactory.decodeFile(localfile.getAbsolutePath());
            chatImage.setImageBitmap(bitmap);
        });
    }

    // Load chats where the current user is a participant
    public void loadChats(final ChatLoadListener listener) {
        if (mCurrentUser == null) {
            Log.e(TAG, "loadChats: Current user is null");
            return;
        }

        Query chatQuery = mDatabase.child("chats").orderByChild("participants/" + mCurrentUser.getUid()).equalTo(true);
        chatQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Chat> chatList = new ArrayList<>();
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    Chat chat = chatSnapshot.getValue(Chat.class);
                    // get the chat id where the user is a participant
                    String chatId = chatSnapshot.getKey();
                    assert chat != null;
                    chat.setChatId(chatId);
                    // get the last message in the chat
                    assert chatId != null;

                    DataSnapshot message = chatSnapshot.child("messages");

                    // get the last message in the in messages
                    String latestMessageKey = getKeyOfLatestMessage(message);

                    if (latestMessageKey != null) {
                        Message latestMessage = message.child(latestMessageKey).getValue(Message.class);
                        if (latestMessage != null) {
                            String messageType = latestMessage.getMessageType();
                            switch (messageType) {
                                case "text":
                                    chat.setLastMessage(latestMessage.getMessageContent());
                                    LastMessage = latestMessage.getMessageContent();
                                    break;
                                case "image":
                                    chat.setLastMessage("Image message");
                                    LastMessage = "Image message";
                                    break;
                                case "voice":
                                    chat.setLastMessage("Voice message");
                                    LastMessage = "Voice message";
                                    break;
                                default:
                                    chat.setLastMessage("Click to view last messages");
                                    LastMessage = "Click to view last messages";
                                    break;
                            }
                            // format and set timestamp of chat
                            long timestampSeconds = latestMessage.getTimestamp() / 1000;
                            Date date = new Date(timestampSeconds * 1000);
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                            // split the date and time by new line
                            sdf.applyPattern("HH:mm\nMMM dd, yyyy");

                            String formattedDate = sdf.format(date);
                            chat.setTimestamp(String.valueOf(formattedDate));

                        }
                    } else {
                        chat.setLastMessage(null);
                    }
                    chatList.add(chat);
                }
                listener.onChatsLoaded(chatList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "loadChats:onCancelled", databaseError.toException());
                listener.onError(databaseError.getMessage());
            }
        });
    }

    // Interface for callback methods
    public interface ChatLoadListener {
        void onChatsLoaded(List<Chat> chatList);

        void onError(String errorMessage);
    }
}