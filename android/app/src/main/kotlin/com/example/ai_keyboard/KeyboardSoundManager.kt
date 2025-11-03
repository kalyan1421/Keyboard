package com.example.ai_keyboard

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.annotation.RawRes

/**
 * ✅ LIGHTWEIGHT SOUNDPOOL MANAGER - Singleton Pattern
 * 
 * Replaces heavy MediaCodec audio pipeline with ultra-fast SoundPool.
 * This eliminates the c2.android.raw.decoder logs and reduces latency to ~2ms.
 * 
 * Usage:
 *   KeyboardSoundManager.init(context)
 *   KeyboardSoundManager.play()
 *   KeyboardSoundManager.release()
 */
object KeyboardSoundManager {

    private const val TAG = "KeyboardSoundManager"
    private const val MAX_STREAMS = 8
    
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()
    private var currentType = "classic"
    private var volume = 1.0f
    private var isInitialized = false

    /**
     * Initialize SoundPool and preload all sound effects.
     * Safe to call multiple times - will skip if already initialized.
     * 
     * @param context Application or service context
     */
    fun init(context: Context) {
        if (soundPool != null) {
            Log.d(TAG, "SoundPool already initialized, skipping")
            return
        }

        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(attrs)
                .build()

            // Preload all sound profiles
            load(context, "classic", R.raw.key_click)
            load(context, "pop", R.raw.key_pop)
            load(context, "mech", R.raw.key_mech)
            load(context, "bubble", R.raw.key_bubble)

            soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    Log.d(TAG, "✅ Sound loaded successfully: ID=$sampleId")
                    isInitialized = true
                } else {
                    Log.e(TAG, "❌ Sound load failed: ID=$sampleId, status=$status")
                }
            }

            Log.d(TAG, "✅ SoundPool initialized with ${soundMap.size} sounds")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize SoundPool", e)
        }
    }

    /**
     * Load a sound file into the pool.
     */
    private fun load(context: Context, key: String, @RawRes resId: Int) {
        try {
            val id = soundPool?.load(context, resId, 1)
            if (id != null && id > 0) {
                soundMap[key] = id
                Log.d(TAG, "Loaded sound: $key → ID=$id")
            } else {
                Log.w(TAG, "Failed to load sound: $key (invalid ID)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading sound: $key", e)
        }
    }

    /**
     * Play the currently selected sound with configured volume.
     * This is a LIGHTWEIGHT operation (~2ms latency).
     * NO MediaCodec initialization/teardown overhead.
     */
    fun play() {
        val pool = soundPool
        if (pool == null) {
            Log.w(TAG, "⚠️ Cannot play: SoundPool not initialized (call init() first)")
            return
        }

        if (!isInitialized) {
            Log.w(TAG, "⚠️ Cannot play: Sounds still loading")
            return
        }

        val soundId = soundMap[currentType] ?: soundMap["classic"]
        if (soundId == null) {
            Log.w(TAG, "⚠️ Cannot play: No sound ID for type '$currentType'")
            return
        }

        try {
            pool.play(soundId, volume, volume, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing sound", e)
        }
    }

    /**
     * Update sound type and volume without reinitializing the pool.
     * 
     * @param type Sound profile: "classic", "pop", "mech", "bubble", "silent"
     * @param vol Volume level (0.0 - 1.0)
     */
    fun update(type: String?, vol: Float?) {
        var silentProfile = false
        type?.let { profile ->
            if (profile.equals("silent", ignoreCase = true)) {
                currentType = "classic" // fallback ID
                volume = 0f
                silentProfile = true
                Log.d(TAG, "🔇 Silent sound profile applied")
            } else if (soundMap.containsKey(profile)) {
                currentType = profile
                Log.d(TAG, "🔊 Sound type updated: $profile")
            } else {
                Log.w(TAG, "⚠️ Unknown sound type: $profile (keeping '$currentType')")
            }
        }
        if (!silentProfile) {
            vol?.let { 
                volume = it.coerceIn(0f, 1f)
                Log.d(TAG, "🔊 Volume updated: $volume")
            }
        }
    }

    /**
     * Release the SoundPool and free all resources.
     * Call this in onDestroy().
     */
    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            soundMap.clear()
            isInitialized = false
            Log.d(TAG, "🔇 SoundPool released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing SoundPool", e)
        }
    }
}
