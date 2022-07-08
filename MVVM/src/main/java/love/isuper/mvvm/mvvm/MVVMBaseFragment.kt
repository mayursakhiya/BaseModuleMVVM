package love.isuper.mvvm.mvvm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import love.isuper.mvvm.base.BaseFragment
import java.lang.reflect.ParameterizedType


abstract class MVVMBaseFragment<VM : BaseViewModel>(private val layoutId: Int) : BaseFragment() {

    val mViewModel: VM by lazy { createViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onPrepare()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutId, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        load()
    }

    private fun load() {
        mViewModel.init(arguments)
        loadState()
        onRegisterLiveListener()
        liveDataObserver()
        init()
    }


    protected open fun onPrepare() {}


    protected open fun getViewModelStoreOwner() : ViewModelStoreOwner {
        return this
    }


    protected open fun createViewModel(): VM {
        val type = javaClass.genericSuperclass
        if (type != null && type is ParameterizedType) {
            val actualTypeArguments = type.actualTypeArguments
            val tClass = actualTypeArguments[0]
            return ViewModelProvider(getViewModelStoreOwner(),
                    ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application))
                    .get(tClass as Class<VM>)
        }
        throw MVVMRuntimeException("ViewModel init error")
    }


    protected open fun onRegisterLiveListener() {}


    protected abstract fun liveDataObserver()


    protected abstract fun init()


    private fun loadState() {
        mViewModel.loadStateLiveData.observe(viewLifecycleOwner) {
            when (it) {
                LoadState.LoadStart -> loadStart()
                LoadState.LoadSuccess -> loadFinish(true)
                LoadState.LoadFail -> loadFinish(false)
            }
        }
        mViewModel.hasMoreStateLiveData.observe(viewLifecycleOwner) {
            when (it) {
                HasMoreState.HasMore -> hasMore()
                HasMoreState.NoMore -> noMore()
            }
        }
    }

    protected open fun loadStart() {}

    protected open fun loadFinish(success: Boolean) {}

    protected open fun hasMore() {}

    protected open fun noMore() {}

}