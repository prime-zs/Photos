package com.prime.photos


import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.prime.photos.theme.Material
import com.prime.photos.theme.statusBarDecor
import com.primex.extra.*
import dagger.hilt.android.AndroidEntryPoint


private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install Splash Screen and Play animation when cold start.
        installSplashScreen().let { splashScreen ->
            // Animate entry of content
            // if cold start
            if (savedInstanceState == null)
                splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
                    val splashScreenView = splashScreenViewProvider.view
                    // Create your custom animation.
                    val alpha = ObjectAnimator.ofFloat(
                        splashScreenView,
                        View.ALPHA,
                        1f,
                        0f
                    )
                    alpha.interpolator = AnticipateInterpolator()
                    alpha.duration = 700L

                    // Call SplashScreenView.remove at the end of your custom animation.
                    alpha.doOnEnd { splashScreenViewProvider.remove() }

                    // Run your animation.
                    alpha.start()
                }

        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        //TODO: Remove when done
        trash()

        setContent {
            val permission =
                rememberPermissionState(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

            Material(isDark = isSystemInDarkTheme()) {
                ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
                    CompositionLocalProvider(LocalElevationOverlay provides null) {
                        Crossfade(targetState = permission.hasPermission) { has ->
                            when (has) {
                                true -> Home()
                                else -> PermissionRationale { permission.launchPermissionRequest() }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun Home() {

}


@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit) {
    Frame(
        modifier = Modifier
            .statusBarDecor()
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .padding(Dp.pLarge)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(modifier = Modifier.weight(0.3f))

            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(
                    R.raw.lt_permission
                )
            )

            LottieAnimation(
                composition = composition,
                iterations = Int.MAX_VALUE,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.0f),
            )

            Header(
                text = stringResource(R.string.storage_permission),
                modifier = Modifier
                    .padding(top = Dp.pNormal)
                    .align(Alignment.Start),
                style = Material.typography.h3,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )

            Text(
                text = stringResource(R.string.storage_permission_message),
                style = Material.typography.body1,
                modifier = Modifier
                    .padding(top = Dp.pSmall)
                    .align(Alignment.Start),
                //  fontWeight = FontWeight.SemiBold
                color = LocalContentColor.current.copy(ContentAlpha.medium)
            )

            Spacer(modifier = Modifier.weight(0.3f))

            OutlinedButton(
                onClick = onRequestPermission,
                modifier = Modifier
                    //.padding(top = Dp.pLarge)
                    .size(width = 200.dp, height = 46.dp),
                elevation = null,
                border = BorderStroke(2.dp, Material.colors.primary),
                shape = RoundedCornerShape(50)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = null,
                    modifier = Modifier.padding(end = Dp.pNormal)
                )
                Text(text = "Allow", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.weight(0.3f))
        }
    }
}

private fun MainActivity.trash() {

}