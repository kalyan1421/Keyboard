package com.example.ai_keyboard

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.graphics.drawable.Drawable

/**
 * Google Keyboard (Gboard) style emoji panel implementation
 */
class GboardEmojiPanel(context: Context) : LinearLayout(context) {
    
    companion object {
        private const val TAG = "GboardEmojiPanel"
        private const val EMOJI_GRID_COLUMNS = 8
        private const val PANEL_HEIGHT_DP = 280  // Keyboard-appropriate height
        private const val CATEGORY_TAB_WIDTH_DP = 50  // Smaller tab width
        private const val CATEGORY_TAB_HEIGHT_DP = 35  // Smaller tab height
        private const val EMOJI_SIZE_DP = 32  // Smaller emojis
        private const val SEARCH_DEBOUNCE_DELAY = 300L
        private const val ANIMATION_DURATION = 200L
    }
    
    // UI Components
    private lateinit var searchBar: EditText
    private lateinit var categoryTabsContainer: HorizontalScrollView
    private lateinit var categoryTabsLayout: LinearLayout
    private lateinit var emojiGridContainer: ScrollView
    private lateinit var emojiGridLayout: LinearLayout
    private lateinit var bottomToolbar: LinearLayout
    private var skinTonePopup: SkinTonePopup? = null
    
    // Data and State
    private val emojiDatabase = EmojiDatabase(context)
    private var onEmojiSelected: ((String) -> Unit)? = null
    private var onKeyboardSwitchRequested: (() -> Unit)? = null
    private var onBackspacePressed: (() -> Unit)? = null
    private var currentCategory = EmojiCategory.RECENTLY_USED
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val categories = mutableListOf<EmojiCategoryData>()
    private val categoryTabs = mutableListOf<CategoryTab>()
    
    // Skin tone preference - default to medium skin tone or user preference
    private var preferredSkinTone = "🏽"  // Medium skin tone as default (global fallback)
    private val skinToneModifiers = listOf("", "🏻", "🏼", "🏽", "🏾", "🏿") // Default, Light, Medium-Light, Medium, Medium-Dark, Dark
    
    // Per-emoji default skin tone storage (like Gboard/CleverType)
    private val defaultEmojiTones = mutableMapOf<String, String>() // Base emoji -> chosen tone variant
    
    // Emoji history (LRU list) - like Gboard
    private val emojiHistory = mutableListOf<String>() // Recently used emojis
    private var emojiHistoryMaxSize = 90 // Configurable max history size
    
    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setBackgroundColor(Color.parseColor("#f8f9fa"))
        
        // Load user preferences
        loadPreferredSkinTone()
        loadDefaultEmojiTones()
        loadEmojiHistory()
        loadEmojiHistoryMaxSize()
        
        setupCategories()
        setupSearchBar()
        setupCategoryTabs()
        setupEmojiGrid()
        setupBottomToolbar()
        setupSkinToneSelector()
        
        // Load initial category
        loadCategory(EmojiCategory.RECENTLY_USED)
        
