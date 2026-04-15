package com.example.businessmanagement.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.businessmanagement.R;
import com.example.businessmanagement.fragments.ChatListFragment;

public class ChatListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new ChatListFragment())
                .commit();
        }
    }
}
