package at.fhooe.mc.mtproject

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import at.fhooe.mc.mtproject.helpers.pose.EMASmoothing
import at.fhooe.mc.mtproject.helpers.pose.PoseClassifier
import at.fhooe.mc.mtproject.helpers.pose.PoseSample
import at.fhooe.mc.mtproject.helpers.pose.RepetitionCounter
import com.google.mlkit.vision.pose.Pose
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class PoseClassification(context: Context) {
    private lateinit var mPoseClassifier: PoseClassifier

    private val mRepCounter = ArrayList<RepetitionCounter>()
    private val mEMASmoothing = EMASmoothing()
    private var mLastRepResult = ""

    init {
        loadPoseSamples(context)
    }

    private fun loadPoseSamples(context: Context) {
        val poseSamplesArray = ArrayList<PoseSample>()
        val file = BufferedReader(InputStreamReader(context.assets.open(POSE_SAMPLES_FILE)))
        file.forEachLine {
            it.let {
                val poseSample = PoseSample.getPoseSample(it, ",")
                if (poseSample != null) {
                    poseSamplesArray.add(poseSample)
                }
            }
        }

        mPoseClassifier = PoseClassifier(poseSamplesArray)
        for (i in POSE_CLASSES) {
            mRepCounter.add(RepetitionCounter(i))
        }
    }

    fun getPoseResult(pose: Pose): ArrayList<String> {
        val result = ArrayList<String>()
        var classification = mPoseClassifier.classify(pose)

        classification = mEMASmoothing.getSmoothedResult(classification)

        if (pose.allPoseLandmarks.isEmpty()) {
            result.add(mLastRepResult)
            return result
        }

        for (repCount in mRepCounter) {
            val repsBefore = repCount.numRepeats
            val repsAfter = repCount.addClassificationResult(classification)
            if (repsAfter > repsBefore) {
                val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP)
                tg.release()
                mLastRepResult = String.format(
                    Locale.US,
                    "%s : %d reps",
                    repCount.className,
                    repsAfter
                )
                break
            }
        }
        result.add(mLastRepResult)

        if (pose.allPoseLandmarks.isNotEmpty()) {
            val maxConfidenceClass = classification.maxConfidenceClass
            val maxConfidenceClassResult = java.lang.String.format(
                Locale.US,
                "%s : %.2f confidence",
                maxConfidenceClass, classification.getClassConfidence(maxConfidenceClass)
                        / mPoseClassifier.confidenceRange()
            )
            result.add(maxConfidenceClassResult)
        }

        return result
    }

    private companion object {
        const val POSE_SAMPLES_FILE = "pose/fitness_pose_samples.csv"
        const val PUSHUPS_CLASS = "pushups_down"
        const val SQUATS_CLASS = "squats_down"
        const val SITUPS_CLASS = "situps_down"
        val POSE_CLASSES: ArrayList<String> = arrayListOf(PUSHUPS_CLASS, SQUATS_CLASS, SITUPS_CLASS)
    }
}