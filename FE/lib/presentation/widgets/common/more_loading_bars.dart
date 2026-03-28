import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';

class MoreLoadingBars extends StatelessWidget {
  const MoreLoadingBars({
    super.key,
    required this.animation,
    required this.color,
    this.width,
    this.height,
  });

  final Animation<double> animation;
  final Color color;
  final double? width;
  final double? height;

  @override
  Widget build(BuildContext context) {
    final widgetWidth = width ?? 80.w;
    final widgetHeight = height ?? 48.h;

    return SizedBox(
      width: widgetWidth,
      height: widgetHeight,
      child: AnimatedBuilder(
        animation: animation,
        builder: (context, child) {
          return Align(
            alignment: Alignment.bottomCenter,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: List.generate(5, (index) {
                final delay = index * 0.15;
                final value = (animation.value + delay) % 1.0;
                final barHeight = 12.h + 24.h * _barHeight(value);
                return Container(
                  width: 6.w,
                  height: barHeight,
                  margin: EdgeInsets.symmetric(horizontal: 3.w),
                  decoration: BoxDecoration(
                    color: color,
                    borderRadius: BorderRadius.circular(3.r),
                  ),
                );
              }),
            ),
          );
        },
      ),
    );
  }

  double _barHeight(double value) {
    return 1 - (2 * value - 1).abs();
  }
}
