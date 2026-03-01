package com.tsiantakis.flutterapp.flutter_app

import io.flutter.embedding.engine.FlutterShellArgs
import io.flutter.embedding.android.FlutterActivity

class MainActivity : FlutterActivity() {
    override fun getFlutterShellArgs(): FlutterShellArgs {
        val args = super.getFlutterShellArgs()
        args.add("--trace-systrace")
        return args
    }
}
