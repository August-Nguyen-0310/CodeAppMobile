package com.example.businessmanagement.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.businessmanagement.R;
import com.example.businessmanagement.activities.ChatActivity;
import com.example.businessmanagement.adapters.ChatListAdapter;
import com.example.businessmanagement.models.ChatList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    private RecyclerView      rvChatList;
    private ChatListAdapter   adapter;
    private List<ChatList>    dataList;

    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;
    private String currentUid;
    private String userRole;

    public ChatListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db      = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUid = (user != null) ? user.getUid() : null;

        SharedPreferences pref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userRole = pref.getString("user_role", "Employee");

        rvChatList = view.findViewById(R.id.rvChatList);

        dataList = new ArrayList<>();
        adapter  = new ChatListAdapter(dataList, currentUid);
        rvChatList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChatList.setAdapter(adapter);

        // Nút back
        android.widget.ImageView icBack = view.findViewById(R.id.icBack);
        if (icBack != null) {
            icBack.setOnClickListener(v -> requireActivity().finish());
        }

        // Open ChatActivity when user taps a chat item
        adapter.setOnItemClickListener((chat, position) -> {
            markAsRead(chat.getChatId());
            navigateToChat(chat);
        });

        loadChatsFromFirestore();
    }

    private void markAsRead(String chatId) {
        if (currentUid == null || chatId == null) return;
        db.collection("Chats").document(chatId)
                .update("seenBy", FieldValue.arrayUnion(currentUid));
    }

    private void navigateToChat(ChatList chat) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CHAT_ID,      chat.getChatId());
        intent.putExtra(ChatActivity.EXTRA_TASK_ID,      chat.getTaskId());
        intent.putExtra(ChatActivity.EXTRA_TASK_NAME,    chat.getTaskName());
        intent.putExtra(ChatActivity.EXTRA_PROJECT_NAME, chat.getProjectName());
        startActivity(intent);
    }

    private void loadChatsFromFirestore() {
        if (currentUid == null) {
            Toast.makeText(getContext(), "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        Query query;
        if (userRole.equalsIgnoreCase("Boss")) {
            // Boss can see all chats
            query = db.collection("Chats");
        } else {
            // Employees and Managers only see chats they are members of
            query = db.collection("Chats").whereArrayContains("memberIds", currentUid);
        }

        listenerReg = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Toast.makeText(getContext(),
                        "Lỗi tải danh sách chat: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshots == null) return;

            dataList.clear();
            dataList.addAll(snapshots.toObjects(ChatList.class));
            // Sort client-side: newest message first
            dataList.sort((a, b) -> Long.compare(b.getLastTimestamp(), a.getLastTimestamp()));
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerReg != null) listenerReg.remove();
    }
}