package com.zoop.sdk.taponphone.sample

import android.app.Application
import com.zoop.sdk.plugin.taponphone.api.TapOnPhone

class Application : Application(){

    override fun onCreate() {
        super.onCreate()
        if(!TapOnPhone.kernelInitialize(this))
            return
    }
}