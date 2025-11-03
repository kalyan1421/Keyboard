# OpenAI Chat Setup Guide

## Overview
The Kvive Keyboard app now includes an AI-powered chat assistant that uses OpenAI's GPT-3.5-turbo model to answer keyboard-related questions.

## Features

### 🤖 AI-Powered Responses
- Intelligent responses powered by OpenAI's GPT-3.5-turbo
- Maintains conversation context for better understanding
- Restricted to keyboard-related topics only

### 🎯 Topic Restrictions
The AI assistant will only answer questions about:
- Keyboard themes and customization
- Typing features (autocorrect, predictions, suggestions)
- Language support and switching
- Keyboard settings and configuration
- AI-powered features
- Emoji, GIFs, and special characters
- Sound and haptic feedback
- Premium features and upgrades
- Troubleshooting keyboard issues

If users ask about unrelated topics, the assistant will politely redirect them to ask keyboard-related questions.

### 🔄 Fallback System
- If OpenAI API is not configured, the chat uses intelligent quick responses
- Quick responses cover common keyboard questions
- No internet connection required for fallback mode

## Setup Instructions

### 1. Get Your OpenAI API Key

1. Go to [https://platform.openai.com](https://platform.openai.com)
2. Sign in or create an account
3. Navigate to [API Keys](https://platform.openai.com/api-keys)
4. Click "Create new secret key"
5. Copy your API key (starts with `sk-...`)

### 2. Configure in the App

#### Option 1: Via Chat Screen (Recommended)
1. Open the app and navigate to the Chat Screen
2. Tap the **three-dot menu** (⋮) in the top right
3. Select **"Configure API Key"**
4. Paste your OpenAI API key
5. Tap **"Save"**

#### Option 2: Via Code (For Developers)
Edit `/lib/services/openai_chat_service.dart`:
```dart
static const String _defaultApiKey = 'sk-your-actual-api-key-here';
```

### 3. Enable/Disable OpenAI

In the Chat Screen:
1. Tap the **three-dot menu** (⋮)
2. Select **"Enable OpenAI"** or **"Disable OpenAI"**
   - **Enabled**: Uses OpenAI API for responses
   - **Disabled**: Uses quick responses (no API calls)

## Usage

### Starting a Conversation
1. Open the Chat Screen from the floating button
2. Type your keyboard-related question
3. Wait for the AI response (typing indicator will appear)
4. Continue the conversation naturally

### Example Questions
- "How do I change keyboard themes?"
- "What languages are supported?"
- "How does autocorrect work?"
- "Can I customize the keyboard colors?"
- "What are the premium features?"
- "How do I enable haptic feedback?"

## Technical Details

### API Configuration
- **Model**: gpt-3.5-turbo
- **Max Tokens**: 300
- **Temperature**: 0.7
- **Timeout**: 30 seconds
- **Context**: Last 10 messages

### Cost Considerations
- OpenAI API charges per token used
- Average cost per message: ~$0.001-0.003
- Monitor usage at [OpenAI Usage Dashboard](https://platform.openai.com/usage)

### Rate Limits
- Free tier: 3 RPM (requests per minute)
- Pay-as-you-go: 3,500 RPM
- If rate limit exceeded, the app will show an appropriate message

## Security

### API Key Storage
- API keys are stored securely in SharedPreferences
- Keys are never logged or exposed in error messages
- Keys are only used for OpenAI API requests

### Best Practices
1. **Never commit API keys to version control**
2. Use environment variables or secure storage for production
3. Rotate API keys regularly
4. Monitor API usage for unusual activity
5. Set spending limits in OpenAI dashboard

## Troubleshooting

### "API key not configured"
**Solution**: Configure your OpenAI API key using the menu in Chat Screen

### "Invalid API key"
**Solution**: 
- Check that your API key is correct
- Ensure the key hasn't expired
- Verify your OpenAI account is active

### "Too many requests"
**Solution**: 
- Wait 60 seconds before trying again
- Check your rate limits in OpenAI dashboard
- Consider upgrading your OpenAI plan

### "Can't reach the server"
**Solution**:
- Check your internet connection
- Verify OpenAI services are online
- Try again in a few moments

### Responses not keyboard-related
**Solution**: The AI should automatically restrict responses. If it doesn't:
- Check the system prompt in `openai_chat_service.dart`
- Verify you're using the latest version
- Report the issue to the development team

## Development

### File Structure
```
lib/
├── services/
│   └── openai_chat_service.dart  # OpenAI API integration
├── screens/
│   └── main screens/
│       └── chat_screen.dart       # Chat UI
```

### Key Components

#### OpenAIChatService
- Manages API key storage and retrieval
- Handles OpenAI API requests
- Provides fallback responses
- Maintains conversation context

#### ChatScreen
- User interface for chat
- Message history management
- Typing indicators
- API key configuration dialog

### Adding New Features

To customize the system prompt:
1. Edit `_systemPrompt` in `openai_chat_service.dart`
2. Adjust instructions for the AI assistant
3. Test thoroughly with various questions

To add new quick responses:
1. Edit `getQuickResponse()` in `openai_chat_service.dart`
2. Add keyword detection logic
3. Provide appropriate responses

## Support

For issues or questions:
1. Check this documentation
2. Review error messages in the app
3. Check OpenAI status at [status.openai.com](https://status.openai.com)
4. Contact the development team

## License

This feature uses OpenAI's API which has its own [Terms of Use](https://openai.com/policies/terms-of-use).

