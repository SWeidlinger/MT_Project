package at.fhooe.mc.mtproject

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import at.fhooe.mc.mtproject.databinding.ActivitySettingsBinding
import at.fhooe.mc.mtproject.helpers.CameraImageGraphic
import at.fhooe.mc.mtproject.helpers.GraphicOverlay

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.setttings_preference, rootKey)

        val countdownTimer =
            preferenceManager.findPreference<EditTextPreference>("edit_text_countdown_timer")
        countdownTimer?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.setSelection(editText.length())
        }

        val thresholdIFL =
            preferenceManager.findPreference<EditTextPreference>("edit_text_threshold_ifl")
        thresholdIFL?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.hint = "0-100"
            editText.setSelection(editText.length())
        }
    }
}

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var mDataSingleton: DataSingleton

    private var mDebugModeValue = false
    private var mResolutionValue = 1
    private var mModelValue = 0
    private var mThresholdIFLValue = 50
    private var mCountDownTimerValue = 3
    private var mSyncPreviewAndOverlayValue = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDataSingleton = DataSingleton.getInstance(this)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.activity_settings_frame_layout,
                    SettingsFragment()
                )
                .commit()
        }

        //activate back button on action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backButtonPressed()
            }
        })
    }

    private fun updateSettingValues() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val debugMode = preferences.getBoolean("switch_debugMode", false)
        val countDownTimerString = preferences.getString("edit_text_countdown_timer", "3")
        val thresholdIFLString = preferences.getString("edit_text_threshold_ifl", "50")
        val resolution = preferences.getString("list_resolution", "1")!!.toInt()
        val syncPreviewAndOverlay =
            preferences.getBoolean("switch_sync_preview_and_overlay", true)
        val model = preferences.getString("list_model", "0")!!.toInt()

        var countDownTimerValue = 3
        var thresholdIFLValue = 50

        if (!countDownTimerString.isNullOrEmpty()) {
            countDownTimerValue = countDownTimerString.toInt()
        }

        if (!thresholdIFLString.isNullOrEmpty()) {
            thresholdIFLValue = thresholdIFLString.toInt()
        }

        mDebugModeValue = debugMode
        mResolutionValue = resolution
        mModelValue = model
        mThresholdIFLValue = thresholdIFLValue
        mCountDownTimerValue = countDownTimerValue
        mSyncPreviewAndOverlayValue = syncPreviewAndOverlay
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onResume() {
        super.onResume()
        updateSettingValues()
    }

    private fun didValuesChange(): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val debugMode = preferences.getBoolean("switch_debugMode", false)
        val countDownTimerString = preferences.getString("edit_text_countdown_timer", "3")
        val thresholdIFLString = preferences.getString("edit_text_threshold_ifl", "50")
        val resolution = preferences.getString("list_resolution", "1")!!.toInt()
        val syncPreviewAndOverlay =
            preferences.getBoolean("switch_sync_preview_and_overlay", true)
        val model = preferences.getString("list_model", "0")!!.toInt()

        var countDownTimerValue = 3
        var thresholdIFLValue = 50

        if (!countDownTimerString.isNullOrEmpty()) {
            countDownTimerValue = countDownTimerString.toInt()
        }

        if (!thresholdIFLString.isNullOrEmpty()) {
            thresholdIFLValue = thresholdIFLString.toInt()
        }

        var didValuesChange = false

        //check if settings changed and change all if one changed
        if (mDebugModeValue != debugMode || mResolutionValue != resolution ||
            mModelValue != model ||
            mThresholdIFLValue != thresholdIFLValue ||
            mCountDownTimerValue != countDownTimerValue ||
            mSyncPreviewAndOverlayValue != syncPreviewAndOverlay
        ) {
            mDebugModeValue = debugMode
            mResolutionValue = resolution
            mModelValue = model
            mThresholdIFLValue = thresholdIFLValue
            mCountDownTimerValue = countDownTimerValue
            mSyncPreviewAndOverlayValue = syncPreviewAndOverlay

            didValuesChange = true
        }

        return didValuesChange
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                backButtonPressed()
            }
        }
        return true
    }

    private fun backButtonPressed() {
        if (!didValuesChange()) {
            //skip updating in main activity since no values changed
            setResult(Activity.RESULT_CANCELED)
        } else {
            mDataSingleton.setSetting(
                DataConstants.DEBUG_MODE,
                mDebugModeValue
            )
            mDataSingleton.setSetting(
                DataConstants.RESOLUTION,
                mResolutionValue
            )
            mDataSingleton.setSetting(
                DataConstants.MODEL,
                mModelValue
            )
            mDataSingleton.setSetting(
                DataConstants.THRESHOLD_IFL,
                mThresholdIFLValue
            )
            mDataSingleton.setSetting(
                DataConstants.COUNTDOWN_TIMER,
                mCountDownTimerValue
            )
            mDataSingleton.setSetting(
                DataConstants.SYNC_PREVIEW_AND_OVERLAY,
                mSyncPreviewAndOverlayValue
            )
            setResult(
                Activity.RESULT_OK,
            )
        }
        finish()
    }
}