package at.fhooe.mc.mtproject

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SettingsSingleton private constructor(context: Context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private lateinit var instance: SettingsSingleton

        fun getInstance(context: Context): SettingsSingleton {
            synchronized(this) {
                if (!::instance.isInitialized) {
                    instance = SettingsSingleton(context)
                }
                return instance
            }
        }
    }

    private val mSharedPreferences: SharedPreferences = context.getSharedPreferences(
        SettingConstants.SETTINGS_SHARED_PREFERENCES,
        Context.MODE_PRIVATE
    )
    private var mDebugMode = false
    private var mResolution = 1
    private var mModel = 0
    private var mThresholdIFL = 50
    private var mCountdownTimer = 3

    init {
        mDebugMode = getBoolean(SettingConstants.DEBUG_MODE, false)
        mResolution = getInt(SettingConstants.RESOLUTION, 1)
        mModel = getInt(SettingConstants.MODEL, 0)
        mThresholdIFL = getInt(SettingConstants.THRESHOLD_IFL, 50)
        mCountdownTimer = getInt(SettingConstants.COUNTDOWN_TIMER, 3)
        saveState()
    }

    private fun saveState() {
        setBoolean(SettingConstants.DEBUG_MODE, mDebugMode)
        setInt(SettingConstants.RESOLUTION, mResolution)
        setInt(SettingConstants.MODEL, mModel)
        setInt(SettingConstants.THRESHOLD_IFL, mThresholdIFL)
        setInt(SettingConstants.COUNTDOWN_TIMER, mCountdownTimer)
    }

    fun setSetting(settingName: String, settingValue: Any) {
        when (settingName) {
            SettingConstants.DEBUG_MODE -> {
                mDebugMode = setBoolean(SettingConstants.DEBUG_MODE, settingValue as Boolean)
            }
            SettingConstants.RESOLUTION -> {
                mResolution = setInt(SettingConstants.RESOLUTION, settingValue as Int)
            }
            SettingConstants.MODEL -> {
                mModel = setInt(SettingConstants.MODEL, settingValue as Int)
            }
            SettingConstants.THRESHOLD_IFL -> {
                mThresholdIFL = setInt(SettingConstants.THRESHOLD_IFL, settingValue as Int)
            }
            SettingConstants.COUNTDOWN_TIMER -> {
                mCountdownTimer = setInt(SettingConstants.COUNTDOWN_TIMER, settingValue as Int)
            }
            else -> {
                Log.e(
                    "invalidSettingName",
                    "This setting with the name $settingName is not available!"
                )
            }
        }
    }

    fun getSetting(settingName: String): Any? {
        return when (settingName) {
            SettingConstants.DEBUG_MODE -> mDebugMode
            SettingConstants.RESOLUTION -> mResolution
            SettingConstants.MODEL -> mModel
            SettingConstants.THRESHOLD_IFL -> mThresholdIFL
            SettingConstants.COUNTDOWN_TIMER -> mCountdownTimer
            else -> {
                Log.e(
                    "invalidSettingName",
                    "This setting with the name $settingName is not available!"
                )
                null
            }
        }
    }

    private fun setBoolean(sharedPreference: String, value: Boolean): Boolean {
        mSharedPreferences.edit()
            .putBoolean(sharedPreference, value).apply()
        return value
    }

    private fun setInt(sharedPreference: String, value: Int): Int {
        mSharedPreferences.edit()
            .putInt(sharedPreference, value).apply()
        return value
    }

    private fun getBoolean(sharedPreference: String, defaultValue: Boolean): Boolean {
        return mSharedPreferences.getBoolean(sharedPreference, defaultValue)
    }

    private fun getInt(sharedPreference: String, defaultValue: Int): Int {
        return mSharedPreferences.getInt(sharedPreference, defaultValue)
    }
}

object SettingConstants {
    const val SETTINGS_SHARED_PREFERENCES = "SETTINGS_SHARED_PREFERENCES"
    const val DEBUG_MODE = "DEBUG_MODE"
    const val RESOLUTION = "RESOLUTION"
    const val MODEL = "MODEL"
    const val THRESHOLD_IFL = "THRESHOLD_IFL"
    const val COUNTDOWN_TIMER = "COUNTDOWN_TIMER"
}