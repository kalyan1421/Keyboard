import 'package:ai_keyboard/utils/appassets.dart';
import 'package:ai_keyboard/utils/apptextstyle.dart';
import 'package:flutter/material.dart';

class NotificationScreen extends StatefulWidget {
  const NotificationScreen({Key? key}) : super(key: key);

  @override
  State<NotificationScreen> createState() => _NotificationScreenState();
}

class _NotificationScreenState extends State<NotificationScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  int _selectedTabIndex = 0;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _tabController.addListener(() {
      setState(() {
        _selectedTabIndex = _tabController.index;
      });
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
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
          'Notification',
          style: AppTextStyle.headlineMedium.copyWith(color: AppColors.white),
        ),
        centerTitle: true,
      ),
      body: Column(
        children: [
          // Custom Tab Bar
          _buildCustomTabBar(),

          // Tab Content
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [_buildRecentActivityTab(), _buildUnreadTab()],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCustomTabBar() {
    return Container(
      color: AppColors.white,
      child: Row(
        children: [
          Expanded(
            child: _buildTabButton(
              'Recent activity',
              0,
              () => _tabController.animateTo(0),
            ),
          ),
          Expanded(
            child: _buildTabButton(
              'Unread',
              1,
              () => _tabController.animateTo(1),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTabButton(String text, int index, VoidCallback onTap) {
    bool isSelected = _selectedTabIndex == index;

    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 16),
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: isSelected ? AppColors.secondary : Colors.transparent,
              width: 2,
            ),
          ),
        ),
        child: Text(
          text,
          textAlign: TextAlign.center,
          style: AppTextStyle.titleMedium.copyWith(
            color: isSelected ? AppColors.secondary : AppColors.grey,
            fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
          ),
        ),
      ),
    );
  }

  Widget _buildRecentActivityTab() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildNotificationSection('Today', _getTodayNotifications()),
          _buildNotificationSection('Yesterday', _getYesterdayNotifications()),
        ],
      ),
    );
  }

  Widget _buildUnreadTab() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildNotificationSection('Today', _getUnreadTodayNotifications()),
          _buildNotificationSection(
            'Yesterday',
            _getUnreadYesterdayNotifications(),
          ),
        ],
      ),
    );
  }

  Widget _buildNotificationSection(
    String title,
    List<Map<String, dynamic>> notifications,
  ) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Text(
            title,
            style: AppTextStyle.titleLarge.copyWith(
              color: AppColors.grey,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
        ...notifications.map(
          (notification) => _buildNotificationCard(notification),
        ),
      ],
    );
  }

  Widget _buildNotificationCard(Map<String, dynamic> notification) {
    bool isUnread = notification['isUnread'] ?? false;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(12),
        border: isUnread
            ? Border(bottom: BorderSide(color: AppColors.secondary, width: 3))
            : null,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildNotificationIcon(notification['iconType']),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.start,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      notification['title'],
                      style: AppTextStyle.titleMedium.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      notification['description'],
                      style: AppTextStyle.bodyMedium.copyWith(
                        color: AppColors.grey,
                      ),
                    ),
                    if (notification['hasAction'] == true) ...[
                      const SizedBox(height: 16),
                      _buildActionButton(notification['actionText']),
                    ],
                  ],
                ),
              ),
              Text(
                notification['time'],
                style: AppTextStyle.bodySmall.copyWith(
                  color: AppColors.primary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
        ],
      ),
    );
  }

  Widget _buildNotificationIcon(String iconType) {
    switch (iconType) {
      case 'premium':
        return Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: AppColors.secondary.withOpacity(0.2),
            borderRadius: BorderRadius.circular(20),
          ),
          child: const Icon(
            Icons.notifications,
            color: AppColors.secondary,
            size: 20,
          ),
        );
      case 'profile':
        return Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: AppColors.tertiary.withOpacity(0.2),
            borderRadius: BorderRadius.circular(20),
          ),
          child: const Icon(Icons.person, color: AppColors.tertiary, size: 20),
        );
      case 'theme':
        return Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: AppColors.primary.withOpacity(0.2),
            borderRadius: BorderRadius.circular(20),
          ),
          child: const Icon(Icons.palette, color: AppColors.primary, size: 20),
        );
      default:
        return Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: AppColors.grey.withOpacity(0.2),
            borderRadius: BorderRadius.circular(20),
          ),
          child: const Icon(
            Icons.notifications,
            color: AppColors.grey,
            size: 20,
          ),
        );
    }
  }

  Widget _buildActionButton(String actionText) {
    return SizedBox(
      // width: double.infinity,
      child: ElevatedButton(
        onPressed: () {
          // Handle action button press
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text('$actionText pressed')));
        },
        style: ElevatedButton.styleFrom(
          elevation: 0,
          backgroundColor: Color(0xffFFF4DE),
          foregroundColor: AppColors.secondary,
          padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
            side: BorderSide(color: AppColors.secondary),
          ),
        ),
        child: Text(
          actionText,
          style: AppTextStyle.buttonPrimary.copyWith(
            color: AppColors.secondary,
          ),
        ),
      ),
    );
  }

  List<Map<String, dynamic>> _getTodayNotifications() {
    return [
      {
        'title': 'Reminder for your Premium Expired',
        'time': '9min ago',
        'description':
            'Your premium was expired, Renew or upgrade premium for better experience.',
        'iconType': 'premium',
        'hasAction': true,
        'actionText': 'Upgrade Now',
        'isUnread': false,
      },
      {
        'title': 'Profile Picture was Updated',
        'time': '14min ago',
        'description': 'Your Profile Picture was updated with new picture',
        'iconType': 'profile',
        'hasAction': false,
        'isUnread': false,
      },
      {
        'title': 'Natural Theme was Launched',
        'time': '9min ago',
        'description': 'Experience new natural theme of keyboard',
        'iconType': 'theme',
        'hasAction': false,
        'isUnread': false,
      },
    ];
  }

  List<Map<String, dynamic>> _getYesterdayNotifications() {
    return [
      {
        'title': 'Natural Theme was Launched',
        'time': '09:45 AM',
        'description': 'Experience new natural theme of keyboard',
        'iconType': 'theme',
        'hasAction': false,
        'isUnread': false,
      },
      {
        'title': 'Red Rose Theme was updated',
        'time': '08:11 AM',
        'description': 'Experience new pink rose theme of keyboard',
        'iconType': 'theme',
        'hasAction': false,
        'isUnread': false,
      },
      {
        'title': 'Pink Rose Theme was updated',
        'time': '07:15 AM',
        'description': 'Experience new pink rose theme of keyboard',
        'iconType': 'theme',
        'hasAction': false,
        'isUnread': false,
      },
    ];
  }

  List<Map<String, dynamic>> _getUnreadTodayNotifications() {
    return [
      {
        'title': 'Reminder for your Premium Expired',
        'time': '9min ago',
        'description':
            'Your premium was expired, Renew or upgrade premium for better experience.',
        'iconType': 'premium',
        'hasAction': true,
        'actionText': 'Upgrade Now',
        'isUnread': true,
      },
      {
        'title': 'Natural Theme was Launched',
        'time': '9min ago',
        'description': 'Experience new natural theme of keyboard',
        'iconType': 'theme',
        'hasAction': true,
        'actionText': 'Got it',
        'isUnread': true,
      },
    ];
  }

  List<Map<String, dynamic>> _getUnreadYesterdayNotifications() {
    return [
      {
        'title': 'Red Rose Theme was updated',
        'time': '08:11 AM',
        'description': 'Experience new pink rose theme of keyboard',
        'iconType': 'theme',
        'hasAction': false,
        'isUnread': true,
      },
    ];
  }
}
