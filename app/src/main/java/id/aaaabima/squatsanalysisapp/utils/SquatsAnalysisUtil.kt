package id.aaaabima.squatsanalysisapp.utils

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.sqrt

fun findAngle(
  p1: Pair<Float, Float>,
  p2: Pair<Float, Float>,
  refPt: Pair<Float, Float> = Pair(0.0f, 0.0f)
): Int {
  val p1Ref = Pair(p1.first - refPt.first, p1.second - refPt.second)
  val p2Ref = Pair(p2.first - refPt.first, p2.second - refPt.second)

  val cosTheta =
    (p1Ref.first * p2Ref.first + p1Ref.second * p2Ref.second) / (p1Ref.length() * p2Ref.length())
  val theta = acos(cosTheta.coerceIn(-1.0f, 1.0f))

  return (180.0 / PI * theta).toInt()
}

fun NormalizedLandmark.toPair(): Pair<Float, Float> = Pair(this.x(), this.y())

fun NormalizedLandmark.toZeroPair(): Pair<Float, Float> = Pair(this.x(), 0f)

// Extension function to calculate the length of a Pair (2D vector)
fun Pair<Float, Float>.length(): Float {
  return sqrt(first * first + second * second)
}

/**
 *  The following are landmark poses needed to escalate squats exercise angle.
 *  Visit this website for reference of numbers: https://developers.google.com/static/mediapipe/images/solutions/pose_landmarks_index.png
 *  Nose (0), Left Sides: Shoulder (11), Elbow (13), Wrist (15), Hip (23), Knee (25), Ankle (27), Foot (31)
 *  Right Sides: Shoulder (12), Elbow (14), Wrist (16), Hip (24), Knee (26), Ankle (28), Foot (32)
 */
fun getSquatsPoseLandmarks(): ArrayList<Int> =
  arrayListOf(
    0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28, 31, 32
  )

fun ArrayList<String>.count(state: String): Int {
  var counter = 0
  this.forEach {
    if (it == state)
      counter++
  }
  return counter
}

fun getFeedbackMessage(case: Int): String = when (case) {
  0 -> "Bend Backwards"
  1 -> "Bend Forward"
  2 -> "Knee falling over toe"
  3 -> "Squat too deep"
  else -> ""
}