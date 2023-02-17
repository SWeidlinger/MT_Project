package at.fhooe.mc.mtproject.bottomSheet.recyclerView

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.fhooe.mc.mtproject.*
import at.fhooe.mc.mtproject.helpers.pose.RepetitionCounter
import com.google.android.material.card.MaterialCardView
import java.util.*

class SessionAdapter(
    private val repCount: ArrayList<RepetitionCounter>,
    private val fragmentManager: FragmentManager
) :
    RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {
    private lateinit var mContext: Context
    private lateinit var mDataSingleton: DataSingleton

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        LayoutInflater.from(parent.context).apply {
            val root = inflate(R.layout.sesssion_exercise_item, null)
            mContext = parent.context
            mDataSingleton = DataSingleton.getInstance(mContext)
            return SessionViewHolder(root)
        }
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val specificExerciseCount = repCount[position]
        val exerciseName = getExerciseName(specificExerciseCount.className)
        holder.exerciseNameText.text = exerciseName
        holder.repCountText.text = specificExerciseCount.numRepeats.toString()

        val averageScore = specificExerciseCount.averageScore
        if (averageScore < 0) {
            holder.avgScoreText.text = "Avg. score: -"
        } else {
            holder.avgScoreText.text =
                "Avg. score: ${
                    String.format(Locale.US, "%.2f", averageScore)
                }"
        }

        if (specificExerciseCount.categoryDataList.isEmpty()) {
            return
        }

        val detailedRepList: ArrayList<DetailedRepData> = when (specificExerciseCount.className) {
            PoseClassification.SQUATS_CLASS -> mDataSingleton.mRepListSquats
            PoseClassification.PUSHUPS_CLASS -> mDataSingleton.mRepListPushUps
            PoseClassification.SITUPS_CLASS -> mDataSingleton.mRepListSitUps
            else -> arrayListOf()
        }

        holder.expandableLayout.isVisible = false
        holder.nestedRecyclerView.adapter = SessionNestedAdapter(
            specificExerciseCount,
            exerciseName,
            fragmentManager,
            detailedRepList,
            mDataSingleton.getSetting(DataConstants.DETAILED_REP_INFO) as Boolean
        )
        holder.nestedRecyclerView.layoutManager =
            LinearLayoutManager(mContext, RecyclerView.HORIZONTAL, false)

        holder.cardView.setOnClickListener {
            holder.expandableLayout.isVisible = !holder.expandableLayout.isVisible
        }
    }

    override fun getItemCount() = repCount.size

    private fun getExerciseName(className: String): String {
        when (className) {
            PoseClassification.SQUATS_CLASS -> {
                return "Squats"
            }
            PoseClassification.PUSHUPS_CLASS -> {
                return "Push-Ups"
            }
            PoseClassification.SITUPS_CLASS -> {
                return "Sit-Ups"
            }
        }
        return "Exercise unknown"
    }

    class SessionViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val cardView: MaterialCardView =
            root.findViewById(R.id.session_exercise_item_cardView)
        val exerciseNameText: TextView =
            root.findViewById(R.id.session_exercise_item_parent_exerciseName)
        val repCountText: TextView =
            root.findViewById(R.id.session_exercise_item_parent_repCount)
        val avgScoreText: TextView =
            root.findViewById(R.id.session_exercise_item_parent_avgScore)
        val relativeLayout: RelativeLayout =
            root.findViewById(R.id.session_exercise_item_relativeLayout)
        val expandableLayout: ConstraintLayout =
            root.findViewById(R.id.session_exercise_item_expandable_layout)
        val nestedRecyclerView: RecyclerView =
            root.findViewById(R.id.session_exercise_item_recyclerView)
    }
}