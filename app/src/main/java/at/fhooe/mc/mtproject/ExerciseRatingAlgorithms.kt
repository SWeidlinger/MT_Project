package at.fhooe.mc.mtproject

import android.util.Log
import at.fhooe.mc.mtproject.ExerciseRatingAlgorithmsUtils.Companion.getAngle
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

class SquatRatingAlgorithms {
    companion object {
        private const val SQUAT_DEPTH_THRESHOLD_ANGLE = 110
        private const val STANCE_WIDTH_THRESHOLD_DELTA = 8
        private const val TORSO_ALIGNMENT_DEVIATION_THRESHOLD = 10

        fun getSquatDepthAngle(pose: Pose, currentMinSquatDepth: Double): Double {
            val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
            val lKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
            val lAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

            val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
            val rKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
            val rAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

            val lKneeAngle = getAngle(lHip, lKnee, lAnkle, true)
            val rKneeAngle = getAngle(rHip, rKnee, rAnkle, true)

            //get the average of those two angles and add it to the array
            val avgAngle = (lKneeAngle + rKneeAngle) / 2

//            Log.e("LEFT KNEE: ", lKneeAngle.toString())
//            Log.e("RIGHT KNEE: ", rKneeAngle.toString())
//            Log.e("AVG: ", avgAngle.toString())

            //returns the maximum angle
            return maxOf(currentMinSquatDepth, avgAngle)
        }

        //compares the minAngle with the threshold, if it is equal or below the threshold
        //the score is 10 the maximum
        fun calculateSquatDepthScore(maxAngle: Double): String {
            val score = minOf(1.0, maxAngle / SQUAT_DEPTH_THRESHOLD_ANGLE) * 10
            return String.format(Locale.US, "%.2f", score)
        }

        fun getStanceWidth(pose: Pose): Double {
            val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

            val lAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
            val rAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

            var shoulderWidth = 0f
            if (lShoulder != null && rShoulder != null) {
                shoulderWidth = abs(lShoulder.position.x - rShoulder.position.x)
            }

            var ankleWidth = 0f
            if (lAnkle != null && rAnkle != null) {
                ankleWidth = abs(lAnkle.position.x - rAnkle.position.x)
            }

            val delta = abs(shoulderWidth - ankleWidth)

//            Log.e("SHOULDER WIDTH: ", shoulderWidth.toString())
//            Log.e("ANKLE WIDTH: ", ankleWidth.toString())
//            Log.e("DELTA", delta.toString())

            return delta.toDouble()
        }

        fun getStanceWidthScore(values: ArrayList<Double>): String {
            val score = minOf(1.0, (STANCE_WIDTH_THRESHOLD_DELTA / values.average())) * 10.0
            return String.format(Locale.US, "%.2f", score)
        }

        fun getTorsoAlignment(pose: Pose): Double {
            val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
            val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

            val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)

            var rTorsoLength = 0f
            if (rShoulder != null && rHip != null) {
                val xDistance = (rShoulder.position.x - rHip.position.x).pow(2)
                val yDistance = (rShoulder.position.y - rHip.position.y).pow(2)

                rTorsoLength = sqrt(abs(xDistance - yDistance))
            }

            var lTorsoLength = 0f
            if (lShoulder != null && lHip != null) {
                val xDistance = (lShoulder.position.x - lHip.position.x).pow(2)
                val yDistance = (lShoulder.position.y - lHip.position.y).pow(2)

                lTorsoLength = sqrt(abs(xDistance - yDistance))
            }

            val avgTorsoLength = (rTorsoLength + lTorsoLength) / 2

//            Log.e("LEFT TORSO", lTorsoLength.toString())
//            Log.e("RIGHT TORSO", rTorsoLength.toString())
//            Log.e("TORSO LENGTH", avgTorsoLength.toString())

            return avgTorsoLength.toDouble()
        }

        fun getTorsoAlignmentScore(values: ArrayList<Double>): String {
            if (values.size <= 1) {
                return "0.0"
            }

            val avg = values.average()

            var variance = 0.0
            for (value in values) {
                variance += (value - avg).pow(2)
            }

            variance /= (values.size - 1)

            val deviation = sqrt(variance)

            var score = 10.0
            if (deviation > 0) {
                score = minOf(1.0, (TORSO_ALIGNMENT_DEVIATION_THRESHOLD / deviation)) * 10
            }

//            Log.e("DEVIATION", deviation.toString())

            return String.format(Locale.US, "%.2f", score)
        }
    }
}

class PushUpRatingAlgorithms {
    companion object {
        const val ELBOW_THRESHOLD_ANGLE = 90
        const val HIP_THRESHOLD_ANGLE = 175

        //experimental
        fun getElbowAngle(pose: Pose): Double {
            val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

            val lWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
            val rWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

            val lElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
            val rElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)

            val lElbowAngle = getAngle(lShoulder, lElbow, lWrist, false)
            val rElbowAngle = getAngle(rShoulder, rElbow, rWrist, false)

            return (rElbowAngle + lElbowAngle) / 2
        }

        //experimental
        fun getHipAngle(pose: Pose): Double {
            val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
            val lAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

            val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
            val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
            val rAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

            val lHipAngle = getAngle(lShoulder, lHip, lAnkle, false)
            val rHipAngle = getAngle(rShoulder, rHip, rAnkle, false)

            return (lHipAngle + rHipAngle) / 2
        }

        fun calculateBodyAlignmentScore(
            elbowAngles: ArrayList<Double>,
            hipAngles: ArrayList<Double>
        ): String {
            val scoreElbow = minOf(1.0, (ELBOW_THRESHOLD_ANGLE / elbowAngles.average())) * 10
            val scoreHip = minOf(1.0, (HIP_THRESHOLD_ANGLE / hipAngles.average())) * 10

            val avgScore = (scoreElbow + scoreHip) / 2

            return String.format(Locale.US, "%.2f", avgScore)
        }
    }
}

class SitUpRatingAlgorithms {
    companion object {

    }
}

class ExerciseRatingAlgorithmsUtils {
    companion object {
        fun getAngle(
            firstPoint: PoseLandmark?,
            midPoint: PoseLandmark?,
            lastPoint: PoseLandmark?,
            isSquat: Boolean
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

            //returns the right angle for the knee angle
            return if (isSquat) {
                abs(result - 180)
            } else {
                result
            }
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