import 'dart:math';
import 'dart:ui';

import "package:google_mlkit_commons/google_mlkit_commons.dart";

/// A human face detected in an image.
class Face {
  /// The axis-aligned bounding rectangle of the detected face.
  ///
  /// The point (0, 0) is defined as the upper-left corner of the image.
  final Rect boundingBox;

  /// The rotation of the face about the horizontal axis of the image.
  ///
  /// Represented in degrees.
  ///
  /// A face with a positive Euler X angle is turned to the camera's up and down.
  ///
  final double? headEulerAngleX;

  /// The rotation of the face about the vertical axis of the image.
  ///
  /// Represented in degrees.
  ///
  /// A face with a positive Euler Y angle is turned to the camera's right and
  /// to its left.
  ///
  /// The Euler Y angle is guaranteed only when using the "accurate" mode
  /// setting of the face detector (as opposed to the "fast" mode setting, which
  /// takes some shortcuts to make detection faster).
  final double? headEulerAngleY;

  /// The rotation of the face about the axis pointing out of the image.
  ///
  /// Represented in degrees.
  ///
  /// A face with a positive Euler Z angle is rotated counter-clockwise relative
  /// to the camera.
  ///
  /// ML Kit always reports the Euler Z angle of a detected face.
  final double? headEulerAngleZ;

  /// Probability that the face's left eye is open.
  ///
  /// A value between 0.0 and 1.0 inclusive, or null if probability was not
  /// computed.
  final double? leftEyeOpenProbability;

  /// Probability that the face's right eye is open.
  ///
  /// A value between 0.0 and 1.0 inclusive, or null if probability was not
  /// computed.
  final double? rightEyeOpenProbability;

  /// Probability that the face is smiling.
  ///
  /// A value between 0.0 and 1.0 inclusive, or null if probability was not
  /// computed.
  final double? smilingProbability;

  /// The tracking ID if the tracking is enabled.
  ///
  /// Null if tracking was not enabled.
  final int? trackingId;

  /// Gets the landmark based on the provided [FaceLandmarkType].
  ///
  /// Null if landmark was not detected.
  final Map<FaceLandmarkType, FaceLandmark?>? landmarks;

  /// Gets the contour based on the provided [FaceContourType].
  ///
  /// Null if contour was not detected.
  final Map<FaceContourType, FaceContour?>? contours;

  Face({
    required this.boundingBox,
    this.landmarks,
    this.contours,
    this.headEulerAngleX,
    this.headEulerAngleY,
    this.headEulerAngleZ,
    this.leftEyeOpenProbability,
    this.rightEyeOpenProbability,
    this.smilingProbability,
    this.trackingId,
  });

  /// Returns an instance of [Face] from a given [json].
  factory Face.fromJson(Map<dynamic, dynamic> json) => Face(
        boundingBox: RectJson.fromJson(json['rect']),
        headEulerAngleX: json['headEulerAngleX'],
        headEulerAngleY: json['headEulerAngleY'],
        headEulerAngleZ: json['headEulerAngleZ'],
        leftEyeOpenProbability: json['leftEyeOpenProbability'],
        rightEyeOpenProbability: json['rightEyeOpenProbability'],
        smilingProbability: json['smilingProbability'],
        trackingId: json['trackingId'],
        // landmarks: Map<FaceLandmarkType, FaceLandmark?>.fromIterables(
        //   FaceLandmarkType.values,
        //   FaceLandmarkType.values.map(
        //     (FaceLandmarkType type) {
        //       final List<dynamic>? pos = json['landmarks'][type.name];
        //       return (pos == null)
        //           ? null
        //           : FaceLandmark(
        //               type: type,
        //               position: Point<int>(pos[0].toInt(), pos[1].toInt()),
        //             );
        //     },
        //   ),
        // ),
        // contours: Map<FaceContourType, FaceContour?>.fromIterables(
        //   FaceContourType.values,
        //   FaceContourType.values.map(
        //     (FaceContourType type) {
        //       /// added empty map to pass the tests
        //       final List<dynamic>? arr =
        //           (json['contours'] ?? <String, dynamic>{})[type.name];
        //       return (arr == null)
        //           ? null
        //           : FaceContour(
        //               type: type,
        //               points: arr
        //                   .map<Point<int>>((dynamic pos) =>
        //                       Point<int>(pos[0].toInt(), pos[1].toInt()))
        //                   .toList(),
        //             );
        //     },
        //   ),
        // ),
      );
}

/// A landmark on a human face detected in an image.
///
/// A landmark is a point on a detected face, such as an eye, nose, or mouth.
class FaceLandmark {
  /// The [FaceLandmarkType] of this landmark.
  final FaceLandmarkType type;

  /// Gets a 2D point for landmark position.
  ///
  /// The point (0, 0) is defined as the upper-left corner of the image.
  final Point<int> position;

  FaceLandmark({required this.type, required this.position});
}

/// A contour on a human face detected in an image.
///
/// Contours of facial features.
class FaceContour {
  /// The [FaceContourType] of this contour.
  final FaceContourType type;

  /// Gets a 2D point [List] for contour positions.
  ///
  /// The point (0, 0) is defined as the upper-left corner of the image.
  final List<Point<int>> points;

  FaceContour({required this.type, required this.points});
}

/// Option for controlling additional trade-offs in performing face detection.
///
/// Accurate tends to detect more faces and may be more precise in determining
/// values such as position, at the cost of speed.
enum FaceDetectorMode {
  accurate,
  fast,
}

/// Available face landmarks detected by [FaceDetector].
enum FaceLandmarkType {
  bottomMouth,
  rightMouth,
  leftMouth,
  rightEye,
  leftEye,
  rightEar,
  leftEar,
  rightCheek,
  leftCheek,
  noseBase,
}

/// Available face contour types detected by [FaceDetector].
enum FaceContourType {
  face,
  leftEyebrowTop,
  leftEyebrowBottom,
  rightEyebrowTop,
  rightEyebrowBottom,
  leftEye,
  rightEye,
  upperLipTop,
  upperLipBottom,
  lowerLipTop,
  lowerLipBottom,
  noseBridge,
  noseBottom,
  leftCheek,
  rightCheek
}
