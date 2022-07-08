package love.isuper.mvvm.mvvm

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import love.isuper.mvvm.base.BaseActivity
import java.lang.reflect.ParameterizedType


abstract class MVVMBaseActivity<VM : BaseViewModel>(private val layoutId: Int) : BaseActivity() {

    val mViewModel: VM by lazy { createViewModel() }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onPrepare()
        setContentView(layoutId)
        mViewModel.init(if (intent != null) intent.extras else null)
        loadState()
        onRegisterLiveListener()
        liveDataObserver()
        init()
    }

    protected open fun onPrepare() {}


    protected open fun getViewModelStoreOwner(): ViewModelStoreOwner {
        return this
    }


    protected open fun createViewModel(): VM {
        val type = javaClass.genericSuperclass
        if (type != null && type is ParameterizedType) {
            val actualTypeArguments = type.actualTypeArguments
            val tClass = actualTypeArguments[0]
            return ViewModelProvider(getViewModelStoreOwner(),
                    ViewModelProvider.AndroidViewModelFactory.getInstance(application))
                    .get(tClass as Class<VM>)
        }
        throw MVVMRuntimeException("ViewModel init error")
    }


    protected open fun onRegisterLiveListener() {}


    protected abstract fun liveDataObserver()


    private fun loadState() {
        mViewModel.loadStateLiveData.observe(this) {
            when (it) {
                LoadState.LoadStart -> loadStart()
                LoadState.LoadSuccess -> loadFinish(true)
                LoadState.LoadFail -> loadFinish(false)
            }
        }
        mViewModel.hasMoreStateLiveData.observe(this) {
            when (it) {
                HasMoreState.HasMore -> hasMore()
                HasMoreState.NoMore -> noMore()
            }
        }
    }


    protected abstract fun init()

    protected open fun loadStart() {}

    protected open fun loadFinish(success: Boolean) {}

    protected open fun hasMore() {}

    protected open fun noMore() {}

}