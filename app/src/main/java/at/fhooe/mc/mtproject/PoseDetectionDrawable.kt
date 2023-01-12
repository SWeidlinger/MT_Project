package at.fhooe.mc.mtproject

import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.camera.core.CameraSelector
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import java.util.*

class PoseDetectionDrawable(
    poseResult: Pose,
    thresholdIFL: Double,
    debugMode: Boolean,
    cameraSelector: CameraSelector
) : Drawable() {
    private val mPose = poseResult
    private val mThresholdIFL = thresholdIFL
    private val mDebugMode = debugMode
    private val mIsFrontCameraUsed = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
    private var mWidth = 0
    private var mHeight = 0
    private var mScaleFactor = 0f
    private var mPostScaleWidthOffset = 0f
    private var mPostScaleHeightOffset = 0f
    private var mTransformationMatrix = Matrix()
    private var mTransformationMatrixUpToDate = false

    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.STROKE
        alpha = 255
    }

    private val pointPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.FILL
        alpha = 255
    }

    private val facePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.FILL
        alpha = 255
    }

    private val armPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.FILL
        alpha = 255
    }

    private val chestPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.FILL
        alpha = 255
    }

    private val legPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.FILL
        alpha = 255
    }

    private val handPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.FILL
        alpha = 255
    }

    private val footPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.FILL
        alpha = 255
    }

    private val rightPaint = Paint().apply {
        color = Color.RED
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.FILL
        alpha = 255
    }

    private val leftPaint = Paint().apply {
        color = Color.parseColor("#007EB8")
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.FILL
        alpha = 255
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = DEBUG_TEXT_WIDTH
        setShadowLayer(10.0f, 0f, 0f, Color.BLACK)
        style = Paint.Style.FILL
        alpha = 255
    }

    private val classificationPaint = Paint().apply {
        color = Color.WHITE
        textSize = POSE_CLASSIFICATION_TEXT_SIZE
        setShadowLayer(10.0f, 0f, 0f, Color.BLACK)
        style = Paint.Style.FILL
        alpha = 255
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        pointPaint.alpha = alpha
        facePaint.alpha = alpha
        armPaint.alpha = alpha
        chestPaint.alpha = alpha
        legPaint.alpha = alpha
        handPaint.alpha = alpha
        footPaint.alpha = alpha
        rightPaint.alpha = alpha
        leftPaint.alpha = alpha
        textPaint.alpha = alpha
        classificationPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        pointPaint.colorFilter = colorFilter
        facePaint.colorFilter = colorFilter
        armPaint.colorFilter = colorFilter
        chestPaint.colorFilter = colorFilter
        legPaint.colorFilter = colorFilter
        handPaint.colorFilter = colorFilter
        footPaint.colorFilter = colorFilter
        rightPaint.colorFilter = colorFilter
        leftPaint.colorFilter = colorFilter
        textPaint.colorFilter = colorFilter
        classificationPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT


    override fun draw(canvas: Canvas) {
        mWidth = canvas.width
        mHeight = canvas.height
        createTransformationMatrix(mTransformationMatrix, 480f, 360f)

        Log.e("postScaleWidthOffset", mPostScaleWidthOffset.toString())
        Log.e("postScaleHeightOffset", mPostScaleHeightOffset.toString())
        Log.e("scaleFactor", mScaleFactor.toString())

        val nose = mPose.getPoseLandmark(PoseLandmark.NOSE)?.position3D
        val leftEye = mPose.getPoseLandmark(PoseLandmark.LEFT_EYE)?.position3D
        val rightEye = mPose.getPoseLandmark(PoseLandmark.RIGHT_EYE)?.position3D



        Log.e("leftEye", leftEye.toString())
        Log.e("rightEye", rightEye.toString())
//
        Log.e("leftEyeXTranslated", translateX(leftEye!!.x).toString())
        Log.e("leftEyeYTranslated", translateY(leftEye.y).toString())

        canvas.drawLine(
            translateX(leftEye!!.x),
            translateY(leftEye.y),
            translateX(rightEye!!.x),
            translateY(rightEye.y),
            rightPaint
        )

        val landmarks = mPose.allPoseLandmarks
        if (landmarks.isEmpty()) {
            return
        }

        initLandmarks(canvas)
//
//        for (landmark in landmarks) {
//            if (checkPoint(landmark)) {
//                drawPoint(canvas, landmark, pointPaint)
//            }
//        }
    }

    private fun initLandmarks(canvas: Canvas) {
        val nose = mPose.getPoseLandmark(PoseLandmark.NOSE)
        val leftEyeInner = mPose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER)
        val leftEye = mPose.getPoseLandmark(PoseLandmark.LEFT_EYE)
        val leftEyeOuter = mPose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER)
        val rightEyeInner = mPose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER)
        val rightEye = mPose.getPoseLandmark(PoseLandmark.RIGHT_EYE)
        val rightEyeOuter = mPose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER)
        val leftEar = mPose.getPoseLandmark(PoseLandmark.LEFT_EAR)
        val rightEar = mPose.getPoseLandmark(PoseLandmark.RIGHT_EAR)
        val leftMouth = mPose.getPoseLandmark(PoseLandmark.LEFT_MOUTH)
        val rightMouth = mPose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH)

        val leftShoulder = mPose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = mPose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = mPose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = mPose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftWrist = mPose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = mPose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftHip = mPose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = mPose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = mPose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = mPose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = mPose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = mPose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftPinky = mPose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
        val rightPinky = mPose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
        val leftIndex = mPose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
        val rightIndex = mPose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
        val leftThumb = mPose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
        val rightThumb = mPose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)
        val leftHeel = mPose.getPoseLandmark(PoseLandmark.LEFT_HEEL)
        val rightHeel = mPose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)
        val leftFootIndex = mPose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)
        val rightFootIndex = mPose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)

        // Face
        drawLine(canvas, nose, leftEyeInner, facePaint)
        drawLine(canvas, leftEyeInner, leftEye, facePaint)
        drawLine(canvas, leftEye, leftEyeOuter, facePaint)
