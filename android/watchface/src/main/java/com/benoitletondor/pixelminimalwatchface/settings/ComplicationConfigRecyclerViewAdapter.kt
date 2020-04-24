/*
 *   Copyright 2020 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.benoitletondor.pixelminimalwatchface.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.wearable.complications.ComplicationHelperActivity
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.ProviderInfoRetriever
import android.support.wearable.complications.ProviderInfoRetriever.OnProviderInfoReceivedCallback
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.benoitletondor.pixelminimalwatchface.BuildConfig
import com.benoitletondor.pixelminimalwatchface.PixelMinimalWatchFace
import com.benoitletondor.pixelminimalwatchface.PixelMinimalWatchFace.Companion.getComplicationId
import com.benoitletondor.pixelminimalwatchface.PixelMinimalWatchFace.Companion.getComplicationIds
import com.benoitletondor.pixelminimalwatchface.PixelMinimalWatchFace.Companion.getSupportedComplicationTypes
import com.benoitletondor.pixelminimalwatchface.R
import com.benoitletondor.pixelminimalwatchface.model.ComplicationColors
import com.benoitletondor.pixelminimalwatchface.model.Storage
import java.util.concurrent.Executors

private const val TYPE_HEADER = 0
private const val TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG = 1
private const val TYPE_COLOR_CONFIG = 2
private const val TYPE_FOOTER = 3
private const val TYPE_BECOME_PREMIUM = 4
private const val TYPE_HOUR_FORMAT = 5
private const val TYPE_SEND_FEEDBACK = 6
private const val TYPE_SHOW_WEAR_OS_LOGO = 7
private const val TYPE_SHOW_COMPLICATIONS_AMBIENT = 8
private const val TYPE_SHOW_FILLED_TIME_AMBIENT = 9
private const val TYPE_SHOW_SECONDS_RING = 10

class ComplicationConfigRecyclerViewAdapter(
    private val context: Context,
    private val storage: Storage,
    private val premiumClickListener: () -> Unit,
    private val hourFormatSelectionListener: (Boolean) -> Unit,
    private val onFeedbackButtonPressed: () -> Unit,
    private val showWearOSButtonListener: (Boolean) -> Unit,
    private val showComplicationsAmbientListener: (Boolean) -> Unit,
    private val showFilledTimeAmbientListener: (Boolean) -> Unit,
    private val showSecondsRingListener: (Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedComplicationLocation: ComplicationLocation? = null

    private val watchFaceComponentName = ComponentName(context, PixelMinimalWatchFace::class.java)
    private val providerInfoRetriever = ProviderInfoRetriever(context, Executors.newCachedThreadPool())
    private var previewAndComplicationsViewHolder: PreviewAndComplicationsViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_HEADER -> return HeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.config_list_header,
                    parent,
                    false
                )
            )
            TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG -> {
                val previewAndComplicationsViewHolder =
                    PreviewAndComplicationsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.config_list_preview_and_complications_item, parent, false)) { location ->
                        selectedComplicationLocation = location

                        val selectedComplicationId = getComplicationId(location)
                        if (selectedComplicationId >= 0) {
                            val supportedTypes = getSupportedComplicationTypes(location)

                            (context as Activity).startActivityForResult(
                                ComplicationHelperActivity.createProviderChooserHelperIntent(
                                    context,
                                    watchFaceComponentName,
                                    selectedComplicationId,
                                    *supportedTypes
                                ),
                                ComplicationConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE
                            )
                        }
                    }

                this.previewAndComplicationsViewHolder = previewAndComplicationsViewHolder
                return previewAndComplicationsViewHolder
            }
            TYPE_COLOR_CONFIG -> return ColorPickerViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.config_list_color_item,
                    parent,
                    false
                )
            )
            TYPE_FOOTER -> return FooterViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.config_list_footer,
                    parent,
                    false
                )
            )
            TYPE_BECOME_PREMIUM -> return PremiumViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.config_list_premium,
                    parent,
                    false
                ),
                premiumClickListener
            )
            TYPE_HOUR_FORMAT -> return HourFormatViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.config_list_hour_format,
                    parent,
                    false
                ),
                hourFormatSelectionListener
            )
            TYPE_SEND_FEEDBACK -> return SendFeedbackViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.config_list_feedback,
                    parent,
                    false
                ),
                onFeedbackButtonPressed
            )
            TYPE_SHOW_WEAR_OS_LOGO -> return ShowWearOSLogoViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.config_list_show_wearos_logo,
                    parent,
                    false
                )) { showWearOSLogo ->
                    showWearOSButtonListener(showWearOSLogo)
                    previewAndComplicationsViewHolder?.showMiddleComplication(!showWearOSLogo)
                }
            TYPE_SHOW_COMPLICATIONS_AMBIENT -> return ShowComplicationsAmbientViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.config_list_show_complications_ambient,
                    parent,
                    false
                ),
                showComplicationsAmbientListener
            )
            TYPE_SHOW_FILLED_TIME_AMBIENT -> return ShowFilledTimeAmbientViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.config_list_show_filled_time_ambient,
                    parent,
                    false
                ),
                showFilledTimeAmbientListener
            )
            TYPE_SHOW_SECONDS_RING -> return ShowSecondsRingViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.config_list_show_seconds_ring,
                    parent,
                    false
                ),
                showSecondsRingListener
            )
        }
        throw IllegalStateException("Unknown option type: $viewType")
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (viewHolder.itemViewType) {
            TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG -> {
                val previewAndComplicationsViewHolder = viewHolder as PreviewAndComplicationsViewHolder

                if( !previewAndComplicationsViewHolder.bound ) {
                    previewAndComplicationsViewHolder.bound = true

                    previewAndComplicationsViewHolder.setDefaultComplicationDrawable()
                    previewAndComplicationsViewHolder.showMiddleComplication(!storage.shouldShowWearOSLogo())
                    initializesColorsAndComplications()
                }
            }
            TYPE_HOUR_FORMAT -> {
                val use24hTimeFormat = storage.getUse24hTimeFormat()
                (viewHolder as HourFormatViewHolder).setHourFormatSwitchChecked(use24hTimeFormat)
            }
            TYPE_SHOW_WEAR_OS_LOGO -> {
                (viewHolder as ShowWearOSLogoViewHolder).apply {
                    setShowWearOSLogoSwitchChecked(storage.shouldShowWearOSLogo())
                    setPremiumTitle(storage.isUserPremium())
                }
            }
            TYPE_SHOW_COMPLICATIONS_AMBIENT -> {
                val showComplicationsAmbient = storage.shouldShowComplicationsInAmbientMode()
                (viewHolder as ShowComplicationsAmbientViewHolder).setShowComplicationsAmbientSwitchChecked(showComplicationsAmbient)
            }
            TYPE_SHOW_FILLED_TIME_AMBIENT -> {
                val showFilledTimeAmbient = storage.shouldShowFilledTimeInAmbientMode()
                (viewHolder as ShowFilledTimeAmbientViewHolder).setShowFilledTimeSwitchChecked(showFilledTimeAmbient)
            }
            TYPE_SHOW_SECONDS_RING -> {
                val showSeconds = storage.shouldShowSecondsRing()
                (viewHolder as ShowSecondsRingViewHolder).setShowSecondsRingSwitchChecked(showSeconds)
            }
        }
    }

    private fun initializesColorsAndComplications() {
        val complicationIds = getComplicationIds()

        providerInfoRetriever.retrieveProviderInfo(
            object : OnProviderInfoReceivedCallback() {
                override fun onProviderInfoReceived(watchFaceComplicationId: Int, complicationProviderInfo: ComplicationProviderInfo?) {

                    previewAndComplicationsViewHolder?.updateComplicationViews(
                        when (watchFaceComplicationId) {
                            getComplicationId(ComplicationLocation.LEFT) -> { ComplicationLocation.LEFT }
                            getComplicationId(ComplicationLocation.MIDDLE) -> { ComplicationLocation.MIDDLE }
                            getComplicationId(ComplicationLocation.BOTTOM) -> { ComplicationLocation.BOTTOM }
                            else -> { ComplicationLocation.RIGHT }
                        },
                        complicationProviderInfo,
                        storage.getComplicationColors()
                    )
                }
            },
            watchFaceComponentName,
            *complicationIds
        )
    }

    override fun getItemViewType(position: Int): Int {
        return if( storage.isUserPremium() ) {
            when (position) {
                0 -> TYPE_HEADER
                1 -> TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG
                2 -> TYPE_COLOR_CONFIG
                3 -> TYPE_SHOW_WEAR_OS_LOGO
                4 -> TYPE_SHOW_COMPLICATIONS_AMBIENT
                5 -> TYPE_HOUR_FORMAT
                6 -> TYPE_SHOW_FILLED_TIME_AMBIENT
                7 -> TYPE_SHOW_SECONDS_RING
                8 -> TYPE_SEND_FEEDBACK
                else -> TYPE_FOOTER
            }
        } else {
            when (position) {
                0 -> TYPE_HEADER
                1 -> TYPE_BECOME_PREMIUM
                2 -> TYPE_SHOW_WEAR_OS_LOGO
                3 -> TYPE_HOUR_FORMAT
                4 -> TYPE_SHOW_FILLED_TIME_AMBIENT
                5 -> TYPE_SHOW_SECONDS_RING
                6 -> TYPE_SEND_FEEDBACK
                else -> TYPE_FOOTER
            }
        }

    }

    override fun getItemCount(): Int {
        return if( storage.isUserPremium() ) {
            10
        } else {
            8
        }
    }

    /** Updates the selected complication id saved earlier with the new information.  */
    fun updateSelectedComplication(complicationProviderInfo: ComplicationProviderInfo?) { // Checks if view is inflated and complication id is valid.
        val selectedComplicationLocation = selectedComplicationLocation

        if ( selectedComplicationLocation != null ) {
            previewAndComplicationsViewHolder?.updateComplicationViews(
                selectedComplicationLocation,
                complicationProviderInfo,
                storage.getComplicationColors()
            )
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        providerInfoRetriever.init()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        onDestroy()
    }

    fun onDestroy() {
        providerInfoRetriever.release()
    }

    fun updatePreviewColors() {
        previewAndComplicationsViewHolder?.updateComplicationsAccentColor(storage.getComplicationColors())
    }
}

