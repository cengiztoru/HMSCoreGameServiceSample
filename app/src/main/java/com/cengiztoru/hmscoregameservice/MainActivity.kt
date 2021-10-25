package com.cengiztoru.hmscoregameservice

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hmf.tasks.Task
import com.huawei.hms.jos.AppParams
import com.huawei.hms.jos.JosApps
import com.huawei.hms.support.account.request.AccountAuthParams


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSdk()
    }

    private fun initSdk() {
        val params = AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME
        val appsClient = JosApps.getJosAppsClient(this)
        val initTask: Task<Void> = appsClient.init(AppParams(params) {
            // Implement the game addiction prevention function, such as saving games and calling the account sign-out API.
        })
        initTask.addOnSuccessListener { printLog("init success") }
            .addOnFailureListener { e -> printLog("init failed, " + e.message) }
    }

    private fun printLog(message: String) {
        Log.i(TAG, message)
    }
}