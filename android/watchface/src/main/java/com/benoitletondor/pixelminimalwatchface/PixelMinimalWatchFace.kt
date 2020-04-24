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
package com.benoitletondor.pixelminimalwatchface

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import android.support.wearable.complications.rendering.ComplicationDrawable
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.WindowInsets
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.benoitletondor.pixelminimalwatchface.model.ComplicationColors
import com.benoitletondor.pixelminimalwatchface.model.Storage
import com.benoitletondor.pixelminimalwatchface.rating.FeedbackActivity
import com.benoitletondor.pixelminimalwatchface.settings.ComplicationLocation
import com.google.android.gms.wearable.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.max

private const val MISC_NOTIFICATION_CHANNEL_ID = "rating"
private const val DATA_KEY_PREMIUM = "premium"
private const val THREE_DAYS_MS: Long = 1000 * 60 * 60 * 24 * 3
private const val MINIMUM_COMPLICATION_UPDATE_INTERVAL_MS = 1000L

class PixelMinimalWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        val storage = Injection.storage(this)

        val latestKnownVersion = storage.getAppVersion()
        if( BuildConfig.VERSION_CODE > latestKnownVersion ) {
            if( latestKnownVersion > 0 ) {
                onAppUpgrade(latestKnownVersion, BuildConfig.VERSION_CODE)
            }

            storage.setAppVersion(BuildConfig.VERSION_CODE)
        }

        return Engine(this, storage)
    }

    @Suppress("SameParameterValue", "UNUSED_PARAMETER")
    private fun onAppUpgrade(oldVersion: Int, newVersion: Int) {
        // No-op
    }

    private class ComplicationTimeDependentUpdateHandler(private val engine: WeakReference<Engine>,
                                                         private var hasUpdateScheduled: Boolean = false) : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val engine = engine.get() ?: return

            hasUpdateScheduled = false

            if( !engine.isAmbientMode() && engine.isVisible ) {
                engine.invalidate()
            }
        }

        fun cancelUpdate() {
            if( hasUpdateScheduled ) {
                hasUpdateScheduled = false
                removeMessages(MSG_UPDATE_TIME)
            }
        }

        fun scheduleUpdate(delay: Long) {
            if( hasUpdateScheduled ) {
                cancelUpdate()
            }

            hasUpdateScheduled = true
            sendEmptyMessageDelayed(MSG_UPDATE_TIME, delay)
        }

        fun hasUpdateScheduled(): Boolean = hasUpdateScheduled

        companion object {
            private const val MSG_UPDATE_TIME = 0
        }
    }

    inner class Engine(private val service: WatchFaceService,
                       private val storage: Storage
    ) : CanvasWatchFaceService.Engine(), DataClient.OnDataChangedListener, Drawable.Callback {
        private lateinit var calendar: Calendar
        private var registeredTimeZoneReceiver = false

        private val watchFaceDrawer = Injection.watchFaceDrawer()

        private lateinit var complicationsColors: ComplicationColors
        private lateinit var complicationDataSparseArray: SparseArray<ComplicationData>
        private lateinit var complicationDrawableSparseArray: SparseArray<ComplicationDrawable>

        private var muteMode = false
        private var ambient = false
        private var lowBitAmbient = false
        private var burnInProtection = false

        private val timeDependentUpdateHandler = ComplicationTimeDependentUpdateHandler(WeakReference(this))
        private val timeDependentTexts = SparseArray<ComplicationText>()

        private val timeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(
                WatchFaceStyle.Builder(service)
                    .setAcceptsTapEvents(true)
                    .build()
            )

            calendar = Calendar.getInstance()

            watchFaceDrawer.onCreate(service, storage)
            initializeComplications()

            Wearable.getDataClient(service).addListener(this)
        }

        private fun initializeComplications() {
            complicationsColors = storage.getComplicationColors()

            val leftComplicationDrawable = ComplicationDrawable(service)
            val middleComplicationDrawable = ComplicationDrawable(service)
            val rightComplicationDrawable = ComplicationDrawable(service)
            val bottomComplicationDrawable = ComplicationDrawable(service)

            complicationDrawableSparseArray = SparseArray(COMPLICATION_IDS.size)
            complicationDataSparseArray = SparseArray(COMPLICATION_IDS.size)

            complicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable)
            complicationDrawableSparseArray.put(MIDDLE_COMPLICATION_ID, middleComplicationDrawable)
            complicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable)
            complicationDrawableSparseArray.put(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable)

            leftComplicationDrawable.callback = this
            middleComplicationDrawable.callback = this
            rightComplicationDrawable.callback = this
            bottomComplicationDrawable.callback = this

            setActiveComplications(*COMPLICATION_IDS)

            watchFaceDrawer.setComplicationDrawable(LEFT_COMPLICATION_ID, leftComplicationDrawable)
            watchFaceDrawer.setComplicationDrawable(MIDDLE_COMPLICATION_ID, middleComplicationDrawable)
            watchFaceDrawer.setComplicationDrawable(RIGHT_COMPLICATION_ID, rightComplicationDrawable)
            watchFaceDrawer.setComplicationDrawable(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable)
            watchFaceDrawer.onComplicationColorsUpdate(complicationsColors, complicationDataSparseArray)
        }

        override fun onDestroy() {
            unregisterReceiver()
            Wearable.getDataClient(service).removeListener(this)
            timeDependentUpdateHandler.cancelUpdate()

            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)

            lowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            burnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )

            COMPLICATION_IDS.forEach {
                complicationDrawableSparseArray[it].setLowBitAmbient(lowBitAmbient)
            }

            COMPLICATION_IDS.forEach {
                complicationDrawableSparseArray[it].setBurnInProtection(burnInProtection)
            }

            invalidate()
        }

        override fun onApplyWindowInsets(insets: WindowInsets) {
            super.onApplyWindowInsets(insets)

            watchFaceDrawer.onApplyWindowInsets(insets)
        }

        override fun onTimeTick() {
            super.onTimeTick()

            if( !storage.hasRatingBeenDisplayed() &&
                System.currentTimeMillis() - storage.getInstallTimestamp() > THREE_DAYS_MS ) {
                storage.setRatingDisplayed(true)
                sendRatingNotification()
            }

            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            ambient = inAmbientMode

            COMPLICATION_IDS.forEach {
                complicationDrawableSparseArray[it].setInAmbientMode(ambient)
            }

            invalidate()
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            if (muteMode != inMuteMode) {
                muteMode = inMuteMode

                invalidate()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            watchFaceDrawer.onSurfaceChanged(width, height)
        }

        override fun onComplicationDataUpdate(watchFaceComplicationId: Int, data: ComplicationData) {
            super.onComplicationDataUpdate(watchFaceComplicationId, data)

            // Updates correct ComplicationDrawable with updated data.
            val complicationDrawable = complicationDrawableSparseArray.get(watchFaceComplicationId)
            complicationDrawable.setComplicationData(data)

            complicationDataSparseArray.put(watchFaceComplicationId, data)

            watchFaceDrawer.onComplicationDataUpdate(watchFaceComplicationId, complicationDrawable, data, complicationsColors)

            // Update time dependent complication
            val nextShortTextChangeTime = data.shortText?.getNextChangeTime(System.currentTimeMillis())
            if( nextShortTextChangeTime != null && nextShortTextChangeTime < Long.MAX_VALUE ) {
                timeDependentTexts.put(watchFaceComplicationId, data.shortText)
            } else {
                timeDependentTexts.remove(watchFaceComplicationId)
            }

            timeDependentUpdateHandler.cancelUpdate()

            if( !ambient || storage.shouldShowComplicationsInAmbientMode() ) {
                invalidate()
            }
        }

        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TAP -> {
                    COMPLICATION_IDS.forEach { complicationId ->
                        val complicationDrawable: ComplicationDrawable = complicationDrawableSparseArray.get(complicationId)

                        if ( complicationDrawable.onTap(x, y) ) {
                            return
                        }
                    }
                }
            }
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            calendar.timeInMillis = System.currentTimeMillis()

            watchFaceDrawer.draw(
                canvas,
                calendar.time,
                muteMode,
                ambient,
                lowBitAmbient,
                burnInProtection
            )

            if( !ambient && isVisible && !timeDependentUpdateHandler.hasUpdateScheduled() ) {
                val nextUpdateDelay = getNextComplicationUpdateDelay()
                if( nextUpdateDelay != null ) {
                    timeDependentUpdateHandler.scheduleUpdate(nextUpdateDelay)
                }
            }
        }

        @Suppress("SameParameterValue")
        private fun getNextComplicationUpdateDelay(): Long? {
            if( storage.shouldShowSecondsRing() ) {
                return 1000
            }

            var minValue = Long.MAX_VALUE

            COMPLICATION_IDS.forEach { complicationId ->
                val timeDependentText = timeDependentTexts.get(complicationId)
                if( timeDependentText != null ) {
                    val nextTime = timeDependentText.getNextChangeTime(calendar.timeInMillis)
                    if( nextTime < Long.MAX_VALUE ) {
                        val updateDelay = max(MINIMUM_COMPLICATION_UPDATE_INTERVAL_MS, calendar.timeInMillis - nextTime)
                        if( updateDelay < minValue ) {
                            minValue = updateDelay
                        }
                    }
                }
            }

            if( minValue == Long.MAX_VALUE ) {
                return null
            }

            return minValue
        }

        fun isAmbientMode(): Boolean = ambient

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()

                /* Update time zone in case it changed while we weren't visible. */
                calendar.timeZone = TimeZone.getDefault()

                val newComplicationColors = storage.getComplicationColors()
                if( newComplicationColors != complicationsColors ) {
                    complicationsColors = newComplicationColors
                    setComplicationsActiveAndAmbientColors(complicationsColors)
                }

                invalidate()
            } else {
                unregisterReceiver()
            }
        }

        private fun registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            service.registerReceiver(timeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = false
            service.unregisterReceiver(timeZoneReceiver)
        }

        private fun setComplicationsActiveAndAmbientColors(complicationColors: ComplicationColors) {
            watchFaceDrawer.onComplicationColorsUpdate(complicationColors, complicationDataSparseArray)
        }

        override fun onDataChanged(dataEvents: DataEventBuffer) {
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED) {
                    val isPremium = DataMapItem.fromDataItem(event.dataItem).dataMap.getBoolean(DATA_KEY_PREMIUM)
                    storage.setUserPremium(isPremium)

                    if( isPremium ) {
                        Toast.makeText(service, R.string.premium_confirmation, Toast.LENGTH_LONG).show()
                    }

                    invalidate()
                }
            }
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            // No-op
        }

        override fun invalidateDrawable(who: Drawable) {
            if( !ambient || storage.shouldShowComplicationsInAmbientMode() ) {
                invalidate()
            }
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, time: Long) {
            // No-op
        }

        private fun sendRatingNotification() {
            // Create notification channel if needed
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val mChannel = NotificationChannel(MISC_NOTIFICATION_CHANNEL_ID, getString(R.string.misc_notification_channel_name), importance)
                mChannel.description = getString(R.string.misc_notification_channel_description)

                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(mChannel)
            }

            val activityIntent = Intent(service, FeedbackActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(service, 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT)

            val notification = NotificationCompat.Builder(service, MISC_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.rating_notification_title))
                .setContentText(getString(R.string.rating_notification_message))
                .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.rating_notification_message)))
                .addAction(NotificationCompat.Action(R.drawable.ic_feedback, getString(R.string.rating_notification_cta), pendingIntent))
                .setAutoCancel(true)
                .build()


            NotificationManagerCompat.from(service).notify(193828, notification)
        }
    }

    companion object {
        const val LEFT_COMPLICATION_ID = 100
        const val RIGHT_COMPLICATION_ID = 101
        const val MIDDLE_COMPLICATION_ID = 102
        const val BOTTOM_COMPLICATION_ID = 103

        private val COMPLICATION_IDS = intArrayOf(
            LEFT_COMPLICATION_ID,
            MIDDLE_COMPLICATION_ID,
            RIGHT_COMPLICATION_ID,
            BOTTOM_COMPLICATION_ID
        )

        private val COMPLICATION_SUPPORTED_TYPES = arrayOf(
            intArrayOf(
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_SMALL_IMAGE
            ),
            intArrayOf(
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_SMALL_IMAGE
            ),
            intArrayOf(
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_SMALL_IMAGE
            ),
            intArrayOf(
                ComplicationData.TYPE_LONG_TEXT,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SMALL_IMAGE,
                ComplicationData.TYPE_RANGED_VALUE
            )
        )

        fun getComplicationId(complicationLocation: ComplicationLocation): Int {
            return when (complicationLocation) {
                ComplicationLocation.LEFT -> LEFT_COMPLICATION_ID
                ComplicationLocation.MIDDLE -> MIDDLE_COMPLICATION_ID
                ComplicationLocation.RIGHT -> RIGHT_COMPLICATION_ID
                ComplicationLocation.BOTTOM -> BOTTOM_COMPLICATION_ID
            }
        }

        fun getSupportedComplicationTypes(complicationLocation: ComplicationLocation): IntArray {
            return when (complicationLocation) {
                ComplicationLocation.LEFT -> COMPLICATION_SUPPORTED_TYPES[0]
                ComplicationLocation.MIDDLE -> COMPLICATION_SUPPORTED_TYPES[1]
                ComplicationLocation.RIGHT -> COMPLICATION_SUPPORTED_TYPES[2]
                ComplicationLocation.BOTTOM -> COMPLICATION_SUPPORTED_TYPES[3]
            }
        }

        fun getComplicationIds(): IntArray {
            return COMPLICATION_IDS
        }
    }
}


