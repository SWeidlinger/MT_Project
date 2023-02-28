package at.fhooe.mc.mtproject.bottomSheet.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import at.fhooe.mc.mtproject.R

class SessionDetailedRepCategoryAdapter(
    private val categoryList: ArrayList<DetailedRepCategoryData>
) :
    RecyclerView.Adapter<SessionDetailedRepCategoryViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SessionDetailedRepCategoryViewHolder {
        LayoutInflater.from(parent.context).apply {
            val root = inflate(R.layout.session_detailed_rep_category_item, null)
            return SessionDetailedRepCategoryViewHolder(root)
        }
    }

    override fun onBindViewHolder(holder: SessionDetailedRepCategoryViewHolder, position: Int) {
        holder.categoryNameText.text = categoryList[position].category
        holder.score.text = categoryList[position].score

        if (categoryList[position].score.toDouble() >= 8) {
            holder.divider.isGone = true
            holder.tip.isGone = true
        } else {
            holder.divider.isGone = false
            holder.tip.isGone = false
            val tip = when (categoryList[position].category) {
                CategoryConstants.STANCE_WIDTH -> CategoryConstants.STANCE_WIDTH_TIP
                CategoryConstants.SQUAT_DEPTH -> CategoryConstants.SQUAT_DEPTH_TIP
                CategoryConstants.TORSO_ALIGNMENT -> CategoryConstants.TORSO_ALIGNMENT_TIP
                CategoryConstants.BODY_ALIGNMENT -> CategoryConstants.BODY_ALIGNMENT_TIP
                else -> "-"
            }
            holder.tip.text = "Tip: $tip"
        }
    }

    override fun getItemCount() = categoryList.size
}

class SessionDetailedRepCategoryViewHolder(root: View) : RecyclerView.ViewHolder(root) {
    val categoryNameText: TextView =
        root.findViewById(R.id.session_detailed_rep_category_item_textView_category)
    val score: TextView =
        root.findViewById(R.id.session_detailed_rep_category_item_textView_score)
    val divider: View =
        root.findViewById(R.id.session_detailed_rep_category_item_divider)
    val tip: TextView =
        root.findViewById(R.id.session_detailed_rep_category_item_tip)
}

data class DetailedRepCategoryData(
    val category: String,
    var score: String,
    val exerciseClassName: String
)

object CategoryConstants {
    const val STANCE_WIDTH = "Stance Width"
    const val STANCE_WIDTH_TIP = "Try to keep your feet shoulder width apart."
    const val SQUAT_DEPTH = "Squat Depth"
    const val SQUAT_DEPTH_TIP = "Try to squat as deep as possible."
    const val TORSO_ALIGNMENT = "Torso Alignment"
    const val TORSO_ALIGNMENT_TIP = "Try to keep your upper body straight."
    const val BODY_ALIGNMENT = "Body Alignment"
    const val BODY_ALIGNMENT_TIP = "Try to keep your body as straight as possible."
}