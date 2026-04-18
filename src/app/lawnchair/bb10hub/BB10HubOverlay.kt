package app.lawnchair.bb10hub

import android.content.Context
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.lawnchair.LawnchairLauncher
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlay
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlayCallbacks

/**
 * Wraps BB10HubScreen in a LauncherOverlay so Lawnchair treats it
 * exactly like the Google Feed panel – same swipe gesture, same
 * scroll progress callbacks, everything.
 *
 * Integration: in LawnchairLauncher.kt replace
 *   private val defaultOverlay by unsafeLazy { OverlayCallbackImpl(this) }
 * with
 *   private val defaultOverlay by unsafeLazy { BB10HubOverlay(this) }
 */
class BB10HubOverlay(private val launcher: LawnchairLauncher) : LauncherOverlay {

    private var callbacks: LauncherOverlayCallbacks? = null
    private var hubView: ComposeView? = null

    init {
        HubLedController.setupChannels(launcher)
    }

    // Called by Lawnchair to get the overlay view
    fun createView(): View {
        val view = ComposeView(launcher).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setViewTreeLifecycleOwner(launcher as LifecycleOwner)
            setViewTreeSavedStateRegistryOwner(launcher as SavedStateRegistryOwner)
            setContent { BB10HubScreen() }
        }
        hubView = view
        return view
    }

    // ── LauncherOverlay implementation ──

    override fun onScrollInteractionBegin() {}

    override fun onScrollInteractionEnd() {}

    override fun onScrollChange(progress: Float, rtl: Boolean) {
        // progress: 0.0 = home, 1.0 = hub fully open
        hubView?.alpha = progress
        callbacks?.onOverlayScrollChanged(progress)
    }

    override fun setOverlayCallbacks(callbacks: LauncherOverlayCallbacks?) {
        this.callbacks = callbacks
    }

    fun onStart()   {}
    fun onResume()  {}
    fun onPause()   {}
    fun onStop()    {}
    fun onDestroy() { hubView = null }

    fun reconnect() {}
    fun setEnableFeed(enable: Boolean) {}
}
