package com.example.ai_keyboard

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
        private const val PANEL_HEIGHT_DP = 280
        private const val CATEGORY_TAB_WIDTH_DP = 72
        private const val CATEGORY_TAB_HEIGHT_DP = 48
        private const val EMOJI_SIZE_DP = 40
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
    private var currentCategory = EmojiCategory.RECENTLY_USED
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val categories = mutableListOf<EmojiCategoryData>()
    private val categoryTabs = mutableListOf<CategoryTab>()
    
    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(PANEL_HEIGHT_DP))
        setBackgroundColor(Color.parseColor("#f8f9fa"))
        
        setupCategories()
        setupSearchBar()
        setupCategoryTabs()
        setupEmojiGrid()
        setupBottomToolbar()
        
        // Load initial category
        loadCategory(EmojiCategory.RECENTLY_USED)
        
        Log.d(TAG, "Gboard-style emoji panel initialized")
    }
    
    private fun setupCategories() {
        val categoryInfo = emojiDatabase.getCategoryInfo()
        
        categories.clear()
        categories.addAll(listOf(
            EmojiCategoryData(EmojiCategory.RECENTLY_USED, "⏰", "Recent"),
            EmojiCategoryData(EmojiCategory.SMILEYS_EMOTION, "😊", "Smileys"),
            EmojiCategoryData(EmojiCategory.PEOPLE_BODY, "👤", "People"),
            EmojiCategoryData(EmojiCategory.ANIMALS_NATURE, "🐶", "Animals"),
            EmojiCategoryData(EmojiCategory.FOOD_DRINK, "🍔", "Food"),
            EmojiCategoryData(EmojiCategory.TRAVEL_PLACES, "🚗", "Travel"),
            EmojiCategoryData(EmojiCategory.ACTIVITIES, "⚽", "Activities"),
            EmojiCategoryData(EmojiCategory.OBJECTS, "💡", "Objects"),
            EmojiCategoryData(EmojiCategory.SYMBOLS, "❤️", "Symbols"),
            EmojiCategoryData(EmojiCategory.FLAGS, "🏁", "Flags")
        ))
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
            setBackgroundResource(R.drawable.search_bar_background)
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
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            setBackgroundColor(Color.parseColor("#f8f9fa"))
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
                // Handle backspace
                onEmojiSelected?.invoke("\b") // Backspace character
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
            setBackgroundResource(R.drawable.space_button_background)
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
                EmojiCategory.RECENTLY_USED -> emojiDatabase.getRecentlyUsedEmojis()
                    EmojiCategory.FREQUENTLY_USED -> emojiDatabase.getFrequentlyUsedEmojis()
                    else -> emojiDatabase.getEmojisByCategory(category)
                }
                
                post {
                    displayEmojis(emojis, category == EmojiCategory.RECENTLY_USED)
                }
                
                Log.d(TAG, "Loaded ${emojis.size} emojis for category: $category")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading category: $category", e)
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
                val results = emojiDatabase.searchEmojis(query)
                post {
                    displayEmojis(results, false)
                }
                
                Log.d(TAG, "Found ${results.size} emojis for query: $query")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching emojis: $query", e)
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
                onEmojiSelected?.invoke(emoji.unicode)
                emojiDatabase.recordEmojiUsage(emoji.unicode)
                
                // Update recent category if currently viewing it
                if (currentCategory == EmojiCategory.RECENTLY_USED) {
                    loadCategory(EmojiCategory.RECENTLY_USED)
                }
            }
            
            setOnLongClickListener { view ->
                if (emoji.skinToneVariants.isNotEmpty()) {
                    showSkinTonePopup(emoji, view)
                    true
                } else {
                    // Show emoji description
                    Toast.makeText(context, emoji.description, Toast.LENGTH_SHORT).show()
                    true
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
            }
        }
    }
    
    fun setOnEmojiSelectedListener(listener: (String) -> Unit) {
        onEmojiSelected = listener
    }
    
    fun setOnKeyboardSwitchRequestedListener(listener: () -> Unit) {
        onKeyboardSwitchRequested = listener
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
        val popupLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.skin_tone_popup_bg)
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                elevation = dpToPx(8).toFloat()
            }
            
            // Add base emoji (no skin tone)
            addEmojiVariant(popupLayout, emoji.unicode, onEmojiSelected)
            
            // Add skin tone variants
            emoji.skinToneVariants.forEach { variant ->
                addEmojiVariant(popupLayout, variant, onEmojiSelected)
            }
            
            popupWindow = PopupWindow(
                popupLayout,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                elevation = dpToPx(8).toFloat()
                setBackgroundDrawable(null)
                isOutsideTouchable = true
                isFocusable = true
                
                // Show above the anchor view
                val location = IntArray(2)
                anchorView.getLocationInWindow(location)
                showAsDropDown(anchorView, 0, -anchorView.height - dpToPx(60))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing skin tone popup", e)
        }
    }
    
    private fun addEmojiVariant(container: LinearLayout, emoji: String, onSelected: (String) -> Unit) {
        val emojiView = TextView(context).apply {
            text = emoji
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            setBackgroundResource(R.drawable.emoji_touch_feedback)
            layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48))
            
            setOnClickListener {
                onSelected(emoji)
                dismiss()
            }
        }
        container.addView(emojiView)
    }
    
    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
