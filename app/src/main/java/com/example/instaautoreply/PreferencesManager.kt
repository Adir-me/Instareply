package com.example.instaautoreply

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class WhitelistContact(
    val id: String = UUID.randomUUID().toString(),
    val handle: String,
    val name: String = "",
    val relationshipContext: String = "Close Friend",
    val customTypingSpeed: Float? = null,
    val customEmojiLevel: Int? = null,
    val customTone: String = "Casual & Warm",
    val isEnabled: Boolean = true
)

data class ContextMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ConversationMemory(
    val handle: String,
    val summary: String = "",
    val recentMessages: List<ContextMessage> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ChatLog(
    val id: String = UUID.randomUUID().toString(),
    val handle: String,
    val incomingMessage: String,
    val replyMessage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SENT", "EMERGENCY_MUTED", "URGENT_MUTED", "OUT_OF_HOURS", "MANUAL_TAKEOVER_PAUSED"
    val modelUsed: String,
    val delayMs: Long
)

class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("insta_auto_reply_prefs", Context.MODE_PRIVATE)

    // Master Controls
    private val _isAutoReplyEnabled = MutableStateFlow(prefs.getBoolean("is_auto_reply_enabled", true))
    val isAutoReplyEnabled: StateFlow<Boolean> = _isAutoReplyEnabled.asStateFlow()

    private val _isEmergencyStopped = MutableStateFlow(prefs.getBoolean("is_emergency_stopped", false))
    val isEmergencyStopped: StateFlow<Boolean> = _isEmergencyStopped.asStateFlow()

    // OpenRouter & AI Controls
    private val _openRouterApiKey = MutableStateFlow(prefs.getString("openrouter_api_key", "") ?: "")
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(prefs.getString("selected_model", "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _basePersona = MutableStateFlow(
        prefs.getString(
            "base_persona",
            "Friendly, witty, and concise Instagram DM assistant. Keeps answers under 20 words. Casual vibe."
        ) ?: "Friendly, witty, and concise Instagram DM assistant. Keeps answers under 20 words. Casual vibe."
    )
    val basePersona: StateFlow<String> = _basePersona.asStateFlow()

    // Timing & Realism
    private val _baseReadingTimeSec = MutableStateFlow(prefs.getFloat("base_reading_time_sec", 2.0f))
    val baseReadingTimeSec: StateFlow<Float> = _baseReadingTimeSec.asStateFlow()

    private val _typingSpeedSec = MutableStateFlow(prefs.getFloat("typing_speed_sec", 0.12f))
    val typingSpeedSec: StateFlow<Float> = _typingSpeedSec.asStateFlow()

    private val _multiBubbleEnabled = MutableStateFlow(prefs.getBoolean("multi_bubble_enabled", true))
    val multiBubbleEnabled: StateFlow<Boolean> = _multiBubbleEnabled.asStateFlow()

    private val _sentenceThreshold = MutableStateFlow(prefs.getInt("sentence_threshold", 70))
    val sentenceThreshold: StateFlow<Int> = _sentenceThreshold.asStateFlow()

    private val _bubbleIntervalSec = MutableStateFlow(prefs.getFloat("bubble_interval_sec", 1.5f))
    val bubbleIntervalSec: StateFlow<Float> = _bubbleIntervalSec.asStateFlow()

    // Style Controls
    private val _capitalizationStyle = MutableStateFlow(prefs.getString("capitalization_style", "Normal") ?: "Normal")
    val capitalizationStyle: StateFlow<String> = _capitalizationStyle.asStateFlow()

    private val _punctuationStyle = MutableStateFlow(prefs.getString("punctuation_style", "Standard") ?: "Standard")
    val punctuationStyle: StateFlow<String> = _punctuationStyle.asStateFlow()

    private val _emojiDensity = MutableStateFlow(prefs.getInt("emoji_density", 2))
    val emojiDensity: StateFlow<Int> = _emojiDensity.asStateFlow()

    private val _useFillerWords = MutableStateFlow(prefs.getBoolean("use_filler_words", true))
    val useFillerWords: StateFlow<Boolean> = _useFillerWords.asStateFlow()

    // Context Memory
    private val _memoryDepth = MutableStateFlow(prefs.getInt("memory_depth", 6))
    val memoryDepth: StateFlow<Int> = _memoryDepth.asStateFlow()

    // Working Hours & Rules
    private val _activeHoursEnabled = MutableStateFlow(prefs.getBoolean("active_hours_enabled", false))
    val activeHoursEnabled: StateFlow<Boolean> = _activeHoursEnabled.asStateFlow()

    private val _activeHoursStart = MutableStateFlow(prefs.getString("active_hours_start", "09:00") ?: "09:00")
    val activeHoursStart: StateFlow<String> = _activeHoursStart.asStateFlow()

    private val _activeHoursEnd = MutableStateFlow(prefs.getString("active_hours_end", "22:00") ?: "22:00")
    val activeHoursEnd: StateFlow<String> = _activeHoursEnd.asStateFlow()

    private val _activeDays = MutableStateFlow(
        prefs.getStringSet("active_days", setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
            ?: setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    )
    val activeDays: StateFlow<Set<String>> = _activeDays.asStateFlow()

    private val _urgencyMuteEnabled = MutableStateFlow(prefs.getBoolean("urgency_mute_enabled", true))
    val urgencyMuteEnabled: StateFlow<Boolean> = _urgencyMuteEnabled.asStateFlow()

    private val _urgencyKeywords = MutableStateFlow(
        loadUrgencyKeywords()
    )
    val urgencyKeywords: StateFlow<List<String>> = _urgencyKeywords.asStateFlow()

    private val _manualTakeoverPauseMinutes = MutableStateFlow(prefs.getInt("manual_takeover_pause_minutes", 15))
    val manualTakeoverPauseMinutes: StateFlow<Int> = _manualTakeoverPauseMinutes.asStateFlow()

    private val _manualTakeoverUntilMillis = MutableStateFlow(prefs.getLong("manual_takeover_until_millis", 0L))
    val manualTakeoverUntilMillis: StateFlow<Long> = _manualTakeoverUntilMillis.asStateFlow()

    // Whitelist
    private val _whitelist = MutableStateFlow(loadWhitelist())
    val whitelist: StateFlow<List<WhitelistContact>> = _whitelist.asStateFlow()

    // Chat Logs
    private val _chatLogs = MutableStateFlow(loadChatLogs())
    val chatLogs: StateFlow<List<ChatLog>> = _chatLogs.asStateFlow()

    // Conversation Memory & Context Map
    private val _conversationMemories = MutableStateFlow<Map<String, ConversationMemory>>(loadConversationMemories())
    val conversationMemories: StateFlow<Map<String, ConversationMemory>> = _conversationMemories.asStateFlow()

    fun getConversationMemory(handle: String): ConversationMemory {
        val clean = handle.lowercase().trim()
        return _conversationMemories.value[clean] ?: ConversationMemory(handle = clean)
    }

    fun saveConversationMemory(memory: ConversationMemory) {
        val current = _conversationMemories.value.toMutableMap()
        val clean = memory.handle.lowercase().trim()
        current[clean] = memory.copy(handle = clean, lastUpdated = System.currentTimeMillis())
        _conversationMemories.value = current
        saveConversationMemories(current)
    }

    fun clearConversationContext(handle: String? = null) {
        if (handle.isNullOrBlank()) {
            _conversationMemories.value = emptyMap()
            prefs.edit().remove("conversation_memories_json").apply()
        } else {
            val current = _conversationMemories.value.toMutableMap()
            val clean = handle.lowercase().trim()
            current.remove(clean)
            _conversationMemories.value = current
            saveConversationMemories(current)
        }
    }

    // Setters & Mutators
    fun setAutoReplyEnabled(enabled: Boolean) {
        _isAutoReplyEnabled.value = enabled
        prefs.edit().putBoolean("is_auto_reply_enabled", enabled).apply()
    }

    fun setEmergencyStopped(stopped: Boolean) {
        _isEmergencyStopped.value = stopped
        prefs.edit().putBoolean("is_emergency_stopped", stopped).apply()
    }

    fun setOpenRouterApiKey(key: String) {
        _openRouterApiKey.value = key
        prefs.edit().putString("openrouter_api_key", key).apply()
    }

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
        prefs.edit().putString("selected_model", model).apply()
    }

    fun setBasePersona(persona: String) {
        _basePersona.value = persona
        prefs.edit().putString("base_persona", persona).apply()
    }

    fun setBaseReadingTimeSec(sec: Float) {
        _baseReadingTimeSec.value = sec
        prefs.edit().putFloat("base_reading_time_sec", sec).apply()
    }

    fun setTypingSpeedSec(sec: Float) {
        _typingSpeedSec.value = sec
        prefs.edit().putFloat("typing_speed_sec", sec).apply()
    }

    fun setMultiBubbleEnabled(enabled: Boolean) {
        _multiBubbleEnabled.value = enabled
        prefs.edit().putBoolean("multi_bubble_enabled", enabled).apply()
    }

    fun setSentenceThreshold(chars: Int) {
        _sentenceThreshold.value = chars
        prefs.edit().putInt("sentence_threshold", chars).apply()
    }

    fun setBubbleIntervalSec(sec: Float) {
        _bubbleIntervalSec.value = sec
        prefs.edit().putFloat("bubble_interval_sec", sec).apply()
    }

    fun setCapitalizationStyle(style: String) {
        _capitalizationStyle.value = style
        prefs.edit().putString("capitalization_style", style).apply()
    }

    fun setPunctuationStyle(style: String) {
        _punctuationStyle.value = style
        prefs.edit().putString("punctuation_style", style).apply()
    }

    fun setEmojiDensity(density: Int) {
        _emojiDensity.value = density
        prefs.edit().putInt("emoji_density", density).apply()
    }

    fun setUseFillerWords(use: Boolean) {
        _useFillerWords.value = use
        prefs.edit().putBoolean("use_filler_words", use).apply()
    }

    fun setMemoryDepth(depth: Int) {
        _memoryDepth.value = depth
        prefs.edit().putInt("memory_depth", depth).apply()
    }

    fun setActiveHoursEnabled(enabled: Boolean) {
        _activeHoursEnabled.value = enabled
        prefs.edit().putBoolean("active_hours_enabled", enabled).apply()
    }

    fun setActiveHoursStart(time: String) {
        _activeHoursStart.value = time
        prefs.edit().putString("active_hours_start", time).apply()
    }

    fun setActiveHoursEnd(time: String) {
        _activeHoursEnd.value = time
        prefs.edit().putString("active_hours_end", time).apply()
    }

    fun setActiveDays(days: Set<String>) {
        _activeDays.value = days
        prefs.edit().putStringSet("active_days", days).apply()
    }

    fun setUrgencyMuteEnabled(enabled: Boolean) {
        _urgencyMuteEnabled.value = enabled
        prefs.edit().putBoolean("urgency_mute_enabled", enabled).apply()
    }

    fun addUrgencyKeyword(keyword: String) {
        val current = _urgencyKeywords.value.toMutableList()
        val trimmed = keyword.trim().uppercase()
        if (trimmed.isNotEmpty() && !current.contains(trimmed)) {
            current.add(trimmed)
            _urgencyKeywords.value = current
            saveUrgencyKeywords(current)
        }
    }

    fun removeUrgencyKeyword(keyword: String) {
        val current = _urgencyKeywords.value.toMutableList()
        current.remove(keyword)
        _urgencyKeywords.value = current
        saveUrgencyKeywords(current)
    }

    fun setManualTakeoverPauseMinutes(minutes: Int) {
        _manualTakeoverPauseMinutes.value = minutes
        prefs.edit().putInt("manual_takeover_pause_minutes", minutes).apply()
    }

    fun triggerManualTakeoverPause() {
        val until = System.currentTimeMillis() + (_manualTakeoverPauseMinutes.value * 60 * 1000L)
        _manualTakeoverUntilMillis.value = until
        prefs.edit().putLong("manual_takeover_until_millis", until).apply()
    }

    fun cancelManualTakeoverPause() {
        _manualTakeoverUntilMillis.value = 0L
        prefs.edit().putLong("manual_takeover_until_millis", 0L).apply()
    }

    fun addOrUpdateWhitelistContact(contact: WhitelistContact) {
        val current = _whitelist.value.toMutableList()
        val index = current.indexOfFirst { it.id == contact.id || it.handle.equals(contact.handle, ignoreCase = true) }
        if (index >= 0) {
            current[index] = contact
        } else {
            current.add(0, contact)
        }
        _whitelist.value = current
        saveWhitelist(current)
    }

    fun removeWhitelistContact(id: String) {
        val current = _whitelist.value.toMutableList()
        current.removeAll { it.id == id }
        _whitelist.value = current
        saveWhitelist(current)
    }

    fun addChatLog(log: ChatLog) {
        val current = _chatLogs.value.toMutableList()
        current.add(0, log)
        if (current.size > 100) {
            current.removeAt(current.size - 1)
        }
        _chatLogs.value = current
        saveChatLogs(current)
    }

    fun clearChatLogs() {
        _chatLogs.value = emptyList()
        prefs.edit().remove("chat_logs_json").apply()
    }

    // Persistence Helpers
    private fun saveUrgencyKeywords(list: List<String>) {
        val array = JSONArray(list)
        prefs.edit().putString("urgency_keywords_json", array.toString()).apply()
    }

    private fun loadUrgencyKeywords(): List<String> {
        val json = prefs.getString("urgency_keywords_json", null)
            ?: return listOf("URGENT", "EMERGENCY", "CALL ME", "ASAP", "HELP", "IMPORTANT")
        return try {
            val array = JSONArray(json)
            List(array.length()) { array.getString(it) }
        } catch (e: Exception) {
            listOf("URGENT", "EMERGENCY", "CALL ME", "ASAP", "HELP", "IMPORTANT")
        }
    }

    private fun saveWhitelist(list: List<WhitelistContact>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("handle", item.handle)
                put("name", item.name)
                put("relationshipContext", item.relationshipContext)
                put("customTypingSpeed", item.customTypingSpeed?.toDouble() ?: JSONObject.NULL)
                put("customEmojiLevel", item.customEmojiLevel ?: JSONObject.NULL)
                put("customTone", item.customTone)
                put("isEnabled", item.isEnabled)
            }
            array.put(obj)
        }
        prefs.edit().putString("whitelist_json", array.toString()).apply()
    }

    private fun loadWhitelist(): List<WhitelistContact> {
        val json = prefs.getString("whitelist_json", null)
        if (json.isNullOrEmpty()) {
            return emptyList()
        }
        return try {
            val array = JSONArray(json)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                WhitelistContact(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    handle = obj.optString("handle"),
                    name = obj.optString("name"),
                    relationshipContext = obj.optString("relationshipContext", "Friend"),
                    customTypingSpeed = if (obj.isNull("customTypingSpeed")) null else obj.getDouble("customTypingSpeed").toFloat(),
                    customEmojiLevel = if (obj.isNull("customEmojiLevel")) null else obj.getInt("customEmojiLevel"),
                    customTone = obj.optString("customTone", "Natural"),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveChatLogs(list: List<ChatLog>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("handle", item.handle)
                put("incomingMessage", item.incomingMessage)
                put("replyMessage", item.replyMessage)
                put("timestamp", item.timestamp)
                put("status", item.status)
                put("modelUsed", item.modelUsed)
                put("delayMs", item.delayMs)
            }
            array.put(obj)
        }
        prefs.edit().putString("chat_logs_json", array.toString()).apply()
    }

    private fun loadChatLogs(): List<ChatLog> {
        val json = prefs.getString("chat_logs_json", null)
        if (json.isNullOrEmpty()) {
            return emptyList()
        }
        return try {
            val array = JSONArray(json)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                ChatLog(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    handle = obj.optString("handle"),
                    incomingMessage = obj.optString("incomingMessage"),
                    replyMessage = obj.optString("replyMessage"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    status = obj.optString("status", "SENT"),
                    modelUsed = obj.optString("modelUsed", "gemini-flash"),
                    delayMs = obj.optLong("delayMs", 0L)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveConversationMemories(map: Map<String, ConversationMemory>) {
        val array = JSONArray()
        map.values.forEach { mem ->
            val obj = JSONObject().apply {
                put("handle", mem.handle)
                put("summary", mem.summary)
                put("lastUpdated", mem.lastUpdated)
                val msgsArr = JSONArray()
                mem.recentMessages.forEach { msg ->
                    msgsArr.put(JSONObject().apply {
                        put("isUser", msg.isUser)
                        put("text", msg.text)
                        put("timestamp", msg.timestamp)
                    })
                }
                put("recentMessages", msgsArr)
            }
            array.put(obj)
        }
        prefs.edit().putString("conversation_memories_json", array.toString()).apply()
    }

    private fun loadConversationMemories(): Map<String, ConversationMemory> {
        val json = prefs.getString("conversation_memories_json", null) ?: return emptyMap()
        return try {
            val array = JSONArray(json)
            val result = mutableMapOf<String, ConversationMemory>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val handle = obj.optString("handle").lowercase().trim()
                val summary = obj.optString("summary", "")
                val lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                val msgsArr = obj.optJSONArray("recentMessages") ?: JSONArray()
                val msgs = mutableListOf<ContextMessage>()
                for (j in 0 until msgsArr.length()) {
                    val mObj = msgsArr.getJSONObject(j)
                    msgs.add(
                        ContextMessage(
                            isUser = mObj.optBoolean("isUser", true),
                            text = mObj.optString("text"),
                            timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (handle.isNotEmpty()) {
                    result[handle] = ConversationMemory(
                        handle = handle,
                        summary = summary,
                        recentMessages = msgs,
                        lastUpdated = lastUpdated
                    )
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context).also { instance = it }
            }
        }
    }
}
