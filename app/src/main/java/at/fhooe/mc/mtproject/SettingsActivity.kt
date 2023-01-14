package at.fhooe.mc.mtproject

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import at.fhooe.mc.mtproject.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var mSettingsSingleton: SettingsSingleton

    private var mOriginalDebugModeValue = false
    private var mOriginalResolutionValue = 1
    private var mOriginalModelValue = 0
    private var mOriginalThresholdIFLValue = 50
    private var mOriginalCountDownTimerValue = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSettingsSingleton = SettingsSingleton.getInstance(this)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        populateSpinners()

        //activate back button on action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backButtonPressed()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.activitySettingsThresholdEditText.setText(
            mSettingsSingleton.getSetting(SettingConstants.THRESHOLD_IFL).toString()
        )
        binding.activitySettingsCountdownEditText.setText(
            mSettingsSingleton.getSetting(SettingConstants.COUNTDOWN_TIMER).toString()
        )
        binding.activitySettingsDebugMode.isChecked =
            mSettingsSingleton.getSetting(SettingConstants.DEBUG_MODE) as Boolean
        binding.activitySettingsSpinnerResolution.setSelection(
            mSettingsSingleton.getSetting(
                SettingConstants.RESOLUTION
            ) as Int
        )
        binding.activitySettingsSpinnerModel.setSelection(
            mSettingsSingleton.getSetting(
                SettingConstants.MODEL
            ) as Int
        )
        updateOriginalValues()
    }

    private fun populateSpinners() {
        val adapterResolution = ArrayAdapter.createFromResource(
            this,
            R.array.Resolutions,
            android.R.layout.simple_spinner_dropdown_item
        )
        binding.activitySettingsSpinnerResolution.adapter = adapterResolution

        val adapterModels = ArrayAdapter.createFromResource(
            this,
            R.array.Models,
            android.R.layout.simple_spinner_dropdown_item
        )
        binding.activitySettingsSpinnerModel.adapter = adapterModels
    }

    private fun updateOriginalValues() {
        mOriginalDebugModeValue = binding.activitySettingsDebugMode.isChecked
        mOriginalResolutionValue = binding.activitySettingsSpinnerResolution.selectedItemPosition
        mOriginalModelValue = binding.activitySettingsSpinnerModel.selectedItemPosition
        mOriginalThresholdIFLValue =
            binding.activitySettingsThresholdEditText.text.toString().toInt()
        mOriginalCountDownTimerValue =
            binding.activitySettingsCountdownEditText.text.toString().toInt()
    }

    private fun didValuesChange(): Boolean {
        if (mOriginalDebugModeValue == binding.activitySettingsDebugMode.isChecked &&
            mOriginalResolutionValue == binding.activitySettingsSpinnerResolution.selectedItemPosition &&
            mOriginalModelValue == binding.activitySettingsSpinnerModel.selectedItemPosition &&
            mOriginalThresholdIFLValue == binding.activitySettingsThresholdEditText.text.toString()
                .toInt() &&
            mOriginalCountDownTimerValue == binding.activitySettingsCountdownEditText.text.toString()
                .toInt()
        ) {
            return false
        }
        return true
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
            setResult(Activity.RESULT_CANCELED)
        } else {
            mSettingsSingleton.setSetting(
                SettingConstants.DEBUG_MODE,
                binding.activitySettingsDebugMode.isChecked
            )
            mSettingsSingleton.setSetting(
                SettingConstants.RESOLUTION,
                binding.activitySettingsSpinnerResolution.selectedItemPosition
            )
            mSettingsSingleton.setSetting(
                SettingConstants.MODEL,
                binding.activitySettingsSpinnerModel.selectedItemPosition
            )
            mSettingsSingleton.setSetting(
                SettingConstants.THRESHOLD_IFL,
                binding.activitySettingsThresholdEditText.text.toString().toInt()
            )
            mSettingsSingleton.setSetting(
                SettingConstants.COUNTDOWN_TIMER,
                binding.activitySettingsCountdownEditText.text.toString().toInt()
            )
            setResult(
                Activity.RESULT_OK,
            )
        }
        finish()
    }
}