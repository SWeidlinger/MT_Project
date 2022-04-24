package at.fhooe.mc.mtproject

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class MLKitPoseDetection {
    lateinit var mPoseDetector: PoseDetector
    lateinit var mImage: InputImage

    fun init(){
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        mPoseDetector = PoseDetection.getClient(options)

        class ImageAnalyzer : ImageAnalysis.Analyzer {
            override fun analyze(imageProxy: ImageProxy) {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    mImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    // Pass image to an ML Kit Vision API
                    // ...
                }
            }
        }

        Task<Pose> result = mPoseDetector.process(mImage)
            .addOnSuccessListener { results ->
                // Task completed successfully
                // ...
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
            }
    }


}