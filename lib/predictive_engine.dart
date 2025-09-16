import 'dart:convert';
import 'dart:math';
import 'package:shared_preferences/shared_preferences.dart';
import 'word_trie.dart';
import 'autocorrect_service.dart';

/// Predictive text engine with N-gram support and learning capabilities
class PredictiveTextEngine {
  static PredictiveTextEngine? _instance;
  static PredictiveTextEngine get instance => _instance ??= PredictiveTextEngine._();
  
  PredictiveTextEngine._();

  final WordTrie _dictionary = WordTrie();
  final Map<String, Map<String, int>> _bigrams = {};
  final Map<String, Map<String, int>> _trigrams = {};
  final Map<String, TypingPattern> _userPatterns = {};
  
  bool _isInitialized = false;
  SharedPreferences? _prefs;
  final List<String> _recentWords = [];
  static const int _maxRecentWords = 10;

  /// Initialize the predictive engine
  Future<void> initialize() async {
    if (_isInitialized) return;

    try {
      _prefs = await SharedPreferences.getInstance();
      
      // Initialize autocorrect service first (shares dictionary)
      await AutocorrectService.instance.initialize();
      
      // Load our own data
      await Future.wait([
        _loadDictionary(),
        _loadNGrams(),
        _loadUserPatterns(),
      ]);

      _isInitialized = true;
    } catch (e) {
      print('PredictiveTextEngine initialization error: $e');
      rethrow;
    }
  }

  /// Get word predictions based on current input and context
  Future<List<PredictionResult>> getPredictions({
    String currentWord = '',
    List<String> context = const [],
    int maxSuggestions = 5,
  }) async {
    if (!_isInitialized) await initialize();

    final List<PredictionResult> predictions = [];
    final Set<String> addedWords = {};

    // 1. Prefix-based predictions (highest priority for current typing)
    if (currentWord.isNotEmpty) {
      final prefixPredictions = await _getPrefixPredictions(currentWord, maxSuggestions);
      for (var prediction in prefixPredictions) {
        if (!addedWords.contains(prediction.word)) {
          predictions.add(prediction);
          addedWords.add(prediction.word);
        }
      }
    }

    // 2. Context-based predictions (N-gram)
    if (predictions.length < maxSuggestions && context.isNotEmpty) {
      final contextPredictions = await _getContextPredictions(context, maxSuggestions - predictions.length);
      for (var prediction in contextPredictions) {
        if (!addedWords.contains(prediction.word)) {
          predictions.add(prediction);
          addedWords.add(prediction.word);
        }
      }
    }

    // 3. User pattern predictions
    if (predictions.length < maxSuggestions) {
      final patternPredictions = await _getPatternPredictions(currentWord, context, maxSuggestions - predictions.length);
      for (var prediction in patternPredictions) {
        if (!addedWords.contains(prediction.word)) {
          predictions.add(prediction);
          addedWords.add(prediction.word);
        }
      }
    }

    // 4. Fallback to most frequent words
    if (predictions.length < maxSuggestions) {
      final frequentPredictions = await _getFrequentWords(maxSuggestions - predictions.length);
      for (var prediction in frequentPredictions) {
        if (!addedWords.contains(prediction.word)) {
          predictions.add(prediction);
          addedWords.add(prediction.word);
        }
      }
    }

    // Sort by confidence and return top results
    predictions.sort((a, b) => b.confidence.compareTo(a.confidence));
    return predictions.take(maxSuggestions).toList();
  }

  /// Learn from user typing patterns
  Future<void> learnFromInput(String word, List<String> context) async {
    if (!_isInitialized || word.isEmpty) return;

    final cleanWord = word.toLowerCase();
    
    // Add to recent words
    _recentWords.insert(0, cleanWord);
    if (_recentWords.length > _maxRecentWords) {
      _recentWords.removeLast();
    }

    // Learn bigrams
    if (context.isNotEmpty) {
      final prevWord = context.last.toLowerCase();
      _bigrams[prevWord] ??= {};
      _bigrams[prevWord]![cleanWord] = (_bigrams[prevWord]![cleanWord] ?? 0) + 1;
    }

    // Learn trigrams
    if (context.length >= 2) {
      final bigramKey = '${context[context.length - 2].toLowerCase()}_${context.last.toLowerCase()}';
      _trigrams[bigramKey] ??= {};
      _trigrams[bigramKey]![cleanWord] = (_trigrams[bigramKey]![cleanWord] ?? 0) + 1;
    }

    // Learn typing patterns
    _learnTypingPattern(cleanWord, context);

    // Periodically save learned data
    if (Random().nextDouble() < 0.1) { // 10% chance to save
      await _saveUserData();
    }
  }