enum class ComplicationLocation {
    LEFT, MIDDLE, RIGHT, BOTTOM
}

class ColorPickerViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

    init {
        view.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        val launchIntent = Intent(view.context, ColorSelectionActivity::class.java)
        val activity = view.context as Activity

        activity.startActivityForResult(
            launchIntent,
            ComplicationConfigActivity.UPDATE_COLORS_CONFIG_REQUEST_CODE
        )
    }
}

class PreviewAndComplicationsViewHolder(
    view: View,
    private val listener: (location: ComplicationLocation) -> Unit
) : RecyclerView.ViewHolder(view), View.OnClickListener {
    var bound = false

    private val wearOSLogoImageView: ImageView = view.findViewById(R.id.wear_os_logo_image_view)
    private val leftComplicationBackground: ImageView = view.findViewById(R.id.left_complication_background)
    private val middleComplicationBackground: ImageView = view.findViewById(R.id.middle_complication_background)
    private val rightComplicationBackground: ImageView = view.findViewById(R.id.right_complication_background)
    private val bottomComplicationBackground: ImageView = view.findViewById(R.id.bottom_complication_background)
    private val leftComplication: ImageButton = view.findViewById(R.id.left_complication)
    private val middleComplication: ImageButton = view.findViewById(R.id.middle_complication)
    private val rightComplication: ImageButton = view.findViewById(R.id.right_complication)
    private val bottomComplication: ImageButton = view.findViewById(R.id.bottom_complication)
    private var addComplicationDrawable: Drawable = view.context.getDrawable(R.drawable.add_complication)!!
    private var addedComplicationDrawable: Drawable = view.context.getDrawable(R.drawable.added_complication)!!

    init {
        leftComplication.setOnClickListener(this)
        middleComplication.setOnClickListener(this)
        rightComplication.setOnClickListener(this)
        bottomComplication.setOnClickListener(this)
    }

    fun setDefaultComplicationDrawable() {
        leftComplication.setImageDrawable(addComplicationDrawable)
        middleComplication.setImageDrawable(addComplicationDrawable)
        rightComplication.setImageDrawable(addComplicationDrawable)
        bottomComplication.setImageDrawable(addComplicationDrawable)
    }

    override fun onClick(view: View) {
        when (view) {
            leftComplication -> { listener(ComplicationLocation.LEFT) }
            middleComplication -> { listener(ComplicationLocation.MIDDLE) }
            rightComplication -> { listener(ComplicationLocation.RIGHT) }
            bottomComplication -> { listener(ComplicationLocation.BOTTOM) }
        }
    }

    fun showMiddleComplication(showMiddleComplication: Boolean) {
        middleComplication.visibility = if( showMiddleComplication ) { View.VISIBLE } else { View.GONE }
        middleComplicationBackground.visibility = if( showMiddleComplication ) { View.VISIBLE } else { View.INVISIBLE }
        wearOSLogoImageView.visibility = if( !showMiddleComplication ) { View.VISIBLE } else { View.GONE }
    }

    fun updateComplicationViews(location: ComplicationLocation,
                                complicationProviderInfo: ComplicationProviderInfo?,
                                complicationColors: ComplicationColors) {
        when (location) {
            ComplicationLocation.LEFT -> {
                updateComplicationView(
                    complicationProviderInfo,
                    leftComplication,
                    leftComplicationBackground,
                    complicationColors
                )
            }
            ComplicationLocation.MIDDLE -> {
                updateComplicationView(
                    complicationProviderInfo,
                    middleComplication,
                    middleComplicationBackground,
                    complicationColors
                )
            }
            ComplicationLocation.RIGHT -> {
                updateComplicationView(
                    complicationProviderInfo,
                    rightComplication,
                    rightComplicationBackground,
                    complicationColors
                )
            }
            ComplicationLocation.BOTTOM -> {
                updateComplicationView(
                    complicationProviderInfo,
                    bottomComplication,
                    bottomComplicationBackground,
                    complicationColors
                )
            }
        }
    }

    private fun updateComplicationView(complicationProviderInfo: ComplicationProviderInfo?,
                                       button: ImageButton,
                                       background: ImageView,
                                       complicationColors: ComplicationColors) {
        if (complicationProviderInfo != null) {
            button.setImageIcon(complicationProviderInfo.providerIcon)
            background.setImageDrawable(addedComplicationDrawable)
        } else {
            button.setImageIcon(null)
            background.setImageDrawable(addComplicationDrawable)
        }

        updateComplicationsAccentColor(complicationColors)
    }

    fun updateComplicationsAccentColor(colors: ComplicationColors) {
        if( rightComplication.drawable == addComplicationDrawable ) {
            rightComplication.setColorFilter(Color.WHITE)
        } else {
            rightComplication.setColorFilter(colors.rightColor)
        }

        if( leftComplication.drawable == addComplicationDrawable ) {
            leftComplication.setColorFilter(Color.WHITE)
        } else {
            leftComplication.setColorFilter(colors.leftColor)
        }

        if( middleComplication.drawable == addComplicationDrawable ) {
            middleComplication.setColorFilter(Color.WHITE)
        } else {
            middleComplication.setColorFilter(colors.middleColor)
        }

        if( bottomComplication.drawable == addComplicationDrawable ) {
            bottomComplication.setColorFilter(Color.WHITE)
        } else {
            bottomComplication.setColorFilter(colors.bottomColor)
        }
    }
}

