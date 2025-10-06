import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

// Language preference constants (reuse across Flutter/Kotlin)
const kEnabledLanguagesKey = 'flutter.enabled_languages';
const kDefaultLanguageKey = 'flutter.default_language';
const kCurrentLanguageKey = 'flutter.current_language';
const kMultilingualEnabledKey = 'flutter.multilingual_enabled';

class LanguageScreen extends StatefulWidget {
  const LanguageScreen({Key? key}) : super(key: key);

  @override
  State<LanguageScreen> createState() => _LanguageScreenState();
}

class _LanguageScreenState extends State<LanguageScreen> {
  // MethodChannel for language configuration
  static const _configChannel = MethodChannel('ai_keyboard/config');
  
  // Track selected language codes (e.g., ['en', 'hi', 'te'])
  List<String> selectedLanguages = [];
  
  // Multilingual mode toggle
  bool _multilingualEnabled = false;
  
  // Phase 2: Transliteration toggles
  bool _transliterationEnabled = true;
  bool _reverseTransliterationEnabled = false;
  
  // Loading state
  bool _isLoading = true;
  
  // Language code to display name mapping
  final Map<String, String> _languageMap = {
    'en': 'English',
    'hi': 'Hindi',
    'te': 'Telugu',
    'ta': 'Tamil',
    'mr': 'Marathi',
    'bn': 'Bengali',
    'gu': 'Gujarati',
    'kn': 'Kannada',
    'ml': 'Malayalam',
    'pa': 'Punjabi',
    'ur': 'Urdu',
    'es': 'Spanish',
    'fr': 'French',
    'de': 'German',
  };
  
  @override
  void initState() {
    super.initState();
    _loadLanguagePreferences();
  }
  
  /// Load language preferences from SharedPreferences
  Future<void> _loadLanguagePreferences() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      // Load enabled languages (Flutter uses "flutter." prefix for FlutterSharedPreferences)
      final enabledLangs = prefs.getStringList(kEnabledLanguagesKey) ?? ['en'];
      final multiEnabled = prefs.getBool(kMultilingualEnabledKey) ?? false;
      
      // Phase 2: Load transliteration toggles
      final translitEnabled = prefs.getBool('transliteration_enabled') ?? true;
      final reverseEnabled = prefs.getBool('reverse_transliteration_enabled') ?? false;
      
