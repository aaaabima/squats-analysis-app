package id.aaaabima.squats_analysis_app

import androidx.lifecycle.ViewModel
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker.PoseLandmarkerOptions

class MainViewModel : ViewModel() {
    private var _delegate: Int = PoseLandmarkerHelper.DELEGATE_CPU
    private var _minPoseDetectionConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE
    private var _minPosePresenceConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE
    private var _minPoseTrackingConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE

    val currentDelegate: Int get() = _delegate
    val currentMinPoseDetectionConfidence: Float
        get() = _minPoseDetectionConfidence
    val currentMinPosePresenceConfidence: Float
        get() = _minPosePresenceConfidence
    val currentMinPoseTrackingConfidence: Float
        get() = _minPoseTrackingConfidence

    fun setDelegate(delegate: Int) {
        _delegate = delegate
    }

    fun setMinPoseDetectionConfidence(confidence: Float) {
        _minPoseDetectionConfidence = confidence
    }

    fun setMinPoseTrackingConfidence(confidence: Float) {
        _minPoseTrackingConfidence = confidence
    }

    fun setMinPosePresenceConfidence(confidence: Float) {
        _minPosePresenceConfidence = confidence
    }
}