class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)

class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val versionTextView: TextView = view.findViewById(R.id.app_version)

    init {
        versionTextView.text = versionTextView.context.getString(R.string.config_version, BuildConfig.VERSION_NAME)
    }
}

class PremiumViewHolder(view: View,
                        premiumClickListener: () -> Unit) : RecyclerView.ViewHolder(view) {
    private val premiumButton: Button = view.findViewById(R.id.premium_button)

    init {
        premiumButton.setOnClickListener {
            premiumClickListener()
        }
    }
}

class HourFormatViewHolder(view: View,
                           hourFormatClickListener: (Boolean) -> Unit) : RecyclerView.ViewHolder(view) {
    private val hourFormatSwitch: Switch = view as Switch

    init {
        hourFormatSwitch.setOnCheckedChangeListener { _, checked ->
            hourFormatClickListener(checked)
        }
    }

    fun setHourFormatSwitchChecked(checked: Boolean) {
        hourFormatSwitch.isChecked = checked
    }
}

class ShowWearOSLogoViewHolder(view: View,
                               showWearOSLogoClickListener: (Boolean) -> Unit) : RecyclerView.ViewHolder(view) {
    private val wearOSLogoSwitch: Switch = view as Switch

    init {
        wearOSLogoSwitch.setOnCheckedChangeListener { _, checked ->
            showWearOSLogoClickListener(checked)
        }
    }

    fun setShowWearOSLogoSwitchChecked(checked: Boolean) {
        wearOSLogoSwitch.isChecked = checked
    }

    fun setPremiumTitle(userPremium: Boolean) {
        wearOSLogoSwitch.text = itemView.context.getString(if( userPremium ) {
            R.string.config_show_wear_os_logo_premium
        } else {
            R.string.config_show_wear_os_logo
        })
    }
}

