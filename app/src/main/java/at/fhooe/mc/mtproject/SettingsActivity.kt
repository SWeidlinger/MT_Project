package at.fhooe.mc.mtproject

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import at.fhooe.mc.mtproject.databinding.ActivitySettingsBinding

private lateinit var binding: ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        //activate back button on action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            android.R.id.home ->{
                finish()
                overridePendingTransition(R.anim.slide_out_right,R.anim.slide_out_right)
            }
        }
        return true
    }
}