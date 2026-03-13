class Goal {
  final String id;
  final String name;
  final String? description;
  final int targetAmount;
  final int currentAmount;
  final DateTime startDate;
  final DateTime endDate;
  final int priority;
  final GoalStatus status;
  final String? iconName;
  final String? sketchVideoAssetPath;

  const Goal({
    required this.id,
    required this.name,
    this.description,
    required this.targetAmount,
    required this.currentAmount,
    required this.startDate,
    required this.endDate,
    this.priority = 0,
    this.status = GoalStatus.inProgress,
    this.iconName,
    this.sketchVideoAssetPath,
  });

  double get progressRate =>
      targetAmount > 0 ? (currentAmount / targetAmount).clamp(0.0, 1.0) : 0.0;

  int get remainingAmount =>
      (targetAmount - currentAmount).clamp(0, targetAmount);

  int get remainingDays {
    final now = DateTime.now();
    return endDate.difference(now).inDays;
  }

  Goal copyWith({
    String? id,
    String? name,
    String? description,
    int? targetAmount,
    int? currentAmount,
    DateTime? startDate,
    DateTime? endDate,
    int? priority,
    GoalStatus? status,
    String? iconName,
    String? sketchVideoAssetPath,
  }) {
    return Goal(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      targetAmount: targetAmount ?? this.targetAmount,
      currentAmount: currentAmount ?? this.currentAmount,
      startDate: startDate ?? this.startDate,
      endDate: endDate ?? this.endDate,
      priority: priority ?? this.priority,
      status: status ?? this.status,
      iconName: iconName ?? this.iconName,
      sketchVideoAssetPath: sketchVideoAssetPath ?? this.sketchVideoAssetPath,
    );
  }
}

enum GoalStatus { inProgress, achieved, failed }
