package id.aaaabima.squatsanalysisapp

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import id.aaaabima.squatsanalysisapp.utils.RequiredCoordinates
import id.aaaabima.squatsanalysisapp.utils.Threshold
import id.aaaabima.squatsanalysisapp.utils.count
import id.aaaabima.squatsanalysisapp.utils.findAngle
import id.aaaabima.squatsanalysisapp.utils.getFeedbackMessage
import id.aaaabima.squatsanalysisapp.utils.getSquatsPoseLandmarks
import id.aaaabima.squatsanalysisapp.utils.toPair
import id.aaaabima.squatsanalysisapp.utils.toZeroPair
import java.lang.Float.max
import kotlin.math.abs

class OverlayView(context: Context?, attrs: AttributeSet) : View(context, attrs) {
  private var results: PoseLandmarkerResult? = null
  private lateinit var activity: Activity
  private var pointPaint = Paint()
  private var linePaint = Paint()

  private var scaleFactor: Float = 1f
  private var imageWidth: Int = 1
  private var imageHeight: Int = 1

  private var stateSequence: ArrayList<String> = arrayListOf()
  private var correctSquat = 0
  private var incorrectSquat = 0
  private var prevState = ""
  private var currentState = ""
  private var incorrectPosture = false

  init {
    initPaints()
  }

  fun clear() {
    results = null
    pointPaint.reset()
    linePaint.reset()
    invalidate()
    initPaints()
  }

  private fun initPaints() {
    linePaint.color = Color.BLUE
    linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
    linePaint.style = Paint.Style.STROKE

    pointPaint.color = Color.YELLOW
    pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
    pointPaint.style = Paint.Style.FILL
  }

