package at.fhooe.mc.mtproject.bottomSheet.recyclerView

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.fhooe.mc.mtproject.PoseClassification
import at.fhooe.mc.mtproject.R
import at.fhooe.mc.mtproject.helpers.pose.RepetitionCounter
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList

class SessionAdapter(private val repCount: ArrayList<RepetitionCounter>) :
    RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {
    private lateinit var mContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        LayoutInflater.from(parent.context).apply {
            val root = inflate(R.layout.sesssion_exercise_item, null)
            mContext = parent.context
            return SessionViewHolder(root)
        }
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val specificRepCount = repCount[position]
        holder.exerciseNameText.text = getExerciseName(specificRepCount.className)
        holder.repCountText.text = specificRepCount.numRepeats.toString()

        val usFormat = NumberFormat.getInstance(Locale.US)
        usFormat.maximumFractionDigits = 2

        holder.avgScoreText.text = "Avg. score: ${usFormat.format(specificRepCount.averageScore)}"

        if (specificRepCount.score.isEmpty()) {
            return
        }

        holder.expandableLayout.isVisible = false
        holder.nestedRecyclerView.adapter = SessionNestedAdapter(specificRepCount.score)
        holder.nestedRecyclerView.layoutManager =
            LinearLayoutManager(mContext, RecyclerView.HORIZONTAL, false)
        holder.nestedRecyclerView.addItemDecoration(
            DividerItemDecoration(
                mContext,
                LinearLayoutManager.VERTICAL
            )
        )

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
                return "Push-ups"
            }
            PoseClassification.SITUPS_CLASS -> {
                return "Sit-ups"
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