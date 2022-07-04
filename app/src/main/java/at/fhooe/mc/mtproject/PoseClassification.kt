package at.fhooe.mc.mtproject

import android.content.Context
import android.media.MediaPlayer
import at.fhooe.mc.mtproject.helpers.pose.EMASmoothing
import at.fhooe.mc.mtproject.helpers.pose.PoseClassifier
import at.fhooe.mc.mtproject.helpers.pose.PoseSample
import at.fhooe.mc.mtproject.helpers.pose.RepetitionCounter
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.math.*

class PoseClassification(context: Context) {
    private lateinit var mPoseClassifier: PoseClassifier

    private val mRepCounter = ArrayList<RepetitionCounter>()
    private val mEMASmoothing = EMASmoothing()
    private var mLastRepResult = ""

    private val mPoseSampleArray = ArrayList<PoseSample>()
    private val mContext: Context

    private lateinit var mRepCountSound: MediaPlayer

    init {
        mContext = context
        loadPoseSamples(context)
    }

    private fun loadPoseSamples(context: Context) {
        if (mPoseSampleArray.isEmpty()) {
            mRepCountSound = MediaPlayer.create(mContext, R.raw.rep_count)
            val file = BufferedReader(InputStreamReader(context.assets.open(POSE_SAMPLES_FILE)))
            file.forEachLine {
                it.let {
                    val poseSample = PoseSample.getPoseSample(it, ",")
                    if (poseSample != null) {
                        mPoseSampleArray.add(poseSample)
                    }
                }
            }
        }

        mPoseClassifier = PoseClassifier(mPoseSampleArray)
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
            val repsAfter = repCount.addClassificationResult(classification, pose)
            if (repsAfter > repsBefore) {
                mRepCountSound.start()
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

    fun clearRepetitions() {
        mRepCounter.clear()
        loadPoseSamples(mContext)
    }

    fun getRepetitionCounter(): ArrayList<RepetitionCounter> {
        return ArrayList(mRepCounter.filter { it.numRepeats > 0 })
    }

    fun getRepetitionCounterFull(): ArrayList<RepetitionCounter>{
        return mRepCounter
    }

    companion object {
        const val POSE_SAMPLES_FILE = "pose/fitness_pose_samples.csv"
        const val PUSHUPS_CLASS = "pushups_down"
        const val SQUATS_CLASS = "squats_down"
        const val SITUPS_CLASS = "situps_down"
        val POSE_CLASSES: ArrayList<String> = arrayListOf(PUSHUPS_CLASS, SQUATS_CLASS, SITUPS_CLASS)

        fun getAngle(
            firstPoint: PoseLandmark?,
            midPoint: PoseLandmark?,
            lastPoint: PoseLandmark?
        ): Double {
            if (firstPoint == null || midPoint == null || lastPoint == null) {
                return 0.0
            }
            var result = Math.toDegrees(
                atan2(
                    lastPoint.position.y - midPoint.position.y,
                    lastPoint.position.x - midPoint.position.x
                ).toDouble()
                        - atan2(
                    firstPoint.position.y - midPoint.position.y,
                    firstPoint.position.x - midPoint.position.x
                )
            )
            result = abs(result) // Angle should never be negative
            if (result > 180) {
                result = 360.0 - result // Always get the acute representation of the angle
            }

            return result
        }

        fun getAngleThreeCoordinates(
            firstPoint: PoseLandmark?,
            midPoint: PoseLandmark?,
            lastPoint: PoseLandmark?
        ): Double {
            if (firstPoint == null || midPoint == null || lastPoint == null) {
                return 0.0
            }
            val vAx = abs((midPoint.position3D.x - lastPoint.position3D.x))
            val vAy = abs((midPoint.position3D.y - lastPoint.position3D.y))
            val vAz = abs((midPoint.position3D.z - lastPoint.position3D.z))

            val vBx = abs((firstPoint.position3D.x - lastPoint.position3D.x))
            val vBy = abs((firstPoint.position3D.y - lastPoint.position3D.y))
            val vBz = abs((firstPoint.position3D.z - lastPoint.position3D.z))

            val vCx = abs((firstPoint.position3D.x - midPoint.position3D.x))
            val vCy = abs((firstPoint.position3D.y - midPoint.position3D.y))
            val vCz = abs((firstPoint.position3D.z - midPoint.position3D.z))

            val lengthA = sqrt(vAx.pow(2) + vAy.pow(2) + vAz.pow(2))
            val lengthB = sqrt(vBx.pow(2) + vBy.pow(2) + vBz.pow(2))
            val lengthC = sqrt(vCx.pow(2) + vCy.pow(2) + vCz.pow(2))

            val radian =
                acos(((lengthA.pow(2) + lengthC.pow(2) - lengthB.pow(2)) / (2 * lengthA * lengthC)).toDouble())

            return Math.toDegrees(radian)
        }
    }
}