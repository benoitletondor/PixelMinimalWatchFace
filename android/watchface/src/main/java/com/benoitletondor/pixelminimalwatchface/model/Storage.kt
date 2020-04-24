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
package com.benoitletondor.pixelminimalwatchface.model

import android.content.Context
import android.content.SharedPreferences

private const val SHARED_PREFERENCES_NAME = "pixelMinimalSharedPref"

private const val DEFAULT_COMPLICATION_COLOR = -147282
private const val KEY_COMPLICATION_COLORS = "complicationColors"
private const val KEY_USER_PREMIUM = "user_premium"
private const val KEY_USE_24H_TIME_FORMAT = "use24hTimeFormat"
private const val KEY_INSTALL_TIMESTAMP = "installTS"
private const val KEY_RATING_NOTIFICATION_SENT = "ratingNotificationSent"
private const val KEY_APP_VERSION = "appVersion"
private const val KEY_SHOW_WEAR_OS_LOGO = "showWearOSLogo"
private const val KEY_SHOW_COMPLICATIONS_AMBIENT = "showComplicationsAmbient"
private const val KEY_FILLED_TIME_AMBIENT = "filledTimeAmbient"
private const val KEY_SECONDS_RING = "secondsRing"

interface Storage {
    fun getComplicationColors(): ComplicationColors
    fun setComplicationColors(complicationColors: ComplicationColors)
    fun isUserPremium(): Boolean
    fun setUserPremium(premium: Boolean)
    fun setUse24hTimeFormat(use: Boolean)
    fun getUse24hTimeFormat(): Boolean
    fun getInstallTimestamp(): Long
    fun hasRatingBeenDisplayed(): Boolean
    fun setRatingDisplayed(sent: Boolean)
    fun getAppVersion(): Int
    fun setAppVersion(version: Int)
    fun shouldShowWearOSLogo(): Boolean
    fun setShouldShowWearOSLogo(shouldShowWearOSLogo: Boolean)
    fun shouldShowComplicationsInAmbientMode(): Boolean
    fun setShouldShowComplicationsInAmbientMode(show: Boolean)
    fun shouldShowFilledTimeInAmbientMode(): Boolean
    fun setShouldShowFilledTimeInAmbientMode(showFilledTime: Boolean)
    fun shouldShowSecondsRing(): Boolean
    fun setShouldShowSecondsRing(showSecondsRing: Boolean)
}

class StorageImpl : Storage {
    private var initialized: Boolean = false

    private lateinit var appContext: Context
    private lateinit var sharedPreferences: SharedPreferences

    // Those values will be called up to 60 times a minute when not in ambient mode
    // SharedPreferences uses a map so we cache the values to avoid map lookups
    private var isUserPremiumCached = false
    private var cacheIsUserPremium = false
    private var isUse24hFormatCached = false
    private var cacheUse24hFormat = false
    private var shouldShowWearOSLogoCached = false
    private var cacheShouldShowWearOSLogo = false
    private var shouldShowComplicationsInAmbientModeCached = false
    private var cacheShouldShowComplicationsInAmbientMode = false
    private var shouldShowSecondsSettingCached = false
    private var cacheShouldShowSecondsSetting = false

    fun init(context: Context): Storage {
        if( !initialized ) {
            appContext = context.applicationContext
            sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

            if( getInstallTimestamp() < 0 ) {
                sharedPreferences.edit().putLong(KEY_INSTALL_TIMESTAMP, System.currentTimeMillis()).apply()
            }

            initialized = true
        }

        return this
    }

    override fun getComplicationColors(): ComplicationColors {
        val color = sharedPreferences.getInt(
            KEY_COMPLICATION_COLORS,
            DEFAULT_COMPLICATION_COLOR
        )

        if( color == DEFAULT_COMPLICATION_COLOR) {
            return ComplicationColorsProvider.getDefaultComplicationColors(appContext)
        }

        return ComplicationColors(
            color,
            color,
            color,
            color,
            "TODO", // FIXME
            false
        )
    }

    override fun setComplicationColors(complicationColors: ComplicationColors) {
        sharedPreferences.edit().putInt(
            KEY_COMPLICATION_COLORS,
            if( complicationColors.isDefault ) {
                DEFAULT_COMPLICATION_COLOR
            } else { complicationColors.leftColor }
        ).apply()
    }

