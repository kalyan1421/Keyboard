package com.example.ai_keyboard.themes

import android.graphics.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * CleverType Theme Engine V2 - Core Models
 * Single source of truth for all keyboard theming
 * Replaces old ThemeManager with centralized JSON-based system
 */

data class KeyboardThemeV2(
    val id: String,
    val name: String,
    val mode: String, // "unified" or "split"
    val background: Background,
    val keys: Keys,
    val specialKeys: SpecialKeys,
    val effects: Effects,
    val sounds: Sounds,
    val stickers: Stickers,
    val advanced: Advanced
) {
    data class Background(
        val type: String, // "solid", "image", "gradient", "adaptive"
        val color: Int?,
        val imagePath: String?,
        val imageOpacity: Float,
        val gradient: Gradient?,
        val overlayEffects: List<String>,
        val adaptive: Adaptive?
    ) {
        data class Gradient(
            val colors: List<Int>, 
            val orientation: String // "TOP_BOTTOM", "TL_BR", etc.
        )
        
        data class Adaptive(
            val enabled: Boolean,
            val source: String, // "wallpaper", "system", "app"
            val materialYou: Boolean
        )
    }
    
    data class Keys(
        val preset: String, // "flat", "bordered", "floating", "3d", "transparent"
        val bg: Int,
        val text: Int, 
        val pressed: Int,
        val rippleAlpha: Float,
        val border: Border,
        val radius: Float,
        val shadow: Shadow,
        val font: Font
    ) {
        data class Border(
            val enabled: Boolean,
            val color: Int,
            val widthDp: Float
        )
        
        data class Shadow(
            val enabled: Boolean,
            val elevationDp: Float,
            val glow: Boolean
        )
        
        data class Font(
            val family: String,
            val sizeSp: Float,
            val bold: Boolean,
            val italic: Boolean
        )
    }

    data class SpecialKeys(
        val accent: Int,
        val useAccentForEnter: Boolean,
        val applyTo: List<String>, // ["enter", "shift", "globe", "emoji", "mic", etc.]
        val spaceLabelColor: Int
    )

    data class Effects(
        val pressAnimation: String, // "ripple", "bounce", "glow", "none"
        val globalEffects: List<String> // "snow", "hearts", "sparkles", "rain", "leaves"
    )

    data class Sounds(
        val pack: String, // "classic", "soft", "mechanical", "clicky", "silent", "custom"
        val customUris: Map<String, String>,
        val volume: Float
    )

    data class Stickers(
        val enabled: Boolean,
        val pack: String,
        val position: String, // "above", "below", "behind"
        val opacity: Float,
        val animated: Boolean
    )

    data class Advanced(
        val livePreview: Boolean,
        val galleryEnabled: Boolean,
        val shareEnabled: Boolean,
        val dynamicTheme: String, // "none", "wallpaper", "time_of_day", "seasonal"
        val seasonalPack: String, // "none", "valentine", "halloween", "christmas", "spring", "summer"
        val materialYouExtract: Boolean
    )

    companion object {
        /**
         * Parse theme from JSON with comprehensive defaults
         */
        fun fromJson(json: String): KeyboardThemeV2 {
            return try {
                val obj = JSONObject(json)
                parseFromJsonObject(obj)
            } catch (e: Exception) {
                createDefault()
            }
        }

        fun parseFromJsonObject(obj: JSONObject): KeyboardThemeV2 {
            return KeyboardThemeV2(
                id = obj.optString("id", "default_theme"),
                name = obj.optString("name", "Default Theme"),
                mode = obj.optString("mode", "unified"),
                background = parseBackground(obj.optJSONObject("background")),
                keys = parseKeys(obj.optJSONObject("keys")),
                specialKeys = parseSpecialKeys(obj.optJSONObject("specialKeys")),
                effects = parseEffects(obj.optJSONObject("effects")),
                sounds = parseSounds(obj.optJSONObject("sounds")),
                stickers = parseStickers(obj.optJSONObject("stickers")),
                advanced = parseAdvanced(obj.optJSONObject("advanced"))
            )
        }

        private fun parseBackground(obj: JSONObject?): Background {
            if (obj == null) return Background(
                type = "solid",
                color = Color.parseColor("#1B1B1F"),
                imagePath = null,
                imageOpacity = 0.85f,
                gradient = null,
                overlayEffects = emptyList(),
                adaptive = null
            )
            
            val gradient = obj.optJSONObject("gradient")?.let {
                Background.Gradient(
                    colors = parseColorArray(it.optJSONArray("colors")) ?: listOf(
                        Color.parseColor("#2B2B2B"),
                        Color.parseColor("#1B1B1F")
                    ),
                    orientation = it.optString("orientation", "TOP_BOTTOM")
                )
            }
            
            val adaptive = obj.optJSONObject("adaptive")?.let {
                Background.Adaptive(
                    enabled = it.optBoolean("enabled", false),
                    source = it.optString("source", "wallpaper"),
                    materialYou = it.optBoolean("materialYou", false)
                )
            }
            
            return Background(
                type = obj.optString("type", "solid"),
                color = parseColor(obj.optString("color", "#1B1B1F")),
                imagePath = obj.optString("imagePath", "").takeIf { it.isNotEmpty() },
                imageOpacity = obj.optDouble("imageOpacity", 0.85).toFloat(),
                gradient = gradient,
                overlayEffects = parseStringArray(obj.optJSONArray("overlayEffects")),
                adaptive = adaptive
            )
        }

        private fun parseKeys(obj: JSONObject?): Keys {
            if (obj == null) return createDefaultKeys()
            
            return Keys(
                preset = obj.optString("preset", "bordered"),
                bg = parseColor(obj.optString("bg", "#3A3A3F")),
                text = parseColor(obj.optString("text", "#FFFFFF")),
                pressed = parseColor(obj.optString("pressed", "#505056")),
                rippleAlpha = obj.optDouble("rippleAlpha", 0.12).toFloat(),
                border = parseBorder(obj.optJSONObject("border")),
                radius = obj.optDouble("radius", 10.0).toFloat(),
                shadow = parseShadow(obj.optJSONObject("shadow")),
                font = parseFont(obj.optJSONObject("font"))
            )
        }

        private fun parseBorder(obj: JSONObject?): Keys.Border {
            if (obj == null) return Keys.Border(
                enabled = true,
                color = Color.parseColor("#636366"),
                widthDp = 1.0f
            )
            
            return Keys.Border(
                enabled = obj.optBoolean("enabled", true),
                color = parseColor(obj.optString("color", "#636366")),
                widthDp = obj.optDouble("widthDp", 1.0).toFloat()
            )
        }

        private fun parseShadow(obj: JSONObject?): Keys.Shadow {
            if (obj == null) return Keys.Shadow(
                enabled = true,
                elevationDp = 2.0f,
                glow = false
            )
            
            return Keys.Shadow(
                enabled = obj.optBoolean("enabled", true),
                elevationDp = obj.optDouble("elevationDp", 2.0).toFloat(),
                glow = obj.optBoolean("glow", false)
            )
        }

        private fun parseFont(obj: JSONObject?): Keys.Font {
            if (obj == null) return Keys.Font(
                family = "Roboto",
                sizeSp = 18.0f,
                bold = false,
                italic = false
            )
            
            return Keys.Font(
                family = obj.optString("family", "Roboto"),
                sizeSp = obj.optDouble("sizeSp", 18.0).toFloat(),
                bold = obj.optBoolean("bold", false),
                italic = obj.optBoolean("italic", false)
            )
        }

        private fun parseSpecialKeys(obj: JSONObject?): SpecialKeys {
            if (obj == null) return SpecialKeys(
                accent = Color.parseColor("#FF9F1A"),
                useAccentForEnter = true,
                applyTo = listOf("enter", "globe", "emoji", "mic"),
                spaceLabelColor = Color.parseColor("#FFFFFF")
            )
            
            return SpecialKeys(
                accent = parseColor(obj.optString("accent", "#FF9F1A")),
                useAccentForEnter = obj.optBoolean("useAccentForEnter", true),
                applyTo = parseStringArray(obj.optJSONArray("applyTo")).takeIf { it.isNotEmpty() }
                    ?: listOf("enter", "globe", "emoji", "mic"),
                spaceLabelColor = parseColor(obj.optString("spaceLabelColor", "#FFFFFF"))
            )
        }

        private fun parseEffects(obj: JSONObject?): Effects {
            return Effects(
                pressAnimation = obj?.optString("pressAnimation", "ripple") ?: "ripple",
                globalEffects = parseStringArray(obj?.optJSONArray("globalEffects")) ?: emptyList()
            )
        }

        private fun parseSounds(obj: JSONObject?): Sounds {
            if (obj == null) return Sounds(
                pack = "soft",
                customUris = emptyMap(),
                volume = 0.6f
            )
            
            val customUris = mutableMapOf<String, String>()
            obj.optJSONObject("customUris")?.let { uris ->
                uris.keys().forEach { key ->
                    customUris[key] = uris.optString(key)
                }
            }
            
            return Sounds(
                pack = obj.optString("pack", "soft"),
                customUris = customUris,
                volume = obj.optDouble("volume", 0.6).toFloat()
            )
        }

        private fun parseStickers(obj: JSONObject?): Stickers {
            if (obj == null) return Stickers(
                enabled = false,
                pack = "",
                position = "behind",
                opacity = 0.9f,
                animated = false
            )
            
            return Stickers(
                enabled = obj.optBoolean("enabled", false),
                pack = obj.optString("pack", ""),
                position = obj.optString("position", "behind"),
                opacity = obj.optDouble("opacity", 0.9).toFloat(),
                animated = obj.optBoolean("animated", false)
            )
        }

        private fun parseAdvanced(obj: JSONObject?): Advanced {
            if (obj == null) return Advanced(
                livePreview = true,
                galleryEnabled = true,
                shareEnabled = true,
                dynamicTheme = "none",
                seasonalPack = "none",
                materialYouExtract = false
            )
            
            return Advanced(
                livePreview = obj.optBoolean("livePreview", true),
                galleryEnabled = obj.optBoolean("galleryEnabled", true),
                shareEnabled = obj.optBoolean("shareEnabled", true),
                dynamicTheme = obj.optString("dynamicTheme", "none"),
                seasonalPack = obj.optString("seasonalPack", "none"),
                materialYouExtract = obj.optBoolean("materialYouExtract", false)
            )
        }

        // Utility functions
        private fun parseColor(colorStr: String?): Int {
            return try {
                if (colorStr.isNullOrEmpty()) Color.TRANSPARENT
                else Color.parseColor(colorStr)
            } catch (e: Exception) {
                Color.TRANSPARENT
            }
        }

        private fun parseColorArray(array: JSONArray?): List<Int>? {
            if (array == null) return null
            val colors = mutableListOf<Int>()
            for (i in 0 until array.length()) {
                colors.add(parseColor(array.optString(i)))
            }
            return colors.takeIf { it.isNotEmpty() }
        }

        private fun parseStringArray(array: JSONArray?): List<String> {
            if (array == null) return emptyList()
            val strings = mutableListOf<String>()
            for (i in 0 until array.length()) {
                strings.add(array.optString(i))
            }
            return strings
        }

        // Default theme creators
        fun createDefault(): KeyboardThemeV2 {
            return KeyboardThemeV2(
                id = "default_theme",
                name = "Default Dark",
                mode = "unified",
                background = Background(
                    type = "solid",
                    color = Color.parseColor("#1B1B1F"),
                    imagePath = null,
                    imageOpacity = 0.85f,
                    gradient = null,
                    overlayEffects = emptyList(),
                    adaptive = null
                ),
                keys = createDefaultKeys(),
                specialKeys = SpecialKeys(
                    accent = Color.parseColor("#FF9F1A"),
                    useAccentForEnter = true,
                    applyTo = listOf("enter", "globe", "emoji", "mic"),
                    spaceLabelColor = Color.parseColor("#FFFFFF")
                ),
                effects = Effects(
                    pressAnimation = "ripple",
                    globalEffects = emptyList()
                ),
                sounds = Sounds(
                    pack = "soft",
                    customUris = emptyMap(),
                    volume = 0.6f
                ),
                stickers = Stickers(
                    enabled = false,
                    pack = "",
                    position = "behind",
                    opacity = 0.9f,
                    animated = false
                ),
                advanced = Advanced(
                    livePreview = true,
                    galleryEnabled = true,
                    shareEnabled = true,
                    dynamicTheme = "none",
                    seasonalPack = "none",
                    materialYouExtract = false
                )
            )
        }

        private fun createDefaultKeys(): Keys {
            return Keys(
                preset = "bordered",
                bg = Color.parseColor("#3A3A3F"),
                text = Color.parseColor("#FFFFFF"),
                pressed = Color.parseColor("#505056"),
                rippleAlpha = 0.12f,
                border = Keys.Border(
                    enabled = true,
                    color = Color.parseColor("#636366"),
                    widthDp = 1.0f
                ),
                radius = 10.0f,
                shadow = Keys.Shadow(
                    enabled = true,
                    elevationDp = 2.0f,
                    glow = false
                ),
                font = Keys.Font(
                    family = "Roboto",
                    sizeSp = 18.0f,
                    bold = false,
                    italic = false
                )
            )
        }
    }

    /**
     * Convert theme to JSON string
     */
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("mode", mode)
        
        // Background
        val bgObj = JSONObject()
        bgObj.put("type", background.type)
        background.color?.let { bgObj.put("color", String.format("#%06X", 0xFFFFFF and it)) }
        background.imagePath?.let { bgObj.put("imagePath", it) }
        bgObj.put("imageOpacity", background.imageOpacity)
        background.gradient?.let { gradient ->
            val gradObj = JSONObject()
            val colorsArray = JSONArray()
            gradient.colors.forEach { color ->
                colorsArray.put(String.format("#%06X", 0xFFFFFF and color))
            }
            gradObj.put("colors", colorsArray)
            gradObj.put("orientation", gradient.orientation)
            bgObj.put("gradient", gradObj)
        }
        if (background.overlayEffects.isNotEmpty()) {
            val effectsArray = JSONArray()
            background.overlayEffects.forEach { effectsArray.put(it) }
            bgObj.put("overlayEffects", effectsArray)
        }
        background.adaptive?.let { adaptive ->
            val adaptiveObj = JSONObject()
            adaptiveObj.put("enabled", adaptive.enabled)
            adaptiveObj.put("source", adaptive.source)
            adaptiveObj.put("materialYou", adaptive.materialYou)
            bgObj.put("adaptive", adaptiveObj)
        }
        obj.put("background", bgObj)
        
        // Keys
        val keysObj = JSONObject()
        keysObj.put("preset", keys.preset)
        keysObj.put("bg", String.format("#%06X", 0xFFFFFF and keys.bg))
        keysObj.put("text", String.format("#%06X", 0xFFFFFF and keys.text))
        keysObj.put("pressed", String.format("#%06X", 0xFFFFFF and keys.pressed))
        keysObj.put("rippleAlpha", keys.rippleAlpha)
        keysObj.put("radius", keys.radius)
        
        // Keys border
        val borderObj = JSONObject()
        borderObj.put("enabled", keys.border.enabled)
        borderObj.put("color", String.format("#%06X", 0xFFFFFF and keys.border.color))
        borderObj.put("widthDp", keys.border.widthDp)
        keysObj.put("border", borderObj)
        
        // Keys shadow
        val shadowObj = JSONObject()
        shadowObj.put("enabled", keys.shadow.enabled)
        shadowObj.put("elevationDp", keys.shadow.elevationDp)
        shadowObj.put("glow", keys.shadow.glow)
        keysObj.put("shadow", shadowObj)
        
        // Keys font
        val fontObj = JSONObject()
        fontObj.put("family", keys.font.family)
        fontObj.put("sizeSp", keys.font.sizeSp)
        fontObj.put("bold", keys.font.bold)
        fontObj.put("italic", keys.font.italic)
        keysObj.put("font", fontObj)
        
        obj.put("keys", keysObj)
        
        // Special Keys
        val specialObj = JSONObject()
        specialObj.put("accent", String.format("#%06X", 0xFFFFFF and specialKeys.accent))
        specialObj.put("useAccentForEnter", specialKeys.useAccentForEnter)
        val applyToArray = JSONArray()
        specialKeys.applyTo.forEach { applyToArray.put(it) }
        specialObj.put("applyTo", applyToArray)
        specialObj.put("spaceLabelColor", String.format("#%06X", 0xFFFFFF and specialKeys.spaceLabelColor))
        obj.put("specialKeys", specialObj)
        
        // Effects
        val effectsObj = JSONObject()
        effectsObj.put("pressAnimation", effects.pressAnimation)
        if (effects.globalEffects.isNotEmpty()) {
            val globalEffectsArray = JSONArray()
            effects.globalEffects.forEach { globalEffectsArray.put(it) }
            effectsObj.put("globalEffects", globalEffectsArray)
        }
        obj.put("effects", effectsObj)
        
        // Sounds
        val soundsObj = JSONObject()
        soundsObj.put("pack", sounds.pack)
        soundsObj.put("volume", sounds.volume)
        val customUrisObj = JSONObject()
        sounds.customUris.forEach { (key, value) ->
            customUrisObj.put(key, value)
        }
        soundsObj.put("customUris", customUrisObj)
        obj.put("sounds", soundsObj)
        
        // Stickers
        val stickersObj = JSONObject()
        stickersObj.put("enabled", stickers.enabled)
        stickersObj.put("pack", stickers.pack)
        stickersObj.put("position", stickers.position)
        stickersObj.put("opacity", stickers.opacity)
        stickersObj.put("animated", stickers.animated)
        obj.put("stickers", stickersObj)
        
        // Advanced
        val advancedObj = JSONObject()
        advancedObj.put("livePreview", advanced.livePreview)
        advancedObj.put("galleryEnabled", advanced.galleryEnabled)
        advancedObj.put("shareEnabled", advanced.shareEnabled)
        advancedObj.put("dynamicTheme", advanced.dynamicTheme)
        advancedObj.put("seasonalPack", advanced.seasonalPack)
        advancedObj.put("materialYouExtract", advanced.materialYouExtract)
        obj.put("advanced", advancedObj)
        
        return obj.toString(2)
    }
}

