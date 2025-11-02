import 'package:flutter/material.dart';
import 'package:ai_keyboard/theme/theme_v2.dart';
import 'dart:math' as math;

/// Visual Button Style Selector Screen
/// Shows different button/key styles with visual previews
class ButtonStyleSelectorScreen extends StatefulWidget {
  final KeyboardThemeV2 currentTheme;
  final Function(KeyboardThemeV2) onThemeUpdated;
  final bool showAppBar;

  const ButtonStyleSelectorScreen({
    super.key,
    required this.currentTheme,
    required this.onThemeUpdated,
    this.showAppBar = true,
  });

  @override
  State<ButtonStyleSelectorScreen> createState() => _ButtonStyleSelectorScreenState();
}

class _ButtonStyleSelectorScreenState extends State<ButtonStyleSelectorScreen> {
  late KeyboardThemeV2 _currentTheme;
  String? _selectedStyleId;

  final List<ButtonStyle> _buttonStyles = [
    ButtonStyle(
      id: 'rounded',
      name: 'Rounded',
      description: 'Classic rounded corners',
      iconType: ButtonIconType.roundedRect,
      preset: 'rounded',
      radius: 12.0,
    ),
    ButtonStyle(
      id: 'bordered',
      name: 'Bordered',
      description: 'Clear borders with rounded edges',
      iconType: ButtonIconType.bordered,
      preset: 'bordered',
      radius: 10.0,
    ),
    ButtonStyle(
      id: 'flat',
      name: 'Flat',
      description: 'Minimal flat design',
      iconType: ButtonIconType.flat,
      preset: 'flat',
      radius: 8.0,
    ),
    ButtonStyle(
      id: 'transparent',
      name: 'Transparent',
      description: 'See-through with borders',
      iconType: ButtonIconType.transparent,
      preset: 'transparent',
      radius: 12.0,
    ),
    ButtonStyle(
      id: 'star',
      name: 'Stars',
      description: 'Fun star-shaped keys',
      iconType: ButtonIconType.star,
      preset: 'star',
      radius: 0.0,
    ),
    ButtonStyle(
      id: 'heart',
      name: 'Hearts',
      description: 'Romantic heart-shaped keys',
      iconType: ButtonIconType.heart,
      preset: 'heart',
      radius: 0.0,
    ),
    ButtonStyle(
      id: 'hexagon',
      name: 'Hexagon',
      description: 'Modern hexagonal keys',
      iconType: ButtonIconType.hexagon,
      preset: 'hexagon',
      radius: 0.0,
    ),
    ButtonStyle(
      id: 'circle',
      name: 'Circles',
      description: 'Perfectly round keys',
      iconType: ButtonIconType.circle,
      preset: 'circle',
      radius: 999.0,
    ),
    ButtonStyle(
      id: 'cone',
      name: 'Traffic Cones',
      description: 'Unique cone-shaped keys',
      iconType: ButtonIconType.cone,
      preset: 'cone',
      radius: 0.0,
    ),
    ButtonStyle(
      id: 'gem',
      name: 'Gems',
      description: 'Sparkling gem-shaped keys',
      iconType: ButtonIconType.gem,
      preset: 'gem',
      radius: 0.0,
    ),
    ButtonStyle(
      id: 'bubble',
      name: 'Bubbles',
      description: 'Soft bubble-shaped keys',
      iconType: ButtonIconType.bubble,
      preset: 'bubble',
      radius: 20.0,
    ),
    ButtonStyle(
      id: 'square',
      name: 'Square',
      description: 'Sharp square corners',
      iconType: ButtonIconType.square,
      preset: 'square',
      radius: 0.0,
    ),
  ];

  @override
  void initState() {
    super.initState();
    _currentTheme = widget.currentTheme;
    _selectedStyleId = _currentTheme.keys.preset;
  }

  @override
  Widget build(BuildContext context) {
    final body = CustomScrollView(
      slivers: [
        // Color customization section
        SliverToBoxAdapter(
          child: _buildColorCustomizationSection(),
        ),
        
        SliverToBoxAdapter(
          child: const Divider(height: 1),
        ),
        
        // Button style grid
        SliverPadding(
          padding: const EdgeInsets.all(16),
          sliver: SliverGrid(
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              crossAxisSpacing: 16,
              mainAxisSpacing: 16,
              childAspectRatio: 0.85,
            ),
            delegate: SliverChildBuilderDelegate(
              (context, index) {
                final style = _buttonStyles[index];
                final isSelected = _selectedStyleId == style.id;
                return _buildButtonStyleCard(style, isSelected);
              },
              childCount: _buttonStyles.length,
            ),
          ),
        ),
      ],
    );

