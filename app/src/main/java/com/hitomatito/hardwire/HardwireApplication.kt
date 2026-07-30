package com.hitomatito.hardwire

import android.app.Application
import com.hitomatito.hardwire.di.AppContainer
import com.hitomatito.hardwire.di.ServiceLocator

class HardwireApplication : Application() {
    val container: AppContainer by lazy { ServiceLocator.getContainer(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