  /// Get autocorrect suggestions integrated with predictions
  Future<List<PredictionResult>> getCorrectionsAndPredictions(String word, List<String> context) async {
    final List<PredictionResult> results = [];

    // Get autocorrect suggestion
    final correction = await AutocorrectService.instance.getCorrection(
      word,
      previousWord: context.isNotEmpty ? context.last : null,
    );

    if (correction.hasCorrection && correction.confidence > 0.5) {
      results.add(PredictionResult(
        word: correction.correctedWord,
        confidence: correction.confidence,
        source: PredictionSource.autocorrect,
        isCorrection: true,
      ));
    }

    // Get regular predictions
    final predictions = await getPredictions(
      currentWord: word,
      context: context,
      maxSuggestions: 4,
    );

    results.addAll(predictions);

    // Remove duplicates and sort
    final uniqueResults = <String, PredictionResult>{};
    for (var result in results) {
      if (!uniqueResults.containsKey(result.word) || 
          uniqueResults[result.word]!.confidence < result.confidence) {
        uniqueResults[result.word] = result;
      }
    }

    final sortedResults = uniqueResults.values.toList();
    sortedResults.sort((a, b) => b.confidence.compareTo(a.confidence));
    
    return sortedResults.take(5).toList();
  }

  /// Get completion suggestions for partial words
  Future<List<String>> getCompletions(String prefix, {int limit = 10}) async {
    if (!_isInitialized || prefix.isEmpty) return [];

    final suggestions = _dictionary.getWordsWithPrefix(prefix.toLowerCase(), limit: limit);
    return suggestions.map((s) => s.word).toList();
  }

  /// Get statistics about the predictive engine
  PredictiveStats getStats() {
    return PredictiveStats(
      dictionaryWords: _dictionary.getStats().wordCount,
      bigramCount: _bigrams.length,
      trigramCount: _trigrams.length,
      userPatterns: _userPatterns.length,
      recentWords: _recentWords.length,
      memoryUsageKB: _estimateMemoryUsage(),
    );
  }

  /// Clear all learned data
  Future<void> clearLearningData() async {
    _bigrams.clear();
    _trigrams.clear();
    _userPatterns.clear();
    _recentWords.clear();
    
    await _prefs?.remove('learned_bigrams');
    await _prefs?.remove('learned_trigrams');
    await _prefs?.remove('user_patterns');
  }

  // Private methods

  Future<void> _loadDictionary() async {
    // Dictionary is shared with AutocorrectService
    final stats = AutocorrectService.instance.getStats();
    if (stats.dictionaryWords > 0) {
      // Copy dictionary from autocorrect service
      // This is a simplified approach - in a real implementation,
      // you might want to share the actual Trie instance
      return;
    }
  }

  Future<void> _loadNGrams() async {
    try {
      // Load bigrams
      final String? bigramData = _prefs?.getString('learned_bigrams');
      if (bigramData != null) {
        final Map<String, dynamic> data = json.decode(bigramData);
        data.forEach((key, value) {
          if (value is Map<String, dynamic>) {
            _bigrams[key] = Map<String, int>.from(value);
          }
        });
      }

      // Load trigrams
      final String? trigramData = _prefs?.getString('learned_trigrams');
      if (trigramData != null) {
        final Map<String, dynamic> data = json.decode(trigramData);
        data.forEach((key, value) {
          if (value is Map<String, dynamic>) {
            _trigrams[key] = Map<String, int>.from(value);
          }
        });
      }
    } catch (e) {
      print('Error loading N-grams: $e');
    }
  }

