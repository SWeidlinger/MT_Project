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

class Draw(
    overlay: GraphicOverlay,
    val pose: Pose,
    private val poseClassificationArray: ArrayList<String>?,
    private val repCountArray: ArrayList<RepetitionCounter>?,
    var debugMode: Boolean,
    private val resolution: Size,
    private val fps: Long,
    private val thresholdIFL: Double,
    private val actionBarHeight: Int,
    private val modelUsed: Int
) : GraphicOverlay.Graphic(overlay) {
    private var mPaint: Paint = Paint()
    private var mFacePaint: Paint = Paint()
    private var mArmPaint: Paint = Paint()
    private var mChestPaint: Paint = Paint()
    private var mLegPaint: Paint = Paint()
    private var mHandPaint: Paint = Paint()
    private var mFootPaint: Paint = Paint()
    private var mTextPaint: Paint = Paint()
    private var mClassificationPaint: Paint = Paint()
    private var mPointPaint: Paint = Paint()
    private var mRightPaint: Paint = Paint()
    private var mLeftPaint: Paint = Paint()
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

        mFacePaint.color = Color.WHITE
        mFacePaint.strokeWidth = STROKE_WIDTH
        mFacePaint.style = Paint.Style.FILL

        mArmPaint.color = Color.WHITE
        mArmPaint.strokeWidth = STROKE_WIDTH
        mArmPaint.style = Paint.Style.FILL

        mChestPaint.color = Color.WHITE
        mChestPaint.strokeWidth = STROKE_WIDTH
        mChestPaint.style = Paint.Style.FILL

        mLegPaint.color = Color.WHITE
        mLegPaint.strokeWidth = STROKE_WIDTH
        mLegPaint.style = Paint.Style.FILL

        mHandPaint.color = Color.WHITE
        mHandPaint.strokeWidth = STROKE_WIDTH
        mHandPaint.style = Paint.Style.FILL

        mFootPaint.color = Color.WHITE
        mFootPaint.strokeWidth = STROKE_WIDTH
        mFootPaint.style = Paint.Style.FILL

        mFootPaint.color = Color.WHITE
        mFootPaint.strokeWidth = STROKE_WIDTH
        mFootPaint.style = Paint.Style.FILL

        mRightPaint.color = Color.RED
        mRightPaint.strokeWidth = STROKE_WIDTH
        mRightPaint.style = Paint.Style.FILL

        mLeftPaint.color = Color.parseColor("#007EB8")
        mLeftPaint.strokeWidth = STROKE_WIDTH
        mLeftPaint.style = Paint.Style.FILL

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

        if (debugMode) {
            debugText()
            if (!poseClassificationArray.isNullOrEmpty()) {
                val classificationX = POSE_CLASSIFICATION_TEXT_SIZE * 0.5f
                var confidence = "no pose detected"
                if (poseClassificationArray.size > 1) {
                    confidence = poseClassificationArray[1]
                }
                val classificationY =
                    (canvas?.height!!-175) - (POSE_CLASSIFICATION_TEXT_SIZE * 1.5f)
                canvas.drawText(
                    confidence,
                    classificationX,
                    classificationY,
                    mClassificationPaint
                )
            }
            if (!repCountArray.isNullOrEmpty()) {
                var counter = 2
                for (i in repCountArray) {
                    canvas?.drawText(
                        "${i.className.dropLast(5)}:  ${i.numRepeats}",
                        POSE_CLASSIFICATION_TEXT_SIZE * 0.5f,
                        (canvas.height-190) - (POSE_CLASSIFICATION_TEXT_SIZE * 1.5f * (counter++).toFloat()),
                        mClassificationPaint
                    )
                }
            }
        }

        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) {
            return
        }

        initLandmarks(mCanvas)

        for (landmark in landmarks) {
            if (checkPoint(landmark)) {
                drawPoint(mCanvas, landmark, mPointPaint)
            }
        }
    }

    private fun initLandmarks(canvas: Canvas) {
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val leftEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER)
        val leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
        val leftEyeOuter = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER)
        val rightEyeInner = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER)
        val rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)
        val rightEyeOuter = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER)
        val leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR)
        val rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR)
        val leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH)
        val rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH)

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
        val rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
        val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
        val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
        val leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
        val rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)
        val leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)
        val rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)
        val leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)
        val rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)

        // Face
        drawLine(canvas, nose, leftEyeInner, mFacePaint)
        drawLine(canvas, leftEyeInner, leftEye, mFacePaint)
        drawLine(canvas, leftEye, leftEyeOuter, mFacePaint)
