package com.example.ai_keyboard

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.example.ai_keyboard.utils.LogUtil
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

/**
 * Clipboard panel UI that shows clipboard history in a popup overlay
 */
class ClipboardPanel(
    private val context: Context,
    private val themeManager: ThemeManager
) {
    
    companion object {
        private const val TAG = "ClipboardPanel"
    }
    
    private var popupWindow: PopupWindow? = null
    private var onItemSelected: ((ClipboardItem) -> Unit)? = null
    private var onItemPinToggled: ((ClipboardItem) -> Unit)? = null
    private var onItemDeleted: ((ClipboardItem) -> Unit)? = null
    
    /**
     * Show the clipboard panel as a popup
     */
    fun show(anchorView: View, items: List<ClipboardItem>) {
        dismiss() // Dismiss any existing popup
        
        try {
            val contentView = createContentView(items)
            
            // Use unified theme color from ThemeManager
            val bgColor = themeManager.getKeyboardBackgroundColor()
            
            popupWindow = PopupWindow(
                contentView,
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                (context.resources.displayMetrics.heightPixels * 0.4).toInt(),
                true
            ).apply {
                // CRITICAL: Prevent keyboard from closing
                inputMethodMode = PopupWindow.INPUT_METHOD_FROM_FOCUSABLE
                
                // Set other properties  
                isOutsideTouchable = true
                isFocusable = false  // Don't steal focus from keyboard
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgColor))
                elevation = 8f
                
                // Show above the anchor view
                showAsDropDown(anchorView, 0, -anchorView.height - height)
            }
            
            Log.d(TAG, "Clipboard panel shown with ${items.size} items")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing clipboard panel", e)
        }
    }
    
    /**
     * Dismiss the popup
     */
    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }
    
    /**
     * Update items in the panel
     */
    fun updateItems(items: List<ClipboardItem>) {
        // Dismiss and re-show if panel is currently visible
        if (popupWindow?.isShowing == true) {
            dismiss()
            // Could re-show with new items if needed
        }
    }
    
    /**
     * Set callbacks for item interactions
     */
    fun setCallbacks(
        onItemSelected: (ClipboardItem) -> Unit,
        onItemPinToggled: (ClipboardItem) -> Unit,
        onItemDeleted: (ClipboardItem) -> Unit
    ) {
        this.onItemSelected = onItemSelected
        this.onItemPinToggled = onItemPinToggled
        this.onItemDeleted = onItemDeleted
    }
    
    private fun createContentView(items: List<ClipboardItem>): View {
        // Use unified theme colors from ThemeManager
        val bgColor = themeManager.getKeyboardBackgroundColor()
        val textColor = themeManager.getTextColor()
        
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(16, 16, 16, 16)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Header
        val header = TextView(context).apply {
            text = "Clipboard History"
            textSize = 18f
            setTextColor(textColor)
            setPadding(0, 0, 0, 16)
            gravity = android.view.Gravity.CENTER
        }
        container.addView(header)
        
        if (items.isEmpty()) {
            // Empty state
            val emptyText = TextView(context).apply {
                text = "No clipboard history yet.\nCopy some text to get started!"
                textSize = 14f
                setTextColor(textColor)
                gravity = android.view.Gravity.CENTER
                setPadding(32, 32, 32, 32)
            }
            container.addView(emptyText)
        } else {
            // Create simple list view instead of RecyclerView for better compatibility
            val scrollView = android.widget.ScrollView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            
            val itemsContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            // Add items (limit to first 8 for performance)
            items.take(8).forEachIndexed { index, item ->
                val itemView = createItemView(item, textColor)
                itemsContainer.addView(itemView)
                
                // Add divider between items
                if (index < items.size - 1) {
                    val divider = View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1
                        )
                        // Use semi-transparent text color for subtle divider
                        val dividerColor = android.graphics.Color.argb(
                            32, 
                            android.graphics.Color.red(textColor), 
                            android.graphics.Color.green(textColor), 
                            android.graphics.Color.blue(textColor)
                        )
                        setBackgroundColor(dividerColor)
                    }
                    itemsContainer.addView(divider)
                }
            }
            
            scrollView.addView(itemsContainer)
            container.addView(scrollView)
        }
        
        return container
    }
    
    private fun createItemView(item: ClipboardItem, textColor: Int): View {
        val itemLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)
            setBackgroundResource(android.R.drawable.list_selector_background)
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            setOnClickListener {
                onItemSelected?.invoke(item)
                dismiss()
            }
        }
        
        // Content text
        val contentText = TextView(context).apply {
            val prefix = if (item.isOTP()) "🔢 OTP: " else "📋 "
            text = "$prefix${item.getPreview(40)}"
            textSize = 14f
            setTextColor(textColor)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        itemLayout.addView(contentText)
        
        // Pin/delete buttons
        if (!item.isTemplate) {
            val pinButton = TextView(context).apply {
                text = if (item.isPinned) "📌" else "📍"
                textSize = 16f
                setPadding(8, 0, 8, 0)
                isClickable = true
                setOnClickListener {
                    onItemPinToggled?.invoke(item)
                }
            }
            itemLayout.addView(pinButton)
            
            val deleteButton = TextView(context).apply {
                text = "🗑️"
                textSize = 16f
                setPadding(8, 0, 0, 0)
                isClickable = true
                setOnClickListener {
                    onItemDeleted?.invoke(item)
                }
            }
            itemLayout.addView(deleteButton)
        }
        
        return itemLayout
    }
}


