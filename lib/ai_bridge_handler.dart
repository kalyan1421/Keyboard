import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'autocorrect_service.dart';
import 'predictive_engine.dart';

/// Bridge handler for connecting Flutter AI services with Android system keyboard
class AIBridgeHandler {
  static const MethodChannel _channel = MethodChannel('ai_keyboard/bridge');
  static bool _initialized = false;

  /// Initialize the bridge handler
  static Future<void> initialize() async {
    if (_initialized) return;

    // Set up method call handler
    _channel.setMethodCallHandler(_handleMethodCall);

    try {
      // Initialize AI services
      await Future.wait([
        AutocorrectService.instance.initialize(),
        PredictiveTextEngine.instance.initialize(),
      ]);

      _initialized = true;
      print('AI Bridge Handler initialized successfully');

      // Notify Android that services are ready
      await _channel.invokeMethod('initialized');
    } catch (e) {
      print('Failed to initialize AI Bridge Handler: $e');
      rethrow;
    }
  }

  /// Handle method calls from Android
  static Future<dynamic> _handleMethodCall(MethodCall call) async {
    try {
      switch (call.method) {
        case 'initialize':
          return await _handleInitialize();

        case 'getSuggestions':
          return await _handleGetSuggestions(call.arguments);

        case 'getCorrection':
          return await _handleGetCorrection(call.arguments);

        case 'learnFromInput':
          return await _handleLearnFromInput(call.arguments);

        case 'learnCorrection':
          return await _handleLearnCorrection(call.arguments);

        case 'getStats':
          return await _handleGetStats();

        case 'clearLearningData':
          return await _handleClearLearningData();

        default:
          throw PlatformException(
            code: 'UNIMPLEMENTED',
            message: 'Method ${call.method} not implemented',
          );
      }
    } catch (e) {
      print('Error handling method call ${call.method}: $e');
      throw PlatformException(
        code: 'ERROR',
        message: e.toString(),
      );
    }
  }

  /// Handle initialization request
  static Future<Map<String, dynamic>> _handleInitialize() async {
    if (!_initialized) {
      await initialize();
    }
    return {'status': 'initialized'};
  }

  /// Handle get suggestions request
  static Future<List<Map<String, dynamic>>> _handleGetSuggestions(
      dynamic arguments) async {
    if (!_initialized) {
      throw PlatformException(
        code: 'NOT_INITIALIZED',
        message: 'AI services not initialized',
      );
    }

    final Map<String, dynamic> args = Map<String, dynamic>.from(arguments);
    final String currentWord = args['currentWord'] ?? '';
    final List<String> context = List<String>.from(args['context'] ?? []);
    final int maxSuggestions = args['maxSuggestions'] ?? 5;

    try {
      // Get combined predictions and corrections
      final predictions = await PredictiveTextEngine.instance
          .getCorrectionsAndPredictions(currentWord, context);

      // Convert to format expected by Android
      final List<Map<String, dynamic>> suggestions = predictions
          .take(maxSuggestions)
          .map((prediction) => {
                'word': prediction.word,
                'confidence': prediction.confidence,
                'source': prediction.source.toString().split('.').last,
                'isCorrection': prediction.isCorrection,
              })
          .toList();

      return suggestions;
    } catch (e) {
      throw PlatformException(
        code: 'SUGGESTION_ERROR',
        message: 'Failed to get suggestions: $e',
      );
    }
  }

  /// Handle get correction request
  static Future<Map<String, dynamic>> _handleGetCorrection(
      dynamic arguments) async {
    if (!_initialized) {
      throw PlatformException(
        code: 'NOT_INITIALIZED',
        message: 'AI services not initialized',
      );
    }

    final Map<String, dynamic> args = Map<String, dynamic>.from(arguments);
    final String word = args['word'] ?? '';
    final String? previousWord = args['previousWord'];

    try {
      final correction = await AutocorrectService.instance.getCorrection(
        word,
        previousWord: previousWord,
      );

      return {
        'originalWord': correction.originalWord,
        'correctedWord': correction.correctedWord,
        'confidence': correction.confidence,
        'source': correction.source.toString().split('.').last,
        'hasCorrection': correction.hasCorrection,
      };
    } catch (e) {
      throw PlatformException(
        code: 'CORRECTION_ERROR',
        message: 'Failed to get correction: $e',
      );
    }
  }

  /// Handle learn from input request
  static Future<void> _handleLearnFromInput(dynamic arguments) async {
    if (!_initialized) return;

    final Map<String, dynamic> args = Map<String, dynamic>.from(arguments);
    final String word = args['word'] ?? '';
    final List<String> context = List<String>.from(args['context'] ?? []);

    try {
      await PredictiveTextEngine.instance.learnFromInput(word, context);
    } catch (e) {
      print('Failed to learn from input: $e');
    }
  }

  /// Handle learn correction request
  static Future<void> _handleLearnCorrection(dynamic arguments) async {
    if (!_initialized) return;

    final Map<String, dynamic> args = Map<String, dynamic>.from(arguments);
    final String original = args['original'] ?? '';
    final String corrected = args['corrected'] ?? '';
    final bool userConfirmed = args['userConfirmed'] ?? false;

    try {
      await AutocorrectService.instance.learnCorrection(
        original,
        corrected,
        userConfirmed: userConfirmed,
      );
    } catch (e) {
      print('Failed to learn correction: $e');
    }
  }

  /// Handle get stats request
  static Future<Map<String, dynamic>> _handleGetStats() async {
    if (!_initialized) {
      throw PlatformException(
        code: 'NOT_INITIALIZED',
        message: 'AI services not initialized',
      );
    }

    try {
      final autocorrectStats = AutocorrectService.instance.getStats();
      final predictiveStats = PredictiveTextEngine.instance.getStats();

      return {
        'autocorrect': {
          'dictionaryWords': autocorrectStats.dictionaryWords,
          'correctionRules': autocorrectStats.correctionRules,
          'cachedCorrections': autocorrectStats.cachedCorrections,
          'contractions': autocorrectStats.contractions,
          'memoryUsageKB': autocorrectStats.memoryUsageKB,
        },
        'predictive': {
          'dictionaryWords': predictiveStats.dictionaryWords,
          'bigramCount': predictiveStats.bigramCount,
          'trigramCount': predictiveStats.trigramCount,
          'userPatterns': predictiveStats.userPatterns,
          'recentWords': predictiveStats.recentWords,
          'memoryUsageKB': predictiveStats.memoryUsageKB,
        },
        'totalMemoryKB':
            autocorrectStats.memoryUsageKB + predictiveStats.memoryUsageKB,
      };
    } catch (e) {
      throw PlatformException(
        code: 'STATS_ERROR',
        message: 'Failed to get stats: $e',
      );
    }
  }

  /// Handle clear learning data request
  static Future<void> _handleClearLearningData() async {
    if (!_initialized) return;

    try {
      await Future.wait([
        AutocorrectService.instance.clearCache(),
        PredictiveTextEngine.instance.clearLearningData(),
      ]);
    } catch (e) {
      print('Failed to clear learning data: $e');
    }
  }

  /// Check if bridge is ready
  static bool get isReady => _initialized;

  /// Dispose of resources
  static void dispose() {
    _initialized = false;
  }
}

/// Entry point for background AI processing
void aiBackgroundMain() {
  // Ensure Flutter binding is initialized
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize the AI bridge handler
  AIBridgeHandler.initialize().then((_) {
    print('AI background service started successfully');
  }).catchError((e) {
    print('Failed to start AI background service: $e');
  });
}
