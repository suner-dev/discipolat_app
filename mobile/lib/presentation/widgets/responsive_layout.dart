import 'package:flutter/material.dart';

/// P2 #98 — Support tablette et mode paysage
/// Layout responsive qui s'adapte à la taille de l'écran.

enum DeviceType { phone, tablet, desktop }

class ResponsiveLayout extends StatelessWidget {
  final Widget mobile;
  final Widget? tablet;
  final Widget? desktop;

  const ResponsiveLayout({
    super.key,
    required this.mobile,
    this.tablet,
    this.desktop,
  });

  static DeviceType getDeviceType(BuildContext context) {
    final width = MediaQuery.of(context).size.width;
    if (width >= 1024) return DeviceType.desktop;
    if (width >= 600) return DeviceType.tablet;
    return DeviceType.phone;
  }

  static bool isTablet(BuildContext context) =>
      getDeviceType(context) == DeviceType.tablet;

  static bool isDesktop(BuildContext context) =>
      getDeviceType(context) == DeviceType.desktop;

  static bool isLandscape(BuildContext context) =>
      MediaQuery.of(context).orientation == Orientation.landscape;

  static int getCrossAxisCount(BuildContext context) {
    final width = MediaQuery.of(context).size.width;
    if (width >= 1024) return 4;
    if (width >= 600) return 3;
    return 2;
  }

  static double getGridAspectRatio(BuildContext context) {
    final orientation = MediaQuery.of(context).orientation;
    if (orientation == Orientation.landscape) return 2.0;
    return 1.4;
  }

  @override
  Widget build(BuildContext context) {
    final deviceType = getDeviceType(context);

    return LayoutBuilder(
      builder: (context, constraints) {
        if (deviceType == DeviceType.desktop && desktop != null) {
          return desktop!;
        }
        if (deviceType == DeviceType.tablet && tablet != null) {
          return tablet!;
        }
        return mobile;
      },
    );
  }
}

/// P2 #98 — Grid responsive pour les écrans en grille
class ResponsiveGrid extends StatelessWidget {
  final List<Widget> children;
  final double spacing;
  final double? childAspectRatio;

  const ResponsiveGrid({
    super.key,
    required this.children,
    this.spacing = 12,
    this.childAspectRatio,
  });

  @override
  Widget build(BuildContext context) {
    final crossAxisCount = ResponsiveLayout.getCrossAxisCount(context);
    final aspectRatio = childAspectRatio ?? ResponsiveLayout.getGridAspectRatio(context);

    return GridView.count(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisCount: crossAxisCount,
      crossAxisSpacing: spacing,
      mainAxisSpacing: spacing,
      childAspectRatio: aspectRatio,
      children: children,
    );
  }
}
