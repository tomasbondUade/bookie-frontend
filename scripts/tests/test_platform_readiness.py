"""Regression tests for static cross-platform preflight, not app feature tests."""
import tempfile
import unittest
from pathlib import Path

from scripts.check_platform_readiness import inspect_repo


MANIFEST = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-permission android:name="android.permission.INTERNET" />
  <application>
    <activity android:name=".MainActivity" android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>
"""


class PlatformReadinessTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.write("androidApp/src/main/AndroidManifest.xml", MANIFEST)
        self.write(
            "androidApp/src/main/kotlin/org/bookie/app/MainActivity.kt",
            "setContent { App() }",
        )
        self.write(
            "iosApp/iosApp/ContentView.swift",
            "MainViewControllerKt.MainViewController()\nComposeView()",
        )
        self.write(
            "iosApp/iosApp/iOSApp.swift",
            "WindowGroup { ContentView() }",
        )
        self.write(
            "shared/src/iosMain/kotlin/org/bookie/app/MainViewController.kt",
            "ComposeUIViewController { App() }",
        )
        self.write("shared/build.gradle.kts", "iosArm64()\niosSimulatorArm64()")

    def write(self, path, content):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")

    def failed(self):
        return [name for name, ok, _ in inspect_repo(self.root) if not ok]

    def test_valid_platform_configuration_passes(self):
        self.assertEqual([], self.failed())

    def test_missing_internet_permission_fails(self):
        self.write(
            "androidApp/src/main/AndroidManifest.xml",
            MANIFEST.replace(
                '  <uses-permission android:name="android.permission.INTERNET" />\n',
                "",
            ),
        )
        self.assertIn("Android INTERNET permission", self.failed())

    def test_missing_ios_host_fails(self):
        self.write("iosApp/iosApp/ContentView.swift", "struct ContentView {}")
        self.assertIn("iOS shared controller host", self.failed())

    def test_non_exported_launcher_fails(self):
        self.write(
            "androidApp/src/main/AndroidManifest.xml",
            MANIFEST.replace('android:exported="true"', 'android:exported="false"'),
        )
        self.assertIn("Android exported launcher", self.failed())

    def test_missing_simulator_target_fails(self):
        self.write("shared/build.gradle.kts", "iosArm64()")
        self.assertIn("KMP iOS device and Apple Silicon simulator targets", self.failed())

    def test_malformed_manifest_fails_cleanly(self):
        self.write("androidApp/src/main/AndroidManifest.xml", "<manifest>")
        self.assertIn("Android manifest parses", self.failed())


if __name__ == "__main__":
    unittest.main()
