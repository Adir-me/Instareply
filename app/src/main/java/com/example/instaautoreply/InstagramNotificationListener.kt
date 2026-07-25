package com.example.instaautoreply

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

import java.util.concurrent.ConcurrentHashMap

class InstagramNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Cooldown tracker per handle to prevent rapid duplicate auto-replies
    private val lastReplyTimeMap = ConcurrentHashMap<String, Long>()

    // Track sent reply messages to prevent infinite self-reply loops
    private val recentSentRepliesMap = ConcurrentHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        // Check if from Instagram or internal test notification
        val isInstagram = packageName.contains("instagram", ignoreCase = true) || packageName == applicationContext.packageName
        if (!isInstagram) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() || text.isBlank()) return

        // Clean handle name (e.g. "alex_designer" from "Direct Message from alex_designer")
        val handle = extractHandle(title)

        // Prevent self-reply loops / notification echoes
        if (isSelfReplyOrEcho(title, handle, text)) {
            Log.d("InstaAutoReply", "Self-reply / outgoing echo detected for handle: $handle ('$text'). Skipping auto-reply.")
            return
        }

        Log.d("InstaAutoReply", "Notification received from: $handle, msg: $text")

        serviceScope.launch {
            processAutoReply(sbn, handle, text)
        }
    }

    private suspend fun processAutoReply(sbn: StatusBarNotification, handle: String, messageText: String) {
        val prefs = PreferencesManager.getInstance(applicationContext)

        // 1. Check Master Switch
        if (!prefs.isAutoReplyEnabled.value) {
            Log.d("InstaAutoReply", "Auto-reply disabled")
            return
        }

        // 2. Check Emergency Stop
        if (prefs.isEmergencyStopped.value) {
            prefs.addChatLog(
                ChatLog(
                    handle = handle,
                    incomingMessage = messageText,
                    replyMessage = "[Auto-Reply Muted: Emergency Stop Active]",
                    status = "EMERGENCY_MUTED",
                    modelUsed = "Emergency Filter",
                    delayMs = 0
                )
            )
            return
        }

        // 3. Check Manual Takeover Protection
        val takeoverUntil = prefs.manualTakeoverUntilMillis.value
        if (takeoverUntil > System.currentTimeMillis()) {
            prefs.addChatLog(
                ChatLog(
                    handle = handle,
                    incomingMessage = messageText,
                    replyMessage = "[Auto-Reply Muted: Manual Takeover Pause Active]",
                    status = "MANUAL_TAKEOVER_PAUSED",
                    modelUsed = "Protection Engine",
                    delayMs = 0
                )
            )
            return
        }

        // 4. Check Active Working Hours
        if (prefs.activeHoursEnabled.value) {
            if (!isWithinActiveHours(
                    prefs.activeHoursStart.value,
                    prefs.activeHoursEnd.value,
                    prefs.activeDays.value
                )
            ) {
                prefs.addChatLog(
                    ChatLog(
                        handle = handle,
                        incomingMessage = messageText,
                        replyMessage = "[Auto-Reply Skipped: Out of Active Hours]",
                        status = "OUT_OF_HOURS",
                        modelUsed = "Hours Scheduler",
                        delayMs = 0
                    )
                )
                return
            }
        }

        // 5. Check Whitelist
        val whitelist = prefs.whitelist.value
        val contact = whitelist.find { it.handle.equals(handle, ignoreCase = true) || it.name.equals(handle, ignoreCase = true) }
        val isWhitelisted = contact?.isEnabled ?: true

        if (!isWhitelisted) {
            Log.d("InstaAutoReply", "Contact $handle is disabled in Whitelist")
            return
        }

        // 6. Check Rapid Cooldown (Prevent loops or spam replies within 15 seconds)
        val lastReply = lastReplyTimeMap[handle] ?: 0L
        val now = System.currentTimeMillis()
        if (now - lastReply < 15000L) {
            Log.d("InstaAutoReply", "Skipping auto-reply for $handle due to 15s cooldown safeguard")
            return
        }

        // 7. Check Urgency Keywords
        if (prefs.urgencyMuteEnabled.value) {
            val upperMsg = messageText.uppercase()
            val matchedKeyword = prefs.urgencyKeywords.value.find { upperMsg.contains(it) }
            if (matchedKeyword != null) {
                prefs.addChatLog(
                    ChatLog(
                        handle = handle,
                        incomingMessage = messageText,
                        replyMessage = "[Auto-Reply Muted: Urgency Trigger '$matchedKeyword']",
                        status = "URGENT_MUTED",
                        modelUsed = "Urgency Mute Filter",
                        delayMs = 0
                    )
                )
                return
            }
        }

        // 8. Retrieve Conversation Context Memory for this Sender
        val conversationMemory = prefs.getConversationMemory(handle)

        // 9. Generate Reply via OpenRouter or Local Fallback
        val apiKey = prefs.openRouterApiKey.value
        val model = prefs.selectedModel.value
        val basePersona = prefs.basePersona.value
        val relationship = contact?.relationshipContext ?: "Casual Contact"
        val tone = contact?.customTone ?: "Natural"

        val generatedReply = if (apiKey.isNotBlank()) {
            callOpenRouterApi(
                apiKey = apiKey,
                primaryModel = model,
                basePersona = basePersona,
                relationship = relationship,
                tone = tone,
                incomingMessage = messageText,
                conversationMemory = conversationMemory,
                emojiDensity = contact?.customEmojiLevel ?: prefs.emojiDensity.value,
                capitalizationStyle = prefs.capitalizationStyle.value,
                punctuationStyle = prefs.punctuationStyle.value
            )
        } else {
            generateRealisticFallbackReply(
                incomingMessage = messageText,
                relationship = relationship,
                tone = tone,
                emojiLevel = contact?.customEmojiLevel ?: prefs.emojiDensity.value,
                capitalizationStyle = prefs.capitalizationStyle.value,
                punctuationStyle = prefs.punctuationStyle.value,
                useFiller = prefs.useFillerWords.value
            )
        }

        // 10. Calculate Realistic Delay
        val typingSpeed = contact?.customTypingSpeed ?: prefs.typingSpeedSec.value
        val readingDelayMs = (prefs.baseReadingTimeSec.value * 1000).toLong()
        val typingDelayMs = (generatedReply.length * typingSpeed * 1000).toLong()
        val totalDelayMs = readingDelayMs + typingDelayMs

        // Simulate reading & typing time
        delay(totalDelayMs.coerceAtMost(10000L))

        // 11. Multi-Bubble Splitting & Sending Action
        val multiBubble = prefs.multiBubbleEnabled.value
        val sentenceThreshold = prefs.sentenceThreshold.value
        val bubbleIntervalSec = prefs.bubbleIntervalSec.value

        val bubbles = if (multiBubble) {
            splitIntoBubbles(generatedReply, sentenceThreshold)
        } else {
            listOf(generatedReply)
        }

        // Cache all full & split bubble responses immediately to block echo loops
        val nowMs = System.currentTimeMillis()
        recentSentRepliesMap[normalizeMessage(generatedReply)] = nowMs
        for (bubble in bubbles) {
            recentSentRepliesMap[normalizeMessage(bubble)] = nowMs
        }

        var allSent = true
        val sentBubblesList = mutableListOf<String>()

        for ((idx, bubble) in bubbles.withIndex()) {
            if (idx > 0) {
                val interBubbleDelayMs = (bubbleIntervalSec * 1000L).toLong().coerceAtLeast(600L)
                delay(interBubbleDelayMs)
            }

            val sent = replyToNotification(sbn, bubble)
            if (sent) {
                val postSentMs = System.currentTimeMillis()
                recentSentRepliesMap[normalizeMessage(bubble)] = postSentMs
                recentSentRepliesMap[normalizeMessage(generatedReply)] = postSentMs
                sentBubblesList.add(bubble)
            } else {
                allSent = false
            }
        }

        if (sentBubblesList.isNotEmpty()) {
            lastReplyTimeMap[handle] = System.currentTimeMillis()
        }

        // 12. Record Chat Log & Update Context Memory
        val finalReplyText = if (bubbles.size > 1) bubbles.joinToString(" || ") else generatedReply
        prefs.addChatLog(
            ChatLog(
                handle = handle,
                incomingMessage = messageText,
                replyMessage = finalReplyText,
                status = if (allSent) "SENT" else if (sentBubblesList.isNotEmpty()) "PARTIAL_SENT" else "FAILED (NO REPLY ACTION)",
                modelUsed = if (apiKey.isNotBlank()) model else "$model (Local Engine)",
                delayMs = totalDelayMs
            )
        )

        // 13. Context Memory Update & Auto-Summarization
        val updatedMsgs = conversationMemory.recentMessages.toMutableList()
        updatedMsgs.add(ContextMessage(isUser = true, text = messageText))
        updatedMsgs.add(ContextMessage(isUser = false, text = finalReplyText))

        val depthLimit = prefs.memoryDepth.value.coerceAtLeast(4)
        if (updatedMsgs.size >= depthLimit) {
            val messagesToSummarize = updatedMsgs.dropLast(2)
            val newRecentMsgs = updatedMsgs.takeLast(2)

            val newSummary = summarizeConversation(
                apiKey = apiKey,
                model = model,
                existingSummary = conversationMemory.summary,
                messagesToSummarize = messagesToSummarize
            )

            prefs.saveConversationMemory(
                ConversationMemory(
                    handle = handle,
                    summary = newSummary,
                    recentMessages = newRecentMsgs
                )
            )
        } else {
            prefs.saveConversationMemory(
                ConversationMemory(
                    handle = handle,
                    summary = conversationMemory.summary,
                    recentMessages = updatedMsgs
                )
            )
        }
    }

    private fun normalizeMessage(text: String): String {
        return text.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    private fun isSelfReplyOrEcho(title: String, handle: String, messageText: String): Boolean {
        val cleanTitle = title.lowercase(Locale.getDefault()).trim()
        val cleanHandle = handle.lowercase(Locale.getDefault()).trim()
        val cleanMsg = messageText.lowercase(Locale.getDefault()).trim()

        if (cleanTitle == "you" || cleanTitle.startsWith("you ") || cleanTitle.startsWith("you:") ||
            cleanHandle == "you" || cleanHandle.contains("yourself")
        ) {
            return true
        }

        if (cleanMsg.startsWith("you: ") || cleanMsg.startsWith("you sent ") ||
            cleanMsg.startsWith("your message: ") || cleanMsg.startsWith("you replied: ")
        ) {
            return true
        }

        val now = System.currentTimeMillis()
        recentSentRepliesMap.entries.removeIf { now - it.value > 600000L }

        val normalizedIncoming = normalizeMessage(messageText)
        if (normalizedIncoming.isNotEmpty() && recentSentRepliesMap.containsKey(normalizedIncoming)) {
            return true
        }

        val stripped = cleanMsg.removePrefix("you: ").removePrefix("you sent ").removePrefix("you replied: ").trim()
        val normalizedStripped = normalizeMessage(stripped)
        if (normalizedStripped.isNotEmpty() && recentSentRepliesMap.containsKey(normalizedStripped)) {
            return true
        }

        return false
    }

    private fun splitIntoBubbles(text: String, threshold: Int): List<String> {
        if (text.length <= threshold) return listOf(text)
        val regex = Regex("(?<=[.!?\\n])\\s+")
        val parts = text.split(regex).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size > 1) return parts
        if (text.contains(", ")) {
            val commaParts = text.split(", ").map { it.trim() }.filter { it.isNotEmpty() }
            if (commaParts.size > 1) return commaParts
        }
        return listOf(text)
    }

    private fun extractHandle(title: String): String {
        return title.replace("Direct Message from", "", ignoreCase = true)
            .replace("Message from", "", ignoreCase = true)
            .replace("Instagram Direct:", "", ignoreCase = true)
            .replace(":", "")
            .trim()
            .ifEmpty { "instagram_user" }
    }

    private fun isWithinActiveHours(startStr: String, endStr: String, days: Set<String>): Boolean {
        try {
            val calendar = Calendar.getInstance()
            val currentDayName = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                Calendar.SUNDAY -> "Sun"
                else -> "Mon"
            }

            if (!days.contains(currentDayName)) return false

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val nowStr = String.format(Locale.getDefault(), "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))

            val now = sdf.parse(nowStr) ?: return true
            val start = sdf.parse(startStr) ?: return true
            val end = sdf.parse(endStr) ?: return true

            return if (end.after(start)) {
                now in start..end
            } else {
                now >= start || now <= end
            }
        } catch (e: Exception) {
            return true
        }
    }

    private fun callOpenRouterApi(
        apiKey: String,
        primaryModel: String,
        basePersona: String,
        relationship: String,
        tone: String,
        incomingMessage: String,
        conversationMemory: ConversationMemory,
        emojiDensity: Int,
        capitalizationStyle: String,
        punctuationStyle: String
    ): String {
        // Models list to attempt in order (Primary model -> Fallback models)
        val modelsToTry = listOf(primaryModel, "google/gemini-2.0-flash-001", "openai/gpt-4o-mini").distinct()

        val summaryContext = if (conversationMemory.summary.isNotBlank()) {
            "\n[LONG-TERM CONVERSATION SUMMARY / MEMORY]: ${conversationMemory.summary}"
        } else ""

        val systemInstruction = """
            $basePersona
            Context: Relationship with sender is '$relationship'. Desired Tone is '$tone'.$summaryContext
            Style Rules: Emoji density level $emojiDensity/5, Capitalization style: $capitalizationStyle, Punctuation style: $punctuationStyle.
            Keep response under 25 words. Sound natural and human-like. Do not wrap in quotes or code blocks.
            If the message indicates shared media/photo/video/story/reel, acknowledge it warmly.
        """.trimIndent()

        for (model in modelsToTry) {
            try {
                val jsonBody = JSONObject().apply {
                    put("model", model)
                    val messages = JSONArray().apply {
                        // System prompt
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemInstruction)
                        })
                        // Multi-turn conversation history from memory
                        for (msg in conversationMemory.recentMessages) {
                            put(JSONObject().apply {
                                put("role", if (msg.isUser) "user" else "assistant")
                                put("content", msg.text)
                            })
                        }
                        // Current incoming message
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", incomingMessage)
                        })
                    }
                    put("messages", messages)
                    put("max_tokens", 80)
                    put("temperature", 0.7)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("HTTP-Referer", "https://github.com/instaautoreply/app")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful && bodyStr.isNotEmpty()) {
                    val json = JSONObject(bodyStr)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val reply = choices
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim()
                        if (reply.isNotEmpty()) {
                            return applyStyleFormatting(reply, capitalizationStyle, punctuationStyle, emojiDensity)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("InstaAutoReply", "Failed to get response from model $model: ${e.message}")
            }
        }

        // Local fallback if API calls fail
        return generateRealisticFallbackReply(incomingMessage, relationship, tone, emojiDensity, capitalizationStyle, punctuationStyle, true)
    }

    private fun summarizeConversation(
        apiKey: String,
        model: String,
        existingSummary: String,
        messagesToSummarize: List<ContextMessage>
    ): String {
        val formattedMsgs = messagesToSummarize.joinToString("\n") {
            val role = if (it.isUser) "Them" else "You"
            "$role: ${it.text}"
        }

        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    Summarize key facts, topics, preferences, and important context from this ongoing Instagram DM conversation into 1-2 concise sentences.
                    ${if (existingSummary.isNotBlank()) "Previous Memory Summary: $existingSummary\n" else ""}
                    New Conversation Segment:
                    $formattedMsgs
                """.trimIndent()

                val jsonBody = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("max_tokens", 100)
                    put("temperature", 0.3)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("HTTP-Referer", "https://github.com/instaautoreply/app")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful && bodyStr.isNotEmpty()) {
                    val json = JSONObject(bodyStr)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val summaryText = choices.getJSONObject(0).getJSONObject("message").getString("content").trim()
                        if (summaryText.isNotBlank()) return summaryText
                    }
                }
            } catch (e: Exception) {
                Log.w("InstaAutoReply", "Summarization failed: ${e.message}")
            }
        }

        // Local Fallback Summarization
        val lastTopic = messagesToSummarize.lastOrNull()?.text ?: "chat topic"
        val snippet = if (lastTopic.length > 40) lastTopic.take(40) + "..." else lastTopic
        return if (existingSummary.isBlank()) {
            "Discussed $snippet"
        } else {
            "$existingSummary | Recent topic: $snippet"
        }
    }

    private fun generateRealisticFallbackReply(
        incomingMessage: String,
        relationship: String,
        tone: String,
        emojiLevel: Int,
        capitalizationStyle: String,
        punctuationStyle: String,
        useFiller: Boolean
    ): String {
        val lower = incomingMessage.lowercase()
        val filler = if (useFiller) listOf("haha ", "hey! ", "tbh ", "yeah ", "hmm ").random() else ""

        val baseReply = when {
            lower.contains("hello") || lower.contains("hey") || lower.contains("hi") ->
                "${filler}hey! how's it going?"
            lower.contains("price") || lower.contains("cost") || lower.contains("how much") ->
                "hey! ill check and send you details in a bit"
            lower.contains("design") || lower.contains("mockup") || lower.contains("work") ->
                "looks great! reviewing now, ill get back to you shortly"
            lower.contains("where") || lower.contains("location") ->
                "at the studio right now! where are you?"
            lower.contains("thanks") || lower.contains("thank you") ->
                "anytime! let me know if you need anything else"
            else ->
                "${filler}got your msg! ill reply properly in a few minutes"
        }

        return applyStyleFormatting(baseReply, capitalizationStyle, punctuationStyle, emojiLevel)
    }

    private fun applyStyleFormatting(
        text: String,
        capitalizationStyle: String,
        punctuationStyle: String,
        emojiLevel: Int
    ): String {
        var formatted = text
        when (capitalizationStyle) {
            "All Lowercase" -> formatted = formatted.lowercase()
            "ALL CAPS" -> formatted = formatted.uppercase()
        }

        when (punctuationStyle) {
            "Minimal" -> formatted = formatted.replace("!", ".").replace("?", ".")
            "None" -> formatted = formatted.replace(Regex("[.,!?]"), "")
        }

        val emojis = when (emojiLevel) {
            1 -> " 👍"
            2 -> " 😊"
            3 -> " 🔥✨"
            4 -> " 😂🙌🔥"
            5 -> " 🥳🎉💯🔥✨"
            else -> ""
        }

        return formatted + emojis
    }

    private fun replyToNotification(sbn: StatusBarNotification, replyMessage: String): Boolean {
        val notification = sbn.notification ?: return false

        // Collect actions from main notification as well as WearableExtender actions
        val actionsList = mutableListOf<Notification.Action>()
        notification.actions?.let { actionsList.addAll(it) }

        val wearableActions = Notification.WearableExtender(notification).actions
        if (!wearableActions.isNullOrEmpty()) {
            actionsList.addAll(wearableActions)
        }

        if (actionsList.isEmpty()) {
            Log.e("InstaAutoReply", "No notification actions found on notification from ${sbn.packageName}")
            return false
        }

        for (action in actionsList) {
            val remoteInputs = action.remoteInputs ?: continue
            if (remoteInputs.isEmpty()) continue

            for (remoteInput in remoteInputs) {
                if (remoteInput.allowFreeFormInput || !remoteInput.resultKey.isNullOrEmpty()) {
                    try {
                        val intent = Intent()
                        val bundle = Bundle()
                        bundle.putCharSequence(remoteInput.resultKey, replyMessage)
                        RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        action.actionIntent.send(applicationContext, 0, intent)
                        Log.d("InstaAutoReply", "Successfully sent reply via RemoteInput to ${sbn.packageName}!")
                        return true
                    } catch (e: Exception) {
                        Log.e("InstaAutoReply", "Error sending reply intent to ${sbn.packageName}", e)
                    }
                }
            }
        }
        Log.e("InstaAutoReply", "No RemoteInput found in actions for ${sbn.packageName}")
        return false
    }
}
