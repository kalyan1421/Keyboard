package com.example.ai_keyboard

import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject
import com.example.ai_keyboard.themes.KeyboardThemeV2
import android.graphics.Color

/**
 * Test theme V2 migration and validation
 */
class ThemeV2MigrationTest {

    @Test
    fun testThemeV2Creation() {
        val theme = KeyboardThemeV2.createDefault()
        
        assertEquals("default_theme", theme.id)
        assertEquals("Default Dark", theme.name)
        assertEquals("unified", theme.mode)
        assertNotNull(theme.background)
        assertNotNull(theme.keys)
        assertNotNull(theme.specialKeys)
    }

    @Test
    fun testThemeV2JsonSerialization() {
        val originalTheme = KeyboardThemeV2.createDefault()
        val json = originalTheme.toJson()
        val parsedTheme = KeyboardThemeV2.fromJson(json)
        
        assertEquals(originalTheme.id, parsedTheme.id)
        assertEquals(originalTheme.name, parsedTheme.name)
        assertEquals(originalTheme.mode, parsedTheme.mode)
    }

    @Test
    fun testInvalidJsonHandling() {
        // Test with empty JSON
        val emptyTheme = KeyboardThemeV2.fromJson("{}")
        assertEquals("default_theme", emptyTheme.id)
        
        // Test with malformed JSON
        val malformedTheme = KeyboardThemeV2.fromJson("invalid json")
        assertEquals("default_theme", malformedTheme.id)
    }

    @Test
    fun testColorParsing() {
        val testJson = """
        {
            "id": "test",
            "name": "Test Theme",
            "mode": "unified",
            "keys": {
                "bg": "#FF0000",
                "text": "#FFFFFF",
                "pressed": "#AA0000"
            }
        }
        """.trimIndent()
        
        val theme = KeyboardThemeV2.fromJson(testJson)
        assertEquals(Color.parseColor("#FF0000"), theme.keys.bg)
        assertEquals(Color.parseColor("#FFFFFF"), theme.keys.text)
        assertEquals(Color.parseColor("#AA0000"), theme.keys.pressed)
    }

    @Test
    fun testThemeInheritanceLogic() {
        val theme = KeyboardThemeV2.createDefault()
        
        // Test unified mode - toolbar should inherit from keys
        assertTrue(theme.toolbar.inheritFromKeys)
        assertTrue(theme.suggestions.inheritFromKeys)
        
        // In unified mode, effective colors should be the same
        assertEquals("unified", theme.mode)
    }

    @Test
    fun testSpecialKeyConfiguration() {
        val theme = KeyboardThemeV2.createDefault()
        
        assertTrue(theme.specialKeys.useAccentForEnter)
        assertTrue(theme.specialKeys.applyTo.contains("enter"))
        assertTrue(theme.specialKeys.applyTo.contains("globe"))
        assertTrue(theme.specialKeys.applyTo.contains("emoji"))
        assertTrue(theme.specialKeys.applyTo.contains("mic"))
    }

    @Test
    fun testThemeDefaultsComplete() {
        val theme = KeyboardThemeV2.createDefault()
        
        // Verify all required fields have valid defaults
        assertTrue(theme.id.isNotEmpty())
        assertTrue(theme.name.isNotEmpty())
        assertTrue(theme.mode in listOf("unified", "split"))
        
        // Verify background has valid type
        assertTrue(theme.background.type in listOf("solid", "image", "gradient"))
        
        // Verify keys have all required properties
        assertTrue(theme.keys.preset.isNotEmpty())
        assertTrue(theme.keys.radius >= 0)
        assertTrue(theme.keys.rippleAlpha in 0.0..1.0)
        
        // Verify sounds have valid volume
        assertTrue(theme.sounds.volume in 0.0..1.0)
    }

    @Test
    fun testAdvancedFeaturesDefaults() {
        val theme = KeyboardThemeV2.createDefault()
        
        assertTrue(theme.advanced.livePreview)
        assertTrue(theme.advanced.galleryEnabled)
        assertTrue(theme.advanced.shareEnabled)
        assertEquals("none", theme.advanced.dynamicTheme)
        assertFalse(theme.advanced.materialYouExtract)
    }

    @Test
    fun testEffectsConfiguration() {
        val theme = KeyboardThemeV2.createDefault()
        
        assertEquals("ripple", theme.effects.pressAnimation)
        assertTrue(theme.effects.pressAnimation in listOf("ripple", "bounce", "glow", "none"))
    }

    @Test
    fun testLegacyMigration() {
        // Simulate old theme data
        val oldThemeJson = """
        {
            "id": "legacy_theme",
            "name": "Legacy Theme",
            "backgroundColor": "#1B1B1F",
            "keyBackgroundColor": "#3A3A3F",
            "keyTextColor": "#FFFFFF",
            "accentColor": "#FF9F1A"
        }
        """.trimIndent()
        
        // Test that old format doesn't break the parser
        val theme = KeyboardThemeV2.fromJson(oldThemeJson)
        assertEquals("legacy_theme", theme.id)
        assertEquals("Legacy Theme", theme.name)
        
        // Should get defaults for missing V2 fields
        assertEquals("unified", theme.mode)
        assertNotNull(theme.background)
        assertNotNull(theme.keys)
    }
}
