package com.nuvio.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.nuvio.app.core.poster.CustomPosterFallbackInterceptor
import com.nuvio.app.core.ui.NativeProfileSwitcherController
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.core.ui.background.AppBackgroundHost
import com.nuvio.app.core.ui.configurePlatformImageLoader
import com.nuvio.app.core.ui.platformProvidesImageLoader
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.settings.ThemeSettingsRepository
import com.nuvio.app.navigation.AppRoute
import com.nuvio.app.navigation.TabsRoute
import kotlinx.coroutines.delay

private val WelcomeMessages = listOf(
    "Welcome back � ready for your next watch?",
    "Lights down, stream on. Enjoy the show.",
    "Good to see you again! What are we watching today?",
    "Your cinema is ready. Dive back in.",
    "Back for another episode? Let's roll.",
    "Popcorn ready? Pick up right where you left off.",
)

fun disposeRoute(route: AppRoute) {
    disposeRouteResources(route)
}

fun setPlayerParkedInPictureInPicture(parked: Boolean) {
    com.nuvio.app.core.ui.background.AppBackgroundRepository.setPlayerParked(parked)
}

@OptIn(ExperimentalCoilApi::class)
@Composable
@Preview
fun App(
    initialTab: AppScreenTab = AppScreenTab.Home,
    initialRoute: AppRoute = TabsRoute,
    useNativeNavigation: Boolean = false,
    useNativeTabBar: Boolean = false,
    useTabletFloatingTabBar: Boolean = false,
    ownsAppRuntime: Boolean = true,
    bypassAppGate: Boolean = false,
    onNavigate: ((AppRoute, launchSingleTop: Boolean) -> Unit)? = null,
    onGoBack: (() -> Unit)? = null,
    onReplace: ((AppRoute) -> Unit)? = null,
    onActivate: ((AppScreenTab) -> Unit)? = null,
    onAppReady: ((Boolean) -> Unit)? = null,
    onTabTitles: ((home: String, search: String, library: String, profile: String, switchProfile: String, addProfile: String) -> Unit)? = null,
    nativeProfileSwitcherController: NativeProfileSwitcherController? = null,
    appGateController: AppGateController? = null,
) {
    AppEnvironment {
        AppGate(
            initialTab = initialTab,
            initialRoute = initialRoute,
            useNativeNavigation = useNativeNavigation,
            useNativeTabBar = useNativeTabBar,
            useTabletFloatingTabBar = useTabletFloatingTabBar,
            ownsAppRuntime = ownsAppRuntime,
            bypassAppGate = bypassAppGate,
            renderMainContent = true,
            onNavigate = onNavigate,
            onGoBack = onGoBack,
            onReplace = onReplace,
            onActivate = onActivate,
            onAppReady = onAppReady,
            onMainContentMountChanged = null,
            onMainContentVisibleChanged = null,
            onTabTitles = onTabTitles,
            nativeProfileSwitcherController = nativeProfileSwitcherController,
            appGateController = appGateController,
        )
    }
}

@Composable
internal fun AppEnvironment(
    drawAppBackground: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!platformProvidesImageLoader) {
        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .components {
                    add(CustomPosterFallbackInterceptor())
                    add(SvgDecoder.Factory())
                    add(
                        coil3.network.ktor3.KtorNetworkFetcherFactory(
                            cacheStrategy = { coil3.network.cachecontrol.CacheControlCacheStrategy() },
                        ),
                    )
                }
                .configurePlatformImageLoader()
                .build()
        }
    }
    val selectedTheme by remember {
        ThemeSettingsRepository.ensureLoaded()
        ThemeSettingsRepository.selectedTheme
    }.collectAsStateWithLifecycle()
    val amoledEnabled by remember {
        ThemeSettingsRepository.amoledEnabled
    }.collectAsStateWithLifecycle()

    val customThemeColors by ThemeSettingsRepository.customThemeColors.collectAsStateWithLifecycle()

    NuvioTheme(appTheme = selectedTheme, amoled = amoledEnabled, customThemeColors = customThemeColors) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (drawAppBackground) {
                AppBackgroundHost {
                    content()
                }
            } else {
                content()
            }
            WelcomeLaunchBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}

@Composable
private fun WelcomeLaunchBanner(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var openCount by remember { mutableIntStateOf(0) }
    var messageIndex by remember { mutableIntStateOf(-1) }
    var visible by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                openCount += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(openCount) {
        if (openCount > 0) {
            val nextIndex = if (WelcomeMessages.size <= 1) {
                0
            } else {
                var candidate = WelcomeMessages.indices.random()
                if (candidate == messageIndex) {
                    candidate = (messageIndex + 1) % WelcomeMessages.size
                }
                candidate
            }
            messageIndex = nextIndex
            visible = true
            delay(3500L)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible && messageIndex in WelcomeMessages.indices,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        val palette = MaterialTheme.themePalette
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(palette.backgroundElevated.copy(alpha = 0.94f))
                .border(1.dp, palette.secondary.copy(alpha = 0.65f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(palette.secondary),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = WelcomeMessages.getOrElse(messageIndex) { WelcomeMessages.first() },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
