# CameraX2

A camera plugin for flutter, which use CameraX on Android, native API on iOS, supports camera preview, capture and analyze face detection.

*Note*: This plugin is inspired by the official [camerax](https://pub.dev/packages/camerax) project but more simple to use, and use [MLKit](https://developers.google.cn/ml-kit) in native for image analyze.

## Features

- [x] CameraView widget.
- [x] Control the torch.
- [x] Access to a face stream.
- [x] Photo capture ability.

## Getting Started

Add `camerax2` as a [dependency in your pubspec.yaml file](https://flutter.dev/using-packages/).

```
dependencies:
  camerax2: ^<latest-version>
```

### Android

Make sure you have a `miniSdkVersion` with 21 or higher in your `android/app/build.gradle` file, because the camera2 API which CameraX used only support Android 5.0 or above.

*Note*: You can run the example on a device emulator with Android 11 or higher and physical devices, CameraX doesn't work when running on emulators with Android 10 or lower. See https://developer.android.google.cn/codelabs/camerax-getting-started#5

### iOS

Make sure you have a minimum deployment target of 10.0, because the `MLKit` only support iOS 10.0 or above, you can uncomment the first line `platform :ios, '10.0'` in your iOS project's `Podfile`

## Issues

- Doesn't work with horizontal orientation.
- No tests for now.
