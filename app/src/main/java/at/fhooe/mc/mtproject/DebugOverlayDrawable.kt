package at.fhooe.mc.mtproject

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Size
import at.fhooe.mc.mtproject.helpers.GraphicOverlay
import at.fhooe.mc.mtproject.helpers.pose.RepetitionCounter
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import java.util.*

class DebugOverlayDrawable(
    overlay: GraphicOverlay,
    val pose: Pose,
    private val poseClassificationArray: ArrayList<String>?,
    private val repCountArray: ArrayList<RepetitionCounter>?,
    private val resolution: Size,
    private val fps: Long,
    private val thresholdIFL: Double,
    private val actionBarHeight: Int,
    private val modelUsed: Int,
    private val syncPreviewAndOverlay: Boolean
) : GraphicOverlay.Graphic(overlay) {
    private var mPaint: Paint = Paint()
    private var mTextPaint: Paint = Paint()
    private var mClassificationPaint: Paint = Paint()
    private var mPointPaint: Paint = Paint()
    private lateinit var mCanvas: Canvas

    private var zMin = java.lang.Float.MAX_VALUE
    private var zMax = java.lang.Float.MIN_VALUE

    init {
        mPaint.color = Color.WHITE
        mPaint.strokeWidth = STROKE_WIDTH
        mPaint.style = Paint.Style.STROKE

        mPointPaint.color = Color.WHITE
        mPointPaint.strokeWidth = STROKE_WIDTH
        mPointPaint.style = Paint.Style.FILL

        mTextPaint.color = Color.WHITE
        mTextPaint.textSize = DEBUG_TEXT_WIDTH
        mTextPaint.setShadowLayer(10.0f, 0f, 0f, Color.BLACK)
        mTextPaint.style = Paint.Style.FILL

        mClassificationPaint.color = Color.WHITE
        mClassificationPaint.textSize = POSE_CLASSIFICATION_TEXT_SIZE
        mClassificationPaint.setShadowLayer(10.0f, 0f, 0f, Color.BLACK)
        mClassificationPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas?) {
        if (canvas != null) {
            mCanvas = canvas
        }

        debugText()
        if (!poseClassificationArray.isNullOrEmpty()) {
            val classificationX = POSE_CLASSIFICATION_TEXT_SIZE * 0.5f
            var confidence = "no pose detected"
            if (poseClassificationArray.size > 1) {
                confidence = poseClassificationArray[1]
            }
            val classificationY =
                (canvas?.height!! - 175) - (POSE_CLASSIFICATION_TEXT_SIZE * 1.5f)
            canvas.drawText(
                confidence,
                classificationX,
                classificationY,
                mClassificationPaint
            )

            if (!repCountArray.isNullOrEmpty()) {
                var counter = 2
                for (i in repCountArray) {
                    canvas.drawText(
                        "${i.className.dropLast(5)}:  ${i.numRepeats}",
                        POSE_CLASSIFICATION_TEXT_SIZE * 0.5f,
                        (canvas.height - 190) - (POSE_CLASSIFICATION_TEXT_SIZE * 1.5f * (counter++).toFloat()),
                        mClassificationPaint
                    )
                }
            }
        }

        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) {
            return
        }

        for (landmark in landmarks) {
            if (checkPoint(landmark)) {
                drawPoint(mCanvas, landmark, mPointPaint)
            }
        }
    }

    private fun drawPoint(canvas: Canvas, landmark: PoseLandmark, paint: Paint) {
        if (landmark.inFrameLikelihood >= thresholdIFL) {
            val paintDebug = Paint()
            paintDebug.color = Color.GREEN
            paintDebug.textSize = IN_FRAME_LIKELIHOOD_TEXT_SIZE

            canvas.drawText(
                String.format(Locale.US, "%.2f", landmark.inFrameLikelihood),
                translateX(landmark.position.x + 5),
                translateY(landmark.position.y + 20),
                paintDebug
            )
        }
    }

    private fun debugText() {
        mCanvas.drawText(
            "RES: ${resolution.height} x ${resolution.width}",
            35f,
            actionBarHeight + 150f,
            mTextPaint
        )

        mCanvas.drawText("FPS: ${1000 / fps}", 35f, actionBarHeight + 200f, mTextPaint)

        mCanvas.drawText("SYNC: $syncPreviewAndOverlay", 35f, actionBarHeight + 250f, mTextPaint)

        mCanvas.drawText(
            "THRESHOLD IFL: ${(thresholdIFL * 100).toInt()}",
            35f,
            actionBarHeight + 300f,
            mTextPaint
        )

        val model = if (modelUsed == 0) "MLKit Fast" else "MLKit Accurate"

        mCanvas.drawText("MODEL: $model", 35f, actionBarHeight + 350f, mTextPaint)
    }

    private fun checkPoint(point: PoseLandmark): Boolean {
        when (point.landmarkType) {
            PoseLandmark.LEFT_EAR -> return false
            PoseLandmark.RIGHT_EAR -> return false
            PoseLandmark.LEFT_PINKY -> return false
            PoseLandmark.RIGHT_PINKY -> return false
            PoseLandmark.LEFT_INDEX -> return false
            PoseLandmark.RIGHT_INDEX -> return false
            PoseLandmark.LEFT_THUMB -> return false
            PoseLandmark.RIGHT_THUMB -> return false
            PoseLandmark.LEFT_HEEL -> return false
            PoseLandmark.RIGHT_HEEL -> return false
            PoseLandmark.LEFT_FOOT_INDEX -> return false
            PoseLandmark.RIGHT_FOOT_INDEX -> return false
        }
        return true
    }

    private companion object {
        const val DOT_RADIUS = 7.0f
        const val IN_FRAME_LIKELIHOOD_TEXT_SIZE = 23.0f
        const val STROKE_WIDTH = 6.0f
        const val DEBUG_TEXT_WIDTH = 45.0f
        const val POSE_CLASSIFICATION_TEXT_SIZE = 60.0f
    }
}