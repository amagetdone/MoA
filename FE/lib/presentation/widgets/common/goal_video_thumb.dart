import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:video_player/video_player.dart';

/// 목표 달성률에 해당하는 프레임 하나를 정지 상태로 보여주는 썸네일 위젯.
///
/// - [assetPath] : 직접 에셋 경로 지정 (goal.sketchVideoAssetPath). 있으면 우선 사용.
/// - [iconName]  : assetPath 없을 때 내부 매핑으로 fallback
/// - [progressRate] : 0.0 ~ 1.0 (예: 0.7 → 영상 70% 지점 프레임을 정지 표시)
/// - [size] : 위젯 너비 & 높이 (정사각형)
/// - [borderRadius] : 모서리 반경
class GoalVideoThumb extends StatefulWidget {
  const GoalVideoThumb({
    super.key,
    this.assetPath,
    this.iconName,
    required this.progressRate,
    required this.size,
    this.borderRadius,
    this.fillWidth = false,
    this.playAnimation = false, // true면 0→progressRate까지 재생 후 정지 (상세 페이지용)
  });

  final String? assetPath;
  final String? iconName;
  final double progressRate;
  final double size;
  final double? borderRadius;
  final bool fillWidth;
  final bool playAnimation;

  /// assetPath 우선, 없으면 iconName → 에셋 경로 매핑.
  static String resolveAsset(String? assetPath, String? iconName) {
    if (assetPath != null && assetPath.isNotEmpty) return assetPath;
    switch (iconName) {
      case 'travel':
        return 'assets/videos/goal_sketch_jeju.webm';
      // TODO: 다른 카테고리 영상 추가 시 여기에 case 추가
      default:
        return 'assets/videos/goal_sketch_jeju.webm';
    }
  }

  @override
  State<GoalVideoThumb> createState() => _GoalVideoThumbState();
}

class _GoalVideoThumbState extends State<GoalVideoThumb> {
  VideoPlayerController? _controller;
  bool _ready = false;
  bool _error = false;
  bool _stopped = false;

  @override
  void initState() {
    super.initState();
    _initVideo();
  }

  Future<void> _initVideo() async {
    final path = GoalVideoThumb.resolveAsset(widget.assetPath, widget.iconName);
    final ctrl = VideoPlayerController.asset(path);

    try {
      await ctrl.initialize();
      if (!mounted) { ctrl.dispose(); return; }

      final target = ctrl.value.duration * widget.progressRate.clamp(0.0, 1.0);

      if (widget.playAnimation && widget.progressRate > 0.0 && widget.progressRate < 1.0) {
        // 상세 페이지: 0에서 progressRate 지점까지 재생 후 정지
        ctrl.addListener(() {
          if (_stopped) return;
          if (ctrl.value.position >= target) {
            _stopped = true;
            ctrl.pause();
            ctrl.seekTo(target);
          }
        });
        setState(() { _controller = ctrl; _ready = true; });
        await ctrl.seekTo(Duration.zero);
        await ctrl.play();
      } else {
        // 썸네일: 해당 프레임으로 바로 seek 후 정지
        await ctrl.seekTo(target);
        await ctrl.pause();
        setState(() { _controller = ctrl; _ready = true; });
      }
    } catch (_) {
      ctrl.dispose();
      if (mounted) setState(() => _error = true);
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

    final double w = widget.fillWidth ? double.infinity : widget.size;
    final double h = widget.size;

    // 로딩 중 / 에러: 그라디언트 플레이스홀더
    if (!_ready || _error) {
      return Container(
        width: w,
        height: h,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(radius),
          gradient: const LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFFFFF2E0), Color(0xFFF6FAEF)],
          ),
          border: Border.all(
            color: AppColors.gray500.withValues(alpha: 0.15),
          ),
        ),
        child: _error
            ? Icon(Icons.broken_image_outlined,
                color: AppColors.textTertiary, size: h * 0.4)
            : const SizedBox.shrink(),
      );
    }

    // 비디오 프레임 표시
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: SizedBox(
        width: w,
        height: h,
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
