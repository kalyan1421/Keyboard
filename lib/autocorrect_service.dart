import 'dart:convert';
import 'dart:math';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'word_trie.dart';

/// Service for autocorrect functionality with caching and learning
class AutocorrectService {
  static AutocorrectService? _instance;
  static AutocorrectService get instance => _instance ??= AutocorrectService._();
  
  AutocorrectService._();

  final WordTrie _dictionary = WordTrie();
  final Map<String, String> _corrections = {};
  final Map<String, List<String>> _contractions = {};
  final Map<String, CorrectionCache> _cache = {};
  
  bool _isInitialized = false;
  SharedPreferences? _prefs;

  /// Initialize the autocorrect service
  Future<void> initialize() async {
    if (_isInitialized) return;

    try {
      _prefs = await SharedPreferences.getInstance();
      
      // Load dictionaries in parallel
      await Future.wait([
        _loadCommonWords(),
        _loadCorrections(),
        _loadUserCache(),
      ]);

      _isInitialized = true;
    } catch (e) {
      print('AutocorrectService initialization error: $e');
      rethrow;
    }
  }

  /// Get autocorrect suggestions for a word
  Future<AutocorrectResult> getCorrection(String word, {String? previousWord}) async {
    if (!_isInitialized) await initialize();
    if (word.isEmpty) return AutocorrectResult.noCorrection(word);

    final String cleanWord = _cleanWord(word);
    final String lowerWord = cleanWord.toLowerCase();

    // Check cache first
    if (_cache.containsKey(lowerWord)) {
      final cached = _cache[lowerWord]!;
      if (DateTime.now().difference(cached.timestamp).inHours < 24) {
        return AutocorrectResult(
          originalWord: word,
          correctedWord: cached.correction,
          confidence: cached.confidence,
          source: CorrectionSource.cache,
        );
      }
    }

    // Check if word is already correct
    if (_dictionary.contains(lowerWord)) {
      return AutocorrectResult.noCorrection(word);
    }

    // Check predefined corrections
    if (_corrections.containsKey(lowerWord)) {
      final correction = _corrections[lowerWord]!;
      final confidence = _calculateCorrectionConfidence(lowerWord, correction);
      
      _cacheCorrection(lowerWord, correction, confidence);
      
      return AutocorrectResult(
        originalWord: word,
        correctedWord: _preserveCase(word, correction),
        confidence: confidence,
        source: CorrectionSource.dictionary,
      );
    }

    // Try Levenshtein distance correction
    final levenshteinResult = await _findLevenshteinCorrection(lowerWord);
    if (levenshteinResult != null) {
      _cacheCorrection(lowerWord, levenshteinResult.correctedWord, levenshteinResult.confidence);
      return AutocorrectResult(
        originalWord: word,
        correctedWord: _preserveCase(word, levenshteinResult.correctedWord),
        confidence: levenshteinResult.confidence,
        source: CorrectionSource.levenshtein,
      );
    }

    // Try contextual correction if previous word is provided
    if (previousWord != null) {
      final contextResult = await _findContextualCorrection(lowerWord, previousWord);
      if (contextResult != null) {
        return AutocorrectResult(
          originalWord: word,
          correctedWord: _preserveCase(word, contextResult.correctedWord),
          confidence: contextResult.confidence,
          source: CorrectionSource.context,
        );
      }
    }

    return AutocorrectResult.noCorrection(word);
  }

  /// Handle contractions (e.g., "don't" -> ["do", "not"])
  List<String>? expandContraction(String word) {
    final lowerWord = word.toLowerCase();
    return _contractions[lowerWord];
  }

  /// Check if a word should be auto-corrected based on confidence threshold
  bool shouldAutoCorrect(AutocorrectResult result, {double threshold = 0.8}) {
    return result.hasCorrection && result.confidence >= threshold;
  }

  /// Learn from user corrections to improve future suggestions
  Future<void> learnCorrection(String original, String corrected, {bool userConfirmed = true}) async {
    if (!_isInitialized) return;

    final lowerOriginal = original.toLowerCase();
    final lowerCorrected = corrected.toLowerCase();

    if (lowerOriginal == lowerCorrected) return;

    // Update cache with high confidence for user-confirmed corrections
    final confidence = userConfirmed ? 0.95 : 0.7;
    _cacheCorrection(lowerOriginal, lowerCorrected, confidence);

    // Save to persistent storage
    await _saveUserCorrection(lowerOriginal, lowerCorrected, confidence);
  }

