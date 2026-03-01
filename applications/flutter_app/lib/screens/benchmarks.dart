import 'dart:developer' as dev;
import 'dart:io';
import 'dart:math';

import 'package:contacts_service_plus/contacts_service_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:light/light.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:sensors_plus/sensors_plus.dart';

const int repetitions = 30;

Future<void> repeat(int iterations, Future<void> Function() block) async {
  for (var i = 0; i < iterations; i++) {
    await block();
  }
}

class Benchmarks extends StatefulWidget {
  const Benchmarks({super.key});

  @override
  State<Benchmarks> createState() => _BenchmarksState();
}

class _BenchmarksState extends State<Benchmarks> {
  bool runningTest = false;

  @override
  void initState() {
    super.initState();
    _checkPermissions();
  }

  void _checkPermissions() async {
    await [Permission.location, Permission.contacts].request();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("FlutterApp")),
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            spacing: 10,
            children: [
              Text("Release mode: $kReleaseMode"),
              Text("Profile mode: $kProfileMode"),
              Text("Debug mode: $kDebugMode"),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () async {
                    setState(() {
                      runningTest = true;
                    });
                    await repeat(repetitions, () {
                      return accelerometerTest();
                    });
                    setState(() {
                      runningTest = false;
                    });
                  },
                  child: Text("Run accelerometer test"),
                ),
              ),

              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () async {
                    setState(() {
                      runningTest = true;
                    });
                    var file = await loadImageToDevice();
                    await repeat(repetitions, () {
                      return imageTest();
                    });
                    if (await file.exists()) {
                      await file.delete();
                    }
                    if (!mounted) {
                      return;
                    }
                    setState(() {
                      runningTest = false;
                    });
                  },
                  child: Text("Run image test"),
                ),
              ),

              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () async {
                    setState(() {
                      runningTest = true;
                    });
                    await repeat(repetitions, () {
                      return contactTest("John Doe", "6934567890");
                    });
                    if (!mounted) {
                      return;
                    }
                    setState(() {
                      runningTest = false;
                    });
                  },
                  child: Text("Run contacts test"),
                ),
              ),

              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () async {
                    setState(() {
                      runningTest = true;
                    });
                    await repeat(repetitions, () {
                      return lightTest();
                    });
                    if (!mounted) {
                      return;
                    }
                    setState(() {
                      runningTest = false;
                    });
                  },
                  child: Text("Run light test"),
                ),
              ),

              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () async {
                    setState(() {
                      runningTest = true;
                    });
                    await compute(mathIsolate, repetitions);
                    if (!mounted) {
                      return;
                    }
                    setState(() {
                      runningTest = false;
                    });
                  },
                  child: Text("Run math test"),
                ),
              ),

              Visibility(
                visible: runningTest,
                child: Column(
                  children: [
                    CircularProgressIndicator(),
                    Text(
                      style: TextStyle(fontWeight: FontWeight.bold),
                      "Running a test please wait...",
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> accelerometerTest() async {
    var task = dev.TimelineTask();
    try {
      task.start("flutter:accelerometer");
      var measurement = await accelerometerEventStream(
        samplingPeriod: SensorInterval.fastestInterval,
      ).first;
      if (kDebugMode) {
        print(
          "Accelerometer value read ${measurement.x} ${measurement.y} ${measurement.z}",
        );
      }
    } on Exception catch (e) {
      throw StateError(e.toString());
    } finally {
      task.finish();
    }
  }

  Future<File> loadImageToDevice() async {
    try {
      final data = await rootBundle.load("assets/solar_system.jpg");
      final Uint8List bytesList = data.buffer.asUint8List();
      final directory = await getDownloadsDirectory();
      final file = File("${directory?.path}/solar_system.jpg");
      await file.writeAsBytes(bytesList);
      return file;
    } on Exception catch (e) {
      throw StateError(e.toString());
    }
  }

  Future<void> imageTest() async {
    final directory = await getDownloadsDirectory();
    var file = File("${directory?.path}/solar_system.jpg");
    var task = dev.TimelineTask();
    try {
      task.start("flutter:image");
      var bytes = await file.readAsBytes();
      if (kDebugMode) {
        print("Image read as ${bytes.toString()}");
      }
    } on Exception catch (e) {
      throw StateError(e.toString());
    } finally {
      task.finish();
    }
  }

  Future<void> contactTest(String name, phoneNumber) async {
    Contact newContact = Contact(
      givenName: name,
      phones: [Item(label: "mobile", value: phoneNumber)],
    );
    var task = dev.TimelineTask();
    try {
      task.start("flutter:contacts");
      await ContactsService.addContact(newContact);
      if (kDebugMode) {
        Iterable<Contact> contacts = await ContactsService.getContacts(
          query: newContact.givenName,
        );
        bool exists = contacts.any((c) => c.givenName == newContact.givenName);
        if (exists) {
          print("Contact created successfully.");
        }
      }
    } on Exception catch (e) {
      throw StateError(e.toString());
    } finally {
      task.finish();
      Iterable<Contact> fetchedContacts = await ContactsService.getContacts(
        query: newContact.givenName,
      );
      for (var c in fetchedContacts) {
        if (c.givenName == newContact.givenName) {
          ContactsService.deleteContact(c);
        }
      }
    }
  }

  Future<void> lightTest() async {
    var task = dev.TimelineTask();
    try {
      task.start("flutter:light");
      var lux = await Light().lightSensorStream.first;
      if (kDebugMode) {
        print("Read $lux from the light sensor");
      }
    } on Exception catch (e) {
      throw StateError(e.toString());
    } finally {
      task.finish();
    }
  }

  static void mathIsolate(int repetitions) async {
    return await repeat(repetitions, mathTest);
  }

  static Future<void> mathTest() async {
    var task = dev.TimelineTask();
    var localResult = 0.0;
    task.start("flutter:math");
    for (var j = 1; j <= 5; j++) {
      for (var k = 1; k <= 1000000; k++) {
        localResult +=
            (log(k.toDouble()) / ln2) +
            (3 * k / 2 * j) +
            sqrt(k.toDouble()) +
            pow(k.toDouble(), j - 1);
      }
    }
    task.finish();
    if (kDebugMode) {
      print("Math calculation result is $localResult");
    }
  }
}
