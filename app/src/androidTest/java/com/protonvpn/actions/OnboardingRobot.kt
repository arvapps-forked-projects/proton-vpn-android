/*
 *  Copyright (c) 2022 Proton AG
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

package com.protonvpn.actions

import com.protonvpn.android.R
import com.protonvpn.base.BaseRobot
import com.protonvpn.base.BaseVerify
import com.protonvpn.data.Timeouts
import me.proton.test.fusion.Fusion.node
import okhttp3.internal.wait
import java.time.Duration

class OnboardingRobot : BaseRobot() {

    fun closeWelcomeDialog(): OnboardingRobot {
        node.withText(R.string.onboarding_welcome_action).click()
        return this
    }

    fun skipOnboardingPayment(): HomeRobot {
        view.withId(R.id.buttonNotNow).click()
        return HomeRobot()
    }

    class Verify : BaseVerify() {
        fun welcomeScreenIsDisplayed() {
            node.withText(R.string.onboarding_welcome_title).await(Timeouts.LONG ) { assertIsDisplayed() }
        }

        fun onboardingPaymentIdDisplayed() {
            view.withText(R.string.upgrade_plus_title).withTimeout(Timeouts.LONG_MS)
                .checkDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
