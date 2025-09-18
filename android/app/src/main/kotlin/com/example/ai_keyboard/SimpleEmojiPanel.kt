package com.example.ai_keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*

/**
 * Simple emoji panel using basic Android views (no RecyclerView dependency)
 */
class SimpleEmojiPanel(context: Context) : LinearLayout(context) {
    
    companion object {
        private const val TAG = "SimpleEmojiPanel"
        private const val EMOJI_GRID_COLUMNS = 8
        private const val PANEL_HEIGHT_DP = 250
    }
    
    private val emojiDatabase = EmojiDatabase(context)
    private lateinit var emojiGridView: LinearLayout
    private lateinit var categoryTabs: HorizontalScrollView
    private lateinit var searchEditText: EditText
    private var onEmojiSelected: ((String) -> Unit)? = null
    private var currentCategory = EmojiCategory.RECENTLY_USED
    
    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(PANEL_HEIGHT_DP))
        setBackgroundColor(Color.parseColor("#F5F5F5"))
        
        setupSearchBar()
        setupCategoryTabs()
        setupEmojiGrid()
        
        // Load initial category
        loadEmojiCategory(EmojiCategory.RECENTLY_USED)
    }
    
    private fun setupSearchBar() {
        val searchContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(40))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setBackgroundColor(Color.WHITE)
        }
        
        searchEditText = EditText(context).apply {
            hint = "Search emojis..."
            textSize = 14f
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            setPadding(dpToPx(8), 0, dpToPx(8), 0)
            
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val query = s.toString().trim()
                    if (query.isNotEmpty()) {
                        searchEmojis(query)
                    } else {
                        loadEmojiCategory(currentCategory)
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        
        val clearButton = Button(context).apply {
            text = "✕"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), LayoutParams.MATCH_PARENT)
            setOnClickListener {
                searchEditText.text.clear()
                loadEmojiCategory(currentCategory)
            }
        }
        
        searchContainer.addView(searchEditText)
        searchContainer.addView(clearButton)
        addView(searchContainer)
    }
    
    private fun setupCategoryTabs() {
        val tabLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(50))
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
        }
        
        val categoryInfo = emojiDatabase.getCategoryInfo()
        val categories = listOf(
            EmojiCategory.RECENTLY_USED,
            EmojiCategory.FREQUENTLY_USED,
            EmojiCategory.SMILEYS_EMOTION,
            EmojiCategory.PEOPLE_BODY,
            EmojiCategory.ANIMALS_NATURE,
            EmojiCategory.FOOD_DRINK,
            EmojiCategory.ACTIVITIES,
            EmojiCategory.TRAVEL_PLACES,
            EmojiCategory.OBJECTS,
            EmojiCategory.SYMBOLS,
            EmojiCategory.FLAGS
        )
        
        categories.forEach { category ->
            val (icon, _) = categoryInfo[category] ?: Pair("❓", "Unknown")
            val tabButton = createCategoryTab(category, icon)
            tabLayout.addView(tabButton)
        }
        
        categoryTabs = HorizontalScrollView(context).apply {
            addView(tabLayout)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(50))
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        addView(categoryTabs)
    }
    
    private fun createCategoryTab(category: EmojiCategory, icon: String): Button {
        return Button(context).apply {
            text = icon
            textSize = 20f
            typeface = Typeface.DEFAULT
            setOnClickListener { 
                currentCategory = category
                loadEmojiCategory(category)
                updateTabSelection(category)
            }
            layoutParams = LinearLayout.LayoutParams(dpToPx(50), dpToPx(45)).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            tag = category
        }
    }
    
    private fun updateTabSelection(selectedCategory: EmojiCategory) {
        val tabLayout = (categoryTabs.getChildAt(0) as LinearLayout)
        for (i in 0 until tabLayout.childCount) {
            val tab = tabLayout.getChildAt(i) as Button
            val isSelected = tab.tag == selectedCategory
            tab.setBackgroundColor(if (isSelected) Color.parseColor("#2196F3") else Color.parseColor("#FFFFFF"))
            tab.setTextColor(if (isSelected) Color.WHITE else Color.BLACK)
        }
    }
    
    private fun setupEmojiGrid() {
        val scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        
        emojiGridView = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        }
        
        scrollView.addView(emojiGridView)
        addView(scrollView)
    }
    
    private fun loadEmojiCategory(category: EmojiCategory) {
        Thread {
            try {
                val emojis = when (category) {
                    EmojiCategory.RECENTLY_USED -> emojiDatabase.getRecentlyUsedEmojis()
                    EmojiCategory.FREQUENTLY_USED -> emojiDatabase.getFrequentlyUsedEmojis()
                    else -> emojiDatabase.getEmojisByCategory(category)
                }
                
                post {
                    updateEmojiGrid(emojis)
                    updateTabSelection(category)
                }
                
                Log.d(TAG, "Loaded ${emojis.size} emojis for category: $category")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading emoji category: $category", e)
            }
        }.start()
    }
    
    private fun searchEmojis(query: String) {
        Thread {
            try {
                val results = emojiDatabase.searchEmojis(query)
                post {
                    updateEmojiGrid(results)
                }
                
                Log.d(TAG, "Found ${results.size} emojis for query: $query")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching emojis: $query", e)
            }
        }.start()
    }
    
    private fun updateEmojiGrid(emojis: List<EmojiData>) {
        emojiGridView.removeAllViews()
        
        // Create rows of 8 emojis each
        var currentRow: LinearLayout? = null
        emojis.take(40).forEachIndexed { index, emoji -> // Limit to 40 emojis for performance
            if (index % EMOJI_GRID_COLUMNS == 0) {
                currentRow = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                }
                emojiGridView.addView(currentRow)
            }
            
            val emojiButton = TextView(context).apply {
                text = emoji.unicode
                textSize = 24f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)).apply {
                    setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
                }
                setBackgroundResource(android.R.drawable.btn_default)
                isClickable = true
                
                setOnClickListener {
                    onEmojiSelected?.invoke(emoji.unicode)
                    emojiDatabase.recordEmojiUsage(emoji.unicode)
                    
                    // Refresh recent/frequent categories if needed
                    if (currentCategory == EmojiCategory.RECENTLY_USED || currentCategory == EmojiCategory.FREQUENTLY_USED) {
                        loadEmojiCategory(currentCategory)
                    }
                }
                
                setOnLongClickListener {
                    if (emoji.skinToneVariants.isNotEmpty()) {
                        showSkinToneVariants(this, emoji)
                    } else {
                        Toast.makeText(context, emoji.description, Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            }
            
            currentRow?.addView(emojiButton)
        }
    }
    
    private fun showSkinToneVariants(anchor: View, emoji: EmojiData) {
        try {
            val popup = PopupWindow(anchor.context)
            val layout = LinearLayout(anchor.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                setBackgroundColor(Color.WHITE)
            }
            
            // Add original emoji first
            val originalView = TextView(anchor.context).apply {
                text = emoji.unicode
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                setBackgroundResource(android.R.drawable.btn_default)
                setOnClickListener {
                    onEmojiSelected?.invoke(emoji.unicode)
                    emojiDatabase.recordEmojiUsage(emoji.unicode)
                    popup.dismiss()
                }
            }
            layout.addView(originalView)
            
            // Add skin tone variants
            emoji.skinToneVariants.forEach { variant ->
                val variantView = TextView(anchor.context).apply {
                    text = variant
                    textSize = 24f
                    gravity = Gravity.CENTER
                    setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                    setBackgroundResource(android.R.drawable.btn_default)
                    setOnClickListener {
                        onEmojiSelected?.invoke(variant)
                        emojiDatabase.recordEmojiUsage(variant)
                        popup.dismiss()
                    }
                }
                layout.addView(variantView)
            }
            
            popup.contentView = layout
            popup.width = LinearLayout.LayoutParams.WRAP_CONTENT
            popup.height = LinearLayout.LayoutParams.WRAP_CONTENT
            popup.isOutsideTouchable = true
            popup.isFocusable = true
            popup.elevation = 10f
            
            // Show the popup above the anchor
            popup.showAsDropDown(anchor, 0, -anchor.height - popup.height, Gravity.CENTER_HORIZONTAL)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing skin tone variants", e)
            Toast.makeText(context, "Skin tone variants: ${emoji.skinToneVariants.joinToString(" ")}", Toast.LENGTH_LONG).show()
        }
    }
    
    fun setOnEmojiSelectedListener(listener: (String) -> Unit) {
        onEmojiSelected = listener
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
