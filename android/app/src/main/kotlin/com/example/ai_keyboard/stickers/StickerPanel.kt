package com.example.ai_keyboard.stickers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

/**
 * Modern Firebase-powered sticker panel for AIKeyboard
 * Replaces SimpleStickerPanel with full Firebase + JSON cache support
 */
class StickerPanel(context: Context) : LinearLayout(context) {
    
    companion object {
        private const val TAG = "StickerPanel"
        private const val PANEL_HEIGHT_DP = 280
        private const val STICKERS_PER_ROW = 6
        private const val PACK_ICON_SIZE = 48
        private const val STICKER_SIZE = 54
    }
    
    private val stickerService = StickerServiceAdapter(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var onStickerSelected: ((String, StickerData?) -> Unit)? = null
    private var currentPackId: String? = null
    
    // UI Components
    private lateinit var packRecyclerView: RecyclerView
    private lateinit var stickerRecyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var errorMessage: TextView
    
    // Adapters
    private val packAdapter = StickerPackAdapter { pack ->
        loadStickersForPack(pack)
    }
    private val stickerAdapter = StickerAdapter { sticker ->
        handleStickerClick(sticker)
    }
    
    init {
        setupLayout()
        loadStickerPacks()
    }
    
    private fun setupLayout() {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(PANEL_HEIGHT_DP))
        setBackgroundColor(Color.parseColor("#FAFAFA"))
        
        // Header with title
        val headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(48))
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }
        
        val titleText = TextView(context).apply {
            text = "Stickers"
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        }
        headerLayout.addView(titleText)
        
        // Refresh button
        val refreshButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_rotate)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setBackgroundResource(android.R.drawable.btn_default_small)
            setOnClickListener { refreshStickers() }
        }
        headerLayout.addView(refreshButton)
        addView(headerLayout)
        
        // Sticker pack horizontal list
        packRecyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(70))
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = packAdapter
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            setBackgroundColor(Color.parseColor("#F0F0F0"))
        }
        addView(packRecyclerView)
        
        // Content container
        val contentContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        
        // Loading indicator
        loadingIndicator = ProgressBar(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            visibility = GONE
        }
        contentContainer.addView(loadingIndicator)
        
        // Error message
        errorMessage = TextView(context).apply {
            text = "Failed to load stickers"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            visibility = GONE
        }
        contentContainer.addView(errorMessage)
        
        // Sticker grid
        stickerRecyclerView = RecyclerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            layoutManager = GridLayoutManager(context, STICKERS_PER_ROW)
            adapter = stickerAdapter
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }
        contentContainer.addView(stickerRecyclerView)
        
        addView(contentContainer)
    }
    
    private fun loadStickerPacks() {
        showLoading(true)
        
        coroutineScope.launch {
            try {
                val packs = withContext(Dispatchers.IO) {
                    stickerService.getAvailablePacks()
                }
                
                packAdapter.updatePacks(packs)
                
                // Load first pack automatically
                if (packs.isNotEmpty()) {
                    loadStickersForPack(packs.first())
                } else {
                    showError("No sticker packs available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sticker packs", e)
                showError("Failed to load sticker packs")
            }
        }
    }
    
    private fun loadStickersForPack(pack: StickerPack) {
        currentPackId = pack.id
        packAdapter.setSelectedPack(pack.id)
        showLoading(true)
        
        coroutineScope.launch {
            try {
                val stickers = withContext(Dispatchers.IO) {
                    stickerService.getStickersFromPack(pack.id)
                }
                
                stickerAdapter.updateStickers(stickers)
                showLoading(false)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading stickers for pack ${pack.id}", e)
                showError("Failed to load stickers")
            }
        }
    }
    
    private fun handleStickerClick(sticker: StickerData) {
        coroutineScope.launch {
            try {
                // Record usage
                withContext(Dispatchers.IO) {
                    stickerService.recordStickerUsage(sticker.id)
                }
                
                // Try to get local path first
                val localPath = withContext(Dispatchers.IO) {
                    stickerService.downloadStickerIfNeeded(sticker)
                }
                
                val content = localPath ?: sticker.imageUrl
                onStickerSelected?.invoke(content, sticker)
                
                Log.d(TAG, "Sticker selected: ${sticker.id}, content: $content")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling sticker click", e)
                // Fallback to URL
                onStickerSelected?.invoke(sticker.imageUrl, sticker)
            }
        }
    }
    
    private fun refreshStickers() {
        coroutineScope.launch {
            try {
                showLoading(true)
                
                // Force refresh from Firebase
                val packs = withContext(Dispatchers.IO) {
                    stickerService.getAvailablePacks()
                }
                
                packAdapter.updatePacks(packs)
                
                // Reload current pack
                currentPackId?.let { packId ->
                    val currentPack = packs.find { it.id == packId }
                    currentPack?.let { loadStickersForPack(it) }
                } ?: run {
                    if (packs.isNotEmpty()) {
                        loadStickersForPack(packs.first())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing stickers", e)
                showError("Failed to refresh stickers")
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        loadingIndicator.visibility = if (show) VISIBLE else GONE
        stickerRecyclerView.visibility = if (show) GONE else VISIBLE
        errorMessage.visibility = GONE
    }
    
    private fun showError(message: String) {
        loadingIndicator.visibility = GONE
        stickerRecyclerView.visibility = GONE
        errorMessage.visibility = VISIBLE
        errorMessage.text = message
    }
    
    fun setOnStickerSelectedListener(listener: (String, StickerData?) -> Unit) {
        onStickerSelected = listener
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    fun onDestroy() {
        coroutineScope.cancel()
    }
}

/**
 * Adapter for sticker packs horizontal list
 */
class StickerPackAdapter(
    private val onPackSelected: (StickerPack) -> Unit
) : RecyclerView.Adapter<StickerPackAdapter.PackViewHolder>() {
    
    private var packs = listOf<StickerPack>()
    private var selectedPackId: String? = null
    
    fun updatePacks(newPacks: List<StickerPack>) {
        packs = newPacks
        notifyDataSetChanged()
    }
    
    fun setSelectedPack(packId: String) {
        val oldSelected = selectedPackId
        selectedPackId = packId
        
        // Update visual selection
        packs.forEachIndexed { index, pack ->
            when {
                pack.id == packId && pack.id != oldSelected -> notifyItemChanged(index)
                pack.id == oldSelected && pack.id != packId -> notifyItemChanged(index)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackViewHolder {
        val container = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                dpToPx(parent.context, 64),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(parent.context, 4), dpToPx(parent.context, 4), dpToPx(parent.context, 4), dpToPx(parent.context, 4))
        }
        
        val imageView = ImageView(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(parent.context, 48),
                dpToPx(parent.context, 48)
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(android.R.drawable.btn_default_small)
        }
        container.addView(imageView)
        
        val textView = TextView(parent.context).apply {
            textSize = 10f
            gravity = Gravity.CENTER
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(textView)
        
        return PackViewHolder(container, imageView, textView)
    }
    
    override fun onBindViewHolder(holder: PackViewHolder, position: Int) {
        val pack = packs[position]
        holder.bind(pack, pack.id == selectedPackId) { onPackSelected(pack) }
    }
    
    override fun getItemCount() = packs.size
    
    class PackViewHolder(
        itemView: LinearLayout,
        private val imageView: ImageView,
        private val textView: TextView
    ) : RecyclerView.ViewHolder(itemView) {
        
        fun bind(pack: StickerPack, isSelected: Boolean, onClick: () -> Unit) {
            textView.text = pack.name
            textView.setTextColor(
                if (isSelected) Color.parseColor("#2196F3") 
                else Color.parseColor("#666666")
            )
            
            itemView.setBackgroundColor(
                if (isSelected) Color.parseColor("#E3F2FD")
                else Color.TRANSPARENT
            )
            
            itemView.setOnClickListener { onClick() }
            
            // Load thumbnail - for now showing category emoji
            val categoryEmoji = when (pack.category.lowercase()) {
                "animals" -> "🐶"
                "emotions" -> "😊"
                "business" -> "💼"
                "food" -> "🍕"
                else -> "📦"
            }
            
            // For now, we'll create a simple text-based thumbnail
            // In production, use StickerServiceAdapter.loadPackThumbnail()
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }
    
    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}

/**
 * Adapter for sticker grid
 */
class StickerAdapter(
    private val onStickerSelected: (StickerData) -> Unit
) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {
    
    private var stickers = listOf<StickerData>()
    
    fun updateStickers(newStickers: List<StickerData>) {
        stickers = newStickers
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                dpToPx(parent.context, 54),
                dpToPx(parent.context, 54)
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(dpToPx(parent.context, 4), dpToPx(parent.context, 4), dpToPx(parent.context, 4), dpToPx(parent.context, 4))
            setBackgroundResource(android.R.drawable.btn_default_small)
        }
        
        return StickerViewHolder(imageView)
    }
    
    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        val sticker = stickers[position]
        holder.bind(sticker) { onStickerSelected(sticker) }
    }
    
    override fun getItemCount() = stickers.size
    
    class StickerViewHolder(private val imageView: ImageView) : RecyclerView.ViewHolder(imageView) {
        
        fun bind(sticker: StickerData, onClick: () -> Unit) {
            imageView.setOnClickListener { onClick() }
            
            // For now, show emoji if available, otherwise placeholder
            if (sticker.emojis.isNotEmpty()) {
                // Create a temporary TextView to show emoji
                val context = imageView.context
                val textView = TextView(context).apply {
                    text = sticker.emojis.first()
                    textSize = 24f
                    gravity = Gravity.CENTER
                    layoutParams = imageView.layoutParams
                    setBackgroundResource(android.R.drawable.btn_default_small)
                    setOnClickListener { onClick() }
                }
                
                // Replace ImageView with TextView temporarily
                (imageView.parent as? ViewGroup)?.let { parent ->
                    val index = parent.indexOfChild(imageView)
                    parent.removeViewAt(index)
                    parent.addView(textView, index)
                }
            } else {
                // Show placeholder or load actual image
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                
                // TODO: Load actual sticker image using StickerServiceAdapter.loadStickerThumbnail()
                // stickerService.loadStickerThumbnail(sticker, 54, 54) { bitmap ->
                //     bitmap?.let { imageView.setImageBitmap(it) }
                // }
            }
        }
    }
    
    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
