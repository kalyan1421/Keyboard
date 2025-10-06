package com.example.ai_keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.TextView
import android.widget.ScrollView

/**
 * Simple media panel manager using basic Android views
 */
class SimpleMediaPanel(private val context: Context) : LinearLayout(context) {
    
    companion object {
        private const val TAG = "SimpleMediaPanel"
        private const val PANEL_HEIGHT_DP = 280
    }
    
    enum class MediaType { 
        EMOJI, 
        GIF, 
        STICKER,
        AI_FEATURES
    }
    
    private lateinit var emojiPanel: GboardEmojiPanel
    private lateinit var gifPanel: SimpleGifPanel
    private lateinit var stickerPanel: SimpleStickerPanel
    private lateinit var aiFeaturesPanel: AIFeaturesPanel
    private lateinit var mediaTabsLayout: LinearLayout
    private var onMediaSelected: ((MediaType, String, Any?) -> Unit)? = null
    private var onKeyboardSwitchRequested: (() -> Unit)? = null
    private var currentMediaType = MediaType.EMOJI
    
    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(PANEL_HEIGHT_DP))
        setBackgroundColor(Color.parseColor("#F0F0F0"))
        
        setupMediaTabs()
        setupMediaPanels()
        
        showPanel(MediaType.EMOJI) // Default to emoji
        
        Log.d(TAG, "SimpleMediaPanel initialized")
    }
    
    private fun setupMediaTabs() {
        mediaTabsLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(50))
            setBackgroundColor(Color.parseColor("#E0E0E0"))
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        }
        
        val emojiTab = createMediaTab("😊", "Emoji", MediaType.EMOJI)
        val gifTab = createMediaTab("🎬", "GIF", MediaType.GIF)
        val stickerTab = createMediaTab("🏷️", "Sticker", MediaType.STICKER)
        val aiTab = createMediaTab("🤖", "AI", MediaType.AI_FEATURES)
        
        mediaTabsLayout.addView(emojiTab)
        mediaTabsLayout.addView(gifTab)
        mediaTabsLayout.addView(stickerTab)
        mediaTabsLayout.addView(aiTab)
        
        addView(mediaTabsLayout)
    }
    
    private fun createMediaTab(icon: String, label: String, type: MediaType): Button {
        return Button(context).apply {
            text = "$icon\n$label"
            textSize = 12f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            
            // Create rounded background
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.WHITE)
                setStroke(1, Color.parseColor("#CCCCCC"))
            }
            
            setOnClickListener { 
                showPanel(type)
                updateTabSelection(type)
            }
            
            tag = type
        }
    }
    
    private fun updateTabSelection(selectedType: MediaType) {
        for (i in 0 until mediaTabsLayout.childCount) {
            val tab = mediaTabsLayout.getChildAt(i) as Button
            val isSelected = tab.tag == selectedType
            
            val background = tab.background as GradientDrawable
            if (isSelected) {
                background.setColor(Color.parseColor("#2196F3"))
                background.setStroke(2, Color.parseColor("#1976D2"))
                tab.setTextColor(Color.WHITE)
            } else {
                background.setColor(Color.WHITE)
                background.setStroke(1, Color.parseColor("#CCCCCC"))
                tab.setTextColor(Color.BLACK)
            }
        }
        
        currentMediaType = selectedType
    }
    
    private fun setupMediaPanels() {
        // Create emoji panel
        emojiPanel = GboardEmojiPanel(context).apply {
            setOnEmojiSelectedListener { emoji ->
                Log.d(TAG, "Emoji selected: $emoji")
                onMediaSelected?.invoke(MediaType.EMOJI, emoji, null)
            }
            setOnKeyboardSwitchRequestedListener {
                Log.d(TAG, "Keyboard switch requested from emoji panel")
                onKeyboardSwitchRequested?.invoke()
            }
            visibility = VISIBLE
        }
        
        // Create GIF panel
        gifPanel = SimpleGifPanel(context).apply {
            setOnGifSelectedListener { gifTitle ->
                Log.d(TAG, "GIF selected: $gifTitle")
                onMediaSelected?.invoke(MediaType.GIF, "[$gifTitle GIF]", null)
            }
            visibility = GONE
        }
        
        // Create sticker panel
        stickerPanel = SimpleStickerPanel(context).apply {
            setOnStickerSelectedListener { sticker ->
                Log.d(TAG, "Sticker selected: $sticker")
                onMediaSelected?.invoke(MediaType.STICKER, sticker, null)
            }
            visibility = GONE
        }
        
        // Create AI features panel
        aiFeaturesPanel = AIFeaturesPanel(context).apply {
            setOnTextProcessedListener { processedText ->
                Log.d(TAG, "AI processed text: $processedText")
                onMediaSelected?.invoke(MediaType.AI_FEATURES, processedText, null)
            }
            setOnSmartReplySelectedListener { reply ->
                Log.d(TAG, "Smart reply selected: $reply")
                onMediaSelected?.invoke(MediaType.AI_FEATURES, reply, null)
            }
            visibility = GONE
        }
        
        addView(emojiPanel)
        addView(gifPanel)
        addView(stickerPanel)
        addView(aiFeaturesPanel)
    }
    
    private fun showPanel(type: MediaType) {
        try {
            emojiPanel.visibility = if (type == MediaType.EMOJI) VISIBLE else GONE
            gifPanel.visibility = if (type == MediaType.GIF) VISIBLE else GONE
            stickerPanel.visibility = if (type == MediaType.STICKER) VISIBLE else GONE
            aiFeaturesPanel.visibility = if (type == MediaType.AI_FEATURES) VISIBLE else GONE
            
            currentMediaType = type
            Log.d(TAG, "Switched to panel: $type")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing panel: $type", e)
        }
    }
    
    fun setOnMediaSelectedListener(listener: (MediaType, String, Any?) -> Unit) {
        onMediaSelected = listener
    }
    
    fun setOnKeyboardSwitchRequestedListener(listener: () -> Unit) {
        onKeyboardSwitchRequested = listener
    }
    
    fun getCurrentMediaType(): MediaType {
        return currentMediaType
    }
    
    /**
     * Set input text for AI processing
     */
    fun setInputText(text: String) {
        aiFeaturesPanel.setInputText(text)
    }
    
    fun switchToEmoji() {
        showPanel(MediaType.EMOJI)
        updateTabSelection(MediaType.EMOJI)
    }
    
    fun switchToGif() {
        showPanel(MediaType.GIF)
        updateTabSelection(MediaType.GIF)
    }
    
    fun switchToSticker() {
        showPanel(MediaType.STICKER)
        updateTabSelection(MediaType.STICKER)
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}

