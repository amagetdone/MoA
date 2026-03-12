/// Base UseCase interface
/// [Type] - 반환 타입
/// [Params] - 파라미터 타입
abstract class UseCase<Type, Params> {
  Future<Type> call(Params params);
}

/// 파라미터가 없는 UseCase용
class NoParams {
  const NoParams();
}
