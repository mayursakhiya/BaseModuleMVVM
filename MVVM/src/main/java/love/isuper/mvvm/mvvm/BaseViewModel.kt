package love.isuper.mvvm.mvvm

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


abstract class BaseViewModel : ViewModel() {

    val loadStateLiveData = MutableLiveData<LoadState>()
    val hasMoreStateLiveData = MutableLiveData<HasMoreState>()

    open fun init(arguments: Bundle?) {}


    protected fun loadStart() {
        loadStateLiveData.value = LoadState.LoadStart
    }


    protected fun loadFinish(success: Boolean) {
        if (success) {
            loadStateLiveData.setValue(LoadState.LoadSuccess)
        } else {
            loadStateLiveData.setValue(LoadState.LoadFail)
        }
    }


    protected fun hasMore(hasMore : Boolean){
        if (hasMore) {
            hasMoreStateLiveData.value = HasMoreState.HasMore
        } else {
            hasMoreStateLiveData.value = HasMoreState.NoMore
        }
    }

    override fun onCleared() {
        Log.v("BaseViewModel", this.javaClass.name + this + " onCleared()")
        super.onCleared()
    }

}