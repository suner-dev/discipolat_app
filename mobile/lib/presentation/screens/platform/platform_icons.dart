import 'package:flutter/material.dart';

/// Associe une clé d'icône (valeur stockée dans `platform_modules.icon` /
/// `menu_entries.icon`, ex. `LayoutDashboard`, `Sparkles`, `Boxes`…) à une
/// icône Flutter. Repli sur une icône neutre pour toute clé inconnue.
IconData platformIcon(String? iconKey) {
  switch (iconKey) {
    case 'LayoutDashboard': return Icons.dashboard_rounded;
    case 'Sparkles': return Icons.auto_awesome_rounded;
    case 'Search': return Icons.search_rounded;
    case 'Map': return Icons.map_rounded;
    case 'Boxes': return Icons.inventory_2_rounded;
    case 'BookOpen': return Icons.menu_book_rounded;
    case 'Heart': return Icons.favorite_rounded;
    case 'Users':
    case 'Users2': return Icons.group_rounded;
    case 'Building2': return Icons.business_rounded;
    case 'BarChart3': return Icons.bar_chart_rounded;
    case 'Bell': return Icons.notifications_rounded;
    case 'Shield': return Icons.shield_rounded;
    case 'FolderOpen': return Icons.folder_open_rounded;
    case 'Activity': return Icons.monitor_heart_rounded;
    case 'AlertTriangle': return Icons.warning_amber_rounded;
    case 'ArrowLeftRight': return Icons.swap_horiz_rounded;
    case 'Calendar': return Icons.calendar_today_rounded;
    case 'CalendarClock': return Icons.event_available_rounded;
    case 'DoorOpen': return Icons.door_front_door_rounded;
    case 'FileText': return Icons.description_rounded;
    case 'GraduationCap': return Icons.school_rounded;
    case 'HandHeart': return Icons.volunteer_activism_rounded;
    case 'Star': return Icons.star_rounded;
    case 'Target': return Icons.flag_rounded;
    case 'UserCog': return Icons.manage_accounts_rounded;
    case 'Menu': return Icons.menu_rounded;
    case 'Home': return Icons.home_rounded;
    case 'Database': return Icons.storage_rounded;
    case 'GitBranch': return Icons.account_tree_rounded;
    case 'Settings':
    case 'Settings2': return Icons.settings_rounded;
    case 'ClipboardList': return Icons.assignment_rounded;
    case 'CheckSquare': return Icons.check_box_rounded;
    case 'MessageCircle': return Icons.chat_rounded;
    case 'Award': return Icons.emoji_events_rounded;
    case 'Route': return Icons.route_rounded;
    case 'MapPin': return Icons.place_rounded;
    case 'Globe': return Icons.public_rounded;
    case 'Phone': return Icons.phone_rounded;
    case 'Mail': return Icons.mail_rounded;
    case 'Church': return Icons.church_rounded;
    case 'List': return Icons.list_rounded;
    case 'Grid': return Icons.grid_view_rounded;
    case 'Layers': return Icons.layers_rounded;
    case 'Landmark': return Icons.account_balance_rounded;
    case 'Wallet': return Icons.account_balance_wallet_rounded;
    case 'Megaphone': return Icons.campaign_rounded;
    case 'PenTool': return Icons.edit_rounded;
    case 'BadgeCheck': return Icons.verified_rounded;
    case 'KeyRound': return Icons.key_rounded;
    case 'Lock': return Icons.lock_rounded;
    case 'Eye': return Icons.visibility_rounded;
    case 'Link': return Icons.link_rounded;
    case 'ChevronRight': return Icons.chevron_right_rounded;
    default: return Icons.extension_rounded;
  }
}

/// Liste des clés proposées dans l'éditeur (icônes les plus courantes).
const List<String> kPlatformIconKeys = [
  'LayoutDashboard', 'Sparkles', 'Search', 'Map', 'Boxes', 'BookOpen',
  'Heart', 'Users', 'Building2', 'BarChart3', 'Bell', 'Shield',
  'FolderOpen', 'Activity', 'AlertTriangle', 'ArrowLeftRight', 'Calendar',
  'CalendarClock', 'DoorOpen', 'FileText', 'GraduationCap', 'HandHeart',
  'Star', 'Target', 'UserCog', 'Menu', 'Home', 'Database', 'GitBranch',
  'Settings', 'ClipboardList', 'MessageCircle', 'Award', 'Route', 'Globe',
  'Phone', 'Mail', 'Church', 'List', 'Layers', 'Landmark', 'Wallet',
  'Megaphone', 'BadgeCheck',
];
