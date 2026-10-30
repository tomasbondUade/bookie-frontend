#!/usr/bin/env python3
"""Static Android/iOS entrypoint preflight; NOT a runtime or end-to-end test.

Uses Python's standard library only so it can run on Linux CI and macOS.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"


def inspect_repo(root: Path) -> list[tuple[str, bool, str]]:
    results: list[tuple[str, bool, str]] = []

    def check(label: str, condition: bool, explanation: str) -> None:
        results.append((label, condition, explanation))

    def source(relative: str) -> str:
        try:
            return (root / relative).read_text(encoding="utf-8")
        except FileNotFoundError:
            return ""

    manifest_text = source("androidApp/src/main/AndroidManifest.xml")
    try:
        manifest = ET.fromstring(manifest_text)
    except ET.ParseError:
        manifest = None
    check("Android manifest parses", manifest is not None, "AndroidManifest.xml must be valid XML")
    if manifest is not None:
        internet = any(
            node.get(ANDROID + "name") == "android.permission.INTERNET"
            for node in manifest.findall("uses-permission")
        )
        check("Android INTERNET permission", internet, "Bookie/Supabase need network permission")
        application = manifest.find("application")
        activities = application.findall("activity") if application is not None else []
        launcher = any(
            activity.get(ANDROID + "name") in (".MainActivity", "org.bookie.app.MainActivity")
            and activity.get(ANDROID + "exported") == "true"
            and any(
                any(
                    action.get(ANDROID + "name") == "android.intent.action.MAIN"
                    for action in intent.findall("action")
                )
                and any(
                    category.get(ANDROID + "name") == "android.intent.category.LAUNCHER"
                    for category in intent.findall("category")
                )
                for intent in activity.findall("intent-filter")
            )
            for activity in activities
        )
        check("Android exported launcher", launcher, "MainActivity needs MAIN/LAUNCHER")
    else:
        check("Android INTERNET permission", False, "Cannot inspect invalid manifest")
        check("Android exported launcher", False, "Cannot inspect invalid manifest")

    android = source("androidApp/src/main/kotlin/org/bookie/app/MainActivity.kt")
    check(
        "Android Compose host",
        "setContent" in android and "App()" in android,
        "MainActivity must render shared App()",
    )

    swift = source("iosApp/iosApp/ContentView.swift")
    check(
        "iOS shared controller host",
        "MainViewControllerKt.MainViewController()" in swift and "ComposeView()" in swift,
        "SwiftUI ContentView must host the shared Compose controller",
    )
    ios_app = source("iosApp/iosApp/iOSApp.swift")
    check(
        "iOS application entrypoint",
        "WindowGroup" in ios_app and "ContentView()" in ios_app,
        "iOSApp must display ContentView",
    )
    controller = source("shared/src/iosMain/kotlin/org/bookie/app/MainViewController.kt")
    check(
        "iOS Compose controller",
        "ComposeUIViewController" in controller and "App()" in controller,
        "Shared MainViewController must render App()",
    )
    gradle = source("shared/build.gradle.kts")
    check(
        "KMP iOS device and Apple Silicon simulator targets",
        "iosArm64()" in gradle and "iosSimulatorArm64()" in gradle,
        "Both configured iOS targets must be present",
    )
    return results


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root", type=Path, default=Path(__file__).resolve().parent.parent,
        help="Repository root (defaults to the parent of scripts/)",
    )
    args = parser.parse_args()
    checks = inspect_repo(args.root)
    for name, passed, detail in checks:
        print(f"{'PASS' if passed else 'FAIL'}: {name} - {detail}")
    print(f"Static preflight: {sum(ok for _, ok, _ in checks)}/{len(checks)} passed")
    print("NOTE: This does not run iOS, Android UI flows, a live API, or accessibility checks.")
    return 0 if all(ok for _, ok, _ in checks) else 1


if __name__ == "__main__":
    sys.exit(main())
