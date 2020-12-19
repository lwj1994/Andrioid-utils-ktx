package com.lwjlol.ktx

import android.view.View
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import java.io.Serializable


inline fun <reified VM : ViewModel> FragmentActivity.viewModel(
    viewModelStore: ViewModelStore = this.viewModelStore,
    crossinline factory: () -> ViewModelProvider.Factory? = { null },
    crossinline key: () -> String? = { null }
): Lazy<VM> = ViewModelLifecycleAwareLazy(this) {
    val factoryValue = factory() ?: ViewModelProvider.NewInstanceFactory()
    val keyValue = key()
    if (keyValue == null) {
        ViewModelProvider(viewModelStore, factoryValue).get(VM::class.java)
    } else {
        ViewModelProvider(viewModelStore, factoryValue).get(keyValue, VM::class.java)
    }
}


inline fun <reified VM : ViewModel> Fragment.activityViewModel(
    crossinline factory: () -> ViewModelProvider.Factory? = { null },
    crossinline key: () -> String? = { null }
): Lazy<VM> = ViewModelLifecycleAwareLazy(this) {
    val factoryValue = factory() ?: ViewModelProvider.NewInstanceFactory()
    val keyValue = key()
    if (keyValue == null) {
        ViewModelProvider(requireActivity().viewModelStore, factoryValue).get(VM::class.java)
    } else {
        ViewModelProvider(requireActivity().viewModelStore, factoryValue).get(
            keyValue,
            VM::class.java
        )
    }
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
inline fun <reified VM : ViewModel> Fragment.parentViewModel(
    crossinline factory: () -> ViewModelProvider.Factory? = { null },
    crossinline keyFactory: () -> String? = { null }
): Lazy<VM> = ViewModelLifecycleAwareLazy(this) {
    val fragment = parentFragment
        ?: throw IllegalArgumentException("There is no parent fragment for ${this::class.java.simpleName}!")
    val keyValue = keyFactory()
    val factoryValue = factory() ?: ViewModelProvider.NewInstanceFactory()
    if (keyValue == null) {
        ViewModelProvider(fragment.viewModelStore, factoryValue).get(VM::class.java)
    } else {
        ViewModelProvider(fragment.viewModelStore, factoryValue).get(keyValue, VM::class.java)

    }
}

inline fun <reified VM : ViewModel> Fragment.viewModel(
    viewModelStore: ViewModelStore = this.viewModelStore,
    crossinline factory: () -> ViewModelProvider.Factory? = { null },
    crossinline key: () -> String? = { null }
): Lazy<VM> = ViewModelLifecycleAwareLazy(this) {
    val factoryValue = factory() ?: ViewModelProvider.NewInstanceFactory()
    val keyValue = key()
    if (keyValue == null) {
        ViewModelProvider(viewModelStore, factoryValue).get(VM::class.java)
    } else {
        ViewModelProvider(viewModelStore, factoryValue).get(keyValue, VM::class.java)
    }
}

inline fun <reified VM : ViewModel> View.viewModel(
    crossinline factory: () -> ViewModelProvider.Factory? = { null },
    crossinline key: () -> String? = { null }
): Lazy<VM> = lazy(
    LazyThreadSafetyMode.NONE
) {
    val factoryValue = factory() ?: ViewModelProvider.NewInstanceFactory()
    val keyValue = key()
    val store = (context as FragmentActivity).viewModelStore
    if (keyValue == null) {
        ViewModelProvider(store, factoryValue).get(VM::class.java)
    } else {
        ViewModelProvider(store, factoryValue).get(keyValue, VM::class.java)
    }
}

inline fun <reified VM : ViewModel> View.getViewModel(
    factory: ViewModelProvider.Factory? = null, key: String? = null
): VM {
    val context = context as? FragmentActivity
        ?: throw IllegalStateException("view context is not FragmentActivity")
    val f = factory ?: ViewModelProvider.NewInstanceFactory()
    return if (key == null) {
        ViewModelProvider(context.viewModelStore, f).get(VM::class.java)
    } else {
        ViewModelProvider(context.viewModelStore, f).get(key, VM::class.java)
    }
}


private object UninitializedValue

/**
 * This was copied from SynchronizedLazyImpl but modified to automatically initialize in ON_CREATE.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("Detekt.ClassNaming")
class ViewModelLifecycleAwareLazy<out T>(private val owner: LifecycleOwner, initializer: () -> T) : Lazy<T>,
    Serializable {
    private var initializer: (() -> T)? = initializer

    @Volatile
    @SuppressWarnings("Detekt.VariableNaming")
    private var _value: Any? = UninitializedValue

    // final field is required to enable safe publication of constructed instance
    private val lock = this

    init {
        owner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onStart() {
                if (!isInitialized()) value
                owner.lifecycle.removeObserver(this)
            }
        })
    }

    @Suppress("LocalVariableName")
    override val value: T
        get() {
            @SuppressWarnings("Detekt.VariableNaming")
            val _v1 = _value
            if (_v1 !== UninitializedValue) {
                @Suppress("UNCHECKED_CAST")
                return _v1 as T
            }

            return synchronized(lock) {
                @SuppressWarnings("Detekt.VariableNaming")
                val _v2 = _value
                if (_v2 !== UninitializedValue) {
                    @Suppress("UNCHECKED_CAST") (_v2 as T)
                } else {
                    val typedValue = initializer!!()
                    _value = typedValue
                    initializer = null
                    typedValue
                }
            }
        }

    override fun isInitialized(): Boolean = _value !== UninitializedValue

    override fun toString(): String =
        if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}