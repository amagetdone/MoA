import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:more_app/core/theme/app_colors.dart';
import 'package:video_player/video_player.dart';

/// 목표 달성률만큼 영상을 재생한 뒤 정지된 프레임을 보여주는 썸네일 위젯.
///
/// - [assetPath] : 직접 에셋 경로 지정 (goal.sketchVideoAssetPath). 있으면 우선 사용.
/// - [iconName]  : assetPath 없을 때 내부 매핑으로 fallback
/// - [progressRate] : 0.0 ~ 1.0 (예: 0.7 → 전체 길이의 70% 지점에서 정지)
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
    this.fillWidth = false, // true면 width: double.infinity, size는 height로 사용
  });

  final String? assetPath;
  final String? iconName;
  final double progressRate;
  final double size;
  final double? borderRadius;
  final bool fillWidth;

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
  bool _stopped = false; // 목표 지점 도달 후 중복 pause 방지

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

      // 달성률이 0이면 그냥 첫 프레임 정지
      if (widget.progressRate <= 0.0) {
        await ctrl.seekTo(Duration.zero);
        await ctrl.pause();
        setState(() { _controller = ctrl; _ready = true; });
        return;
      }

      // 달성률이 100% 이상이면 마지막 프레임으로
      if (widget.progressRate >= 1.0) {
        await ctrl.seekTo(ctrl.value.duration);
        await ctrl.pause();
        setState(() { _controller = ctrl; _ready = true; });
        return;
      }

      // 0 → target 지점까지 재생 후 자동 정지하는 listener 등록
      ctrl.addListener(() {
        if (_stopped) return;
        final pos = ctrl.value.position;
        if (pos >= target) {
          _stopped = true;
          ctrl.pause();
          ctrl.seekTo(target); // 딱 목표 프레임에 고정
        }
      });

      setState(() { _controller = ctrl; _ready = true; });

      // 처음부터 재생 시작
      await ctrl.seekTo(Duration.zero);
      await ctrl.play();
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
