package com.example.ai_keyboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Modern emoji panel controller matching Gboard/CleverType UX
 * - Fixed toolbar (never scrolls)
 * - Themable background matching keyboard
 * - Minimal footer with ABC, Space, Delete
 */
class EmojiPanelController(
    private val context: Context,
    private val themeManager: ThemeManager,
    private val onBackToLetters: () -> Unit,
    private val inputConnectionProvider: () -> InputConnection?
) {
    
    companion object {
        private const val TAG = "EmojiPanelController"
        private const val SPAN_COUNT = 8
        private const val DELETE_REPEAT_DELAY = 50L
    }
    
    private var root: View? = null
    private var emojiGrid: RecyclerView? = null
    private var btnABC: TextView? = null
    private var btnSpace: TextView? = null
    private var btnSend: ImageView? = null
    private var btnDelete: ImageView? = null
    private var emojiSearchInput: EditText? = null
    private var emojiCategories: LinearLayout? = null
    private var emojiToneBtn: ImageView? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var deleteRepeatRunnable: Runnable? = null
    
    // Integrated emoji panel (reuse existing GboardEmojiPanel logic)
    private var gboardEmojiPanel: GboardEmojiPanel? = null
    
    // Category selection state
    private var selectedCategoryView: TextView? = null
    private var currentCategory = "Recent"
    
    fun inflate(parent: ViewGroup): View {
        if (root != null) return root!!
        
        LogUtil.d(TAG, "Inflating emoji panel layout")
        root = LayoutInflater.from(context).inflate(R.layout.panel_emoji, parent, false)
        
        // Find views
        emojiGrid = root!!.findViewById(R.id.emojiGrid)
        btnABC = root!!.findViewById(R.id.btnEmojiToABC)
        btnSpace = root!!.findViewById(R.id.btnEmojiSpace)
        btnSend = root!!.findViewById(R.id.btnEmojiSend)
        btnDelete = root!!.findViewById(R.id.btnEmojiDelete)
        emojiSearchInput = root!!.findViewById(R.id.emojiSearchInput)
        emojiCategories = root!!.findViewById(R.id.emojiCategories)
        emojiToneBtn = root!!.findViewById(R.id.emojiToneBtn)
        
        // Set keyboard height to match normal keyboard
        val keyboardHeight = getKeyboardHeight()
        root!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            keyboardHeight
        )
        
        setupEmojiGrid()
        setupFooterButtons()
        setupToolbar()
        applyTheme()
        
        val orientation = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
        LogUtil.d(TAG, "✓ Emoji panel inflated with FIXED height: ${keyboardHeight}px ($orientation)")
        LogUtil.d(TAG, "  - Height calculation based on key_height, gaps, and padding from dimens.xml")
        LogUtil.d(TAG, "  - Grid will scroll inside this fixed panel (NOT fullscreen)")
        return root!!
    }
    
    private fun setupEmojiGrid() {
        emojiGrid?.layoutManager = GridLayoutManager(context, SPAN_COUNT)
        
        // Integrate with existing GboardEmojiPanel
        gboardEmojiPanel = GboardEmojiPanel(context).apply {
            setOnEmojiSelectedListener { emoji ->
                onEmojiClicked(emoji)
            }
            setOnBackspacePressedListener {
                backspaceOnce()
            }
        }
        
        // Extract emoji grid from GboardEmojiPanel and wire to our RecyclerView
        // For now, use a simple adapter
        val adapter = EmojiAdapter(::onEmojiClicked, ::onEmojiLongClicked)
        emojiGrid?.adapter = adapter
        
        LogUtil.d(TAG, "Emoji grid setup complete with ${SPAN_COUNT} columns")
    }
    
    private fun setupFooterButtons() {
        // ABC button - return to letters
        btnABC?.setOnClickListener {
            LogUtil.d(TAG, "ABC button clicked - returning to letters")
            onBackToLetters()
        }
        
        // Space button - insert space
        btnSpace?.setOnClickListener {
            LogUtil.d(TAG, "Space button clicked")
            inputConnectionProvider()?.commitText(" ", 1)
        }
        
        // Send/Done button - context-aware action
        btnSend?.setOnClickListener {
            val ic = inputConnectionProvider()
            try {
                // Try to perform editor action (Send/Done)
                val performed = ic?.performEditorAction(EditorInfo.IME_ACTION_SEND) ?: false
                if (!performed) {
                    // Fallback: try DONE action
                    val donePerformed = ic?.performEditorAction(EditorInfo.IME_ACTION_DONE) ?: false
                    if (!donePerformed) {
                        // Final fallback: insert newline
                        LogUtil.d(TAG, "Send button - inserting newline (fallback)")
                        ic?.commitText("\n", 1)
                    } else {
                        LogUtil.d(TAG, "Send button - performed DONE action")
                    }
                } else {
                    LogUtil.d(TAG, "Send button - performed SEND action")
                }
            } catch (e: Exception) {
                LogUtil.e(TAG, "Error performing send action", e)
                ic?.commitText("\n", 1)
            }
        }
        
        // Delete button - backspace with long-press repeat
        btnDelete?.setOnClickListener {
            backspaceOnce()
        }
        
        btnDelete?.setOnLongClickListener {
            startDeleteRepeat()
            true
        }
        
        btnDelete?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopDeleteRepeat()
                    view.performClick()
                    false
                }
                else -> false
            }
        }
        
        LogUtil.d(TAG, "Footer buttons wired successfully (including Send/Done)")
    }
    
    private fun setupToolbar() {
        // Search input
        emojiSearchInput?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    performEmojiSearch(query)
                } else {
                    // Show default emojis
                    loadDefaultEmojis()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        // Skin tone button
        emojiToneBtn?.setOnClickListener {
            LogUtil.d(TAG, "Skin tone button clicked")
            showSkinToneBottomSheet()
        }
        
        // Setup category tabs
        setupCategories()
        
        LogUtil.d(TAG, "Toolbar setup complete")
    }
    
    private fun setupCategories() {
        val categories = listOf(
            "⏰" to "Recent",
            "😊" to "Smileys",
            "👤" to "People",
            "❤️" to "Hearts",
            "🐶" to "Animals",
            "🍔" to "Food",
            "⚽" to "Activities",
            "🚗" to "Travel",
            "💡" to "Objects",
            "🏁" to "Flags"
        )
        
        categories.forEachIndexed { index, (icon, name) ->
            val categoryBtn = TextView(context).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                val size = dpToPx(32)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(dpToPx(4), 0, dpToPx(4), 0)
                }
                tag = name  // Store category name for reference
                alpha = if (index == 0) 1.0f else 0.6f  // First one selected
                setOnClickListener {
                    LogUtil.d(TAG, "Category clicked: $name")
                    selectCategory(name, this)
                }
            }
            emojiCategories?.addView(categoryBtn)
            
            // Auto-select first category (Recent)
            if (index == 0) {
                selectedCategoryView = categoryBtn
                loadCategoryEmojis(name)
            }
        }
    }
    
    private fun selectCategory(category: String, view: TextView) {
        // Update visual selection
        selectedCategoryView = view
        currentCategory = category
        
        // Apply theme to update selected/unselected states
        applyCategoryTheme()
        
        // Load emojis
        loadCategoryEmojis(category)
    }
    
    fun applyTheme() {
        try {
            val palette = themeManager.getCurrentPalette()
            
            // 1. Apply keyboard background (matches main keyboard)
            root?.setBackgroundColor(palette.keyboardBg)
            
            // 2. Apply toolbar background (matches keyboard)
            root?.findViewById<View>(R.id.emojiToolbar)?.setBackgroundColor(palette.toolbarBg)
            
            // 3. Apply category bar background
            root?.findViewById<View>(R.id.emojiCategoriesScroll)?.setBackgroundColor(palette.keyboardBg)
            
            // 4. Apply search input colors
            emojiSearchInput?.setTextColor(palette.keyText)
            emojiSearchInput?.setHintTextColor(palette.suggestionText)
            emojiSearchInput?.setBackgroundColor(Color.TRANSPARENT)
            
            // 5. Apply icon tints
            root?.findViewById<ImageView>(R.id.emojiSearchBtn)?.setColorFilter(palette.keyText)
            emojiToneBtn?.setColorFilter(palette.keyText)
            btnSend?.setColorFilter(palette.keyText)
            btnDelete?.setColorFilter(palette.keyText)
            
            // 6. Apply button backgrounds using ThemeManager
            val keyDrawable = themeManager.createKeyDrawable()
            btnABC?.background = keyDrawable.constantState?.newDrawable()?.mutate()
            btnSpace?.background = keyDrawable.constantState?.newDrawable()?.mutate()
            btnSend?.background = keyDrawable.constantState?.newDrawable()?.mutate()
            btnDelete?.background = keyDrawable.constantState?.newDrawable()?.mutate()
            
            // 7. Apply button text colors
            btnABC?.setTextColor(palette.keyText)
            btnSpace?.setTextColor(palette.keyText)
            
            // 8. Apply footer background
            root?.findViewById<View>(R.id.emojiFooter)?.setBackgroundColor(palette.keyboardBg)
            
            // 9. Apply category button theming
            applyCategoryTheme()
            
            LogUtil.d(TAG, "Theme applied successfully to emoji panel")
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error applying theme", e)
        }
    }
    
    private fun applyCategoryTheme() {
        val palette = themeManager.getCurrentPalette()
        emojiCategories?.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i) as? TextView ?: continue
                val isSelected = child == selectedCategoryView
                
                // Selected: Use keyBg background
                // Unselected: Transparent background with reduced opacity
                if (isSelected) {
                    val selectedBg = GradientDrawable().apply {
                        cornerRadius = dpToPx(8).toFloat()
                        setColor(palette.keyBg)
                    }
                    child.background = selectedBg
                    child.alpha = 1.0f
                } else {
                    child.background = null
                    child.alpha = 0.6f
                }
                child.setTextColor(palette.keyText)
            }
        }
    }
    
    private fun onEmojiClicked(emoji: String) {
        LogUtil.d(TAG, "Emoji clicked: $emoji")
        
        // Apply skin tone if supported
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val preferredTone = prefs.getString("preferred_skin_tone", "") ?: ""
        
        // Get base emoji (without tone)
        val baseEmoji = getBaseEmoji(emoji)
        
        // Check if there's a per-emoji default tone (like Gboard)
        val defaultTone = getDefaultEmojiTone(prefs, baseEmoji)
        
        // Apply tone: per-emoji default > global preferred > no tone
        val finalEmoji = when {
            defaultTone != null -> defaultTone
            preferredTone.isNotEmpty() && supportsSkinTone(baseEmoji) -> baseEmoji + preferredTone
            else -> emoji
        }
        
        inputConnectionProvider()?.commitText(finalEmoji, 1)
        
        // Update emoji history (LRU)
        updateEmojiHistory(prefs, finalEmoji)
        
        // Reload Recent category if currently viewing it
        if (currentCategory == "Recent") {
            loadCategoryEmojis("Recent")
        }
        
        LogUtil.d(TAG, "Inserted emoji: $finalEmoji (base: $baseEmoji, tone: ${if (defaultTone != null) "default" else preferredTone})")
    }
    
    private fun getBaseEmoji(emojiWithTone: String): String {
        val skinToneModifiers = listOf("🏻", "🏼", "🏽", "🏾", "🏿")
        skinToneModifiers.forEach { modifier ->
            if (emojiWithTone.contains(modifier)) {
                return emojiWithTone.replace(modifier, "")
            }
        }
        return emojiWithTone
    }
    
    private fun getDefaultEmojiTone(prefs: SharedPreferences, baseEmoji: String): String? {
        val defaultTonesJson = prefs.getString("default_emoji_tones", "{}") ?: "{}"
        try {
            if (defaultTonesJson != "{}") {
                // Simple JSON parsing for key-value pairs
                defaultTonesJson.removeSurrounding("{", "}").split(", ").forEach { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim('"')
                        val value = parts[1].trim('"')
                        if (key == baseEmoji) {
                            return value
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LogUtil.w(TAG, "Error parsing default emoji tones: ${e.message}")
        }
        return null
    }
    
    private fun updateEmojiHistory(prefs: SharedPreferences, emoji: String) {
        try {
            // Load current history
            val historyJson = prefs.getString("emoji_history", "[]") ?: "[]"
            val history = mutableListOf<String>()
            
            val cleanJson = historyJson.trim('[', ']')
            if (cleanJson.isNotEmpty()) {
                cleanJson.split(",").forEach { e ->
                    val cleanEmoji = e.trim().trim('"')
                    if (cleanEmoji.isNotEmpty()) {
                        history.add(cleanEmoji)
                    }
                }
            }
            
            // Remove if already exists (move to front)
            history.remove(emoji)
            
            // Add to front
            history.add(0, emoji)
            
            // Trim to max size
            val maxSize = prefs.getInt("emoji_history_max_size", 90)
            while (history.size > maxSize) {
                history.removeAt(history.size - 1)
            }
            
            // Save back to preferences
            val newHistoryJson = history.joinToString(",") { "\"$it\"" }
            prefs.edit().putString("emoji_history", "[$newHistoryJson]").apply()
            
            LogUtil.d(TAG, "Updated emoji history: added '$emoji' (total: ${history.size})")
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error updating emoji history", e)
        }
    }
    
    private fun onEmojiLongClicked(emoji: String, anchorView: View): Boolean {
        if (!supportsSkinTone(emoji)) return false
        showEmojiVariants(emoji, anchorView)
        return true
    }
    
    private fun showEmojiVariants(emoji: String, anchorView: View) {
        val baseEmoji = getBaseEmoji(emoji)
        val tones = listOf("", "🏻", "🏼", "🏽", "🏾", "🏿")
        val variants = tones.map { if (it.isEmpty()) baseEmoji else baseEmoji + it }
        
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val currentDefault = getDefaultEmojiTone(prefs, baseEmoji)
        
        val popup = PopupWindow(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val palette = themeManager.getCurrentPalette()
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(12).toFloat()
                setColor(palette.keyBg)
            }
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            elevation = dpToPx(8).toFloat()
        }
        
        variants.forEach { variant ->
            val isDefault = (variant == currentDefault) || (currentDefault == null && variant == baseEmoji)
            
            val btn = TextView(context).apply {
                text = variant
                textSize = 28f
                gravity = Gravity.CENTER
                val size = dpToPx(48)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(dpToPx(4), 0, dpToPx(4), 0)
                }
                
                // Highlight current default (like Gboard)
                if (isDefault) {
                    background = GradientDrawable().apply {
                        cornerRadius = dpToPx(8).toFloat()
                        setColor(themeManager.getCurrentPalette().specialAccent)
                    }
                    setTextColor(Color.WHITE)
                }
                
                setOnClickListener {
                    // Save as default for this emoji (like Gboard)
                    saveDefaultEmojiTone(prefs, baseEmoji, variant)
                    
                    // Insert the selected emoji
                    onEmojiClicked(variant)
                    popup.dismiss()
                    
                    // Reload category to show updated defaults in grid
                    loadCategoryEmojis(currentCategory)
                    
                    LogUtil.d(TAG, "Set default tone for $baseEmoji to $variant")
                }
            }
            layout.addView(btn)
        }
        
        popup.contentView = layout
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popup.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.elevation = dpToPx(8).toFloat()
        
        // Show popup above the emoji
        popup.showAsDropDown(anchorView, 0, -dpToPx(60))
        
        LogUtil.d(TAG, "Showing skin tone variants for: $baseEmoji (current default: $currentDefault)")
    }
    
    private fun saveDefaultEmojiTone(prefs: SharedPreferences, baseEmoji: String, toneVariant: String) {
        try {
            // Load existing defaults
            val defaultTonesJson = prefs.getString("default_emoji_tones", "{}") ?: "{}"
            val defaultTones = mutableMapOf<String, String>()
            
            if (defaultTonesJson != "{}") {
                defaultTonesJson.removeSurrounding("{", "}").split(", ").forEach { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) {
                        defaultTones[parts[0].trim('"')] = parts[1].trim('"')
                    }
                }
            }
            
            // Update or add this emoji's default
            defaultTones[baseEmoji] = toneVariant
            
            // Save back
            val jsonString = defaultTones.entries.joinToString(", ") { "\"${it.key}\"=\"${it.value}\"" }
            prefs.edit().putString("default_emoji_tones", "{$jsonString}").apply()
            
            LogUtil.d(TAG, "Saved default tone for '$baseEmoji' → '$toneVariant'")
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error saving default emoji tone", e)
        }
    }
    
    private fun supportsSkinTone(emoji: String): Boolean {
        // Emojis that support Fitzpatrick modifiers
        val skinToneSupportedEmojis = setOf(
            "👋", "🤚", "🖐", "✋", "🖖", "👌", "🤌", "🤏", "✌", "🤞", "🤟",
            "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝", "👍", "👎", "✊",
            "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍", "💅",
            "🤳", "💪", "🦵", "🦶", "👂", "🦻", "👃", "👶", "👧", "🧒", "👦",
            "👩", "🧑", "👨", "👩‍🦱", "🧑‍🦱", "👨‍🦱", "👩‍🦰", "🧑‍🦰", "👨‍🦰",
            "👱‍♀️", "👱", "👱‍♂️", "👩‍🦳", "🧑‍🦳", "👨‍🦳", "👩‍🦲", "🧑‍🦲", "👨‍🦲",
            "🧔", "👵", "🧓", "👴", "👲", "👳‍♀️", "👳", "👳‍♂️", "🧕", "👮‍♀️",
            "👮", "👮‍♂️", "👷‍♀️", "👷", "👷‍♂️", "💂‍♀️", "💂", "💂‍♂️"
        )
        return emoji in skinToneSupportedEmojis
    }
    
    private fun backspaceOnce() {
        inputConnectionProvider()?.deleteSurroundingText(1, 0)
    }
    
    private fun startDeleteRepeat() {
        deleteRepeatRunnable = object : Runnable {
            override fun run() {
                backspaceOnce()
                mainHandler.postDelayed(this, DELETE_REPEAT_DELAY)
            }
        }
        mainHandler.postDelayed(deleteRepeatRunnable!!, DELETE_REPEAT_DELAY)
        LogUtil.d(TAG, "Delete repeat started")
    }
    
    private fun stopDeleteRepeat() {
        deleteRepeatRunnable?.let {
            mainHandler.removeCallbacks(it)
            deleteRepeatRunnable = null
            LogUtil.d(TAG, "Delete repeat stopped")
        }
    }
    
    private fun performEmojiSearch(query: String) {
        LogUtil.d(TAG, "Searching emojis: $query")
        val searchResults = EmojiSuggestionEngine.searchEmojis(query)
        updateEmojiGrid(searchResults)
    }
    
    private fun loadDefaultEmojis() {
        val defaultEmojis = EmojiCollection.popularEmojis
        updateEmojiGrid(defaultEmojis)
    }
    
    private fun loadCategoryEmojis(category: String) {
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val preferredTone = prefs.getString("preferred_skin_tone", "") ?: ""
        
        val emojis = when (category) {
            "Recent" -> {
                // Load history from SharedPreferences
                val historyJson = prefs.getString("emoji_history", "[]") ?: "[]"
                val history = mutableListOf<String>()
                try {
                    val cleanJson = historyJson.trim('[', ']')
                    if (cleanJson.isNotEmpty()) {
                        cleanJson.split(",").forEach { emoji ->
                            val cleanEmoji = emoji.trim().trim('"')
                            if (cleanEmoji.isNotEmpty()) {
                                history.add(cleanEmoji)
                            }
                        }
                    }
                } catch (e: Exception) {
                    LogUtil.e(TAG, "Error loading emoji history", e)
                }
                
                // If no history, show popular emojis with tone applied
                if (history.isEmpty()) {
                    EmojiCollection.popularEmojis.map { applyPreferredSkinTone(it, preferredTone) }
                } else {
                    history
                }
            }
            "Smileys" -> EmojiCollection.smileys.map { applyPreferredSkinTone(it, preferredTone) }
            "People" -> EmojiCollection.people.map { applyPreferredSkinTone(it, preferredTone) }
            "Hearts" -> EmojiCollection.hearts.map { applyPreferredSkinTone(it, preferredTone) }
            "Animals" -> EmojiCollection.animals.map { applyPreferredSkinTone(it, preferredTone) }
            "Food" -> EmojiCollection.food.map { applyPreferredSkinTone(it, preferredTone) }
            "Activities" -> EmojiCollection.activities.map { applyPreferredSkinTone(it, preferredTone) }
            "Travel" -> EmojiCollection.travel.map { applyPreferredSkinTone(it, preferredTone) }
            "Objects" -> EmojiCollection.objects.map { applyPreferredSkinTone(it, preferredTone) }
            "Flags" -> EmojiCollection.flags // Flags don't have skin tones
            else -> EmojiCollection.popularEmojis.map { applyPreferredSkinTone(it, preferredTone) }
        }
        updateEmojiGrid(emojis)
    }
    
    private fun applyPreferredSkinTone(baseEmoji: String, preferredTone: String): String {
        val cleanBase = getBaseEmoji(baseEmoji)
        
        // Check for per-emoji default first
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val defaultTone = getDefaultEmojiTone(prefs, cleanBase)
        if (defaultTone != null) {
            return defaultTone
        }
        
        // Apply global preferred tone if applicable
        return if (preferredTone.isNotEmpty() && supportsSkinTone(cleanBase)) {
            cleanBase + preferredTone
        } else {
            baseEmoji
        }
    }
    
    private fun updateEmojiGrid(emojis: List<String>) {
        (emojiGrid?.adapter as? EmojiAdapter)?.updateEmojis(emojis)
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    private fun getKeyboardHeight(): Int {
        // Match the letters keyboard height exactly
        // Letters keyboard: 5 rows (or 6 with number row) × 50dp key_height + gaps + padding
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val keyHeight = context.resources.getDimensionPixelSize(R.dimen.key_height) // 50dp
        val rows = 5 // Standard: 4 letter rows + 1 bottom row
        val verticalGap = context.resources.getDimensionPixelSize(R.dimen.keyboard_vertical_gap) // 2dp
        val padding = context.resources.getDimensionPixelSize(R.dimen.keyboard_padding) // 4dp
        val suggestionBarHeight = context.resources.getDimensionPixelSize(R.dimen.suggestion_bar_height) // 36dp
        
        // Calculate total keyboard height = suggestion bar + (rows × key height) + (rows-1 × gaps) + padding
        val calculatedHeight = suggestionBarHeight + (rows * keyHeight) + ((rows - 1) * verticalGap) + (padding * 2)
        
        LogUtil.d(TAG, "Keyboard height calculation:")
        LogUtil.d(TAG, "  - Key height: ${keyHeight}px")
        LogUtil.d(TAG, "  - Rows: $rows")
        LogUtil.d(TAG, "  - Vertical gap: ${verticalGap}px")
        LogUtil.d(TAG, "  - Padding: ${padding}px")
        LogUtil.d(TAG, "  - Suggestion bar: ${suggestionBarHeight}px")
        LogUtil.d(TAG, "  - Calculated: ${calculatedHeight}px")
        
        // Use calculated height or fallback to hardcoded values
        val finalHeight = if (isLandscape) {
            minOf(calculatedHeight, dpToPx(220)) // Landscape: smaller
        } else {
            calculatedHeight // Portrait: full height
        }
        
        LogUtil.d(TAG, "  - Final height (${if (isLandscape) "landscape" else "portrait"}): ${finalHeight}px")
        return finalHeight
    }
    
    private fun showSkinToneBottomSheet() {
        val tones = listOf(
            "" to "Default (Yellow)",
            "🏻" to "Light",
            "🏼" to "Medium-Light",
            "🏽" to "Medium",
            "🏾" to "Medium-Dark",
            "🏿" to "Dark"
        )
        
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val currentTone = prefs.getString("preferred_skin_tone", "🏽") ?: "🏽"
        
        // Create popup window
        val popup = PopupWindow(context)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val palette = themeManager.getCurrentPalette()
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(12).toFloat()
                setColor(palette.keyBg)
            }
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            elevation = dpToPx(8).toFloat()
        }
        
        // Add title
        val title = TextView(context).apply {
            text = "Select Skin Tone"
            textSize = 16f
            setTextColor(themeManager.getCurrentPalette().keyText)
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(16))
            gravity = Gravity.CENTER
        }
        layout.addView(title)
        
        tones.forEach { (modifier, name) ->
            val option = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                
                // Highlight current selection
                if (modifier == currentTone) {
                    background = GradientDrawable().apply {
                        cornerRadius = dpToPx(8).toFloat()
                        setColor(ColorUtils.blendARGB(
                            themeManager.getCurrentPalette().specialAccent, 
                            Color.WHITE, 
                            0.2f
                        ))
                    }
                }
                
                setOnClickListener {
                    // Save skin tone preference
                    prefs.edit().putString("preferred_skin_tone", modifier).apply()
                    
                    // Notify via MainActivity's broadcast system
                    val intent = Intent("com.example.ai_keyboard.EMOJI_SETTINGS_CHANGED").apply {
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(intent)
                    
                    popup.dismiss()
                    
                    LogUtil.d(TAG, "Skin tone changed to: $modifier ($name)")
                    
                    // Reload current category to show updated emojis
                    loadCategoryEmojis(currentCategory)
                }
            }
            
            // Emoji preview
            val emojiPreview = TextView(context).apply {
                text = "👋$modifier"
                textSize = 24f
                layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48))
                gravity = Gravity.CENTER
            }
            option.addView(emojiPreview)
            
            // Tone name
            val toneName = TextView(context).apply {
                text = name
                textSize = 14f
                setTextColor(themeManager.getCurrentPalette().keyText)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setPadding(dpToPx(12), 0, 0, 0)
            }
            option.addView(toneName)
            
            layout.addView(option)
        }
        
        popup.contentView = layout
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.width = dpToPx(280)
        popup.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.elevation = dpToPx(8).toFloat()
        
        // Show popup above the tone button
        emojiToneBtn?.let { btn ->
            popup.showAsDropDown(btn, -dpToPx(120), -dpToPx(400))
        }
        
        LogUtil.d(TAG, "Skin tone selector opened")
    }
    
    /**
     * Reload emoji settings and refresh UI
     */
    fun reloadEmojiSettings() {
        gboardEmojiPanel?.reloadEmojiSettings()
        loadCategoryEmojis(currentCategory)
        applyTheme()
        LogUtil.d(TAG, "Emoji settings reloaded")
    }
    
    /**
     * Simple emoji adapter for RecyclerView
     */
    class EmojiAdapter(
        private val onEmojiClick: (String) -> Unit,
        private val onEmojiLongClick: ((String, View) -> Boolean)? = null
    ) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {
        
        private val emojis = mutableListOf<String>()
        
        init {
            // Load default popular emojis
            emojis.addAll(EmojiCollection.popularEmojis)
        }
        
        fun updateEmojis(newEmojis: List<String>) {
            emojis.clear()
            emojis.addAll(newEmojis)
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
            val textView = TextView(parent.context).apply {
                textSize = 24f
                gravity = Gravity.CENTER
                val size = (parent.context.resources.displayMetrics.density * 40).toInt()
                layoutParams = ViewGroup.LayoutParams(size, size)
            }
            return EmojiViewHolder(textView)
        }
        
        override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
            holder.bind(emojis[position], onEmojiClick, onEmojiLongClick)
        }
        
        override fun getItemCount() = emojis.size
        
        class EmojiViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
            fun bind(
                emoji: String, 
                onClick: (String) -> Unit,
                onLongClick: ((String, View) -> Boolean)? = null
            ) {
                textView.text = emoji
                textView.setOnClickListener { onClick(emoji) }
                textView.setOnLongClickListener { view ->
                    onLongClick?.invoke(emoji, view) ?: false
                }
            }
        }
    }
}