/**
 * Unified theme palette - derived colors for all UI elements
 * Single source of truth for runtime theming
 */
data class ThemePaletteV2(
    val theme: KeyboardThemeV2
) {
    // Resolved colors based on inheritance rules
    val keyboardBg: Int = resolveKeyboardBackground()
    
    // Adaptive and seasonal features
    val isAdaptive: Boolean = theme.background.type == "adaptive" && theme.background.adaptive?.enabled == true
    val adaptiveSource: String = theme.background.adaptive?.source ?: "wallpaper"
    val isMaterialYou: Boolean = theme.background.adaptive?.materialYou == true
    val isSeasonalActive: Boolean = theme.advanced.seasonalPack != "none"
    val currentSeasonalPack: String = theme.advanced.seasonalPack
    val hasGlobalEffects: Boolean = theme.effects.globalEffects.isNotEmpty()
    val globalEffects: List<String> = theme.effects.globalEffects
    val hasStickers: Boolean = theme.stickers.enabled
    val stickerOpacity: Float = theme.stickers.opacity
    
    private fun resolveKeyboardBackground(): Int {
        return when (theme.background.type) {
            "adaptive" -> {
                // Will be resolved at runtime based on system colors
                theme.background.color ?: Color.parseColor("#1B1B1F")
            }
            else -> theme.background.color ?: Color.parseColor("#1B1B1F")
        }
    }
    
    // Keys - primary theme source
    val keyBg: Int = theme.keys.bg
    val keyText: Int = theme.keys.text
    val keyPressed: Int = theme.keys.pressed
    val keyBorder: Int = theme.keys.border.color
    val keyRadius: Float = theme.keys.radius
    val keyShadowEnabled: Boolean = theme.keys.shadow.enabled
    
    val specialAccent: Int = theme.specialKeys.accent
    val spaceLabelColor: Int = theme.specialKeys.spaceLabelColor
    
    // Toolbar & Suggestion Bar: Always match keyboard background (SIMPLIFIED)
    val toolbarBg: Int = keyboardBg
    val suggestionBg: Int = keyboardBg
    
    // Toolbar icons: Use PNGs directly (no tint applied in code)
    val toolbarIcon: Int? = null  // null = no tint
    val toolbarHeight: Float = 44f // dp baseline
    val toolbarActiveAccent: Int = theme.specialKeys.accent

    // Suggestion text: Auto-contrast from background (SIMPLIFIED: no chips)
    val suggestionText: Int = getContrastColor(keyboardBg)
    
    // Font properties
    val keyFontSize: Float = theme.keys.font.sizeSp
    val keyFontFamily: String = theme.keys.font.family
    val keyFontBold: Boolean = theme.keys.font.bold
    val keyFontItalic: Boolean = theme.keys.font.italic
    
    val suggestionFontSize: Float = 15.0f // Fixed size for suggestions
    val suggestionFontFamily: String = theme.keys.font.family // Inherit from keys
    val suggestionFontBold: Boolean = false
    
    // Key styling properties - needed by ThemeManager
    val keyBorderEnabled: Boolean = theme.keys.border.enabled
    val keyBorderColor: Int = theme.keys.border.color
    val keyBorderWidth: Float = theme.keys.border.widthDp
    val keyShadowElevation: Float = theme.keys.shadow.elevationDp
    val keyShadowGlow: Boolean = theme.keys.shadow.glow
    
    // Effects
    val pressAnimation: String = theme.effects.pressAnimation
    val rippleAlpha: Float = theme.keys.rippleAlpha
    
    // Special key rules
    fun shouldApplyAccentTo(keyType: String): Boolean {
        return theme.specialKeys.applyTo.contains(keyType)
    }
    
    fun shouldUseAccentForEnter(): Boolean {
        return theme.specialKeys.useAccentForEnter
    }

    // Helper: Auto-contrast color (black or white based on background luminance)
    private fun getContrastColor(bgColor: Int): Int {
        val r = Color.red(bgColor)
        val g = Color.green(bgColor)
        val b = Color.blue(bgColor)
        
        // Calculate perceived luminance (ITU-R BT.709)
        val luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
        
        // Return white for dark backgrounds, black for light backgrounds
        return if (luminance < 0.5) Color.WHITE else Color.BLACK
    }

    // Helper: Lighten or darken a color
    private fun lightenOrDarken(color: Int, delta: Float): Int {
        // delta > 0 → lighten, < 0 → darken
        // Used to create subtle variations from background color
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        fun adj(c: Int) = (c + (255 - c) * delta).coerceIn(0f, 255f).toInt()
        return Color.argb(a, adj(r), adj(g), adj(b))
    }
}
