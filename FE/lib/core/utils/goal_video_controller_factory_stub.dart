import 'package:more_app/core/utils/goal_video_source.dart';
import 'package:video_player/video_player.dart';

VideoPlayerController createGoalVideoController(GoalVideoSource source) {
  switch (source.type) {
    case GoalVideoSourceType.network:
      return VideoPlayerController.networkUrl(Uri.parse(source.path));
    case GoalVideoSourceType.asset:
      return VideoPlayerController.asset(source.path);
    case GoalVideoSourceType.file:
      return VideoPlayerController.networkUrl(Uri.parse(source.path));
  }
}
