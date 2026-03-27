import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:more_app/core/utils/goal_video_cache.dart';
import 'package:more_app/core/utils/goal_video_controller_factory.dart';
import 'package:video_player/video_player.dart';

class GoalVideoThumb extends StatefulWidget {
  const GoalVideoThumb({
    super.key,
    required this.goalId,
    this.assetPath,
    this.iconName,
    required this.progressRate,
    required this.size,
    this.borderRadius,
    this.fillWidth = false,
    this.playAnimation = false,
    this.forceLoading = false,
  });

  final String goalId;
  final String? assetPath;
  final String? iconName;
  final double progressRate;
  final double size;
  final double? borderRadius;
  final bool fillWidth;
  final bool playAnimation;
  final bool forceLoading;

  @override
  State<GoalVideoThumb> createState() => _GoalVideoThumbState();
}

class _GoalVideoThumbState extends State<GoalVideoThumb> {
  VideoPlayerController? _controller;
  bool _ready = false;
  bool _error = false;
  bool _stopped = false;
  int _loadVersion = 0;

  @override
  void initState() {
    super.initState();
    _reloadVideo();
  }

  @override
  void didUpdateWidget(covariant GoalVideoThumb oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.goalId != widget.goalId ||
        oldWidget.assetPath != widget.assetPath ||
        oldWidget.iconName != widget.iconName ||
        oldWidget.progressRate != widget.progressRate ||
        oldWidget.playAnimation != widget.playAnimation) {
      _reloadVideo();
    }
  }

  Future<void> _reloadVideo() async {
    final version = ++_loadVersion;
    final previousController = _controller;
    _controller = null;
    _ready = false;
    _error = false;
    _stopped = false;
    if (mounted) {
      setState(() {});
    }
    await previousController?.dispose();
    await _initVideo(version);
  }

  Future<void> _initVideo(int version) async {
    final source = await GoalVideoCache.resolve(
      goalId: widget.goalId,
      sourcePath: widget.assetPath,
      iconName: widget.iconName,
    );

    if (!mounted || version != _loadVersion) {
      return;
    }

    final ctrl = createGoalVideoController(source);

    try {
      await ctrl.initialize();
      if (!mounted || version != _loadVersion) {
        ctrl.dispose();
        return;
      }

      final target = ctrl.value.duration * widget.progressRate.clamp(0.0, 1.0);

      if (widget.playAnimation &&
          widget.progressRate > 0.0 &&
          widget.progressRate < 1.0) {
        ctrl.addListener(() {
          if (_stopped) {
            return;
          }
          if (ctrl.value.position >= target) {
            _stopped = true;
            ctrl.pause();
            ctrl.seekTo(target);
          }
        });
        setState(() {
          _controller = ctrl;
          _ready = true;
        });
        await ctrl.seekTo(Duration.zero);
        await ctrl.play();
      } else {
        await ctrl.seekTo(target);
        await ctrl.pause();
        setState(() {
          _controller = ctrl;
          _ready = true;
        });
      }
    } catch (_) {
      ctrl.dispose();
      if (mounted && version == _loadVersion) {
        setState(() => _error = true);
      }
    }
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final radius = widget.borderRadius ?? 14.r;
    final width = widget.fillWidth ? double.infinity : widget.size;
    final height = widget.size;

    if (widget.forceLoading || !_ready || _error) {
      final colors = AppColors.of(context);
      return Container(
        width: width,
        height: height,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(radius),
          gradient: const LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFFFFF2E0), Color(0xFFF6FAEF)],
          ),
          border: Border.all(color: colors.gray500.withValues(alpha: 0.15)),
        ),
        child: Center(
          child: _error
              ? Icon(
                  Icons.broken_image_outlined,
                  color: colors.textTertiary,
                  size: height * 0.4,
                )
              : SizedBox(
                  width: height * 0.24,
                  height: height * 0.24,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.4,
                    color: AppColors.orange500,
                    backgroundColor: colors.gray500.withValues(alpha: 0.2),
                  ),
                ),
        ),
      );
    }

    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: SizedBox(
        width: width,
        height: height,
        child: FittedBox(
          fit: BoxFit.cover,
          clipBehavior: Clip.hardEdge,
          child: SizedBox(
            width: _controller!.value.size.width,
            height: _controller!.value.size.height,
            child: VideoPlayer(_controller!),
          ),
        ),
      ),
    );
  }
}
