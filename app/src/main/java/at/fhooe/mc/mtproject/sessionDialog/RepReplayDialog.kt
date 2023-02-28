package at.fhooe.mc.mtproject.sessionDialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import at.fhooe.mc.mtproject.DetailedRepData
import at.fhooe.mc.mtproject.ExerciseBitmap
import at.fhooe.mc.mtproject.R
import at.fhooe.mc.mtproject.helpers.CameraImageGraphic
import at.fhooe.mc.mtproject.helpers.GraphicOverlay
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView

class RepReplayDialog(
    private val dialogTitle: String,
    private val detailedRepData: DetailedRepData,
    private val context: Context
) : DialogFragment() {
    private lateinit var toolbar: Toolbar
    private lateinit var graphicOverlay: GraphicOverlay
    private var isPlaying = true
    private lateinit var text: TextView
    private lateinit var backward: MaterialCardView
    private lateinit var forward: MaterialCardView
    private lateinit var play: MaterialCardView
    private lateinit var playIcon: ImageView
    private lateinit var slider: SeekBar
    private lateinit var delayText: TextView
    private var isOverlayOn = true
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var dragHandle: ImageView

    private var currentFramePosition = 0
    private var delay = 100L
    private var currentBitmapList = detailedRepData.overlayBitmapList
    private var currentFrameMode = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.dialog_rep_replay, container, false)

        toolbar = view.findViewById(R.id.dialog_rep_detailed_view_toolbar)
        graphicOverlay = view.findViewById(R.id.dialog_rep_detailed_view_graphic_overlay)
        text = view.findViewById(R.id.bottom_sheet_persistent_replay_player_textView_frames)
        delayText = view.findViewById(R.id.bottom_sheet_persistent_replay_player_seekbar_textView)

        backward = view.findViewById(R.id.bottom_sheet_persistent_replay_player_back)
        forward = view.findViewById(R.id.bottom_sheet_persistent_replay_player_forward)
        play = view.findViewById(R.id.bottom_sheet_persistent_replay_player_play)
        playIcon = view.findViewById(R.id.bottom_sheet_persistent_replay_player_play_icon)

        slider = view.findViewById(R.id.bottom_sheet_persistent_replay_player_seekbar)

        bottomSheetBehavior =
            BottomSheetBehavior.from(view.findViewById(R.id.bottom_sheet_persistent_replay_player))

        dragHandle = view.findViewById(R.id.bottom_sheet_persistent_replay_player_handle_icon)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener { dismiss() }
        toolbar.title = dialogTitle
        toolbar.setTitleTextColor(requireContext().getColor(R.color.customTextColor))

        toolbar.inflateMenu(R.menu.dialog_rep_detailed_view_menu)

        val colorText = SpannableString(currentFrameMode)
        colorText.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    context,
                    R.color.customTextColor
                )
            ), 0, colorText.length, 0
        )
        toolbar.menu.getItem(1).titleCondensed = colorText

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.dialog_rep_detailed_view_menu_frameMode -> {
                    isPlaying = false
                    currentFrameMode = when (currentFrameMode) {
                        "ALL" -> "UP"
                        "UP" -> "DOWN"
                        "DOWN" -> "ALL"
                        else -> ""
                    }
                    currentBitmapList = if (isOverlayOn) {
                        detailedRepData.overlayBitmapList
                    } else {
                        detailedRepData.cameraBitmapList
                    }
                    if (currentFrameMode != "ALL") {
                        currentBitmapList =
                            currentBitmapList.filter {
                                it.exerciseClass.contains(
                                    currentFrameMode,
                                    ignoreCase = true
                                )
                            } as ArrayList<ExerciseBitmap>
                    }
                    currentFramePosition = 0

                    val colorTextCurrent = SpannableString(currentFrameMode)
                    colorTextCurrent.setSpan(
                        ForegroundColorSpan(
                            ContextCompat.getColor(
                                context,
                                R.color.customTextColor
                            )
                        ), 0, colorTextCurrent.length, 0
                    )
                    toolbar.menu.getItem(1).titleCondensed = colorTextCurrent

                    changeStartButtonIcon()
                    drawBitmapWithDelay()
                    Toast.makeText(
                        context,
                        "Showing Frames classified as $currentFrameMode",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                R.id.dialog_rep_detailed_view_menu_overlay -> {
                    isOverlayOn = !isOverlayOn
                    currentBitmapList = if (isOverlayOn) {
                        toolbar.menu.getItem(0).setIcon(R.drawable.baseline_person_24)
                        detailedRepData.overlayBitmapList
                    } else {
                        toolbar.menu.getItem(0).setIcon(R.drawable.baseline_person_off_24)
                        detailedRepData.cameraBitmapList
                    }

                    currentFrameMode = "ALL"
                    val colorTextCurrent = SpannableString(currentFrameMode)
                    colorTextCurrent.setSpan(
                        ForegroundColorSpan(
                            ContextCompat.getColor(
                                context,
                                R.color.customTextColor
                            )
                        ), 0, colorTextCurrent.length, 0
                    )
                    toolbar.menu.getItem(1).titleCondensed = colorTextCurrent

                    if (!isPlaying) {
                        drawBitmapWithDelay()
                    }
                    true
                }
                else -> true
            }
        }

        graphicOverlay.setOnClickListener {
            play.performClick()
        }

        slider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                delay = p1.toLong()
                delayText.text = "Frame Delay: ${delay}ms"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        backward.setOnClickListener {
            isPlaying = false
            currentFramePosition--
            changeStartButtonIcon()
            drawBitmapWithDelay()
        }

        forward.setOnClickListener {
            isPlaying = false
            currentFramePosition++
            changeStartButtonIcon()
            drawBitmapWithDelay()
        }

        play.setOnClickListener {
            isPlaying = !isPlaying
            changeStartButtonIcon()
            drawBitmapWithDelay()
        }

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                dragHandle.rotation = -slideOffset * 180
            }
        })

        dragHandle.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun changeStartButtonIcon() {
        if (isPlaying) {
            playIcon.setImageResource(R.drawable.baseline_pause_24)
            play.setCardBackgroundColor(Color.parseColor("#FF818181"))
        } else {
            playIcon.setImageResource(R.drawable.baseline_play_arrow_24)
            play.setCardBackgroundColor(ContextCompat.getColor(context, R.color.start_blue))
        }

    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog

        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog?.window?.setLayout(width, height)

        text.setTextColor(Color.WHITE)
        delayText.setTextColor(Color.WHITE)
        delayText.text = "Frame Delay: ${delay}ms"

        //get the width and height once the layout has been inflated
        graphicOverlay.doOnLayout {
            drawBitmapWithDelay()
        }
    }

    private fun drawBitmapWithDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (isPlaying) {
                currentFramePosition++
            }
            if (currentFramePosition >= currentBitmapList.size) {
                currentFramePosition = 0
            } else if (currentFramePosition < 0) {
                currentFramePosition = currentBitmapList.size - 1
            }

            text.text =
                "Frame: ${currentFramePosition + 1}/${currentBitmapList.size}"

            if (currentBitmapList.size > 0) {
                graphicOverlay.clear()
                graphicOverlay.add(
                    CameraImageGraphic(
                        graphicOverlay,
                        Bitmap.createScaledBitmap(
                            currentBitmapList[currentFramePosition].bitmap,
                            graphicOverlay.width,
                            graphicOverlay.height,
                            false
                        )
                    )
                )
            }

            if (isPlaying) {
                drawBitmapWithDelay()
            }
        }, delay)
    }
}