        LogUtil.d(TAG, "Gboard-style emoji panel initialized with skin tone preference: $preferredSkinTone")
    }
    
    private fun setupCategories() {
        categories.clear()
        categories.addAll(listOf(
            EmojiCategoryData(EmojiCategory.RECENTLY_USED, "⏰", "Recent"),
            EmojiCategoryData(EmojiCategory.FREQUENTLY_USED, "🔥", "Popular"),
            EmojiCategoryData(EmojiCategory.SMILEYS_EMOTION, "😊", "Smileys"),
            EmojiCategoryData(EmojiCategory.PEOPLE_BODY, "👤", "People"),
            EmojiCategoryData(EmojiCategory.SYMBOLS, "❤️", "Hearts"),
            EmojiCategoryData(EmojiCategory.ANIMALS_NATURE, "🐶", "Animals"),
            EmojiCategoryData(EmojiCategory.FOOD_DRINK, "🍔", "Food"),
            EmojiCategoryData(EmojiCategory.ACTIVITIES, "⚽", "Activities"),
            EmojiCategoryData(EmojiCategory.TRAVEL_PLACES, "🚗", "Travel"),
            EmojiCategoryData(EmojiCategory.OBJECTS, "💡", "Objects"),
            EmojiCategoryData(EmojiCategory.FLAGS, "🏁", "Flags")
        ))
        
        LogUtil.d(TAG, "Categories setup complete with enhanced collection: ${categories.size} categories")
    }
    
    private fun setupSearchBar() {
        val searchContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(40))
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            setBackgroundColor(Color.parseColor("#ffffff"))
            elevation = dpToPx(2).toFloat()
        }
        
        // Search icon
        val searchIcon = TextView(context).apply {
            text = "🔍"
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), LayoutParams.MATCH_PARENT)
        }
        
        // Search input
        searchBar = EditText(context).apply {
            hint = "Search emojis"
            textSize = 14f
            setBackgroundResource(R.drawable.input_field_background)
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(dpToPx(8), 0, dpToPx(8), 0)
            }
            
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val query = s.toString().trim()
                    searchRunnable?.let { searchHandler.removeCallbacks(it) }
                    searchRunnable = Runnable { performSearch(query) }
                    searchHandler.postDelayed(searchRunnable!!, SEARCH_DEBOUNCE_DELAY)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        
        // Clear button
        val clearButton = TextView(context).apply {
            text = "✕"
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), LayoutParams.MATCH_PARENT)
            setOnClickListener {
                searchBar.text.clear()
                loadCategory(currentCategory)
            }
        }
        
        searchContainer.addView(searchIcon)
        searchContainer.addView(searchBar)
        searchContainer.addView(clearButton)
        addView(searchContainer)
    }
    
    private fun setupCategoryTabs() {
        categoryTabsLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dpToPx(CATEGORY_TAB_HEIGHT_DP))
        }
        
        categoryTabsContainer = HorizontalScrollView(context).apply {
            addView(categoryTabsLayout)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(CATEGORY_TAB_HEIGHT_DP))
            setBackgroundColor(Color.parseColor("#ffffff"))
            isHorizontalScrollBarEnabled = false
        }
        
        // Create category tabs
        categories.forEachIndexed { index, categoryData ->
            val tab = CategoryTab(context, categoryData, index == 0).apply {
                setOnClickListener {
                    selectCategory(categoryData.category)
                }
            }
            categoryTabs.add(tab)
            categoryTabsLayout.addView(tab)
        }
        
        addView(categoryTabsContainer)
    }
    
    private fun setupEmojiGrid() {
        emojiGridLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        }
        
        emojiGridContainer = ScrollView(context).apply {
            addView(emojiGridLayout)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(PANEL_HEIGHT_DP - 140))  // Leave room for tabs and toolbar
            setBackgroundColor(Color.parseColor("#f8f9fa"))
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
        }
        
        addView(emojiGridContainer)
    }
    
    private fun setupBottomToolbar() {
        bottomToolbar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(56)) // Taller for better visibility
            setBackgroundColor(Color.parseColor("#f8f9fa")) // Lighter background
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            elevation = dpToPx(2).toFloat() // Add subtle shadow
        }
        
        // Keyboard switch button (ABC) - Enhanced styling
        val keyboardButton = TextView(context).apply {
            text = "ABC"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#1a73e8")) // Google blue
            layoutParams = LinearLayout.LayoutParams(dpToPx(80), LayoutParams.MATCH_PARENT).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            setBackgroundResource(R.drawable.bottom_button_background)
            setOnClickListener {
                // Switch back to keyboard
                onKeyboardSwitchRequested?.invoke()
            }
        }
        
        // Backspace button - Enhanced styling
        val backspaceButton = TextView(context).apply {
            text = "⌫"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#5f6368")) // Gray text
            layoutParams = LinearLayout.LayoutParams(dpToPx(80), LayoutParams.MATCH_PARENT).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            setBackgroundResource(R.drawable.bottom_button_background)
            setOnClickListener {
                // Handle backspace properly - call dedicated backspace handler
                onBackspacePressed?.invoke()
            }
        }
        
        // Space button - Long button like Google Keyboard
        val spaceButton = TextView(context).apply {
            text = "Space"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#5f6368")) // Gray text
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            setBackgroundResource(R.drawable.key_background_default)
            setOnClickListener {
                // Handle space
                onEmojiSelected?.invoke(" ") // Space character
            }
        }
        
        // Enter button - Enhanced styling
        val enterButton = TextView(context).apply {
            text = "⏎"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#5f6368")) // Gray text
            layoutParams = LinearLayout.LayoutParams(dpToPx(80), LayoutParams.MATCH_PARENT).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            setBackgroundResource(R.drawable.bottom_button_background)
            setOnClickListener {
                // Handle enter
                onEmojiSelected?.invoke("\n") // Newline character
            }
        }
        
        bottomToolbar.addView(keyboardButton)
        bottomToolbar.addView(backspaceButton)
        bottomToolbar.addView(spaceButton) // Long space button in the middle
        bottomToolbar.addView(enterButton)
        addView(bottomToolbar)
    }
    
    private fun selectCategory(category: EmojiCategory) {
        if (currentCategory == category) return
        
        currentCategory = category
        
        // Update tab selection
        categoryTabs.forEachIndexed { index, tab ->
            tab.setSelected(categories[index].category == category)
        }
        
        // Load category emojis
        loadCategory(category)
        
        // Scroll to selected tab if needed
        val selectedIndex = categories.indexOfFirst { it.category == category }
        if (selectedIndex >= 0) {
            val selectedTab = categoryTabs[selectedIndex]
            categoryTabsContainer.smoothScrollTo(selectedTab.left - dpToPx(50), 0)
        }
    }
    
    private fun loadCategory(category: EmojiCategory) {
        Thread {
            try {
                val emojis = when (category) {
                    EmojiCategory.RECENTLY_USED -> {
                        // Use our emoji history instead of database
                        emojiHistory.map { emoji ->
                            EmojiData(unicode = emoji, description = "Recent emoji", category = category, keywords = listOf("recent"))
                        }
                    }
                    EmojiCategory.FREQUENTLY_USED -> EmojiCollection.popularEmojis.map { 
                        EmojiData(unicode = applyPreferredSkinTone(it), description = "Popular emoji", category = category, keywords = listOf("popular"))
                    }
                    EmojiCategory.SMILEYS_EMOTION -> EmojiCollection.smileys.map { 
                        EmojiData(unicode = applyPreferredSkinTone(it), description = "Smiley", category = category, keywords = listOf("smiley"))
                    }
                    EmojiCategory.PEOPLE_BODY -> EmojiCollection.people.map { 
                        EmojiData(unicode = applyPreferredSkinTone(it), description = "People", category = category, keywords = listOf("people"))
                    }
                    EmojiCategory.SYMBOLS -> EmojiCollection.hearts.map { 
                        EmojiData(unicode = applyPreferredSkinTone(it), description = "Heart", category = category, keywords = listOf("heart"))
                    }
                    EmojiCategory.ANIMALS_NATURE -> EmojiCollection.animals.map { 
                        EmojiData(unicode = applyPreferredSkinTone(it), description = "Animal", category = category, keywords = listOf("animal"))
                    }
                    EmojiCategory.FOOD_DRINK -> EmojiCollection.food.map { 
                        EmojiData(unicode = applyPreferredSkinTone(it), description = "Food", category = category, keywords = listOf("food"))
                    }
                    EmojiCategory.ACTIVITIES -> EmojiCollection.activities.map { 
                        EmojiData(unicode = applyPreferredSkinTone(it), description = "Activity", category = category, keywords = listOf("activity"))
                    }
                    EmojiCategory.TRAVEL_PLACES -> EmojiCollection.travel.map { 
                        EmojiData(unicode = applyPreferredSkinTone(it), description = "Travel", category = category, keywords = listOf("travel"))
                    }
                    EmojiCategory.OBJECTS -> EmojiCollection.objects.map { 
                        EmojiData(unicode = applyPreferredSkinTone(it), description = "Object", category = category, keywords = listOf("object"))
                    }
                    EmojiCategory.FLAGS -> EmojiCollection.flags.map { 
                        EmojiData(unicode = applyPreferredSkinTone(it), description = "Flag", category = category, keywords = listOf("flag"))
                    }
                    else -> emojiDatabase.getEmojisByCategory(category)
                }
                
                post {
                    displayEmojis(emojis, category == EmojiCategory.RECENTLY_USED)
                }
                
                LogUtil.d(TAG, "Loaded ${emojis.size} emojis from EmojiCollection for category: $category")
            } catch (e: Exception) {
                LogUtil.e(TAG, "Error loading category: $category", e)
            }
        }.start()
    }
    
    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            loadCategory(currentCategory)
            return
        }
        
        Thread {
            try {
                // Use our enhanced emoji search engine
                val searchResults = EmojiSuggestionEngine.searchEmojis(query).map { emoji ->
                    EmojiData(unicode = emoji, description = "Search result", category = EmojiCategory.SMILEYS_EMOTION, keywords = listOf("search"))
                }
                
                post {
                    displayEmojis(searchResults, false)
                }
                
                LogUtil.d(TAG, "Enhanced search found ${searchResults.size} emojis for query: $query")
            } catch (e: Exception) {
                LogUtil.e(TAG, "Error in enhanced search for: $query", e)
                // Fallback to database search
                try {
                    val fallbackResults = emojiDatabase.searchEmojis(query)
                    post {
                        displayEmojis(fallbackResults, false)
                    }
                    LogUtil.d(TAG, "Fallback search found ${fallbackResults.size} emojis for query: $query")
                } catch (fallbackError: Exception) {
                    LogUtil.e(TAG, "Fallback search also failed", fallbackError)
                }
            }
        }.start()
    }
    
    private fun displayEmojis(emojis: List<EmojiData>, showRecentHeader: Boolean) {
        emojiGridLayout.removeAllViews()
        
        // Add recent header if needed
        if (showRecentHeader && emojis.isNotEmpty()) {
            val headerText = TextView(context).apply {
                text = "Recently used"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#5f6368"))
                setPadding(dpToPx(8), dpToPx(16), dpToPx(8), dpToPx(8))
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }
            emojiGridLayout.addView(headerText)
        }
        
        // Create emoji rows (8 columns per row)
        var currentRow: LinearLayout? = null
        emojis.forEachIndexed { index, emoji ->
            if (index % EMOJI_GRID_COLUMNS == 0) {
                currentRow = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(EMOJI_SIZE_DP))
                }
                emojiGridLayout.addView(currentRow)
            }
            
            val emojiView = createEmojiView(emoji)
            currentRow?.addView(emojiView)
        }
        
        // Fill remaining spaces in last row
        val remainingSpaces = EMOJI_GRID_COLUMNS - (emojis.size % EMOJI_GRID_COLUMNS)
        if (remainingSpaces < EMOJI_GRID_COLUMNS && currentRow != null) {
            repeat(remainingSpaces) {
                val spacer = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                }
                currentRow?.addView(spacer)
            }
        }
    }
    
    private fun createEmojiView(emoji: EmojiData): TextView {
        return TextView(context).apply {
            text = emoji.unicode
            textSize = 20f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            setBackgroundResource(R.drawable.emoji_touch_feedback)
            
            setOnClickListener {
                LogUtil.d(TAG, "Emoji clicked: ${emoji.unicode}")
                
                // Apply default skin tone if one exists for this emoji
                val baseEmoji = getBaseEmoji(emoji.unicode)
                val defaultTone = getDefaultEmojiTone(baseEmoji)
                val emojiToInsert = defaultTone ?: emoji.unicode
                
                LogUtil.d(TAG, "Inserting emoji: '$emojiToInsert' (base: '$baseEmoji', default: '$defaultTone')")
                onEmojiSelected?.invoke(emojiToInsert)
                emojiDatabase.recordEmojiUsage(emojiToInsert)
                
                // Add to emoji history (LRU)
                addToEmojiHistory(emojiToInsert)
                
                // Update recent category if currently viewing it
                if (currentCategory == EmojiCategory.RECENTLY_USED) {
                    loadCategory(EmojiCategory.RECENTLY_USED)
                }
            }
            
            setOnLongClickListener { view ->
                LogUtil.d(TAG, "Long press detected on emoji: ${emoji.unicode} with ${emoji.skinToneVariants.size} variants")
                if (emoji.skinToneVariants.isNotEmpty()) {
                    showSkinTonePopup(emoji, view)
                    true
                } else {
                    // Check if this emoji should have skin tone variants and generate them if missing
                    val generatedVariants = generateSkinToneVariants(emoji.unicode)
                    if (generatedVariants.isNotEmpty()) {
                        val emojiWithVariants = emoji.copy(skinToneVariants = generatedVariants)
                        LogUtil.d(TAG, "Generated ${generatedVariants.size} skin tone variants for ${emoji.unicode}")
                        showSkinTonePopup(emojiWithVariants, view)
                        true
                    } else {
                        // Show emoji description for emojis without skin tone variants
                        Toast.makeText(context, emoji.description, Toast.LENGTH_SHORT).show()
                        true
                    }
                }
            }
        }
    }
    
    private fun showSkinTonePopup(emoji: EmojiData, anchorView: View) {
        skinTonePopup?.dismiss()
        
        skinTonePopup = SkinTonePopup(context).apply {
            showForEmoji(emoji, anchorView) { selectedEmoji ->
                onEmojiSelected?.invoke(selectedEmoji)
                emojiDatabase.recordEmojiUsage(selectedEmoji)
                
                // Add to emoji history (LRU)
                addToEmojiHistory(selectedEmoji)
                
                // Update recent category if currently viewing it
                if (currentCategory == EmojiCategory.RECENTLY_USED) {
                    loadCategory(EmojiCategory.RECENTLY_USED)
                }
            }
        }
    }
    
    fun setOnEmojiSelectedListener(listener: (String) -> Unit) {
        onEmojiSelected = listener
    }
    
    fun setOnBackspacePressedListener(listener: () -> Unit) {
        onBackspacePressed = listener
    }
    
    fun setOnKeyboardSwitchRequestedListener(listener: () -> Unit) {
        onKeyboardSwitchRequested = listener
    }
    
    private fun generateSkinToneVariants(baseEmoji: String): List<String> {
        
        val cleanBaseEmoji = getBaseEmoji(baseEmoji)
        
        return if (EmojiCollection.skinToneSupportedEmojis.contains(cleanBaseEmoji)) {
            skinToneModifiers.map { modifier ->
                if (modifier.isEmpty()) cleanBaseEmoji else cleanBaseEmoji + modifier
            }
        } else {
            emptyList()
        } 
    }
    
    /**
     * Apply the user's preferred skin tone to an emoji if it supports skin tones
     */
    private fun applyPreferredSkinTone(baseEmoji: String): String {
        // First check if we have a per-emoji default
        val cleanBaseEmoji = getBaseEmoji(baseEmoji)
        val defaultTone = getDefaultEmojiTone(cleanBaseEmoji)
        if (defaultTone != null) {
            return defaultTone
        }
        
        // Fallback to global preferred skin tone for supported emojis
        return if (EmojiCollection.skinToneSupportedEmojis.contains(cleanBaseEmoji) && preferredSkinTone.isNotEmpty()) {
            cleanBaseEmoji + preferredSkinTone
        } else {
            baseEmoji
        }
    }
    
    /**
     * Set user's preferred skin tone
     */
    fun setPreferredSkinTone(skinTone: String) {
        preferredSkinTone = skinTone
        // Save to preferences
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        prefs.edit().putString("preferred_skin_tone", skinTone).apply()
        
        // Reload current category to apply new skin tone
        loadCategory(currentCategory)
        
        LogUtil.d(TAG, "Preferred skin tone set to: $skinTone")
    }
    
    /**
     * Load user's preferred skin tone from preferences
     */
    private fun loadPreferredSkinTone() {
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        preferredSkinTone = prefs.getString("preferred_skin_tone", "🏽") ?: "🏽" // Default to medium
        LogUtil.d(TAG, "Loaded preferred skin tone: $preferredSkinTone")
    }
    
    /**
     * Load emoji history from preferences (LRU list)
     */
    private fun loadEmojiHistory() {
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val historyJson = prefs.getString("emoji_history", "[]") ?: "[]"
        
        try {
            emojiHistory.clear()
            // Simple JSON array parsing
            val cleanJson = historyJson.trim('[', ']')
            if (cleanJson.isNotEmpty()) {
                cleanJson.split(",").forEach { emoji ->
                    val cleanEmoji = emoji.trim().trim('"')
                    if (cleanEmoji.isNotEmpty()) {
                        emojiHistory.add(cleanEmoji)
                    }
                }
            }
            LogUtil.d(TAG, "Loaded ${emojiHistory.size} emojis from history")
        } catch (e: Exception) {
            LogUtil.w(TAG, "Error loading emoji history: ${e.message}")
            emojiHistory.clear()
        }
    }
    
    /**
     * Save emoji history to preferences
     */
    private fun saveEmojiHistory() {
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val jsonString = emojiHistory.joinToString(",") { "\"$it\"" }
        prefs.edit().putString("emoji_history", "[$jsonString]").apply()
        LogUtil.d(TAG, "Saved ${emojiHistory.size} emojis to history")
    }
    
    /**
     * Load emoji history max size from preferences
     */
    private fun loadEmojiHistoryMaxSize() {
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        emojiHistoryMaxSize = prefs.getInt("emoji_history_max_size", 90)
        LogUtil.d(TAG, "Loaded emoji history max size: $emojiHistoryMaxSize")
    }
    
    /**
     * Set emoji history max size
     */
    fun setEmojiHistoryMaxSize(maxSize: Int) {
        emojiHistoryMaxSize = maxSize
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        prefs.edit().putInt("emoji_history_max_size", maxSize).apply()
        
        // Trim history if needed
        while (emojiHistory.size > emojiHistoryMaxSize) {
            emojiHistory.removeAt(emojiHistory.size - 1)
        }
        saveEmojiHistory()
        
        LogUtil.d(TAG, "Set emoji history max size to: $maxSize")
    }
    
    /**
     * Add emoji to history (LRU - most recent first)
     */
    private fun addToEmojiHistory(emoji: String) {
        // Remove if already exists (to move to front)
        emojiHistory.remove(emoji)
        
        // Add to front
        emojiHistory.add(0, emoji)
        
        // Trim to max size
        while (emojiHistory.size > emojiHistoryMaxSize) {
            emojiHistory.removeAt(emojiHistory.size - 1)
        }
        
        // Save to preferences
        saveEmojiHistory()
        
        LogUtil.d(TAG, "Added '$emoji' to history (total: ${emojiHistory.size})")
    }
    
    /**
     * Get emoji history
     */
    fun getEmojiHistory(): List<String> {
        return emojiHistory.toList()
    }
    
    /**
     * Load per-emoji default skin tone preferences (like Gboard/CleverType)
     */
    private fun loadDefaultEmojiTones() {
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val defaultTonesJson = prefs.getString("default_emoji_tones", "{}")
        
        try {
            // Parse stored emoji defaults
            defaultTonesJson?.let { json ->
                if (json != "{}") {
                    // Simple JSON parsing for key-value pairs
                    json.removeSurrounding("{", "}").split(", ").forEach { pair ->
                        val (baseEmoji, toneVariant) = pair.split("=", limit = 2)
                        if (baseEmoji.isNotEmpty() && toneVariant.isNotEmpty()) {
                            defaultEmojiTones[baseEmoji.trim('"')] = toneVariant.trim('"')
                        }
                    }
                }
            }
            LogUtil.d(TAG, "Loaded ${defaultEmojiTones.size} per-emoji default tones")
        } catch (e: Exception) {
            LogUtil.w(TAG, "Error loading default emoji tones: ${e.message}")
            defaultEmojiTones.clear()
        }
    }
    
    /**
     * Save per-emoji default skin tone preference
     */
    private fun saveDefaultEmojiTone(baseEmoji: String, toneVariant: String) {
        defaultEmojiTones[baseEmoji] = toneVariant
        
        // Save to SharedPreferences
        val prefs = context.getSharedPreferences("emoji_preferences", Context.MODE_PRIVATE)
        val jsonString = defaultEmojiTones.entries.joinToString(", ") { "\"${it.key}\"=\"${it.value}\"" }
        prefs.edit().putString("default_emoji_tones", "{$jsonString}").apply()
        
        LogUtil.d(TAG, "Saved default tone for '$baseEmoji' → '$toneVariant'")
    }
    
    /**
     * Get the default skin tone for a specific emoji
     */
    fun getDefaultEmojiTone(baseEmoji: String): String? {
        return defaultEmojiTones[baseEmoji]
    }
    
    /**
     * Get the base emoji without skin tone modifiers
     */
    fun getBaseEmoji(emojiWithTone: String): String {
        skinToneModifiers.forEach { modifier ->
            if (modifier.isNotEmpty() && emojiWithTone.contains(modifier)) {
                return emojiWithTone.replace(modifier, "")
            }
        }
        return emojiWithTone
    }
    
    /**
     * Save per-emoji default skin tone preference (public method for SkinTonePopup)
     */
    fun saveEmojiDefaultTone(baseEmoji: String, toneVariant: String) {
        saveDefaultEmojiTone(baseEmoji, toneVariant)
    }
    
    /**
     * Reload emoji settings from SharedPreferences (called when settings change)
     */
    fun reloadEmojiSettings() {
        loadPreferredSkinTone()
        loadDefaultEmojiTones()
        loadEmojiHistory()
        loadEmojiHistoryMaxSize()
        
        // Reload current category to apply new settings
        loadCategory(currentCategory)
        updateSkinToneButton()
        
        LogUtil.d(TAG, "Emoji settings reloaded from preferences")
    }
    
    /**
     * Cleanup method to dismiss any open popups when emoji panel is hidden
     */
    fun dismissAllPopups() {
        try {
            skinTonePopup?.dismiss()
            skinTonePopup = null
            LogUtil.d(TAG, "All popups dismissed")
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error dismissing popups", e)
        }
    }
    
    private fun setupSkinToneSelector() {
        // Add enhanced skin tone selector to bottom toolbar
        bottomToolbar?.let { toolbar ->
            // Skin tone display and selector button
            val skinToneButton = TextView(context).apply {
                text = "👋$preferredSkinTone" // Show current skin tone with hand emoji
                textSize = 18f
                setTextColor(Color.parseColor("#1a73e8"))
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                setBackgroundResource(R.drawable.key_background_default)
                setOnClickListener { showSkinToneSelector() }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
                }
            }
            
            // Quick tone cycle button (for easy switching)
            val toneCycleButton = TextView(context).apply {
                text = "🎨" // Palette emoji for tone cycling
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#5f6368"))
                setBackgroundResource(R.drawable.key_background_default)
                setOnClickListener { cycleSkinTone() }
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(40), 
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
                }
            }
            
            toolbar.addView(skinToneButton, 0) // Add at the beginning
            toolbar.addView(toneCycleButton, 1) // Add tone cycle button next
        }
    }
    
    /**
     * Cycle through skin tones quickly
     */
    private fun cycleSkinTone() {
        val currentIndex = skinToneModifiers.indexOf(preferredSkinTone)
        val nextIndex = (currentIndex + 1) % skinToneModifiers.size
        val nextSkinTone = skinToneModifiers[nextIndex]
        
        setPreferredSkinTone(nextSkinTone)
        updateSkinToneButton()
        
        // Show visual feedback
        val toneName = when(nextSkinTone) {
            "" -> "Default"
            "🏻" -> "Light"
            "🏼" -> "Medium-Light"
            "🏽" -> "Medium"
            "🏾" -> "Medium-Dark"
            "🏿" -> "Dark"
            else -> "Unknown"
        }
        
        Toast.makeText(context, "🎨 Skin tone: $toneName", Toast.LENGTH_SHORT).show()
        LogUtil.d(TAG, "Cycled to skin tone: $toneName ($nextSkinTone)")
    }
    
    private fun showSkinToneSelector() {
        val popup = PopupWindow(context)
        val popupLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#ffffff"))
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }
        
        // Create skin tone options
        val skinToneOptions = listOf(
            Pair("", "Default"),
            Pair("🏻", "Light"),
            Pair("🏼", "Medium-Light"), 
            Pair("🏽", "Medium"),
            Pair("🏾", "Medium-Dark"),
            Pair("🏿", "Dark")
        )
        
        skinToneOptions.forEach { (modifier, description) ->
            val button = TextView(context).apply {
                text = "👋$modifier"
                textSize = 20f
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                setBackgroundColor(if (modifier == preferredSkinTone) Color.parseColor("#e8f0fe") else Color.TRANSPARENT)
                setOnClickListener {
                    setPreferredSkinTone(modifier)
                    updateSkinToneButton()
                    popup.dismiss()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(4)
                }
            }
            popupLayout.addView(button)
        }
        
        popup.contentView = popupLayout
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.showAsDropDown(bottomToolbar, 0, -dpToPx(60))
    }
    
    private fun updateSkinToneButton() {
        // Update the skin tone button display
        bottomToolbar?.let { toolbar ->
            if (toolbar.childCount > 0) {
                val skinToneButton = toolbar.getChildAt(0) as? TextView
                skinToneButton?.text = "👋$preferredSkinTone"
                
                // Update visual state to show current selection
                val toneName = when(preferredSkinTone) {
                    "" -> "Default tone"
                    "🏻" -> "Light tone"
                    "🏼" -> "Medium-light tone"
                    "🏽" -> "Medium tone"
                    "🏾" -> "Medium-dark tone"
                    "🏿" -> "Dark tone"
                    else -> "Custom tone"
                }
                skinToneButton?.contentDescription = toneName
            }
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    // Data classes
    data class EmojiCategoryData(
        val category: EmojiCategory,
        val icon: String,
        val displayName: String
    )
    
    // Category Tab View
    private class CategoryTab(
        context: Context,
        private val categoryData: EmojiCategoryData,
        isSelected: Boolean = false
    ) : LinearLayout(context) {
        
        private val iconView: TextView
        private val indicatorView: View
        
        init {
            orientation = VERTICAL
            layoutParams = LinearLayout.LayoutParams(dpToPx(CATEGORY_TAB_WIDTH_DP), dpToPx(CATEGORY_TAB_HEIGHT_DP))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            gravity = Gravity.CENTER
            
            iconView = TextView(context).apply {
                text = categoryData.icon
                textSize = 20f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            }
            
            indicatorView = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(3)).apply {
                    topMargin = dpToPx(4)
                }
                setBackgroundColor(Color.parseColor("#1a73e8"))
                visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            }
            
            addView(iconView)
            addView(indicatorView)
            
            setSelected(isSelected)
        }
        
        override fun setSelected(selected: Boolean) {
            indicatorView.visibility = if (selected) View.VISIBLE else View.INVISIBLE
            alpha = if (selected) 1.0f else 0.6f
            setBackgroundResource(if (selected) R.drawable.category_tab_selected else R.drawable.category_tab_unselected)
        }
        
        private fun dpToPx(dp: Int): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }
    }
}