  Future<void> _loadUserPatterns() async {
    try {
      final String? patternData = _prefs?.getString('user_patterns');
      if (patternData != null) {
        final Map<String, dynamic> data = json.decode(patternData);
        data.forEach((key, value) {
          if (value is Map<String, dynamic>) {
            _userPatterns[key] = TypingPattern.fromJson(value);
          }
        });
      }
    } catch (e) {
      print('Error loading user patterns: $e');
    }
  }

  Future<List<PredictionResult>> _getPrefixPredictions(String prefix, int limit) async {
    final suggestions = _dictionary.getWordsWithPrefix(prefix.toLowerCase(), limit: limit);
    
    return suggestions.map((suggestion) {
      // Calculate confidence based on frequency and prefix match
      final prefixRatio = prefix.length / suggestion.word.length;
      final frequencyScore = min(suggestion.frequency / 100000.0, 0.4);
      final confidence = (prefixRatio * 0.6) + frequencyScore;

      return PredictionResult(
        word: suggestion.word,
        confidence: confidence,
        source: PredictionSource.prefix,
      );
    }).toList();
  }

  Future<List<PredictionResult>> _getContextPredictions(List<String> context, int limit) async {
    final List<PredictionResult> predictions = [];

    if (context.isEmpty) return predictions;

    // Try trigram prediction first
    if (context.length >= 2) {
      final trigramKey = '${context[context.length - 2].toLowerCase()}_${context.last.toLowerCase()}';
      if (_trigrams.containsKey(trigramKey)) {
        final candidates = _trigrams[trigramKey]!;
        final sortedCandidates = candidates.entries.toList()
          ..sort((a, b) => b.value.compareTo(a.value));

        for (var entry in sortedCandidates.take(limit)) {
          final confidence = min(entry.value / 10.0, 0.9); // Cap at 0.9
          predictions.add(PredictionResult(
            word: entry.key,
            confidence: confidence,
            source: PredictionSource.trigram,
          ));
        }
      }
    }

    // Try bigram prediction
    if (predictions.length < limit) {
      final prevWord = context.last.toLowerCase();
      if (_bigrams.containsKey(prevWord)) {
        final candidates = _bigrams[prevWord]!;
        final sortedCandidates = candidates.entries.toList()
          ..sort((a, b) => b.value.compareTo(a.value));

        for (var entry in sortedCandidates.take(limit - predictions.length)) {
          final confidence = min(entry.value / 20.0, 0.8); // Cap at 0.8
          if (!predictions.any((p) => p.word == entry.key)) {
            predictions.add(PredictionResult(
              word: entry.key,
              confidence: confidence,
              source: PredictionSource.bigram,
            ));
          }
        }
      }
    }

    return predictions;
  }

  Future<List<PredictionResult>> _getPatternPredictions(String currentWord, List<String> context, int limit) async {
    final List<PredictionResult> predictions = [];

    // Use user typing patterns to predict next words
    for (var pattern in _userPatterns.values) {
      if (pattern.matches(currentWord, context)) {
        for (var nextWord in pattern.nextWords.entries) {
          final confidence = min(nextWord.value / pattern.totalOccurrences, 0.7);
          if (confidence > 0.1) {
            predictions.add(PredictionResult(
              word: nextWord.key,
              confidence: confidence,
              source: PredictionSource.pattern,
            ));
          }
        }
      }
    }

    predictions.sort((a, b) => b.confidence.compareTo(a.confidence));
    return predictions.take(limit).toList();
  }

  Future<List<PredictionResult>> _getFrequentWords(int limit) async {
    // Return most frequent recent words as fallback
    final recentSet = _recentWords.take(limit).toSet();
    return recentSet.map((word) => PredictionResult(
      word: word,
      confidence: 0.3,
      source: PredictionSource.frequent,
    )).toList();
  }

