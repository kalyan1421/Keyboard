package com.example.ai_keyboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import android.widget.*
import android.text.TextUtils
import androidx.core.graphics.ColorUtils
import androidx.core.widget.ImageViewCompat
import androidx.core.content.ContextCompat
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.os.SystemClock
import com.example.ai_keyboard.themes.PanelTheme
import com.example.ai_keyboard.themes.ThemePaletteV2
import com.example.ai_keyboard.utils.LogUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Unified Panel Manager V2 - 100% Dynamic, Zero XML
 * Single Source of Truth for All Panels
 * Consolidates: Grammar, Tone, AI Assistant, Emoji, Clipboard, and Settings
 * 
 * Key Features:
 * - Pure programmatic UI (no XML layouts)
 * - Lazy loading of panels (only create when needed)
 * - Consistent theming via ThemeManager
 * - Unified height management via KeyboardHeightManager
 * - Dynamic panel switching with smooth transitions
 * - Broadcast integration for theme/prompt updates
 * - Theme change listener for live rebuilds
 */
class UnifiedPanelManager(
    private val context: Context,
    private val themeManager: ThemeManager,
    private val keyboardHeightManager: KeyboardHeightManager? = null,
    private val inputConnectionProvider: () -> InputConnection? = { null },
    private val onBackToKeyboard: () -> Unit = {}
) {
    
    companion object {
        private const val TAG = "UnifiedPanelManager"
        private const val PANEL_HEIGHT_DP = 280
    }
    
    /**
     * Panel types supported by UnifiedPanelManager
     */
    enum class PanelType {
        GRAMMAR,
        TONE,
        AI_ASSISTANT,
        TRANSLATE,
        CLIPBOARD,
        EMOJI,
        SETTINGS
    }

    private data class LanguageOption(val label: String, val value: String)

    private data class LanguageConfig(
        val options: List<LanguageOption>,
        var current: LanguageOption,
        val onChanged: (LanguageOption) -> Unit
    )

    private data class GrammarResultViews(
        val container: LinearLayout,
        val primary: TextView,
        val translation: TextView,
        val replaceButton: Button
    )

    private data class ToneResultViews(
        val container: LinearLayout,
        val primary: TextView,
        val translation: TextView
    )

    private enum class QuickSettingType {
        ACTION,
        TOGGLE
    }

    private data class QuickSettingItem(
        val id: String,
        val label: String,
        @DrawableRes val iconRes: Int,
        val type: QuickSettingType,
        var isActive: Boolean = false,
        val handler: (QuickSettingItem) -> Unit
    )

    private enum class GrammarAction(
        val label: String,
        val description: String,
        val feature: AdvancedAIService.ProcessingFeature? = null,
        val customPrompt: String? = null
    ) {
        REPHRASE(
            label = "Rephrase",
            description = "Say the same thing more naturally",
            customPrompt = "Rewrite this text so it sounds natural, clear, and friendly. Preserve the original meaning."
        ),
        GRAMMAR_FIX(
            label = "Grammar Fix",
            description = "Fix Grammar of the sentence",
            feature = AdvancedAIService.ProcessingFeature.GRAMMAR_FIX
        ),
        ADD_EMOJIS(
            label = "Add emojis",
            description = "Add relevant emojis",
            customPrompt = "Rewrite this text and add fitting emojis while keeping the original intent. Return only the new text."
        )
    }

    private enum class ToneAction(val label: String, val prompt: String) {
        FUNNY("Funny", "Create playful and funny rewrites"),
        POETIC("Poetic", "Make lyrical, poetic versions with imagery"),
        SHORTEN("Shorten", "Condense the message but keep the core meaning"),
        SARCASTIC("Sarcastic", "Use witty, sarcastic humor while staying light-hearted")
    }
    
    // ✅ REFACTORED: No container management - caller handles display
    private var currentPanelType: PanelType? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val unifiedAIService = UnifiedAIService(context)
    
    // Panel views (lazy initialization)
    private var grammarPanelView: View? = null
    private var tonePanelView: View? = null
    private var aiAssistantPanelView: View? = null
    private var translatePanelView: View? = null
    private var clipboardPanelView: View? = null
    private var emojiPanelController: EmojiPanelController? = null
    private var emojiPanelView: View? = null
    private var settingsPanelView: View? = null
    private var emojiSearchModeListener: ((Boolean) -> Unit)? = null
    
    // Services
    private val advancedAIService = AdvancedAIService(context)
    
    // Callbacks
    private var onTextProcessedCallback: ((String) -> Unit)? = null
    
    // Input text for AI processing
    private var currentInputText: String = ""
    private var grammarHeroView: View? = null
    private var toneHeroView: View? = null
    private var grammarResultCard: GrammarResultViews? = null
    private var grammarDescriptionView: TextView? = null
    private var grammarShimmerContainer: LinearLayout? = null
    private var toneResultCards: List<ToneResultViews> = emptyList()
    private var toneShimmerContainer: LinearLayout? = null
    private var aiAssistantStatusText: TextView? = null
    private var aiAssistantResultContainer: LinearLayout? = null
    private var aiAssistantReplaceButton: Button? = null
    private var aiAssistantEmptyState: View? = null
    private var aiAssistantShimmerContainer: LinearLayout? = null
    private var aiAssistantChipRow: LinearLayout? = null
    private var aiAssistantReplyToneRow: LinearLayout? = null
    private var grammarChipScroll: HorizontalScrollView? = null
    private var toneChipScroll: HorizontalScrollView? = null
    private var aiChipScroll: HorizontalScrollView? = null
    private var aiAssistantPrompts: List<PromptManager.PromptItem> = emptyList()
    private var aiAssistantChipViews: MutableList<TextView> = mutableListOf()
    private var aiSelectedReplyTone: String = "Positive"
    private var lastAiResult: String = ""
    private var grammarEmptyState: View? = null
    private var toneEmptyState: View? = null
    private var grammarKeyboardButton: View? = null
    private var toneKeyboardButton: View? = null
    private var aiKeyboardButton: View? = null
    private val grammarChipViews: MutableList<TextView> = mutableListOf()
    private val toneChipViews: MutableList<TextView> = mutableListOf()
    private val grammarChipActions = mutableMapOf<TextView, () -> Unit>()
    private val toneChipActions = mutableMapOf<TextView, () -> Unit>()
    private var selectedGrammarChip: TextView? = null
    private var selectedToneChip: TextView? = null
    
    private val supportedLanguages = listOf(
        LanguageOption("English", "English"),
        LanguageOption("Hindi", "Hindi"),
        LanguageOption("Telugu", "Telugu"),
        LanguageOption("Spanish", "Spanish"),
        LanguageOption("French", "French"),
        LanguageOption("German", "German"),
        LanguageOption("Japanese", "Japanese")
    )
    private var grammarLanguage = supportedLanguages.first()
    private var toneLanguage = supportedLanguages.first()
    private var grammarTranslationJob: Job? = null
    private val toneTranslationJobs = mutableListOf<Job>()
    private var lastGrammarResult: String = ""
    private val lastToneResults = mutableListOf<String>()
    private var lastGrammarRequestedInput: String = ""
    private var lastToneRequestedInput: String = ""
    
    init {
        // Register theme change listener to rebuild panels dynamically
        themeManager.addThemeChangeListener(object : ThemeManager.ThemeChangeListener {
            override fun onThemeChanged(theme: com.example.ai_keyboard.themes.KeyboardThemeV2, palette: com.example.ai_keyboard.themes.ThemePaletteV2) {
                LogUtil.d(TAG, "Theme changed - rebuilding panels")
                rebuildDynamicPanelsFromPrompts()
            }
        })
    }
    
    /**
     * ✅ NEW API: Build panel view (caller manages display)
     * Returns a panel view ready to be displayed by UnifiedKeyboardView
     * 
     * @param type The panel type to build
     * @return Panel view ready for display
     */
    fun buildPanel(type: PanelType): View {
        LogUtil.d(TAG, "Building panel: $type")
        
        currentPanelType = type
        
        // Get or create the requested panel
        val panelView = when (type) {
            PanelType.GRAMMAR -> getOrCreateGrammarPanel()
            PanelType.TONE -> getOrCreateTonePanel()
            PanelType.AI_ASSISTANT -> getOrCreateAIAssistantPanel()
            PanelType.TRANSLATE -> getOrCreateTranslatePanel()
            PanelType.CLIPBOARD -> getOrCreateClipboardPanel()
            PanelType.EMOJI -> getOrCreateEmojiPanel()
            PanelType.SETTINGS -> getOrCreateSettingsPanel()
        }
        
        LogUtil.d(TAG, "✅ Panel $type built successfully")
        return panelView
    }
    
    /**
     * Check if a panel is currently visible
     */
    fun isPanelVisible(): Boolean {
        return currentPanelType != null
    }
    
    fun isEmojiSearchMode(): Boolean {
        return emojiPanelController?.isInSearchMode() == true
    }
    
    fun resetEmojiPanelState() {
        emojiPanelController?.resetToNormalMode()
    }
    
    fun setEmojiSearchModeListener(listener: (Boolean) -> Unit) {
        emojiSearchModeListener = listener
    }

    fun appendEmojiSearchCharacter(char: Char) {
        emojiPanelController?.appendToSearchQuery(char)
    }

    fun removeEmojiSearchCharacter() {
        emojiPanelController?.removeLastFromSearchQuery()
    }

    fun clearEmojiSearch() {
        emojiPanelController?.clearSearchQuery()
    }
    
    /**
     * Get current panel type
     */
    fun getCurrentPanelType(): PanelType? = currentPanelType
    
    /**
     * Refresh AI prompts (called when broadcast received)
     */
    fun refreshAIPrompts() {
        LogUtil.d(TAG, "Refreshing AI prompts - rebuilding dynamic panels")
        rebuildDynamicPanelsFromPrompts()
    }
    
    /**
     * Rebuild all dynamic panels from scratch (for theme changes and prompt updates)
     * ✅ REFACTORED: Clears cached panels - caller must re-request panel if needed
     */
    fun rebuildDynamicPanelsFromPrompts() {
        // Clear cached panels (will be rebuilt on next buildPanel() call)
        grammarPanelView = null
        tonePanelView = null
        aiAssistantPanelView = null
        translatePanelView = null
        clipboardPanelView = null
        settingsPanelView = null
        grammarHeroView = null
        toneHeroView = null
        grammarResultCard = null
        grammarDescriptionView = null
        toneResultCards = emptyList()
        grammarTranslationJob?.cancel()
        grammarTranslationJob = null
        toneTranslationJobs.forEach { it.cancel() }
        toneTranslationJobs.clear()
        lastGrammarResult = ""
        lastToneResults.clear()
        aiAssistantStatusText = null
        aiAssistantResultContainer = null
        aiAssistantReplaceButton = null
        aiAssistantEmptyState = null
        aiAssistantShimmerContainer = null
        aiAssistantChipRow = null
        aiAssistantReplyToneRow = null
        aiAssistantPrompts = emptyList()
        aiAssistantChipViews.clear()
        aiSelectedReplyTone = "Positive"
        lastAiResult = ""
        grammarEmptyState = null
        toneEmptyState = null
        grammarKeyboardButton = null
        toneKeyboardButton = null
        aiKeyboardButton = null
        lastGrammarRequestedInput = ""
        lastToneRequestedInput = ""
        grammarChipViews.clear()
        toneChipViews.clear()
        grammarChipActions.clear()
        toneChipActions.clear()
        selectedGrammarChip = null
        selectedToneChip = null
        
        // Rebuild emoji panel controller if it exists
        emojiPanelController?.applyTheme()
        
        // ✅ Note: Caller (UnifiedKeyboardView) will need to rebuild current panel if visible
        
        LogUtil.d(TAG, "✅ Dynamic panels rebuilt (cached cleared)")
    }
    
    /**
     * Apply theme to panel (called when theme changes)
     */
    fun applyTheme(theme: com.example.ai_keyboard.themes.KeyboardThemeV2) {
        LogUtil.d(TAG, "Applying theme to all panels...")
        rebuildDynamicPanelsFromPrompts()
        emojiPanelController?.applyTheme()
        LogUtil.d(TAG, "✅ Theme applied to panels")
    }
    
    /**
     * Set input text for AI panels
     */
    fun setInputText(text: String) {
        currentInputText = text
        updateTextDependentPanels()
        LogUtil.d(TAG, "Input text set: ${text.take(50)}...")
    }

    private fun updateTextDependentPanels() {
        val hasInput = currentInputText.trim().isNotEmpty()

        // Update AI Assistant state
        updateAIAssistantState()

        if (!hasInput) {
            grammarEmptyState?.visibility = View.VISIBLE
            grammarChipScroll?.visibility = View.GONE
            grammarDescriptionView?.visibility = View.GONE
            grammarHeroView?.visibility = View.GONE
            grammarResultCard?.let { card ->
                card.container.visibility = View.GONE
                card.primary.text = context.getString(R.string.panel_prompt_grammar)
                card.translation.visibility = View.GONE
                card.translation.text = ""
                card.replaceButton.isEnabled = false
            }
            grammarShimmerContainer?.clearAnimation()
            grammarShimmerContainer?.visibility = View.GONE
            grammarKeyboardButton?.visibility = View.GONE
            grammarTranslationJob?.cancel()
            grammarTranslationJob = null
            lastGrammarResult = ""
            lastGrammarRequestedInput = ""

            toneEmptyState?.visibility = View.VISIBLE
            toneChipScroll?.visibility = View.GONE
            toneHeroView?.visibility = View.GONE
            toneResultCards.forEach { card ->
                card.container.visibility = View.GONE
                card.primary.text = context.getString(R.string.panel_prompt_tone)
                card.translation.visibility = View.GONE
                card.translation.text = ""
            }
            toneShimmerContainer?.clearAnimation()
            toneShimmerContainer?.visibility = View.GONE
            toneKeyboardButton?.visibility = View.GONE
            toneTranslationJobs.forEach { it.cancel() }
            toneTranslationJobs.clear()
            lastToneResults.clear()
            lastToneRequestedInput = ""
        } else {
            grammarEmptyState?.visibility = View.GONE
            grammarChipScroll?.visibility = View.VISIBLE
            grammarDescriptionView?.visibility = View.VISIBLE
            grammarKeyboardButton?.visibility = View.VISIBLE
            grammarResultCard?.let { card ->
                if (card.primary.text.isNullOrBlank() ||
                    card.primary.text.toString() == context.getString(R.string.panel_prompt_grammar)
                ) {
                    card.primary.text = context.getString(R.string.panel_result_pending)
                }
            }

            toneEmptyState?.visibility = View.GONE
            toneChipScroll?.visibility = View.VISIBLE
            toneKeyboardButton?.visibility = View.VISIBLE
            toneResultCards.forEachIndexed { index, card ->
                if (card.primary.text.isNullOrBlank() ||
                    card.primary.text.toString() == context.getString(R.string.panel_prompt_tone)
                ) {
                    card.primary.text = "Variation ${index + 1} will appear here..."
                }
            }

            val trimmed = currentInputText.trim()
            if (trimmed.isNotEmpty() && trimmed != lastGrammarRequestedInput) {
                triggerDefaultGrammarAction()
            }
            if (trimmed.isNotEmpty() && trimmed != lastToneRequestedInput) {
                triggerDefaultToneAction()
            }
        }
    }
    
    private fun triggerDefaultGrammarAction() {
        val palette = PanelTheme.palette
        val firstChip = grammarChipViews.firstOrNull() ?: return
        updateChipGroupSelection(grammarChipViews, firstChip, palette)
        selectedGrammarChip = firstChip
        grammarChipActions[firstChip]?.invoke()
    }

    private fun triggerDefaultToneAction() {
        val palette = PanelTheme.palette
        val firstChip = toneChipViews.firstOrNull() ?: return
        updateChipGroupSelection(toneChipViews, firstChip, palette)
        selectedToneChip = firstChip
        toneChipActions[firstChip]?.invoke()
    }
    
    /**
     * Set callback for processed text
     */
    fun setOnTextProcessedListener(listener: (String) -> Unit) {
        onTextProcessedCallback = listener
    }
    
    // ========================================
    // PRIVATE: Panel Creation Methods
    // ========================================
    // ✅ REMOVED: hideCurrentPanel() and getCurrentPanelView()
    // UnifiedKeyboardView now manages panel visibility
    
    private fun getOrCreateGrammarPanel(): View {
        if (grammarPanelView == null) {
            grammarPanelView = createGrammarPanel()
        }
        return grammarPanelView!!
    }
    
    private fun getOrCreateTonePanel(): View {
        if (tonePanelView == null) {
            tonePanelView = createTonePanel()
        }
        return tonePanelView!!
    }
    
    private fun getOrCreateAIAssistantPanel(): View {
        if (aiAssistantPanelView == null) {
            aiAssistantPanelView = createAIAssistantPanel()
        }
        return aiAssistantPanelView!!
    }

    private fun getOrCreateTranslatePanel(): View {
        if (translatePanelView == null) {
            translatePanelView = createTranslatePanel()
        }
        return translatePanelView!!
    }
    
    private fun getOrCreateClipboardPanel(): View {
        if (clipboardPanelView == null) {
            clipboardPanelView = createClipboardPanel()
        }
        return clipboardPanelView!!
    }
    
    private fun getOrCreateEmojiPanel(): View {
        if (emojiPanelView == null && emojiPanelController == null) {
            emojiPanelController = EmojiPanelController(
                context,
                themeManager,
                onBackToKeyboard,
                inputConnectionProvider
            ) { active ->
                emojiSearchModeListener?.invoke(active)
            }
            val dummyContainer = FrameLayout(context)
            emojiPanelView = emojiPanelController!!.inflate(dummyContainer)
        }
        return emojiPanelView!!
    }
    
    private fun getOrCreateSettingsPanel(): View {
        if (settingsPanelView == null) {
            settingsPanelView = createSettingsPanel()
        }
        return settingsPanelView!!
    }
    
    // ========================================
    // PROGRAMMATIC PANEL BUILDERS (NO XML!)
    // ========================================
    
    /**
     * Create Grammar Panel - Pure Kotlin UI
     */
    private fun createGrammarPanel(): View {
        val palette = PanelTheme.palette
        val height = keyboardHeightManager?.getPanelHeight() ?: dpToPx(PANEL_HEIGHT_DP)
        val languageConfig = LanguageConfig(supportedLanguages, grammarLanguage) { option ->
            grammarLanguage = option
            refreshGrammarTranslation()
        }

        val contentRoot = createPanelRoot(palette, height)
        contentRoot.addView(createPanelHeader("Fix Grammar", languageConfig))

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dpToPx(4), 0, dpToPx(12))
        }
        scrollView.addView(content)
        contentRoot.addView(scrollView)

        grammarHeroView = null
        val emptyState = createEmptyState(
            title = "Type Something",
            subtitle = "Tap the Grammar toolbar icon to fix your text",
            palette = palette,
            iconRes = R.drawable.grammar_icon
        )
        grammarEmptyState = emptyState
        content.addView(emptyState)

        val chipScroll = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dpToPx(8))
            visibility = View.GONE
        }
        val chipRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        chipScroll.addView(chipRow)
        content.addView(chipScroll)
        grammarChipScroll = chipScroll
        grammarChipViews.clear()
        grammarChipActions.clear()
        selectedGrammarChip = null

        val actionDescription = TextView(context).apply {
            text = GrammarAction.REPHRASE.description
            setTextColor(ColorUtils.setAlphaComponent(Color.WHITE, 180))
            textSize = 11f
            setPadding(0, 0, 0, dpToPx(12))
            visibility = View.GONE
        }
        grammarDescriptionView = actionDescription
        content.addView(actionDescription)

        val grammarShimmer = createShimmerContainer(palette)
        grammarShimmerContainer = grammarShimmer
        content.addView(grammarShimmer)

        val resultCard = createGrammarResultCard(palette)
        resultCard.container.visibility = View.GONE
        grammarResultCard = resultCard
        resultCard.replaceButton.isEnabled = false
        resultCard.replaceButton.setOnClickListener {
            val resolved = resolveTextForLanguage(
                resultCard.primary.text.toString(),
                resultCard.translation.text,
                grammarLanguage
            )
            if (resolved.isNotBlank() && !resolved.startsWith("Result") && !resolved.startsWith("⏳")) {
                onTextProcessedCallback?.invoke(resolved)
                onBackToKeyboard()
            }
        }
        content.addView(resultCard.container)

        GrammarAction.values().forEach { action ->
            val isFirst = grammarChipViews.isEmpty()
            val chip = createSelectableChipPill(action.label, palette, isFirst)
            registerGrammarChip(chip, palette, isFirst) {
                grammarDescriptionView?.text = action.description
                processGrammarAction(action)
            }
            chipRow.addView(chip)
        }

        val customPrompts = PromptManager.getPrompts("grammar")
        customPrompts.forEach { prompt ->
            val chip = createSelectableChipPill(prompt.title, palette, false)
            registerGrammarChip(chip, palette, false) {
                grammarDescriptionView?.text = prompt.title
                processGrammarCustomPrompt(prompt.prompt, prompt.title)
            }
            chipRow.addView(chip)
        }
        chipRow.addView(createAddPromptChip(palette, "grammar"))

        val container = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        }
        contentRoot.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        container.addView(contentRoot)

        val keyboardButton = createFloatingKeyboardButton(palette).apply {
            visibility = View.GONE
        }
        grammarKeyboardButton = keyboardButton
        container.addView(keyboardButton, createKeyboardButtonLayoutParams())

        updateTextDependentPanels()
        return container
    }
    
    /**
     * Create Tone Panel - Pure Kotlin UI
     */
    private fun createTonePanel(): View {
        val palette = PanelTheme.palette
        val height = keyboardHeightManager?.getPanelHeight() ?: dpToPx(PANEL_HEIGHT_DP)
        val languageConfig = LanguageConfig(supportedLanguages, toneLanguage) { option ->
            toneLanguage = option
            refreshToneTranslations()
        }

        val contentRoot = createPanelRoot(palette, height)
        contentRoot.addView(createPanelHeader("Word Tone", languageConfig))

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dpToPx(4), 0, dpToPx(12))
        }
        scrollView.addView(content)
        contentRoot.addView(scrollView)

        toneHeroView = null
        val emptyState = createEmptyState(
            title = "Type Something",
            subtitle = "Tap the Tone toolbar icon for rewrites",
            palette = palette,
            iconRes = R.drawable.tone_icon
        )
        toneEmptyState = emptyState
        content.addView(emptyState)

        val chipScroll = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
            setPadding(0, dpToPx(12), 0, dpToPx(8))
            visibility = View.GONE
        }
        val chipRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        chipScroll.addView(chipRow)
        content.addView(chipScroll)
        toneChipScroll = chipScroll
        toneChipViews.clear()
        toneChipActions.clear()
        selectedToneChip = null

        val toneShimmer = createShimmerContainer(palette)
        toneShimmerContainer = toneShimmer
        content.addView(toneShimmer)

        val cards = mutableListOf<ToneResultViews>()
        repeat(3) {
            val card = createToneResultCard(palette)
            card.container.visibility = View.GONE
            cards.add(card)
            content.addView(card.container)
            content.addView(spacerView(dpToPx(8)))
        }
        if (content.childCount > 0) {
            content.removeViewAt(content.childCount - 1)
        }
        toneResultCards = cards
        lastToneResults.clear()
        repeat(cards.size) { lastToneResults.add("") }
        cards.forEachIndexed { index, card ->
            card.container.setOnClickListener {
                val resolved = resolveTextForLanguage(
                    card.primary.text.toString(),
                    card.translation.text,
                    toneLanguage
                )
                if (resolved.isNotBlank() && !resolved.startsWith("Variation")) {
                    onTextProcessedCallback?.invoke(resolved)
                    onBackToKeyboard()
                }
            }
        }

        ToneAction.values().forEach { action ->
            val isFirst = toneChipViews.isEmpty()
            val chip = createSelectableChipPill(action.label, palette, isFirst)
            registerToneChip(chip, palette, isFirst) {
                processToneAction(action)
            }
            chipRow.addView(chip)
        }

        val customPrompts = PromptManager.getPrompts("tone")
        customPrompts.forEach { prompt ->
            val chip = createSelectableChipPill(prompt.title, palette, false)
            registerToneChip(chip, palette, false) {
                processCustomTonePrompt(prompt.prompt, prompt.title)
            }
            chipRow.addView(chip)
        }
        chipRow.addView(createAddPromptChip(palette, "tone"))

        val container = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        }
        contentRoot.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        container.addView(contentRoot)

        val keyboardButton = createFloatingKeyboardButton(palette).apply {
            visibility = View.GONE
        }
        toneKeyboardButton = keyboardButton
        container.addView(keyboardButton, createKeyboardButtonLayoutParams())

        updateTextDependentPanels()
        return container
    }
    
    /**
     * Create AI Assistant Panel - Pure Kotlin UI matching reference images
     */
    private fun createAIAssistantPanel(): View {
        val palette = PanelTheme.palette
        val height = keyboardHeightManager?.getPanelHeight() ?: dpToPx(PANEL_HEIGHT_DP)
        
        val panelBg = if (themeManager.isImageBackground()) palette.panelSurface else palette.keyboardBg
        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            setBackgroundColor(panelBg)  // Solid color instead of gradient
            isClickable = true
            isFocusable = true
            setOnTouchListener { _, _ -> true }
        }

        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(14))
        }
        root.addView(mainLayout)

        // Header
        mainLayout.addView(createPanelHeader("AI Writing Assistance"))

        // Chip row for AI prompts
        val chipScroll = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dpToPx(8))
        }
        val chipRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        chipScroll.addView(chipRow)
        mainLayout.addView(chipScroll)
        aiChipScroll = chipScroll

        // Reply tone filters row (hidden by default)
        val replyToneRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, dpToPx(8))
            visibility = View.GONE
        }
        mainLayout.addView(replyToneRow)

        // Scrollable content area
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        scrollView.addView(content)
        mainLayout.addView(scrollView)

        // Empty state (Type Something)
        val emptyState = createEmptyState(
            title = "Type Something",
            subtitle = "Tap the AI toolbar icon to ask for help",
            palette = palette,
            iconRes = R.drawable.chatgpt_icon
        )
        content.addView(emptyState)

        // Status text
        val statusText = TextView(context).apply {
            textSize = 11f
            setTextColor(ColorUtils.setAlphaComponent(palette.keyText, 180))
            setPadding(0, dpToPx(4), 0, dpToPx(12))
            visibility = View.GONE
        }
        content.addView(statusText)

        // Shimmer container (for loading state)
        val shimmerContainer = createShimmerContainer(palette)
        content.addView(shimmerContainer)

        // Result container
        val resultContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        content.addView(resultContainer)

        // Replace button
        val replaceButton = Button(context).apply {
            text = "Replace Text"
            isAllCaps = false
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = createAccentButtonBackground(palette, 24)
            setTextColor(getContrastColor(palette.specialAccent))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(44)
            ).apply {
                topMargin = dpToPx(12)
            }
            visibility = View.GONE
            setOnClickListener {
                val result = lastAiResult
                if (result.isNotBlank()) {
                    onTextProcessedCallback?.invoke(result)
                    onBackToKeyboard()
                }
            }
        }
        content.addView(replaceButton)

        // Removed guide link per user request

        // Load prompts and setup chips
        val customPrompts = PromptManager.getPrompts("assistant")
        var selectedPromptIndex = 0
        val chipViews = mutableListOf<TextView>()
        
        if (customPrompts.isEmpty()) {
            val noPromptsMessage = TextView(context).apply {
                text = "No AI prompts added. Add from app."
                textSize = 11f
                setTextColor(ColorUtils.setAlphaComponent(palette.keyText, 160))
                setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8))
            }
            chipRow.addView(noPromptsMessage)
        } else {
            customPrompts.forEachIndexed { index, prompt ->
                val chip = createSelectableChipPill(prompt.title, palette, index == 0)
                chip.setOnClickListener {
                    selectedPromptIndex = index
                    // Update chip selection visuals
                    chipViews.forEachIndexed { i, view ->
                        updateChipSelection(view, i == selectedPromptIndex, palette)
                    }
                    // Show/hide reply tone filters
                    replyToneRow.visibility = if (prompt.title.equals("Reply", ignoreCase = true)) {
                        View.VISIBLE
        } else {
                        View.GONE
                    }
                    // Auto-process if there's input
                    if (currentInputText.trim().isNotEmpty()) {
                        processAIPromptWithShimmer(
                            prompt.prompt,
                            prompt.title,
                            statusText,
                            shimmerContainer,
                            resultContainer,
                            replaceButton,
                            emptyState
                        )
                    }
                }
                chipRow.addView(chip)
                chipViews.add(chip)
            }
        }
        chipRow.addView(createAddPromptChip(palette, "assistant"))

        // Setup reply tone filters
        setupReplyToneFilters(replyToneRow, palette)

        // Store references
        aiAssistantStatusText = statusText
        aiAssistantResultContainer = resultContainer
        aiAssistantReplaceButton = replaceButton
        aiAssistantEmptyState = emptyState
        aiAssistantShimmerContainer = shimmerContainer
        aiAssistantChipRow = chipRow
        aiAssistantReplyToneRow = replyToneRow
        aiAssistantPrompts = customPrompts
        aiAssistantChipViews = chipViews
        val keyboardButton = createFloatingKeyboardButton(palette).apply {
            visibility = View.GONE
        }
        aiKeyboardButton = keyboardButton
        root.addView(keyboardButton, createKeyboardButtonLayoutParams())

        // Update initial state
        updateAIAssistantState()

        return root
    }

    private fun createEmptyState(
        title: String,
        subtitle: String,
        palette: ThemePaletteV2,
        actionLabel: String = "⌨️ Back to Keyboard",
        @DrawableRes iconRes: Int? = null
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dpToPx(32), 0, dpToPx(32))
            
            addView(TextView(context).apply {
                text = title
                textSize = 24f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(palette.keyText)
                gravity = Gravity.CENTER
            })
            
            addView(TextView(context).apply {
                text = subtitle
                textSize = 14f
                setTextColor(ColorUtils.setAlphaComponent(palette.keyText, 180))
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(8), 0, dpToPx(24))
                iconRes?.let { icon ->
                    val drawable = ContextCompat.getDrawable(context, icon)
                    setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                    compoundDrawablePadding = dpToPx(8)
                }
            })
            
            addView(Button(context).apply {
                text = actionLabel
                isAllCaps = false
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                background = createAccentButtonBackground(palette, 24)
                setTextColor(getContrastColor(palette.specialAccent))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(48)
                )
                setOnClickListener { onBackToKeyboard() }
            })
        }
    }

    private fun createShimmerContainer(palette: ThemePaletteV2): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            
            repeat(3) { index ->
                addView(View(context).apply {
                    background = GradientDrawable().apply {
                        cornerRadius = dpToPx(12).toFloat()
                        setColor(ColorUtils.blendARGB(palette.keyBg, Color.WHITE, 0.08f))
                        setStroke(1, ColorUtils.setAlphaComponent(palette.keyBorderColor, 80))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(60)
                    ).apply {
                        if (index < 2) bottomMargin = dpToPx(8)
                    }
                })
            }
        }
    }

    private fun createSelectableChipPill(label: String, palette: ThemePaletteV2, selected: Boolean): TextView {
        return TextView(context).apply {
            text = label
            textSize = 11f
            setTextColor(if (selected) {
                if (ColorUtils.calculateLuminance(palette.specialAccent) > 0.5)
                    Color.BLACK else Color.WHITE
            } else palette.keyText)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(20).toFloat()
                if (selected) {
                    setColor(palette.specialAccent)
                } else {
                    setColor(ColorUtils.blendARGB(palette.keyBg, Color.BLACK, 0.5f))
                    setStroke(1, ColorUtils.setAlphaComponent(palette.specialAccent, 180))
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, dpToPx(8), 0)
            }
        }
    }

    private fun registerGrammarChip(
        chip: TextView,
        palette: ThemePaletteV2,
        selected: Boolean,
        action: () -> Unit
    ) {
        grammarChipViews.add(chip)
        grammarChipActions[chip] = action
        if (selected || selectedGrammarChip == null) {
            selectedGrammarChip = chip
            updateChipGroupSelection(grammarChipViews, chip, palette)
        }
        chip.setOnClickListener {
            updateChipGroupSelection(grammarChipViews, chip, palette)
            selectedGrammarChip = chip
            action()
        }
    }

    private fun registerToneChip(
        chip: TextView,
        palette: ThemePaletteV2,
        selected: Boolean,
        action: () -> Unit
    ) {
        toneChipViews.add(chip)
        toneChipActions[chip] = action
        if (selected || selectedToneChip == null) {
            selectedToneChip = chip
            updateChipGroupSelection(toneChipViews, chip, palette)
        }
        chip.setOnClickListener {
            updateChipGroupSelection(toneChipViews, chip, palette)
            selectedToneChip = chip
            action()
        }
    }

    private fun updateChipSelection(chip: TextView, selected: Boolean, palette: ThemePaletteV2) {
        chip.background = GradientDrawable().apply {
            cornerRadius = dpToPx(20).toFloat()
            if (selected) {
                setColor(palette.specialAccent)
            } else {
                setColor(ColorUtils.blendARGB(palette.keyBg, Color.BLACK, 0.5f))
                setStroke(1, ColorUtils.setAlphaComponent(palette.specialAccent, 180))
            }
        }
        chip.setTextColor(if (selected) {
            if (ColorUtils.calculateLuminance(palette.specialAccent) > 0.5)
                Color.BLACK else Color.WHITE
        } else palette.keyText)
    }

    private fun updateChipGroupSelection(
        chips: List<TextView>,
        selectedChip: TextView,
        palette: ThemePaletteV2
    ) {
        chips.forEach { chip ->
            updateChipSelection(chip, chip == selectedChip, palette)
        }
    }

    private fun setupReplyToneFilters(replyToneRow: LinearLayout, palette: ThemePaletteV2) {
        replyToneRow.removeAllViews()
        val tones = listOf("Positive", "Negative", "Neutral")
        tones.forEach { tone ->
            val chip = TextView(context).apply {
                text = tone
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(16).toFloat()
                    if (tone == aiSelectedReplyTone) {
                        setColor(palette.specialAccent)
                    } else {
                        setColor(Color.TRANSPARENT)
                        setStroke(1, palette.keyText)
                    }
                }
                setTextColor(if (tone == aiSelectedReplyTone) {
                    if (ColorUtils.calculateLuminance(palette.specialAccent) > 0.5)
                        Color.BLACK else Color.WHITE
                } else palette.keyText)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = dpToPx(8)
                }
                setOnClickListener {
                    aiSelectedReplyTone = tone
                    setupReplyToneFilters(replyToneRow, palette)
                    // Reprocess with new tone if there's input
                    if (currentInputText.trim().isNotEmpty()) {
                        val selectedPrompt = aiAssistantPrompts.firstOrNull { 
                            it.title.equals("Reply", ignoreCase = true) 
                        }
                        selectedPrompt?.let { prompt ->
                            processAIPromptWithShimmer(
                                prompt.prompt,
                                prompt.title,
                                aiAssistantStatusText!!,
                                aiAssistantShimmerContainer!!,
                                aiAssistantResultContainer!!,
                                aiAssistantReplaceButton!!,
                                aiAssistantEmptyState!!
                            )
                        }
                    }
                }
            }
            replyToneRow.addView(chip)
        }
    }

    private fun processAIPromptWithShimmer(
        prompt: String,
        title: String,
        statusText: TextView,
        shimmerContainer: LinearLayout,
        resultContainer: LinearLayout,
        replaceButton: Button,
        emptyState: View
    ) {
        val inputText = currentInputText.trim()
        if (inputText.isEmpty()) return

        // Show shimmer, hide results
        shimmerContainer.visibility = View.VISIBLE
        resultContainer.visibility = View.GONE
        replaceButton.visibility = View.GONE
        statusText.visibility = View.VISIBLE
        statusText.text = "Processing…"
        lastAiResult = ""

        // Start shimmer animation
        startShimmerAnimation(shimmerContainer)

        scope.launch {
            try {
                // Modify prompt for Reply with tone
                val modifiedPrompt = if (title.equals("Reply", ignoreCase = true)) {
                    "Generate 3 short reply options to the following message in a $aiSelectedReplyTone tone. Keep each under 25 words. Return each on its own line without numbering."
                } else {
                    prompt
                }

                val result = unifiedAIService.processCustomPrompt(
                    text = inputText,
                    prompt = modifiedPrompt,
                    stream = false
                ).first()

                withContext(Dispatchers.Main) {
                    shimmerContainer.clearAnimation()
                    shimmerContainer.visibility = View.GONE
                    
                    if (result.success && result.text.isNotBlank()) {
                        displayAIResultsInPanel(
                            result.text,
                            statusText,
                            resultContainer,
                            replaceButton,
                            PanelTheme.palette
                        )
        } else {
                        statusText.text = "❌ Error: ${result.error ?: "No response"}"
                        resultContainer.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    shimmerContainer.clearAnimation()
                    shimmerContainer.visibility = View.GONE
                    statusText.text = "❌ Error: ${e.message}"
                    resultContainer.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun startShimmerAnimation(shimmerContainer: LinearLayout) {
        val shimmerAnim = android.view.animation.AlphaAnimation(0.3f, 1.0f).apply {
            duration = 1000
            repeatCount = android.view.animation.Animation.INFINITE
            repeatMode = android.view.animation.Animation.REVERSE
        }
        shimmerContainer.startAnimation(shimmerAnim)
    }

    private fun displayAIResultsInPanel(
        text: String,
        statusText: TextView,
        resultContainer: LinearLayout,
        replaceButton: Button,
        palette: ThemePaletteV2
    ) {
        resultContainer.removeAllViews()
        resultContainer.visibility = View.VISIBLE
        
        // Strip numbering (1., 2., 3., etc.) and bullet points
        val results = text.split("\n")
            .map { line ->
                line.trim()
                    .replace(Regex("^\\d+[.)\\s]+"), "") // Remove "1. ", "1) ", "1 ", etc.
                    .trimStart('-', '•', '*', '·')
                    .trim()
            }
            .filter { it.isNotEmpty() }
            .take(3)

        if (results.isEmpty()) {
            statusText.text = "No results available"
            return
        }

        lastAiResult = results.first()
        statusText.text = "Tap a suggestion to use it"

        results.forEach { result ->
            val card = TextView(context).apply {
                this.text = result
                textSize = 13f
                setLineSpacing(0f, 1.2f)
                setTextColor(palette.keyText)
                setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(12).toFloat()
                    setColor(ColorUtils.blendARGB(palette.keyBg, Color.BLACK, 0.4f))
                    setStroke(1, ColorUtils.setAlphaComponent(palette.specialAccent, 100))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(8)
                }
                setOnClickListener {
                    lastAiResult = result
                    onTextProcessedCallback?.invoke(result)
                    onBackToKeyboard()
                }
            }
            resultContainer.addView(card)
        }

        replaceButton.visibility = View.VISIBLE
    }

    private fun updateAIAssistantState() {
        val hasInput = currentInputText.trim().isNotEmpty()
        val palette = PanelTheme.palette
        
        aiAssistantEmptyState?.visibility = if (hasInput) View.GONE else View.VISIBLE
        aiChipScroll?.visibility = if (hasInput) View.VISIBLE else View.GONE
        aiAssistantReplyToneRow?.visibility = View.GONE  // Hide by default
        aiKeyboardButton?.visibility = if (hasInput) View.VISIBLE else View.GONE
        
        if (!hasInput) {
            // Clear all processed results when no input
            aiAssistantResultContainer?.removeAllViews()
            aiAssistantResultContainer?.visibility = View.GONE
            aiAssistantReplaceButton?.visibility = View.GONE
            aiAssistantShimmerContainer?.clearAnimation()
            aiAssistantShimmerContainer?.visibility = View.GONE
            aiAssistantStatusText?.visibility = View.GONE
            lastAiResult = ""
        } else if (
            aiAssistantPrompts.isNotEmpty() &&
            aiAssistantStatusText != null &&
            aiAssistantShimmerContainer != null &&
            aiAssistantResultContainer != null &&
            aiAssistantReplaceButton != null &&
            aiAssistantEmptyState != null
        ) {
            // Auto-select first prompt and process
            aiAssistantChipViews.forEachIndexed { index, view ->
                updateChipSelection(view, index == 0, palette)
            }
            val firstPrompt = aiAssistantPrompts.first()
            processAIPromptWithShimmer(
                firstPrompt.prompt,
                firstPrompt.title,
                aiAssistantStatusText!!,
                aiAssistantShimmerContainer!!,
                aiAssistantResultContainer!!,
                aiAssistantReplaceButton!!,
                aiAssistantEmptyState!!
            )
        }
    }

    
    /**
     * Create Translate Panel - Pure Kotlin UI
     */
    private fun createTranslatePanel(): View {
        val palette = PanelTheme.palette
        val height = keyboardHeightManager?.getPanelHeight() ?: dpToPx(PANEL_HEIGHT_DP)
        val languageOptions = listOf(
            "🇺🇸 English" to "English",
            "🇪🇸 Spanish" to "Spanish",
            "🇫🇷 French" to "French",
            "🇩🇪 German" to "German",
            "🇮🇳 Hindi" to "Hindi",
            "🇯🇵 Japanese" to "Japanese",
            "🇧🇷 Portuguese" to "Portuguese",
            "🇰🇷 Korean" to "Korean"
        )
        
        val panelBg = if (themeManager.isImageBackground()) palette.panelSurface else palette.keyboardBg
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            setBackgroundColor(panelBg)
            
            // Consume focus so touches stay within the panel
            isClickable = true
            isFocusable = true
            
            addView(createPanelHeader("🌐 Translate"))
            
            addView(ScrollView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(8))
                    
                    val outputView = themedTextView("Translation will appear here...", 14f, false).apply {
                        minHeight = dpToPx(80)
                        background = createRoundedDrawable(PanelTheme.palette.keyBg, dpToPx(8))
                        setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                        setOnClickListener {
                            val text = this.text.toString()
                            if (text.isNotEmpty() && !text.startsWith("Translation")) {
                                onTextProcessedCallback?.invoke(text)
                                Toast.makeText(context, "Translation inserted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    addView(outputView)
                    
                    addView(View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(16)
                        )
                    })
                    
                    val scrollView = HorizontalScrollView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        isHorizontalScrollBarEnabled = false
                    }
                    
                    val buttonContainer = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    
                    languageOptions.forEach { (label, language) ->
                        buttonContainer.addView(themedButton(label) {
                            processTranslateAction(language, outputView)
                        })
                    }
                    
                    scrollView.addView(buttonContainer)
                    addView(scrollView)
                    
                    addView(View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(16)
                        )
                    })
                    
                    addView(themedButton("↩️ Insert Translation") {
                        val text = outputView.text.toString()
                        if (text.isNotEmpty() && !text.startsWith("Translation")) {
                            onTextProcessedCallback?.invoke(text)
                            Toast.makeText(context, "Translation inserted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Nothing to insert yet", Toast.LENGTH_SHORT).show()
                        }
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(44)
                        ).apply {
                            bottomMargin = dpToPx(8)
                        }
                    })
                    
                    addView(themedButton("📋 Copy Translation") {
                        val text = outputView.text.toString()
                        if (text.isNotEmpty() && !text.startsWith("Translation")) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Translation", text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Nothing to copy yet", Toast.LENGTH_SHORT).show()
                        }
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(44)
                        )
                    })
                })
            })
        }
    }
    
    /**
     * Create Clipboard Panel - Pure Kotlin UI
     */
    private fun createClipboardPanel(): View {
        val palette = PanelTheme.palette
        val height = keyboardHeightManager?.getPanelHeight() ?: dpToPx(PANEL_HEIGHT_DP)
        val prefs = context.getSharedPreferences("ai_keyboard_clipboard", Context.MODE_PRIVATE)
        val root = createPanelRoot(palette, height)
        root.addView(createPanelHeader("Clipboard"))

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dpToPx(4), 0, dpToPx(12))
        }
        scrollView.addView(content)
        root.addView(scrollView)

        content.addView(createHeroBlock("Clipboard", "Tap an entry to paste instantly"))

        val history = prefs.getStringSet("clipboard_history", emptySet())?.toList() ?: emptyList()
        if (history.isEmpty()) {
            content.addView(TextView(context).apply {
                text = "No clipboard items yet"
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(ColorUtils.setAlphaComponent(Color.WHITE, 200))
                setPadding(0, dpToPx(32), 0, dpToPx(32))
            })
        } else {
            val grid = GridLayout(context).apply {
                columnCount = 2
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            history.take(20).forEach { item ->
                grid.addView(createClipboardTile(item, prefs, palette))
            }
            content.addView(grid)
        }

        val clearLink = TextView(context).apply {
            text = "Clear history"
            textSize = 13f
            setPadding(0, dpToPx(12), 0, dpToPx(4))
            setTextColor(palette.specialAccent)
            setOnClickListener {
                prefs.edit().remove("clipboard_history").apply()
                Toast.makeText(context, "Clipboard history cleared", Toast.LENGTH_SHORT).show()
                rebuildDynamicPanelsFromPrompts()
            }
        }
        content.addView(clearLink)

        
        

        return root
    }
    
    /**
     * Create Settings Panel - Pure Kotlin UI
     */
    private fun createSettingsPanel(): View {
        val palette = PanelTheme.palette
        val height = keyboardHeightManager?.getPanelHeight() ?: dpToPx(PANEL_HEIGHT_DP)
        val prefs = context.getSharedPreferences("ai_keyboard_settings", Context.MODE_PRIVATE)
        val quickSettings = loadQuickSettings(prefs)
        val panelBg = if (themeManager.isImageBackground()) palette.panelSurface else palette.keyboardBg

        val adapter = QuickSettingsAdapter(
            items = quickSettings,
            palette = palette,
            onItemInvoked = { item ->
                try {
                    item.handler(item)
                } catch (e: Exception) {
                    LogUtil.e(TAG, "Error executing quick setting ${item.id}", e)
                    Toast.makeText(context, "Action unavailable", Toast.LENGTH_SHORT).show()
                }
            },
            onOrderChanged = { items -> persistQuickSettingsOrder(prefs, items) }
        )

        val navBarHeight = keyboardHeightManager?.getNavigationBarHeight() ?: 0

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            setBackgroundColor(panelBg)
            val horizontalPadding = dpToPx(16)
            val topPadding = dpToPx(16)
            val bottomPadding = dpToPx(14) + navBarHeight
            setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding)
            clipToPadding = false
            clipChildren = false
            isClickable = true
            isFocusable = true
            setOnTouchListener { _, _ -> true }
        }

        container.addView(createSettingsPanelToolbar(palette))

        

        val recyclerView = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            clipToPadding = false
            setPadding(20, 0, 0, dpToPx(8))
            layoutManager = GridLayoutManager(context, calculateQuickSettingsSpan())
            this.adapter = adapter
        }

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.swapItems(
                    viewHolder.bindingAdapterPosition,
                    target.bindingAdapterPosition
                )
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // no-op
            }

            override fun isLongPressDragEnabled(): Boolean = false
        })
        touchHelper.attachToRecyclerView(recyclerView)
        adapter.setDragStartListener { holder -> touchHelper.startDrag(holder) }

        container.addView(recyclerView)
        return container
    }
    
    // ========================================
    // HELPER FUNCTIONS FOR PROGRAMMATIC UI
    // ========================================
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    /**
     * Helper: Create compact header bar with back button and title
     * ✅ REFINED: Minimal padding, no extra top space
     */
    private fun createPanelHeader(title: String, languageConfig: LanguageConfig? = null): View {
        val palette = PanelTheme.palette
        val headerTextColor = palette.keyText
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )
            setPadding(0, dpToPx(4), 0, dpToPx(8))
            setBackgroundColor(Color.TRANSPARENT)

            val backButton = ImageView(context).apply {
                setImageResource(R.drawable.ic_back_chevron)
                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(headerTextColor)
                )
                layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
                setOnClickListener { onBackToKeyboard() }
            }
            addView(backButton)

            addView(TextView(context).apply {
                text = title
                setTextColor(headerTextColor)
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })

            languageConfig?.let {
                addView(createLanguageChipView(palette, it))
            }
        }
    }

    private fun createPanelRoot(palette: ThemePaletteV2, height: Int): LinearLayout {
        val panelBg = if (themeManager.isImageBackground()) palette.panelSurface else palette.keyboardBg
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            setBackgroundColor(panelBg)  // Solid color instead of gradient
            
            // ✅ CRITICAL FIX: Add navigation bar padding at bottom
            val navBarHeight = keyboardHeightManager?.getNavigationBarHeight() ?: 0
            val basePaddingH = dpToPx(16)
            val basePaddingTop = dpToPx(10)
            val basePaddingBottom = dpToPx(14)
            
            setPadding(
                basePaddingH,
                basePaddingTop,
                basePaddingH,
                basePaddingBottom + navBarHeight  // Add nav bar height to bottom padding
            )
            
            clipToPadding = false
            clipChildren = false
            
            // ✅ Consume all touch events to prevent keyboard keys from being triggered
            isClickable = true
            isFocusable = true
            setOnTouchListener { _, _ -> true }
            
            LogUtil.d(TAG, "🔧 Panel created with nav bar padding: $navBarHeight px")
        }
    }

    private fun createLanguageChipView(palette: ThemePaletteV2, config: LanguageConfig): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val baseColor = if (themeManager.isImageBackground()) palette.panelSurface else palette.keyboardBg
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(18).toFloat()
                setColor(ColorUtils.blendARGB(baseColor, Color.WHITE, 0.12f))
                setStroke(1, ColorUtils.setAlphaComponent(palette.specialAccent, 160))
            }
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))

            val icon = ImageView(context).apply {
                setImageResource(R.drawable.sym_keyboard_globe)
                setColorFilter(palette.keyText)
                layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(18))
            }
            addView(icon)

            val labelView = TextView(context).apply {
                text = config.current.label
                setTextColor(palette.keyText)
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(6), 0, dpToPx(4), 0)
            }
            addView(labelView)

            val chevron = ImageView(context).apply {
                setImageResource(R.drawable.ic_back_chevron)
                rotation = 90f
                setColorFilter(palette.keyText)
                layoutParams = LinearLayout.LayoutParams(dpToPx(16), dpToPx(16))
            }
            addView(chevron)

            setOnClickListener {
                val popup = PopupMenu(context, this)
                config.options.forEach { option ->
                    popup.menu.add(option.label)
                }
                popup.setOnMenuItemClickListener { item ->
                    val selected = config.options.firstOrNull { it.label == item.title }
                    if (selected != null) {
                        config.current = selected
                        labelView.text = selected.label
                        config.onChanged(selected)
                    }
                    true
                }
                popup.show()
            }
        }
    }

    private fun createHeroBlock(title: String, subtitle: String): LinearLayout {
        val palette = PanelTheme.palette
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dpToPx(4), 0, dpToPx(12))
            visibility = View.GONE  // Hidden by default
            addView(TextView(context).apply {
                text = title
                textSize = 22f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(palette.keyText)
            })
            addView(TextView(context).apply {
                text = subtitle
                textSize = 12f
                setTextColor(ColorUtils.setAlphaComponent(palette.keyText, 200))
                setPadding(0, dpToPx(4), 0, 0)
            })
        }
    }

    private fun createPrimaryButton(label: String, palette: ThemePaletteV2, onClick: () -> Unit): Button {
        return Button(context).apply {
            text = label
            textSize = 16f
            setTextColor(getContrastColor(palette.specialAccent))
            isAllCaps = false
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    ColorUtils.blendARGB(palette.specialAccent, Color.WHITE, 0.12f),
                    palette.specialAccent
                )
            ).apply { cornerRadius = dpToPx(24).toFloat() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )
            setOnClickListener { onClick() }
        }
    }

    private fun createGuideLink(palette: ThemePaletteV2): TextView {
        return TextView(context).apply {
            text = "How to Use Kvive Guide"
            gravity = Gravity.CENTER
            textSize = 13f
            setPadding(0, dpToPx(10), 0, 0)
            setTextColor(palette.specialAccent)
            setOnClickListener {
                Toast.makeText(context, "Guide coming soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createAddPromptChip(palette: ThemePaletteV2, category: String): TextView {
        return TextView(context).apply {
            text = "+ Add More To Keyboard"
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(palette.specialAccent)
            setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(18).toFloat()
                setColor(Color.TRANSPARENT)
                setStroke(1, ColorUtils.setAlphaComponent(palette.specialAccent, 200))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, dpToPx(8), 0)
            }
            setOnClickListener { launchPromptManager(category) }
        }
    }

    private fun createPanelCardView(placeholder: String, palette: ThemePaletteV2): TextView {
        val baseKeyColor = if (themeManager.isImageBackground()) {
            ColorUtils.setAlphaComponent(Color.BLACK, 160)
        } else {
            palette.keyBg
        }
        val blended = ColorUtils.blendARGB(baseKeyColor, palette.keyboardBg, 0.25f)
        return TextView(context).apply {
            text = placeholder
            textSize = 15f
            setLineSpacing(0f, 1.15f)
            setTextColor(palette.keyText)
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(14).toFloat()
                setColor(blended)
                setStroke(1, ColorUtils.setAlphaComponent(palette.keyBorderColor, 140))
            }
            setPadding(dpToPx(18), dpToPx(14), dpToPx(18), dpToPx(14))
        }
    }

    private fun getContrastColor(color: Int): Int {
        return if (ColorUtils.calculateLuminance(color) > 0.5) Color.BLACK else Color.WHITE
    }

    private fun createAccentButtonBackground(
        palette: ThemePaletteV2,
        cornerRadiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dpToPx(cornerRadiusDp).toFloat()
            setColor(palette.specialAccent)
        }
    }

    private fun createGrammarResultCard(palette: ThemePaletteV2): GrammarResultViews {
        val baseKeyColor = if (themeManager.isImageBackground()) {
            ColorUtils.setAlphaComponent(Color.BLACK, 160)
        } else {
            palette.keyBg
        }
        val blended = ColorUtils.blendARGB(baseKeyColor, palette.keyboardBg, 0.25f)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(14).toFloat()
                setColor(blended)
                setStroke(1, ColorUtils.setAlphaComponent(palette.keyBorderColor, 140))
            }
            setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val primary = TextView(context).apply {
            text = context.getString(R.string.panel_result_pending)
            textSize = 13f
            setLineSpacing(0f, 1.2f)
            setTextColor(palette.keyText)
        }
        container.addView(primary)

        val translation = TextView(context).apply {
            visibility = View.GONE
            textSize = 11f
            setPadding(0, dpToPx(6), 0, dpToPx(6))
            setTextColor(ColorUtils.setAlphaComponent(palette.keyText, 200))
        }
        container.addView(translation)

        val replaceButton = Button(context).apply {
            text = context.getString(R.string.ai_panel_button_replace)
            isAllCaps = false
            textSize = 12f
            background = createAccentButtonBackground(palette, 20)
            setTextColor(getContrastColor(palette.specialAccent))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(36)
            ).apply {
                gravity = Gravity.END
            }
        }
        container.addView(replaceButton)

        return GrammarResultViews(container, primary, translation, replaceButton)
    }

    private fun createToneResultCard(palette: ThemePaletteV2): ToneResultViews {
        val baseKeyColor = if (themeManager.isImageBackground()) {
            ColorUtils.setAlphaComponent(Color.BLACK, 160)
        } else {
            palette.keyBg
        }
        val blended = ColorUtils.blendARGB(baseKeyColor, palette.keyboardBg, 0.25f)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(14).toFloat()
                setColor(blended)
                setStroke(1, ColorUtils.setAlphaComponent(palette.keyBorderColor, 140))
            }
            setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val primary = TextView(context).apply {
            text = context.getString(R.string.panel_prompt_tone)
            textSize = 13f
            setLineSpacing(0f, 1.2f)
            setTextColor(palette.keyText)
        }
        container.addView(primary)

        val translation = TextView(context).apply {
            visibility = View.GONE
            textSize = 11f
            setPadding(0, dpToPx(6), 0, 0)
            setTextColor(ColorUtils.setAlphaComponent(palette.keyText, 200))
        }
        container.addView(translation)

        return ToneResultViews(container, primary, translation)
    }

    private fun createFloatingKeyboardButton(palette: ThemePaletteV2): ImageButton {
        return ImageButton(context).apply {
            setImageResource(R.drawable.keyboard_icon)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(palette.specialAccent)
            }
            ImageViewCompat.setImageTintList(
                this,
                ColorStateList.valueOf(getContrastColor(palette.specialAccent))
            )
            contentDescription = context.getString(R.string.keyboard_button_back)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            elevation = dpToPx(6).toFloat()
            setOnClickListener { onBackToKeyboard() }
        }
    }

    private fun createKeyboardButtonLayoutParams(): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            dpToPx(52),
            dpToPx(52),
            Gravity.END or Gravity.BOTTOM
        ).apply {
            rightMargin = dpToPx(16)
            bottomMargin = dpToPx(16)
        }
    }

    private fun spacerView(heightPx: Int): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heightPx
            )
        }
    }

    private fun launchPromptManager(category: String) {
        try {
            // Map category to Flutter navigation routes
            val navigationRoute = when (category) {
                "assistant" -> "ai_writing_custom"  // AI Writing Assistance -> Custom Assistance tab
                "grammar" -> "custom_grammar"        // Custom Grammar screen
                "tone" -> "custom_tones"             // Custom Tones screen
                else -> "prompts_$category"          // Fallback to old format
            }
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", navigationRoute)
            }
            context.startActivity(intent)
            Log.d(TAG, "✅ Launching Flutter app: category=$category, route=$navigationRoute")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching prompt manager for $category", e)
            Toast.makeText(context, "Unable to open app", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun themedTextView(text: String, size: Float = 16f, bold: Boolean = false): TextView {
        val palette = PanelTheme.palette
        return TextView(context).apply {
            this.text = text
            this.textSize = size
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(palette.keyText)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }
    
    private fun themedButton(label: String, onClick: () -> Unit): Button {
        val palette = PanelTheme.palette
        return Button(context).apply {
            text = label
            textSize = 13f
            background = createRoundedDrawable(palette.keyBg, dpToPx(12))
            setTextColor(palette.keyText)
            setOnClickListener { onClick() }
            isAllCaps = false
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(40)
            ).apply {
                setMargins(0, 0, dpToPx(8), 0)
            }
        }
    }
    
    private fun createRoundedDrawable(color: Int, radiusPx: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusPx.toFloat()
            val palette = PanelTheme.palette
            setStroke(dpToPx(1), palette.keyBorderColor)
        }
    }
    
    private fun createHorizontalScrollButtonContainer(): LinearLayout {
        val scrollView = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
        }
        
        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        scrollView.addView(buttonContainer)
        
        // Return the LinearLayout so buttons can be added to it
        return buttonContainer
    }
    
    private fun createSettingToggle(
        label: String,
        key: String,
        prefs: android.content.SharedPreferences
    ): View {
        val palette = PanelTheme.palette
        
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, dpToPx(12), 0, dpToPx(12))
            background = createRoundedDrawable(PanelTheme.palette.keyBg, dpToPx(8))
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            
            val labelView = TextView(context).apply {
                text = label
                textSize = 16f
                setTextColor(palette.keyText)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            addView(labelView)
            
            val toggle = Switch(context).apply {
                isChecked = prefs.getBoolean(key, true)
                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(key, isChecked).apply()
                    
                    // Broadcast settings change
                    val intent = android.content.Intent("com.example.ai_keyboard.SETTINGS_CHANGED")
                    intent.setPackage(context.packageName)
                    context.sendBroadcast(intent)
                    
                    LogUtil.d(TAG, "Setting changed: $key = $isChecked")
                }
            }
            addView(toggle)
        }.apply {
            val outerParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            outerParams.setMargins(0, 0, 0, dpToPx(8))
            layoutParams = outerParams
        }
    }

    private fun createSettingsPanelToolbar(palette: ThemePaletteV2): View {
        val service = AIKeyboardService.getInstance()

        fun addButton(
            parent: LinearLayout,
            @DrawableRes iconRes: Int,
            withBox: Boolean = false,
            onClick: () -> Unit
        ) {
            val button = ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)).apply {
                    rightMargin = dpToPx(8)
                }
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                background = createToolbarButtonBackground(palette, withBox)
                setImageResource(iconRes)
                val tintColor = if (withBox) {
                    val drawableColor = (background as? GradientDrawable)?.color?.defaultColor
                        ?: palette.specialAccent
                    getContrastColor(drawableColor)
                } else {
                    getContrastColor(palette.toolbarBg)
                }
                ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(tintColor))
                setOnClickListener { onClick() }
            }
            parent.addView(button)
        }

        val leftGroup = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val rightGroup = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        addButton(leftGroup, R.drawable.keyboard_icon, withBox = true) { onBackToKeyboard() }
        addButton(leftGroup, R.drawable.voice, withBox = true) {
            if (service != null) {
                service.startVoiceInputFromToolbar()
            } else {
                showToast("Voice input unavailable")
            }
        }
        addButton(leftGroup, R.drawable.emoji_icon, withBox = true) {
            openPanelFromToolbar(PanelType.EMOJI)
        }

        addButton(rightGroup, R.drawable.chatgpt_icon, withBox = true) {
            openPanelFromToolbar(PanelType.AI_ASSISTANT)
        }
        addButton(rightGroup, R.drawable.grammar_icon, withBox = true) {
            openPanelFromToolbar(PanelType.GRAMMAR)
        }
        addButton(rightGroup, R.drawable.tone_icon, withBox = true) {
            openPanelFromToolbar(PanelType.TONE)
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 0)
            addView(leftGroup)
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            })
            addView(rightGroup)
        }
    }

    private fun createToolbarButtonBackground(
        palette: ThemePaletteV2,
        withBox: Boolean
    ): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dpToPx(18).toFloat()
            if (withBox) {
                setColor(palette.specialAccent)
            } else {
                setColor(ColorUtils.setAlphaComponent(palette.keyBg, 160))
                setStroke(dpToPx(1), ColorUtils.setAlphaComponent(palette.keyBorderColor, 120))
            }
        }
    }

    private fun loadQuickSettings(prefs: SharedPreferences): MutableList<QuickSettingItem> {
        val defaultsList = createDefaultQuickSettings(prefs)
        val defaultsMap = LinkedHashMap<String, QuickSettingItem>()
        defaultsList.forEach { defaultsMap[it.id] = it }

        val stored = prefs.getString("quick_settings_order_v2", null)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        if (stored.isEmpty()) {
            return defaultsList.toMutableList()
        }

        val ordered = mutableListOf<QuickSettingItem>()
        stored.forEach { id ->
            defaultsMap.remove(id)?.let { ordered.add(it) }
        }

        if (defaultsMap.isNotEmpty()) {
            ordered.addAll(defaultsMap.values)
        }

        return if (ordered.isEmpty()) defaultsList.toMutableList() else ordered
    }

    private fun createDefaultQuickSettings(prefs: SharedPreferences): List<QuickSettingItem> {
        val service = AIKeyboardService.getInstance()
        val flutterPrefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)

        val soundFallback = prefs.getBoolean("sound_enabled", true)
        val vibrationFallback = prefs.getBoolean("vibration_enabled", true)
        val numberRowFallback = prefs.getBoolean("show_number_row", false)
        val autoCorrectFallback = prefs.getBoolean("auto_correct", true)
        val oneHandedFallback = flutterPrefs.getBoolean("flutter.keyboard_settings.one_handed_mode", false)

        return listOf(
            QuickSettingItem(
                id = "themes",
                label = "Themes",
                iconRes = R.drawable.ic_qs_themes,
                type = QuickSettingType.ACTION
            ) {
                openFlutterRoute("theme_editor")
            },
            QuickSettingItem(
                id = "number_row",
                label = "Number Row",
                iconRes = R.drawable.number_123,
                type = QuickSettingType.TOGGLE,
                isActive = service?.isNumberRowEnabled() ?: numberRowFallback
            ) { item ->
                if (service != null) {
                    service.toggleNumberRow()
                    item.isActive = service.isNumberRowEnabled()
                } else {
                    val newState = !item.isActive
                    prefs.edit().putBoolean("show_number_row", newState).apply()
                    item.isActive = newState
                }
            },
            QuickSettingItem(
                id = "sound",
                label = "Sound",
                iconRes = R.drawable.ic_qs_sound,
                type = QuickSettingType.TOGGLE,
                isActive = service?.isSoundEnabled() ?: soundFallback
            ) { item ->
                if (service != null) {
                    service.toggleSound()
                    item.isActive = service.isSoundEnabled()
                } else {
                    val newState = !item.isActive
                    prefs.edit().putBoolean("sound_enabled", newState).apply()
                    item.isActive = newState
                }
            },
            QuickSettingItem(
                id = "vibration",
                label = "Vibration",
                iconRes = R.drawable.ic_qs_vibration,
                type = QuickSettingType.TOGGLE,
                isActive = service?.isVibrationEnabled() ?: vibrationFallback
            ) { item ->
                if (service != null) {
                    service.toggleVibration()
                    item.isActive = service.isVibrationEnabled()
                } else {
                    val newState = !item.isActive
                    prefs.edit().putBoolean("vibration_enabled", newState).apply()
                    item.isActive = newState
                }
            },
            QuickSettingItem(
                id = "undo",
                label = "Undo",
                iconRes = R.drawable.ic_qs_undo,
                type = QuickSettingType.ACTION
            ) {
                performEditorCommand(EditorCommand.UNDO)
            },
            QuickSettingItem(
                id = "redo",
                label = "Redo",
                iconRes = R.drawable.ic_qs_redo,
                type = QuickSettingType.ACTION
            ) {
                performEditorCommand(EditorCommand.REDO)
            },
            QuickSettingItem(
                id = "copy",
                label = "Copy",
                iconRes = R.drawable.ic_qs_copy,
                type = QuickSettingType.ACTION
            ) {
                performEditorCommand(EditorCommand.COPY)
            },
            QuickSettingItem(
                id = "paste",
                label = "Paste",
                iconRes = R.drawable.ic_qs_paste,
                type = QuickSettingType.ACTION
            ) {
                performEditorCommand(EditorCommand.PASTE)
            },
            QuickSettingItem(
                id = "translate",
                label = "Translator",
                iconRes = R.drawable.sym_keyboard_globe,
                type = QuickSettingType.ACTION
            ) {
                if (service != null) {
                    service.showUnifiedPanel(UnifiedPanelManager.PanelType.TRANSLATE)
                } else {
                    showToast("Translator not available")
                }
            },
            QuickSettingItem(
                id = "auto_correct",
                label = "Auto-Correct",
                iconRes = R.drawable.ic_qs_spellcheck,
                type = QuickSettingType.TOGGLE,
                isActive = service?.isAutoCorrectEnabled() ?: autoCorrectFallback
            ) { item ->
                if (service != null) {
                    service.toggleAutoCorrect()
                    item.isActive = service.isAutoCorrectEnabled()
                } else {
                    val newState = !item.isActive
                    prefs.edit().putBoolean("auto_correct", newState).apply()
                    item.isActive = newState
                }
            },
            QuickSettingItem(
                id = "one_handed",
                label = "One Handed",
                iconRes = R.drawable.ic_qs_one_handed,
                type = QuickSettingType.TOGGLE,
                isActive = service?.isOneHandedModeEnabled() ?: oneHandedFallback
            ) { item ->
                if (service != null) {
                    service.toggleOneHandedMode()
                    item.isActive = service.isOneHandedModeEnabled()
                } else {
                    val newState = !item.isActive
                    flutterPrefs.edit().putBoolean("flutter.keyboard_settings.one_handed_mode", newState).apply()
                    item.isActive = newState
                }
            },
            QuickSettingItem(
                id = "settings",
                label = "Settings",
                iconRes = R.drawable.setting,
                type = QuickSettingType.ACTION
            ) {
                openFlutterRoute("settings_screen")
            }
        )
    }

    private fun persistQuickSettingsOrder(prefs: SharedPreferences, items: List<QuickSettingItem>) {
        val order = items.joinToString(",") { it.id }
        prefs.edit().putString("quick_settings_order_v2", order).apply()
    }

    private fun calculateQuickSettingsSpan(): Int {
        val availableWidth = context.resources.displayMetrics.widthPixels - dpToPx(32)
        val itemWidth = dpToPx(88).coerceAtLeast(1)
        val span = (availableWidth / itemWidth).coerceIn(3, 6)
        return if (span <= 0) 3 else span
    }

    private fun openFlutterRoute(route: String? = null) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                route?.let { putExtra("navigate_to", it) }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            LogUtil.e(TAG, "Unable to open Flutter route: $route", e)
            showToast("Unable to open app")
        }
    }

    private enum class EditorCommand {
        UNDO,
        REDO,
        COPY,
        PASTE
    }

    private fun performEditorCommand(command: EditorCommand) {
        val inputConnection = inputConnectionProvider()
        if (inputConnection == null) {
            showToast("No text field active")
            return
        }
        when (command) {
            EditorCommand.UNDO -> sendKeyCombination(inputConnection, KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON)
            EditorCommand.REDO -> sendKeyCombination(
                inputConnection,
                KeyEvent.KEYCODE_Z,
                KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON
            )
            EditorCommand.COPY -> inputConnection.performContextMenuAction(android.R.id.copy)
            EditorCommand.PASTE -> inputConnection.performContextMenuAction(android.R.id.paste)
        }
    }

    private fun sendKeyCombination(
        inputConnection: InputConnection,
        keyCode: Int,
        metaState: Int
    ) {
        val eventTime = SystemClock.uptimeMillis()
        val down = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)
        val up = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0, metaState)
        inputConnection.sendKeyEvent(down)
        inputConnection.sendKeyEvent(up)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun openPanelFromToolbar(type: PanelType) {
        val service = AIKeyboardService.getInstance()
        if (service != null) {
            service.showUnifiedPanel(type)
        } else {
            showToast("Keyboard not available")
        }
    }

    private inner class QuickSettingsAdapter(
        private val items: MutableList<QuickSettingItem>,
        private val palette: ThemePaletteV2,
        private val onItemInvoked: (QuickSettingItem) -> Unit,
        private val onOrderChanged: (List<QuickSettingItem>) -> Unit
    ) : RecyclerView.Adapter<QuickSettingsAdapter.QuickSettingViewHolder>() {

        private var dragStartListener: ((RecyclerView.ViewHolder) -> Unit)? = null

        fun setDragStartListener(listener: (RecyclerView.ViewHolder) -> Unit) {
            dragStartListener = listener
        }

        fun swapItems(from: Int, to: Int) {
            if (from == to) return
            val moved = items.removeAt(from)
            items.add(to, moved)
            notifyItemMoved(from, to)
            onOrderChanged(items)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickSettingViewHolder {
            val context = parent.context
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                ).apply {
                    val spacing = dpToPx(12)
                    bottomMargin = spacing
                }
            }

            val iconSize = dpToPx(48)
            val iconView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
            container.addView(iconView)

            val labelView = TextView(context).apply {
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(8), 0, 0)
                setTextColor(ColorUtils.setAlphaComponent(palette.keyText, 220))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            container.addView(labelView)

            return QuickSettingViewHolder(container, iconView, labelView)
        }

        override fun onBindViewHolder(holder: QuickSettingViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
            holder.itemView.setOnClickListener {
                onItemInvoked(item)
                if (item.type == QuickSettingType.TOGGLE) {
                    val adapterPosition = holder.bindingAdapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(adapterPosition)
                    }
                }
            }
            holder.itemView.setOnLongClickListener {
                dragStartListener?.invoke(holder)
                true
            }
        }

        override fun getItemCount(): Int = items.size

        inner class QuickSettingViewHolder(
            itemView: View,
            private val iconView: ImageView,
            private val labelView: TextView
        ) : RecyclerView.ViewHolder(itemView) {

            fun bind(item: QuickSettingItem) {
                labelView.text = item.label

                iconView.setImageResource(item.iconRes)

                val baseColor = ColorUtils.blendARGB(
                    palette.keyBg,
                    palette.keyboardBg,
                    0.18f
                )
                val activeColor = palette.specialAccent

                val backgroundColor = if (item.type == QuickSettingType.TOGGLE && item.isActive) {
                    activeColor
                } else {
                    baseColor
                }

                iconView.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(backgroundColor)
                }

                val tintColor = if (item.type == QuickSettingType.TOGGLE && item.isActive) {
                    getContrastColor(activeColor)
                } else {
                    palette.keyText
                }
                ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(tintColor))
            }
        }
    }
    
    private fun createClipboardTile(
        text: String,
        prefs: android.content.SharedPreferences,
        palette: com.example.ai_keyboard.themes.ThemePaletteV2
    ): View {
        val display = text.take(80) + if (text.length > 80) "…" else ""
        val baseColor = if (themeManager.isImageBackground()) palette.panelSurface else palette.keyboardBg
        val background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                ColorUtils.blendARGB(baseColor, Color.WHITE, 0.08f),
                ColorUtils.blendARGB(baseColor, Color.BLACK, 0.5f)
            )
        ).apply {
            cornerRadius = dpToPx(16).toFloat()
            setStroke(1, ColorUtils.setAlphaComponent(palette.specialAccent, 180))
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            }
            this.background = background
            setPadding(dpToPx(14), dpToPx(12), dpToPx(12), dpToPx(12))

            val textView = TextView(context).apply {
                this.text = display
                textSize = 14f
                setTextColor(Color.WHITE)
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            addView(textView)

            val deleteBtn = ImageView(context).apply {
                setImageResource(R.drawable.sym_keyboard_delete)
                setColorFilter(ColorUtils.setAlphaComponent(Color.WHITE, 200))
                layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28))
                setOnClickListener {
                    val history = prefs.getStringSet("clipboard_history", emptySet())?.toMutableSet() ?: mutableSetOf()
                    history.remove(text)
                    prefs.edit().putStringSet("clipboard_history", history).apply()
                    Toast.makeText(context, "Item removed", Toast.LENGTH_SHORT).show()
                    rebuildDynamicPanelsFromPrompts()
                }
            }
            addView(deleteBtn)

            setOnClickListener {
                val ic = inputConnectionProvider()
                ic?.commitText(text, 1)
                Toast.makeText(context, "Inserted from clipboard", Toast.LENGTH_SHORT).show()
                onBackToKeyboard()
            }
        }
    }
    
    // ========================================
    // AI PROCESSING METHODS
    // ========================================
    
    private fun processGrammarAction(action: GrammarAction) {
        val inputText = currentInputText
        if (inputText.isBlank()) {
            Toast.makeText(context, "Please type something first", Toast.LENGTH_SHORT).show()
            return
        }
        lastGrammarRequestedInput = inputText.trim()
        val card = grammarResultCard ?: return
        val shimmer = grammarShimmerContainer
        
        // Show shimmer, hide card
        card.container.visibility = View.GONE
        shimmer?.visibility = View.VISIBLE
        shimmer?.let { startShimmerAnimation(it) }
        
        card.translation.visibility = View.GONE
        card.translation.text = ""
        card.replaceButton.isEnabled = false
        grammarTranslationJob?.cancel()
        grammarTranslationJob = null

        scope.launch {
            val result = try {
                when {
                    action.feature != null -> advancedAIService.processText(inputText, action.feature)
                    !action.customPrompt.isNullOrBlank() -> advancedAIService.processCustomPrompt(
                        inputText,
                        action.customPrompt,
                        "grammar_${action.name.lowercase()}"
                    )
                    else -> advancedAIService.processText(
                        inputText,
                        AdvancedAIService.ProcessingFeature.GRAMMAR_FIX
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Hide shimmer, show card
                    shimmer?.clearAnimation()
                    shimmer?.visibility = View.GONE
                    card.container.visibility = View.VISIBLE
                    
                    card.primary.text = "❌ Error: ${e.message}"
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                if (result.success) {
                    displayGrammarResult(result.text)
                } else {
                    // Hide shimmer, show card
                    shimmer?.clearAnimation()
                    shimmer?.visibility = View.GONE
                    card.container.visibility = View.VISIBLE
                    
                    card.primary.text = "❌ Error: ${result.error}"
                }
            }
        }
    }

    private fun processGrammarCustomPrompt(prompt: String, title: String) {
        val inputText = currentInputText
        if (inputText.isBlank()) {
            Toast.makeText(context, "Please type something first", Toast.LENGTH_SHORT).show()
            return
        }
        lastGrammarRequestedInput = inputText.trim()
        val card = grammarResultCard ?: return
        val shimmer = grammarShimmerContainer
        
        // Show shimmer, hide card
        card.container.visibility = View.GONE
        shimmer?.visibility = View.VISIBLE
        shimmer?.let { startShimmerAnimation(it) }
        
        card.translation.visibility = View.GONE
        card.translation.text = ""
        card.replaceButton.isEnabled = false
        grammarTranslationJob?.cancel()
        grammarTranslationJob = null

        scope.launch {
            val result = try {
                advancedAIService.processCustomPrompt(
                    inputText,
                    prompt,
                    "grammar_custom_${title.hashCode()}"
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Hide shimmer, show card
                    shimmer?.clearAnimation()
                    shimmer?.visibility = View.GONE
                    card.container.visibility = View.VISIBLE
                    
                    card.primary.text = "❌ Error: ${e.message}"
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                if (result.success) {
                    displayGrammarResult(result.text)
                } else {
                    // Hide shimmer, show card
                    shimmer?.clearAnimation()
                    shimmer?.visibility = View.GONE
                    card.container.visibility = View.VISIBLE
                    
                    card.primary.text = "❌ Error: ${result.error}"
                }
            }
        }
    }

    private fun displayGrammarResult(text: String) {
        val card = grammarResultCard ?: return
        val shimmer = grammarShimmerContainer
        
        // Hide shimmer, show card
        shimmer?.clearAnimation()
        shimmer?.visibility = View.GONE
        card.container.visibility = View.VISIBLE
        
        lastGrammarResult = text
        lastGrammarRequestedInput = currentInputText.trim()
        card.primary.text = text
        card.replaceButton.isEnabled = true
        refreshGrammarTranslation()
    }

    private fun refreshGrammarTranslation() {
        val card = grammarResultCard ?: return
        grammarTranslationJob?.cancel()
        card.translation.text = ""
        card.translation.visibility = View.GONE

        if (!shouldTranslate(grammarLanguage) || lastGrammarResult.isBlank()) {
            return
        }

        card.translation.visibility = View.VISIBLE
        card.translation.text = context.getString(R.string.translation_loading, grammarLanguage.label)
        grammarTranslationJob = scope.launch {
            val result = advancedAIService.translateText(lastGrammarResult, grammarLanguage.value)
            withContext(Dispatchers.Main) {
                if (result.success && result.text.isNotBlank()) {
                    card.translation.text = result.text
                } else {
                    card.translation.text = context.getString(R.string.translation_error, grammarLanguage.label)
                }
            }
        }
    }

    private fun processToneAction(action: ToneAction) {
        requestToneVariants(action.prompt, "tone_${action.name.lowercase()}")
    }

    private fun processCustomTonePrompt(prompt: String, title: String) {
        requestToneVariants(prompt, "tone_custom_${title.hashCode()}")
    }

    private fun requestToneVariants(instruction: String, cacheKey: String) {
        val inputText = currentInputText
        if (inputText.isBlank()) {
            Toast.makeText(context, "Please type something first", Toast.LENGTH_SHORT).show()
            return
        }
        lastToneRequestedInput = inputText.trim()
        if (toneResultCards.isEmpty()) return
        val shimmer = toneShimmerContainer

        // Show shimmer, hide cards
        toneResultCards.forEach { it.container.visibility = View.GONE }
        shimmer?.visibility = View.VISIBLE
        shimmer?.let { startShimmerAnimation(it) }

        toneTranslationJobs.forEach { it.cancel() }
        toneTranslationJobs.clear()
        toneResultCards.forEachIndexed { index, card ->
            card.translation.visibility = View.GONE
            card.translation.text = ""
            if (index >= lastToneResults.size) {
                lastToneResults.add("")
            } else {
                lastToneResults[index] = ""
            }
        }

        scope.launch {
            val systemPrompt = """
                You are rewriting the user's message.
                Instruction: $instruction.
                Generate exactly ${toneResultCards.size} unique variations.
                Return each variation on its own line without numbering or bullet characters.
            """.trimIndent()

            val result = try {
                advancedAIService.processCustomPrompt(inputText, systemPrompt, cacheKey)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Hide shimmer, show cards
                    shimmer?.clearAnimation()
                    shimmer?.visibility = View.GONE
                    toneResultCards.forEach { it.container.visibility = View.VISIBLE }
                    
                    toneResultCards.firstOrNull()?.primary?.text = "❌ Error: ${e.message}"
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                // Hide shimmer, show cards
                shimmer?.clearAnimation()
                shimmer?.visibility = View.GONE
                toneResultCards.forEach { it.container.visibility = View.VISIBLE }
                
                if (result.success) {
                    val variants = parseVariants(result.text, toneResultCards.size)
                    toneResultCards.forEachIndexed { index, card ->
                        val text = variants.getOrNull(index).orEmpty()
                        card.primary.text = if (text.isBlank()) {
                            "⚠️ Couldn't create variation ${index + 1}"
                        } else {
                            text
                        }
                        if (index >= lastToneResults.size) {
                            lastToneResults.add(text)
                        } else {
                            lastToneResults[index] = text
                        }
                    }
                    refreshToneTranslations()
                    lastToneRequestedInput = currentInputText.trim()
                } else {
                    toneResultCards.firstOrNull()?.primary?.text = "❌ Error: ${result.error}"
                }
            }
        }
    }

    private fun refreshToneTranslations() {
        toneTranslationJobs.forEach { it.cancel() }
        toneTranslationJobs.clear()

        if (!shouldTranslate(toneLanguage)) {
            toneResultCards.forEach { it.translation.visibility = View.GONE }
            return
        }

        toneResultCards.forEachIndexed { index, card ->
            val text = lastToneResults.getOrNull(index).orEmpty()
            if (text.isBlank()) {
                card.translation.visibility = View.GONE
            } else {
                card.translation.visibility = View.VISIBLE
                card.translation.text = context.getString(R.string.translation_loading, toneLanguage.label)
                val job = scope.launch {
                    val translation = advancedAIService.translateText(text, toneLanguage.value)
                    withContext(Dispatchers.Main) {
                        if (translation.success && translation.text.isNotBlank()) {
                            card.translation.text = translation.text
                        } else {
                            card.translation.text = context.getString(R.string.translation_error, toneLanguage.label)
                        }
                    }
                }
                toneTranslationJobs.add(job)
            }
        }
    }

    private fun shouldTranslate(language: LanguageOption): Boolean {
        return !language.value.equals("English", ignoreCase = true)
    }

    private fun resolveTextForLanguage(
        primary: String,
        translation: CharSequence?,
        language: LanguageOption
    ): String {
        if (shouldTranslate(language)) {
            val loading = context.getString(R.string.translation_loading, language.label)
            val error = context.getString(R.string.translation_error, language.label)
            val candidate = translation?.toString()?.trim().orEmpty()
            if (candidate.isNotEmpty() && candidate != loading && candidate != error) {
                return candidate
            }
        }
        return primary
    }

    private fun parseVariants(raw: String, expected: Int): List<String> {
        val lines = raw
            .split('\n')
            .map { line ->
                line.trim()
                    .replace(Regex("^\\d+[.)\\s]+"), "")  // Remove "1. ", "1) ", "1 ", etc.
                    .trimStart('-', '•', '*', '·')  // Remove bullet points
                    .trim()
            }
            .filter { it.isNotEmpty() }

        return when {
            lines.size >= expected -> lines.take(expected)
            else -> lines
        }
    }
    
    private fun processAIAction(prompt: String, outputView: TextView) {
        val inputText = currentInputText
        if (inputText.isBlank()) {
            Toast.makeText(context, "Please type something first", Toast.LENGTH_SHORT).show()
            return
        }
        
        outputView.text = "⏳ Processing..."
        
        scope.launch {
            try {
                val result = advancedAIService.processText(
                    inputText,
                    AdvancedAIService.ProcessingFeature.SIMPLIFY
                )
                withContext(Dispatchers.Main) {
                    if (result.success) {
                        outputView.text = result.text
                        onTextProcessedCallback?.invoke(result.text)
                    } else {
                        outputView.text = "❌ Error: ${result.error}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    outputView.text = "❌ Error: ${e.message}"
                }
            }
        }
    }
    
    private fun processTranslateAction(targetLanguage: String, outputView: TextView) {
        val inputText = currentInputText
        if (inputText.isBlank()) {
            Toast.makeText(context, "Please type something first", Toast.LENGTH_SHORT).show()
            return
        }
        
        outputView.text = "⏳ Translating to $targetLanguage..."
        
        scope.launch {
            try {
                val result = advancedAIService.translateText(inputText, targetLanguage)
                withContext(Dispatchers.Main) {
                    if (result.success) {
                        outputView.text = result.text
                        onTextProcessedCallback?.invoke(result.text)
                    } else {
                        outputView.text = "❌ Error: ${result.error}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    outputView.text = "❌ Error: ${e.message}"
                }
            }
        }
    }

    private fun processCustomAIPrompt(
        prompt: String,
        title: String,
        statusText: TextView,
        resultContainer: LinearLayout,
        replaceButton: Button
    ) {
        val inputText = currentInputText
        if (inputText.isBlank()) {
            statusText.text = "Please type something first"
            return
        }

        statusText.text = "⏳ Processing with $title..."
        resultContainer.removeAllViews()
        replaceButton.visibility = View.GONE
        lastAiResult = ""

        scope.launch {
            try {
                val result = unifiedAIService.processCustomPrompt(
                    text = inputText,
                    prompt = prompt,
                    stream = false
                ).first()

                withContext(Dispatchers.Main) {
                    if (result.success && result.text.isNotBlank()) {
                        displayAIResults(result.text, statusText, resultContainer, replaceButton)
                    } else {
                        statusText.text = "❌ Error: ${result.error ?: "No response"}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "❌ Error: ${e.message}"
                }
            }
        }
    }

    private fun displayAIResults(
        text: String,
        statusText: TextView,
        resultContainer: LinearLayout,
        replaceButton: Button
    ) {
        val palette = PanelTheme.palette
        
        // Parse multiple results if newline-separated
        val results = text.split("\n")
            .map { it.trim().trimStart('-', '•', '*').trim() }
            .filter { it.isNotEmpty() }
            .take(3)

        if (results.isEmpty()) {
            statusText.text = "No results available"
            return
        }

        lastAiResult = results.first()
        statusText.text = "Tap a suggestion to use it"

        results.forEach { result ->
            val card = TextView(context).apply {
                this.text = result
                textSize = 13f
                setLineSpacing(0f, 1.1f)
                setTextColor(Color.WHITE)
                setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(14).toFloat()
                    setColor(ColorUtils.blendARGB(palette.keyBg, Color.BLACK, 0.45f))
                    setStroke(1, ColorUtils.setAlphaComponent(palette.specialAccent, 120))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(8)
                }
                setOnClickListener {
                    lastAiResult = result
                    onTextProcessedCallback?.invoke(result)
                    onBackToKeyboard()
                }
            }
            resultContainer.addView(card)
        }

        replaceButton.visibility = View.VISIBLE
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        advancedAIService.cleanup()
        unifiedAIService.cleanup()
        emojiPanelController = null
        grammarPanelView = null
        tonePanelView = null
        aiAssistantPanelView = null
        translatePanelView = null
        clipboardPanelView = null
        emojiPanelView = null
        settingsPanelView = null
        LogUtil.d(TAG, "UnifiedPanelManager cleaned up")
    }
}
