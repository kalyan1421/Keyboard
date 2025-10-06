package com.example.ai_keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Clipboard strip view that displays recent/pinned clipboard items above the suggestion bar
 * Similar to Gboard's clipboard suggestions
 */
class ClipboardStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {
    
    companion object {
        private const val TAG = "ClipboardStripView"
        private const val MAX_VISIBLE_ITEMS = 5
        private const val ITEM_MAX_CHARS = 20
    }
    
    private val itemsContainer: LinearLayout
    private val items = mutableListOf<ClipboardItem>()
    private var onItemClickListener: ((ClipboardItem) -> Unit)? = null
    private var onItemLongClickListener: ((ClipboardItem) -> Unit)? = null
    private var themeManager: ThemeManager? = null
    
    init {
        // Create horizontal container for clipboard items
        itemsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(8.dp, 4.dp, 8.dp, 4.dp)
        }
        
        addView(itemsContainer)
        
        // Configure scroll view
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        
        // Set default background
        setBackgroundColor(Color.parseColor("#F5F5F5"))
        
        // Hide by default
        visibility = View.GONE
    }
    
    /**
     * Set theme manager for dynamic theming
     */
    fun setThemeManager(themeManager: ThemeManager) {
        this.themeManager = themeManager
        applyTheme()
    }
    
    /**
     * Update clipboard items to display
     */
    fun updateItems(clipboardItems: List<ClipboardItem>) {
        items.clear()
        items.addAll(clipboardItems.take(MAX_VISIBLE_ITEMS))
        
        renderItems()
        
        // Show/hide strip based on items
        visibility = if (items.isNotEmpty()) View.VISIBLE else View.GONE
    }
    
    /**
     * Set click listener for clipboard items
     */
    fun setOnItemClickListener(listener: (ClipboardItem) -> Unit) {
        onItemClickListener = listener
    }
    
    /**
     * Set long click listener for clipboard items (for pin/unpin, delete)
     */
    fun setOnItemLongClickListener(listener: (ClipboardItem) -> Unit) {
        onItemLongClickListener = listener
    }
    
    /**
     * Clear all displayed items
     */
    fun clear() {
        items.clear()
        itemsContainer.removeAllViews()
        visibility = View.GONE
    }
    
    private fun renderItems() {
        itemsContainer.removeAllViews()
        
        items.forEach { item ->
            val itemView = createItemView(item)
            itemsContainer.addView(itemView)
        }
    }
    
    private fun createItemView(item: ClipboardItem): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                marginEnd = 8.dp
            }
            setPadding(12.dp, 6.dp, 12.dp, 6.dp)
            
            // Set background based on item type
            val bgColor = when {
                item.isPinned -> Color.parseColor("#E3F2FD") // Light blue for pinned
                item.isOTP() -> Color.parseColor("#FFF3E0") // Light orange for OTP
                else -> Color.parseColor("#FFFFFF") // White for normal
            }
            setBackgroundColor(bgColor)
            
            // Rounded corners
            background = ContextCompat.getDrawable(context, android.R.drawable.dialog_holo_light_frame)
            background?.alpha = 50
            
            // Click listeners
            setOnClickListener {
                onItemClickListener?.invoke(item)
            }
            
            setOnLongClickListener {
                onItemLongClickListener?.invoke(item)
                true
            }
            
            isClickable = true
            isFocusable = true
        }
        
        // Add pin icon if pinned
        if (item.isPinned) {
            val pinIcon = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(16.dp, 16.dp).apply {
                    marginEnd = 4.dp
                }
                setImageResource(android.R.drawable.star_on)
                setColorFilter(Color.parseColor("#1976D2"))
            }
            container.addView(pinIcon)
        }
        
        // Add text
        val textView = TextView(context).apply {
            val displayText = if (item.text.length > ITEM_MAX_CHARS) {
                "${item.text.take(ITEM_MAX_CHARS)}..."
            } else {
                item.text
            }
            
            text = displayText
            textSize = 14f
            setTextColor(Color.parseColor("#212121"))
            typeface = Typeface.DEFAULT
            setSingleLine(true)
            
            // Special styling for OTP
            if (item.isOTP()) {
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#E65100"))
            }
        }
        container.addView(textView)
        
        return container
    }
    
    private fun applyTheme() {
        themeManager?.let { theme ->
            try {
                // Apply theme colors from current palette
                val palette = theme.getCurrentPalette()
                setBackgroundColor(palette.keyboardBg)
            } catch (e: Exception) {
                // Fallback to default
                setBackgroundColor(Color.parseColor("#F5F5F5"))
            }
        }
    }
    
    // Extension for dp to px conversion
    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).toInt()
}

