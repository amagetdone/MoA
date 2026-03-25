import 'package:more_app/core/utils/goal_video_source.dart';

class GoalVideoCache {
  GoalVideoCache._();

  static String resolveFallbackAsset(String? assetPath, String? iconName) {
    if (assetPath != null &&
        assetPath.isNotEmpty &&
        !assetPath.startsWith('http://') &&
        !assetPath.startsWith('https://')) {
      return assetPath;
    }

    switch (iconName) {
      case 'travel':
        return 'assets/videos/goal_sketch_jeju.webm';
      default:
        return 'assets/videos/goal_sketch_jeju.webm';
    }
  }

  static Future<GoalVideoSource> resolve({
    required String goalId,
    String? sourcePath,
    String? iconName,
  }) async {
    final fallbackAsset = resolveFallbackAsset(sourcePath, iconName);

    if (sourcePath == null || sourcePath.isEmpty) {
      return GoalVideoSource.asset(fallbackAsset);
    }

    if (sourcePath.startsWith('http://') || sourcePath.startsWith('https://')) {
      return GoalVideoSource.network(sourcePath);
    }

    return GoalVideoSource.asset(fallbackAsset);
  }

  static Future<void> evict(String goalId) async {}
}
