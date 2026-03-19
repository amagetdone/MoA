enum GoalVideoSourceType { asset, file, network }

class GoalVideoSource {
  const GoalVideoSource.asset(this.path) : type = GoalVideoSourceType.asset;
  const GoalVideoSource.file(this.path) : type = GoalVideoSourceType.file;
  const GoalVideoSource.network(this.path) : type = GoalVideoSourceType.network;

  final GoalVideoSourceType type;
  final String path;
}
