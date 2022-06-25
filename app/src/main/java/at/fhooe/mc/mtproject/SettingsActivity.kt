package at.fhooe.mc.mtproject

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import at.fhooe.mc.mtproject.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        populateSpinners()

        binding.activitySettingsThresholdEditText.setText(
            intent.getIntExtra("thresholdIFL", 50).toString()
        )

        binding.activitySettingsDebugMode.isChecked = intent.getBooleanExtra("debugMode", false)

        //activate back button on action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
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

        binding.activitySettingsSpinnerResolution.setSelection(intent.getIntExtra("resolution", 1))
        binding.activitySettingsSpinnerModel.setSelection(intent.getIntExtra("model", 0))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra("debugMode", binding.activitySettingsDebugMode.isChecked)
                        .putExtra(
                            "resolution",
                            binding.activitySettingsSpinnerResolution.selectedItemPosition
                        ).putExtra(
                            "model",
                            binding.activitySettingsSpinnerModel.selectedItemPosition
                        ).putExtra(
                            "thresholdIFL",
                            binding.activitySettingsThresholdEditText.text.toString().toInt()
                        )
                )
                finish()
            }
        }
        return true
    }

    override fun onBackPressed() {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra("debugMode", binding.activitySettingsDebugMode.isChecked).putExtra(
                "resolution",
                binding.activitySettingsSpinnerResolution.selectedItemPosition
            ).putExtra(
                "model",
                binding.activitySettingsSpinnerModel.selectedItemPosition
            ).putExtra(
                "thresholdIFL",
                binding.activitySettingsThresholdEditText.text.toString().toInt()
            )
        )
        finish()
    }
}