//        drawLine(canvas, leftEyeOuter, leftEar, mFacePaint)
        drawLine(canvas, nose, rightEyeInner, facePaint)
        drawLine(canvas, rightEyeInner, rightEye, facePaint)
        drawLine(canvas, rightEye, rightEyeOuter, facePaint)
//        drawLine(canvas, rightEyeOuter, rightEar, mFacePaint)
        drawLine(canvas, leftMouth, rightMouth, facePaint)

        // Chest
        drawLine(canvas, leftShoulder, rightShoulder, chestPaint)
        drawLine(canvas, leftShoulder, leftHip, chestPaint)
        drawLine(canvas, rightShoulder, rightHip, chestPaint)

        // Arms
        drawLine(canvas, leftShoulder, leftElbow, leftPaint)
        drawLine(canvas, leftElbow, leftWrist, leftPaint)
        drawLine(canvas, rightShoulder, rightElbow, rightPaint)
        drawLine(canvas, rightElbow, rightWrist, rightPaint)

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
        drawLine(canvas, leftHip, rightHip, legPaint)
        drawLine(canvas, leftHip, leftKnee, leftPaint)
        drawLine(canvas, leftKnee, leftAnkle, leftPaint)
        drawLine(canvas, rightHip, rightKnee, rightPaint)
        drawLine(canvas, rightKnee, rightAnkle, rightPaint)

        // Feet
