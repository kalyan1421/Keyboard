// Quick fix: Use assets stickers as emoji fallbacks
// Add this to EmojiPanelController.kt in loadStickersFromPack method

private fun loadStickersFromPackWithAssetsFallback(packId: String) {
    coroutineScope.launch {
        try {
            val stickers = withContext(Dispatchers.IO) {
                stickerService.getStickersFromPack(packId)
            }
            
            currentStickers = stickers
            LogUtil.d(TAG, "Loaded ${stickers.size} stickers from pack $packId")
            
            // If no stickers from Firebase, use emoji fallbacks
            val stickerEmojis = if (stickers.isEmpty()) {
                // Fallback: Use emojis that represent stickers
                listOf(
                    "😸", // Happy cat
                    "🐶", // Happy dog  
                    "🐰", // Bunny
                    "🐼", // Panda
                    "🦊", // Fox
                    "🐻", // Bear
                    "👍", // Thumbs up
                    "🤝", // Handshake
                    "📈", // Chart
                    "😆", // Super happy
                    "🤯", // Mind blown
                    "😍", // Heart eyes
                    "🍕", // Pizza
                    "☕", // Coffee
                    "🍦"  // Ice cream
                )
            } else {
                stickers.map { sticker ->
                    if (sticker.emojis.isNotEmpty()) {
                        sticker.emojis.first()
                    } else {
                        "🖼️" // Placeholder
                    }
                }
            }
            
            // Create fake sticker data for emoji fallbacks
            if (stickers.isEmpty()) {
                currentStickers = stickerEmojis.mapIndexed { index, emoji ->
                    com.example.ai_keyboard.stickers.StickerData(
                        id = "fallback_$index",
                        packId = packId,
                        imageUrl = "emoji://$emoji",
                        emojis = listOf(emoji),
                        tags = listOf("emoji", "fallback")
                    )
                }
            }
            
            // Update the emoji grid
            updateEmojiGrid(stickerEmojis)
            setupStickerClickHandling()
            
        } catch (e: Exception) {
            LogUtil.e(TAG, "Error loading stickers from pack $packId", e)
            // Show fallback emojis on error
            val fallbackEmojis = listOf("😸", "🐶", "🐰", "🐼", "🦊")
            updateEmojiGrid(fallbackEmojis)
        }
    }
}
