import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:more_app/domain/entities/user.dart';

part 'user_dto.freezed.dart';
part 'user_dto.g.dart';

@freezed
abstract class UserDto with _$UserDto {
  const factory UserDto({
    required String id,
    required String name,
    required String email,
    @JsonKey(name: 'profile_image_url') String? profileImageUrl,
  }) = _UserDto;

  factory UserDto.fromJson(Map<String, dynamic> json) =>
      _$UserDtoFromJson(json);
}

extension UserDtoX on UserDto {
  User toEntity() => User(
    id: id,
    name: name,
    email: email,
    profileImageUrl: profileImageUrl,
  );
}
