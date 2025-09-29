import 'package:flutter/material.dart';
import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'info_app_screen.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _ReminderCard(onUpgradeTap: () {}),
              const SizedBox(height: 24),
              Text(
                'Profile',
                style: AppTextStyle.titleMedium.copyWith(
                  color: AppColors.secondary,
                ),
              ),
              const SizedBox(height: 12),
              _TileOption(
                title: 'Change Profile Name',
                subtitle: 'Edit or change profile name',
                icon: AppIcons.profile_color,
                onTap: () => _showChangeNameDialog(context),
              ),
              const SizedBox(height: 12),
              _TileOption(
                title: 'Theme',
                subtitle: 'Edit or change theme',
                icon: AppIcons.theme_color,
                onTap: () {},
                isSvgIcon: false,
              ),
              const SizedBox(height: 12),
              _TileOption(
                title: 'Your Plan',
                subtitle: 'Premium plan was expired 9 day ago',
                icon: AppIcons.crown_color,
                onTap: () {},
              ),
              const SizedBox(height: 24),
              Text(
                'Other',
                style: AppTextStyle.titleMedium.copyWith(
                  color: AppColors.secondary,
                ),
              ),
              const SizedBox(height: 12),
              _TileOption(
                title: 'Help Center',
                subtitle: '24/7 Customer service available',
                icon: AppIcons.help_center_icon,
                onTap: () {},
              ),
              const SizedBox(height: 12),
              _TileOption(
                title: 'Info App',
                subtitle: 'Edit or change theme',
                icon: AppIcons.info_app_icon,
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => const InfoAppScreen(),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              _TileOption(
                title: 'Log out',
                subtitle: 'fivive001@gmail.com',
                icon: AppIcons.logout_icon,
                onTap: () => _showLogoutDialog(context),
              ),
              const SizedBox(height: 12),
              _TileOption(
                title: 'Delete Account',
                subtitle: 'Delete permanently account',
                icon: AppIcons.Delete_icon,
                onTap: () => _showDeleteDialog(context),
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }

  void _showLogoutDialog(BuildContext context) {
    showModalBottomSheet(
      context: context,
      enableDrag: false,

      builder: (BuildContext context) {
        return _LogoutConfirmationDialog();
      },
    );
  }

  void _showDeleteDialog(BuildContext context) {
    showModalBottomSheet(
      context: context,
      enableDrag: false,
      builder: (BuildContext context) {
        return _DeleteConfirmationDialog();
      },
    );
  }

  void _showChangeNameDialog(BuildContext context) {
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (BuildContext context) {
        return _ChangeNameDialog();
      },
    );
  }
}

