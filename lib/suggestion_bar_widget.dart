import 'package:flutter/material.dart';
import 'predictive_engine.dart';

/// Animated suggestion bar widget for displaying autocorrect and predictive text
class SuggestionBarWidget extends StatefulWidget {
  final List<PredictionResult> suggestions;
  final Function(String) onSuggestionSelected;
  final Function(String)? onSuggestionDismissed;
  final double height;
  final EdgeInsets padding;
  final bool showConfidenceIndicators;
  final Duration animationDuration;

  const SuggestionBarWidget({
    super.key,
    required this.suggestions,
    required this.onSuggestionSelected,
    this.onSuggestionDismissed,
    this.height = 50.0,
    this.padding = const EdgeInsets.symmetric(horizontal: 8.0, vertical: 4.0),
    this.showConfidenceIndicators = true,
    this.animationDuration = const Duration(milliseconds: 250),
  });

  @override
  State<SuggestionBarWidget> createState() => _SuggestionBarWidgetState();
}

class _SuggestionBarWidgetState extends State<SuggestionBarWidget>
    with TickerProviderStateMixin {
  late AnimationController _slideController;
  late AnimationController _fadeController;
  late Animation<Offset> _slideAnimation;
  late Animation<double> _fadeAnimation;

  List<PredictionResult> _currentSuggestions = [];
  List<PredictionResult> _previousSuggestions = [];

  @override
  void initState() {
    super.initState();
    
    _slideController = AnimationController(
      duration: widget.animationDuration,
      vsync: this,
    );
    
    _fadeController = AnimationController(
      duration: widget.animationDuration,
      vsync: this,
    );

    _slideAnimation = Tween<Offset>(
      begin: const Offset(0, -1),
      end: Offset.zero,
    ).animate(CurvedAnimation(
      parent: _slideController,
      curve: Curves.easeOutCubic,
    ));

    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _fadeController,
      curve: Curves.easeInOut,
    ));

    _currentSuggestions = widget.suggestions;
    _animateIn();
  }

  @override
  void didUpdateWidget(SuggestionBarWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    
    if (_suggestionsChanged(oldWidget.suggestions, widget.suggestions)) {
      _updateSuggestions(widget.suggestions);
    }
  }

  @override
  void dispose() {
    _slideController.dispose();
    _fadeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_currentSuggestions.isEmpty) {
      return SizedBox(height: widget.height);
    }

    return ListenableBuilder(
      listenable: _themeManager,
      builder: (context, _) {
        final theme = _themeManager.currentTheme;
        
        return Container(
          height: widget.height,
          padding: widget.padding,
          decoration: BoxDecoration(
            color: theme.suggestionBarColor,
            border: Border(
              bottom: BorderSide(
                color: theme.keyTextColor.withValues(alpha: 0.2),
                width: 0.5,
              ),
            ),
          ),
          child: SlideTransition(
            position: _slideAnimation,
            child: FadeTransition(
              opacity: _fadeAnimation,
              child: Row(
                children: [
                  Expanded(
                    child: ListView.builder(
                      scrollDirection: Axis.horizontal,
                      itemCount: _currentSuggestions.length,
                      itemBuilder: (context, index) {
                        final suggestion = _currentSuggestions[index];
                        return _buildSuggestionChip(suggestion, index, theme);
                      },
                    ),
                  ),
                  if (_currentSuggestions.isNotEmpty)
                    _buildDismissButton(theme),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildSuggestionChip(PredictionResult suggestion, int index, KeyboardThemeData theme) {
    final isCorrection = suggestion.isCorrection;
    final isHighConfidence = suggestion.confidence > 0.8;
    
    Color chipColor = _getChipColor(suggestion, theme);
    Color textColor = _getTextColor(suggestion, theme);
    
    return Padding(
      padding: const EdgeInsets.only(right: 8.0),
      child: GestureDetector(
        onTap: () => _selectSuggestion(suggestion),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
          decoration: BoxDecoration(
            color: chipColor,
            borderRadius: BorderRadius.circular(20.0),
            border: isCorrection
                ? Border.all(color: theme.accentColor, width: 1.5)
                : null,
            boxShadow: isHighConfidence
                ? [
                    BoxShadow(
                      color: chipColor.withValues(alpha: 0.3),
                      blurRadius: 4.0,
                      offset: const Offset(0, 2),
                    ),
                  ]
                : null,
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                suggestion.word,
                style: TextStyle(
                  color: textColor,
                  fontSize: theme.suggestionFontSize,
                  fontFamily: theme.fontFamily,
                  fontWeight: isCorrection ? FontWeight.w600 : FontWeight.w500,
                ),
              ),
              if (widget.showConfidenceIndicators)
                _buildConfidenceIndicator(suggestion),
              if (isCorrection)
                const SizedBox(width: 4.0),
              if (isCorrection)
                Icon(
                  Icons.auto_fix_high,
                  size: 14.0,
                  color: textColor.withValues(alpha: 0.7),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildConfidenceIndicator(PredictionResult suggestion) {
    if (suggestion.confidence < 0.3) return const SizedBox.shrink();
    
    return Padding(
      padding: const EdgeInsets.only(left: 6.0),
      child: Container(
        width: 4.0,
        height: 4.0,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: _getConfidenceColor(suggestion.confidence),
        ),
      ),
    );
  }

  Widget _buildDismissButton(KeyboardThemeData theme) {
    return GestureDetector(
      onTap: _dismissSuggestions,
      child: Container(
        padding: const EdgeInsets.all(8.0),
        child: Icon(
          Icons.keyboard_arrow_down,
          color: theme.keyTextColor.withValues(alpha: 0.6),
          size: 20.0,
        ),
      ),
    );
  }

  Color _getChipColor(PredictionResult suggestion, KeyboardThemeData theme) {
    if (suggestion.isCorrection) {
      return suggestion.confidence > 0.8
          ? theme.accentColor.withValues(alpha: 0.3)
          : theme.accentColor.withValues(alpha: 0.1);
    }
    
    return theme.keyBackgroundColor.withValues(alpha: 0.8);
  }

  Color _getTextColor(PredictionResult suggestion, KeyboardThemeData theme) {
    if (suggestion.isCorrection) {
      return suggestion.confidence > 0.8
          ? theme.accentColor
          : theme.accentColor.withValues(alpha: 0.8);
    }
    
    return theme.keyTextColor;
  }

  Color _getConfidenceColor(double confidence) {
    if (confidence > 0.8) return Colors.green;
    if (confidence > 0.6) return Colors.orange;
    return Colors.grey;
  }

  void _selectSuggestion(PredictionResult suggestion) {
    // Animate selection
    _animateSelection(() {
      widget.onSuggestionSelected(suggestion.word);
    });
  }

  void _dismissSuggestions() {
    if (widget.onSuggestionDismissed != null && _currentSuggestions.isNotEmpty) {
      widget.onSuggestionDismissed!(_currentSuggestions.first.word);
    }
    
    _animateOut();
  }

  void _updateSuggestions(List<PredictionResult> newSuggestions) {
    setState(() {
      _previousSuggestions = List.from(_currentSuggestions);
      _currentSuggestions = List.from(newSuggestions);
    });

    if (newSuggestions.isEmpty) {
      _animateOut();
    } else if (_previousSuggestions.isEmpty) {
      _animateIn();
    } else {
      // Crossfade animation for suggestion updates
      _fadeController.reset();
      _fadeController.forward();
    }
  }

  void _animateIn() {
    _slideController.reset();
    _fadeController.reset();
    
    _slideController.forward();
    _fadeController.forward();
  }

  void _animateOut() {
    _slideController.reverse();
    _fadeController.reverse();
  }

  void _animateSelection(VoidCallback onComplete) {
    // Quick scale animation to indicate selection
    _fadeController.reverse().then((_) {
      onComplete();
      if (mounted) {
        _fadeController.forward();
      }
    });
  }

  bool _suggestionsChanged(List<PredictionResult> old, List<PredictionResult> current) {
    if (old.length != current.length) return true;
    
    for (int i = 0; i < old.length; i++) {
      if (old[i].word != current[i].word ||
          old[i].confidence != current[i].confidence ||
          old[i].source != current[i].source) {
        return true;
      }
    }
    
    return false;
  }
}

/// Compact suggestion bar for smaller screens
class CompactSuggestionBar extends StatelessWidget {
  final List<PredictionResult> suggestions;
  final Function(String) onSuggestionSelected;
  final int maxSuggestions;

  const CompactSuggestionBar({
    super.key,
    required this.suggestions,
    required this.onSuggestionSelected,
    this.maxSuggestions = 3,
  });

  @override
  Widget build(BuildContext context) {
    final displaySuggestions = suggestions.take(maxSuggestions).toList();
    
    if (displaySuggestions.isEmpty) {
      return const SizedBox.shrink();
    }

    return Container(
      height: 40.0,
      padding: const EdgeInsets.symmetric(horizontal: 8.0),
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor.withValues(alpha: 0.9),
        border: Border(
          bottom: BorderSide(
            color: Theme.of(context).dividerColor,
            width: 0.5,
          ),
        ),
      ),
      child: Row(
        children: displaySuggestions.asMap().entries.map((entry) {
          final index = entry.key;
          final suggestion = entry.value;
          
          return Expanded(
            child: GestureDetector(
              onTap: () => onSuggestionSelected(suggestion.word),
              child: Container(
                margin: EdgeInsets.only(
                  right: index < displaySuggestions.length - 1 ? 4.0 : 0.0,
                ),
                decoration: BoxDecoration(
                  color: suggestion.isCorrection
                      ? Colors.orange.shade100
                      : Colors.grey.shade100,
                  borderRadius: BorderRadius.circular(6.0),
                ),
                child: Center(
                  child: Text(
                    suggestion.word,
                    style: TextStyle(
                      fontSize: 14.0,
                      fontWeight: suggestion.isCorrection
                          ? FontWeight.w600
                          : FontWeight.w500,
                      color: suggestion.isCorrection
                          ? Colors.orange.shade700
                          : Colors.grey.shade700,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }
}

/// Floating suggestion bubble for individual word corrections
class FloatingSuggestionBubble extends StatefulWidget {
  final String originalWord;
  final String suggestedWord;
  final double confidence;
  final Offset position;
  final Function(String) onAccept;
  final VoidCallback onDismiss;
  final Duration displayDuration;

  const FloatingSuggestionBubble({
    super.key,
    required this.originalWord,
    required this.suggestedWord,
    required this.confidence,
    required this.position,
    required this.onAccept,
    required this.onDismiss,
    this.displayDuration = const Duration(seconds: 3),
  });

  @override
  State<FloatingSuggestionBubble> createState() => _FloatingSuggestionBubbleState();
}

class _FloatingSuggestionBubbleState extends State<FloatingSuggestionBubble>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;
  late Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();
    
    _controller = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );

    _scaleAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.elasticOut,
    ));

    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _controller,
      curve: Curves.easeInOut,
    ));

    _controller.forward();

    // Auto-dismiss after duration
    Future.delayed(widget.displayDuration, () {
      if (mounted) {
        _dismiss();
      }
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Positioned(
      left: widget.position.dx,
      top: widget.position.dy,
      child: ScaleTransition(
        scale: _scaleAnimation,
        child: FadeTransition(
          opacity: _fadeAnimation,
          child: Material(
            elevation: 8.0,
            borderRadius: BorderRadius.circular(12.0),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 8.0),
              decoration: BoxDecoration(
                color: Colors.orange.shade100,
                borderRadius: BorderRadius.circular(12.0),
                border: Border.all(color: Colors.orange.shade300),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    widget.originalWord,
                    style: TextStyle(
                      fontSize: 14.0,
                      decoration: TextDecoration.lineThrough,
                      color: Colors.grey.shade600,
                    ),
                  ),
                  const SizedBox(width: 8.0),
                  const Icon(
                    Icons.arrow_forward,
                    size: 14.0,
                    color: Colors.orange,
                  ),
                  const SizedBox(width: 8.0),
                  GestureDetector(
                    onTap: () => _accept(),
                    child: Text(
                      widget.suggestedWord,
                      style: TextStyle(
                        fontSize: 14.0,
                        fontWeight: FontWeight.w600,
                        color: Colors.orange.shade700,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8.0),
                  GestureDetector(
                    onTap: () => _dismiss(),
                    child: Icon(
                      Icons.close,
                      size: 16.0,
                      color: Colors.grey.shade600,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  void _accept() {
    _controller.reverse().then((_) {
      widget.onAccept(widget.suggestedWord);
    });
  }

  void _dismiss() {
    _controller.reverse().then((_) {
      widget.onDismiss();
    });
  }
}