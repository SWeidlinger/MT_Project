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
    private var mExerciseString = "All"
    private var mModeString = "Endless"
    private var mSessionCount = 0
    private var mCameraSelection = 0
    private var mSyncPreviewAndOverlay = false

    init {
        mDebugMode = getBoolean(SettingConstants.DEBUG_MODE, false)
        mResolution = getInt(SettingConstants.RESOLUTION, 1)
        mModel = getInt(SettingConstants.MODEL, 0)
        mThresholdIFL = getInt(SettingConstants.THRESHOLD_IFL, 50)
        mCountdownTimer = getInt(SettingConstants.COUNTDOWN_TIMER, 3)
        mExerciseString = getString(SettingConstants.EXERCISE_STRING, "All")
        mModeString = getString(SettingConstants.MODE_STRING, "Endless")
        mSessionCount = getInt(SettingConstants.SESSION_COUNT, 0)
        mCameraSelection = getInt(SettingConstants.CAMERA_SELECTION, 0)
        mSyncPreviewAndOverlay = getBoolean(SettingConstants.SYNC_PREVIEW_AND_OVERLAY, false)
        saveState()
    }

    private fun saveState() {
        setBoolean(SettingConstants.DEBUG_MODE, mDebugMode)
        setInt(SettingConstants.RESOLUTION, mResolution)
        setInt(SettingConstants.MODEL, mModel)
        setInt(SettingConstants.THRESHOLD_IFL, mThresholdIFL)
        setInt(SettingConstants.COUNTDOWN_TIMER, mCountdownTimer)
        setString(SettingConstants.EXERCISE_STRING, mExerciseString)
        setString(SettingConstants.MODE_STRING, mModeString)
        setInt(SettingConstants.SESSION_COUNT, mSessionCount)
        setInt(SettingConstants.CAMERA_SELECTION, mCameraSelection)
        setBoolean(SettingConstants.SYNC_PREVIEW_AND_OVERLAY, mSyncPreviewAndOverlay)
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
            SettingConstants.EXERCISE_STRING -> {
                mExerciseString = setString(SettingConstants.EXERCISE_STRING, settingValue as String)
            }
            SettingConstants.MODE_STRING -> {
                mModeString = setString(SettingConstants.MODE_STRING, settingValue as String)
            }
            SettingConstants.SESSION_COUNT -> {
                mSessionCount = setInt(SettingConstants.SESSION_COUNT, settingValue as Int)
            }
            SettingConstants.CAMERA_SELECTION -> {
                mCameraSelection = setInt(SettingConstants.CAMERA_SELECTION, settingValue as Int)
            }
            SettingConstants.SYNC_PREVIEW_AND_OVERLAY -> {
                mSyncPreviewAndOverlay = setBoolean(SettingConstants.SYNC_PREVIEW_AND_OVERLAY, settingValue as Boolean)
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
            SettingConstants.EXERCISE_STRING -> mExerciseString
            SettingConstants.MODE_STRING -> mModeString
            SettingConstants.SESSION_COUNT -> mSessionCount
            SettingConstants.CAMERA_SELECTION -> mCameraSelection
            SettingConstants.SYNC_PREVIEW_AND_OVERLAY -> mSyncPreviewAndOverlay
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

    private fun setString(sharedPreference: String, value: String) : String{
        mSharedPreferences.edit()
            .putString(sharedPreference, value).apply()
        return value
    }

    private fun getBoolean(sharedPreference: String, defaultValue: Boolean): Boolean {
        return mSharedPreferences.getBoolean(sharedPreference, defaultValue)
    }

    private fun getInt(sharedPreference: String, defaultValue: Int): Int {
        return mSharedPreferences.getInt(sharedPreference, defaultValue)
    }

    private fun getString(sharedPreference: String, defaultValue: String): String{
        return mSharedPreferences.getString(sharedPreference, defaultValue) ?: ""
    }
}

object SettingConstants {
    const val SETTINGS_SHARED_PREFERENCES = "SETTINGS_SHARED_PREFERENCES"
    const val DEBUG_MODE = "DEBUG_MODE"
    const val RESOLUTION = "RESOLUTION"
    const val MODEL = "MODEL"
    const val THRESHOLD_IFL = "THRESHOLD_IFL"
    const val COUNTDOWN_TIMER = "COUNTDOWN_TIMER"
    const val EXERCISE_STRING = "EXERCISE_STRING"
    const val MODE_STRING = "MODE_STRING"
    const val SESSION_COUNT = "SESSION_COUNT"
    val SETTINGS_EXERCISE_LIST = arrayListOf("All", "Squat", "Push-Up", "Sit-Up")
    val SETTINGS_MODE_LIST = arrayListOf("Endless", "Time", "Rep")
    const val CAMERA_SELECTION = "CAMERA_SELECTION"
    const val SYNC_PREVIEW_AND_OVERLAY = "SYNC_PREVIEW_AND_OVERLAY"
}