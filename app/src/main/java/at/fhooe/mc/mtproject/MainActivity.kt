package at.fhooe.mc.mtproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import at.fhooe.mc.mtproject.bottomSheet.BottomSheetFragmentSession
import at.fhooe.mc.mtproject.databinding.ActivityMainBinding
import at.fhooe.mc.mtproject.databinding.BottomSheetSessionSettingsBinding
import at.fhooe.mc.mtproject.databinding.DialogSesssionSettingsBinding
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
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), ServiceCallbacks {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mCameraExecutor: ExecutorService
    private lateinit var mCameraProvider: ProcessCameraProvider
    private var mImageResolution: Size = Size(480, 640)
    private lateinit var mImageAnalyzer: ImageAnalysis
    private lateinit var mPreview: Preview
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

    private var mRotationDegrees: Int = 0

    // Select back camera as default
    private var mCameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var mDebugMode: Boolean = false
    private lateinit var mResultLauncher: ActivityResultLauncher<Intent>
    private var mModel: String = "MLKit Normal"

    private var mSpinnerResolutionID: Int = 1
    private var mSpinnerModelID: Int = 0

    private var mCountDownTimerSeconds: Int = 3
    private var mCountDownTimer: CountDownTimer? = null

    private var mService: PorcupineService? = null

    private var mBottomSheetVisible = false

    private lateinit var mMediaPlayerCountdownStart: MediaPlayer
    private lateinit var mMediaPlayerSessionFinished: MediaPlayer
    private lateinit var mMediaPlayerSessionStart: MediaPlayer

    private lateinit var mSettingsSingleton: SettingsSingleton

    private var mSessionExercise = "All"
    private var mSessionMode = "Endless"
    private var mSessionCount = 0
    private lateinit var mSessionTimeCountdown: CountDownTimer

    private var mCameraSettingSelection = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.activityMainButtonStartSession.setOnClickListener {
            performSessionAction()
        }

        binding.activityMainViewFinder.setOnTouchListener(configureDoubleTap())

        mSettingsSingleton = SettingsSingleton.getInstance(this)

        mGraphicOverlay = binding.activityMainGraphicOverlay

        getSettings()
        getSessionSettings()

        mFpsUpdateTimer.scheduleAtFixedRate(
            timerTask {
                mFps = mCurrentTime - mPrevTime
            }, 0, 500
        )

        //switch to front camera if saved in settings
        if (mCameraSettingSelection == 1) {
            mCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }

        mCameraExecutor = Executors.newSingleThreadExecutor()

        mMediaPlayerCountdownStart = MediaPlayer.create(this, R.raw.countdown_beep)
        mMediaPlayerSessionStart = MediaPlayer.create(this, R.raw.session_start)
        mMediaPlayerSessionFinished = MediaPlayer.create(this, R.raw.session_finished)

        if (mSpinnerModelID == 0) {
            initPoseDetectionFast()
        } else {
            initPoseDetectionAccurate()
        }

        mPoseClassification = PoseClassification(this, mSettingsSingleton)

        mPrevTime = SystemClock.elapsedRealtime()

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
        mDebugMode = mSettingsSingleton.getSetting(SettingConstants.DEBUG_MODE) as Boolean
        mSpinnerResolutionID = mSettingsSingleton.getSetting(SettingConstants.RESOLUTION) as Int
        mThresholdIFL = mSettingsSingleton.getSetting(SettingConstants.THRESHOLD_IFL) as Int
        mCountDownTimerSeconds =
            mSettingsSingleton.getSetting(SettingConstants.COUNTDOWN_TIMER) as Int

        val spinnerModePrev = mSpinnerModelID
        mSpinnerModelID = mSettingsSingleton.getSetting(SettingConstants.MODEL) as Int

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
            mSettingsSingleton.getSetting(SettingConstants.CAMERA_SELECTION) as Int
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
            mRotationDegrees = rotationDegrees
            val image = imageProxy.image ?: return@setAnalyzer
            val inputImage = InputImage.fromMediaImage(image, rotationDegrees)

            mPoseDetector
                .process(inputImage)
                .addOnFailureListener {
                    imageProxy.close()
                }.addOnSuccessListener { pose ->
                    mPrevTime = mCurrentTime
                    mCurrentTime = SystemClock.elapsedRealtime()
                    val frontCameraUsed =
                        mCameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA

                    mGraphicOverlay.setImageSourceInfo(
                        imageProxy.height,
                        imageProxy.width,
                        frontCameraUsed
                    )

                    var poseClassification: ArrayList<String>? = null
                    var repCounter: ArrayList<RepetitionCounter>? = null
                    if (mSessionActive) {
                        poseClassification = mPoseClassification.getPoseResult(pose)
                        repCounter = mPoseClassification.getRepetitionCounterFull()
                    } else {
                        mPoseClassification.clearRepetitions()
                    }

                    //add to cardView when rep mode activated
                    if (mSessionMode == "Rep") {
                        val currentRepCount = repCounter?.get(0)?.numRepeats
                        if (currentRepCount != null) {
                            val repsLeft = mSessionCount - currentRepCount
                            if (repsLeft == 0) {
                                performSessionAction()
                                binding.activityMainTextviewSessionCount.text =
                                    mSessionCount.toString()
                            } else {
                                binding.activityMainTextviewSessionCount.text = repsLeft.toString()
                            }
                        }
                    }

                    val element = Draw(
                        mGraphicOverlay,
                        pose,
                        poseClassification,
                        repCounter,
                        mDebugMode,
                        mImageResolution,
                        mFps,
                        mThresholdIFL / 100.0,
                        supportActionBar!!.height,
                        mSpinnerModelID
                    )

                    mGraphicOverlay.clear()
                    mGraphicOverlay.add(element)

                    imageProxy.close()
                }
        }
    }

    private fun switchCameraInput() {
        if (mCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            mCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            mSettingsSingleton.setSetting(SettingConstants.CAMERA_SELECTION, 1)
        } else {
            mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            mSettingsSingleton.setSetting(SettingConstants.CAMERA_SELECTION, 0)
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

            mPreview.setSurfaceProvider(binding.activityMainViewFinder.surfaceProvider)
            try {
                // Unbind use cases before rebinding
                mCameraProvider.unbindAll()
                // Bind use cases to camera
                mCameraProvider.bindToLifecycle(
                    this, mCameraSelector, mImageAnalyzer, mPreview
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun performSessionAction() {
        if (mSessionActive) {
            showSessionEndSheet()
            binding.activityMainCardViewLeft.isClickable = true

            //cancel sessionTimer if pause button pressed before timer finishes
            if (mSessionMode == "Time") {
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
                SettingConstants.SETTINGS_EXERCISE_LIST,
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
                SettingConstants.SETTINGS_MODE_LIST,
                false,
                mSessionMode,
                modeName,
                textFieldOutline,
                textFieldAmount,
                transparentLayer
            )
        }

        //to check the textfield when there is nothing typed
        textFieldAmount.doAfterTextChanged {
            if (it != null && it.toString() != "") {
                mSettingsSingleton.setSetting(
                    SettingConstants.SESSION_COUNT,
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
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialogBinding = DialogSesssionSettingsBinding.inflate(dialog.layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(false)

        val title = dialogBinding.customDialogSessionSettingsTitle
        title.text = dialogTitle

        val recyclerViewExercise = dialogBinding.customDialogSessionSettingsRecyclerviewExercise
        recyclerViewExercise.adapter = SessionSettingsAdapter(
            recyclerViewElements,
            this,
            isExercise,
            currentSelection
        )
        recyclerViewExercise.layoutManager = LinearLayoutManager(this)

        val cancel = dialogBinding.customDialogSessionSettingBtnCancel
        cancel.setOnClickListener {
            dialog.dismiss()
        }

        val save = dialogBinding.customDialogSessionSettingBtnSave
        save.setOnClickListener {
            dialog.dismiss()
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
        mSessionExercise = mSettingsSingleton.getSetting(SettingConstants.EXERCISE_STRING) as String
        mSessionMode = mSettingsSingleton.getSetting(SettingConstants.MODE_STRING) as String

        if (mSessionMode == "Endless") {
            mSessionCount = 0
            mSettingsSingleton.setSetting(SettingConstants.SESSION_COUNT, 0)
        } else {
            mSessionCount = mSettingsSingleton.getSetting(SettingConstants.SESSION_COUNT) as Int
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
            }
        }

        val bottomSheet =
            BottomSheetFragmentSession(
                mPoseClassification.getRepetitionCounter(),
                (SystemClock.elapsedRealtime() - mWorkoutStartTime),
                listener
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