    // Return with or without Scaffold based on usage
    if (widget.showAppBar) {
      return Scaffold(
        appBar: AppBar(
          title: const Text('Button Style'),
          backgroundColor: _currentTheme.background.color,
          foregroundColor: _currentTheme.keys.text,
        ),
        body: body,
      );
    } else {
      return body;
    }
  }

  Widget _buildColorCustomizationSection() {
    return Container(
      padding: const EdgeInsets.all(16),
      color: Colors.grey.shade100,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Customize Colors',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 12),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                _buildColorOption('Key BG', _currentTheme.keys.bg, (color) {
                  _updateTheme(_currentTheme.copyWith(
                    keys: _currentTheme.keys.copyWith(bg: color),
                  ));
                }),
                const SizedBox(width: 12),
                _buildColorOption('Key Text', _currentTheme.keys.text, (color) {
                  _updateTheme(_currentTheme.copyWith(
                    keys: _currentTheme.keys.copyWith(text: color),
                  ));
                }),
                const SizedBox(width: 12),
                _buildColorOption('Pressed', _currentTheme.keys.pressed, (color) {
                  _updateTheme(_currentTheme.copyWith(
                    keys: _currentTheme.keys.copyWith(pressed: color),
                  ));
                }),
                const SizedBox(width: 12),
                _buildColorOption('Accent', _currentTheme.specialKeys.accent, (color) {
                  _updateTheme(_currentTheme.copyWith(
                    specialKeys: _currentTheme.specialKeys.copyWith(accent: color),
                  ));
                }),
              ],
            ),
          ),
          const SizedBox(height: 12),
          // Corner radius slider (only for applicable styles)
          if (_selectedStyleId != null && 
              !['star', 'heart', 'hexagon', 'cone', 'gem'].contains(_selectedStyleId))
            Row(
              children: [
                const Text('Corner Radius:'),
                Expanded(
                  child: Slider(
                    value: _currentTheme.keys.radius,
                    min: 0,
                    max: 20,
                    divisions: 20,
                    label: '${_currentTheme.keys.radius.round()}',
                    onChanged: (value) {
                      _updateTheme(_currentTheme.copyWith(
                        keys: _currentTheme.keys.copyWith(radius: value),
                      ));
                    },
                  ),
                ),
                Text('${_currentTheme.keys.radius.round()}'),
              ],
            ),
        ],
      ),
    );
  }


  Widget _buildColorOption(String label, Color color, Function(Color) onColorSelected) {
    return Column(
      children: [
        GestureDetector(
          onTap: () => _showColorPicker(color, onColorSelected),
          child: Container(
            width: 50,
            height: 50,
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.grey.shade400, width: 2),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.1),
                  blurRadius: 4,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: const TextStyle(fontSize: 11),
        ),
      ],
    );
  }

  Widget _buildButtonStyleCard(ButtonStyle style, bool isSelected) {
    return GestureDetector(
      onTap: () => _selectButtonStyle(style),
      child: Card(
        elevation: isSelected ? 8 : 2,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(
            color: isSelected ? _currentTheme.specialKeys.accent : Colors.transparent,
            width: 3,
          ),
        ),
        child: Column(
          children: [
            // Preview section with 3 keys
            Expanded(
              child: Container(
                decoration: BoxDecoration(
                  color: _currentTheme.background.color ?? Colors.grey.shade900,
                  borderRadius: const BorderRadius.vertical(top: Radius.circular(12)),
                ),
                child: Center(
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        _buildKeyPreview(style, 'A', false),
                        const SizedBox(width: 4),
                        _buildKeyPreview(style, 'S', false),
                        const SizedBox(width: 4),
                        _buildKeyPreview(style, '⏎', true),
                      ],
                    ),
                  ),
                ),
              ),
            ),
            // Info section
            Container(
              padding: const EdgeInsets.all(12),
              child: Column(
                children: [
                  Row(
                    children: [
                      if (isSelected)
                        Icon(
                          Icons.check_circle,
                          color: _currentTheme.specialKeys.accent,
                          size: 20,
                        ),
                      if (isSelected) const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          style.name,
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: isSelected ? FontWeight.bold : FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    style.description,
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey.shade600,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildKeyPreview(ButtonStyle style, String label, bool isAccent) {
    final color = isAccent ? _currentTheme.specialKeys.accent : _currentTheme.keys.bg;
    final textColor = isAccent ? Colors.white : _currentTheme.keys.text;

    return CustomPaint(
      painter: KeyShapePainter(
        iconType: style.iconType,
        color: color,
        borderColor: _currentTheme.keys.border.color,
        radius: style.radius,
      ),
      child: SizedBox(
        width: 36,
        height: 36,
        child: Center(
          child: Text(
            label,
            style: TextStyle(
              color: textColor,
              fontSize: 14,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
      ),
    );
  }

  void _selectButtonStyle(ButtonStyle style) {
    setState(() {
      _selectedStyleId = style.id;
    });
    
    _updateTheme(_currentTheme.copyWith(
      keys: _currentTheme.keys.copyWith(
        preset: style.preset,
        radius: style.radius,
      ),
    ));
  }

  void _updateTheme(KeyboardThemeV2 newTheme) {
    setState(() {
      _currentTheme = newTheme;
    });
    widget.onThemeUpdated(newTheme);
  }

  void _showColorPicker(Color currentColor, Function(Color) onColorSelected) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Pick a Color'),
        content: SingleChildScrollView(
          child: Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              // Basic colors
              Colors.red, Colors.pink, Colors.purple, Colors.deepPurple,
              Colors.indigo, Colors.blue, Colors.lightBlue, Colors.cyan,
              Colors.teal, Colors.green, Colors.lightGreen, Colors.lime,
              Colors.yellow, Colors.amber, Colors.orange, Colors.deepOrange,
              Colors.brown, Colors.grey, Colors.blueGrey, Colors.black,
              Colors.white,
              // Additional vibrant colors
              const Color(0xFFFF6B9D), const Color(0xFF4CAF50), const Color(0xFF9C27B0),
              const Color(0xFFFF9800), const Color(0xFF00BCD4), const Color(0xFF2196F3),
              const Color(0xFFFFC107), const Color(0xFFE91E63), const Color(0xFF3F51B5),
              const Color(0xFF009688), const Color(0xFF8BC34A), const Color(0xFFFF5722),
            ].map((color) {
              return GestureDetector(
                onTap: () {
                  onColorSelected(color);
                  Navigator.of(context).pop();
                },
                child: Container(
                  width: 50,
                  height: 50,
                  decoration: BoxDecoration(
                    color: color,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: color == currentColor ? Colors.blue : Colors.grey.shade400,
                      width: color == currentColor ? 3 : 1,
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }
}

/// Button style data model
class ButtonStyle {
  final String id;
  final String name;
  final String description;
  final ButtonIconType iconType;
  final String preset;
  final double radius;

  ButtonStyle({
    required this.id,
    required this.name,
    required this.description,
    required this.iconType,
    required this.preset,
    required this.radius,
  });
}

/// Button icon types for different shapes
enum ButtonIconType {
  roundedRect,
  bordered,
  flat,
  transparent,
  star,
  heart,
  hexagon,
  circle,
  cone,
  gem,
  bubble,
  square,
}

/// Custom painter for drawing different key shapes
class KeyShapePainter extends CustomPainter {
  final ButtonIconType iconType;
  final Color color;
  final Color borderColor;
  final double radius;

  KeyShapePainter({
    required this.iconType,
    required this.color,
    required this.borderColor,
    required this.radius,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    final borderPaint = Paint()
      ..color = borderColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.5;

    final center = Offset(size.width / 2, size.height / 2);
    final rect = Rect.fromCenter(center: center, width: size.width * 0.9, height: size.height * 0.9);

    switch (iconType) {
      case ButtonIconType.star:
        _drawStar(canvas, size, paint, borderPaint);
        break;
      case ButtonIconType.heart:
        _drawHeart(canvas, size, paint, borderPaint);
        break;
      case ButtonIconType.hexagon:
        _drawHexagon(canvas, size, paint, borderPaint);
        break;
      case ButtonIconType.circle:
        canvas.drawCircle(center, size.width * 0.45, paint);
        canvas.drawCircle(center, size.width * 0.45, borderPaint);
        break;
      case ButtonIconType.cone:
        _drawCone(canvas, size, paint, borderPaint);
        break;
      case ButtonIconType.gem:
        _drawGem(canvas, size, paint, borderPaint);
        break;
      case ButtonIconType.bubble:
        final bubbleRect = RRect.fromRectAndRadius(rect, Radius.circular(radius));
        canvas.drawRRect(bubbleRect, paint);
        canvas.drawRRect(bubbleRect, borderPaint);
        break;
      case ButtonIconType.square:
        canvas.drawRect(rect, paint);
        canvas.drawRect(rect, borderPaint);
        break;
      case ButtonIconType.transparent:
        final transparentPaint = Paint()
          ..color = color.withOpacity(0.3)
          ..style = PaintingStyle.fill;
        final rrect = RRect.fromRectAndRadius(rect, Radius.circular(radius));
        canvas.drawRRect(rrect, transparentPaint);
        canvas.drawRRect(rrect, borderPaint);
        break;
      default:
        // Rounded rect (default)
        final rrect = RRect.fromRectAndRadius(rect, Radius.circular(radius));
        canvas.drawRRect(rrect, paint);
        if (iconType == ButtonIconType.bordered) {
          canvas.drawRRect(rrect, borderPaint);
        }
    }
  }

  void _drawStar(Canvas canvas, Size size, Paint paint, Paint borderPaint) {
    final path = Path();
    final center = Offset(size.width / 2, size.height / 2);
    final outerRadius = size.width * 0.45;
    final innerRadius = outerRadius * 0.4;
    
    for (int i = 0; i < 10; i++) {
      final radius = i.isEven ? outerRadius : innerRadius;
      final angle = (i * 36 - 90) * math.pi / 180;
      final x = center.dx + radius * math.cos(angle);
      final y = center.dy + radius * math.sin(angle);
      
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }
    path.close();
    
    canvas.drawPath(path, paint);
    canvas.drawPath(path, borderPaint);
  }

  void _drawHeart(Canvas canvas, Size size, Paint paint, Paint borderPaint) {
    final path = Path();
    final width = size.width * 0.9;
    final height = size.height * 0.9;
    final startX = size.width * 0.05;
    final startY = size.height * 0.15;

    path.moveTo(size.width / 2, startY + height * 0.85);
    
    // Left side of heart
    path.cubicTo(
      startX, startY + height * 0.5,
      startX, startY + height * 0.2,
      size.width / 2 - width * 0.2, startY + height * 0.1,
    );
    path.cubicTo(
      size.width / 2, startY,
      size.width / 2, startY + height * 0.2,
      size.width / 2, startY + height * 0.3,
    );
    
    // Right side of heart
    path.cubicTo(
      size.width / 2, startY + height * 0.2,
      size.width / 2, startY,
      size.width / 2 + width * 0.2, startY + height * 0.1,
    );
    path.cubicTo(
      startX + width, startY + height * 0.2,
      startX + width, startY + height * 0.5,
      size.width / 2, startY + height * 0.85,
    );
    
    canvas.drawPath(path, paint);
    canvas.drawPath(path, borderPaint);
  }

  void _drawHexagon(Canvas canvas, Size size, Paint paint, Paint borderPaint) {
    final path = Path();
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width * 0.45;
    
    for (int i = 0; i < 6; i++) {
      final angle = (i * 60 - 90) * math.pi / 180;
      final x = center.dx + radius * math.cos(angle);
      final y = center.dy + radius * math.sin(angle);
      
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }
    path.close();
    
    canvas.drawPath(path, paint);
    canvas.drawPath(path, borderPaint);
  }

  void _drawCone(Canvas canvas, Size size, Paint paint, Paint borderPaint) {
    final path = Path();
    final topCenter = Offset(size.width / 2, size.height * 0.15);
    final bottomLeft = Offset(size.width * 0.2, size.height * 0.85);
    final bottomRight = Offset(size.width * 0.8, size.height * 0.85);
    
    path.moveTo(topCenter.dx, topCenter.dy);
    path.lineTo(bottomLeft.dx, bottomLeft.dy);
    path.lineTo(bottomRight.dx, bottomRight.dy);
    path.close();
    
    canvas.drawPath(path, paint);
    canvas.drawPath(path, borderPaint);
  }

  void _drawGem(Canvas canvas, Size size, Paint paint, Paint borderPaint) {
    final path = Path();
    
    // Top point
    path.moveTo(size.width / 2, size.height * 0.1);
    
    // Top facets
    path.lineTo(size.width * 0.75, size.height * 0.3);
    path.lineTo(size.width * 0.85, size.height * 0.5);
    path.lineTo(size.width / 2, size.height * 0.9);
    path.lineTo(size.width * 0.15, size.height * 0.5);
    path.lineTo(size.width * 0.25, size.height * 0.3);
    path.close();
    
    canvas.drawPath(path, paint);
    canvas.drawPath(path, borderPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}

