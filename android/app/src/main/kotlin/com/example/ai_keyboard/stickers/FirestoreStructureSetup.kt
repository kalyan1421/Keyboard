package com.example.ai_keyboard.stickers

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Setup script for Firebase Firestore structure for stickers
 * This should be run once to initialize the database structure
 */
class FirestoreStructureSetup {

    companion object {
        private const val TAG = "FirestoreStructureSetup"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /**
     * Initialize Firestore with sample sticker data
     * Call this method once to set up the database structure
     */
    suspend fun initializeFirestoreStructure(context: Context): Boolean {
        return try {
            Log.d(TAG, "🔥 Setting up Firestore structure for stickers...")
            
            // Create sample sticker packs
            val samplePacks = createSamplePacks()
            
            // Upload packs to Firestore
            samplePacks.forEach { pack ->
                val packData = mapOf(
                    "name" to pack.name,
                    "author" to pack.author,
                    "category" to pack.category,
                    "thumbnailUrl" to pack.thumbnailUrl,
                    "version" to pack.version,
                    "stickerCount" to pack.stickerCount,
                    "description" to pack.description,
                    "featured" to pack.featured,
                    "tags" to pack.tags
                )
                
                firestore.collection("sticker_packs")
                    .document(pack.id)
                    .set(packData)
                    .await()
                
                Log.d(TAG, "✅ Created pack: ${pack.name}")
                
                // Create sample stickers for each pack
                val sampleStickers = createSampleStickers(pack)
                sampleStickers.forEach { sticker ->
                    val stickerData = mapOf(
                        "imageUrl" to sticker.imageUrl,
                        "tags" to sticker.tags,
                        "emojis" to sticker.emojis,
                        "usageCount" to 0,
                        "lastUsed" to 0L
                    )
                    
                    firestore.collection("sticker_packs")
                        .document(pack.id)
                        .collection("stickers")
                        .document(sticker.id)
                        .set(stickerData)
                        .await()
                }
                
                Log.d(TAG, "✅ Created ${sampleStickers.size} stickers for pack: ${pack.name}")
            }
            
            Log.d(TAG, "🎉 Firestore structure setup completed successfully!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up Firestore structure", e)
            false
        }
    }

    /**
     * Create sample sticker packs
     */
    private fun createSamplePacks(): List<StickerPack> {
        return listOf(
            StickerPack(
                id = "cute_animals_v2",
                name = "Cute Animals",
                author = "AI Keyboard Team",
                thumbnailUrl = "gs://ai-keyboard-stickers/packs/cute_animals/thumb.png",
                category = "animals",
                version = "2.0",
                stickerCount = 8,
                description = "Adorable animal stickers for everyday conversations",
                featured = true,
                tags = listOf("animals", "cute", "pets", "popular")
            ),
            StickerPack(
                id = "business_pro_v2",
                name = "Business Pro",
                author = "AI Keyboard Team",
                thumbnailUrl = "gs://ai-keyboard-stickers/packs/business/thumb.png",
                category = "business",
                version = "2.0",
                stickerCount = 6,
                description = "Professional stickers for workplace communication",
                featured = false,
                tags = listOf("business", "professional", "work", "office")
            ),
            StickerPack(
                id = "emotions_express_v2",
                name = "Express Emotions",
                author = "AI Keyboard Team",
                thumbnailUrl = "gs://ai-keyboard-stickers/packs/emotions/thumb.png",
                category = "emotions",
                version = "2.0",
                stickerCount = 10,
                description = "Express your feelings with these emotion stickers",
                featured = true,
                tags = listOf("emotions", "feelings", "expressions", "mood")
            ),
            StickerPack(
                id = "food_love_v2",
                name = "Food Lover",
                author = "AI Keyboard Team", 
                thumbnailUrl = "gs://ai-keyboard-stickers/packs/food/thumb.png",
                category = "food",
                version = "2.0",
                stickerCount = 7,
                description = "Delicious food stickers for food enthusiasts",
                featured = false,
                tags = listOf("food", "delicious", "cooking", "restaurant")
            )
        )
    }

    /**
     * Create sample stickers for a pack
     */
    private fun createSampleStickers(pack: StickerPack): List<StickerData> {
        return when (pack.category) {
            "animals" -> listOf(
                StickerData(
                    id = "cat_happy",
                    packId = pack.id,
                    imageUrl = "gs://ai-keyboard-stickers/packs/cute_animals/cat_happy.png",
                    tags = listOf("cat", "happy", "smile", "cute"),
                    emojis = listOf("😸", "🐱")
                ),
                StickerData(
                    id = "dog_excited",
                    packId = pack.id,
                    imageUrl = "gs://ai-keyboard-stickers/packs/cute_animals/dog_excited.png",
                    tags = listOf("dog", "excited", "playful", "happy"),
                    emojis = listOf("🐶", "😄")
                ),
                StickerData(
                    id = "bunny_love",
                    packId = pack.id,
                    imageUrl = "gs://ai-keyboard-stickers/packs/cute_animals/bunny_love.png",
                    tags = listOf("bunny", "rabbit", "love", "heart"),
                    emojis = listOf("🐰", "❤️")
                ),
                StickerData(
                    id = "panda_sleepy",
                    packId = pack.id,
                    imageUrl = "gs://ai-keyboard-stickers/packs/cute_animals/panda_sleepy.png",
                    tags = listOf("panda", "sleepy", "tired", "rest"),
                    emojis = listOf("🐼", "😴")
                )
            )
            "emotions" -> listOf(
                StickerData(
                    id = "super_happy",
                    packId = pack.id,
                    imageUrl = "gs://ai-keyboard-stickers/packs/emotions/super_happy.png",
                    tags = listOf("happy", "excited", "joy", "celebration"),
                    emojis = listOf("😆", "🎉")
                ),
                StickerData(
                    id = "mind_blown",
                    packId = pack.id,
                    imageUrl = "gs://ai-keyboard-stickers/packs/emotions/mind_blown.png",
                    tags = listOf("shocked", "amazed", "wow", "incredible"),
                    emojis = listOf("🤯", "💥")
                ),
                StickerData(
                    id = "heart_eyes",
                    packId = pack.id,
                    imageUrl = "gs://ai-keyboard-stickers/packs/emotions/heart_eyes.png",
                    tags = listOf("love", "adore", "crush", "romantic"),
                    emojis = listOf("😍", "❤️")
                )
            )
            "business" -> listOf(
                StickerData(
                    id = "thumbs_up_pro",
                    packId = pack.id,
                    imageUrl = "gs://ai-keyboard-stickers/packs/business/thumbs_up.png",
                    tags = listOf("approval", "good", "success", "professional"),
                    emojis = listOf("👍", "💼")
                ),
                StickerData(
                    id = "handshake_deal",
                    packId = pack.id,
                    imageUrl = "gs://ai-keyboard-stickers/packs/business/handshake.png",
                    tags = listOf("handshake", "deal", "agreement", "partnership"),
                    emojis = listOf("🤝", "💼")
                )
            )
            "food" -> listOf(
                StickerData(
                    id = "pizza_slice",
                    packId = pack.id,
                    imageUrl = "gs://ai-keyboard-stickers/packs/food/pizza.png",
                    tags = listOf("pizza", "delicious", "italian", "yummy"),
                    emojis = listOf("🍕", "😋")
                ),
                StickerData(
                    id = "coffee_love",
                    packId = pack.id,
                    imageUrl = "gs://ai-keyboard-stickers/packs/food/coffee.png",
                    tags = listOf("coffee", "caffeine", "morning", "energy"),
                    emojis = listOf("☕", "❤️")
                )
            )
            else -> emptyList()
        }
    }

    /**
     * Test Firebase connection
     */
    suspend fun testFirebaseConnection(): Boolean {
        return try {
            Log.d(TAG, "🔍 Testing Firebase connection...")
            
            val testDoc = firestore.collection("test").document("connection")
            testDoc.set(mapOf("timestamp" to System.currentTimeMillis())).await()
            
            val result = testDoc.get().await()
            val success = result.exists()
            
            // Clean up test document
            testDoc.delete()
            
            Log.d(TAG, if (success) "✅ Firebase connection successful" else "❌ Firebase connection failed")
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase connection test failed", e)
            false
        }
    }

    /**
     * Get current Firestore structure info
     */
    suspend fun getFirestoreInfo(): FirestoreInfo {
        return try {
            val packsSnapshot = firestore.collection("sticker_packs").get().await()
            val packCount = packsSnapshot.documents.size
            
            var totalStickers = 0
            packsSnapshot.documents.forEach { packDoc ->
                val stickersSnapshot = packDoc.reference.collection("stickers").get().await()
                totalStickers += stickersSnapshot.documents.size
            }
            
            FirestoreInfo(
                connected = true,
                packCount = packCount,
                stickerCount = totalStickers,
                lastChecked = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Firestore info", e)
            FirestoreInfo(connected = false)
        }
    }
}

/**
 * Data class for Firestore information
 */
data class FirestoreInfo(
    val connected: Boolean,
    val packCount: Int = 0,
    val stickerCount: Int = 0,
    val lastChecked: Long = 0L
)

