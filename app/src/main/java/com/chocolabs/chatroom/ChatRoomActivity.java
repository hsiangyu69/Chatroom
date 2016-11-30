package com.chocolabs.chatroom;

/**
 * Created by chenpusheng on 2016/8/2.
 */

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatRoomActivity extends BaseActivity {

    private static final String TAG = ChatRoomActivity.class.getSimpleName();

    public final static String EXTRA_PREFIX = ChatRoomActivity.class.getName();
    public final static String EXTRA_ROOM_ID = EXTRA_PREFIX + ".ROOM_ID";
    private RecyclerView messageRecyclerView;

    private Button mSendButton;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;

    // Firebase instance variables
    private DatabaseReference messagesReference;
    private FirebaseRecyclerAdapter<Message, MessageViewHolder> mFirebaseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);
        String chatRoomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        messagesReference = mFirebaseDatabaseReference.child(Room.CHILD_NAME).child(chatRoomId).child(Message.CHILD_NAME);  //將列表要顯示的資料集指定到我們設計的訊息資料結構上。
        initView();
    }

    private void initView() {
        messageRecyclerView = (RecyclerView) findViewById(R.id.message_recycler_view);
        // Initialize ProgressBar and RecyclerView.
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        //設定好聊天室要接的訊息列表
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        messageRecyclerView.setLayoutManager(mLinearLayoutManager);

        //利用 Firebase UI 來幫我們簡單的達成接回訊息顯示以及訊息更新會自動更新畫面。
        mFirebaseAdapter = new FirebaseRecyclerAdapter<Message, MessageViewHolder>(
                Message.class,
                MessageViewHolder.layoutResId,
                MessageViewHolder.class,
                messagesReference) {

            @Override
            protected void populateViewHolder(MessageViewHolder viewHolder,
                                              Message message, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                viewHolder.messageTextView.setText(message.getText());

                String name = message.getName();
                viewHolder.messengerTextView.setText(null == name ? ANONYMOUS : name);
                if (message.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(
                            ContextCompat.getDrawable(ChatRoomActivity.this, R.mipmap.ic_account_circle_black));
                } else {
                    Glide.with(ChatRoomActivity.this)
                            .load(message.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }
            }
        };

        //當有新訊息時捲動到最新的訊息
        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    messageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        messageRecyclerView.setLayoutManager(mLinearLayoutManager);
        messageRecyclerView.setAdapter(mFirebaseAdapter);

        mMessageEditText = (EditText) findViewById(R.id.message_edit_text);
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //在沒有輸入訊息時，不能按送出
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = ANONYMOUS;
                String photoUrl = null;
                if(null != mFirebaseUser) {
                    userName = mFirebaseUser.getDisplayName();
                    if (null != mFirebaseUser.getPhotoUrl()) {
                        photoUrl = mFirebaseUser.getPhotoUrl().toString();
                    }
                }

                //按送出時，將訊息寫入到 Realtime Database
                Message message = new
                        Message(mMessageEditText.getText().toString(),
                        userName,
                        photoUrl);
                messagesReference.push().setValue(message);
                mMessageEditText.setText("");
            }
        });
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        public final static int layoutResId = R.layout.item_message;

        public TextView messageTextView;
        public TextView messengerTextView;
        public CircleImageView messengerImageView;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.message_text_view);
            messengerTextView = (TextView) itemView.findViewById(R.id.messenger_text_view);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messenger_thumb);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //如果使用者已經被登出，則回到使用者列表
        if(null ==  mFirebaseUser) {
            finish();
            return;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFirebaseAdapter.cleanup();
    }

}