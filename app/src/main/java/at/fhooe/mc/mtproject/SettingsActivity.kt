package at.fhooe.mc.mtproject

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import at.fhooe.mc.mtproject.databinding.ActivitySettingsBinding

class SettingsFragment(private val context: Context) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.setttings_preference, rootKey)

        val countdownTimer =
            preferenceManager.findPreference<EditTextPreference>("edit_text_countdown_timer")
        countdownTimer?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.hint = "Timer in Seconds"
            editText.setSelection(editText.length())
        }

        val thresholdIFL =
            preferenceManager.findPreference<EditTextPreference>("edit_text_threshold_ifl")
        thresholdIFL?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.hint = "0-100"
            editText.setSelection(editText.length())
        }

        val amountFramesForFrame =
            preferenceManager.findPreference<EditTextPreference>("edit_text_max_frames_rep")
        amountFramesForFrame?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.hint = "Amount of Frames"
            editText.setSelection(editText.length())
        }

        val detailedRepInfo =
            preferenceManager.findPreference<SwitchPreference>("switch_detailedRepInfo")

        detailedRepInfo!!.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean){
                preferenceManager.findPreference<EditTextPreference>("edit_text_max_frames_rep")?.isEnabled = true
                preferenceManager.findPreference<SwitchPreference>("switch_save_active_movement")?.isEnabled = true
            }else{
                preferenceManager.findPreference<EditTextPreference>("edit_text_max_frames_rep")?.isEnabled = false
                preferenceManager.findPreference<SwitchPreference>("switch_save_active_movement")?.isEnabled = false
            }
            true
        }

        //disable preferences if not enabled
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val detailedRepInfoValue = preferences.getBoolean("switch_detailedRepInfo", true)
        if (detailedRepInfoValue) {
            preferenceScreen.findPreference<EditTextPreference>("edit_text_max_frames_rep")?.isEnabled = true
            preferenceScreen.findPreference<SwitchPreference>("switch_save_active_movement")?.isEnabled = true
        }else{
            preferenceScreen.findPreference<EditTextPreference>("edit_text_max_frames_rep")?.isEnabled = false
            preferenceScreen.findPreference<SwitchPreference>("switch_save_active_movement")?.isEnabled = false
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
    private var mDetailedRepInfoValue = true
    private var mMaxFramesRepValue = 50
    private var mSaveActiveMovement = false

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
                    SettingsFragment(context = this)
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
        val detailedRepInfo = preferences.getBoolean("switch_detailedRepInfo", true)
        val countDownTimerString = preferences.getString("edit_text_countdown_timer", "3")
        val thresholdIFLString = preferences.getString("edit_text_threshold_ifl", "50")
        val resolution = preferences.getString("list_resolution", "1")!!.toInt()
        val syncPreviewAndOverlay =
            preferences.getBoolean("switch_sync_preview_and_overlay", true)
        val model = preferences.getString("list_model", "0")!!.toInt()
        val maxFramesRepString = preferences.getString("edit_text_max_frames_rep", "50")
        val saveActiveMovement = preferences.getBoolean("switch_save_active_movement", false)

        var countDownTimerValue = 3
        var thresholdIFLValue = 50
        var maxFramesRepValue = 50

        if (!countDownTimerString.isNullOrEmpty()) {
            countDownTimerValue = countDownTimerString.toInt()
        }

        if (!thresholdIFLString.isNullOrEmpty()) {
            thresholdIFLValue = thresholdIFLString.toInt()
        }

        if (!maxFramesRepString.isNullOrEmpty()) {
            maxFramesRepValue = maxFramesRepString.toInt()
        }

        mDebugModeValue = debugMode
        mResolutionValue = resolution
        mModelValue = model
        mThresholdIFLValue = thresholdIFLValue
        mCountDownTimerValue = countDownTimerValue
        mSyncPreviewAndOverlayValue = syncPreviewAndOverlay
        mDetailedRepInfoValue = detailedRepInfo
        mMaxFramesRepValue = maxFramesRepValue
        mSaveActiveMovement = saveActiveMovement
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onResume() {
        super.onResume()
        updateSettingValues()
    }

    private fun didValuesChange(): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val debugMode = preferences.getBoolean("switch_debugMode", false)
        val detailedRepInfo = preferences.getBoolean("switch_detailedRepInfo", true)
        val countDownTimerString = preferences.getString("edit_text_countdown_timer", "3")
        val thresholdIFLString = preferences.getString("edit_text_threshold_ifl", "50")
        val resolution = preferences.getString("list_resolution", "1")!!.toInt()
        val syncPreviewAndOverlay =
            preferences.getBoolean("switch_sync_preview_and_overlay", true)
        val model = preferences.getString("list_model", "0")!!.toInt()
        val maxFramesRepString = preferences.getString("edit_text_max_frames_rep", "50")
        val saveActiveMovement = preferences.getBoolean("switch_save_active_movement", false)

        var countDownTimerValue = 3
        var thresholdIFLValue = 50
        var maxFramesRepValue = 50

        if (!countDownTimerString.isNullOrEmpty()) {
            countDownTimerValue = countDownTimerString.toInt()
        }

        if (!thresholdIFLString.isNullOrEmpty()) {
            thresholdIFLValue = thresholdIFLString.toInt()
        }

        if (!maxFramesRepString.isNullOrEmpty()) {
            maxFramesRepValue = maxFramesRepString.toInt()
        }

        var didValuesChange = false

        //check if settings changed and change all if one changed
        if (mDebugModeValue != debugMode || mResolutionValue != resolution ||
            mModelValue != model ||
            mThresholdIFLValue != thresholdIFLValue ||
            mCountDownTimerValue != countDownTimerValue ||
            mSyncPreviewAndOverlayValue != syncPreviewAndOverlay ||
            mDetailedRepInfoValue != detailedRepInfo ||
            mMaxFramesRepValue != maxFramesRepValue ||
            mSaveActiveMovement != saveActiveMovement
        ) {
            mDebugModeValue = debugMode
            mResolutionValue = resolution
            mModelValue = model
            mThresholdIFLValue = thresholdIFLValue
            mCountDownTimerValue = countDownTimerValue
            mSyncPreviewAndOverlayValue = syncPreviewAndOverlay
            mDetailedRepInfoValue = detailedRepInfo
            mMaxFramesRepValue = maxFramesRepValue
            mSaveActiveMovement = saveActiveMovement

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
            mDataSingleton.setSetting(
                DataConstants.DETAILED_REP_INFO,
                mDetailedRepInfoValue
            )
            mDataSingleton.setSetting(
                DataConstants.MAX_FRAMES_REP,
                mMaxFramesRepValue
            )
            mDataSingleton.setSetting(
                DataConstants.SAVE_ACTIVE_MOVEMENT,
                mSaveActiveMovement
            )
            setResult(
                Activity.RESULT_OK,
            )
        }
        finish()
    }
}