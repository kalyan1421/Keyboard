# Theme Gallery - Image Background Feature

## Overview
Enhanced the Theme Gallery screen with a new **Image Background Selection** section, allowing users to choose from predefined background images or upload their own custom images as keyboard backgrounds.

## Features Implemented

### 1. **Image Background Section**
- **Location**: Top of the Theme Gallery screen, scrollable horizontally
- **Functionality**: 
  - Displays categorized background images (Nature, Abstract, Flowers, etc.)
  - Each category has its own section with an icon and label
  - Users can tap on any image to apply it as their keyboard background
  - Images are displayed in a horizontal scrollable list

### 2. **Upload Custom Photo**
- **Location**: At the top of the image background section
- **Functionality**:
  - Prominent "Upload Photo" button with icon
  - Opens device gallery to select a custom image
  - Automatically creates a theme with the selected image as background
  - Supports image quality optimization (max 1920x1080, 90% quality)
  - Shows success/error feedback to the user

### 3. **Scrollable Architecture**
- **Implementation**: Uses `CustomScrollView` with Slivers for optimal performance
- **Structure**:
  1. **SliverToBoxAdapter** - Image Background Section (top)
  2. **SliverToBoxAdapter** - Category Filters (scrollable horizontally)
  3. **SliverGrid** - Theme Grid (main content, scrollable vertically)
  4. **SliverToBoxAdapter** - "Try your theme here" section (bottom)

### 4. **Category Filters**
- **Location**: Below the image background section, above the theme grid
- **Functionality**:
  - Horizontal scrollable filter chips
  - Shows all theme categories (Popular, Vibrant, Cool, Warm, etc.)
  - Selected category is highlighted
  - Filters the theme grid below

### 5. **Try Theme Section**
- **Location**: Bottom of the screen
- **Functionality**:
  - Placeholder for keyboard preview
  - Shows "Try your theme here" message
  - Provides visual feedback area for testing themes

## Technical Implementation

### New Class: `BackgroundImage`
```dart
class BackgroundImage {
  final String id;
  final String category;
  final String imageUrl;
  final IconData icon;
}
```

### Key Methods

#### `_buildImageBackgroundSection()`
- Groups images by category
- Creates horizontal scrollable lists for each category
- Displays category headers with icons

#### `_uploadCustomImage()`
- Opens image picker
- Validates and optimizes selected image
- Creates a new theme with the custom image
- Applies theme and shows feedback

#### `_applyImageBackground(BackgroundImage image)`
- Creates a theme using the selected predefined image
- Applies the theme to the keyboard

### UI Components

1. **Upload Photo Button**:
   - Prominent call-to-action design
   - Icon + title + description
   - Primary color theming
   - Forward arrow indicator

2. **Image Cards**:
   - 160px wide cards
   - Rounded corners (12px radius)
   - Shadow effects for depth
   - Gradient overlay for better visibility
   - Loading and error states handled

3. **Category Headers**:
   - Icon + category name
   - Bold typography
   - Consistent spacing

## User Experience Flow

1. **User opens Theme Gallery**
2. **Sees "Upload Photo" button at the top**
   - Can tap to add custom image from gallery
3. **Scrolls through categorized background images**
   - Nature images with leaf icon
   - Abstract images with sparkle icon
   - Flowers with flower icon
4. **Taps on any image to apply**
   - Image is applied as keyboard background
   - Success message shown
   - Navigates back to home screen with new theme
5. **Scrolls down to see category filters**
   - Filters are horizontally scrollable
   - Can select different categories (Popular, Vibrant, etc.)
6. **Theme grid updates based on selected category**
   - Shows 2 columns of theme cards
   - Scrollable vertically
7. **Bottom section shows "Try your theme here"**
   - Keyboard preview placeholder

## Integration with Existing System

### Theme V2 Integration
- Uses `KeyboardThemeV2` data model
- Creates `ThemeBackground` with type 'image'
- Sets image path and opacity
- Maintains compatibility with existing theme system

### Theme Manager Integration
- Uses `ThemeManagerV2.saveThemeV2()` to persist themes
- Broadcasts theme changes to keyboard service
- Maintains cross-platform compatibility

## Sample Background Images

The implementation includes sample images from three categories:

### Nature (🌿)
- 4 sample landscape images
- Icon: `Icons.eco`

### Abstract (✨)
- 2 sample abstract patterns
- Icon: `Icons.auto_awesome`

### Flowers (🌸)
- 2 sample flower images
- Icon: `Icons.local_florist`

**Note**: Images use Picsum Photos placeholder service for demonstration. In production, these should be replaced with:
- Local assets in `assets/images/backgrounds/`
- Firebase Storage URLs
- Or user-uploaded images

## Benefits

1. **Enhanced User Experience**:
   - More visual customization options
   - Easy image selection
   - Custom photo support

2. **Better Organization**:
   - Categorized backgrounds
   - Scrollable filters
   - Clear visual hierarchy

3. **Performance**:
   - Lazy loading with slivers
   - Optimized image sizes
   - Efficient memory management

4. **Extensibility**:
   - Easy to add new image categories
   - Simple to integrate with image services
   - Modular component design

## Future Enhancements

1. **Image Library**:
   - Add more predefined images
   - Seasonal/holiday themed images
   - Trending backgrounds

2. **Image Editing**:
   - Crop and rotate functionality
   - Brightness/contrast adjustment
   - Filter effects

3. **Online Gallery**:
   - Download images from online library
   - User-submitted backgrounds
   - Featured artist collections

4. **Recently Used**:
   - Show recently applied backgrounds
   - Quick access to favorites
   - Save custom image uploads

5. **AI-Generated Backgrounds**:
   - Integration with AI image generation
   - Custom prompts for backgrounds
   - Style transfer options

## Files Modified

- `lib/theme/theme_editor_v2.dart` - Added image background section and upload functionality

## Dependencies

Uses existing dependencies:
- `image_picker` - For selecting images from gallery
- `theme_v2.dart` - Theme data models
- Material Design widgets - UI components