//        drawLine(canvas, leftAnkle, leftHeel, mFootPaint)
//        drawLine(canvas, leftHeel, leftFootIndex, mFootPaint)
//        drawLine(canvas, rightAnkle, rightHeel, mFootPaint)
//        drawLine(canvas, rightHeel, rightFootIndex, mFootPaint)
    }


    private fun drawPoint(canvas: Canvas, landmark: PoseLandmark, paint: Paint) {
        if (landmark.inFrameLikelihood >= mThresholdIFL) {
            val point = landmark.position3D
            canvas.drawCircle(point.x, point.y, PoseDetectionDrawable.DOT_RADIUS, paint)
            if (mDebugMode) {
                val paintDebug = Paint()
                paintDebug.color = Color.GREEN
                paintDebug.textSize = PoseDetectionDrawable.IN_FRAME_LIKELIHOOD_TEXT_SIZE

                canvas.drawText(
                    String.format(Locale.US, "%.2f", landmark.inFrameLikelihood),
                    landmark.position.x + 5,
                    landmark.position.y + 20,
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
        if (startLandmark != null && endLandmark != null && startLandmark.inFrameLikelihood >= mThresholdIFL && endLandmark.inFrameLikelihood >= mThresholdIFL) {
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

//    private fun debugText() {
//        mCanvas.drawText(
//            "RES: ${resolution.height} x ${resolution.width}",
//            35f,
//            actionBarHeight + 150f,
//            mTextPaint
//        )
//
//        mCanvas.drawText("FPS: ${1000 / fps}", 35f, actionBarHeight + 200f, mTextPaint)
//        mCanvas.drawText(
//            "THRESHOLD IFL: ${(thresholdIFL * 100).toInt()}",
//            35f,
//            actionBarHeight + 250f,
//            mTextPaint
//        )
//
//        val model = if (modelUsed == 0) "MLKit Fast" else "MLKit Accurate"
//
//        mCanvas.drawText("MODEL: $model", 35f, actionBarHeight + 300f, mTextPaint)
//    }

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


    private fun createTransformationMatrix(
        matrix: Matrix,
        analyzerWidth: Float,
        analyzerHeight: Float
    ) {
        if (mTransformationMatrixUpToDate) {
            return
        }

        val analyzerAspectRatio = analyzerWidth / analyzerHeight
        val canvasAspectRatio = mWidth.toFloat() / mHeight.toFloat()
        mPostScaleWidthOffset = 0f
        mPostScaleHeightOffset = 0f

        if (analyzerAspectRatio > canvasAspectRatio) {
            mScaleFactor = analyzerWidth / mWidth.toFloat()
            mPostScaleHeightOffset =
                (analyzerWidth / canvasAspectRatio - analyzerHeight) / 2f
        } else {
            mScaleFactor = analyzerHeight / mHeight.toFloat()
            mPostScaleWidthOffset =
                (analyzerHeight * canvasAspectRatio - analyzerWidth) / 2f
        }

        mTransformationMatrix.reset()
        mTransformationMatrix.setScale(mScaleFactor, mScaleFactor)
        mTransformationMatrix.postTranslate(-mPostScaleWidthOffset, -mPostScaleHeightOffset)

        if (mIsFrontCameraUsed) {
            mTransformationMatrix.postScale(-1f, 1f, analyzerWidth / 2f, analyzerHeight / 2f)
        }

        mTransformationMatrixUpToDate = true
    }


    private fun translateX(x: Float): Float {
        return if (mIsFrontCameraUsed) {
            mWidth - ((mScaleFactor * x) - mPostScaleWidthOffset)
        } else {
            (mScaleFactor * x) - mPostScaleWidthOffset
        }
    }

    /**
     * Adjusts the y coordinate from the image's coordinate system to the view coordinate system.
     */
    private fun translateY(y: Float): Float {
        return (mScaleFactor * y) + mPostScaleHeightOffset
    }

    private companion object {
        const val DOT_RADIUS = 7.0f
        const val IN_FRAME_LIKELIHOOD_TEXT_SIZE = 23.0f
        const val STROKE_WIDTH = 6.0f
        const val DEBUG_TEXT_WIDTH = 45.0f
        const val POSE_CLASSIFICATION_TEXT_SIZE = 60.0f
    }
}