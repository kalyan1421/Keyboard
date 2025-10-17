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
import android.widget.HorizontalScrollView
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
import com.example.ai_keyboard.stickers.StickerServiceAdapter
import com.example.ai_keyboard.stickers.StickerData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var btnDelete: ImageView? = null
    private var emojiSearchInput: EditText? = null
    private var emojiCategories: LinearLayout? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var deleteRepeatRunnable: Runnable? = null
    
    // Sticker integration
    private val stickerService = StickerServiceAdapter(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentStickerPacks = listOf<com.example.ai_keyboard.stickers.StickerPack>()
    private var currentStickers = listOf<StickerData>()
    
    // Integrated emoji panel (reuse existing GboardEmojiPanel logic)
    // gboardEmojiPanel removed - using direct emoji management
    
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
        btnDelete = root!!.findViewById(R.id.btnEmojiDelete)
        emojiSearchInput = root!!.findViewById(R.id.emojiSearchInput)
        emojiCategories = root!!.findViewById(R.id.emojiCategories)
        
        
        // Set keyboard height to match normal keyboard with FIXED dimensions
        val keyboardHeight = getKeyboardHeight()
        root!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            keyboardHeight
        )
        
        // Ensure the root panel itself is completely FIXED - no scrolling at all
        root!!.isScrollContainer = false
        root!!.overScrollMode = View.OVER_SCROLL_NEVER
        
        // Ensure all parent containers don't scroll
        (root as? LinearLayout)?.apply {
            isScrollContainer = false
        }
        
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
        emojiGrid?.apply {
            layoutManager = GridLayoutManager(context, SPAN_COUNT)
            
            // CRITICAL: Ensure ONLY this RecyclerView scrolls, not the entire panel
            isNestedScrollingEnabled = true // Enable scrolling for this RecyclerView
            overScrollMode = View.OVER_SCROLL_NEVER // Remove overscroll bounce effects
            setHasFixedSize(true) // Optimize performance
            isScrollbarFadingEnabled = true // Fade scrollbar for cleaner look
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            
            // Extract emoji grid from GboardEmojiPanel and wire to our RecyclerView
            val adapter = EmojiAdapter(::onEmojiClicked, ::onEmojiLongClicked)
            this.adapter = adapter
            
            LogUtil.d(TAG, "✅ Emoji grid ONLY scrolls - panel header/footer fixed")
        }
        
        // Use EmojiDatabase directly for emoji data (GboardEmojiPanel removed)
    }
    
    private fun setupFooterButtons() {
        // ABC button - return to normal keyboard (transparent background)
        btnABC?.setOnClickListener {
            LogUtil.d(TAG, "ABC button clicked - returning to letters")
            onBackToLetters()
        }
        
        // Delete button - backspace with long-press repeat
        btnDelete?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    LogUtil.d(TAG, "🗑️ Delete button touched")
                    backspaceOnce() // Delete once immediately
                    false // Allow long press to be detected
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    LogUtil.d(TAG, "🗑️ Delete button released")
                    stopDeleteRepeat()
                    false
                }
                else -> false
            }
        }
        
        btnDelete?.setOnLongClickListener {
            LogUtil.d(TAG, "🗑️ Delete button long-pressed - starting repeat")
            startDeleteRepeat()
            true // Consume the long press event
        }
        
        LogUtil.d(TAG, "Footer buttons wired successfully (including Send/Done)")
    }
    
    private fun setupToolbar() {
        // Setup category tabs (CleverType style)
        setupCategories()
        
        LogUtil.d(TAG, "Toolbar setup complete (CleverType style)")
    }
    
    private fun showSearchDialog() {
        // Create a simple search popup
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
        
        // Add search input
        val searchInput = EditText(context).apply {
            hint = "Search emojis..."
            textSize = 16f
            setTextColor(themeManager.getCurrentPalette().keyText)
            setHintTextColor(themeManager.getCurrentPalette().suggestionText)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(dpToPx(280), dpToPx(48))
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(8).toFloat()
                setColor(themeManager.getCurrentPalette().keyboardBg)
            }
            
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val query = s.toString().trim()
                    if (query.isNotEmpty()) {
                        performEmojiSearch(query)
                    } else {
                        loadDefaultEmojis()
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        layout.addView(searchInput)
        
        popup.contentView = layout
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popup.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.elevation = dpToPx(8).toFloat()
        
        // Show popup at the top of the panel
        root?.let { rootView ->
            popup.showAtLocation(rootView, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, dpToPx(60))
        }
        
        // Request focus on search input
        searchInput.requestFocus()
        
        LogUtil.d(TAG, "Search dialog opened")
    }
    
    private fun setupCategories() {
        // CleverType style categories matching the reference image + Stickers
        val categories = listOf(
            "⏰" to "Recent",
            "😊" to "Smileys", 
            "📦" to "Stickers",
            "🏃" to "Activities",
            "📋" to "Objects",
            "😃" to "People",
            "💡" to "Symbols",
            "🍔" to "Food",
            "🐶" to "Animals",
            "🚗" to "Travel",
            "🏁" to "Flags"
        )
        
        // Add ABC button at the start
        val abcBtn = TextView(context).apply {
            text = "ABC"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            val size = dpToPx(48)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(dpToPx(4), 0, dpToPx(4), 0)
            }
            tag = "ABC"
            alpha = 1.0f
            
            setOnClickListener {
                LogUtil.d(TAG, "ABC button clicked - returning to letters")
                onBackToLetters()
            }
        }
        emojiCategories?.addView(abcBtn)
        
        categories.forEachIndexed { index, (icon, name) ->
            val categoryBtn = TextView(context).apply {
                text = icon
                textSize = 28f // Larger icons like CleverType
                gravity = Gravity.CENTER
                val size = dpToPx(48) // Bigger touch targets
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(dpToPx(4), 0, dpToPx(4), 0)
                }
                tag = name  // Store category name for reference
                alpha = if (index == 0) 1.0f else 0.6f  // Recent selected by default (index 0 now)
                
                setOnClickListener {
                    LogUtil.d(TAG, "Category clicked: $name")
                    if (name == "Stickers") {
                        // Load stickers
                        selectCategory(name, this)
                        loadStickers()
                    } else {
                        selectCategory(name, this)
                    }
                }
            }
            emojiCategories?.addView(categoryBtn)
            
            // Auto-select Recent category (index 0 now)
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
        
        // Auto-scroll to selected category if it's off screen
        scrollToSelectedCategory(view)
        
        // Load emojis
        loadCategoryEmojis(category)
    }
    
    /**
     * Scroll the category bar to ensure selected category is visible
     */
    private fun scrollToSelectedCategory(selectedView: TextView) {
        try {
            val categoryScrollView = root?.findViewById<HorizontalScrollView>(R.id.emojiCategoryScroll)
            categoryScrollView?.let { scrollView ->
                // Calculate the position of the selected category
                val scrollX = selectedView.left - (scrollView.width / 2) + (selectedView.width / 2)
                
                // Smooth scroll to center the selected category
                scrollView.smoothScrollTo(scrollX, 0)
                
                LogUtil.d(TAG, "📍 Auto-scrolled to selected category: $currentCategory")
            }
        } catch (e: Exception) {
            LogUtil.w(TAG, "Could not auto-scroll to category", e)
        }
    }
    
    fun applyTheme() {
        try {
            val palette = themeManager.getCurrentPalette()
            
            // 1. Apply main background (overrides XML @color/kb_panel_bg)
            root?.setBackgroundColor(palette.keyboardBg)
            
            // 2. Apply category bar background (now HorizontalScrollView)
            root?.findViewById<HorizontalScrollView>(R.id.emojiCategoryScroll)?.apply {
                setBackgroundColor(palette.keyboardBg)
                // Ensure smooth horizontal scrolling for categories
                isSmoothScrollingEnabled = true
                isHorizontalScrollBarEnabled = false // Hide scrollbar for clean look
            }
            root?.findViewById<LinearLayout>(R.id.emojiCategories)?.setBackgroundColor(palette.keyboardBg)
            
            // 3. Apply emoji grid background
            root?.findViewById<View>(R.id.emojiGrid)?.setBackgroundColor(palette.keyboardBg)
            
            // 4. Apply bottom bar background (overrides hardcoded colors)
            root?.findViewById<View>(R.id.emojiBottomBar)?.setBackgroundColor(palette.keyboardBg)
            
            // 5. Apply button text colors (ABC button - overrides @color/kb_text_primary)
            btnABC?.setTextColor(palette.keyText)
            btnDelete?.setColorFilter(palette.keyText)
            
            // 6. Apply search input colors if visible
            emojiSearchInput?.apply {
                setTextColor(palette.keyText)
                setHintTextColor(palette.keyText)
                setBackgroundColor(palette.keyBg)
            }
            
            // 7. Apply category button theming
            applyCategoryTheme()
            
            LogUtil.d(TAG, "✅ Complete theme applied to emoji panel (overriding XML colors)")
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error applying theme", e)
        }
    }
    
    private fun applyCategoryTheme() {
        val palette = themeManager.getCurrentPalette()
        
        // Force override container backgrounds to match keyboard theme
        emojiCategories?.setBackgroundColor(palette.keyboardBg)
        root?.findViewById<HorizontalScrollView>(R.id.emojiCategoryScroll)?.setBackgroundColor(palette.keyboardBg)
        
        emojiCategories?.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i) as? TextView ?: continue
                val isSelected = child == selectedCategoryView
                val isAbcButton = child.tag == "ABC"
                
                // ABC button: No background, always visible, distinct styling
                // Selected: Use theme accent background for clear visibility
                // Unselected: Transparent background with reduced opacity
                if (isAbcButton) {
                    child.background = null
                    child.alpha = 1.0f
                    child.setTextColor(palette.keyText)
                } else if (isSelected) {
                    val selectedBg = GradientDrawable().apply {
                        cornerRadius = dpToPx(16).toFloat()
                        setColor(palette.specialAccent) // Full accent color
                        setStroke(2, palette.specialAccent)
                    }
                    child.background = selectedBg
                    child.alpha = 1.0f
                    child.setTextColor(Color.WHITE) // White text on accent background
                } else {
                    child.background = null
                    child.alpha = 0.6f
                    child.setTextColor(palette.keyText)
                }
            }
        }
        
        LogUtil.d(TAG, "✅ Category theme applied - forcing keyboard background colors")
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
        
        // Log detailed emoji information for debugging
        LogUtil.d(TAG, "About to insert emoji: '$finalEmoji'")
        LogUtil.d(TAG, "  - Emoji length: ${finalEmoji.length}")
        LogUtil.d(TAG, "  - Emoji codepoints: ${finalEmoji.codePoints().toArray().joinToString(",") { "U+${it.toString(16).uppercase()}" }}")
        LogUtil.d(TAG, "  - Emoji bytes: ${finalEmoji.toByteArray(Charsets.UTF_8).joinToString(",") { it.toString() }}")
        
        val ic = inputConnectionProvider()
        if (ic != null) {
            // Commit as CharSequence to ensure proper encoding
            ic.commitText(finalEmoji as CharSequence, 1)
            LogUtil.d(TAG, "✅ Successfully committed emoji via InputConnection")
        } else {
            LogUtil.e(TAG, "❌ InputConnection is null, cannot insert emoji")
        }
        
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
        try {
            val ic = inputConnectionProvider()
            if (ic != null) {
                ic.deleteSurroundingText(1, 0)
                LogUtil.d(TAG, "✅ Deleted 1 character via inputConnection")
            } else {
                LogUtil.e(TAG, "❌ InputConnection is null, cannot delete")
            }
        } catch (e: Exception) {
            LogUtil.e(TAG, "❌ Error deleting character: ${e.message}")
        }
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
            "Animals" -> EmojiCollection.animals.map { applyPreferredSkinTone(it, preferredTone) }
            "Food" -> EmojiCollection.food.map { applyPreferredSkinTone(it, preferredTone) }
            "Activities" -> EmojiCollection.activities.map { applyPreferredSkinTone(it, preferredTone) }
            "Travel" -> EmojiCollection.travel.map { applyPreferredSkinTone(it, preferredTone) }
            "Objects" -> EmojiCollection.objects.map { applyPreferredSkinTone(it, preferredTone) }
            "Symbols" -> (EmojiCollection.hearts + EmojiCollection.flags).map { applyPreferredSkinTone(it, preferredTone) } // Combine hearts + symbols
            "Flags" -> EmojiCollection.flags // Flags don't have skin tones
            "Stickers" -> {
                // For stickers, we'll handle this differently in loadStickers()
                // Return empty list here and let loadStickers() handle the UI
                return
            }
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
        val keyHeight = context.resources.getDimensionPixelSize(R.dimen.key_height)
        val rows = 5 // Standard: 4 letter rows + 1 bottom row
        val verticalGap = context.resources.getDimensionPixelSize(R.dimen.keyboard_vertical_gap)
        val paddingTop = context.resources.getDimensionPixelSize(R.dimen.keyboard_padding_top)
        val paddingBottom = context.resources.getDimensionPixelSize(R.dimen.keyboard_padding_bottom)
        val suggestionBarHeight = context.resources.getDimensionPixelSize(R.dimen.suggestion_bar_height)
        
        // Calculate total keyboard height = suggestion bar + (rows × key height) + (rows-1 × gaps) + padding
        val calculatedHeight = suggestionBarHeight + (rows * keyHeight) + ((rows - 1) * verticalGap) + paddingTop + paddingBottom
        
        LogUtil.d(TAG, "Keyboard height calculation:")
        LogUtil.d(TAG, "  - Key height: ${keyHeight}px")
        LogUtil.d(TAG, "  - Rows: $rows")
        LogUtil.d(TAG, "  - Vertical gap: ${verticalGap}px")
        LogUtil.d(TAG, "  - Padding top: ${paddingTop}px, bottom: ${paddingBottom}px")
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
        
        
        
        LogUtil.d(TAG, "Skin tone selector opened")
    }
    
    /**
     * Reload emoji settings and refresh UI
     */
    fun reloadEmojiSettings() {
            // gboardEmojiPanel removed - handle settings directly
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
                
                // Explicitly use system emoji font
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                
                // Enable emoji rendering optimizations
                includeFontPadding = false
            }
            return EmojiViewHolder(textView)
        }
        
        override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
            holder.bind(emojis[position], onEmojiClick, onEmojiLongClick)
        }
        
        override fun getItemCount() = emojis.size
        
        /**
         * Set a custom click handler for stickers
         */
        fun setCustomClickHandler(handler: (Int) -> Unit) {
            customClickHandler = handler
        }
        
        private var customClickHandler: ((Int) -> Unit)? = null
        
        class EmojiViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
            fun bind(
                emoji: String, 
                onClick: (String) -> Unit,
                onLongClick: ((String, View) -> Boolean)? = null
            ) {
                textView.text = emoji
                textView.setOnClickListener { 
                    // Check if we have a custom click handler (for stickers)
                    val adapter = bindingAdapter as? EmojiAdapter
                    adapter?.customClickHandler?.let { handler ->
                        handler(adapterPosition)
                    } ?: run {
                        onClick(emoji)
                    }
                }
                textView.setOnLongClickListener { view ->
                    onLongClick?.invoke(emoji, view) ?: false
                }
            }
        }
    }
    
    // -----------------------------------------------------
    // 🔥 STICKER INTEGRATION METHODS
    // -----------------------------------------------------
    
    /**
     * Load stickers from Firebase and display them in the emoji grid
     */
    private fun loadStickers() {
        LogUtil.d(TAG, "Loading stickers from Firebase...")
        
        coroutineScope.launch {
            try {
                // Show loading state
                updateEmojiGrid(listOf("⏳"))
                
                // Load sticker packs from Firebase
                val packs = withContext(Dispatchers.IO) {
                    stickerService.getAvailablePacks()
                }
                
                currentStickerPacks = packs
                LogUtil.d(TAG, "Loaded ${packs.size} sticker packs")
                
                if (packs.isNotEmpty()) {
                    // Load stickers from the first pack
                    loadStickersFromPack(packs.first().id)
                } else {
                    // Show empty state
                    updateEmojiGrid(listOf("📦"))
                    LogUtil.w(TAG, "No sticker packs available")
                }
                
            } catch (e: Exception) {
                LogUtil.e(TAG, "Error loading stickers", e)
                // Show error state
                updateEmojiGrid(listOf("❌"))
            }
        }
    }
    
    /**
     * Load stickers from a specific pack
     */
    private fun loadStickersFromPack(packId: String) {
        coroutineScope.launch {
            try {
                val stickers = withContext(Dispatchers.IO) {
                    stickerService.getStickersFromPack(packId)
                }
                
                currentStickers = stickers
                LogUtil.d(TAG, "Loaded ${stickers.size} stickers from pack $packId")
                
                // Convert stickers to displayable format
                val stickerEmojis = stickers.map { sticker ->
                    // Use emoji representation if available, otherwise use a placeholder
                    if (sticker.emojis.isNotEmpty()) {
                        sticker.emojis.first()
                    } else {
                        "🖼️" // Placeholder for stickers without emoji representation
                    }
                }
                
                // Update the emoji grid with sticker emojis
                updateEmojiGrid(stickerEmojis)
                
                // Override the adapter's click handler for stickers
                setupStickerClickHandling()
                
            } catch (e: Exception) {
                LogUtil.e(TAG, "Error loading stickers from pack $packId", e)
                updateEmojiGrid(listOf("❌"))
            }
        }
    }
    
    /**
     * Setup click handling specifically for stickers
     */
    private fun setupStickerClickHandling() {
        emojiGrid?.adapter?.let { adapter ->
            if (adapter is EmojiAdapter && currentCategory == "Stickers") {
                // Set custom click handler for stickers
                adapter.setCustomClickHandler { position ->
                    handleStickerClick(position)
                }
                LogUtil.d(TAG, "Sticker click handling enabled for ${currentStickers.size} stickers")
            }
        }
    }
    
    /**
     * Handle sticker click - different from emoji click
     */
    private fun handleStickerClick(position: Int) {
        if (position >= 0 && position < currentStickers.size) {
            val sticker = currentStickers[position]
            
            coroutineScope.launch {
                try {
                    // Record sticker usage
                    withContext(Dispatchers.IO) {
                        stickerService.recordStickerUsage(sticker.id)
                    }
                    
                    // Get local path or use URL
                    val content = withContext(Dispatchers.IO) {
                        stickerService.downloadStickerIfNeeded(sticker)
                    } ?: sticker.imageUrl
                    
                    // Insert sticker content into text field
                    insertStickerContent(content, sticker)
                    
                    LogUtil.d(TAG, "Sticker selected: ${sticker.id}")
                    
                } catch (e: Exception) {
                    LogUtil.e(TAG, "Error handling sticker click", e)
                    // Fallback to emoji if available
                    if (sticker.emojis.isNotEmpty()) {
                        insertEmojiDirectly(sticker.emojis.first())
                    }
                }
            }
        }
    }
    
    /**
     * Insert sticker content (similar to AIKeyboardService.insertStickerContent)
     */
    private fun insertStickerContent(content: String, stickerData: StickerData) {
        val ic = inputConnectionProvider()
        if (ic == null) {
            LogUtil.w(TAG, "No input connection available for sticker insertion")
            return
        }
        
        try {
            when {
                content.startsWith("/") && java.io.File(content).exists() -> {
                    // Local file - try to insert as rich content, fallback to emoji
                    if (stickerData.emojis.isNotEmpty()) {
                        ic.commitText(stickerData.emojis.first(), 1)
                        saveEmojiToHistory(stickerData.emojis.first())
                    } else {
                        ic.commitText("[Sticker: ${stickerData.id}]", 1)
                    }
                }
                content.startsWith("http") -> {
                    // Network URL - fallback to emoji or text
                    if (stickerData.emojis.isNotEmpty()) {
                        ic.commitText(stickerData.emojis.first(), 1)
                        saveEmojiToHistory(stickerData.emojis.first())
                    } else {
                        ic.commitText("[Sticker]", 1)
                    }
                }
                else -> {
                    // Direct content (emoji) or fallback
                    if (stickerData.emojis.isNotEmpty()) {
                        insertEmojiDirectly(stickerData.emojis.first())
                    } else {
                        ic.commitText(content, 1)
                    }
                }
            }
            
            LogUtil.d(TAG, "Inserted sticker content: $content")
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error inserting sticker content", e)
            // Ultimate fallback
            ic.commitText("🖼️", 1)
        }
    }
    
    /**
     * Insert emoji directly (reuse existing emoji insertion logic)
     */
    private fun insertEmojiDirectly(emoji: String) {
        val ic = inputConnectionProvider()
        ic?.let {
            it.commitText(emoji, 1)
            saveEmojiToHistory(emoji)
            LogUtil.d(TAG, "Inserted emoji: $emoji")
        }
    }
    
    /**
     * Save emoji to history (reuse existing method)
     */
    private fun saveEmojiToHistory(emoji: String) {
        try {
            val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
            val currentHistory = prefs.getString("emoji_history", "[]") ?: "[]"
            
            // Parse and update history (simplified version)
            val history = mutableListOf<String>()
            val cleanJson = currentHistory.trim('[', ']')
            if (cleanJson.isNotEmpty()) {
                cleanJson.split(",").forEach { item ->
                    val cleanItem = item.trim().trim('"')
                    if (cleanItem.isNotEmpty() && cleanItem != emoji) {
                        history.add(cleanItem)
                    }
                }
            }
            
            // Add new emoji to front
            history.add(0, emoji)
            
            // Keep only last 50 emojis
            if (history.size > 50) {
                history.subList(50, history.size).clear()
            }
            
            // Save back to preferences
            val historyJson = history.joinToString(",") { "\"$it\"" }
            prefs.edit().putString("emoji_history", "[$historyJson]").apply()
            
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error saving emoji to history", e)
        }
    }
}