//        drawLine(canvas, leftEyeOuter, leftEar, mFacePaint)
        drawLine(canvas, nose, rightEyeInner, mFacePaint)
        drawLine(canvas, rightEyeInner, rightEye, mFacePaint)
        drawLine(canvas, rightEye, rightEyeOuter, mFacePaint)
//        drawLine(canvas, rightEyeOuter, rightEar, mFacePaint)
        drawLine(canvas, leftMouth, rightMouth, mFacePaint)

        // Chest
        drawLine(canvas, leftShoulder, rightShoulder, mChestPaint)
        drawLine(canvas, leftShoulder, leftHip, mChestPaint)
        drawLine(canvas, rightShoulder, rightHip, mChestPaint)

        // Arms
        drawLine(canvas, leftShoulder, leftElbow, mLeftPaint)
        drawLine(canvas, leftElbow, leftWrist, mLeftPaint)
        drawLine(canvas, rightShoulder, rightElbow, mRightPaint)
        drawLine(canvas, rightElbow, rightWrist, mRightPaint)

//        // Hands
//        drawLine(canvas, leftWrist, leftThumb, mHandPaint)
//        drawLine(canvas, leftWrist, leftPinky, mHandPaint)
//        drawLine(canvas, leftWrist, leftIndex, mHandPaint)
//        drawLine(canvas, leftIndex, leftPinky, mHandPaint)
//        drawLine(canvas, rightWrist, rightThumb, mHandPaint)
//        drawLine(canvas, rightWrist, rightPinky, mHandPaint)
//        drawLine(canvas, rightWrist, rightIndex, mHandPaint)
//        drawLine(canvas, rightIndex, rightPinky, mHandPaint)

        // Legs
        drawLine(canvas, leftHip, rightHip, mLegPaint)
        drawLine(canvas, leftHip, leftKnee, mLeftPaint)
        drawLine(canvas, leftKnee, leftAnkle, mLeftPaint)
        drawLine(canvas, rightHip, rightKnee, mRightPaint)
        drawLine(canvas, rightKnee, rightAnkle, mRightPaint)

        // Feet
//        drawLine(canvas, leftAnkle, leftHeel, mFootPaint)
//        drawLine(canvas, leftHeel, leftFootIndex, mFootPaint)
//        drawLine(canvas, rightAnkle, rightHeel, mFootPaint)
//        drawLine(canvas, rightHeel, rightFootIndex, mFootPaint)
    }

    private fun drawPoint(canvas: Canvas, landmark: PoseLandmark, paint: Paint) {
        if (landmark.inFrameLikelihood >= thresholdIFL) {
            val point = landmark.position3D
            canvas.drawCircle(translateX(point.x), translateY(point.y), DOT_RADIUS, paint)
            if (debugMode) {
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
    }

    private fun drawLine(
        canvas: Canvas,
        startLandmark: PoseLandmark?,
        endLandmark: PoseLandmark?,
        paint: Paint
    ) {
        if (startLandmark != null && endLandmark != null && startLandmark.inFrameLikelihood >= thresholdIFL && endLandmark.inFrameLikelihood >= thresholdIFL) {
            val start = startLandmark.position3D
            val end = endLandmark.position3D

            canvas.drawLine(
                translateX(start.x),
                translateY(start.y),
                translateX(end.x),
                translateY(end.y),
                paint
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
        mCanvas.drawText(
            "THRESHOLD IFL: ${(thresholdIFL * 100).toInt()}",
            35f,
            actionBarHeight + 250f,
            mTextPaint
        )

        val model = if (modelUsed == 0) "MLKit Fast" else "MLKit Accurate"

        mCanvas.drawText("MODEL: $model", 35f, actionBarHeight + 300f, mTextPaint)
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