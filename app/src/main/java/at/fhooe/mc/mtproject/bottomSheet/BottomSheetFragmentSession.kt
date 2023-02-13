package at.fhooe.mc.mtproject.bottomSheet

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.fhooe.mc.mtproject.MainActivity
import at.fhooe.mc.mtproject.R
import at.fhooe.mc.mtproject.bottomSheet.recyclerView.SessionAdapter
import at.fhooe.mc.mtproject.helpers.pose.RepetitionCounter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

class BottomSheetFragmentSession(
    repCounter: ArrayList<RepetitionCounter>,
    workoutTime: Long,
    private val listener: MainActivity.BottomSheetFragmentSessionListener,
    private val fragmentManager: FragmentManager
) :
    BottomSheetDialogFragment() {
    private val mRepCounter: ArrayList<RepetitionCounter>
    private val mWorkoutTime: Long
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mOverallAvgScore: TextView
    private lateinit var mWorkoutTimeText: TextView
    private lateinit var mNoWorkoutsDetectedText: TextView

    init {
        mRepCounter = repCounter
        mWorkoutTime = workoutTime
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_session_end, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mRecyclerView = view.findViewById(R.id.frag_bottom_sheet_recyclervView)
        mOverallAvgScore = view.findViewById(R.id.frag_bottom_sheet_overallAvgScore)
        mWorkoutTimeText = view.findViewById(R.id.frag_bottom_sheet_workoutTime)
        mWorkoutTimeText.text = String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(mWorkoutTime),
            TimeUnit.MILLISECONDS.toSeconds(mWorkoutTime) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mWorkoutTime))
        )
        mNoWorkoutsDetectedText = view.findViewById(R.id.frag_bottom_sheet_noWorkoutsDetected)

        if (mRepCounter.isEmpty()) {
            mNoWorkoutsDetectedText.isVisible = true
            return
        }

        mNoWorkoutsDetectedText.isVisible = false

        var overallAvgScore = 0.0
        var exercisesSkipped = 0
        for (i in mRepCounter) {
            if (i.averageScore < 0) {
                exercisesSkipped++
            } else {
                overallAvgScore += i.averageScore
            }
        }
        overallAvgScore /= mRepCounter.size - exercisesSkipped

        val usFormat = NumberFormat.getInstance(Locale.US)
        usFormat.maximumFractionDigits = 2

        if (overallAvgScore.isNaN()) {
            mOverallAvgScore.text = "-"
        } else {
            mOverallAvgScore.text = String.format(Locale.US, "%.2f", overallAvgScore)
        }

        mRecyclerView.adapter = SessionAdapter(mRepCounter, fragmentManager)
        mRecyclerView.layoutManager = LinearLayoutManager(view.context)
        mRecyclerView.isNestedScrollingEnabled = true
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener.isDismissed()
    }

    companion object {
        const val TAG = "CustomBottomSheetDialogFragment"
    }
}