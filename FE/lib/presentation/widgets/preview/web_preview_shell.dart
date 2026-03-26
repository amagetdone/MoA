import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class WebPreviewShell extends StatefulWidget {
  const WebPreviewShell({
    super.key,
    required this.child,
    required this.designSize,
    required this.minTextAdapt,
    required this.splitScreenMode,
  });

  final Widget child;
  final Size designSize;
  final bool minTextAdapt;
  final bool splitScreenMode;

  static bool get isEnabled {
    if (!kIsWeb) {
      return false;
    }

    final uri = Uri.base;
    final pathSegments = uri.pathSegments;
    final preview = uri.queryParameters['preview'];

    if (pathSegments.isNotEmpty && pathSegments.first == 'app') {
      return false;
    }

    if (pathSegments.isNotEmpty && pathSegments.first == 'preview') {
      return true;
    }

    if (preview != null) {
      final normalized = preview.toLowerCase();
      return normalized != '0' &&
          normalized != 'false' &&
          normalized != 'off';
    }

    return uri.host == 'j14d101.p.ssafy.io';
  }

  @override
  State<WebPreviewShell> createState() => _WebPreviewShellState();
}

class _WebPreviewShellState extends State<WebPreviewShell> {
  static const List<_PreviewDevice> _devices = [
    _PreviewDevice(id: 's24', label: 'Galaxy S24', viewport: Size(360, 780)),
    _PreviewDevice(
      id: 's24_ultra',
      label: 'Galaxy S24 Ultra',
      viewport: Size(412, 915),
    ),
    _PreviewDevice(
      id: 'flip6',
      label: 'Galaxy Z Flip6',
      viewport: Size(360, 880),
    ),
    _PreviewDevice(
      id: 'fold6_cover',
      label: 'Galaxy Z Fold6 Cover',
      viewport: Size(344, 844),
    ),
    _PreviewDevice(
      id: 'fold6_open',
      label: 'Galaxy Z Fold6 Open',
      viewport: Size(904, 1053),
    ),
  ];

  _PreviewDevice _selectedDevice = _devices.first;
  bool _isLandscape = false;

