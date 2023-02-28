package at.fhooe.mc.mtproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import at.fhooe.mc.mtproject.bottomSheet.BottomSheetFragmentSession
import at.fhooe.mc.mtproject.databinding.ActivityMainBinding
import at.fhooe.mc.mtproject.databinding.BottomSheetSessionSettingsBinding
import at.fhooe.mc.mtproject.databinding.BottomSheetSessionSettingsSelectionBinding
import at.fhooe.mc.mtproject.helpers.BitmapUtils
import at.fhooe.mc.mtproject.helpers.CameraImageGraphic
import at.fhooe.mc.mtproject.helpers.GraphicOverlay
import at.fhooe.mc.mtproject.helpers.pose.RepetitionCounter
import at.fhooe.mc.mtproject.sessionDialog.SessionSettingsAdapter
import at.fhooe.mc.mtproject.speechRecognition.PorcupineService
import at.fhooe.mc.mtproject.speechRecognition.PorcupineService.LocalBinder
import at.fhooe.mc.mtproject.speechRecognition.ServiceCallbacks
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.timerTask

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), ServiceCallbacks {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mCameraExecutor: ExecutorService
    private lateinit var mCameraProvider: ProcessCameraProvider
    private var mImageResolution: Size = Size(480, 640)
    private lateinit var mImageAnalyzer: ImageAnalysis
    private lateinit var mPreview: Preview
    private lateinit var mPreviewView: PreviewView
    private lateinit var mGraphicOverlay: GraphicOverlay
    private var mPrevTime: Long = 0
    private var mCurrentTime: Long = 0
    private val mFpsUpdateTimer = Timer()
    private var mFps: Long = 0
    private var mThresholdIFL: Int = 50

    //pose detection
    private lateinit var mPoseDetector: PoseDetector
    private lateinit var mPoseClassification: PoseClassification

    private var mSessionActive = false
    private var mWorkoutStartTime: Long = 0

    // Select back camera as default
    private var mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var mDebugMode = false
    private var mDetailedRepInfo = true
    private lateinit var mResultLauncher: ActivityResultLauncher<Intent>
    private var mModel = "MLKit Normal"
    private var mSyncPreviewAndOverlay = false

    private var mSpinnerResolutionID: Int = 1
    private var mSpinnerModelID: Int = 0

    private var mCountDownTimerSeconds: Int = 3
    private var mCountDownTimer: CountDownTimer? = null

    private var mService: PorcupineService? = null

    private var mBottomSheetVisible = false

    private lateinit var mMediaPlayerCountdownStart: MediaPlayer
    private lateinit var mMediaPlayerSessionFinished: MediaPlayer
    private lateinit var mMediaPlayerSessionStart: MediaPlayer
    private lateinit var mMediaPlayerRepCount: MediaPlayer

    private lateinit var mDataSingleton: DataSingleton

    private var mSessionExercise = "All"
    private var mSessionMode = "Endless"
    private var mSessionCount = 0
    private lateinit var mSessionTimeCountdown: CountDownTimer

    private var mCameraSettingSelection = 0

    private var mCameraBitmapList = arrayListOf<ExerciseBitmap>()
    private var mOverlayBitmapList = arrayListOf<ExerciseBitmap>()

    private var mSquatReps = 0
    private var mPushUpReps = 0
    private var mSitUpReps = 0
    private var mRepStartTime = 0L
    private var mRepDurationMovementDownStart = 0L
    private var mRepDurationMovementDownStop = 0L

    private var mMaxFramesRep = 50
    private var mCurrentFrameCount = 0
    private var mSaveActiveMovement = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mDataSingleton = DataSingleton.getInstance(this)
        getSettings()
        getSessionSettings()

        mFpsUpdateTimer.scheduleAtFixedRate(
            timerTask {
                mFps = mCurrentTime - mPrevTime
            }, 0, 500
        )
        mPrevTime = SystemClock.elapsedRealtime()

        mGraphicOverlay = binding.activityMainGraphicOverlay
        mPreviewView = binding.activityMainPreviewView

        //switch to front camera if saved in settings
        if (mCameraSettingSelection == 1) {
            mCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
        mCameraExecutor = Executors.newSingleThreadExecutor()

        mMediaPlayerCountdownStart = MediaPlayer.create(this, R.raw.countdown_beep)
        mMediaPlayerSessionStart = MediaPlayer.create(this, R.raw.session_start)
        mMediaPlayerSessionFinished = MediaPlayer.create(this, R.raw.session_finished)
        mMediaPlayerRepCount = MediaPlayer.create(this, R.raw.rep_count)

        if (mSpinnerModelID == 0) {
            initPoseDetectionFast()
        } else {
            initPoseDetectionAccurate()
        }

        mPoseClassification = PoseClassification(
            this,
            mDataSingleton,
            mMediaPlayerCountdownStart,
            mMediaPlayerRepCount
        )

        //request camera permissions
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        mResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    getSettings()
                }
            }

        binding.activityMainCardViewLeft.setOnClickListener {
            showBottomSheetSessionSettings()
        }
        binding.activityMainCardViewRight.setOnClickListener {
            showBottomSheetSessionSettings()
        }
        binding.activityMainButtonStartSession.setOnClickListener {
            performSessionAction()
        }
        binding.activityMainPreviewView.setOnTouchListener(configureDoubleTap())
    }

    override fun onResume() {
        super.onResume()
        if (!allPermissionsGranted()) {
            return
        }
        initImageAnalyzer()
        startCamera()
        startService()
    }

    override fun onPause() {
        super.onPause()
        stopService()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraExecutor.shutdown()
        mFpsUpdateTimer.cancel()
        stopService()
        mMediaPlayerCountdownStart.release()
        mMediaPlayerSessionStart.release()
        mMediaPlayerSessionFinished.release()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initImageAnalyzer()
                startCamera()
                startService()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun getSettings() {
        mDebugMode = mDataSingleton.getSetting(DataConstants.DEBUG_MODE) as Boolean
        mSpinnerResolutionID = mDataSingleton.getSetting(DataConstants.RESOLUTION) as Int
        mThresholdIFL = mDataSingleton.getSetting(DataConstants.THRESHOLD_IFL) as Int
        mCountDownTimerSeconds =
            mDataSingleton.getSetting(DataConstants.COUNTDOWN_TIMER) as Int
        mSyncPreviewAndOverlay =
            mDataSingleton.getSetting(DataConstants.SYNC_PREVIEW_AND_OVERLAY) as Boolean
        mDetailedRepInfo = mDataSingleton.getSetting(DataConstants.DETAILED_REP_INFO) as Boolean
        mMaxFramesRep = mDataSingleton.getSetting(DataConstants.MAX_FRAMES_REP) as Int
        mSaveActiveMovement =
            mDataSingleton.getSetting(DataConstants.SAVE_ACTIVE_MOVEMENT) as Boolean

        val spinnerModePrev = mSpinnerModelID
        mSpinnerModelID = mDataSingleton.getSetting(DataConstants.MODEL) as Int

        if (spinnerModePrev != mSpinnerModelID) {
            if (mSpinnerModelID == 0) {
                initPoseDetectionFast()
            } else {
                initPoseDetectionAccurate()
            }
        }

        when (mSpinnerResolutionID) {
            0 -> mImageResolution = Size(240, 320)
            1 -> mImageResolution = Size(480, 640)
            2 -> mImageResolution = Size(720, 1280)
            3 -> mImageResolution = Size(1080, 1920)
        }
        when (mSpinnerModelID) {
            0 -> mModel = "MLKit Normal"
            1 -> mModel = "MLKit Accurate"
        }

        mCameraSettingSelection =
            mDataSingleton.getSetting(DataConstants.CAMERA_SELECTION) as Int

        bindCameraUseCases()
    }

    //sets up the double tap to switch cameras
    private fun configureDoubleTap(): View.OnTouchListener {
        return object : View.OnTouchListener {
            private val gestureDetector = GestureDetector(this@MainActivity,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        mGraphicOverlay.clear()
                        //vibration for when the camera changes
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            binding.root.rootView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        }
                        switchCameraInput()
                        return super.onDoubleTap(e)
                    }
                })

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(p0: View?, p1: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(p1)
                return true
            }
        }
    }

    private fun initPoseDetectionFast() {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        mPoseDetector = PoseDetection.getClient(options)
    }

    private fun initPoseDetectionAccurate() {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        mPoseDetector = PoseDetection.getClient(options)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun initImageAnalyzer() {
        mImageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(mImageResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        mImageAnalyzer.setAnalyzer(mCameraExecutor) { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = imageProxy.image ?: return@setAnalyzer
            val inputImage = InputImage.fromMediaImage(image, rotationDegrees)

            //might be needed inside the pose detector on success listener since
            //if it is not inside this might lead to problems when switching the camera
            //while a session is active
            mGraphicOverlay.setImageSourceInfo(
                imageProxy.height,
                imageProxy.width,
                mCameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
            )

            mPoseDetector
                .process(inputImage)
                .addOnFailureListener {
                    imageProxy.close()
                }.addOnSuccessListener { pose ->
                    mPrevTime = mCurrentTime
                    mCurrentTime = SystemClock.elapsedRealtime()

                    var poseClassification: ArrayList<String>? = null
                    var repCounter: ArrayList<RepetitionCounter>? = null
                    if (mSessionActive) {
                        poseClassification = mPoseClassification.getPoseResult(pose)
                        repCounter = mPoseClassification.getRepetitionCounterFull()
                    } else {
                        mPoseClassification.clearRepetitions()
                    }

                    //check the repCounter if any repCount increased and then
                    //add the DetailedRepData object to the corresponding list in the singleton
                    if (!repCounter.isNullOrEmpty()) {
                        val detailedRep = DetailedRepData(
                            cameraBitmapList = mCameraBitmapList,
                            overlayBitmapList = mOverlayBitmapList,
                            duration = SystemClock.elapsedRealtime() - mRepStartTime,
                            durationMovementDown = mRepDurationMovementDownStop - mRepDurationMovementDownStart
                        )

                        repCounter.forEach {
                            when (it.className) {
                                PoseClassification.SQUATS_CLASS -> {
                                    if (mSquatReps < it.numRepeats) {
                                        mDataSingleton.mRepListSquats.add(detailedRep)
                                        mSquatReps = it.numRepeats
                                        resetRepDetailedData()
                                    }
                                }
                                PoseClassification.PUSHUPS_CLASS -> {
                                    if (mPushUpReps < it.numRepeats) {
                                        mDataSingleton.mRepListPushUps.add(detailedRep)
                                        mPushUpReps = it.numRepeats
                                        resetRepDetailedData()
                                    }
                                }
                                PoseClassification.SITUPS_CLASS -> {
                                    if (mSitUpReps < it.numRepeats) {
                                        mDataSingleton.mRepListSitUps.add(detailedRep)
                                        mSitUpReps = it.numRepeats
                                        resetRepDetailedData()
                                    }
                                }
                            }
                        }
                    }

                    //add to cardView when rep mode activated
                    if (mSessionMode == "Rep" && repCounter != null) {
                        var currentRepCount = 0
                        for (individualExercise in repCounter) {
                            currentRepCount += individualExercise.numRepeats
                        }
                        val repsLeft = mSessionCount - currentRepCount
                        if (repsLeft == 0) {
                            performSessionAction()
                            binding.activityMainTextviewSessionCount.text =
                                mSessionCount.toString()
                        } else {
                            binding.activityMainTextviewSessionCount.text = repsLeft.toString()
                        }
                    }

                    //increase count of right cardView in endless mode for every recorded exercise
                    if (mSessionMode == "Endless") {
                        var allReps = 0
                        repCounter?.forEach {
                            allReps += it.numRepeats
                        }
                        binding.activityMainTextviewSessionCount.text = allReps.toString()
                    }

                    mGraphicOverlay.clear()

                    val cameraPreviewBitmap = BitmapUtils.getBitmap(imageProxy)

                    mGraphicOverlay.add(CameraImageGraphic(mGraphicOverlay, cameraPreviewBitmap))

                    if (mSessionActive && !poseClassification.isNullOrEmpty() && poseClassification.size > 2) {
                        //start the timer for the repetition
                        if (mRepStartTime == 0L) {
                            mRepStartTime = SystemClock.elapsedRealtime()
                        }
                        val exerciseClass = poseClassification[2]

                        //set the repActiveMovementTimer only when the image is classified in down
                        //stop once the exercise is categorized as up again
                        if (exerciseClass.contains("down", ignoreCase = true)) {
                            if (mRepDurationMovementDownStart == 0L) {
                                mRepDurationMovementDownStart = SystemClock.elapsedRealtime()
                            } else {
                                mRepDurationMovementDownStop = SystemClock.elapsedRealtime()
                            }
                        }

                        val exerciseBitmap =
                            ExerciseBitmap(exerciseClass, mGraphicOverlay.drawToBitmap())

                        //future me sorry for that, basically what it does it only adds the bitmap if the DetailedRep is active
                        //also only adds the certain classes that are defined as active if the saveActiveMovement switch is active
                        if (mDetailedRepInfo) {
                            if (mCameraBitmapList.size >= mMaxFramesRep) {
                                if (mCurrentFrameCount >= mCameraBitmapList.size) {
                                    mCurrentFrameCount = 0
                                }
                                if (mSaveActiveMovement) {
                                    if (exerciseClass == "squats_down" || exerciseClass == "pushups_down" || exerciseClass == "situps_up") {
                                        mCameraBitmapList[mCurrentFrameCount] = exerciseBitmap
                                    }
                                } else {
                                    mCameraBitmapList[mCurrentFrameCount] = exerciseBitmap
                                }
                            } else {
                                if (mSaveActiveMovement) {
                                    if (exerciseClass == "squats_down" || exerciseClass == "pushups_down" || exerciseClass == "situps_up") {
                                        mCameraBitmapList.add(exerciseBitmap)

                                    }
                                } else {
                                    mCameraBitmapList.add(exerciseBitmap)
                                }
                            }
                        }
                    }

                    val poseDetectionDrawable = PoseDetectionDrawable(
                        mGraphicOverlay,
                        pose,
                        mThresholdIFL / 100.0
                    )

                    mGraphicOverlay.add(poseDetectionDrawable)

                    //only save the bitmap if a session is running and a pose is detected
                    if (mSessionActive && !poseClassification.isNullOrEmpty() && poseClassification.size > 2) {
                        val exerciseClass = poseClassification[2]
                        if (mDetailedRepInfo) {
                            val exerciseBitmap = ExerciseBitmap(
                                exerciseClass,
                                mGraphicOverlay.drawToBitmap()
                            )

                            //only add new frames if its under the threshold
                            //otherwise replace the old ones with new ones
                            if (mOverlayBitmapList.size >= mMaxFramesRep) {
                                if (mCurrentFrameCount >= mOverlayBitmapList.size) {
                                    mCurrentFrameCount = 0
                                }
                                if (mSaveActiveMovement) {
                                    if (exerciseClass == "squats_down" || exerciseClass == "pushups_down" || exerciseClass == "situps_up") {
                                        mOverlayBitmapList[mCurrentFrameCount++] = exerciseBitmap
                                    }
                                } else {
                                    mOverlayBitmapList[mCurrentFrameCount++] = exerciseBitmap
                                }
                            } else {
                                //only add bitmap if the class is the correct one if saveActiveMovement is active
                                if (mSaveActiveMovement) {
                                    if (exerciseClass == "squats_down" || exerciseClass == "pushups_down" || exerciseClass == "situps_up") {
                                        mOverlayBitmapList.add(exerciseBitmap)
                                    }
                                } else {
                                    mOverlayBitmapList.add(exerciseBitmap)
                                }
                            }
                        }
                    }

                    //clear the overlay if not synced so it does not show cameraBitmap
                    if (!mSyncPreviewAndOverlay) {
                        mGraphicOverlay.clear()
                        mGraphicOverlay.add(poseDetectionDrawable)
                    }

                    if (mDebugMode) {
                        val debugOverlay = DebugOverlayDrawable(
                            mGraphicOverlay,
                            pose,
                            poseClassification,
                            repCounter,
                            mImageResolution,
                            mFps,
                            mThresholdIFL / 100.0,
                            supportActionBar!!.height,
                            mSpinnerModelID,
                            mSyncPreviewAndOverlay,
                            mDetailedRepInfo,
                            mMaxFramesRep,
                            mSaveActiveMovement
                        )
                        mGraphicOverlay.add(debugOverlay)
                    }

                    imageProxy.close()
                }
        }
    }

    private fun resetRepDetailedData() {
        mCameraBitmapList = arrayListOf()
        mOverlayBitmapList = arrayListOf()
        mRepStartTime = 0L
        mCurrentFrameCount = 0
        mRepDurationMovementDownStop = 0L
        mRepDurationMovementDownStart = 0L
    }

    private fun switchCameraInput() {
        mGraphicOverlay.clear()
        if (mCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            mCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            mDataSingleton.setSetting(DataConstants.CAMERA_SELECTION, 1)
        } else {
            mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            mDataSingleton.setSetting(DataConstants.CAMERA_SELECTION, 0)
        }
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            mCameraProvider = cameraProviderFuture.get()
            // Preview
            mPreview = Preview.Builder()
                .setTargetResolution(mImageResolution)
                .build()

            mPreview.setSurfaceProvider(binding.activityMainPreviewView.surfaceProvider)

            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        try {
            // Unbind use cases before rebinding
            mCameraProvider.unbindAll()
            // Bind use cases to camera
            if (mSyncPreviewAndOverlay) {
                mCameraProvider.bindToLifecycle(this, mCameraSelector, mImageAnalyzer)
            } else {
                mCameraProvider.bindToLifecycle(this, mCameraSelector, mPreview, mImageAnalyzer)
            }
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun performSessionAction() {
        if (mSessionActive) {
            showSessionEndSheet()
            binding.activityMainCardViewLeft.isClickable = true
            binding.activityMainCardViewRight.isClickable = true

            //cancel sessionTimer if pause button pressed before timer finishes
            if (mSessionMode == "Time" && this::mSessionTimeCountdown.isInitialized) {
                mSessionTimeCountdown.cancel()
            }

            if (mSessionMode != "Endless") {
                binding.activityMainTextviewSessionCount.text = mSessionCount.toString()
            }

            mSessionActive = false
            mCountDownTimer = null
            mMediaPlayerSessionFinished.start()
            binding.activityMainButtonStartSession.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            binding.activityMainButtonStartSession.backgroundTintList =
                getColorStateList(R.color.start_blue)
        } else {
            if (mCountDownTimer != null) {
                return
            }
            binding.activityMainCardViewLeft.isClickable = false
            binding.activityMainCardViewRight.isClickable = false

            binding.activityMainTextFieldCountdown.isVisible = true
            mCountDownTimer =
                object : CountDownTimer((mCountDownTimerSeconds * 1000).toLong(), 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        binding.activityMainTextFieldCountdown.text =
                            "${(millisUntilFinished / 1000) + 1}"
                        mMediaPlayerCountdownStart.start()
                    }

                    override fun onFinish() {
                        binding.activityMainTextFieldCountdown.isVisible = false
                        mSessionActive = true
                        binding.activityMainButtonStartSession.setImageResource(R.drawable.ic_baseline_stop_24)
                        binding.activityMainButtonStartSession.backgroundTintList =
                            getColorStateList(R.color.stop_gray)
                        mMediaPlayerSessionStart.start()
                        mWorkoutStartTime = SystemClock.elapsedRealtime()

                        if (mSessionMode == "Time") {
                            mSessionTimeCountdown =
                                object : CountDownTimer((mSessionCount * 1000).toLong(), 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                        binding.activityMainTextviewSessionCount.text =
                                            "${(millisUntilFinished / 1000) + 1}"

                                        //play sound for last 3 seconds
                                        if ((millisUntilFinished / 1000) + 1 in 1..2) {
                                            mMediaPlayerCountdownStart.start()
                                        }
                                    }

                                    override fun onFinish() {
                                        performSessionAction()
                                        binding.activityMainTextviewSessionCount.text =
                                            mSessionCount.toString()
                                    }
                                }.start()
                        }
                    }
                }.start()
        }
    }

    private fun showBottomSheetSessionSettings() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = BottomSheetSessionSettingsBinding.inflate(dialog.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val exerciseName = dialogBinding.bottomSheetSessionSettingsExerciseName
        val modeName = dialogBinding.bottomSheetSessionSettingsModeName
        val textFieldAmount = dialogBinding.bottomSheetSessionSettingsTextfieldAmount
        val textFieldOutline = dialogBinding.bottomSheetSessionSettingsTextfieldOutline
        val transparentLayer = dialogBinding.bottomSheetSessionSettingsTransparentCardview

        exerciseName.text = mSessionExercise
        modeName.text = mSessionMode

        when (mSessionMode) {
            "Endless" -> {
                textFieldOutline.hint = "Amount"
                textFieldOutline.isEnabled = false
                transparentLayer.isVisible = true
            }
            "Time" -> {
                textFieldOutline.hint = "Seconds"
                textFieldOutline.isEnabled = true
                transparentLayer.isVisible = false
            }
            "Rep" -> {
                textFieldOutline.hint = "Amount"
                textFieldOutline.isEnabled = true
                transparentLayer.isVisible = false
            }
        }

        if (mSessionCount > 0) {
            textFieldAmount.setText(mSessionCount.toString())
        }

        val exerciseSelector = dialogBinding.bottomSheetSessionSettingsExerciseSelector
        exerciseSelector.setOnClickListener {
            showSessionOptionsDialog(
                "Exercise",
                DataConstants.SETTINGS_EXERCISE_LIST,
                true,
                mSessionExercise,
                exerciseName,
                textFieldOutline,
                textFieldAmount,
                transparentLayer
            )
        }

        val modeSelector = dialogBinding.bottomSheetSessionSettingsModeSelector
        modeSelector.setOnClickListener {
            showSessionOptionsDialog(
                "Mode",
                DataConstants.SETTINGS_MODE_LIST,
                false,
                mSessionMode,
                modeName,
                textFieldOutline,
                textFieldAmount,
                transparentLayer
            )
        }

        //to check textField when nothing got typed
        textFieldAmount.doAfterTextChanged {
            if (it != null && it.toString() != "") {
                mDataSingleton.setSetting(
                    DataConstants.SESSION_COUNT,
                    it.toString().toInt()
                )
                getSessionSettings()
            }
        }

        dialog.show()
    }

    private fun showSessionOptionsDialog(
        dialogTitle: String,
        recyclerViewElements: ArrayList<String>,
        isExercise: Boolean,
        currentSelection: String,
        sessionConfigurationText: TextView,
        textInputLayout: TextInputLayout,
        textInput: TextInputEditText,
        transparentLayer: CardView
    ) {
        val dialog = BottomSheetDialog(this)
        val dialogBinding =
            BottomSheetSessionSettingsSelectionBinding.inflate(dialog.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val toolbar = dialogBinding.customDialogSessionSettingsToolbar
        toolbar.title = dialogTitle

        val recyclerViewExercise = dialogBinding.customDialogSessionSettingsRecyclerviewExercise
        recyclerViewExercise.adapter = SessionSettingsAdapter(
            recyclerViewElements,
            this,
            isExercise,
            currentSelection,
            dialog
        )
        recyclerViewExercise.layoutManager = LinearLayoutManager(this)

        dialog.setOnDismissListener {
            getSessionSettings()
            //update text in bottomSheet
            if (isExercise) {
                sessionConfigurationText.text = mSessionExercise
            } else {
                sessionConfigurationText.text = mSessionMode
                when (mSessionMode) {
                    "Endless" -> {
                        textInputLayout.hint = "Amount"
                        textInputLayout.isEnabled = false
                        transparentLayer.isVisible = true
                        textInput.setText("")
                    }
                    "Time" -> {
                        textInputLayout.hint = "Seconds"
                        textInputLayout.isEnabled = true
                        transparentLayer.isVisible = false
                    }
                    "Rep" -> {
                        textInputLayout.hint = "Amount"
                        textInputLayout.isEnabled = true
                        transparentLayer.isVisible = false
                    }
                }
            }
        }
        dialog.show()
    }

    private fun getSessionSettings() {
        mSessionExercise = mDataSingleton.getSetting(DataConstants.EXERCISE_STRING) as String
        mSessionMode = mDataSingleton.getSetting(DataConstants.MODE_STRING) as String

        if (mSessionMode == "Endless") {
            mSessionCount = 0
            mDataSingleton.setSetting(DataConstants.SESSION_COUNT, 0)
        } else {
            mSessionCount = mDataSingleton.getSetting(DataConstants.SESSION_COUNT) as Int
        }

        binding.activityMainTextviewExercise.text = mSessionExercise
        binding.activityMainTextviewMode.text = mSessionMode

        if (mSessionCount == 0) {
            binding.activityMainTextviewSessionCount.text = "-"
        } else {
            binding.activityMainTextviewSessionCount.text = mSessionCount.toString()
        }
    }

    interface BottomSheetFragmentSessionListener {
        fun isDismissed()
    }

    private fun showSessionEndSheet() {
        mBottomSheetVisible = true
        val listener = object : BottomSheetFragmentSessionListener {
            override fun isDismissed() {
                mBottomSheetVisible = false
                //clear the arrays once the session stats have been dismissed
                mDataSingleton.mRepListSquats = arrayListOf()
                mDataSingleton.mRepListPushUps = arrayListOf()
                mDataSingleton.mRepListSitUps = arrayListOf()
                mSquatReps = 0
                mPushUpReps = 0
                mSitUpReps = 0
                mCurrentFrameCount = 0
                mRepStartTime = 0
            }
        }

        val bottomSheet =
            BottomSheetFragmentSession(
                mPoseClassification.getRepetitionCounter(),
                (SystemClock.elapsedRealtime() - mWorkoutStartTime),
                listener,
                supportFragmentManager
            )
        bottomSheet.show(supportFragmentManager, BottomSheetFragmentSession.TAG)
    }

    override fun startSession() {
        if (!mBottomSheetVisible) {
            performSessionAction()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.activity_main_menu_settings -> {
                val intent = (Intent(this, SettingsActivity::class.java))
                mResultLauncher.launch(intent)
            }
            R.id.activity_main_menu_switchCamera -> {
                switchCameraInput()
            }
            else -> {
                Log.e(
                    "",
                    "MainActivity::onOptionsItemsSelected ... unhandled ID ${item.itemId} encountered"
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //Porcupine Service and Service Management
    private var bound: Boolean = false
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LocalBinder
            mService = binder.service
            mService?.setCallback(this@MainActivity)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mService?.setCallback(null)
            mService = null
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, PorcupineService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        bound = true
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopService() {
        val serviceIntent = Intent(this, PorcupineService::class.java)
        if (bound) {
            unbindService(serviceConnection)
        }
        stopService(serviceIntent)
    }

    //Permissions Management
    private companion object {
        const val REQUEST_CODE_PERMISSIONS = 10
        val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).toTypedArray()
    }
}