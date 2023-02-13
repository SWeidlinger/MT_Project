package at.fhooe.mc.mtproject.bottomSheet.recyclerView

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import at.fhooe.mc.mtproject.DetailedRepData
import at.fhooe.mc.mtproject.R
import at.fhooe.mc.mtproject.sessionDialog.RepDetailedViewDialog
import com.google.android.material.card.MaterialCardView
import java.util.*

class SessionNestedAdapter(
    private val scoreCount: ArrayList<Double>,
    private val exerciseName: String,
    private val fragmentManager: FragmentManager,
    private val detailedRepList: ArrayList<DetailedRepData>
) :
    RecyclerView.Adapter<SessionNestedAdapter.SessionViewHolder>() {
    lateinit var parentContext: Context
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SessionViewHolder {
        LayoutInflater.from(parent.context).apply {
            val root = inflate(R.layout.session_exercise_item_nested_item, null)

            parentContext = parent.context

            return SessionViewHolder(root)
        }
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val repNumber = (position + 1)
        holder.repNumber.text = repNumber.toString()
        val score = scoreCount[position]
        if (scoreCount[position] < 0) {
            holder.score.text = "-"
        } else {
            holder.score.text = String.format(Locale.US, "%.1f", score)
        }

        holder.cardView.setOnClickListener {
            showRepDialog(repNumber, score, detailedRepList[position])
        }
    }

    private fun showRepDialog(
        repNumber: Int,
        score: Double,
        detailedRep: DetailedRepData
    ) {
        val dialogTitle = "$exerciseName Rep #$repNumber"

        val dialog = RepDetailedViewDialog(dialogTitle, detailedRep, parentContext)
        dialog.show(fragmentManager, "RepDetailedDialog")
    }

    override fun getItemCount() = scoreCount.size

    class SessionViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val cardView: MaterialCardView =
            root.findViewById(R.id.session_exercise_item_nested_cardView)
        val repNumber: TextView =
            root.findViewById(R.id.session_exercise_item_nested_repNumber)
        val score: TextView =
            root.findViewById(R.id.session_exercise_item_nested_score)
    }
}