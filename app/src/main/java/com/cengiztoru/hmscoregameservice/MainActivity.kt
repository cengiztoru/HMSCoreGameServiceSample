package com.cengiztoru.hmscoregameservice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.cengiztoru.hmscoregameservice.databinding.ActivityMainBinding
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hmf.tasks.Task
import com.huawei.hms.common.ApiException
import com.huawei.hms.jos.AppParams
import com.huawei.hms.jos.JosApps
import com.huawei.hms.jos.games.Games
import com.huawei.hms.jos.games.achievement.Achievement
import com.huawei.hms.jos.games.event.Event
import com.huawei.hms.jos.games.player.Player
import com.huawei.hms.support.account.request.AccountAuthParams
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.huawei.hms.support.hwid.result.HuaweiIdAuthResult
import org.json.JSONException


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivityTAG"
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
        printLog("Game Service Initializatition started")
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
                eventsClient.grow(
                    "990EC9FB259E4FCE3319C198635132D3C2BB68D3C7DA1A2AC8FDD43D83CBDEAF",
                    1
                )
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
            getAchievementList()

        }.addOnFailureListener { e -> //  Failed to obtain player information.
            if (e is ApiException) {

                printLog("Obtaining player info failed code : ${e.statusCode} message : ${e.localizedMessage}")

            }
        }

    }

//endregion

//region ACHIEVEMENTS

    private val achievementsClient by lazy {
        Games.getAchievementsClient(this)
    }

    private var achievements: List<Achievement>? = null

    private fun getAchievementList() {
        // Obtain the achievement list.
        val task = achievementsClient.getAchievementList(true)
        task.addOnSuccessListener(OnSuccessListener { data ->


            if (data == null || data.isEmpty()) {
                printLog("Achievement list is null")
                achievements = null
                return@OnSuccessListener
            }

            //you obtained all achievements now you can show them manually or via app assistant
            achievements = data

            var achievements = ""
            data.forEachIndexed { index, achievement ->
                achievements += achievement.displayName + if (index != data.lastIndex) ",  " else ""
            }

            printLog("${data.size} achievement found : $achievements")
            showAchievementListByAppAssistant()

        }).addOnFailureListener { e ->
            if (e is ApiException) {
                val message = when (e.statusCode) {
                    7018 -> {
                        initGameServiceSdk()
                        "PLEASE CALL SDK INIT FUNCTION FIRSTLY"
                    }

                    7201 -> {
                        "The achievement not found. Please configure achievements on AppGallery"
                    }
                    7218 -> {
                        "PLEASE ENABLE GAME SERVICE via  Me > Settings > Game Services on AppGallery"
                    }
                    else -> {
                        "getAchievementList failed statusCode : ${e.statusCode}"
                    }
                }
                printLog(message)
            }
        }
    }

    private var showAchievementsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                printLog("ACHIEVEMENTS SHOWED. RETURNING TO THIS ACTVITY STARTED")
            }
        }

    private fun showAchievementListByAppAssistant() {
        val task: Task<Intent> = achievementsClient.showAchievementListIntent
        task.addOnSuccessListener { intent ->

            intent?.let {
                showAchievementsLauncher.launch(it)
            } ?: run {
                //you can show the achievement list manually
            }

        }.addOnFailureListener { e ->
            if (e is ApiException) {
                // If result code 7204 is returned, HUAWEI AppAssistant is not available for displaying the achievement list page, and then the SDK will display a message indicating a service access failure. In this case, you can ignore the result code.
                if (e.statusCode == 7204) {
                    printLog("AppAssistant does not support the display of achievements interface for some reason. You can ignore the error code")
                    return@addOnFailureListener
                }

                //you can show the achievement list manually

                printLog("SHOW ACHIEVEMENT LIST FAILED.  Code: ${e.statusCode} Message: ${e.localizedMessage}")

            }
        }
    }

    private fun growFirstAchievement() {
        achievements?.firstOrNull()?.apply {
            val task: Task<Boolean> = achievementsClient.growWithResult(id, 1)
            task.addOnSuccessListener { isSucess ->
                if (isSucess) {
                    showAchievementListByAppAssistant()
                    printLog("growWithResult success")
                } else {
                    printLog("achievement can not grow")
                }
            }.addOnFailureListener { e ->
                if (e is ApiException) {
                    if (e.statusCode == 7203) {
                        eventsClient.grow(
                            "B1A320798CB1CFD7FAC3F2C421FE87E6C713948382E6BF631276CDE7B7BC7FFE",
                            1
                        )
                        printLog("Already, you reached first achievement. Congratulations!")
                        return@addOnFailureListener
                    }
                    printLog("growWithResult failure. Code:${e.statusCode} Message : ${e.localizedMessage}")
                }
            }
        }
    }

    //Set specific step to an achievement
    private fun setStepOfAchievement(achievementId: String, specificStep: Int) {
        achievementsClient.makeStepsWithResult(achievementId, specificStep)
            .addOnSuccessListener { isSucess ->
                if (isSucess) {
                    printLog("make steps success")
                } else {
                    printLog("achievement can not makeSteps")
                }
            }.addOnFailureListener { e ->
                if (e is ApiException) {
                    val result = ("rtnCode:"
                            + e.statusCode)
                    printLog("make steps result:$result")
                }
            }
    }

    private fun reachAchievement(achievementId: String) {
        achievementsClient.reachWithResult(achievementId)
            .addOnSuccessListener { printLog("reach  success") }
            .addOnFailureListener { e ->
                if (e is ApiException) {
                    val result = ("rtnCode:"
                            + e.statusCode)
                    printLog("reach result$result")
                }
            }
    }

//endregion

//region EVENTS


    private val eventsClient by lazy {
        Games.getEventsClient(this)
    }

    private var events: List<Event>? = null

    private fun getEvents() {
        eventsClient.getEventList(true).addOnSuccessListener {
            events = it
            events?.forEach {
                printLog("EventName : ${it.name}")
            }
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                printLog("getEventList failured. Code:${exception.statusCode} Message: ${exception.localizedMessage}")
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

        mBinding.btnShowAchievements.setOnClickListener {
            getAchievementList()
        }

        mBinding.btnGrowFirstAchievement.setOnClickListener {
            growFirstAchievement()
        }

        mBinding.btnGetEvents.setOnClickListener {
            getEvents()
        }
    }

    private fun printLog(message: String) {
        mBinding.tvLogger.append(message + "\n\n")
        Log.i(TAG, message)
    }
}