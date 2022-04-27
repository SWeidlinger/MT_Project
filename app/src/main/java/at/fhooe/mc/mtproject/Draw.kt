package at.fhooe.mc.mtproject

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import at.fhooe.mc.mtproject.GraphicOverlay
import at.fhooe.mc.mtproject.GraphicOverlay.Graphic

//TODO scale and translate the drawings to the correct size,
// consider flipped image when front camera is used
class Draw(context: Context?, var pose: Pose, var text: String, val frontCamera: Boolean) : View(context) {
    private var mPaint: Paint = Paint()
    private var mFacePaint: Paint = Paint()
    private var mArmPaint: Paint = Paint()
    private var mChestPaint: Paint = Paint()
    private var mLegPaint: Paint = Paint()
    private var mHandPaint: Paint = Paint()
    private var mFootPaint: Paint = Paint()


    private var zMin = java.lang.Float.MAX_VALUE
    private var zMax = java.lang.Float.MIN_VALUE

    init {
        mPaint.color = Color.WHITE
        mPaint.strokeWidth = 20f
        mPaint.style = Paint.Style.STROKE

        mFacePaint = Paint()
        mFacePaint.color = Color.BLACK
        mFacePaint.strokeWidth = 10f
        mFacePaint.style = Paint.Style.FILL

        mArmPaint = Paint()
        mArmPaint.color = Color.RED
        mArmPaint.strokeWidth = 10f
        mArmPaint.style = Paint.Style.FILL

        mChestPaint = Paint()
        mChestPaint.color = Color.BLUE
        mChestPaint.strokeWidth = 10f
        mChestPaint.style = Paint.Style.FILL

        mLegPaint = Paint()
        mLegPaint.color = Color.YELLOW
        mLegPaint.strokeWidth = 10f
        mLegPaint.style = Paint.Style.FILL

        mHandPaint = Paint()
        mHandPaint.color = Color.GREEN
        mHandPaint.strokeWidth = 10f
        mHandPaint.style = Paint.Style.FILL

        mFootPaint = Paint()
        mFootPaint.color = Color.LTGRAY
        mFootPaint.strokeWidth = 10f
        mFootPaint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val landmarks = pose.allPoseLandmarks
        for (landmark in landmarks) {
            drawPoint(canvas, landmark, mPaint)
            drawLines(canvas)
        }
    }

    private fun drawPoint(canvas: Canvas, landmark: PoseLandmark, paint: Paint) {
        val point = landmark.position3D
        canvas.drawCircle(translateX(point.x), translateY(point.y), DOT_RADIUS, paint)
    }

    private fun drawLines(canvas: Canvas) {
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val leftyEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER)
        val leftyEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
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
        drawLine(canvas, nose, leftyEyeInner, mFacePaint)
        drawLine(canvas, leftyEyeInner, leftyEye, mFacePaint)
        drawLine(canvas, leftyEye, leftEyeOuter, mFacePaint)
        drawLine(canvas, leftEyeOuter, leftEar, mFacePaint)
        drawLine(canvas, nose, rightEyeInner, mFacePaint)
        drawLine(canvas, rightEyeInner, rightEye, mFacePaint)
        drawLine(canvas, rightEye, rightEyeOuter, mFacePaint)
        drawLine(canvas, rightEyeOuter, rightEar, mFacePaint)
        drawLine(canvas, leftMouth, rightMouth, mFacePaint)

        drawLine(canvas, leftShoulder, rightShoulder, mChestPaint)

        drawLine(canvas, leftHip, rightHip, mLegPaint)

        // Left body
        drawLine(canvas, leftShoulder, leftElbow, mArmPaint)
        drawLine(canvas, leftElbow, leftWrist, mArmPaint)
        drawLine(canvas, leftShoulder, leftHip, mChestPaint)
        drawLine(canvas, leftHip, leftKnee, mLegPaint)
        drawLine(canvas, leftKnee, leftAnkle, mLegPaint)
        drawLine(canvas, leftWrist, leftThumb, mHandPaint)
        drawLine(canvas, leftWrist, leftPinky, mHandPaint)
        drawLine(canvas, leftWrist, leftIndex, mHandPaint)
        drawLine(canvas, leftIndex, leftPinky, mHandPaint)
        drawLine(canvas, leftAnkle, leftHeel, mFootPaint)
        drawLine(canvas, leftHeel, leftFootIndex, mFootPaint)

        // Right body
        drawLine(canvas, rightShoulder, rightElbow, mArmPaint)
        drawLine(canvas, rightElbow, rightWrist, mArmPaint)
        drawLine(canvas, rightShoulder, rightHip, mChestPaint)
        drawLine(canvas, rightHip, rightKnee, mLegPaint)
        drawLine(canvas, rightKnee, rightAnkle, mLegPaint)
        drawLine(canvas, rightWrist, rightThumb, mHandPaint)
        drawLine(canvas, rightWrist, rightPinky, mHandPaint)
        drawLine(canvas, rightWrist, rightIndex, mHandPaint)
        drawLine(canvas, rightIndex, rightPinky, mHandPaint)
        drawLine(canvas, rightAnkle, rightHeel, mFootPaint)
        drawLine(canvas, rightHeel, rightFootIndex, mFootPaint)
    }

    private fun drawLine(
        canvas: Canvas,
        startLandmark: PoseLandmark?,
        endLandmark: PoseLandmark?,
        paint: Paint
    ) {
        val start = startLandmark!!.position3D
        val end = endLandmark!!.position3D
        canvas.drawLine(
            translateX(start.x),
            translateY(start.y),
            translateX(end.x),
            translateY(end.y),
            paint
        )
    }

    private fun translateX(x: Float): Float{
//        if (frontCamera) {
//            return context.getWidth() - (scale(x) - overlay.postScaleWidthOffset);
//        } else {
//            return scale(x) - overlay.postScaleWidthOffset;
//        }
        return x + 100
    }

    private fun translateY(y: Float): Float{
//        if (frontCamera) {
//            return context.getWidth() - (scale(x) - overlay.postScaleWidthOffset);
//        } else {
//            return scale(x) - overlay.postScaleWidthOffset;
//        }
        return y + 600
    }

    companion object {
        private const val DOT_RADIUS = 6.0f
        private const val IN_FRAME_LIKELIHOOD_TEXT_SIZE = 30.0f
        private const val STROKE_WIDTH = 10.0f
        private const val POSE_CLASSIFICATION_TEXT_SIZE = 60.0f
    }
}