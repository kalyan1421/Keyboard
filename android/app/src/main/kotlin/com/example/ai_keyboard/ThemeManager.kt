package com.example.ai_keyboard

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.*
import android.util.Log
import android.util.LruCache
import androidx.core.content.res.ResourcesCompat
import com.example.ai_keyboard.themes.KeyboardThemeV2
import com.example.ai_keyboard.themes.ThemePaletteV2
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import kotlin.math.*

/**
 * CleverType Theme Engine V2 - Single Source of Truth
 * Replaces old theme system with centralized JSON-based theming
 * All colors, drawables, and styling come from KeyboardThemeV2
 */
class ThemeManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ThemeManagerV2"
        private const val PREFS_NAME = "FlutterSharedPreferences"
        // CRITICAL: Flutter plugin adds "flutter." prefix automatically!
        // So we access "flutter.theme.v2.json" which matches Flutter's 'theme.v2.json' key
        private const val THEME_V2_KEY = "flutter.theme.v2.json"
        private const val SETTINGS_CHANGED_KEY = "flutter.keyboard_settings.settings_changed"
        
        // Cache sizes
        private const val DRAWABLE_CACHE_SIZE = 50
        private const val IMAGE_CACHE_SIZE = 10
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var currentTheme: KeyboardThemeV2? = null
    private var currentPalette: ThemePaletteV2? = null
    private var themeHash: String = ""
    
    // LRU Caches for performance
    private val drawableCache = LruCache<String, Drawable>(DRAWABLE_CACHE_SIZE)
    private val imageCache = LruCache<String, Drawable>(IMAGE_CACHE_SIZE)
    
    // Theme change listeners
    private val listeners = mutableListOf<ThemeChangeListener>()
    
    // SharedPreferences listener for automatic theme updates
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == THEME_V2_KEY || key == SETTINGS_CHANGED_KEY) {
            loadThemeFromPrefs()
            notifyThemeChanged()
        }
    }
    
    interface ThemeChangeListener {
        fun onThemeChanged(theme: KeyboardThemeV2, palette: ThemePaletteV2)
    }
    
    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        loadThemeFromPrefs()
    }
    
    fun addThemeChangeListener(listener: ThemeChangeListener) {
        listeners.add(listener)
    }
    
    fun removeThemeChangeListener(listener: ThemeChangeListener) {
        listeners.remove(listener)
    }
    
    private fun notifyThemeChanged() {
        val theme = currentTheme
        val palette = currentPalette
        if (theme != null && palette != null) {
            listeners.forEach { it.onThemeChanged(theme, palette) }
        }
    }
    
    /**
     * Load theme from SharedPreferences
     * Migrates old themes to V2 format automatically
     */
    private fun loadThemeFromPrefs() {
        val themeJson = prefs.getString(THEME_V2_KEY, null)
        
        if (themeJson != null) {
            // Load V2 theme
            val theme = KeyboardThemeV2.fromJson(themeJson)
            val newHash = themeJson.hashCode().toString()
            
            // Only update if theme actually changed
            if (newHash != themeHash) {
                currentTheme = theme
                currentPalette = ThemePaletteV2(theme)
                themeHash = newHash
                
                // Clear caches on theme change
                drawableCache.evictAll()
                imageCache.evictAll()
            }
        } else {
            // Check for old theme format and migrate
            migrateOldTheme()
        }
    }
    
    /**
     * Migrate old theme data to V2 format
     */
    private fun migrateOldTheme() {
        // Check for old theme keys
        val oldThemeData = prefs.getString("flutter.current_theme_data", null)
        
        if (oldThemeData != null) {
            try {
                val oldTheme = JSONObject(oldThemeData)
                val migratedTheme = createMigratedTheme(oldTheme)
                
                // Save as V2 and remove old keys
                saveTheme(migratedTheme)
                prefs.edit()
                    .remove("flutter.current_theme_data")
                    .remove("flutter.current_theme_id")
                    .apply()
        } catch (e: Exception) {
                loadDefaultTheme()
            }
            } else {
            loadDefaultTheme()
        }
    }
    
    private fun createMigratedTheme(oldTheme: JSONObject): KeyboardThemeV2 {
        // Map old theme fields to new V2 structure with reasonable defaults
        val theme = KeyboardThemeV2.createDefault().copy(
            id = oldTheme.optString("id", "migrated_${System.currentTimeMillis()}"),
            name = oldTheme.optString("name", "Migrated Theme"),
            background = KeyboardThemeV2.Background(
                type = "solid",
                color = parseOldColor(oldTheme.optString("backgroundColor", "#1B1B1F")),
                imagePath = null,
                imageOpacity = 0.85f,
                gradient = null,
                overlayEffects = emptyList(),
                adaptive = null
            ),
            keys = KeyboardThemeV2.Keys(
                preset = "bordered",
                bg = parseOldColor(oldTheme.optString("keyBackgroundColor", "#3A3A3F")),
                text = parseOldColor(oldTheme.optString("keyTextColor", "#FFFFFF")),
                pressed = parseOldColor(oldTheme.optString("keyPressedColor", "#505056")),
                rippleAlpha = 0.12f,
                border = KeyboardThemeV2.Keys.Border(
                    enabled = true,
                    color = parseOldColor(oldTheme.optString("keyBorderColor", "#636366")),
                    widthDp = 1.0f
                ),
                radius = oldTheme.optDouble("keyCornerRadius", 10.0).toFloat(),
                shadow = KeyboardThemeV2.Keys.Shadow(
                    enabled = oldTheme.optBoolean("showKeyShadows", true),
                    elevationDp = oldTheme.optDouble("shadowDepth", 2.0).toFloat(),
                    glow = false
                ),
                font = KeyboardThemeV2.Keys.Font(
                    family = oldTheme.optString("fontFamily", "Roboto"),
                    sizeSp = oldTheme.optDouble("fontSize", 18.0).toFloat(),
                    bold = oldTheme.optBoolean("isBold", false),
                    italic = oldTheme.optBoolean("isItalic", false)
                )
            ),
            specialKeys = KeyboardThemeV2.SpecialKeys(
                accent = parseOldColor(oldTheme.optString("accentColor", "#FF9F1A")),
                useAccentForEnter = true,
                applyTo = listOf("enter", "globe", "emoji", "mic"),
                spaceLabelColor = parseOldColor(oldTheme.optString("keyTextColor", "#FFFFFF"))
            )
        )
        
        return theme
    }
    
    private fun parseOldColor(colorStr: String): Int {
        return try {
            Color.parseColor(colorStr)
        } catch (e: Exception) {
            Color.TRANSPARENT
        }
    }
    
    private fun loadDefaultTheme() {
        val defaultTheme = KeyboardThemeV2.createDefault()
        saveTheme(defaultTheme)
    }
    
    /**
     * Save theme to SharedPreferences
     */
    fun saveTheme(theme: KeyboardThemeV2) {
        val json = theme.toJson()
        prefs.edit()
            .putString(THEME_V2_KEY, json)
            .putBoolean(SETTINGS_CHANGED_KEY, true)
            .apply()
    }
    
    /**
     * Get current theme (never null)
     */
    fun getCurrentTheme(): KeyboardThemeV2 {
        return currentTheme ?: KeyboardThemeV2.createDefault()
    }
    
    /**
     * Get current palette (derived colors)
     */
    fun getCurrentPalette(): ThemePaletteV2 {
        return currentPalette ?: ThemePaletteV2(KeyboardThemeV2.createDefault())
    }
    
    /**
     * Force reload theme from preferences
     */
    fun reload() {
        loadThemeFromPrefs()
    }
    
    // ===== DRAWABLE FACTORY METHODS =====
    
    /**
     * Create cached key background drawable
     */
    fun createKeyDrawable(): Drawable {
        val cacheKey = "key_${themeHash}"
        return drawableCache.get(cacheKey) ?: run {
            val drawable = buildKeyDrawable()
            drawableCache.put(cacheKey, drawable)
            drawable
        }
    }
    
    /**
     * Create cached key pressed drawable
     */
    fun createKeyPressedDrawable(): Drawable {
        val cacheKey = "key_pressed_${themeHash}"
        return drawableCache.get(cacheKey) ?: run {
            val drawable = buildKeyPressedDrawable()
            drawableCache.put(cacheKey, drawable)
            drawable
        }
    }
    
    /**
     * Create cached special key drawable (with accent)
     */
    fun createSpecialKeyDrawable(): Drawable {
        val cacheKey = "special_key_${themeHash}"
        return drawableCache.get(cacheKey) ?: run {
            val drawable = buildSpecialKeyDrawable()
            drawableCache.put(cacheKey, drawable)
            drawable
        }
    }
    
    /**
     * Create cached toolbar background drawable
     */
    fun createToolbarBackground(): Drawable {
        val cacheKey = "toolbar_bg_$themeHash"
        drawableCache.get(cacheKey)?.let { return it }
        val palette = getCurrentPalette()
        val drawable = GradientDrawable().apply {
            setColor(palette.toolbarBg)
            cornerRadius = 0f // No corner radius for seamless connection
        }
        drawableCache.put(cacheKey, drawable)
        return drawable
    }

    fun createSuggestionBarBackground(): Drawable {
        val cacheKey = "suggestion_bg_$themeHash"
        drawableCache.get(cacheKey)?.let { return it }
        val palette = getCurrentPalette()
        val drawable = GradientDrawable().apply {
            setColor(palette.suggestionBg)
            cornerRadius = 0f // No corner radius for seamless connection
        }
        drawableCache.put(cacheKey, drawable)
        return drawable
    }

    /**
     * DEPRECATED: Chips removed - suggestions are now text-only
     * Returning transparent drawable for backward compatibility
     */
    @Deprecated("Suggestions are now text-only, no chip backgrounds")
    fun createSuggestionChip(isPressed: Boolean): Drawable {
        return ColorDrawable(Color.TRANSPARENT)
    }
    
    /**
     * DEPRECATED: Chips removed - suggestions are now text-only
     * Returning transparent drawable for backward compatibility
     */
    @Deprecated("Suggestions are now text-only, no chip backgrounds")
    fun createSuggestionChipDrawable(): Drawable {
        return ColorDrawable(Color.TRANSPARENT)
    }
    
    /**
     * Create cached keyboard background drawable
     */
    fun createKeyboardBackground(): Drawable {
        val theme = getCurrentTheme()
        val palette = getCurrentPalette()
        val cacheKey = "keyboard_bg_${theme.background.type}_${palette.isSeasonalActive}_${themeHash}"
        
        return drawableCache.get(cacheKey) ?: run {
            val drawable = when (theme.background.type) {
                "adaptive" -> buildAdaptiveBackground()
                "gradient" -> buildGradientBackground()
                "image" -> buildImageBackground()
                else -> buildSolidDrawable(theme.background.color ?: Color.BLACK)
            }
            
            // Apply seasonal overlay if active
            val finalDrawable = if (palette.isSeasonalActive) {
                applySeasonalOverlay(drawable, palette.currentSeasonalPack)
            } else {
                drawable
            }
            
            drawableCache.put(cacheKey, finalDrawable)
            finalDrawable
        }
    }
    
    /**
     * Create sticker overlay drawable
     */
    fun createStickerOverlay(): Drawable? {
        val theme = getCurrentTheme()
        val palette = getCurrentPalette()
        
        if (!palette.hasStickers) return null
        
        val cacheKey = "sticker_${theme.stickers.pack}_${theme.stickers.position}_${themeHash}"
        
        return drawableCache.get(cacheKey) ?: run {
            val drawable = loadStickerDrawable(theme.stickers.pack)
            drawable?.let { 
                it.alpha = (palette.stickerOpacity * 255).toInt()
                drawableCache.put(cacheKey, it)
            }
            drawable
        }
    }

    // ===== PRIVATE DRAWABLE BUILDERS =====
    
    private fun buildKeyDrawable(): Drawable {
        val palette = getCurrentPalette()
        val theme = getCurrentTheme()
        
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(palette.keyBg)
        drawable.cornerRadius = palette.keyRadius * context.resources.displayMetrics.density
        
        // Apply border if enabled
        if (palette.keyBorderEnabled) {
            drawable.setStroke(
                (palette.keyBorderWidth * context.resources.displayMetrics.density).toInt(),
                palette.keyBorderColor
            )
        }
        
        // Add shadow/elevation if enabled
        if (palette.keyShadowEnabled) {
            // Note: GradientDrawable doesn't support shadows directly
            // For full shadow support, would need LayerDrawable with shadow layer
        }
        
        return drawable
    }
    
    private fun buildKeyPressedDrawable(): Drawable {
        val palette = getCurrentPalette()
        
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(palette.keyPressed)
        drawable.cornerRadius = palette.keyRadius * context.resources.displayMetrics.density
        
        if (palette.keyBorderEnabled) {
            drawable.setStroke(
                (palette.keyBorderWidth * context.resources.displayMetrics.density).toInt(),
                palette.keyBorderColor
            )
        }
        
        return drawable
    }
    
    private fun buildSpecialKeyDrawable(): Drawable {
        val palette = getCurrentPalette()
        
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(palette.specialAccent)
        drawable.cornerRadius = palette.keyRadius * context.resources.displayMetrics.density
        
        if (palette.keyBorderEnabled) {
            drawable.setStroke(
                (palette.keyBorderWidth * context.resources.displayMetrics.density).toInt(),
                palette.keyBorderColor
            )
        }
        
        return drawable
    }
    
    @Deprecated("Chips removed - suggestions are text-only")
    private fun buildSuggestionChipDrawable(): Drawable {
        return ColorDrawable(Color.TRANSPARENT)
    }
    
    @Deprecated("Chips removed - suggestions are text-only")
    private fun buildSuggestionChipPressedDrawable(): Drawable {
        return ColorDrawable(Color.TRANSPARENT)
    }
    
    private fun buildSolidDrawable(color: Int): Drawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(color)
        return drawable
    }
    
    private fun buildGradientBackground(): Drawable {
        val theme = getCurrentTheme()
        val gradient = theme.background.gradient ?: return buildSolidDrawable(Color.BLACK)
        
        val orientation = when (gradient.orientation) {
            "TOP_BOTTOM" -> GradientDrawable.Orientation.TOP_BOTTOM
            "TL_BR" -> GradientDrawable.Orientation.TL_BR
            "TR_BL" -> GradientDrawable.Orientation.TR_BL
            "LEFT_RIGHT" -> GradientDrawable.Orientation.LEFT_RIGHT
            "BR_TL" -> GradientDrawable.Orientation.BR_TL
            "BL_TR" -> GradientDrawable.Orientation.BL_TR
            else -> GradientDrawable.Orientation.TOP_BOTTOM
        }
        
        val drawable = GradientDrawable(orientation, gradient.colors.toIntArray())
        drawable.shape = GradientDrawable.RECTANGLE
        
        return drawable
    }
    
    private fun buildImageBackground(): Drawable {
        val theme = getCurrentTheme()
        val imagePath = theme.background.imagePath
        
        if (imagePath.isNullOrEmpty()) {
            return buildSolidDrawable(theme.background.color ?: Color.BLACK)
        }
        
        // Try to load cached image
        val cacheKey = "bg_image_$imagePath"
        return imageCache.get(cacheKey) ?: run {
            try {
                val bitmap = loadImageBitmap(imagePath)
                val drawable = BitmapDrawable(context.resources, bitmap)
                drawable.alpha = (theme.background.imageOpacity * 255).toInt()
                
                imageCache.put(cacheKey, drawable)
                drawable
            } catch (e: Exception) {
                buildSolidDrawable(theme.background.color ?: Color.BLACK)
            }
        }
    }
    
    private fun loadImageBitmap(path: String): Bitmap {
        return if (path.startsWith("/")) {
            // Absolute path
            val file = File(path)
            BitmapFactory.decodeStream(FileInputStream(file))
        } else {
            // Asset path
            val inputStream = context.assets.open(path)
            BitmapFactory.decodeStream(inputStream)
        }
    }
    
    // ===== RIPPLE DRAWABLE FACTORY =====
    
    /**
     * Create ripple drawable for key press effect
     */
    fun createKeyRippleDrawable(): RippleDrawable? {
        val palette = getCurrentPalette()
        
        if (palette.pressAnimation != "ripple") {
            return null
        }
        
        val rippleColor = ColorStateList.valueOf(
            Color.argb(
                (palette.rippleAlpha * 255).toInt(),
                Color.red(palette.keyText),
                Color.green(palette.keyText),
                Color.blue(palette.keyText)
            )
        )
        
        val mask = GradientDrawable()
        mask.shape = GradientDrawable.RECTANGLE
        mask.setColor(Color.WHITE)
        mask.cornerRadius = palette.keyRadius * context.resources.displayMetrics.density
        
        return RippleDrawable(rippleColor, createKeyDrawable(), mask)
    }
    
    // ===== PAINT FACTORY METHODS =====
    
    /**
     * Create text paint for keys
     */
    fun createKeyTextPaint(): Paint {
        val palette = getCurrentPalette()
        
        return Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = palette.keyFontSize * context.resources.displayMetrics.scaledDensity
            color = palette.keyText
            typeface = createTypeface(palette.keyFontFamily, palette.keyFontBold, palette.keyFontItalic)
        }
    }
    
    /**
     * Create text paint for suggestions
     */
    fun createSuggestionTextPaint(): Paint {
        val palette = getCurrentPalette()
        
        return Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = palette.suggestionFontSize * context.resources.displayMetrics.scaledDensity
            color = palette.suggestionText
            typeface = createTypeface(palette.suggestionFontFamily, palette.suggestionFontBold, false)
        }
    }
    
    /**
     * Create text paint for space label
     */
    fun createSpaceLabelPaint(): Paint {
        val palette = getCurrentPalette()
        
        return Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = (palette.keyFontSize * 0.75f) * context.resources.displayMetrics.scaledDensity
            color = palette.spaceLabelColor
            typeface = createTypeface(palette.keyFontFamily, false, false)
        }
    }
    
    private fun createTypeface(family: String, bold: Boolean, italic: Boolean): Typeface {
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        
        return try {
            // Try to load custom font family
            val typeface = ResourcesCompat.getFont(context, 
                context.resources.getIdentifier(family.lowercase(), "font", context.packageName)
            )
            Typeface.create(typeface ?: Typeface.DEFAULT, style)
        } catch (e: Exception) {
            Typeface.create(Typeface.DEFAULT, style)
        }
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Check if a key should use accent color
     */
    fun shouldUseAccentForKey(keyType: String): Boolean {
        return getCurrentPalette().shouldApplyAccentTo(keyType)
    }
    
    /**
     * Check if Enter key should use accent
     */
    fun shouldUseAccentForEnter(): Boolean {
        return getCurrentPalette().shouldUseAccentForEnter()
    }
    
    /**
     * Get contrast ratio for accessibility
     */
    fun getContrastRatio(foreground: Int, background: Int): Float {
        val fgLum = calculateLuminance(foreground)
        val bgLum = calculateLuminance(background)
        
        val lighter = maxOf(fgLum, bgLum)
        val darker = minOf(fgLum, bgLum)
        
        return (lighter + 0.05f) / (darker + 0.05f)
    }
    
    private fun calculateLuminance(color: Int): Float {
        val r = Color.red(color) / 255.0f
        val g = Color.green(color) / 255.0f
        val b = Color.blue(color) / 255.0f
        
        val rLum = if (r <= 0.03928f) r / 12.92f else ((r + 0.055f) / 1.055f).pow(2.4f)
        val gLum = if (g <= 0.03928f) g / 12.92f else ((g + 0.055f) / 1.055f).pow(2.4f)
        val bLum = if (b <= 0.03928f) b / 12.92f else ((b + 0.055f) / 1.055f).pow(2.4f)
        
        return 0.2126f * rLum + 0.7152f * gLum + 0.0722f * bLum
    }
    
    /**
     * Auto-adjust text color for better contrast
     */
    fun getContrastAdjustedTextColor(backgroundColor: Int): Int {
        val whiteContrast = getContrastRatio(Color.WHITE, backgroundColor)
        val blackContrast = getContrastRatio(Color.BLACK, backgroundColor)
        
        return if (whiteContrast > blackContrast) Color.WHITE else Color.BLACK
    }
    
    // ===== NEW ADAPTIVE & SEASONAL METHODS =====
    
    private fun buildAdaptiveBackground(): Drawable {
        val theme = getCurrentTheme()
        val adaptiveConfig = theme.background.adaptive
        
        return when (adaptiveConfig?.source) {
            "wallpaper" -> extractWallpaperColors()
            "system" -> extractSystemColors()
            else -> buildSolidDrawable(theme.background.color ?: Color.BLACK)
        }
    }
    
    private fun extractWallpaperColors(): Drawable {
        // For now, return a fallback. In production, this would extract dominant colors from wallpaper
        val theme = getCurrentTheme()
        return buildSolidDrawable(theme.background.color ?: Color.BLACK)
    }
    
    private fun extractSystemColors(): Drawable {
        // For now, return a fallback. In production, this would use Material You system colors
        val theme = getCurrentTheme()
        return buildSolidDrawable(theme.background.color ?: Color.BLACK)
    }
    
    private fun applySeasonalOverlay(baseDrawable: Drawable, seasonalPack: String): Drawable {
        // Create a layer drawable with the base and seasonal overlay
        val layerDrawable = LayerDrawable(arrayOf(
            baseDrawable,
            createSeasonalOverlay(seasonalPack)
        ))
        return layerDrawable
    }
    
    private fun createSeasonalOverlay(seasonalPack: String): Drawable {
        // For now, create a simple tinted overlay. In production, load seasonal resources
        val overlayColor = when (seasonalPack) {
            "valentine" -> Color.parseColor("#33FF6B9D") // Pink tint
            "halloween" -> Color.parseColor("#33FF8C00") // Orange tint
            "christmas" -> Color.parseColor("#3300FF00") // Green tint
            else -> Color.TRANSPARENT
        }
        
        val drawable = GradientDrawable()
        drawable.setColor(overlayColor)
        return drawable
    }
    
    private fun loadStickerDrawable(pack: String): Drawable? {
        // For now, return null. In production, load sticker assets from pack
        // This would load animated or static drawables based on pack name
        return try {
            // Example: context.getDrawable(R.drawable.sticker_pack_name)
            null
        } catch (e: Exception) {
            null
        }
    }
    
    fun cleanup() {
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        drawableCache.evictAll()
        imageCache.evictAll()
            listeners.clear()
    }
    
    // ===== LEGACY COMPATIBILITY =====
    
}