/**
 * Skin tone variant popup
 */
class SkinTonePopup(private val context: Context) {
    
    companion object {
        private const val TAG = "SkinTonePopup"
        private val SKIN_TONE_MODIFIERS = listOf("🏻", "🏼", "🏽", "🏾", "🏿")
    }
    
    private var popupWindow: PopupWindow? = null
    
    fun showForEmoji(emoji: EmojiData, anchorView: View, onEmojiSelected: (String) -> Unit) {
        try {
            LogUtil.d(TAG, "Showing skin tone popup for emoji: ${emoji.unicode} with ${emoji.skinToneVariants.size} variants")
            
            val popupLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.popup_background)
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                elevation = dpToPx(8).toFloat()
            }
            
            // Get the emoji panel instance to access default tone methods
            val emojiPanel = (anchorView.parent as? ViewGroup)?.let { findEmojiPanel(it) }
            val baseEmoji = emojiPanel?.getBaseEmoji(emoji.unicode) ?: emoji.unicode
            val currentDefault = emojiPanel?.getDefaultEmojiTone(baseEmoji)
            
            // Add base emoji (no skin tone) - highlight if it's the current default
            addEmojiVariant(popupLayout, emoji.unicode, currentDefault == null, onEmojiSelected) { selectedEmoji ->
                emojiPanel?.saveEmojiDefaultTone(baseEmoji, selectedEmoji)
            }
            
