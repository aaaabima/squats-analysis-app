package id.aaaabima.squatsanalysisapp

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
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
import id.aaaabima.squatsanalysisapp.utils.getSquatsPoseLandmarks
import id.aaaabima.squatsanalysisapp.utils.toPair
import id.aaaabima.squatsanalysisapp.utils.toZeroPair
import java.lang.Float.max
import kotlin.math.abs

class OverlayView(context: Context?, attrs: AttributeSet) : View(context, attrs) {
    private var results: PoseLandmarkerResult? = null
    private lateinit var activity: Activity
    private var reqCoords = RequiredCoordinates()
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
            process(poseLandmarkerResult)
            val coordsBuffer: HashMap<Int, NormalizedLandmark> = hashMapOf()
            poseLandmarkerResult.landmarks().map {
                for (i in getSquatsPoseLandmarks())
                    coordsBuffer[i] = it[i]
            }

            // Separate every needed coordinate
            val nose = coordsBuffer[reqCoords.nose]
            val leftShoulder = coordsBuffer[reqCoords.leftShoulder]
            val rightShoulder = coordsBuffer[reqCoords.rightShoulder]
            val leftElbow = coordsBuffer[reqCoords.leftElbow]
            val rightElbow = coordsBuffer[reqCoords.rightElbow]
            val leftWrist = coordsBuffer[reqCoords.leftWrist]
            val rightWrist = coordsBuffer[reqCoords.rightWrist]
            val leftHip = coordsBuffer[reqCoords.leftHip]
            val rightHip = coordsBuffer[reqCoords.rightHip]
            val leftKnee = coordsBuffer[reqCoords.leftKnee]
            val rightKnee = coordsBuffer[reqCoords.rightKnee]
            val leftAnkle = coordsBuffer[reqCoords.leftAnkle]
            val rightAnkle = coordsBuffer[reqCoords.rightAnkle]
            val leftFoot = coordsBuffer[reqCoords.leftFoot]
            val rightFoot = coordsBuffer[reqCoords.rightFoot]

            val offsetAngle = findAngle(
                leftShoulder?.toPair() ?: Pair(0f, 0f),
                rightShoulder?.toPair() ?: Pair(0f, 0f),
                nose?.toPair() ?: Pair(0f, 0f)
            )

            val feedback: TextView = activity.findViewById(R.id.tv_feedback)

            if (offsetAngle > Threshold.OffsetThresh.value[0].toInt()) {
                feedback.isVisible = true
                feedback.text = String.format(
                    context.getString(R.string.feedback),
                    "Camera not aligned properly"
                )
//                coordsBuffer.forEach {
//                    if (it.key in listOf(0,11,12))
//                    canvas.drawPoint(
//                        it.value.x() * imageWidth * scaleFactor,
//                        it.value.y() * imageHeight * scaleFactor,
//                        pointPaint
//                    )
//                }

                for (normalizedLandmark in coordsBuffer.entries) {
                    canvas.drawPoint(
                        normalizedLandmark.value.x() * imageWidth * scaleFactor,
                        normalizedLandmark.value.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                Log.d(TAG, "Offset angle: $offsetAngle degree")
            } else {
                feedback.isVisible = false

                val distLeftShoulderHip = abs((leftFoot?.y() ?: 0f) - (leftShoulder?.y()?: 0f))
                val distRightShoulderHip = abs((rightFoot?.y() ?: 0f) - (rightShoulder?.y()?: 0f))

                val shoulder: NormalizedLandmark?
                val elbow: NormalizedLandmark?
                val wrist: NormalizedLandmark?
                val hip: NormalizedLandmark?
                val knee: NormalizedLandmark?
                val ankle: NormalizedLandmark?
                val foot: NormalizedLandmark?
                val multiplier: Int

                if (distLeftShoulderHip < distRightShoulderHip) {
                    shoulder = leftShoulder
                    elbow = leftElbow
                    wrist = leftWrist
                    hip = leftHip
                    knee = leftKnee
                    ankle = leftAnkle
                    foot = leftFoot
                    multiplier = -1
                } else {
                    shoulder = rightShoulder
                    elbow = rightElbow
                    wrist = rightWrist
                    hip = rightHip
                    knee = rightKnee
                    ankle = rightAnkle
                    foot = rightFoot
                    multiplier = 1
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

                // Computer counters

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

    private fun process(landmarks: PoseLandmarkerResult) {

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