  @override
  void initState() {
    super.initState();
    final requestedDeviceId = Uri.base.queryParameters['device'];
    _selectedDevice = _devices.firstWhere(
      (device) => device.id == requestedDeviceId,
      orElse: () => _devices.first,
    );
    final landscape = Uri.base.queryParameters['landscape']?.toLowerCase();
    _isLandscape = landscape == '1' || landscape == 'true';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final mediaQuery = MediaQuery.of(context);
    final viewportSize = _isLandscape
        ? Size(_selectedDevice.viewport.height, _selectedDevice.viewport.width)
        : _selectedDevice.viewport;
    final frameSize = Size(viewportSize.width + 28, viewportSize.height + 28);
    final safePadding = _isLandscape
        ? const EdgeInsets.symmetric(horizontal: 20)
        : const EdgeInsets.only(top: 28, bottom: 12);

    return ColoredBox(
      color: const Color(0xFFF1EEE7),
      child: SafeArea(
        minimum: const EdgeInsets.all(20),
        child: Column(
          children: [
            _PreviewToolbar(
              devices: _devices,
              selectedDevice: _selectedDevice,
              isLandscape: _isLandscape,
              onDeviceSelected: (device) {
                setState(() => _selectedDevice = device);
              },
              onRotatePressed: () {
                setState(() => _isLandscape = !_isLandscape);
              },
            ),
            const SizedBox(height: 20),
            Expanded(
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final previewHeight = math.max(
                    0.0,
                    constraints.maxHeight - 8,
                  );
                  return Center(
                    child: ConstrainedBox(
                      constraints: BoxConstraints(
                        maxWidth: constraints.maxWidth,
                        maxHeight: previewHeight,
                      ),
                      child: FittedBox(
                        fit: BoxFit.contain,
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            _PreviewHeader(
                              label: _selectedDevice.label,
                              viewportSize: viewportSize,
                              isLandscape: _isLandscape,
                            ),
                            const SizedBox(height: 14),
                            Container(
                              width: frameSize.width,
                              height: frameSize.height,
                              padding: const EdgeInsets.all(14),
                              decoration: BoxDecoration(
                                color: const Color(0xFF1D2127),
                                borderRadius: BorderRadius.circular(42),
                                boxShadow: const [
                                  BoxShadow(
                                    color: Color(0x33111A23),
                                    blurRadius: 48,
                                    offset: Offset(0, 28),
                                  ),
                                ],
                              ),
                              child: ClipRRect(
                                borderRadius: BorderRadius.circular(30),
                                child: Container(
                                  color: theme.scaffoldBackgroundColor,
                                  child: _PreviewViewport(
                                    designSize: widget.designSize,
                                    minTextAdapt: widget.minTextAdapt,
                                    splitScreenMode: widget.splitScreenMode,
                                    viewportSize: viewportSize,
                                    safePadding: safePadding,
                                    previewKey: ValueKey(
                                      '${_selectedDevice.id}:${_isLandscape ? 'landscape' : 'portrait'}',
                                    ),
                                    mediaQuery: mediaQuery,
                                    child: widget.child,
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PreviewViewport extends StatelessWidget {
  const _PreviewViewport({
    required this.designSize,
    required this.minTextAdapt,
    required this.splitScreenMode,
    required this.viewportSize,
    required this.safePadding,
    required this.previewKey,
    required this.mediaQuery,
    required this.child,
  });

  final Size designSize;
  final bool minTextAdapt;
  final bool splitScreenMode;
  final Size viewportSize;
  final EdgeInsets safePadding;
  final Key previewKey;
  final MediaQueryData mediaQuery;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final previewMediaQuery = mediaQuery.copyWith(
      size: viewportSize,
      padding: safePadding,
      viewPadding: safePadding,
      viewInsets: EdgeInsets.zero,
    );

    ScreenUtil.configure(
      data: previewMediaQuery,
      designSize: designSize,
      splitScreenMode: splitScreenMode,
      minTextAdapt: minTextAdapt,
    );

    return MediaQuery(
      data: previewMediaQuery,
      child: SizedBox(
        width: viewportSize.width,
        height: viewportSize.height,
        child: KeyedSubtree(key: previewKey, child: child),
      ),
    );
  }
}

class _PreviewToolbar extends StatelessWidget {
  const _PreviewToolbar({
    required this.devices,
    required this.selectedDevice,
    required this.isLandscape,
    required this.onDeviceSelected,
    required this.onRotatePressed,
  });

  final List<_PreviewDevice> devices;
  final _PreviewDevice selectedDevice;
  final bool isLandscape;
  final ValueChanged<_PreviewDevice> onDeviceSelected;
  final VoidCallback onRotatePressed;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white.withValues(alpha: 0.9),
      elevation: 0,
      borderRadius: BorderRadius.circular(24),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
        child: Wrap(
          spacing: 10,
          runSpacing: 10,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            const _PreviewBadge(label: 'Preview Mode'),
            for (final device in devices)
              ChoiceChip(
                label: Text(device.label),
                selected: device.id == selectedDevice.id,
                onSelected: (_) => onDeviceSelected(device),
              ),
            FilledButton.tonalIcon(
              onPressed: onRotatePressed,
              icon: Icon(
                isLandscape
                    ? Icons.stay_current_portrait
                    : Icons.screen_rotation,
              ),
              label: Text(isLandscape ? 'Portrait' : 'Landscape'),
            ),
          ],
        ),
      ),
    );
  }
}

class _PreviewHeader extends StatelessWidget {
  const _PreviewHeader({
    required this.label,
    required this.viewportSize,
    required this.isLandscape,
  });

  final String label;
  final Size viewportSize;
  final bool isLandscape;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final sizeLabel =
        '${viewportSize.width.toInt()} x ${viewportSize.height.toInt()}';

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.92),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            label,
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(width: 12),
          Text(
            sizeLabel,
            style: theme.textTheme.bodyMedium?.copyWith(
              color: const Color(0xFF667085),
            ),
          ),
          const SizedBox(width: 12),
          _PreviewBadge(label: isLandscape ? 'Landscape' : 'Portrait'),
        ],
      ),
    );
  }
}

class _PreviewBadge extends StatelessWidget {
  const _PreviewBadge({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xFF18212D),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelLarge?.copyWith(
          color: Colors.white,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _PreviewDevice {
  const _PreviewDevice({
    required this.id,
    required this.label,
    required this.viewport,
  });

  final String id;
  final String label;
  final Size viewport;
}
