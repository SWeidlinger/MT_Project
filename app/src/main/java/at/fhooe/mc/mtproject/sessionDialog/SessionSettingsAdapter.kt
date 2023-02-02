package at.fhooe.mc.mtproject.sessionDialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import at.fhooe.mc.mtproject.R
import at.fhooe.mc.mtproject.SettingConstants
import at.fhooe.mc.mtproject.SettingsSingleton
import com.google.android.material.card.MaterialCardView

class SessionSettingsAdapter(
    private val settingsList: ArrayList<String>,
    context: Context,
    isExercise: Boolean,
    initialSelection: String,
) :
    RecyclerView.Adapter<SessionSettingsAdapter.SessionViewHolder>() {
    private var mIsCheckedHolder: SessionViewHolder? = null
    private var mSettingsSingleton = SettingsSingleton.getInstance(context)
    private var mIsExercise = isExercise
    private var mInitialSelection = initialSelection
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SessionViewHolder {
        LayoutInflater.from(parent.context).apply {
            val root = inflate(R.layout.session_setting_item, null)
            return SessionViewHolder(root)
        }
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.settingName.text = settingsList[position]

        if (mInitialSelection == settingsList[position]) {
            holder.cardView.isChecked = true
            mIsCheckedHolder = holder
            saveSetting(position)
        }

        holder.cardView.setOnClickListener {
            if (holder.cardView == mIsCheckedHolder?.cardView) {
                return@setOnClickListener
            }
            holder.cardView.isChecked = !holder.cardView.isChecked
            mIsCheckedHolder?.cardView?.isChecked = false
            mIsCheckedHolder = holder
            saveSetting(position)
        }
    }

    private fun saveSetting(position: Int) {
        if (mIsExercise) {
            mSettingsSingleton.setSetting(
                SettingConstants.EXERCISE_STRING,
                settingsList[position]
            )
        } else {
            mSettingsSingleton.setSetting(
                SettingConstants.MODE_STRING,
                settingsList[position]
            )
        }
    }

    override fun getItemCount() = settingsList.size

    class SessionViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val cardView: MaterialCardView =
            root.findViewById(R.id.session_setting_item_cardview)
        val settingName: TextView =
            root.findViewById(R.id.session_setting_item_name)
    }
}