import 'package:flutter/material.dart';
import 'package:flutter_app/screens/benchmarks.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    WakelockPlus.enable();
    return MaterialApp(
      debugShowCheckedModeBanner: true,
      title: 'FlutterApp',
      theme: ThemeData(colorScheme: .fromSeed(seedColor: Colors.deepPurple)),
      home: Benchmarks(),
    );
  }
}
