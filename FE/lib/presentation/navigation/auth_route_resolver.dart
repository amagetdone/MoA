import 'package:more_app/domain/entities/auth_result.dart';
import 'package:more_app/presentation/navigation/app_router.dart';

String resolvePostAuthRoute(
  AuthResult result, {
  bool isTest = false,
}) {
  if (result.status == 'TEMP') {
    return AppRoutes.onboardingProfile;
  }

  if (isTest || !result.account || !result.plan) {
    return AppRoutes.onboarding;
  }

  return AppRoutes.home;
}
