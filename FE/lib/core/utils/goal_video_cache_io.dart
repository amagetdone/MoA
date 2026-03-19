import 'dart:io';

import 'package:dio/dio.dart';
import 'package:more_app/core/utils/goal_video_source.dart';
import 'package:path_provider/path_provider.dart';

class GoalVideoCache {
  GoalVideoCache._();

  static final Dio _dio = Dio();
  static final Map<String, Future<String?>> _pendingDownloads = {};

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

    if (sourcePath.startsWith('assets/')) {
      return GoalVideoSource.asset(sourcePath);
    }

    if (sourcePath.startsWith('http://') || sourcePath.startsWith('https://')) {
      final cachedPath = await _findExistingCachedPath(goalId);
      if (cachedPath != null) {
        return GoalVideoSource.file(cachedPath);
      }

      final downloadedPath = await _downloadOnce(goalId, sourcePath);
      if (downloadedPath != null) {
        return GoalVideoSource.file(downloadedPath);
      }

      return GoalVideoSource.network(sourcePath);
    }

    final file = File(sourcePath);
    if (await file.exists()) {
      return GoalVideoSource.file(file.path);
    }

    return GoalVideoSource.asset(fallbackAsset);
  }

  static Future<String?> _downloadOnce(String goalId, String url) {
    return _pendingDownloads.putIfAbsent(goalId, () async {
      try {
        return await _download(goalId, url);
      } finally {
        _pendingDownloads.remove(goalId);
      }
    });
  }

  static Future<String?> _findExistingCachedPath(String goalId) async {
    final directory = await _getGoalVideoDirectory();
    for (final ext in const ['mp4', 'webm']) {
      final file = File('${directory.path}/goal_$goalId.$ext');
      if (await file.exists() && await file.length() > 0) {
        return file.path;
      }
    }
    return null;
  }

  static Future<String?> _download(String goalId, String url) async {
    try {
      final response = await _dio.get<List<int>>(
        url,
        options: Options(responseType: ResponseType.bytes),
      );
      final bytes = response.data;
      if (bytes == null || bytes.isEmpty) {
        return null;
      }

      final extension = _resolveExtension(
        url,
        response.headers.value(HttpHeaders.contentTypeHeader),
      );
      final directory = await _getGoalVideoDirectory();
      await _deleteOtherExtensions(directory, goalId, keepExtension: extension);

      final file = File('${directory.path}/goal_$goalId.$extension');
      await file.writeAsBytes(bytes, flush: true);
      return file.path;
    } catch (_) {
      return null;
    }
  }

  static Future<Directory> _getGoalVideoDirectory() async {
    final baseDir = await getApplicationSupportDirectory();
    final dir = Directory('${baseDir.path}/goal_videos');
    if (!await dir.exists()) {
      await dir.create(recursive: true);
    }
    return dir;
  }

  static Future<void> _deleteOtherExtensions(
    Directory directory,
    String goalId, {
    required String keepExtension,
  }) async {
    for (final ext in const ['mp4', 'webm']) {
      if (ext == keepExtension) {
        continue;
      }
      final file = File('${directory.path}/goal_$goalId.$ext');
      if (await file.exists()) {
        await file.delete();
      }
    }
  }

  static String _resolveExtension(String url, String? contentType) {
    final path = Uri.tryParse(url)?.path.toLowerCase() ?? url.toLowerCase();
    if (path.endsWith('.webm')) {
      return 'webm';
    }
    if (path.endsWith('.mp4')) {
      return 'mp4';
    }

    final normalizedContentType = (contentType ?? '').toLowerCase();
    if (normalizedContentType.contains('webm')) {
      return 'webm';
    }
    if (normalizedContentType.contains('mp4')) {
      return 'mp4';
    }

    return 'mp4';
  }
}