  /// Get statistics about the autocorrect service
  AutocorrectStats getStats() {
    return AutocorrectStats(
      dictionaryWords: _dictionary.getStats().wordCount,
      correctionRules: _corrections.length,
      cachedCorrections: _cache.length,
      contractions: _contractions.length,
      memoryUsageKB: _dictionary.getStats().memoryUsageKB,
    );
  }

  /// Clear all cached corrections
  Future<void> clearCache() async {
    _cache.clear();
    await _prefs?.remove('user_corrections');
  }

  // Private methods

  Future<void> _loadCommonWords() async {
    try {
      final String jsonString = await rootBundle.loadString('assets/dictionaries/common_words.json');
      final Map<String, dynamic> data = json.decode(jsonString);
      
      final List<String> words = List<String>.from(data['words'] ?? []);
      final Map<String, dynamic> frequencies = Map<String, dynamic>.from(data['frequency'] ?? {});
      
      for (String word in words) {
        final frequency = frequencies[word] ?? 1;
        _dictionary.insert(word, frequency: frequency as int);
      }
    } catch (e) {
      print('Error loading common words: $e');
    }
  }

  Future<void> _loadCorrections() async {
    try {
      final String jsonString = await rootBundle.loadString('assets/dictionaries/corrections.json');
      final Map<String, dynamic> data = json.decode(jsonString);
      
      final Map<String, dynamic> corrections = Map<String, dynamic>.from(data['corrections'] ?? {});
      final Map<String, dynamic> contractions = Map<String, dynamic>.from(data['contractions'] ?? {});
      
      // Load corrections
      corrections.forEach((key, value) {
        _corrections[key.toLowerCase()] = value.toString();
      });
      
      // Load contractions
      contractions.forEach((key, value) {
        if (value is List) {
          _contractions[key.toLowerCase()] = List<String>.from(value);
        }
      });
    } catch (e) {
      print('Error loading corrections: $e');
    }
  }

  Future<void> _loadUserCache() async {
    try {
      final String? cacheData = _prefs?.getString('user_corrections');
      if (cacheData != null) {
        final Map<String, dynamic> cache = json.decode(cacheData);
        
        cache.forEach((key, value) {
          if (value is Map<String, dynamic>) {
            _cache[key] = CorrectionCache(
              correction: value['correction'] ?? '',
              confidence: (value['confidence'] ?? 0.0).toDouble(),
              timestamp: DateTime.fromMillisecondsSinceEpoch(value['timestamp'] ?? 0),
            );
          }
        });
      }
    } catch (e) {
      print('Error loading user cache: $e');
    }
  }

  Future<AutocorrectResult?> _findLevenshteinCorrection(String word) async {
    if (word.length < 2) return null;

    final suggestions = _dictionary.getSuggestions(word, limit: 5, maxDistance: 2);
    
    if (suggestions.isEmpty) return null;

    final best = suggestions.first;
    final distance = _calculateLevenshteinDistance(word, best.word);
    
    // Calculate confidence based on edit distance and word frequency
    final maxDistance = max(word.length, best.word.length);
    final distanceRatio = 1.0 - (distance / maxDistance);
    final frequencyBonus = min(best.frequency / 100000.0, 0.3); // Cap at 0.3
    final confidence = (distanceRatio * 0.7) + frequencyBonus;

    if (confidence < 0.3) return null;

    return AutocorrectResult(
      originalWord: word,
      correctedWord: best.word,
      confidence: confidence,
      source: CorrectionSource.levenshtein,
    );
  }

  Future<AutocorrectResult?> _findContextualCorrection(String word, String previousWord) async {
    // Simple contextual correction based on common word pairs
    // This could be expanded with n-gram models
    
    final contextPairs = {
      'i': {'am', 'was', 'will', 'have', 'had'},
      'you': {'are', 'were', 'will', 'have', 'had'},
      'he': {'is', 'was', 'will', 'has', 'had'},
      'she': {'is', 'was', 'will', 'has', 'had'},
      'we': {'are', 'were', 'will', 'have', 'had'},
      'they': {'are', 'were', 'will', 'have', 'had'},
    };

    final prevLower = previousWord.toLowerCase();
    if (contextPairs.containsKey(prevLower)) {
      final expectedWords = contextPairs[prevLower]!;
      
      for (String expected in expectedWords) {
        final distance = _calculateLevenshteinDistance(word, expected);
        if (distance <= 2 && distance > 0) {
          final confidence = 0.6 - (distance * 0.1);
          return AutocorrectResult(
            originalWord: word,
            correctedWord: expected,
            confidence: confidence,
            source: CorrectionSource.context,
          );
        }
      }
    }

    return null;
  }

