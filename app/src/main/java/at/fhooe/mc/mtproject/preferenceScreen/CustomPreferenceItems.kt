package at.fhooe.mc.mtproject.preferenceScreen

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginStart
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import at.fhooe.mc.mtproject.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CustomPreferenceList : ListPreference {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    @SuppressLint("PrivateResource")
    override fun onClick() {
        val builder = MaterialAlertDialogBuilder(
            context,
            R.style.MaterialAlertDialog_rounded
        ).setSingleChoiceItems(entries, getValueIndex())
        { dialog, index ->
            if (callChangeListener(entryValues[index].toString())) {
                setValueIndex(index)
            }
            dialog.dismiss()
        }
            .setNegativeButton(com.google.android.material.R.string.mtrl_picker_cancel) { dialog, _ -> dialog.dismiss() }
            .setTitle(title)

        val dialog = builder.create()

        dialog.show()

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(context.getColor(R.color.customTextColor))
    }

    private fun getValueIndex() = entryValues.indexOf(value)
}