    override fun isUserPremium(): Boolean {
        if( !isUserPremiumCached ) {
            cacheIsUserPremium = sharedPreferences.getBoolean(KEY_USER_PREMIUM, false)
            isUserPremiumCached = true
        }

        return cacheIsUserPremium
    }

    override fun setUserPremium(premium: Boolean) {
        cacheIsUserPremium = premium
        isUserPremiumCached = true

        sharedPreferences.edit().putBoolean(KEY_USER_PREMIUM, premium).apply()
    }

    override fun setUse24hTimeFormat(use: Boolean) {
        cacheUse24hFormat = use
        isUse24hFormatCached = true

        sharedPreferences.edit().putBoolean(KEY_USE_24H_TIME_FORMAT, use).apply()
    }

    override fun getUse24hTimeFormat(): Boolean {
        if( !isUse24hFormatCached ) {
            cacheUse24hFormat = sharedPreferences.getBoolean(KEY_USE_24H_TIME_FORMAT, true)
            isUse24hFormatCached = true
        }

        return cacheUse24hFormat
    }

    override fun getInstallTimestamp(): Long {
        return sharedPreferences.getLong(KEY_INSTALL_TIMESTAMP, -1)
    }

    override fun hasRatingBeenDisplayed(): Boolean {
        return sharedPreferences.getBoolean(KEY_RATING_NOTIFICATION_SENT, false)
    }

    override fun setRatingDisplayed(sent: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_RATING_NOTIFICATION_SENT, sent).apply()
    }

    override fun getAppVersion(): Int {
        return sharedPreferences.getInt(KEY_APP_VERSION, -1)
    }

    override fun setAppVersion(version: Int) {
        sharedPreferences.edit().putInt(KEY_APP_VERSION, version).apply()
    }

    override fun shouldShowWearOSLogo(): Boolean {
        if( !shouldShowWearOSLogoCached ) {
            cacheShouldShowWearOSLogo = sharedPreferences.getBoolean(KEY_SHOW_WEAR_OS_LOGO, true)
            shouldShowWearOSLogoCached = true
        }

        return cacheShouldShowWearOSLogo
    }

    override fun setShouldShowWearOSLogo(shouldShowWearOSLogo: Boolean) {
        cacheShouldShowWearOSLogo = shouldShowWearOSLogo
        shouldShowWearOSLogoCached = true

        sharedPreferences.edit().putBoolean(KEY_SHOW_WEAR_OS_LOGO, shouldShowWearOSLogo).apply()
    }

    override fun shouldShowComplicationsInAmbientMode(): Boolean {
        if( !shouldShowComplicationsInAmbientModeCached ) {
            cacheShouldShowComplicationsInAmbientMode = sharedPreferences.getBoolean(KEY_SHOW_COMPLICATIONS_AMBIENT, false)
            shouldShowComplicationsInAmbientModeCached = true
        }

        return cacheShouldShowComplicationsInAmbientMode
    }

    override fun setShouldShowComplicationsInAmbientMode(show: Boolean) {
        cacheShouldShowComplicationsInAmbientMode = show
        shouldShowComplicationsInAmbientModeCached = true

        sharedPreferences.edit().putBoolean(KEY_SHOW_COMPLICATIONS_AMBIENT, show).apply()
    }

    override fun shouldShowFilledTimeInAmbientMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_FILLED_TIME_AMBIENT, false)
    }

    override fun setShouldShowFilledTimeInAmbientMode(showFilledTime: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_FILLED_TIME_AMBIENT, showFilledTime).apply()
    }


    override fun shouldShowSecondsRing(): Boolean {
        if( !shouldShowSecondsSettingCached ) {
            cacheShouldShowSecondsSetting = sharedPreferences.getBoolean(KEY_SECONDS_RING, false)
            shouldShowSecondsSettingCached = true
        }

        return cacheShouldShowSecondsSetting
    }

    override fun setShouldShowSecondsRing(showSecondsRing: Boolean) {
        cacheShouldShowSecondsSetting = showSecondsRing
        shouldShowSecondsSettingCached = true

        sharedPreferences.edit().putBoolean(KEY_SECONDS_RING, showSecondsRing).apply()
    }
}