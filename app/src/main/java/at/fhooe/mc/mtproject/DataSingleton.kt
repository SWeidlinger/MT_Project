package at.fhooe.mc.mtproject

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class DataSingleton private constructor(context: Context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private lateinit var instance: DataSingleton

        fun getInstance(context: Context): DataSingleton {
            synchronized(this) {
                if (!::instance.isInitialized) {
                    instance = DataSingleton(context)
                }
                return instance
            }
        }
    }

    private val mSharedPreferences: SharedPreferences = context.getSharedPreferences(
        DataConstants.SETTINGS_SHARED_PREFERENCES,
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
    private var mSyncPreviewAndOverlay = true
    private var mDetailedRepInfo = true
    private var mMaxFramesRep = 50
    private var mSaveActiveMovement = false

    var mRepListSquats: ArrayList<DetailedRepData> = arrayListOf()
    var mRepListPushUps: ArrayList<DetailedRepData> = arrayListOf()
    var mRepListSitUps: ArrayList<DetailedRepData> = arrayListOf()

    init {
        mDebugMode = getBoolean(DataConstants.DEBUG_MODE, false)
        mResolution = getInt(DataConstants.RESOLUTION, 1)
        mModel = getInt(DataConstants.MODEL, 0)
        mThresholdIFL = getInt(DataConstants.THRESHOLD_IFL, 50)
        mCountdownTimer = getInt(DataConstants.COUNTDOWN_TIMER, 3)
        mExerciseString = getString(DataConstants.EXERCISE_STRING, "All")
        mModeString = getString(DataConstants.MODE_STRING, "Endless")
        mSessionCount = getInt(DataConstants.SESSION_COUNT, 0)
        mCameraSelection = getInt(DataConstants.CAMERA_SELECTION, 0)
        mSyncPreviewAndOverlay = getBoolean(DataConstants.SYNC_PREVIEW_AND_OVERLAY, true)
        mDetailedRepInfo = getBoolean(DataConstants.DETAILED_REP_INFO, true)
        mMaxFramesRep = getInt(DataConstants.MAX_FRAMES_REP, 50)
        mSaveActiveMovement = getBoolean(DataConstants.SAVE_ACTIVE_MOVEMENT, false)
        saveState()
    }

    private fun saveState() {
        setBoolean(DataConstants.DEBUG_MODE, mDebugMode)
        setInt(DataConstants.RESOLUTION, mResolution)
        setInt(DataConstants.MODEL, mModel)
        setInt(DataConstants.THRESHOLD_IFL, mThresholdIFL)
        setInt(DataConstants.COUNTDOWN_TIMER, mCountdownTimer)
        setString(DataConstants.EXERCISE_STRING, mExerciseString)
        setString(DataConstants.MODE_STRING, mModeString)
        setInt(DataConstants.SESSION_COUNT, mSessionCount)
        setInt(DataConstants.CAMERA_SELECTION, mCameraSelection)
        setBoolean(DataConstants.SYNC_PREVIEW_AND_OVERLAY, mSyncPreviewAndOverlay)
        setBoolean(DataConstants.DETAILED_REP_INFO, mDetailedRepInfo)
        setInt(DataConstants.MAX_FRAMES_REP, mMaxFramesRep)
        setBoolean(DataConstants.SAVE_ACTIVE_MOVEMENT, mSaveActiveMovement)
    }

    fun setSetting(settingName: String, settingValue: Any) {
        when (settingName) {
            DataConstants.DEBUG_MODE -> {
                mDebugMode = setBoolean(DataConstants.DEBUG_MODE, settingValue as Boolean)
            }
            DataConstants.RESOLUTION -> {
                mResolution = setInt(DataConstants.RESOLUTION, settingValue as Int)
            }
            DataConstants.MODEL -> {
                mModel = setInt(DataConstants.MODEL, settingValue as Int)
            }
            DataConstants.THRESHOLD_IFL -> {
                mThresholdIFL = setInt(DataConstants.THRESHOLD_IFL, settingValue as Int)
            }
            DataConstants.COUNTDOWN_TIMER -> {
                mCountdownTimer = setInt(DataConstants.COUNTDOWN_TIMER, settingValue as Int)
            }
            DataConstants.EXERCISE_STRING -> {
                mExerciseString = setString(DataConstants.EXERCISE_STRING, settingValue as String)
            }
            DataConstants.MODE_STRING -> {
                mModeString = setString(DataConstants.MODE_STRING, settingValue as String)
            }
            DataConstants.SESSION_COUNT -> {
                mSessionCount = setInt(DataConstants.SESSION_COUNT, settingValue as Int)
            }
            DataConstants.CAMERA_SELECTION -> {
                mCameraSelection = setInt(DataConstants.CAMERA_SELECTION, settingValue as Int)
            }
            DataConstants.SYNC_PREVIEW_AND_OVERLAY -> {
                mSyncPreviewAndOverlay =
                    setBoolean(DataConstants.SYNC_PREVIEW_AND_OVERLAY, settingValue as Boolean)
            }
            DataConstants.DETAILED_REP_INFO -> {
                mDetailedRepInfo =
                    setBoolean(DataConstants.DETAILED_REP_INFO, settingValue as Boolean)
            }
            DataConstants.MAX_FRAMES_REP -> {
                mMaxFramesRep =
                    setInt(DataConstants.MAX_FRAMES_REP, settingValue as Int)
            }
            DataConstants.SAVE_ACTIVE_MOVEMENT -> {
                mSaveActiveMovement =
                    setBoolean(DataConstants.SAVE_ACTIVE_MOVEMENT, settingValue as Boolean)
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
            DataConstants.DEBUG_MODE -> mDebugMode
            DataConstants.RESOLUTION -> mResolution
            DataConstants.MODEL -> mModel
            DataConstants.THRESHOLD_IFL -> mThresholdIFL
            DataConstants.COUNTDOWN_TIMER -> mCountdownTimer
            DataConstants.EXERCISE_STRING -> mExerciseString
            DataConstants.MODE_STRING -> mModeString
            DataConstants.SESSION_COUNT -> mSessionCount
            DataConstants.CAMERA_SELECTION -> mCameraSelection
            DataConstants.SYNC_PREVIEW_AND_OVERLAY -> mSyncPreviewAndOverlay
            DataConstants.DETAILED_REP_INFO -> mDetailedRepInfo
            DataConstants.MAX_FRAMES_REP -> mMaxFramesRep
            DataConstants.SAVE_ACTIVE_MOVEMENT -> mSaveActiveMovement
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

    private fun setString(sharedPreference: String, value: String): String {
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

    private fun getString(sharedPreference: String, defaultValue: String): String {
        return mSharedPreferences.getString(sharedPreference, defaultValue) ?: ""
    }
}

object DataConstants {
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
    const val DETAILED_REP_INFO = "DETAILED_REP_INFO"
    const val MAX_FRAMES_REP = "MAX_FRAMES_REP"
    const val SAVE_ACTIVE_MOVEMENT = "SAVE_ACTIVE_MOVEMENT"
}