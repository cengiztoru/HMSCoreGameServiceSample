package com.cengiztoru.hmscoregameservice

import android.app.Application
import com.huawei.hms.api.HuaweiMobileServicesUtil

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        HuaweiMobileServicesUtil.setApplication(this)
    }

}