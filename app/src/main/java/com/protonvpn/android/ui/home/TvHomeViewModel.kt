/*
 * Copyright (c) 2020 Proton Technologies AG
 *
 * This file is part of ProtonVPN.
 *
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.protonvpn.android.ui.home

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.protonvpn.android.R
import com.protonvpn.android.appconfig.AppConfig
import com.protonvpn.android.models.config.UserData
import com.protonvpn.android.models.profiles.Profile
import com.protonvpn.android.models.profiles.ServerWrapper
import com.protonvpn.android.models.vpn.VpnCountry
import com.protonvpn.android.tv.main.TvMapRenderer
import com.protonvpn.android.tv.models.DrawableImage
import com.protonvpn.android.tv.models.Card
import com.protonvpn.android.tv.models.CountryCard
import com.protonvpn.android.tv.models.ProfileCard
import com.protonvpn.android.tv.models.QuickConnectCard
import com.protonvpn.android.tv.models.Title
import com.protonvpn.android.utils.CountryTools
import com.protonvpn.android.utils.ServerManager
import com.protonvpn.android.vpn.RecentsManager
import com.protonvpn.android.vpn.VpnState
import com.protonvpn.android.vpn.VpnStateMonitor
import javax.inject.Inject

class TvHomeViewModel @Inject constructor(
    val appConfig: AppConfig,
    val serverManager: ServerManager,
    val serverListUpdater: ServerListUpdater,
    val vpnStateMonitor: VpnStateMonitor,
    private val recentsManager: RecentsManager,
    val userData: UserData
) : ViewModel() {

    val selectedCountry = MutableLiveData<VpnCountry>()
    val connectedCountryFlag = MutableLiveData<String>()
    val mapRegion = MutableLiveData<TvMapRenderer.MapRegion>()
    val vpnStatus = vpnStateMonitor.vpnStatus.map { it.state }

    init {
        vpnStateMonitor.vpnStatus.observeForever {
            if (it.state == VpnState.Connected) {
                connectedCountryFlag.value = it.server!!.flag
            } else {
                connectedCountryFlag.value = ""
            }
        }
        serverListUpdater.getServersList(null)
    }

    val haveAccessToStreaming get() = userData.isUserPlusOrAbove

    fun showConnectButtons(card: CountryCard) =
        !vpnStateMonitor.isConnectingToCountry(card.vpnCountry.flag)

    fun disconnectText(card: CountryCard) =
        if (!showConnectButtons(card) && vpnStateMonitor.vpnStatus.value?.state?.isEstablishingConnection == true)
            R.string.cancel
        else
            R.string.disconnect

    fun getRecentCardList(context: Context): List<Card> {
        val recentsList = mutableListOf<Card>()
        val quickConnectCard = QuickConnectCard(
            title = Title(
                text = context.getString(if (isConnected()) R.string.disconnect else R.string.quickConnect),
                resId = if (isConnected())
                    R.drawable.ic_notification_disconnected else R.drawable.ic_thunder
            ),
            backgroundImage = DrawableImage(
                resId = quickConnectBackground(context), opacity = if (isConnected()) 0.5f else 1.0f
            )
        )
        recentsList.add(quickConnectCard)
        recentsManager.getRecentConnections().forEach {
            recentsList.add(
                ProfileCard(
                    title = it.name,
                    backgroundImage = CountryTools.getFlagResource(context, it.wrapper.country),
                    profile = it
                )
            )
        }
        return recentsList
    }

    fun disconnect() = vpnStateMonitor.disconnect()

    fun isConnected() = vpnStateMonitor.isConnected

    fun quickConnectBackground(context: Context): Int {
        val server =
            if (isConnected()) vpnStateMonitor.connectingToServer else serverManager.defaultConnection.server
        return CountryTools.getFlagResource(context, server?.flag)
    }

    fun onQuickConnectAction(activity: Activity) {
        if (vpnStateMonitor.isConnected) {
            vpnStateMonitor.disconnect()
        } else {
            vpnStateMonitor.connect(activity, serverManager.defaultConnection)
        }
    }

    fun connect(activity: Activity, card: CountryCard?) {
        val profile = if (card != null) Profile.getTempProfile(
            serverManager.getBestScoreServer(card.vpnCountry), serverManager
        )
        else serverManager.defaultConnection
        vpnStateMonitor.connect(activity, profile)
    }

    fun connect(activity: Activity, card: ProfileCard?) {
        card?.let { vpnStateMonitor.connect(activity, it.profile) }
    }

    fun resetMap() {
        mapRegion.value = TvMapRenderer.MapRegion(0f, 0f, 1f)
    }

    fun isDefaultCountry(vpnCountry: VpnCountry) =
        userData.defaultConnection?.wrapper?.country == vpnCountry.flag

    fun setAsDefaultCountry(checked: Boolean, vpnCountry: VpnCountry) {
        userData.defaultConnection = if (checked) Profile(
            vpnCountry.countryName, "", ServerWrapper.makeFastestForCountry(vpnCountry.flag, serverManager)
        ) else null
    }

    fun streamingServicesIcons(vpnCountry: VpnCountry): List<String>? =
        if (!appConfig.getFeatureFlags().displayTVLogos)
            null
        else serverManager.streamingServices?.let { response ->
            response.filter(userData.userTier, vpnCountry.flag)?.map {
                Uri.parse(response.resourceBaseURL).buildUpon().appendPath(it.iconName).toString()
            }
        }
}