package at.fhooe.mc.mtproject.bottomSheet.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import at.fhooe.mc.mtproject.R

class SessionNestedAdapter(private val scoreCount: ArrayList<Int>) :
    RecyclerView.Adapter<SessionNestedAdapter.SessionViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SessionNestedAdapter.SessionViewHolder {
        LayoutInflater.from(parent.context).apply {
            val root = inflate(R.layout.session_exercise_item_nested_item, null)
            return SessionNestedAdapter.SessionViewHolder(root)
        }
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.repNumber.text = (position + 1).toString()
        holder.score.text = scoreCount[position].toDouble().toString()
    }

    override fun getItemCount() = scoreCount.size

    class SessionViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val repNumber: TextView =
            root.findViewById(R.id.session_exercise_item_nested_repNumber)
        val score: TextView =
            root.findViewById(R.id.session_exercise_item_nested_score)
    }


}