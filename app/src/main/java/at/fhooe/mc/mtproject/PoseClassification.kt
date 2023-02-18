package at.fhooe.mc.mtproject

import android.content.Context
import android.media.MediaPlayer
import at.fhooe.mc.mtproject.helpers.pose.EMASmoothing
import at.fhooe.mc.mtproject.helpers.pose.PoseClassifier
import at.fhooe.mc.mtproject.helpers.pose.PoseSample
import at.fhooe.mc.mtproject.helpers.pose.RepetitionCounter
import com.google.mlkit.vision.pose.Pose
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class PoseClassification(
    context: Context,
    dataSingleton: DataSingleton,
    private val mFinalRepsSound: MediaPlayer,
    private val mRepCountSound: MediaPlayer
) {
    private lateinit var mPoseClassifier: PoseClassifier

    private val mRepCounter = ArrayList<RepetitionCounter>()
    private val mEMASmoothing = EMASmoothing()
    private var mLastRepResult = ""

    private val mPoseSampleArray = ArrayList<PoseSample>()
    private val mContext: Context

    private val mDataSingleton = dataSingleton

    private var mSessionMode = "Endless"
    private var mSessionCount = 0

    init {
        mContext = context
        loadPoseSamples(context)
    }

    private fun loadPoseSamples(context: Context) {
        if (mPoseSampleArray.isEmpty()) {
            val file = BufferedReader(InputStreamReader(context.assets.open(POSE_SAMPLES_FILE)))
            file.forEachLine {
                it.let {
                    val poseSample = PoseSample.getPoseSample(it, ",")
                    if (poseSample != null) {
                        mPoseSampleArray.add(poseSample)
                    }
                }
            }
            mPoseClassifier = PoseClassifier(mPoseSampleArray)
        }

        val chosenExercise: ArrayList<String> = arrayListOf()

        when (mDataSingleton.getSetting(DataConstants.EXERCISE_STRING)) {
            "All" -> chosenExercise.addAll(POSE_CLASSES)
            "Squat" -> chosenExercise.add(SQUATS_CLASS)
            "Push-Up" -> chosenExercise.add(PUSHUPS_CLASS)
            "Sit-Up" -> chosenExercise.add(SITUPS_CLASS)
        }

        mSessionMode = mDataSingleton.getSetting(DataConstants.MODE_STRING) as String
        mSessionCount = mDataSingleton.getSetting(DataConstants.SESSION_COUNT) as Int

        for (i in chosenExercise) {
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
            val repsAfter = repCount.addClassificationResult(classification, pose)
            if (repsAfter > repsBefore) {
                //play sound for last 3 reps
                if (mSessionMode == "Rep" && ((mSessionCount - repsAfter) in 1..2)) {
                    mFinalRepsSound.start()
                } else {
                    if (mSessionMode == "Rep" && mSessionCount - repsAfter == 0) {
                    } else {
                        mRepCountSound.start()
                    }
                }
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

        val maxConfidenceClass = classification.maxConfidenceClass
        val maxConfidenceClassResult = java.lang.String.format(
            Locale.US,
            "%s : %.2f confidence",
            maxConfidenceClass, classification.getClassConfidence(maxConfidenceClass)
                    / mPoseClassifier.confidenceRange()
        )
        result.add(maxConfidenceClassResult)
        result.add(maxConfidenceClass)

        return result
    }

    fun clearRepetitions() {
        if (mRepCounter.isNotEmpty()) {
            mRepCounter.clear()
        }
        loadPoseSamples(mContext)
    }

    fun getRepetitionCounter(): ArrayList<RepetitionCounter> {
        return ArrayList(mRepCounter.filter { it.numRepeats > 0 })
    }

    fun getRepetitionCounterFull(): ArrayList<RepetitionCounter> {
        return mRepCounter
    }

    companion object {
        const val POSE_SAMPLES_FILE = "pose/fitness_pose_samples.csv"
        const val PUSHUPS_CLASS = "pushups_down"
        const val SQUATS_CLASS = "squats_down"
        const val SITUPS_CLASS = "situps_down"
        val POSE_CLASSES: ArrayList<String> = arrayListOf(PUSHUPS_CLASS, SQUATS_CLASS, SITUPS_CLASS)
    }
}