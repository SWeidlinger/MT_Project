package at.fhooe.mc.mtproject.bottomSheet.recyclerView

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.fhooe.mc.mtproject.DetailedRepData
import at.fhooe.mc.mtproject.PoseClassification
import at.fhooe.mc.mtproject.R
import at.fhooe.mc.mtproject.databinding.BottomSheetRepDetailedBinding
import at.fhooe.mc.mtproject.helpers.pose.RepetitionCounter
import at.fhooe.mc.mtproject.sessionDialog.RepReplayDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import java.util.*


class SessionNestedAdapter(
    private val repCounter: RepetitionCounter,
    private val exerciseName: String,
    private val fragmentManager: FragmentManager,
    private val detailedRepList: ArrayList<DetailedRepData>,
    private val repReplayActive: Boolean,
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
        val score = repCounter.getScore(position)
        if (repCounter.getScore(position) < 0) {
            holder.score.text = "-"
        } else {
            holder.score.text = String.format(Locale.US, "%.1f", score)
        }

        holder.cardView.setOnClickListener {
            showBottomSheetDialogRep(repNumber, detailedRepList, position, score)
        }
    }

    private fun showBottomSheetDialogRep(
        repNumber: Int,
        detailedRepList: ArrayList<DetailedRepData>,
        index: Int,
        score: Double
    ) {
        val bottomSheet = BottomSheetDialog(parentContext)
        val bottomSheetBinding = BottomSheetRepDetailedBinding.inflate(bottomSheet.layoutInflater)
        bottomSheet.setContentView(bottomSheetBinding.root)
        bottomSheet.setCancelable(false)

        val toolbar = bottomSheetBinding.dialogRepDetailedToolbar
        toolbar.title = "${exerciseName.dropLast(1)} Rep $repNumber"

        toolbar.setNavigationOnClickListener {
            bottomSheet.dismiss()
        }

        val replay = toolbar.menu.getItem(0)
        replay.isEnabled = repReplayActive
        if (!repReplayActive || detailedRepList.size <= 0) {
            val strikeThrough = SpannableString(replay.title)
            strikeThrough.setSpan(
                StrikethroughSpan(),
                0,
                replay.title!!.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            replay.title = strikeThrough
            replay.isEnabled = false
        }

        replay.setOnMenuItemClickListener {
            showRepReplayDialog(repNumber, detailedRepList[index])
            true
        }

        val additionalInfoList = arrayListOf<DetailedRepInfoData>()

        if (detailedRepList.size > 0 && detailedRepList.size > index) {
            additionalInfoList.addAll(
                getAdditionalInfoItems(
                    detailedRepList[index],
                    repCounter,
                    index
                )
            )
        } else if (repCounter.className == PoseClassification.SQUATS_CLASS) {
            //if the rep detailed data is not available the depth should still be available
            val maxSquatAngle =
                String.format(Locale.US, "%.0f", repCounter.maxSquatDepthList[index])
            additionalInfoList.add(
                DetailedRepInfoData(
                    "Max Squat Angle",
                    "$maxSquatAngle°"
                )
            )
        }

        val recyclerViewAdditionalInfo =
            bottomSheetBinding.dialogRepDetailedAdditionalInfoRecyclerView
        recyclerViewAdditionalInfo.adapter =
            SessionDetailedRepAdditionalInfoAdapter(additionalInfoList)
        recyclerViewAdditionalInfo.layoutManager =
            LinearLayoutManager(parentContext, LinearLayoutManager.HORIZONTAL, false)


        val scoreTextView = bottomSheetBinding.dialogRepDetailedTextviewOverallScore
        scoreTextView.text = if (score > 0) {
            "Total Score: ${String.format(Locale.US, "%.2f", score)}"
        } else {
            "Total Score: -"
        }

        val recyclerView = bottomSheetBinding.dialogRepDetailedRecyclerview
        recyclerView.adapter = SessionDetailedRepCategoryAdapter(
            repCounter.categoryDataList[index]
        )
        recyclerView.layoutManager = LinearLayoutManager(parentContext)
        bottomSheet.show()
    }

    private fun getAdditionalInfoItems(
        detailedRep: DetailedRepData,
        currentRepCounter: RepetitionCounter,
        index: Int
    ): ArrayList<DetailedRepInfoData> {
        val additionalInfoList = arrayListOf<DetailedRepInfoData>()

        when (currentRepCounter.className) {
            PoseClassification.SQUATS_CLASS -> {
                val maxSquatAngle =
                    String.format(Locale.US, "%.0f", currentRepCounter.maxSquatDepthList[index])
                additionalInfoList.add(
                    DetailedRepInfoData(
                        "Max Squat Angle",
                        "$maxSquatAngle°"
                    )
                )
            }
            PoseClassification.PUSHUPS_CLASS -> {

            }
            PoseClassification.SITUPS_CLASS -> {

            }
        }

        val durationUpValue =
            (detailedRep.duration - detailedRep.durationMovementDown.toDouble()) / 1000

        val duration =
            String.format(Locale.US, "%.2f", detailedRep.duration.toDouble() / 1000) + "s"
        var durationDown =
            String.format(
                Locale.US,
                "%.2f",
                detailedRep.durationMovementDown.toDouble() / 1000
            ) + "s"
        var durationUp = String.format(
            Locale.US,
            "%.2f",
            durationUpValue
        ) + "s"

        //something went wrong when measuring, since up or down cant
        //be lower than 0 so do not show the faulty values
        if (durationUpValue < 0.0 || (detailedRep.durationMovementDown.toDouble() / 1000) < 0.0) {
            durationDown = "-"
            durationUp = "-"
        }

        val newValues = arrayListOf(
            DetailedRepInfoData(
                "Total Rep Duration",
                duration
            ),
            DetailedRepInfoData(
                "Duration Down Phase",
                durationDown
            ),
            DetailedRepInfoData(
                "Duration Up Phase",
                durationUp
            )
        )
        additionalInfoList.addAll(newValues)

        return additionalInfoList
    }

    private fun showRepReplayDialog(
        repNumber: Int,
        detailedRep: DetailedRepData
    ) {
        val dialogTitle = "Replay ${exerciseName.dropLast(1)} Rep $repNumber"

        val dialog = RepReplayDialog(dialogTitle, detailedRep, parentContext)
        dialog.show(fragmentManager, "RepDetailedDialog")
    }

    override fun getItemCount() = repCounter.categoryDataList.size

    class SessionViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val cardView: MaterialCardView =
            root.findViewById(R.id.session_exercise_item_nested_cardView)
        val repNumber: TextView =
            root.findViewById(R.id.session_exercise_item_nested_repNumber)
        val score: TextView =
            root.findViewById(R.id.session_exercise_item_nested_score)
    }
}