package at.fhooe.mc.mtproject.bottomSheet.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import at.fhooe.mc.mtproject.R

class SessionDetailedRepAdditionalInfoAdapter(
    private val additionalInfoList: ArrayList<DetailedRepInfoData>
) :
    RecyclerView.Adapter<SessionDetailedRepAdditionalInfoViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SessionDetailedRepAdditionalInfoViewHolder {
        LayoutInflater.from(parent.context).apply {
            val root = inflate(R.layout.session_detailed_rep_additional_info_item, null)
            return SessionDetailedRepAdditionalInfoViewHolder(root)
        }
    }

    override fun onBindViewHolder(
        holder: SessionDetailedRepAdditionalInfoViewHolder,
        position: Int
    ) {
        holder.title.text = additionalInfoList[position].title
        holder.value.text = additionalInfoList[position].value
    }

    override fun getItemCount() = additionalInfoList.size
}

class SessionDetailedRepAdditionalInfoViewHolder(root: View) : RecyclerView.ViewHolder(root) {
    val title: TextView =
        root.findViewById(R.id.session_detailed_rep_additional_info_title)
    val value: TextView =
        root.findViewById(R.id.session_detailed_rep_additional_info_value)
}

data class DetailedRepInfoData(val title: String, val value: String)