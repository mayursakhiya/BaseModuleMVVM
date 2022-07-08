package love.isuper.mvvm.extensions

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty



/**
 * Activity
 */
@JvmName("viewBindingActivity")
inline fun <V : ViewBinding> ComponentActivity.viewBinding(
    crossinline viewBinder: (View) -> V,
    crossinline viewProvider: (ComponentActivity) -> View = ::findRootView
): ViewBindingProperty<ComponentActivity, V> =
    ActivityViewBindingProperty { activity: ComponentActivity ->
        viewBinder(viewProvider(activity))
    }


/**
 * Fragment
 */
@JvmName("viewBindingFragment")
inline fun <F : Fragment, V : ViewBinding> Fragment.viewBinding(
    crossinline viewBinder: (View) -> V,
    crossinline viewProvider: (F) -> View = Fragment::requireView
): ViewBindingProperty<F, V> = when (this) {
    is DialogFragment -> DialogFragmentViewBindingProperty { fragment: F ->
        viewBinder(viewProvider(fragment))
    }
    else -> FragmentViewBindingProperty { fragment: F ->
        viewBinder(viewProvider(fragment))
    }
}

/**
 * ViewBindingProperty
 */
private const val TAG = "ViewBindingProperty"

interface ViewBindingProperty<in R : Any, out V : ViewBinding> : ReadOnlyProperty<R, V?> {
    @MainThread
    fun clear()
}

class CustomViewBindingProperty<in R : Any, out V : ViewBinding>(
    private val viewBinder: (R) -> V
) : ViewBindingProperty<R, V> {

    private var viewBinding: V? = null

    @MainThread
    override fun getValue(thisRef: R, property: KProperty<*>): V {
        viewBinding?.let { return it }

        return viewBinder(thisRef).also {
            this.viewBinding = it
        }
    }

    @MainThread
    override fun clear() {
        viewBinding = null
    }
}

abstract class LifecycleViewBindingProperty<in R : Any, out V : ViewBinding>(
    private val viewBinder: (R) -> V
) : ViewBindingProperty<R, V> {

    private var viewBinding: V? = null

    protected abstract fun getLifecycle(thisRef: R): Lifecycle

    @MainThread
    override fun getValue(thisRef: R, property: KProperty<*>): V? {
        // Already bound
        viewBinding?.let { return it }

        val lifecycle = getLifecycle(thisRef)

        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            Log.d(
                TAG, "Access to viewBinding after Lifecycle is destroyed or hasn't created yet. " +
                        "The instance of viewBinding will be not cached."
            )
            // We can access to ViewBinding after Fragment.onDestroyView(), but don't save it to prevent memory leak
        } else {
            try {
                val viewBinding = viewBinder(thisRef)
                lifecycle.addObserver(ClearOnDestroyLifecycleObserver(this))
                this.viewBinding = viewBinding
            } catch (e: IllegalStateException) {
                Log.e(TAG, "did not return a View from onCreateView() or this was called before onCreateView()")
            }
        }
        return viewBinding
    }

    @MainThread
    override fun clear() {
        Log.d(TAG, "clear---${viewBinding}")
        viewBinding = null
    }

    private class ClearOnDestroyLifecycleObserver(
        private val property: LifecycleViewBindingProperty<*, *>
    ) : LifecycleObserver {

        private companion object {
            private val mainHandler = Handler(Looper.getMainLooper())
        }

        @MainThread
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy(owner: LifecycleOwner) {
            mainHandler.post { property.clear() }
        }
    }
}

class FragmentViewBindingProperty<in F : Fragment, out V : ViewBinding>(
    viewBinder: (F) -> V
) : LifecycleViewBindingProperty<F, V>(viewBinder) {

    override fun getLifecycle(thisRef: F): Lifecycle {
        try {
            return thisRef.viewLifecycleOwner.lifecycle
        } catch (ignored: IllegalStateException) {
            Log.e(
                TAG,
                "IllegalStateException: Fragment doesn't have view associated with it or the view has been destroyed"
            )
            return thisRef.lifecycle
            //error("Fragment doesn't have view associated with it or the view has been destroyed")
        }
    }
}

class DialogFragmentViewBindingProperty<in F : Fragment, out V : ViewBinding>(
    viewBinder: (F) -> V
) : LifecycleViewBindingProperty<F, V>(viewBinder) {
    override fun getLifecycle(thisRef: F): Lifecycle {
        return if (thisRef is DialogFragment && thisRef.showsDialog) {
            thisRef.lifecycle
        } else {
            try {
                thisRef.viewLifecycleOwner.lifecycle
            } catch (ignored: IllegalStateException) {
                return thisRef.lifecycle
                //error("Fragment doesn't have view associated with it or the view has been destroyed")
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
class ActivityViewBindingProperty<in A : ComponentActivity, out V : ViewBinding>(
    viewBinder: (A) -> V
) : LifecycleViewBindingProperty<A, V>(viewBinder) {

    override fun getLifecycle(thisRef: A): Lifecycle {
        return thisRef.lifecycle
    }
}

/**
 * Utility to find root view for ViewBinding in Activity
 */
fun findRootView(activity: Activity): View {
    val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
    checkNotNull(contentView) { "Activity has no content view" }
    return when (contentView.childCount) {
        1 -> contentView.getChildAt(0)
        0 -> error("Content view has no children. Provide root view explicitly")
        else -> error("More than one child view found in Activity content view")
    }
}