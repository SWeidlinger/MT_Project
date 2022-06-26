package at.fhooe.mc.mtproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.util.TypedValue
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
import at.fhooe.mc.mtproject.databinding.ActivityMainBinding
import at.fhooe.mc.mtproject.helpers.GraphicOverlay
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.timerTask

private const val TAG = "MainActivity"
private const val USE_ML_KIT = true

class MainActivity : AppCompatActivity() {
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

    //Right now classification of Poses is always active
    //might be interesting to only activate it when a session is started...
    private var mPoseClassificationActive = true

    // Select back camera as default
    private var mCameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var mDebugMode: Boolean = false
    private lateinit var mResultLauncher: ActivityResultLauncher<Intent>
    private var mModel: String = "MLKit Normal"

    private var mSpinnerResolutionID: Int = 1
    private var mSpinnerModelID: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getSettings()

        mPoseClassification = PoseClassification(this)

        mFpsUpdateTimer.scheduleAtFixedRate(
            timerTask {
                mFps = mCurrentTime - mPrevTime
            }, 0, 1000
        )

        mGraphicOverlay = binding.activityMainGraphicOverlay
        //request camera permissions
        if (allPermissionsGranted()) {
            mCameraExecutor = Executors.newSingleThreadExecutor()
            mPrevTime = SystemClock.elapsedRealtime()
            initPoseDetection()
            initImageAnalyzer()

            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.activityMainViewFinder.setOnTouchListener(configureDoubleTap())

        //switch camera on long Press
//        binding.activityMainViewFinder.setOnLongClickListener{
//            switchCameraInput()
//            return@setOnLongClickListener true
//        }
    }

    override fun onResume() {
        super.onResume()
        initImageAnalyzer()
        startCamera()
    }

    private fun getSettings() {
        mResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // There are no request codes
                    mDebugMode = result.data!!.getBooleanExtra("debugMode", false)
                    mSpinnerResolutionID = result.data!!.getIntExtra("resolution", 1)
                    mSpinnerModelID = result.data!!.getIntExtra("model", 0)
                    mThresholdIFL = result.data!!.getIntExtra("thresholdIFL", 50)

                    when (mSpinnerResolutionID) {
                        0 -> {
                            mImageResolution = Size(240, 320)
                        }
                        1 -> {
                            mImageResolution = Size(480, 640)
                        }
                        2 -> {
                            mImageResolution = Size(720, 1280)
                        }
                        3 -> {
                            mImageResolution = Size(1080, 1920)
                        }
                    }

                    when (mSpinnerModelID) {
                        0 -> {
                            mModel = "MLKit Normal"
                        }
                        1 -> {
                            mModel = "MLKit Accurate"
                        }
                        2 -> {
                            mModel = "MoveNet Thunder"
                        }
                        3 -> {
                            mModel = "MoveNet Lightning"
                        }
                        4 -> {
                            mModel = "MoveNet MultiPose"
                        }
                    }
                }
            }
    }

    //sets up the double tap, so that you can double tap to switch cameras
    private fun configureDoubleTap(): View.OnTouchListener {
        return object : View.OnTouchListener {
            private val gestureDetector = GestureDetector(this@MainActivity,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent?): Boolean {
                        //vibration for when the camera changes
                        binding.root.rootView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        switchCameraInput()
                        mGraphicOverlay.clear()
                        return super.onDoubleTap(e)
                    }
                })

            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                gestureDetector.onTouchEvent(p1)
                return true;
            }
        }
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
                startCamera()
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

    private fun initPoseDetection() {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
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
            val image = imageProxy.image
            if (image != null) {
                val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
                mPoseDetector
                    .process(inputImage)
                    .addOnFailureListener {
                        imageProxy.close()
                    }.addOnSuccessListener { pose ->
                        mPrevTime = mCurrentTime
                        mCurrentTime = SystemClock.elapsedRealtime()
                        val frontCamera = mCameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA

                        //not quite sure if this is needed, but im just gonna leave it here in case
                        //removing it breaks something
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            mGraphicOverlay.setImageSourceInfo(
                                imageProxy.width,
                                imageProxy.height,
                                frontCamera
                            )
                        } else {
                            mGraphicOverlay.setImageSourceInfo(
                                imageProxy.height,
                                imageProxy.width,
                                frontCamera
                            )
                        }
                        var poseClassification: ArrayList<String>? = null
                        if (mPoseClassificationActive) {
                            poseClassification = mPoseClassification.getPoseResult(pose)
                        }

                        val element = Draw(
                            mGraphicOverlay,
                            pose,
                            poseClassification,
                            mDebugMode,
                            mImageResolution,
                            mFps,
                            mThresholdIFL / 100.0,
                            supportActionBar!!.height
                        )

                        if (binding.root.childCount > 1) {
                            binding.root.removeViewAt(1)
                        }

                        binding.root.addView(mGraphicOverlay, 1)
                        mGraphicOverlay.clear()
                        mGraphicOverlay.add(element)

                        imageProxy.close()
                    }
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
                    this, mCameraSelector, mPreview, mImageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.activity_main_menu_settings -> {
                val intent = (Intent(this, SettingsActivity::class.java))
                intent.putExtra("debugMode", mDebugMode)
                intent.putExtra("resolution", mSpinnerResolutionID)
                intent.putExtra("model", mSpinnerModelID)
                intent.putExtra("thresholdIFL", mThresholdIFL)
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

    override fun onDestroy() {
        super.onDestroy()
        mCameraExecutor.shutdown()
        mFpsUpdateTimer.cancel()
    }

    //Permissions Management
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).toTypedArray()
    }
}