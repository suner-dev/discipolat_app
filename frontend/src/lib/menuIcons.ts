import type { LucideIcon } from 'lucide-react';
import {
  LayoutDashboard, Search, Map as MapIcon, Heart, Users, Activity, Sprout, Target,
  DoorOpen, AlertTriangle, Building2, FileText, BookOpen, Calendar, BarChart3, Shield,
  Star as StarIcon, MessageSquare, MessagesSquare, Bell, FolderOpen, GraduationCap,
  Trophy, CalendarClock, UserCog, User, Sparkles, Church, HandHeart, ArrowLeftRight,
  Workflow, Palette, Boxes, Menu as MenuIcon, CircleDot,
} from 'lucide-react';

/**
 * Registre des icônes de menus configurables.
 * L'administrateur choisit une clé d'icône (stockée en base) qui est
 * résolue ici vers le composant Lucide correspondant.
 */
export const MENU_ICONS: Record<string, LucideIcon> = {
  LayoutDashboard,
  Search,
  Map: MapIcon,
  Heart,
  Users,
  Activity,
  Sprout,
  Target,
  DoorOpen,
  AlertTriangle,
  Building2,
  FileText,
  BookOpen,
  Calendar,
  BarChart3,
  Shield,
  Star: StarIcon,
  MessageSquare,
  MessagesSquare,
  Bell,
  FolderOpen,
  GraduationCap,
  Trophy,
  CalendarClock,
  UserCog,
  User,
  Sparkles,
  Church,
  HandHeart,
  ArrowLeftRight,
  Workflow,
  Palette,
  Boxes,
  Menu: MenuIcon,
};

export const MENU_ICON_KEYS = Object.keys(MENU_ICONS).sort();

export function resolveIcon(name?: string | null): LucideIcon {
  return (name && MENU_ICONS[name]) || CircleDot;
}