class SendFeedbackViewHolder(view: View,
                             onFeedbackButtonPressed: () -> Unit) : RecyclerView.ViewHolder(view) {
    init {
        view.setOnClickListener {
            onFeedbackButtonPressed()
        }
    }
}

class ShowComplicationsAmbientViewHolder(view: View,
                                         showComplicationsAmbientClickListener: (Boolean) -> Unit) : RecyclerView.ViewHolder(view) {
    private val showComplicationsAmbientSwitch: Switch = view as Switch

    init {
        showComplicationsAmbientSwitch.setOnCheckedChangeListener { _, checked ->
            showComplicationsAmbientClickListener(checked)
        }
    }

    fun setShowComplicationsAmbientSwitchChecked(checked: Boolean) {
        showComplicationsAmbientSwitch.isChecked = checked
    }
}

class ShowFilledTimeAmbientViewHolder(view: View,
                                      showFilledTimeClickListener: (Boolean) -> Unit) : RecyclerView.ViewHolder(view) {
    private val showFilledTimeSwitch: Switch = view as Switch

    init {
        showFilledTimeSwitch.setOnCheckedChangeListener { _, checked ->
            showFilledTimeClickListener(!checked)
        }
    }

    fun setShowFilledTimeSwitchChecked(checked: Boolean) {
        showFilledTimeSwitch.isChecked = !checked
    }
}

class ShowSecondsRingViewHolder(view: View,
                                      showSecondsRingClickListener: (Boolean) -> Unit) : RecyclerView.ViewHolder(view) {
    private val showSecondsRingSwitch: Switch = view as Switch

    init {
        showSecondsRingSwitch.setOnCheckedChangeListener { _, checked ->
            showSecondsRingClickListener(checked)
        }
    }

    fun setShowSecondsRingSwitchChecked(checked: Boolean) {
        showSecondsRingSwitch.isChecked = checked
    }
}