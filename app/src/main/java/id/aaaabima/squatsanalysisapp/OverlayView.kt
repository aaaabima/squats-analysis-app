package id.aaaabima.squatsanalysisapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import id.aaaabima.squatsanalysisapp.utils.getSquatsPoseLandmarks
import java.lang.Float.max

class OverlayView(context: Context?, attrs: AttributeSet) : View(context, attrs) {
    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

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
        linePaint.color =
            ContextCompat.getColor(context!!, androidx.appcompat.R.color.material_blue_grey_800)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->
            val needed: ArrayList<NormalizedLandmark> = arrayListOf()
            poseLandmarkerResult.landmarks().map {
                for (i in getSquatsPoseLandmarks())
                    needed.add(it[i])
            }
            Log.d(TAG, "draw: $needed")
            for (landmark in poseLandmarkerResult.landmarks()) {
                for (normalizedLandmark in needed) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

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
        }
    }

    fun setResults(
        poseLandmarkerResult: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = poseLandmarkerResult

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        invalidate()
    }

    private fun getState(kneeAngle: Int): String {
        return when (kneeAngle) {
            in Threshold.values()[0].normalAngle.first() .. Threshold.values()[0].normalAngle.last() -> {
                return "s1"
            }
            in Threshold.values()[0].transAngle.first() .. Threshold.values()[0].transAngle.last() -> {
                return "s2"
            }
            in Threshold.values()[0].passAngle.first() .. Threshold.values()[0].passAngle.last() -> {
                return "s3"
            }
            else -> ""
        }
    }

    private fun updateStateSequence(state: String) {
        when(state) {
            "s2" -> {
                if ((!stateSequence.contains("s3") && stateSequence.count("s3") == 0) || (stateSequence.contains("s3") && stateSequence.count("s2") == 1))
                    stateSequence.add(state)
            }
            "s3" -> {
                if (!stateSequence.contains(state) && stateSequence.contains("s2"))
                    stateSequence.add(state)
            }
        }
    }

    private fun process() {

    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
        private const val TAG = "OverlayView"
    }
}