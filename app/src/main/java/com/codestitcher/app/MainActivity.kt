package com.codestitcher.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.codestitcher.app.databinding.ActivityMainBinding
import com.codestitcher.app.ui.screens.SplashScreen
import com.codestitcher.app.utils.DownloadHelper
import com.codestitcher.app.utils.NetworkHelper
import com.codestitcher.app.webview.CodeStitcherWebChromeClient
import com.codestitcher.app.webview.CodeStitcherWebViewClient
import com.codestitcher.app.webview.WebAppInterface
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var networkHelper: NetworkHelper
    private lateinit var downloadHelper: DownloadHelper
    private lateinit var webChromeClient: CodeStitcherWebChromeClient
    private lateinit var webViewClient: CodeStitcherWebViewClient

    private var backPressedTime: Long = 0L
    private var isOfflineDinoActive = false
    private var isSplashAnimationComplete = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        webChromeClient.onFileChooserResult(uris.toTypedArray())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Zero cold-start flicker: OS splash screen configured to pure black
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()

        networkHelper = NetworkHelper(this)
        downloadHelper = DownloadHelper(this)

        setupNativeComposeSplash()
        setupWebView()
        setupSwipeRefresh()
        setupBackNavigation()
        observeNetworkState()

        // Preload target URL in background during the splash animation
        if (!networkHelper.isCurrentlyConnected()) {
            isOfflineDinoActive = true
            binding.webView.loadUrl("file:///android_asset/dino/index.html")
        } else {
            val targetUrl = intent?.dataString ?: getString(R.string.url_codestitcher_home)
            binding.webView.loadUrl(targetUrl)
        }
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, insets.bottom)
            windowInsets
        }
    }

    /**
     * 100% Native Jetpack Compose GPU Canvas Studio Brand Intro.
     * Locked 120 FPS, tap-to-skip enabled, zero cold-start delay.
     */
    private fun setupNativeComposeSplash() {
        binding.composeSplashView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var showSplash by remember { mutableStateOf(true) }
                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                    },
                    label = "SplashToMainTransition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onSplashFinished = {
                                showSplash = false
                                onNativeSplashFinished()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun onNativeSplashFinished() {
        if (isSplashAnimationComplete) return
        isSplashAnimationComplete = true

        binding.composeSplashView.animate()
            .alpha(0f)
            .setDuration(500L)
            .withEndAction {
                binding.composeSplashView.visibility = View.GONE
            }
            .start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView

        webView.apply {
            setBackgroundColor(Color.BLACK)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true

            // Strictly disable all zoom (no pinch-to-zoom, no double-tap zoom)
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100

            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        // Enable third party cookies for WebAssembly IDE packages
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        // WebApp interface bridge
        webView.addJavascriptInterface(WebAppInterface(this), "CodeStitcherNative")

        // Chrome client
        webChromeClient = CodeStitcherWebChromeClient(
            activity = this,
            customViewContainer = binding.customViewContainer,
            fileChooserLauncher = filePickerLauncher,
            onProgressUpdate = { progress ->
                if (progress in 1..99) {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = progress
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            },
            onTitleReceived = { /* fullscreen mode */ },
            onCustomViewChanged = { isCustomShowing ->
                toggleSystemBars(!isCustomShowing)
            }
        )
        webView.webChromeClient = webChromeClient

        // WebView Client
        webViewClient = CodeStitcherWebViewClient(
            context = this,
            onPageLoadingChanged = { isLoading ->
                if (isLoading) {
                    binding.progressBar.visibility = View.VISIBLE
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            },
            onErrorReceived = { hasError, _ ->
                if (hasError) {
                    isOfflineDinoActive = true
                    binding.webView.loadUrl("file:///android_asset/dino/index.html")
                } else {
                    isOfflineDinoActive = false
                }
            },
            onRetryRequested = {
                retryConnection()
            }
        )
        webView.webViewClient = webViewClient

        // Download Listener
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            downloadHelper.downloadFile(url, userAgent, contentDisposition, mimetype)
        }
    }

    private fun retryConnection() {
        performHapticTick()
        if (networkHelper.isCurrentlyConnected()) {
            isOfflineDinoActive = false
            Toast.makeText(this, "Connecting to CodeStitcher...", Toast.LENGTH_SHORT).show()
            binding.webView.loadUrl(getString(R.string.url_codestitcher_home))
        } else {
            Toast.makeText(this, "Still offline. Keep playing!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.brand_primary,
            R.color.brand_secondary,
            R.color.brand_accent
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            performHapticTick()
            if (networkHelper.isCurrentlyConnected()) {
                isOfflineDinoActive = false
                binding.webView.loadUrl(getString(R.string.url_codestitcher_home))
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        // Prevent swipe-refresh triggering while user is scrolling or playing dino game
        binding.webView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            binding.swipeRefreshLayout.isEnabled = (scrollY == 0 && !isOfflineDinoActive)
        }
    }

    private fun observeNetworkState() {
        lifecycleScope.launch {
            networkHelper.isConnected.collectLatest { isConnected ->
                // Auto-reconnect when internet becomes available while playing Dino
                if (isConnected && isOfflineDinoActive) {
                    isOfflineDinoActive = false
                    Toast.makeText(this@MainActivity, "Internet restored! Loading CodeStitcher...", Toast.LENGTH_SHORT).show()
                    binding.webView.loadUrl(getString(R.string.url_codestitcher_home))
                }
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    webChromeClient.isCustomViewShowing() -> {
                        webChromeClient.onHideCustomView()
                    }
                    !isOfflineDinoActive && binding.webView.canGoBack() -> {
                        binding.webView.goBack()
                    }
                    else -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - backPressedTime < 2000L) {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        } else {
                            backPressedTime = currentTime
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.msg_back_press_again),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        })
    }

    private fun performHapticTick() {
        binding.root.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    private fun toggleSystemBars(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController ?: return
            if (show) {
                controller.show(WindowInsets.Type.systemBars())
            } else {
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (show) {
                View.SYSTEM_UI_FLAG_VISIBLE
            } else {
                (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onStart() {
        super.onStart()
        networkHelper.startMonitoring()
    }

    override fun onStop() {
        super.onStop()
        networkHelper.stopMonitoring()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        binding.webView.destroy()
        super.onDestroy()
    }
}
