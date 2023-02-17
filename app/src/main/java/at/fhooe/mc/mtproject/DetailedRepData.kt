package at.fhooe.mc.mtproject

import android.graphics.Bitmap

data class DetailedRepData(
    val duration: Long,
    val cameraBitmapList: ArrayList<ExerciseBitmap>,
    val overlayBitmapList: ArrayList<ExerciseBitmap>,
    val durationMovementDown: Long
)

data class ExerciseBitmap(val exerciseClass: String, val bitmap: Bitmap)