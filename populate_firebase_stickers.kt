// Add this method to populate Firebase with actual sticker data
// Run this once in your MainActivity to populate the database

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun populateFirebaseStickers() {
    val firestore = FirebaseFirestore.getInstance()
    
    CoroutineScope(Dispatchers.Main).launch {
        try {
            // Add stickers to the existing pack
            val stickers = listOf(
                mapOf(
                    "imageUrl" to "https://example.com/cat.png",
                    "tags" to listOf("cat", "happy", "cute"),
                    "emojis" to listOf("😸", "🐱"),
                    "usageCount" to 0,
                    "lastUsed" to 0L
                ),
                mapOf(
                    "imageUrl" to "https://example.com/dog.png", 
                    "tags" to listOf("dog", "excited", "happy"),
                    "emojis" to listOf("🐶", "😄"),
                    "usageCount" to 0,
                    "lastUsed" to 0L
                ),
                mapOf(
                    "imageUrl" to "https://example.com/panda.png",
                    "tags" to listOf("panda", "sleepy", "cute"),
                    "emojis" to listOf("🐼", "😴"),
                    "usageCount" to 0,
                    "lastUsed" to 0L
                )
            )
            
            // Add each sticker to the pack
            stickers.forEachIndexed { index, stickerData ->
                firestore.collection("sticker_packs")
                    .document("funny_animals")
                    .collection("stickers")
                    .document("sticker_$index")
                    .set(stickerData)
                    .await()
            }
            
            // Update pack with sticker count
            firestore.collection("sticker_packs")
                .document("funny_animals")
                .update("stickerCount", stickers.size)
                .await()
                
            Log.d("Firebase", "✅ Added ${stickers.size} stickers to funny_animals pack")
            
        } catch (e: Exception) {
            Log.e("Firebase", "❌ Error populating stickers: ${e.message}")
        }
    }
}