/**
 * Simple GIF panel
 */
class SimpleGifPanel(context: Context) : LinearLayout(context) {
    
    companion object {
        private const val TAG = "SimpleGifPanel"
        private const val PANEL_HEIGHT_DP = 280
    }
    
    private var onGifSelected: ((String) -> Unit)? = null
    
    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(PANEL_HEIGHT_DP))
        setBackgroundColor(Color.parseColor("#F5F5F5"))
        
        setupGifPanel()
    }
    
    private fun setupGifPanel() {
        // Create header
        val headerText = TextView(context).apply {
            text = "GIFs"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        addView(headerText)
        
        // Create category buttons
        val categoryLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(50))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
        }
        
        val categories = listOf(
            Pair("Reactions", "reactions"),
            Pair("Activities", "activities"),
            Pair("Celebrations", "celebrations")
        )
        
        categories.forEach { (name, category) ->
            val button = Button(context).apply {
                text = name
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(dpToPx(2), 0, dpToPx(2), 0)
                }
                setOnClickListener { 
                    onGifSelected?.invoke("$name GIF")
                    Toast.makeText(context, "Selected $name GIF", Toast.LENGTH_SHORT).show()
                }
            }
            categoryLayout.addView(button)
        }
        
        addView(categoryLayout)
        
        // Create GIF grid placeholder
        val gridPlaceholder = TextView(context).apply {
            text = "📱 GIF Categories\n\n🎭 Reactions\n🎯 Activities\n🎉 Celebrations\n\nTap a category above to insert a GIF!"
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            setBackgroundColor(Color.WHITE)
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }
        addView(gridPlaceholder)
    }
    
    fun setOnGifSelectedListener(listener: (String) -> Unit) {
        onGifSelected = listener
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}

/**
 * Simple sticker panel
 */
class SimpleStickerPanel(context: Context) : LinearLayout(context) {
    
    companion object {
        private const val TAG = "SimpleStickerPanel"
        private const val PANEL_HEIGHT_DP = 280
    }
    
    private var onStickerSelected: ((String) -> Unit)? = null
    
    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(PANEL_HEIGHT_DP))
        setBackgroundColor(Color.parseColor("#F5F5F5"))
        
        setupStickerPanel()
    }
    
    private fun setupStickerPanel() {
        // Create header
        val headerText = TextView(context).apply {
            text = "Stickers"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        addView(headerText)
        
        // Create sticker categories
        val categoryLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(50))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
        }
        
        val stickerCategories = listOf(
            Pair("Animals", "🐶"),
            Pair("Food", "🍕"),
            Pair("Hearts", "❤️"),
            Pair("Stars", "⭐")
        )
        
        stickerCategories.forEach { (name, emoji) ->
            val button = Button(context).apply {
                text = "$emoji\n$name"
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(dpToPx(1), 0, dpToPx(1), 0)
                }
                setOnClickListener { 
                    onStickerSelected?.invoke(emoji)
                    Toast.makeText(context, "Selected $name sticker", Toast.LENGTH_SHORT).show()
                }
            }
            categoryLayout.addView(button)
        }
        
        addView(categoryLayout)
        
        // Create sticker grid using LinearLayout rows
        val stickerGridContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }
        
        val basicStickers = listOf(
            "😊", "😍", "🥰", "😎", "🤔", "😴",
            "👍", "👎", "✌️", "🤞", "👌", "🙏",
            "❤️", "💙", "💚", "💛", "🧡", "💜",
            "🔥", "⭐", "✨", "💯", "👑", "🎉"
        )
        
        // Create rows of 6 stickers each
        var currentRow: LinearLayout? = null
        basicStickers.forEachIndexed { index, emoji ->
            if (index % 6 == 0) {
                currentRow = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                }
                stickerGridContainer.addView(currentRow)
            }
            
            val stickerButton = TextView(context).apply {
                text = emoji
                textSize = 20f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)).apply {
                    setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
                }
                setBackgroundResource(android.R.drawable.btn_default)
                isClickable = true
                
                setOnClickListener {
                    onStickerSelected?.invoke(emoji)
                }
            }
            
            currentRow?.addView(stickerButton)
        }
        
        val scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        scrollView.addView(stickerGridContainer)
        addView(scrollView)
    }
    
    fun setOnStickerSelectedListener(listener: (String) -> Unit) {
        onStickerSelected = listener
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