      setState(() {
        selectedLanguages = enabledLangs;
        _multilingualEnabled = multiEnabled;
        _transliterationEnabled = translitEnabled;
        _reverseTransliterationEnabled = reverseEnabled;
        _isLoading = false;
      });
    } catch (e) {
      debugPrint('Error loading language preferences: $e');
      setState(() {
        selectedLanguages = ['en']; // Default to English
        _isLoading = false;
      });
    }
  }
  
  /// Save languages and notify Kotlin via MethodChannel
  Future<void> _saveLanguagesAndNotify() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      
      // Save to SharedPreferences
      await prefs.setStringList(kEnabledLanguagesKey, selectedLanguages);
      
      // Set default language to first in list if not set
      if ((prefs.getString(kDefaultLanguageKey) ?? '').isEmpty && selectedLanguages.isNotEmpty) {
        await prefs.setString(kDefaultLanguageKey, selectedLanguages.first);
      }
      
      // Set current language if not set
      final currentLang = prefs.getString(kCurrentLanguageKey);
      if (currentLang == null || currentLang.isEmpty) {
        await prefs.setString(kCurrentLanguageKey, selectedLanguages.isNotEmpty ? selectedLanguages.first : 'en');
      }
      
      // Notify Kotlin via MethodChannel
      final current = prefs.getString(kCurrentLanguageKey) ?? (selectedLanguages.isNotEmpty ? selectedLanguages.first : 'en');
      await _configChannel.invokeMethod('setEnabledLanguages', {
        'enabled': selectedLanguages,
        'current': current,
      });
      
      // Show success feedback
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Language settings saved'),
            duration: Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      debugPrint('Error saving language preferences: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error saving: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }
  
  /// Toggle multilingual mode
  Future<void> _toggleMultilingual(bool enabled) async {
    setState(() => _multilingualEnabled = enabled);
    
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool(kMultilingualEnabledKey, enabled);
      
      await _configChannel.invokeMethod('setMultilingual', {'enabled': enabled});
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(enabled ? 'Multilingual mode enabled' : 'Multilingual mode disabled'),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      debugPrint('Error toggling multilingual: $e');
    }
  }
  
  /// Phase 2: Toggle transliteration (Roman → Native)
  Future<void> _toggleTransliteration(bool enabled) async {
    setState(() {
      _transliterationEnabled = enabled;
      // Disable reverse if forward is enabled
      if (enabled) _reverseTransliterationEnabled = false;
    });
    
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('transliteration_enabled', enabled);
      if (enabled) {
        await prefs.setBool('reverse_transliteration_enabled', false);
      }
      
      await _configChannel.invokeMethod('setTransliterationEnabled', {'enabled': enabled});
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(enabled ? 'Transliteration enabled' : 'Transliteration disabled'),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      debugPrint('Error toggling transliteration: $e');
    }
  }
  
  /// Phase 2: Toggle reverse transliteration (Native → Roman)
  Future<void> _toggleReverseTransliteration(bool enabled) async {
    setState(() {
      _reverseTransliterationEnabled = enabled;
      // Disable forward if reverse is enabled
      if (enabled) _transliterationEnabled = false;
    });
    
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('reverse_transliteration_enabled', enabled);
      if (enabled) {
        await prefs.setBool('transliteration_enabled', false);
      }
      
      await _configChannel.invokeMethod('setReverseTransliterationEnabled', {'enabled': enabled});
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(enabled ? 'Reverse transliteration enabled' : 'Reverse transliteration disabled'),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      debugPrint('Error toggling reverse transliteration: $e');
    }
  }
  
  /// Add a language
  void _addLanguage(String langCode) {
    if (!selectedLanguages.contains(langCode)) {
      setState(() {
        selectedLanguages.add(langCode);
      });
      _saveLanguagesAndNotify();
    }
  }
  
  /// Remove a language
  void _removeLanguage(String langCode) {
    if (selectedLanguages.length > 1) { // Keep at least one language
      setState(() {
        selectedLanguages.remove(langCode);
      });
      _saveLanguagesAndNotify();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('At least one language must be selected'),
          duration: Duration(seconds: 2),
        ),
      );
    }
  }
  
  /// Get display name for language code
  String _getLanguageName(String code) {
    return _languageMap[code] ?? code.toUpperCase();
  }
  
  /// Get available (not selected) languages
  List<String> _getAvailableLanguageCodes() {
    return _languageMap.keys.where((code) => !selectedLanguages.contains(code)).toList();
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return Scaffold(
        backgroundColor: AppColors.white,
        appBar: AppBar(
          toolbarHeight: 70,
          backgroundColor: AppColors.primary,
          elevation: 0,
          leading: IconButton(
            icon: const Icon(Icons.arrow_back, color: AppColors.white),
            onPressed: () => Navigator.pop(context),
          ),
          title: Text(
            'Language',
            style: AppTextStyle.headlineMedium.copyWith(color: AppColors.white),
          ),
        ),
        body: const Center(child: CircularProgressIndicator()),
      );
    }
    
    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        toolbarHeight: 70,
        backgroundColor: AppColors.primary,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Language',
          style: AppTextStyle.headlineMedium.copyWith(color: AppColors.white),
        ),
        actions: [
          IconButton(
            icon: const Icon(
              Icons.notifications_outlined,
              color: AppColors.white,
            ),
            onPressed: () {},
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Language Information Card
            _buildLanguageInfoCard(),

            const SizedBox(height: 24),
            
            // Multilingual Mode Toggle
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.lightGrey,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(
                    'Use multiple languages at once',
                    style: AppTextStyle.headlineSmall,
                  ),
                  subtitle: Text(
                    'Get suggestions from top 2 languages simultaneously',
                    style: AppTextStyle.bodyMedium.copyWith(color: AppColors.grey),
                  ),
                  value: _multilingualEnabled,
                  onChanged: _toggleMultilingual,
                  activeColor: AppColors.secondary,
                ),
              ),
            ),
            
            // Phase 2: Transliteration Toggles
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 24),
              decoration: BoxDecoration(
                color: Theme.of(context).cardColor,
                borderRadius: BorderRadius.circular(12),
              ),
              child: SwitchListTile(
                title: const Text('Transliteration (Roman → Native)'),
                subtitle: const Text('Type in English to get native script'),
                value: _transliterationEnabled,
                onChanged: _toggleTransliteration,
                activeColor: AppColors.secondary,
              ),
            ),
            
            const SizedBox(height: 12),
            
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 24),
              decoration: BoxDecoration(
                color: Theme.of(context).cardColor,
                borderRadius: BorderRadius.circular(12),
              ),
              child: SwitchListTile(
                title: const Text('Reverse Transliteration (Native → Roman)'),
                subtitle: const Text('Convert native script back to English'),
                value: _reverseTransliterationEnabled,
                onChanged: _toggleReverseTransliteration,
                activeColor: AppColors.secondary,
              ),
            ),

            const SizedBox(height: 24),

            // Selected Languages Section
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Selected Languages',
                    style: AppTextStyle.headlineSmall,
                  ),
                  Text(
                    '${selectedLanguages.length} active',
                    style: AppTextStyle.bodyMedium.copyWith(color: AppColors.grey),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Selected Languages List (Reorderable)
            if (selectedLanguages.isEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                child: Text(
                  'No languages selected',
                  style: AppTextStyle.bodyMedium.copyWith(color: AppColors.grey),
                ),
              )
            else
              ...selectedLanguages.asMap().entries.map(
                (entry) => _buildSelectedLanguageTile(entry.value, entry.key),
              ),

            const SizedBox(height: 24),

            // Available Languages Section
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Text(
                'Available Languages',
                style: AppTextStyle.headlineSmall,
              ),
            ),
            const SizedBox(height: 16),

            // Available Languages List
            ..._getAvailableLanguageCodes().map(
              (langCode) => _buildAvailableLanguageTile(langCode),
            ),

            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _buildLanguageInfoCard() {
    return Container(
      margin: const EdgeInsets.all(24),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          // Left language icon
          Image.asset(AppIcons.languages, width: 24, height: 24),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Language', style: AppTextStyle.headlineSmall),
                const SizedBox(height: 4),
                Text(
                  'Select multiple languages',
                  style: AppTextStyle.bodyMedium.copyWith(
                    color: AppColors.grey,
                  ),
                ),
              ],
            ),
          ),
          // Right language icon
          Image.asset(AppAssets.languages_image, width: 40, height: 40),
        ],
      ),
    );
  }

  Widget _buildSelectedLanguageTile(String langCode, int index) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
      child: _buildTileOption(
        title: _getLanguageName(langCode),
        subtitle: index == 0 ? 'Default • QWERTY' : 'QWERTY',
        icon: Icons.check_box,
        isSelected: true,
        onTap: () => _removeLanguage(langCode),
        trailing: index == 0
            ? Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: AppColors.secondary.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(
                  'DEFAULT',
                  style: AppTextStyle.bodySmall.copyWith(
                    color: AppColors.secondary,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              )
            : null,
      ),
    );
  }

  Widget _buildAvailableLanguageTile(String langCode) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
      child: _buildTileOption(
        title: _getLanguageName(langCode),
        subtitle: 'QWERTY',
        icon: Icons.add_circle_outline,
        isDownloadable: true,
        onTap: () => _addLanguage(langCode),
      ),
    );
  }

  Widget _buildTileOption({
    required String title,
    required String subtitle,
    required IconData icon,
    required VoidCallback onTap,
    bool isSelected = false,
    bool isDownloadable = false,
    Widget? trailing,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(12),
      ),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
        minTileHeight: 72,
        onTap: onTap,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        leading: _buildLeadingIcon(icon, isSelected, isDownloadable),
        title: Text(title, style: AppTextStyle.headlineSmall),
        subtitle: Text(
          subtitle,
          style: AppTextStyle.bodyMedium.copyWith(color: AppColors.grey),
        ),
        trailing: trailing ??
            (isDownloadable
                ? Image.asset(AppIcons.download_button, width: 24, height: 24)
                : null),
      ),
    );
  }

  Widget _buildLeadingIcon(IconData icon, bool isSelected, bool isDownloadable) {
    if (isSelected) {
      return Container(
        width: 24,
        height: 24,
        decoration: BoxDecoration(
          color: AppColors.secondary,
          borderRadius: BorderRadius.circular(4),
        ),
        child: const Icon(Icons.check, color: AppColors.white, size: 16),
      );
    } else if (isDownloadable) {
      return Container(
        width: 24,
        height: 24,
        decoration: BoxDecoration(
          color: AppColors.secondary,
          borderRadius: BorderRadius.circular(12),
        ),
        child: const Icon(Icons.add, color: AppColors.white, size: 16),
      );
    } else {
      return Icon(icon, color: AppColors.grey, size: 24);
    }
  }
}
