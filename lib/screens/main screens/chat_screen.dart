import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:flutter/material.dart';

class ChatScreen extends StatefulWidget {
  const ChatScreen({Key? key}) : super(key: key);

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  final List<ChatMessage> _messages = [];

  @override
  void initState() {
    super.initState();
    _initializeChat();
  }

  @override
  void dispose() {
    _messageController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _initializeChat() {
    // Add initial messages
    _messages.addAll([
      ChatMessage(
        text: "Hello, I need help.",
        isUser: true,
        timestamp: DateTime.now().subtract(const Duration(minutes: 2)),
      ),
      ChatMessage(
        text: "Fine, How can Help you?",
        isUser: false,
        timestamp: DateTime.now().subtract(const Duration(minutes: 1)),
      ),
      ChatMessage(
        text: "Can i change themes?",
        isUser: true,
        timestamp: DateTime.now().subtract(const Duration(seconds: 30)),
      ),
      ChatMessage(
        text: "Yes, You can change themes & Customize",
        isUser: false,
        timestamp: DateTime.now(),
      ),
    ]);
  }

  void _sendMessage() {
    if (_messageController.text.trim().isEmpty) return;

    final userMessage = ChatMessage(
      text: _messageController.text.trim(),
      isUser: true,
      timestamp: DateTime.now(),
    );

    setState(() {
      _messages.add(userMessage);
    });

    _messageController.clear();
    _scrollToBottom();

    // Generate AI response after a short delay
    Future.delayed(const Duration(milliseconds: 1000), () {
      _generateAIResponse(userMessage.text);
    });
  }

  void _generateAIResponse(String userMessage) {
    String aiResponse = _getAIResponse(userMessage);

    final aiMessage = ChatMessage(
      text: aiResponse,
      isUser: false,
      timestamp: DateTime.now(),
    );

    setState(() {
      _messages.add(aiMessage);
    });

    _scrollToBottom();
  }

  String _getAIResponse(String userMessage) {
    final message = userMessage.toLowerCase();

    if (message.contains('theme') || message.contains('themes')) {
      return "You can change themes from the settings menu. We have various themes like Natural, Red Rose, Pink Rose, and many more!";
    } else if (message.contains('keyboard') || message.contains('key')) {
      return "Our AI keyboard offers smart predictions, autocorrect, and multiple language support. What specific feature would you like to know about?";
    } else if (message.contains('help') || message.contains('support')) {
      return "I'm here to help! You can ask me about themes, keyboard features, settings, or any other questions about the app.";
    } else if (message.contains('language') || message.contains('lang')) {
      return "You can add multiple languages from the Language settings. We support English, Hindi, Gujarati, Bengali, and many more!";
    } else if (message.contains('premium') || message.contains('upgrade')) {
      return "Premium features include advanced AI suggestions, unlimited themes, and priority support. You can upgrade from the settings menu.";
    } else if (message.contains('hello') || message.contains('hi')) {
      return "Hello! How can I assist you today?";
    } else if (message.contains('thank') || message.contains('thanks')) {
      return "You're welcome! Is there anything else I can help you with?";
    } else {
      return "I understand you're asking about \"$userMessage\". Could you please provide more details so I can help you better?";
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        backgroundColor: AppColors.primary,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Check Keyboard',
          style: AppTextStyle.headlineMedium.copyWith(color: AppColors.white),
        ),
        centerTitle: false,
        actions: [
          Stack(
            children: [
              IconButton(
                icon: const Icon(
                  Icons.notifications_outlined,
                  color: AppColors.white,
                ),
                onPressed: () {},
              ),
              Positioned(
                right: 8,
                top: 8,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: AppColors.secondary,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
      body: Column(
        children: [
          // Chat Messages
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.all(16),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                return _buildMessageBubble(_messages[index]);
              },
            ),
          ),

          // Input Field
          _buildInputField(),
        ],
      ),
    );
  }

  Widget _buildMessageBubble(ChatMessage message) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      child: Row(
        mainAxisAlignment: message.isUser
            ? MainAxisAlignment.end
            : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Flexible(
            child: Column(
              crossAxisAlignment: message.isUser
                  ? CrossAxisAlignment.end
                  : CrossAxisAlignment.start,
              children: [
                ClipPath(
                  clipper: MessageBubbleClipper(isUser: message.isUser),
                  child: Container(
                    height: 60,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 12,
                    ),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(10),
                      color: message.isUser
                          ? AppColors.secondary.withOpacity(0.8)
                          : AppColors.lightGrey,
                    ),
                    child: Container(
                      constraints: BoxConstraints(
                        maxWidth: MediaQuery.of(context).size.width * 0.7,
                      ),

                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          // if (!message.isUser)
                          //   Text(
                          //     _formatTime(message.timestamp),
                          //     style: AppTextStyle.bodySmall.copyWith(
                          //       color: AppColors.black,
                          //       fontWeight: FontWeight.w500,
                          //     ),
                          //   ),
                          // const SizedBox(width: 4),
                          SizedBox(
                            width: MediaQuery.of(context).size.width * 0.5,
                            child: Text(
                              message.text,
                              style: AppTextStyle.bodyMedium.copyWith(
                                color: AppColors.black,
                              ),
                            ),
                          ),
                          const SizedBox(width: 4),
                          // if (message.isUser)
                          Text(
                            _formatTime(message.timestamp),
                            style: AppTextStyle.bodySmall.copyWith(
                              color: message.isUser
                                  ? AppColors.white
                                  : AppColors.black,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 4),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInputField() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: AppColors.lightGrey,
                borderRadius: BorderRadius.circular(25),
              ),
              child: TextField(
                controller: _messageController,
                decoration: const InputDecoration(
                  hintText: 'Type your message...',
                  border: InputBorder.none,
                  contentPadding: EdgeInsets.zero,
                ),
                style: AppTextStyle.bodyMedium,
                maxLines: null,
                textCapitalization: TextCapitalization.sentences,
                onSubmitted: (_) => _sendMessage(),
              ),
            ),
          ),
          const SizedBox(width: 12),
          GestureDetector(
            onTap: _sendMessage,
            child: Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: AppColors.secondary,
                borderRadius: BorderRadius.circular(24),
              ),
              child: const Icon(Icons.send, color: AppColors.white, size: 20),
            ),
          ),
        ],
      ),
    );
  }

  String _formatTime(DateTime timestamp) {
    final now = DateTime.now();
    final difference = now.difference(timestamp);

    if (difference.inMinutes < 1) {
      return 'Just now';
    } else if (difference.inMinutes < 60) {
      return '${difference.inMinutes}min ago';
    } else if (difference.inHours < 24) {
      final hour = timestamp.hour;
      final minute = timestamp.minute.toString().padLeft(2, '0');
      final period = hour >= 12 ? 'PM' : 'AM';
      final displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
      return '$displayHour:$minute $period';
    } else {
      return '${timestamp.day}/${timestamp.month}/${timestamp.year}';
    }
  }
}