class _ReminderCard extends StatelessWidget {
  final VoidCallback onUpgradeTap;
  const _ReminderCard({required this.onUpgradeTap});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border(bottom: BorderSide(color: AppColors.secondary)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: AppColors.secondary.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    alignment: Alignment.center,
                    child: Icon(
                      Icons.notifications_none,
                      color: AppColors.secondary,
                      size: 24,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Reminder for your Premium Expired',
                        style: AppTextStyle.titleSmall,
                      ),
                      const SizedBox(height: 4),
                      SizedBox(
                        width: MediaQuery.of(context).size.width * 0.5,
                        child: Text(
                          'Your premium was expired, Renew or upgrade premium  for better experience.',
                          style: AppTextStyle.bodySmall,
                        ),
                      ),
                      const SizedBox(height: 12),
                      Align(
                        alignment: Alignment.centerLeft,
                        child: SizedBox(
                          height: 40,
                          child: ElevatedButton(
                            style: ElevatedButton.styleFrom(
                              elevation: 0,
                              backgroundColor: Color(0xffFFF4DE),
                              foregroundColor: AppColors.secondary,
                              padding: const EdgeInsets.symmetric(
                                horizontal: 16,
                                vertical: 8,
                              ),
                              shape: RoundedRectangleBorder(
                                side: BorderSide(color: AppColors.secondary),
                                borderRadius: BorderRadius.circular(8),
                              ),
                            ),
                            onPressed: onUpgradeTap,
                            child: Text(
                              'Upgrade Now',
                              style: AppTextStyle.buttonPrimary.copyWith(
                                color: AppColors.secondary,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              Text(
                '9min ago',
                style: AppTextStyle.bodySmall.copyWith(
                  color: AppColors.primary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _TileOption extends StatelessWidget {
  final String title;
  final String subtitle;
  final String icon;
  final VoidCallback onTap;
  final bool isSvgIcon;

  const _TileOption({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.onTap,
    this.isSvgIcon = false,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: onTap,
      contentPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
      minTileHeight: 72,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      tileColor: AppColors.lightGrey,
      leading: isSvgIcon
          ? SizedBox(width: 28, height: 28, child: _SvgIcon(path: icon))
          : Image.asset(icon, width: 24, height: 28),
      title: Text(title, style: AppTextStyle.headlineSmall),
      subtitle: Text(subtitle),
      trailing: const Icon(Icons.chevron_right),
    );
  }
}

class _SvgIcon extends StatelessWidget {
  final String path;
  const _SvgIcon({required this.path});

  @override
  Widget build(BuildContext context) {
    // Fallback to Image.asset for simplicity if SVG package not used on this widget
    return SvgPicture.asset(path);
  }
}

class _LogoutConfirmationDialog extends StatelessWidget {
  const _LogoutConfirmationDialog();

  @override
  Widget build(BuildContext context) {
    return Container(
      // height: 200,
      width: double.infinity,

      padding: const EdgeInsets.all(24),
      decoration: const BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(16),
          topRight: Radius.circular(16),
        ),
      ),
      child: Column(
        spacing: 24,
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          // Title
          Text(
            'Log Out ?',
            style: AppTextStyle.headlineMedium.copyWith(
              fontWeight: FontWeight.bold,
              color: AppColors.black,
            ),
          ),
          // const SizedBox(height: 16),
          // Confirmation message
          Text(
            'Are you sure want to log out?',
            style: AppTextStyle.bodyLarge.copyWith(color: AppColors.secondary),
            textAlign: TextAlign.center,
          ),
          // const SizedBox(height: 24),
          // Buttons
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Cancel button
              Container(
                height: 48,
                width: 120,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [
                      Color(0xff002E6C),
                      Color(0xff023170),
                      Color(0xff0145A0),
                    ],
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    // stops: [0.5, 1.0],
                  ),
                  borderRadius: BorderRadius.circular(24),
                ),
                child: TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  style: TextButton.styleFrom(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(24),
                    ),
                    padding: EdgeInsets.zero,
                  ),
                  child: Text('Cancel', style: AppTextStyle.buttonPrimary),
                ),
              ),
              const SizedBox(width: 12),
              // Log out button
              Container(
                height: 48,
                width: 120,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [
                      AppColors.secondary,
                      AppColors.secondary.withOpacity(0.8),
                    ],
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                  ),
                  borderRadius: BorderRadius.circular(24),
                ),
                child: TextButton(
                  onPressed: () {
                    Navigator.of(context).pop();
                    // TODO: Implement actual logout logic here
                    // For now, just close the dialog
                  },
                  style: TextButton.styleFrom(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(24),
                    ),
                    padding: EdgeInsets.zero,
                  ),
                  child: Text('Log out', style: AppTextStyle.buttonPrimary),
                ),
              ),
            ],
          ),
          SizedBox(height: 24),
        ],
      ),
    );
  }
}

class _DeleteConfirmationDialog extends StatelessWidget {
  const _DeleteConfirmationDialog();