            // Add skin tone variants - highlight the current default
            emoji.skinToneVariants.forEach { variant ->
                LogUtil.d(TAG, "Adding skin tone variant: $variant")
                val isDefault = (currentDefault == variant)
                addEmojiVariant(popupLayout, variant, isDefault, onEmojiSelected) { selectedEmoji ->
                    emojiPanel?.saveEmojiDefaultTone(baseEmoji, selectedEmoji)
                }
            }
            
            popupWindow = PopupWindow(
                popupLayout,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                false // Prevent keyboard from losing focus
            ).apply {
                elevation = dpToPx(8).toFloat()
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                isOutsideTouchable = true
                isFocusable = false // Keep keyboard active
                isTouchable = true
                
                // Prevent keyboard dismissal
                inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
                
                // Calculate position to show above the anchor view
                val location = IntArray(2)
                anchorView.getLocationOnScreen(location)
                val popupHeight = dpToPx(60)
                val yOffset = -anchorView.height - popupHeight - dpToPx(4) // Extra margin
                
                LogUtil.d(TAG, "Showing popup at offset: $yOffset")
                try {
                    showAsDropDown(anchorView, 0, yOffset)
                } catch (e: Exception) {
                    LogUtil.e(TAG, "Error showing popup", e)
                    // Fallback: show as a toast instead
                    android.widget.Toast.makeText(context, "Skin tone variants: ${emoji.skinToneVariants.size + 1}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error showing skin tone popup", e)
            // Show a toast as fallback to indicate the issue
            android.widget.Toast.makeText(context, "Skin tone variants: ${emoji.skinToneVariants.size}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun addEmojiVariant(container: LinearLayout, emoji: String, isDefault: Boolean, onSelected: (String) -> Unit, onDefaultSet: (String) -> Unit) {
        val emojiView = TextView(context).apply {
            text = emoji
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            
            // Highlight the current default with blue background (like Gboard)
            if (isDefault) {
                setBackgroundColor(Color.parseColor("#1976D2")) // Blue highlight
                setTextColor(Color.WHITE)
            } else {
                setBackgroundResource(R.drawable.emoji_touch_feedback)
                setTextColor(Color.BLACK)
            }
            
            layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48))
            
            setOnClickListener {
                LogUtil.d(TAG, "Skin tone variant selected: $emoji (setting as default)")
                
                // Set as new default for this emoji
                onDefaultSet(emoji)
                
                // Show feedback like Gboard
                android.widget.Toast.makeText(context, "✅ Default skin tone set", android.widget.Toast.LENGTH_SHORT).show()
                
                // Insert the selected emoji
                onSelected(emoji)
                dismiss()
            }
        }
        container.addView(emojiView)
    }
    
    /**
     * Fallback method for backward compatibility
     */
    private fun addEmojiVariant(container: LinearLayout, emoji: String, onSelected: (String) -> Unit) {
        addEmojiVariant(container, emoji, false, onSelected) { /* no-op for backward compatibility */ }
    }
    
    /**
     * Helper method to find the parent GboardEmojiPanel instance
     */
    private fun findEmojiPanel(viewGroup: ViewGroup): GboardEmojiPanel? {
        var parent = viewGroup.parent
        while (parent != null) {
            if (parent is GboardEmojiPanel) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }
    
    fun dismiss() {
        try {
            popupWindow?.dismiss()
            popupWindow = null
            LogUtil.d(TAG, "Skin tone popup dismissed")
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error dismissing popup", e)
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
