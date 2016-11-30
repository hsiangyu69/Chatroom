package com.chocolabs.chatroom;

import android.app.Application;

import com.firebase.client.Firebase;

/**
 * Created by chenpusheng on 2016/7/27.
 */
public class ChatroomApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}
