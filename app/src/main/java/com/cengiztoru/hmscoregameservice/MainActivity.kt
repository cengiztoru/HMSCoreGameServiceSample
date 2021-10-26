package com.cengiztoru.hmscoregameservice

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.cengiztoru.hmscoregameservice.databinding.ActivityMainBinding
import com.huawei.hmf.tasks.Task
import com.huawei.hms.common.ApiException
import com.huawei.hms.jos.AppParams
import com.huawei.hms.jos.JosApps
import com.huawei.hms.jos.games.Games
import com.huawei.hms.jos.games.player.Player
import com.huawei.hms.support.account.request.AccountAuthParams
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.huawei.hms.support.hwid.result.HuaweiIdAuthResult
import org.json.JSONException


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
        initGameServiceSdk()      // Must be called upon app launch, rather than during user operations such as sign-in and payment.
    }

//region SERVICE INITIALIZATION

    private fun initGameServiceSdk() {
        val params = AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME
        val appsClient = JosApps.getJosAppsClient(this)
        val initTask: Task<Void> = appsClient.init(AppParams(params) {
            // Implement the game addiction prevention function, such as saving games and calling the account sign-out API.
        })
        initTask.addOnSuccessListener {
            printLog("init success")
            signIn()
        }.addOnFailureListener { e ->
            printLog("init failed, " + e.message)
        }
    }

//endregion

//region SIGN IN WITH HUAWEI ID

    // SignIn Explicitly
    private val singInResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

                result.data?.run {

                    val jsonSignInResult = result.data?.getStringExtra("HUAWEIID_SIGNIN_RESULT")

                    if (jsonSignInResult.isNullOrBlank()) {
                        printLog("SingIn result empty.")
                        return@registerForActivityResult
                    }

                    try {
                        val signInResult = HuaweiIdAuthResult().fromJson(jsonSignInResult)
                        if (signInResult.status.statusCode == 0) {
                            printLog("SignIn Sucess \nDisplay Name ${signInResult.toJson()}")
                            getCurrentPlayerInfo()
                        } else {
                            printLog("SignIn Failed \nStatus Code : ${signInResult.status.statusCode}}")
                        }
                    } catch (jsonException: JSONException) {
                        printLog("Failed to convert json from signInResult.")
                    }

                } ?: run {
                    printLog("SignIn intent is null")
                    return@registerForActivityResult
                }

            }
        }

    private fun signIn() {

        //Try to Silent Sign In
        HuaweiIdAuthManager.getService(this, getHuaweiIdParams()).silentSignIn()
            .addOnSuccessListener { authHuaweiId ->
                printLog("SilentSignIn success \nDisplayName: ${authHuaweiId.displayName}")
                getCurrentPlayerInfo()
            }.addOnFailureListener { e ->
                if (e is ApiException) {
                    printLog("SilentsignIn failed. Normal signin starting")
//              Sign in explicitly. The sign-in result is obtained in onActivityResult.
                    val service =
                        HuaweiIdAuthManager.getService(this@MainActivity, getHuaweiIdParams())
                    singInResultLauncher.launch(service?.signInIntent)
                }
            }

    }

    private fun getHuaweiIdParams(): HuaweiIdAuthParams? {
        return HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME).setIdToken()
            .createParams()
    }


//endregion


//region OBTAIN CURRENT USER INFO

    private fun getCurrentPlayerInfo() {

        val playersClient = Games.getPlayersClient(this)
        val playerTask: Task<Player> = playersClient.currentPlayer
        playerTask.addOnSuccessListener { player ->

        printLog("Obtained Player Info \n ID: ${player.playerId} \nLevel: ${player.level}")

        }.addOnFailureListener { e -> //  Failed to obtain player information.
            if (e is ApiException) {

                printLog("Obtaining player info failed code : ${e.statusCode} message : ${e.localizedMessage}")

            }
        }

    }

//endregion


    private fun setListeners() {
        //this is just for sample. You should call initialization when app launched
        mBinding.btnInit.setOnClickListener {
            initGameServiceSdk()
        }

        mBinding.btnSignin.setOnClickListener {
            signIn()
        }

        mBinding.btnCurrentPlayer.setOnClickListener {
            getCurrentPlayerInfo()
        }
    }

    private fun printLog(message: String) {
        mBinding.tvLogger.append(message + "\n\n")
        Log.i(TAG, message)
    }
}