class ChatMessage {
  final String text;
  final bool isUser;
  final DateTime timestamp;

  ChatMessage({
    required this.text,
    required this.isUser,
    required this.timestamp,
  });
}

class MessageBubbleClipper extends CustomClipper<Path> {
  final bool isUser;

  MessageBubbleClipper({required this.isUser});

  @override
  Path getClip(Size size) {
    final path = Path();
    final radius = 20.0;
    final tailSize = 10.0;

    if (isUser) {
      // User message bubble with curved inward bottom-right corner
      path.moveTo(radius, 0);
      path.lineTo(size.width - radius, 0);
      path.quadraticBezierTo(size.width, 0, size.width, radius);
      path.lineTo(size.width, size.height);

      // Curved inward bottom-right corner
      path.quadraticBezierTo(
        size.width,
        size.height - tailSize,
        size.width - radius,
        size.height - tailSize,
      );

      // // Tail pointing right
      // path.lineTo(size.width - radius - tailSize, size.height - tailSize);
      // path.lineTo(size.width - radius - tailSize, size.height);
      // path.lineTo(size.width - radius, size.height - tailSize);

      // Continue with left side
      path.lineTo(radius, size.height - tailSize);
      path.quadraticBezierTo(
        0,
        size.height - tailSize,
        0,
        size.height - radius - tailSize,
      );
      path.lineTo(0, radius);
      path.quadraticBezierTo(0, 0, radius, 0);
      path.close();
    } else {
      // AI message bubble with curved inward bottom-left corner
      path.moveTo(radius, 0);
      path.lineTo(size.width - radius, 0);
      path.quadraticBezierTo(size.width, 0, size.width, radius);
      path.lineTo(size.width, size.height - radius);
      path.quadraticBezierTo(
        size.width,
        size.height,
        size.width - radius,
        size.height,
      );
      path.lineTo(radius + tailSize, size.height);

      // Tail pointing left
      path.lineTo(radius + tailSize, size.height - tailSize);
      path.lineTo(radius, size.height - tailSize);
      path.lineTo(radius + tailSize, size.height);

      // Curved inward bottom-left corner
      path.lineTo(radius, size.height);
      path.quadraticBezierTo(0, size.height, 0, size.height - radius);
      path.lineTo(0, radius);
      path.quadraticBezierTo(0, 0, radius, 0);
      path.close();
    }

    return path;
  }

  @override
  bool shouldReclip(CustomClipper<Path> oldClipper) => false;
}
