package at.fhooe.mc.mtproject

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.android.odml.image.MlImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

private const val TAG = "MLKitPoseDetection"

class MLKitPoseDetection(_image: InputImage) {
    lateinit var mPoseDetector: PoseDetector
    private val mImage: InputImage = _image

    fun init(){
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        mPoseDetector = PoseDetection.getClient(options)
    }

    fun detectInImage(){
        mPoseDetector.process(mImage).continueWith { task ->
            val pose = task.result
            var classificationResult: List<String> = ArrayList()
            Log.d(TAG, pose.toString())
            Log.d(TAG, classificationResult.toString())
        }
    }

    fun stop(){
        mPoseDetector.close()
    }
}