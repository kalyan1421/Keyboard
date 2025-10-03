package com.example.ai_keyboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import android.app.WallpaperManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Comprehensive Android Theme Manager
 * Handles Gboard baseline features + CleverType advanced customizations
 * Integrates with Flutter Theme Manager via SharedPreferences
 */
class ThemeManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ThemeManager"
        private const val PREFS_NAME = "FlutterSharedPreferences"
        private const val THEME_DATA_KEY = "flutter.current_theme_data"
        private const val THEME_ID_KEY = "flutter.current_theme_id"
        private const val IMAGE_CACHE_DIR = "theme_backgrounds"
        
        // Default theme values
        private const val DEFAULT_BACKGROUND_COLOR = 0xFFF5F5F5.toInt()
        private const val DEFAULT_KEY_BACKGROUND = 0xFFFFFFFF.toInt()
        private const val DEFAULT_KEY_TEXT = 0xFF212121.toInt()
        private const val DEFAULT_ACCENT = 0xFF1A73E8.toInt()
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var currentTheme: ThemeData? = null
    private val imageCache = mutableMapOf<String, Drawable>()
    
    // Theme change listeners
    private val listeners = mutableListOf<ThemeChangeListener>()
    
    // SharedPreferences listener for automatic theme updates
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == THEME_DATA_KEY || key == THEME_ID_KEY) {
            Log.d(TAG, "Theme data changed in SharedPreferences, reloading...")
            loadCurrentTheme()
            notifyThemeChanged()
        }
    }
    
    interface ThemeChangeListener {
        fun onThemeChanged(theme: ThemeData)
    }
    
    /**
     * Unified theme palette for consistent theming across all UI elements
     */
    data class ThemePalette(
        // Keyboard background
        val keyboardBg: Int,
        
        // Regular keys
        val keyBg: Int,
        val keyText: Int,
        val keyPressedBg: Int,
        val keyPressedText: Int,
        
        // Special keys (space, return, shift, backspace)
        val specialKeyBg: Int,
        val specialKeyText: Int,
        val specialKeyIcon: Int,
        val specialKeyPressedBg: Int,
        
        // Accent & highlights
        val accent: Int,
        val accentPressed: Int,
        
        // Toolbar
        val toolbarBg: Int,
        val toolbarIcon: Int,
        val toolbarIconPressed: Int,
        val toolbarDivider: Int,
        
        // Suggestion bar
        val suggestBg: Int,
        val suggestText: Int,
        val suggestChipBg: Int,
        val suggestChipText: Int,
        val suggestChipPressed: Int,
        val suggestChipBorder: Int,
        
        // Bottom navigation
        val navBarBg: Int,
        val navBarIcon: Int,
        val navBarIconSelected: Int,
        val navBarDivider: Int,
        
        // Emoji panel
        val emojiPanelBg: Int,
        val emojiPanelHeader: Int,
        val emojiCategoryBg: Int,
        val emojiCategorySelected: Int,
        val emojiCategoryText: Int,
        
        // GIF/Sticker panels
        val mediaPanelBg: Int,
        val mediaPanelHeader: Int,
        val mediaPanelSearchBg: Int,
        
        // Key styling
        val cornerRadius: Float,
        val keyBorderWidth: Float,
        val keyBorderColor: Int,
        val fontSize: Float,
        val suggestionBarHeight: Float,
        val toolbarHeight: Float
    )
    
    /**
     * Comprehensive theme data class
     * Maps to Flutter KeyboardThemeData
     */
    data class ThemeData(
        val id: String,
        val name: String,
        val description: String,
        
        // Background Properties
        val backgroundColor: Int,
        val keyBackgroundColor: Int,
        val keyPressedColor: Int,
        val keyDisabledColor: Int,
        
        // Text Properties  
        val keyTextColor: Int,
        val keyPressedTextColor: Int,
        val fontSize: Float,
        val fontFamily: String,
        val isBold: Boolean,
        val isItalic: Boolean,
        
        // Accent & Special Keys
        val accentColor: Int,
        val specialKeyColor: Int,
        val deleteKeyColor: Int,
        
        // Suggestion Bar
        val suggestionBarColor: Int,
        val suggestionTextColor: Int,
        val suggestionHighlightColor: Int,
        val suggestionFontSize: Float,
        val suggestionBold: Boolean,
        val suggestionItalic: Boolean,
        
        // Key Appearance
        val keyCornerRadius: Float,
        val showKeyShadows: Boolean,
        val shadowDepth: Float,
        val shadowColor: Int,
        val keyBorderWidth: Float,
        val keyBorderColor: Int,
        
        // Key Sizing & Spacing
        val keyHeight: Float,
        val keyWidth: Float,
        val keySpacing: Float,
        val rowSpacing: Float,
        
        // Advanced Background Options
        val backgroundType: String, // 'solid', 'gradient', 'image'
        val gradientColors: List<Int>,
        val gradientAngle: Float,
        val backgroundImagePath: String?,
        val backgroundOpacity: Float,
        val imageScaleType: String,
        
        // Material You Integration
        val useMaterialYou: Boolean,
        val followSystemTheme: Boolean,
        
        // Swipe Typing
        val swipeTrailColor: Int,
        val swipeTrailWidth: Float,
        val swipeTrailOpacity: Float,
        
        // Performance Optimizations
        val enableAnimations: Boolean,
        val animationDuration: Int
    ) {
        companion object {
            fun fromJson(json: JSONObject): ThemeData {
                // Parse gradient colors
                val gradientColorsArray = json.optJSONArray("gradientColors")
                val gradientColors = mutableListOf<Int>()
                if (gradientColorsArray != null) {
                    for (i in 0 until gradientColorsArray.length()) {
                        gradientColors.add(gradientColorsArray.getInt(i))
                    }
                }
                
                return ThemeData(
                    id = json.optString("id", "default"),
                    name = json.optString("name", "Default Theme"),
                    description = json.optString("description", ""),
                    backgroundColor = json.optInt("backgroundColor", DEFAULT_BACKGROUND_COLOR),
                    keyBackgroundColor = json.optInt("keyBackgroundColor", DEFAULT_KEY_BACKGROUND),
                    keyPressedColor = json.optInt("keyPressedColor", 0xFFE3F2FD.toInt()),
                    keyDisabledColor = json.optInt("keyDisabledColor", 0xFFEEEEEE.toInt()),
                    keyTextColor = json.optInt("keyTextColor", DEFAULT_KEY_TEXT),
                    keyPressedTextColor = json.optInt("keyPressedTextColor", 0xFF1976D2.toInt()),
                    fontSize = json.optDouble("fontSize", 18.0).toFloat(),
                    fontFamily = json.optString("fontFamily", "Roboto"),
                    isBold = json.optBoolean("isBold", false),
                    isItalic = json.optBoolean("isItalic", false),
                    accentColor = json.optInt("accentColor", DEFAULT_ACCENT),
                    specialKeyColor = json.optInt("specialKeyColor", 0xFFE0E0E0.toInt()),
                    deleteKeyColor = json.optInt("deleteKeyColor", 0xFFF44336.toInt()),
                    suggestionBarColor = json.optInt("suggestionBarColor", 0xFFFAFAFA.toInt()),
                    suggestionTextColor = json.optInt("suggestionTextColor", 0xFF424242.toInt()),
                    suggestionHighlightColor = json.optInt("suggestionHighlightColor", DEFAULT_ACCENT),
                    suggestionFontSize = json.optDouble("suggestionFontSize", 16.0).toFloat(),
                    suggestionBold = json.optBoolean("suggestionBold", false),
                    suggestionItalic = json.optBoolean("suggestionItalic", false),
                    keyCornerRadius = json.optDouble("keyCornerRadius", 6.0).toFloat(),
                    showKeyShadows = json.optBoolean("showKeyShadows", true),
                    shadowDepth = json.optDouble("shadowDepth", 2.0).toFloat(),
                    shadowColor = json.optInt("shadowColor", 0x1A000000),
                    keyBorderWidth = json.optDouble("keyBorderWidth", 0.5).toFloat(),
                    keyBorderColor = json.optInt("keyBorderColor", 0xFFE0E0E0.toInt()),
                    keyHeight = json.optDouble("keyHeight", 48.0).toFloat(),
                    keyWidth = json.optDouble("keyWidth", 32.0).toFloat(),
                    keySpacing = json.optDouble("keySpacing", 4.0).toFloat(),
                    rowSpacing = json.optDouble("rowSpacing", 8.0).toFloat(),
                    backgroundType = json.optString("backgroundType", "solid"),
                    gradientColors = gradientColors,
                    gradientAngle = json.optDouble("gradientAngle", 45.0).toFloat(),
                    backgroundImagePath = json.optString("backgroundImagePath", null),
                    backgroundOpacity = json.optDouble("backgroundOpacity", 1.0).toFloat(),
                    imageScaleType = json.optString("imageScaleType", "cover"),
                    useMaterialYou = json.optBoolean("useMaterialYou", false),
                    followSystemTheme = json.optBoolean("followSystemTheme", false),
                    swipeTrailColor = json.optInt("swipeTrailColor", json.optInt("accentColor", DEFAULT_ACCENT)),
                    swipeTrailWidth = json.optDouble("swipeTrailWidth", 8.0).toFloat(),
                    swipeTrailOpacity = json.optDouble("swipeTrailOpacity", 0.7).toFloat(),
                    enableAnimations = json.optBoolean("enableAnimations", true),
                    animationDuration = json.optInt("animationDuration", 150)
                )
            }
            
            fun getDefault(): ThemeData = ThemeData(
                id = "gboard_light",
                name = "Gboard Light",
                description = "Clean, minimal design inspired by Google Keyboard",
                backgroundColor = DEFAULT_BACKGROUND_COLOR,
                keyBackgroundColor = DEFAULT_KEY_BACKGROUND,
                keyPressedColor = 0xFFE3F2FD.toInt(),
                keyDisabledColor = 0xFFEEEEEE.toInt(),
                keyTextColor = DEFAULT_KEY_TEXT,
                keyPressedTextColor = 0xFF1976D2.toInt(),
                fontSize = 18.0f,
                fontFamily = "Roboto",
                isBold = false,
                isItalic = false,
                accentColor = DEFAULT_ACCENT,
                specialKeyColor = 0xFFE0E0E0.toInt(),
                deleteKeyColor = 0xFFF44336.toInt(),
                suggestionBarColor = 0xFFFAFAFA.toInt(),
                suggestionTextColor = 0xFF424242.toInt(),
                suggestionHighlightColor = DEFAULT_ACCENT,
                suggestionFontSize = 16.0f,
                suggestionBold = false,
                suggestionItalic = false,
                keyCornerRadius = 6.0f,
                showKeyShadows = true,
                shadowDepth = 2.0f,
                shadowColor = 0x1A000000,
                keyBorderWidth = 0.5f,
                keyBorderColor = 0xFFE0E0E0.toInt(),
                keyHeight = 48.0f,
                keyWidth = 32.0f,
                keySpacing = 4.0f,
                rowSpacing = 8.0f,
                backgroundType = "solid",
                gradientColors = listOf(0xFFF5F5F5.toInt(), 0xFFE0E0E0.toInt()),
                gradientAngle = 45.0f,
                backgroundImagePath = null,
                backgroundOpacity = 1.0f,
                imageScaleType = "cover",
                useMaterialYou = false,
                followSystemTheme = false,
                swipeTrailColor = DEFAULT_ACCENT,
                swipeTrailWidth = 8.0f,
                swipeTrailOpacity = 0.7f,
                enableAnimations = true,
                animationDuration = 150
            )
        }
    }
    
    /**
     * Initialize theme manager and load current theme
     */
    fun initialize() {
        Log.d(TAG, "Initializing ThemeManager")
        loadCurrentTheme()
        
        // Register SharedPreferences listener for automatic theme updates
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        
        // Create cache directory for background images
        val cacheDir = File(context.filesDir, IMAGE_CACHE_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    /**
     * Load current theme from SharedPreferences (set by Flutter)
     */
    private fun loadCurrentTheme() {
        try {
            // Force preferences reload to get latest data
            val freshPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val themeDataString = freshPrefs.getString(THEME_DATA_KEY, null)
            
            Log.d(TAG, "Loading theme data: ${themeDataString?.take(100)}...")
            
            if (themeDataString != null && themeDataString.isNotBlank()) {
                val jsonObject = JSONObject(themeDataString)
                currentTheme = ThemeData.fromJson(jsonObject)
                Log.d(TAG, "Successfully loaded theme: ${currentTheme?.name}")
            } else {
                Log.w(TAG, "No theme data found, using default")
                currentTheme = ThemeData.getDefault()
            }
            
            // Load background image if specified
            currentTheme?.backgroundImagePath?.let { imagePath ->
                loadBackgroundImage(imagePath)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading theme, using default", e)
            currentTheme = ThemeData.getDefault()
        }
    }
    
    /**
     * Get current theme data
     */
    fun getCurrentTheme(): ThemeData {
        return currentTheme ?: ThemeData.getDefault()
    }
    
    /**
     * Get unified theme palette for consistent UI theming
     */
    fun getCurrentPalette(): ThemePalette {
        val theme = getCurrentTheme()
        return ThemePalette(
            // Keyboard background
            keyboardBg = theme.backgroundColor,
            
            // Regular keys
            keyBg = theme.keyBackgroundColor,
            keyText = theme.keyTextColor,
            keyPressedBg = theme.keyPressedColor,
            keyPressedText = theme.keyPressedTextColor,
            
            // Special keys (space, return, shift, backspace)
            // Use slightly darker version of key background for contrast
            specialKeyBg = adjustColorBrightness(theme.keyBackgroundColor, 0.9f),
            specialKeyText = theme.keyTextColor,
            specialKeyIcon = theme.keyTextColor,
            specialKeyPressedBg = adjustColorBrightness(theme.keyBackgroundColor, 0.75f),
            
            // Accent & highlights
            accent = theme.accentColor,
            accentPressed = adjustColorBrightness(theme.accentColor, 0.8f),
            
            // Toolbar
            toolbarBg = theme.backgroundColor,
            toolbarIcon = theme.keyTextColor,
            toolbarIconPressed = theme.accentColor,
            toolbarDivider = adjustColorBrightness(theme.backgroundColor, 0.9f),
            
            // Suggestion bar
            suggestBg = theme.suggestionBarColor,
            suggestText = theme.suggestionTextColor,
            suggestChipBg = theme.keyBackgroundColor,
            suggestChipText = theme.keyTextColor,
            suggestChipPressed = theme.keyPressedColor,
            suggestChipBorder = adjustColorBrightness(theme.keyBackgroundColor, 0.85f),
            
            // Bottom navigation
            navBarBg = theme.backgroundColor,
            navBarIcon = theme.keyTextColor,
            navBarIconSelected = theme.accentColor,
            navBarDivider = adjustColorBrightness(theme.backgroundColor, 0.9f),
            
            // Emoji panel
            emojiPanelBg = theme.backgroundColor,
            emojiPanelHeader = adjustColorBrightness(theme.backgroundColor, 0.97f),
            emojiCategoryBg = theme.backgroundColor,
            emojiCategorySelected = theme.accentColor,
            emojiCategoryText = theme.keyTextColor,
            
            // GIF/Sticker panels
            mediaPanelBg = theme.backgroundColor,
            mediaPanelHeader = adjustColorBrightness(theme.backgroundColor, 0.97f),
            mediaPanelSearchBg = theme.keyBackgroundColor,
            
            // Key styling
            cornerRadius = theme.keyCornerRadius,
            keyBorderWidth = theme.keyBorderWidth,
            keyBorderColor = theme.keyBorderColor,
            fontSize = theme.fontSize,
            suggestionBarHeight = 56f, // Increased from 48dp for better touch targets
            toolbarHeight = 48f
        )
    }
    
    /**
     * Adjust color brightness for visual hierarchy
     */
    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }
    
    /**
     * Reload theme from SharedPreferences
     * Called when Flutter app updates theme
     */
    fun reloadTheme(): Boolean {
        return try {
            Log.d(TAG, "Reloading theme from SharedPreferences...")
            
            val prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val themeJson = prefs.getString("flutter.current_theme_data", null)
            
            if (themeJson != null) {
                currentTheme = ThemeData.fromJson(JSONObject(themeJson))
                Log.d(TAG, "Theme reloaded: ${currentTheme?.name}")
                
                // Notify listeners immediately
                notifyThemeChanged()
                true
            } else {
                Log.w(TAG, "No theme data found in SharedPreferences")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reloading theme", e)
            false
        }
    }
    
    /**
     * Create Paint for key text with current theme
     */
    fun createKeyTextPaint(): Paint {
        val theme = getCurrentTheme()
        return Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = theme.fontSize * context.resources.displayMetrics.density
            color = theme.keyTextColor
            typeface = createTypeface(theme.fontFamily, theme.isBold, theme.isItalic)
        }
    }
    
    /**
     * Create Paint for pressed key text
     */
    fun createPressedKeyTextPaint(): Paint {
        val theme = getCurrentTheme()
        return Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = theme.fontSize * context.resources.displayMetrics.density
            color = theme.keyPressedTextColor
            typeface = createTypeface(theme.fontFamily, theme.isBold, theme.isItalic)
        }
    }
    
    /**
     * Create Paint for key background
     */
    fun createKeyBackgroundPaint(): Paint {
        val theme = getCurrentTheme()
        return Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = theme.keyBackgroundColor
        }
    }
    
    /**
     * Create Paint for pressed key background
     */
    fun createPressedKeyBackgroundPaint(): Paint {
        val theme = getCurrentTheme()
        return Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = theme.keyPressedColor
        }
    }
    
    /**
     * Create Paint for special keys (Shift, Enter, etc.)
     */
    fun createSpecialKeyBackgroundPaint(): Paint {
        val theme = getCurrentTheme()
        return Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = theme.specialKeyColor
        }
    }
    
    /**
     * Create Paint for accent/highlighted keys
     */
    fun createAccentKeyBackgroundPaint(): Paint {
        val theme = getCurrentTheme()
        return Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = theme.accentColor
        }
    }
    
    /**
     * Create Paint for key borders
     */
    fun createKeyBorderPaint(): Paint {
        val theme = getCurrentTheme()
        return Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = theme.keyBorderWidth
            color = theme.keyBorderColor
        }
    }
    
    /**
     * Create Paint for key shadows
     */
    fun createKeyShadowPaint(): Paint {
        val theme = getCurrentTheme()
        return Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = theme.shadowColor
        }
    }
    
    /**
     * Create Paint for suggestion text
     */
    fun createSuggestionTextPaint(): Paint {
        val theme = getCurrentTheme()
        return Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = theme.suggestionFontSize * context.resources.displayMetrics.density
            color = theme.suggestionTextColor
            typeface = createTypeface(theme.fontFamily, theme.suggestionBold, theme.suggestionItalic)
        }
    }
    
    /**
     * Create keyboard background drawable
     */
    fun createKeyboardBackgroundDrawable(): Drawable {
        val theme = getCurrentTheme()
        
        return when (theme.backgroundType) {
            "gradient" -> createGradientBackground(theme)
            "image" -> createImageBackground(theme) ?: createSolidBackground(theme)
            else -> createSolidBackground(theme)
        }
    }
    
    /**
     * Create solid color background
     */
    private fun createSolidBackground(theme: ThemeData): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(theme.backgroundColor)
        }
    }
    
    /**
     * Create gradient background
     */
    private fun createGradientBackground(theme: ThemeData): Drawable {
        val colors = theme.gradientColors.toIntArray()
        val orientation = getGradientOrientation(theme.gradientAngle)
        
        return GradientDrawable(orientation, colors).apply {
            shape = GradientDrawable.RECTANGLE
        }
    }
    
    /**
     * Create image background
     */
    private fun createImageBackground(theme: ThemeData): Drawable? {
        val imagePath = theme.backgroundImagePath ?: return null
        
        return try {
            val drawable = imageCache[imagePath] ?: loadBackgroundImage(imagePath)
            drawable?.let { 
                // Apply opacity
                it.alpha = (theme.backgroundOpacity * 255).toInt()
                it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image background", e)
            null
        }
    }
    
    /**
     * Load background image from file path
     */
    private fun loadBackgroundImage(imagePath: String): Drawable? {
        return try {
            val imageFile = File(imagePath)
            if (!imageFile.exists()) {
                Log.w(TAG, "Background image not found: $imagePath")
                return null
            }
            
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                val drawable = BitmapDrawable(context.resources, bitmap)
                imageCache[imagePath] = drawable
                Log.d(TAG, "Loaded background image: $imagePath")
                drawable
            } else {
                Log.e(TAG, "Failed to decode image: $imagePath")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading background image: $imagePath", e)
            null
        }
    }
    
    /**
     * Convert gradient angle to GradientDrawable.Orientation
     */
    private fun getGradientOrientation(angle: Float): GradientDrawable.Orientation {
        val normalizedAngle = ((angle % 360) + 360) % 360
        
        return when {
            normalizedAngle < 22.5 || normalizedAngle >= 337.5 -> GradientDrawable.Orientation.LEFT_RIGHT
            normalizedAngle < 67.5 -> GradientDrawable.Orientation.BL_TR
            normalizedAngle < 112.5 -> GradientDrawable.Orientation.BOTTOM_TOP
            normalizedAngle < 157.5 -> GradientDrawable.Orientation.BR_TL
            normalizedAngle < 202.5 -> GradientDrawable.Orientation.RIGHT_LEFT
            normalizedAngle < 247.5 -> GradientDrawable.Orientation.TR_BL
            normalizedAngle < 292.5 -> GradientDrawable.Orientation.TOP_BOTTOM
            else -> GradientDrawable.Orientation.TL_BR
        }
    }
    
    /**
     * Create typeface with font family and style
     */
    private fun createTypeface(fontFamily: String, bold: Boolean, italic: Boolean): Typeface {
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        
        return when (fontFamily.lowercase()) {
            "roboto" -> Typeface.create("sans-serif", style)
            "roboto-mono", "monospace" -> Typeface.create("monospace", style)
            "serif" -> Typeface.create("serif", style)
            else -> Typeface.create(fontFamily, style)
        }
    }
    
    /**
     * Get Material You colors (Android 12+)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun getMaterialYouColors(): Map<String, Int>? {
        return try {
            val colors = mutableMapOf<String, Int>()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val colorScheme = context.resources.configuration.uiMode
                
                // This would need proper Material You color extraction
                // For now, return sample colors
                colors["background"] = ContextCompat.getColor(context, android.R.color.system_accent1_100)
                colors["surface"] = ContextCompat.getColor(context, android.R.color.system_accent1_50)
                colors["accent"] = ContextCompat.getColor(context, android.R.color.system_accent1_600)
                colors["onSurface"] = ContextCompat.getColor(context, android.R.color.system_neutral1_900)
            }
            
            colors
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Material You colors", e)
            null
        }
    }
    
    /**
     * Add theme change listener
     */
    fun addThemeChangeListener(listener: ThemeChangeListener) {
        listeners.add(listener)
    }
    
    /**
     * Remove theme change listener
     */
    fun removeThemeChangeListener(listener: ThemeChangeListener) {
        listeners.remove(listener)
    }
    
    /**
     * Notify all listeners of theme change
     */
    private fun notifyThemeChanged() {
        val theme = getCurrentTheme()
        listeners.forEach { listener ->
            try {
                listener.onThemeChanged(theme)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying theme change listener", e)
            }
        }
    }
    
    /**
     * Clear image cache
     */
    fun clearImageCache() {
        imageCache.clear()
    }
    
    /**
     * Extract Material You theme from wallpaper (Android 8.1+)
     * Uses Palette API for dominant color extraction
     */
    fun extractMaterialYouTheme(): ThemeData? {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val wallpaperDrawable = wallpaperManager.drawable ?: return null
            
            // Convert drawable to bitmap
            val bitmap = when (wallpaperDrawable) {
                is BitmapDrawable -> wallpaperDrawable.bitmap
                else -> {
                    val width = wallpaperDrawable.intrinsicWidth.coerceAtLeast(1)
                    val height = wallpaperDrawable.intrinsicHeight.coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    wallpaperDrawable.setBounds(0, 0, canvas.width, canvas.height)
                    wallpaperDrawable.draw(canvas)
                    bmp
                }
            }
            
            // Extract color palette from wallpaper
            val palette = Palette.from(bitmap).generate()
            
            // Determine if dark mode based on dominant color
            val dominantColor = palette.getDominantColor(Color.BLUE)
            val isDarkMode = isColorDark(dominantColor)
            
            // Extract Material You colors
            val primaryColor = palette.getVibrantColor(dominantColor)
            val secondaryColor = palette.getLightVibrantColor(palette.getLightMutedColor(Color.CYAN))
            val accentColor = palette.getDarkVibrantColor(palette.getVibrantColor(Color.MAGENTA))
            val backgroundColor = if (isDarkMode) {
                Color.parseColor("#1C1B1F") // Dark background
            } else {
                Color.parseColor("#FFFBFE") // Light background
            }
            
            // Create adaptive key colors with proper contrast
            val keyBackgroundColor = if (isDarkMode) {
                adjustColorBrightness(primaryColor, 0.3f)
            } else {
                adjustColorBrightness(primaryColor, 1.8f)
            }
            
            val keyTextColor = if (isColorDark(keyBackgroundColor)) Color.WHITE else Color.BLACK
            
            val specialKeyColor = if (isDarkMode) {
                adjustColorBrightness(secondaryColor, 0.4f)
            } else {
                adjustColorBrightness(secondaryColor, 1.6f)
            }
            
            // Create Material You theme
            ThemeData(
                id = "material_you_${System.currentTimeMillis()}",
                name = "Material You",
                description = "Adaptive colors from your wallpaper",
                backgroundColor = backgroundColor,
                keyBackgroundColor = keyBackgroundColor,
                keyPressedColor = adjustColorBrightness(keyBackgroundColor, 0.85f),
                keyDisabledColor = adjustColorAlpha(keyBackgroundColor, 0.5f),
                keyTextColor = keyTextColor,
                keyPressedTextColor = keyTextColor,
                fontSize = 18f,
                fontFamily = "Roboto",
                isBold = false,
                isItalic = false,
                accentColor = accentColor,
                specialKeyColor = specialKeyColor,
                deleteKeyColor = accentColor,
                suggestionBarColor = adjustColorAlpha(backgroundColor, 0.95f),
                suggestionTextColor = keyTextColor,
                suggestionHighlightColor = accentColor,
                suggestionFontSize = 16f,
                suggestionBold = false,
                suggestionItalic = false,
                keyCornerRadius = 8f,
                showKeyShadows = true,
                shadowDepth = 2f,
                shadowColor = if (isDarkMode) 0x40000000 else 0x1A000000,
                keyBorderWidth = 0f,
                keyBorderColor = Color.TRANSPARENT,
                keyHeight = 48f,
                keyWidth = 32f,
                keySpacing = 4f,
                rowSpacing = 8f,
                backgroundType = "solid",
                gradientColors = listOf(backgroundColor, adjustColorBrightness(backgroundColor, 0.95f)),
                gradientAngle = 45f,
                backgroundImagePath = null,
                backgroundOpacity = 1f,
                imageScaleType = "cover",
                useMaterialYou = true,
                followSystemTheme = true,
                swipeTrailColor = accentColor,
                swipeTrailWidth = 8f,
                swipeTrailOpacity = 0.7f,
                enableAnimations = true,
                animationDuration = 200
            ).also {
                Log.d(TAG, "🎨 Material You theme extracted successfully")
                Log.d(TAG, "Primary: ${Integer.toHexString(primaryColor)}, " +
                          "Secondary: ${Integer.toHexString(secondaryColor)}, " +
                          "Accent: ${Integer.toHexString(accentColor)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting Material You theme", e)
            null
        }
    }
    
    /**
     * Check if a color is considered dark
     */
    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 
                           0.587 * Color.green(color) + 
                           0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
    
    /**
     * Adjust color alpha channel
     */
    private fun adjustColorAlpha(color: Int, alpha: Float): Int {
        val a = (255 * alpha).toInt().coerceIn(0, 255)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(a, r, g, b)
    }
    
    /**
     * Get system theme colors (Android 12+)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSystemDynamicColors(): Map<String, Int>? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mapOf(
                    "primary" to ContextCompat.getColor(context, android.R.color.system_accent1_600),
                    "secondary" to ContextCompat.getColor(context, android.R.color.system_accent2_600),
                    "tertiary" to ContextCompat.getColor(context, android.R.color.system_accent3_600),
                    "background" to ContextCompat.getColor(context, android.R.color.system_neutral1_50),
                    "surface" to ContextCompat.getColor(context, android.R.color.system_neutral1_100),
                    "onSurface" to ContextCompat.getColor(context, android.R.color.system_neutral1_900)
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting system dynamic colors", e)
            null
        }
    }
    
    /**
     * Cleanup theme manager resources
     */
    fun cleanup() {
        try {
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
            listeners.clear()
            clearImageCache()
            Log.d(TAG, "ThemeManager cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during ThemeManager cleanup", e)
        }
    }
    
    /**
     * Get theme properties for debugging
     */
    fun getThemeDebugInfo(): String {
        val theme = getCurrentTheme()
        return """
            Theme: ${theme.name} (${theme.id})
            Background: ${Integer.toHexString(theme.backgroundColor)}
            Key Background: ${Integer.toHexString(theme.keyBackgroundColor)}
            Text Color: ${Integer.toHexString(theme.keyTextColor)}
            Font Size: ${theme.fontSize}
            Background Type: ${theme.backgroundType}
        """.trimIndent()
    }
    
    /**
     * Create themed drawable for toolbar buttons
     */
    fun createToolbarButtonDrawable(): Drawable {
        val palette = getCurrentPalette()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(android.graphics.Color.TRANSPARENT)
            cornerRadius = palette.cornerRadius
        }
    }
    
    /**
     * Create themed drawable for suggestion chips
     */
    fun createSuggestionChipDrawable(): Drawable {
        val palette = getCurrentPalette()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(palette.suggestChipBg)
            cornerRadius = palette.cornerRadius
            if (palette.keyBorderWidth > 0) {
                setStroke(palette.keyBorderWidth.toInt(), palette.suggestChipBorder)
            }
        }
    }
    
    /**
     * Create themed drawable for special keys (space, return, etc.)
     */
    fun createSpecialKeyDrawable(): Drawable {
        val palette = getCurrentPalette()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(palette.specialKeyBg)
            cornerRadius = palette.cornerRadius
            if (palette.keyBorderWidth > 0) {
                setStroke(palette.keyBorderWidth.toInt(), palette.keyBorderColor)
            }
        }
    }
    
    /**
     * Create themed paint for toolbar icons
     */
    fun createToolbarIconPaint(): Paint {
        val palette = getCurrentPalette()
        return Paint().apply {
            isAntiAlias = true
            color = palette.toolbarIcon
            style = Paint.Style.FILL
        }
    }
    
    /**
     * Create themed paint for special key icons
     */
    fun createSpecialKeyIconPaint(): Paint {
        val palette = getCurrentPalette()
        return Paint().apply {
            isAntiAlias = true
            color = palette.specialKeyIcon
            style = Paint.Style.FILL
        }
    }
}
