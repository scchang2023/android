package com.example.smartwater

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

enum class MqttSub {
    WATER_IS_ON,
    DURATION_COUNT,
    DURATION
}
val mqttSubTopic = arrayOf(
    "SCCHANG/waterIsOn",
    "SCCHANG/durationCount",
    "SCCHANG/duration"
)
//val mqttSubData = Array<Byte>(MqttSub.values().size) { 0 }
enum class MqttPub {
    DURATION_SET,
    WATER_ON_SET
}
//val mqttPubData = Array<Byte>(MqttPub.values().size) { 0 }
val mqttPubTopic = arrayOf(
    "SCCHANG/durationSet",
    "SCCHANG/waterOnSet"
)
data class SubWaterData(
    var waterIsOn:Boolean = false,
    var durationCount:Int = 0,
    var duration:Int = 0
)
class MqttConnection(private val brokerUrl: String, private val clientId: String) : MqttCallback {
    private val _tag = "AndroidMqttClient"
    private lateinit var mqttClient: MqttAsyncClient
    val itemDataSet = ArrayList<SubWaterData>()
    val isAlives = BooleanArray(MAX_AREA_NUM)
    val lastHeartBeatTime = LongArray(MAX_AREA_NUM)

    fun connect() {
        Log.d(_tag,"fun connect")
        Log.d(_tag,"brokerUrl = $brokerUrl, clientId = $clientId")
        mqttClient = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())
        mqttClient.setCallback(this)
        val connectOptions = MqttConnectOptions()
        connectOptions.isCleanSession = true
        mqttClient.connect(connectOptions, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(_tag, "onSuccess")
                for (str in mqttSubTopic) {
                    subscribeToTopic(str) // 訂閱主題
                    Log.d(_tag,str)
                }
            }
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(_tag, "onFailure")
                Log.e(_tag, "Connection failed: ${exception?.message}", exception)
            }
        }
        )
        for(i in 0..MAX_AREA_NUM) {
            itemDataSet.add(SubWaterData(false, 0))
        }
    }
    fun disconnect() {
        Log.d(_tag,"fun disconnect")
        mqttClient.disconnect()
    }
    fun publish(topic: String, message: String) {
        Log.d(_tag,"fun publish")
        mqttClient.publish(topic, MqttMessage(message.toByteArray()))
    }
    fun subscribeToTopic(topic: String) {
        mqttClient.subscribe(topic, 0)
    }
    override fun connectionLost(cause: Throwable?) {
        Log.d(_tag,"fun connectionLost")
        mqttClient.disconnect()
    }
    override fun messageArrived(topic: String?, message: MqttMessage?) {
        message?.let {
            val payload = String(it.payload)
            val str:String? = topic?.substringAfter("/")
            str?.let{
                Log.d(_tag,"$it $payload")
                val idStr:String = payload.substringBefore(",")
                val subDataStr:String = payload.substringAfter(",")
                if(topic == mqttSubTopic[MqttSub.WATER_IS_ON.ordinal]){
                    itemDataSet[idStr.toInt()].waterIsOn = subDataStr.toInt() == 1
                    lastHeartBeatTime[idStr.toInt()] = System.currentTimeMillis()
                }
                if(topic == mqttSubTopic[MqttSub.DURATION_COUNT.ordinal]){
                    itemDataSet[idStr.toInt()].durationCount = subDataStr.toInt()
                }
            }
        }
    }
    override fun deliveryComplete(token: IMqttDeliveryToken?) {
    }
}

