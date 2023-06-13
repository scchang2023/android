package com.example.water.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.water.databinding.FragmentHomeBinding
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.random.Random


class UpdateUIEvent(val data: MutableMap<String, Int?>, val connected: Boolean,
                    val isTimeoutSlave: Boolean, val isTimeoutMaster: Boolean)
class MqttConnection(private val brokerUrl: String, private val clientId: String) : MqttCallback {
    private val TAG = "AndroidMqttClient"
    private lateinit var mqttClient: MqttAsyncClient
    private var curTimeGetSlave:Long = 0
    private var curTimeGetMaster:Long = 0
    private var isTimeoutSlave:Boolean = false
    private var isTimeoutMaster:Boolean = false

    val subTopicMap = mutableMapOf<String, Int?>(
        "temp" to null, "humi" to null, "mois" to null,
        "waterStatus" to null, "waterMode" to null, "moisThres" to null,
        "waterDuration" to null, "waterTimerEn" to null,"waterTimer0" to null,
        "waterTimer1" to null, "waterTimer2" to null)
    //val pubTopicMap = listOf("water","waterModeSet")
    fun connect() {
        println("fun connect")
        mqttClient = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())
        mqttClient.setCallback(this)
        val connectOptions = MqttConnectOptions()
        connectOptions.isCleanSession = true
        println("$subTopicMap")
        mqttClient.connect(connectOptions, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                // 連線成功
                Log.d(TAG, "onSuccess")
                for (subTopic in subTopicMap) {
                    subscribeToTopic("SCCHANG/"+subTopic.key) // 訂閱主題
                    println("SCCHANG/"+subTopic)
                }
                stopTimerUpdateUI()
                startTimerUpdateUI()
            }
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                // 連線失敗
                Log.d(TAG, "onFailure")
                for (subTopic in subTopicMap) {
                    subTopicMap[subTopic.key] = null
                }
                stopTimerUpdateUI()
                EventBus.getDefault().post(UpdateUIEvent(subTopicMap, false,
                    isTimeoutSlave, isTimeoutMaster))
            }
        })
    }
    fun disconnect() {
        println("fun disconnect")
        mqttClient.disconnect()
        for (subTopic in subTopicMap) {
            subTopicMap[subTopic.key] = null
        }
        stopTimerUpdateUI()
        EventBus.getDefault().post(UpdateUIEvent(subTopicMap, false,
            isTimeoutSlave, isTimeoutMaster))
    }
    fun publish(topic: String, message: String) {
        mqttClient.publish(topic, MqttMessage(message.toByteArray()))
    }
    fun subscribeToTopic(topic: String) {
        mqttClient.subscribe(topic, 0)
    }
    fun isConnected(): Boolean {
        if(::mqttClient.isInitialized) {
            return mqttClient.isConnected()
        }else {
            return false
        }
    }
    override fun connectionLost(cause: Throwable?) {
        // 連線斷開
        mqttClient.disconnect()
        for (subTopic in subTopicMap) {
            subTopicMap[subTopic.key] = null
        }
        stopTimerUpdateUI()
        EventBus.getDefault().post(UpdateUIEvent(subTopicMap, false,
            isTimeoutSlave, isTimeoutMaster))
    }
    override fun messageArrived(topic: String?, message: MqttMessage?) {
        // 收到訊息
        message?.let {
            val payload = String(it.payload)
            // 在此處對收到的訊息進行處理
            val str:String? = topic?.substringAfter("/")
            str?.let{
                subTopicMap[it] = payload.toInt()
                println("$it $payload")
                if(it == "temp"){
                    curTimeGetSlave = System.currentTimeMillis()
                }
                if(it == "waterMode"){
                    curTimeGetMaster = System.currentTimeMillis()
                }
            }
        }
    }
    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        // 訊息發送完成
    }
    var timerUpdateUI = Timer()
    fun startTimerUpdateUI() {
        println("fun startTimerUpdateUI")
        timerUpdateUI = Timer()
        val task = object : TimerTask() {
            override fun run() {
                println("taskUpdateUI")
                val currentTime = System.currentTimeMillis()
                if(curTimeGetSlave==0L || (currentTime-curTimeGetSlave >=10000)){
                    println("Have Not Received Data from Slaved!!")
                    isTimeoutSlave = true
                }else{
                    isTimeoutSlave = false
                }
                if(curTimeGetMaster==0L || (currentTime-curTimeGetMaster >=10000)){
                    println("Have Not Received Data from Slaved!!")
                    isTimeoutMaster = true
                }else{
                    isTimeoutMaster = false
                }
                EventBus.getDefault().post(UpdateUIEvent(subTopicMap, true,
                    isTimeoutSlave, isTimeoutMaster))
            }
        }
        timerUpdateUI.schedule(task, 0, 3000)
    }
    fun stopTimerUpdateUI() {
        println("fun stopTimerUpdateUI")
        curTimeGetSlave =0
        curTimeGetMaster =0
        timerUpdateUI.cancel()
    }
}
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var mqttConnection: MqttConnection

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(
                this,
                ViewModelProvider.NewInstanceFactory()
            ).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        val MQTTServer: String = "tcp://test.mosquitto.org:1883"
        val MQTTClientid = "esp32-" + Random.nextInt(1000000, 9999999).toString()
        println("MQTTServer = ${MQTTServer}, MQTTClientid = ${MQTTClientid}")
        mqttConnection = MqttConnection(MQTTServer, MQTTClientid)
        mqttConnection.connect()

        binding.btnConnect.setOnClickListener{
            if(mqttConnection.isConnected()==false) {
                println("do connect")
                mqttConnection.connect()
            }else{
                println("do disconnect")
                mqttConnection.disconnect()
            }
        }
        binding.switchWater.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                println("turn on")
                mqttConnection.publish("SCCHANG/water","1")
            } else {
                println("turn off")
                mqttConnection.publish("SCCHANG/water","0")
            }
        }
        return root
    }
    override fun onStart() {
        println("onStart")
        super.onStart()
        EventBus.getDefault().register(this)
    }
    override fun onStop() {
        println("onStop")
        super.onStop()
        EventBus.getDefault().unregister(this)
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateUI(event: UpdateUIEvent) {
        println("onUpdateUI")
        binding.textCurTemp.text = if (event.isTimeoutSlave) "---" else event.data["temp"]?.toString() ?: "---"
        binding.textCurHumi.text = if (event.isTimeoutSlave) "---" else event.data["humi"]?.toString() ?: "---"
        binding.textCurMois.text = if (event.isTimeoutSlave) "---" else event.data["mois"]?.toString() ?: "---"
        binding.textCurMoisSetting.text = if (event.isTimeoutMaster) "---" else event.data["moisThres"]?.toString() ?: "---"
        binding.btnConnect.text = if (event.connected) "停止\n連線" else "開始\n連線"
        binding.textValWaterStatus.text = when (event.data["waterStatus"]) {
            null -> "---"
            1 -> "澆水中"
            else -> "停水中"
        }.takeUnless { event.isTimeoutMaster } ?: "---"
        binding.textValWaterMode.text = when (event.data["waterMode"]) {
            null -> "---"
            1 -> "自動"
            else -> "手動"
        }.takeUnless { event.isTimeoutMaster } ?: "---"
        binding.textDurationVal.text = when (event.data["waterDuration"]) {
            null -> "---"
            0 -> "3秒"
            1 -> "10秒"
            2 -> "30秒"
            else -> "1分鐘"
        }.takeUnless { event.isTimeoutMaster } ?: "---"
        binding.textValWaterMode.text = when (event.data["waterMode"]) {
            null -> "---"
            0 -> "手動"
            else -> "自動"
        }.takeUnless { event.isTimeoutMaster } ?: "---"
        if(event.data["waterTimer0"]==null || event.isTimeoutMaster){
            binding.textTimer0Val.text="--:--"
        }else{
            event.data["waterTimer0"]?.let {
                binding.textTimer0Val.text=String.format("%02d:%02d", (it*10/60),((it*10)%60))
            }
        }
        if(event.data["waterTimer1"]==null || event.isTimeoutMaster){
            binding.textTimer1Val.text="--:--"
        }else{
            event.data["waterTimer1"]?.let {
                binding.textTimer1Val.text=String.format("%02d:%02d", (it*10/60),((it*10)%60))
            }
        }
        if(event.data["waterTimer2"]==null || event.isTimeoutMaster){
            binding.textTimer2Val.text="--:--"
        }else{
            event.data["waterTimer2"]?.let {
                binding.textTimer2Val.text=String.format("%02d:%02d", (it*10/60),((it*10)%60))
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}