  @override
  Widget build(BuildContext context) {
    return Container(
      // height: 200,
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: const BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(16),
          topRight: Radius.circular(16),
        ),
      ),
      child: Column(
        spacing: 24,
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          // Title
          Text(
            'Delete Account ?',
            style: AppTextStyle.headlineMedium.copyWith(
              fontWeight: FontWeight.bold,
              color: AppColors.black,
            ),
          ),
          // const SizedBox(height: 16),
          // Confirmation message
          Text(
            'Are you sure want to delete account?',
            style: AppTextStyle.bodyLarge.copyWith(color: AppColors.grey),
            textAlign: TextAlign.center,
          ),
          // const SizedBox(height: 24),
          // Buttons
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Cancel button
              Container(
                height: 48,
                width: 120,
                decoration: BoxDecoration(
                  border: Border.all(color: AppColors.black),
                  borderRadius: BorderRadius.circular(8), // Square corners
                ),
                child: TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  style: TextButton.styleFrom(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8), // Square corners
                    ),
                    padding: EdgeInsets.zero,
                  ),
                  child: Text(
                    'Cancel',
                    style: AppTextStyle.buttonPrimary.copyWith(
                      color: AppColors.black,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              // Delete button
              Container(
                height: 48,
                width: 120,
                decoration: BoxDecoration(
                  color: AppColors.secondary,
                  borderRadius: BorderRadius.circular(8), // Square corners
                ),
                child: TextButton(
                  onPressed: () {
                    Navigator.of(context).pop();
                    // TODO: Implement actual delete account logic here
                    // For now, just close the dialog
                  },
                  style: TextButton.styleFrom(
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8), // Square corners
                    ),
                    padding: EdgeInsets.zero,
                  ),
                  child: Text('Delete', style: AppTextStyle.buttonPrimary),
                ),
              ),
            ],
          ),

          const SizedBox(height: 24),
        ],
      ),
    );
  }
}

class _ChangeNameDialog extends StatefulWidget {
  const _ChangeNameDialog();

  @override
  State<_ChangeNameDialog> createState() => _ChangeNameDialogState();
}

class _ChangeNameDialogState extends State<_ChangeNameDialog> {
  final TextEditingController _nameController = TextEditingController();

  @override
  void initState() {
    super.initState();
    // Set initial value to current name
    _nameController.text = 'Sarad kumar';
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: Colors.transparent,
      child: Container(
        width: MediaQuery.of(context).size.width * 0.9,
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Header with title and close button
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Change Profile Name',
                  style: AppTextStyle.headlineMedium.copyWith(
                    fontWeight: FontWeight.bold,
                    fontSize: 24,
                    color: AppColors.black,
                  ),
                ),
                GestureDetector(
                  onTap: () => Navigator.of(context).pop(),
                  child: Container(
                    width: 32,
                    height: 32,
                    decoration: BoxDecoration(
                      color: AppColors.lightGrey,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(Icons.close, color: AppColors.black, size: 20),
                  ),
                ),
              ],
            ),
            Divider(color: AppColors.lightGrey),
            // Input field label
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                'Enter Name',
                style: AppTextStyle.bodyMedium.copyWith(
                  color: AppColors.black,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            const SizedBox(height: 8),
            // Input field
            TextField(
              controller: _nameController,
              decoration: InputDecoration(
                hintText: 'Sarad kumar',
                hintStyle: AppTextStyle.bodyMedium.copyWith(
                  color: AppColors.grey,
                ),
                filled: true,
                fillColor: AppColors.lightGrey,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide.none,
                ),
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 12,
                ),
              ),
              style: AppTextStyle.bodyMedium.copyWith(color: AppColors.black),
            ),
            const SizedBox(height: 24),
            // Buttons
            Row(
              children: [
                // Cancel button
                Expanded(
                  child: Container(
                    height: 48,
                    decoration: BoxDecoration(
                      color: AppColors.white,
                      border: Border.all(color: AppColors.grey),
                      borderRadius: BorderRadius.circular(24),
                    ),
                    child: TextButton(
                      onPressed: () => Navigator.of(context).pop(),
                      style: TextButton.styleFrom(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(24),
                        ),
                        padding: EdgeInsets.zero,
                      ),
                      child: Text(
                        'Cancel',
                        style: AppTextStyle.buttonPrimary.copyWith(
                          color: AppColors.black,
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                // Save button
                Expanded(
                  child: Container(
                    height: 48,
                    decoration: BoxDecoration(
                      color: AppColors.secondary,
                      borderRadius: BorderRadius.circular(24),
                    ),
                    child: TextButton(
                      onPressed: () {
                        // TODO: Implement save name logic here
                        Navigator.of(context).pop();
                      },
                      style: TextButton.styleFrom(
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(24),
                        ),
                        padding: EdgeInsets.zero,
                      ),
                      child: Text('Save', style: AppTextStyle.buttonSecondary),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