  void _learnTypingPattern(String word, List<String> context) {
    if (context.isEmpty) return;

    final patternKey = context.join('_');
    _userPatterns[patternKey] ??= TypingPattern(
      context: List.from(context),
      nextWords: {},
      totalOccurrences: 0,
    );

    final pattern = _userPatterns[patternKey]!;
    pattern.nextWords[word] = (pattern.nextWords[word] ?? 0) + 1;
    pattern.totalOccurrences++;

    // Limit pattern size to prevent memory bloat
    if (pattern.nextWords.length > 50) {
      final sortedWords = pattern.nextWords.entries.toList()
        ..sort((a, b) => b.value.compareTo(a.value));
      
      pattern.nextWords.clear();
      for (var entry in sortedWords.take(30)) {
        pattern.nextWords[entry.key] = entry.value;
      }
    }
  }

  Future<void> _saveUserData() async {
    try {
      // Save bigrams
      await _prefs?.setString('learned_bigrams', json.encode(_bigrams));
      
      // Save trigrams
      await _prefs?.setString('learned_trigrams', json.encode(_trigrams));
      
      // Save user patterns
      final patternsJson = <String, dynamic>{};
      _userPatterns.forEach((key, pattern) {
        patternsJson[key] = pattern.toJson();
      });
      await _prefs?.setString('user_patterns', json.encode(patternsJson));
    } catch (e) {
      print('Error saving user data: $e');
    }
  }

  double _estimateMemoryUsage() {
    double usage = 0.0;
    
    // Estimate bigrams memory
    _bigrams.forEach((key, value) {
      usage += key.length * 2; // String overhead
      usage += value.length * 8; // Map entries
    });
    
    // Estimate trigrams memory
    _trigrams.forEach((key, value) {
      usage += key.length * 2;
      usage += value.length * 8;
    });
    
    // Estimate patterns memory
    _userPatterns.forEach((key, pattern) {
      usage += key.length * 2;
      usage += pattern.nextWords.length * 8;
    });
    
    return usage / 1024; // Convert to KB
  }
}

/// Result of a prediction operation
class PredictionResult {
  final String word;
  final double confidence;
  final PredictionSource source;
  final bool isCorrection;

  const PredictionResult({
    required this.word,
    required this.confidence,
    required this.source,
    this.isCorrection = false,
  });

  @override
  String toString() {
    return 'PredictionResult($word, conf: ${confidence.toStringAsFixed(2)}, src: $source)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        other is PredictionResult &&
            runtimeType == other.runtimeType &&
            word == other.word;
  }

  @override
  int get hashCode => word.hashCode;
}

/// Source of the prediction
enum PredictionSource {
  prefix,
  bigram,
  trigram,
  pattern,
  frequent,
  autocorrect,
}

/// User typing pattern for learning
class TypingPattern {
  final List<String> context;
  final Map<String, int> nextWords;
  int totalOccurrences;

  TypingPattern({
    required this.context,
    required this.nextWords,
    required this.totalOccurrences,
  });

  bool matches(String currentWord, List<String> inputContext) {
    if (context.length != inputContext.length) return false;
    
    for (int i = 0; i < context.length; i++) {
      if (context[i].toLowerCase() != inputContext[i].toLowerCase()) {
        return false;
      }
    }
    
    return true;
  }

  Map<String, dynamic> toJson() {
    return {
      'context': context,
      'nextWords': nextWords,
      'totalOccurrences': totalOccurrences,
    };
  }

  factory TypingPattern.fromJson(Map<String, dynamic> json) {
    return TypingPattern(
      context: List<String>.from(json['context'] ?? []),
      nextWords: Map<String, int>.from(json['nextWords'] ?? {}),
      totalOccurrences: json['totalOccurrences'] ?? 0,
    );
  }
}

/// Statistics about the predictive engine
class PredictiveStats {
  final int dictionaryWords;
  final int bigramCount;
  final int trigramCount;
  final int userPatterns;
  final int recentWords;
  final double memoryUsageKB;

  const PredictiveStats({
    required this.dictionaryWords,
    required this.bigramCount,
    required this.trigramCount,
    required this.userPatterns,
    required this.recentWords,
    required this.memoryUsageKB,
  });

  @override
  String toString() {
    return 'PredictiveStats(dict: $dictionaryWords, bigrams: $bigramCount, trigrams: $trigramCount, patterns: $userPatterns, memory: ${memoryUsageKB.toStringAsFixed(1)}KB)';
  }
}
