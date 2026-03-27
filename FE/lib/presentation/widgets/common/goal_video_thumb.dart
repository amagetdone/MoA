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
  static const Duration _endFrameEpsilon = Duration(milliseconds: 50);
  static const Duration _frameWarmupDelay = Duration(milliseconds: 180);
  static const Duration _firstFrameTimeout = Duration(milliseconds: 900);
  static const Duration _firstFramePollInterval = Duration(milliseconds: 40);
  static const Duration _minimumVisiblePlayback = Duration(milliseconds: 50);
  VideoPlayerController? _controller;
  bool _ready = false;
  bool _displayReady = false;
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
    _displayReady = false;
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

      await ctrl.setLooping(false);

      final target = _resolveTargetPosition(ctrl.value.duration);
      final shouldAnimate = widget.playAnimation && target > Duration.zero;

      if (shouldAnimate) {
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

        await ctrl.seekTo(Duration.zero);
        if (!mounted || version != _loadVersion) {
          ctrl.dispose();
          return;
        }

        setState(() {
          _controller = ctrl;
          _ready = true;
          _displayReady = false;
        });
        await ctrl.play();
        if (!await _waitForFirstFrame(
          ctrl,
          version,
          minimumPosition: _minimumVisiblePlayback,
        )) {
          return;
        }
        await Future<void>.delayed(_frameWarmupDelay);
        if (!mounted || version != _loadVersion || _controller != ctrl) {
          return;
        }
        setState(() => _displayReady = true);
      } else {
        setState(() {
          _controller = ctrl;
          _ready = true;
          _displayReady = false;
        });
        if (!await _warmUpStaticFrame(ctrl, version, target: target)) {
          return;
        }
        setState(() => _displayReady = true);
      }
    } catch (_) {
      ctrl.dispose();
      if (mounted && version == _loadVersion) {
        setState(() => _error = true);
      }
    }
  }

  Future<bool> _warmUpStaticFrame(
    VideoPlayerController ctrl,
    int version, {
    required Duration target,
  }) async {
    await WidgetsBinding.instance.endOfFrame;
    if (!mounted || version != _loadVersion || _controller != ctrl) {
      return false;
    }

    final warmupStart = target > const Duration(milliseconds: 180)
        ? target - const Duration(milliseconds: 180)
        : Duration.zero;

    await ctrl.seekTo(warmupStart);
    await ctrl.play();
    if (!await _waitForFirstFrame(
      ctrl,
      version,
      minimumPosition: warmupStart + _minimumVisiblePlayback,
    )) {
      return false;
    }
    await ctrl.pause();
    await ctrl.seekTo(target);
    await Future<void>.delayed(_frameWarmupDelay);

    return mounted && version == _loadVersion && _controller == ctrl;
  }

  Future<bool> _waitForFirstFrame(
    VideoPlayerController ctrl,
    int version, {
    required Duration minimumPosition,
  }) async {
    final deadline = DateTime.now().add(_firstFrameTimeout);

    while (DateTime.now().isBefore(deadline)) {
      if (!mounted || version != _loadVersion || _controller != ctrl) {
        return false;
      }

      if (ctrl.value.position >= minimumPosition && !ctrl.value.isBuffering) {
        return true;
      }

      await Future<void>.delayed(_firstFramePollInterval);
    }

    return mounted && version == _loadVersion && _controller == ctrl;
  }

  Duration _resolveTargetPosition(Duration duration) {
    if (duration <= Duration.zero) {
      return Duration.zero;
    }

    final clampedProgress = widget.progressRate.clamp(0.0, 1.0);
    final totalMilliseconds = duration.inMilliseconds;
    var targetMilliseconds = (totalMilliseconds * clampedProgress).round();

    if (clampedProgress >= 1.0 &&
        totalMilliseconds > _endFrameEpsilon.inMilliseconds) {
      targetMilliseconds = totalMilliseconds - _endFrameEpsilon.inMilliseconds;
    }

    targetMilliseconds = targetMilliseconds.clamp(0, totalMilliseconds);
    return Duration(milliseconds: targetMilliseconds);
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

    final colors = AppColors.of(context);
    final loadingOverlay = Container(
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
        child: SizedBox(
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

    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: SizedBox(
        width: width,
        height: height,
        child: Stack(
          fit: StackFit.expand,
          children: [
            Opacity(
              opacity: _displayReady ? 1 : 0,
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
            if (!_displayReady) loadingOverlay,
          ],
        ),
      ),
    );
  }
}
