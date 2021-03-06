package com.lwjlol.ktx

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*


/**
 *
 * 为 Activity/Fragment 创建一个 ViewModel
 * 使用方法：
 * class XXActivity{
 *  val viewModel:XXViewModel by duViewModel()
 *
 * }
 *
 * class  XXFragment{
 *  val viewModel:XXViewModel by duViewModel()
 * }
 *
 */
inline fun <T, reified VM : ViewModel> T.viewModel(
    crossinline factory: () -> ViewModelProvider.Factory? = { null },
    crossinline key: () -> String? = { null }
): Lazy<VM> where T : ViewModelStoreOwner, T : LifecycleOwner = ViewModelLifecycleAwareLazy(this) {
    createViewModel(this, factory, key)
}

inline fun <reified VM : ViewModel> Fragment.activityViewModel(
    crossinline factory: () -> ViewModelProvider.Factory? = { null },
    crossinline key: () -> String? = { null }
): Lazy<VM> = ViewModelLifecycleAwareLazy(this) {
    createViewModel(requireActivity(), factory, key)
}

/**
 *
 * 在父 fragment 寻找一个现有的 [ViewModel] 找不到就在父 Fragment 里创建一个
 *
 * 只能在子 fragment 里使用
 *
 * @param factory
 * @param keyFactory
 * @return [ViewModelLifecycleAwareLazy]
 */
inline fun <reified VM : ViewModel> Fragment.parentFragmentViewModel(
    crossinline factory: () -> ViewModelProvider.Factory? = { null },
    crossinline keyFactory: () -> String? = { null }
): Lazy<VM> = ViewModelLifecycleAwareLazy(this) {
    val fragment = parentFragment
        ?: throw IllegalArgumentException("There is no parent fragment for ${this::class.java.simpleName}!")
    createViewModel(fragment, factory, keyFactory)
}

inline fun <reified VM : ViewModel> createViewModel(
    owner: ViewModelStoreOwner,
    crossinline factory: () -> ViewModelProvider.Factory? = { null },
    crossinline key: () -> String? = { null }
): VM {
    val factoryValue = factory() ?: ViewModelProvider.NewInstanceFactory()
    val keyValue = key()
    return if (keyValue == null) {
        ViewModelProvider(owner.viewModelStore, factoryValue).get(VM::class.java)
    } else {
        ViewModelProvider(owner.viewModelStore, factoryValue).get(keyValue, VM::class.java)
    }
}
