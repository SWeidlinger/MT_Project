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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import at.fhooe.mc.mtproject.bottomSheet.BottomSheetFragmentSession
import at.fhooe.mc.mtproject.databinding.ActivityMainBinding
import at.fhooe.mc.mtproject.helpers.GraphicOverlay
import at.fhooe.mc.mtproject.helpers.pose.RepetitionCounter
import at.fhooe.mc.mtproject.speechRecognition.PorcupineService
import at.fhooe.mc.mtproject.speechRecognition.PorcupineService.LocalBinder
import at.fhooe.mc.mtproject.speechRecognition.ServiceCallbacks
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

        mPoseClassification = PoseClassification(this)

        mFpsUpdateTimer.scheduleAtFixedRate(
            timerTask {
                mFps = mCurrentTime - mPrevTime
            }, 0, 500
        )

        mCameraExecutor = Executors.newSingleThreadExecutor()

        mMediaPlayerCountdownStart = MediaPlayer.create(this, R.raw.countdown_beep)
        mMediaPlayerSessionStart = MediaPlayer.create(this, R.raw.session_start)
        mMediaPlayerSessionFinished = MediaPlayer.create(this, R.raw.session_finished)

        if (mSpinnerModelID == 0) {
            initPoseDetectionFast()
        } else {
            initPoseDetectionAccurate()
        }

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
                        if (mDebugMode) {
                            repCounter = mPoseClassification.getRepetitionCounterFull()
                        }
                    } else {
                        mPoseClassification.clearRepetitions()
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
        } else {
            mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
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
                    }
                }.start()
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