  override fun draw(canvas: Canvas) {
    super.draw(canvas)
    results?.let { poseLandmarkerResult ->
      val coordsBuffer: HashMap<Int, NormalizedLandmark> = hashMapOf()
      poseLandmarkerResult.landmarks().map {
        for (i in getSquatsPoseLandmarks())
          coordsBuffer[i] = it[i]
      }

      // Separate every needed coordinate
      val nose = coordsBuffer[RequiredCoordinates.Nose.value]
      val leftShoulder = coordsBuffer[RequiredCoordinates.LeftShoulder.value]
      val rightShoulder = coordsBuffer[RequiredCoordinates.RightShoulder.value]
      val leftHip = coordsBuffer[RequiredCoordinates.LeftHip.value]
      val rightHip = coordsBuffer[RequiredCoordinates.RightHip.value]
      val leftKnee = coordsBuffer[RequiredCoordinates.LeftKnee.value]
      val rightKnee = coordsBuffer[RequiredCoordinates.RightKnee.value]
      val leftAnkle = coordsBuffer[RequiredCoordinates.LeftAnkle.value]
      val rightAnkle = coordsBuffer[RequiredCoordinates.RightAnkle.value]
      val leftFoot = coordsBuffer[RequiredCoordinates.LeftFoot.value]
      val rightFoot = coordsBuffer[RequiredCoordinates.RightFoot.value]

      val offsetAngle = findAngle(
        leftShoulder?.toPair() ?: Pair(0f, 0f),
        rightShoulder?.toPair() ?: Pair(0f, 0f),
        nose?.toPair() ?: Pair(0f, 0f)
      )

      val tvFeedback: TextView = activity.findViewById(R.id.tv_feedback)
      val tvLowerHips: TextView = activity.findViewById(R.id.tv_lower_hips)

      if (offsetAngle > Threshold.OffsetThresh.value[0].toInt()) {
        tvFeedback.isVisible = true
        tvFeedback.text = String.format(
          context.getString(R.string.feedback),
          "Camera not aligned properly"
        )

        for (normalizedLandmark in coordsBuffer.entries) {
          canvas.drawPoint(
            normalizedLandmark.value.x() * imageWidth * scaleFactor,
            normalizedLandmark.value.y() * imageHeight * scaleFactor,
            pointPaint
          )
        }
      } else {
        tvFeedback.isVisible = false

        val distLeftShoulderHip = abs((leftFoot?.y() ?: 0f) - (leftShoulder?.y() ?: 0f))
        val distRightShoulderHip = abs((rightFoot?.y() ?: 0f) - (rightShoulder?.y() ?: 0f))

        val shoulder: NormalizedLandmark?
        val hip: NormalizedLandmark?
        val knee: NormalizedLandmark?
        val ankle: NormalizedLandmark?

        if (distLeftShoulderHip < distRightShoulderHip) {
          shoulder = leftShoulder
          hip = leftHip
          knee = leftKnee
          ankle = leftAnkle
        } else {
          shoulder = rightShoulder
          hip = rightHip
          knee = rightKnee
          ankle = rightAnkle
        }

        // Calculate Vertical Angle
        val hipVerticalAngle = findAngle(
          shoulder?.toPair() ?: Pair(0f, 0f),
          hip?.toZeroPair() ?: Pair(0f, 0f),
          hip?.toPair() ?: Pair(0f, 0f)
        )
        val kneeVerticalAngle = findAngle(
          hip?.toPair() ?: Pair(0f, 0f),
          knee?.toZeroPair() ?: Pair(0f, 0f),
          knee?.toPair() ?: Pair(0f, 0f)
        )
        val ankleVerticalAngle = findAngle(
          knee?.toPair() ?: Pair(0f, 0f),
          ankle?.toZeroPair() ?: Pair(0f, 0f),
          ankle?.toPair() ?: Pair(0f, 0f)
        )

        currentState = getState(kneeVerticalAngle)
        updateStateSequence(currentState)

        // Compute counters

        if (currentState == "s1") {
          if (stateSequence.size == 3 && !incorrectPosture)
            correctSquat++
          else if (stateSequence.contains("s2") && stateSequence.size == 1)
            incorrectSquat++
          else if (incorrectPosture)
            incorrectSquat++

          stateSequence.clear()
          incorrectPosture = false
        }

        // Perform Feedback Actions
        else {
          if (hipVerticalAngle > Threshold.HipThresh.value.last().toInt())
            tvFeedback.text = getFeedbackMessage(0)
          else if (hipVerticalAngle < Threshold.HipThresh.value.first()
              .toInt() && stateSequence.count("s2") == 1
          )
            tvFeedback.text = getFeedbackMessage(1)

          if (Threshold.KneeThresh.value.first()
              .toInt() < kneeVerticalAngle && kneeVerticalAngle < Threshold.KneeThresh.value[1]
              .toInt() && stateSequence.count("s2") == 1
          )
            tvLowerHips.isVisible = true
          else if (kneeVerticalAngle > Threshold.KneeThresh.value.last().toInt()) {
            tvFeedback.text = getFeedbackMessage(3)
            incorrectPosture = true
          }

          if (ankleVerticalAngle > Threshold.AnkleThresh.value.first().toInt()) {
            tvFeedback.text = getFeedbackMessage(2)
            incorrectPosture = true
          }
        }

        // Compute Inactivity
        if (currentState != prevState) {
          if (stateSequence.contains("s3") || currentState == "s1")
            tvLowerHips.isVisible = false
        }

        for (normalizedLandmark in coordsBuffer.entries) {
          canvas.drawPoint(
            normalizedLandmark.value.x() * imageWidth * scaleFactor,
            normalizedLandmark.value.y() * imageHeight * scaleFactor,
            pointPaint
          )
        }

        prevState = currentState
      }

      for (landmark in poseLandmarkerResult.landmarks()) {
        PoseLandmarker.POSE_LANDMARKS.forEach {
          canvas.drawLine(
            poseLandmarkerResult.landmarks()[0][it!!.start()].x() * imageWidth * scaleFactor,
            poseLandmarkerResult.landmarks()[0][it.start()].y() * imageHeight * scaleFactor,
            poseLandmarkerResult.landmarks()[0][it.end()].x() * imageWidth * scaleFactor,
            poseLandmarkerResult.landmarks()[0][it.end()].y() * imageHeight * scaleFactor,
            linePaint
          )
        }
      }

      activity.apply {
        findViewById<TextView>(R.id.tv_correct).text =
          String.format(context.getString(R.string.correct_squats), correctSquat)
        findViewById<TextView>(R.id.tv_incorrect).text =
          String.format(context.getString(R.string.incorrect_squats), incorrectSquat)
        findViewById<TextView>(R.id.tv_state).text =
          String.format(context.getString(R.string.state), currentState)
      }
    }
  }

  fun setResults(
    poseLandmarkerResult: PoseLandmarkerResult,
    imageHeight: Int,
    imageWidth: Int,
    activity: Activity
  ) {
    results = poseLandmarkerResult

    this.imageHeight = imageHeight
    this.imageWidth = imageWidth
    this.activity = activity

    scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    invalidate()
  }

  private fun getState(kneeAngle: Int): String {
    return when (kneeAngle) {
      in Threshold.NormalAngle.value.first().toInt()..Threshold.NormalAngle.value.last()
        .toInt() -> {
        return "s1"
      }

      in Threshold.TransAngle.value.first().toInt()..Threshold.TransAngle.value.last()
        .toInt() -> {
        return "s2"
      }

      in Threshold.PassAngle.value.first().toInt()..Threshold.PassAngle.value.last()
        .toInt() -> {
        return "s3"
      }

      else -> ""
    }
  }

  private fun updateStateSequence(state: String) {
    when (state) {
      "s2" -> {
        if ((!stateSequence.contains("s3") && stateSequence.count("s3") == 0) || (stateSequence.contains(
            "s3"
          ) && stateSequence.count("s2") == 1)
        )
          stateSequence.add(state)
      }

      "s3" -> {
        if (!stateSequence.contains(state) && stateSequence.contains("s2"))
          stateSequence.add(state)
      }
    }
  }

  companion object {
    private const val LANDMARK_STROKE_WIDTH = 12F
    private const val TAG = "OverlayView"
  }
}