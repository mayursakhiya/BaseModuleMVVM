package love.isuper.app

import androidx.lifecycle.MutableLiveData
import love.isuper.mvvm.mvvm.BaseViewModel


class MainViewModel : BaseViewModel() {

    val modelLiveData = MutableLiveData<String>()

    fun getModel() {
        modelLiveData.value = "Hello!!!"
    }

}