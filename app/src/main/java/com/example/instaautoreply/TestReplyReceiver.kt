package com.example.instaautoreply

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput

class TestReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val results = RemoteInput.getResultsFromIntent(intent)
        if (results != null) {
            val replyText = results.getCharSequence("key_text_reply")?.toString() ?: ""
            Log.d("InstaAutoReply", "TestReplyReceiver successfully received AI reply: $replyText")
        }
    }
}
