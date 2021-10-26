package com.cengiztoru.hmscoregameservice

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.cengiztoru.hmscoregameservice.databinding.ActivityMainBinding
import com.huawei.hmf.tasks.Task
import com.huawei.hms.jos.AppParams
import com.huawei.hms.jos.JosApps
import com.huawei.hms.support.account.request.AccountAuthParams


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = "MainActivityTAG"
    }

    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setListeners()
//        initGameServiceSdk()      // Must be called upon app launch, rather than during user operations such as sign-in and payment.
    }


    private fun initGameServiceSdk() {
        val params = AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME
        val appsClient = JosApps.getJosAppsClient(this)
        val initTask: Task<Void> = appsClient.init(AppParams(params) {
            // Implement the game addiction prevention function, such as saving games and calling the account sign-out API.
        })
        initTask.addOnSuccessListener { printLog("init success") }
            .addOnFailureListener { e -> printLog("init failed, " + e.message) }
    }


    private fun setListeners() {
        //this is just for sample. You should call initialization when app launched
        mBinding.btnInit.setOnClickListener {
            initGameServiceSdk()
        }
    }

    private fun printLog(message: String) {
        mBinding.tvLogger.append(message + "\n\n")
        Log.i(TAG, message)
    }
}