  int _calculateLevenshteinDistance(String s1, String s2) {
    if (s1.isEmpty) return s2.length;
    if (s2.isEmpty) return s1.length;

    List<List<int>> dp = List.generate(
      s1.length + 1,
      (i) => List.filled(s2.length + 1, 0),
    );

    for (int i = 0; i <= s1.length; i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= s2.length; j++) {
      dp[0][j] = j;
    }

    for (int i = 1; i <= s1.length; i++) {
      for (int j = 1; j <= s2.length; j++) {
        if (s1[i - 1] == s2[j - 1]) {
          dp[i][j] = dp[i - 1][j - 1];
        } else {
          dp[i][j] = 1 + [
            dp[i - 1][j],
            dp[i][j - 1],
            dp[i - 1][j - 1],
          ].reduce((a, b) => a < b ? a : b);
        }
      }
    }

    return dp[s1.length][s2.length];
  }

  double _calculateCorrectionConfidence(String original, String correction) {
    final distance = _calculateLevenshteinDistance(original, correction);
    final maxLength = max(original.length, correction.length);
    
    if (maxLength == 0) return 0.0;
    
    final similarity = 1.0 - (distance / maxLength);
    return max(0.5, similarity); // Minimum confidence of 0.5 for dictionary corrections
  }

  String _cleanWord(String word) {
    // Remove punctuation but preserve apostrophes for contractions
    return word.replaceAll(RegExp(r"[^\w']"), '');
  }

  String _preserveCase(String original, String corrected) {
    if (original.isEmpty || corrected.isEmpty) return corrected;

    // If original is all uppercase, make correction uppercase
    if (original == original.toUpperCase()) {
      return corrected.toUpperCase();
    }

    // If original starts with uppercase, capitalize correction
    if (original[0] == original[0].toUpperCase()) {
      return corrected[0].toUpperCase() + corrected.substring(1);
    }

    return corrected;
  }

  void _cacheCorrection(String original, String correction, double confidence) {
    _cache[original] = CorrectionCache(
      correction: correction,
      confidence: confidence,
      timestamp: DateTime.now(),
    );

    // Limit cache size
    if (_cache.length > 1000) {
      final oldest = _cache.entries
          .reduce((a, b) => a.value.timestamp.isBefore(b.value.timestamp) ? a : b);
      _cache.remove(oldest.key);
    }
  }

  Future<void> _saveUserCorrection(String original, String corrected, double confidence) async {
    try {
      final Map<String, dynamic> userData = {};
      
      // Load existing data
      final String? existingData = _prefs?.getString('user_corrections');
      if (existingData != null) {
        userData.addAll(json.decode(existingData));
      }

      // Add new correction
      userData[original] = {
        'correction': corrected,
        'confidence': confidence,
        'timestamp': DateTime.now().millisecondsSinceEpoch,
      };

      // Save back to preferences
      await _prefs?.setString('user_corrections', json.encode(userData));
    } catch (e) {
      print('Error saving user correction: $e');
    }
  }
}

/// Result of an autocorrect operation
class AutocorrectResult {
  final String originalWord;
  final String correctedWord;
  final double confidence;
  final CorrectionSource source;

  const AutocorrectResult({
    required this.originalWord,
    required this.correctedWord,
    required this.confidence,
    required this.source,
  });

  bool get hasCorrection => originalWord.toLowerCase() != correctedWord.toLowerCase();

  factory AutocorrectResult.noCorrection(String word) {
    return AutocorrectResult(
      originalWord: word,
      correctedWord: word,
      confidence: 0.0,
      source: CorrectionSource.none,
    );
  }

  @override
  String toString() {
    return 'AutocorrectResult($originalWord -> $correctedWord, conf: ${confidence.toStringAsFixed(2)}, source: $source)';
  }
}

/// Source of the correction
enum CorrectionSource {
  none,
  dictionary,
  levenshtein,
  context,
  cache,
}

/// Cache entry for corrections
class CorrectionCache {
  final String correction;
  final double confidence;
  final DateTime timestamp;

  const CorrectionCache({
    required this.correction,
    required this.confidence,
    required this.timestamp,
  });
}

/// Statistics about the autocorrect service
class AutocorrectStats {
  final int dictionaryWords;
  final int correctionRules;
  final int cachedCorrections;
  final int contractions;
  final double memoryUsageKB;

  const AutocorrectStats({
    required this.dictionaryWords,
    required this.correctionRules,
    required this.cachedCorrections,
    required this.contractions,
    required this.memoryUsageKB,
  });

  @override
  String toString() {
    return 'AutocorrectStats(dict: $dictionaryWords, rules: $correctionRules, cache: $cachedCorrections, memory: ${memoryUsageKB.toStringAsFixed(1)}KB)';
  }
}
