import 'package:flutter/material.dart';

ThemeData buildTheme() {
  final base = ThemeData(colorSchemeSeed: const Color(0xFFb91c1c), useMaterial3: true);
  return base.copyWith(
    appBarTheme: const AppBarTheme(centerTitle: true),
  );
}
