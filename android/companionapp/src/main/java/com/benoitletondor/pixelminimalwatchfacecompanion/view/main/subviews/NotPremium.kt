package com.benoitletondor.pixelminimalwatchfacecompanion.view.main.subviews

import android.app.Activity
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.benoitletondor.pixelminimalwatchfacecompanion.R
import com.benoitletondor.pixelminimalwatchfacecompanion.sync.Sync
import com.benoitletondor.pixelminimalwatchfacecompanion.ui.AppMaterialTheme
import com.benoitletondor.pixelminimalwatchfacecompanion.ui.whiteButtonColors
import com.benoitletondor.pixelminimalwatchfacecompanion.view.main.MainViewModel
import me.relex.circleindicator.CircleIndicator
import java.util.*

@Composable
fun NotPremium(state: MainViewModel.State.NotPremium, viewModel: MainViewModel) {
    val context = LocalContext.current

    NotPremiumLayout(
        canInstallApp = state.appInstalledStatus is MainViewModel.AppInstalledStatus.Result && state.appInstalledStatus.wearableStatus == Sync.WearableStatus.AvailableAppNotInstalled,
        watchFaceInstallButtonPressed = {
            viewModel.onInstallWatchFaceButtonPressed()
        },
        becomePremiumButtonPressed = {
            viewModel.launchPremiumBuyFlow(context as Activity)
        },
        redeemPromoCodeButtonPressed = {
            viewModel.onRedeemPromoCodeButtonPressed()
        },
    )
}

@Composable
private fun NotPremiumLayout(
    showCarousel: Boolean = true,
    canInstallApp: Boolean,
    watchFaceInstallButtonPressed: () -> Unit,
    becomePremiumButtonPressed: () -> Unit,
    redeemPromoCodeButtonPressed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        if (showCarousel) {
            AndroidView(
                factory = { context ->
                    val layout = LayoutInflater.from(context).inflate(R.layout.not_premium_view_pager, null)
                    val pager = layout.findViewById<ViewPager>(R.id.not_premium_view_pager)
                    val indicator = layout.findViewById<CircleIndicator>(R.id.not_premium_view_pager_indicator)
                    pager.adapter = object : FragmentPagerAdapter((context as AppCompatActivity).supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

                        override fun getItem(position: Int): Fragment = Fragment(when(position) {
                            0 -> R.layout.fragment_premium_1
                            1 -> R.layout.fragment_premium_2
                            2 -> R.layout.fragment_premium_3
                            else -> throw IllegalStateException("invalid position: $position")
                        })

                        override fun getCount(): Int = 3
                    }

                    indicator.setViewPager(pager)
                    layout
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp),
            )
        } else {
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .background(color = Color.Gray)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (canInstallApp) {
            Button(
                onClick = watchFaceInstallButtonPressed,
                colors = whiteButtonColors(),
            ) {
                Text(text = stringResource(R.string.premium_install_cta).toUpperCase(Locale.getDefault()))
            }
        } else {
            Text(
                text = stringResource(R.string.not_premium_install),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.onBackground,
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.not_premium_status),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.onBackground,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = becomePremiumButtonPressed) {
            Text(text = stringResource(R.string.premium_cta).toUpperCase(Locale.getDefault()))
        }
        Text(
            text = stringResource(R.string.not_premium_status_2),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colors.onBackground,
        )

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(onClick = redeemPromoCodeButtonPressed) {
            Text(text = stringResource(R.string.promocode_cta).toUpperCase(Locale.getDefault()))
        }
    }

}

@Composable
@Preview(showSystemUi = true, name = "Can install app")
private fun PreviewCanInstallApp() {
    Preview(true)
}

@Composable
@Preview(showSystemUi = true, name = "Can't install app")
private fun PreviewCantInstallApp() {
    Preview(false)
}

@Composable
private fun Preview(canInstallApp: Boolean) {
    AppMaterialTheme {
        NotPremiumLayout(
            showCarousel = false,
            canInstallApp = canInstallApp,
            watchFaceInstallButtonPressed = {},
            becomePremiumButtonPressed = {},
            redeemPromoCodeButtonPressed = {},
        )
    }
}