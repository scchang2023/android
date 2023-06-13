package com.example.smartwater

import android.app.TimePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random
const val MAX_AREA_NUM: Int = 4
const val MAX_CLOCK_NUM: Int = 4
const val UPDATE_CTRL_PANEL_TIME: Long = 1000

class MainActivity : AppCompatActivity() {
    private val _tag = "MainActivity"
    val itemCtrlPanel = ArrayList<CtrlPanelItem>()
    lateinit var lvAdapter: SWAdapter
    lateinit var mqttConnection: MqttConnection
    lateinit var lvCtrlPanel: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lvCtrlPanel = findViewById<ListView>(R.id.lv_ctrl_panel)
        val btnWaterStop = findViewById<Button>(R.id.btn_water_stop)
        val btnWaterContinue = findViewById<Button>(R.id.btn_water_continue)
        for(i in 0 until MAX_AREA_NUM) {
            itemCtrlPanel.add(CtrlPanelItem())
        }
        lvAdapter = SWAdapter(this, itemCtrlPanel,
            R.layout.sw_adapter)
        lvCtrlPanel.adapter=lvAdapter
        lvCtrlPanel.onItemClickListener = AdapterView.OnItemClickListener {
                _/*parent*/, _/*view*/, position, _/*id*/ ->
            Toast.makeText(this@MainActivity,
                "${mqttPubTopic[MqttPub.WATER_ON_SET.ordinal]} ${position},1,0,${itemCtrlPanel[position].durationSet}",
                Toast.LENGTH_SHORT).show()
            //waterOnSet/id,1,delay,duration
            mqttConnection.publish(mqttPubTopic[MqttPub.WATER_ON_SET.ordinal],
                "${position},1,0,${itemCtrlPanel[position].durationSet}")

        }

        val gvClockSet = findViewById<GridView>(R.id.gv_clock_set)
        val itemClockSet = ArrayList<ClockSetItem>()
        for(i in 0 until MAX_CLOCK_NUM) {
            itemClockSet.add(ClockSetItem())
        }
        val gvClockSetAdapter = ClockSetAdapter(this, itemClockSet,
            R.layout.clock_set_adapter)
        gvClockSet.adapter=gvClockSetAdapter
        gvClockSet.onItemClickListener = AdapterView.OnItemClickListener {
                _/*parent*/, _/*view*/, _/*position*/, _/*id*/ ->
            val timePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener {
                    _/*view*/, hourOfDay, minute ->
                // 在這裡處理所選擇的時間
                Log.d(_tag, "選擇的時間為：$hourOfDay:$minute")
            }, 0, 0, true)
            timePickerDialog.show()
        }

        btnWaterStop.setOnClickListener{
            var str:String
            for(i in 0 until MAX_AREA_NUM) {
                str = "$i,0,0,${itemCtrlPanel[i].durationSet}"
                //waterOnSet/id,0,delay,duration
                mqttConnection.publish(mqttPubTopic[MqttPub.WATER_ON_SET.ordinal],str)
                Toast.makeText(this@MainActivity,
                    "${mqttPubTopic[MqttPub.WATER_ON_SET.ordinal]} $str",
                    Toast.LENGTH_SHORT).show()
            }
        }

        btnWaterContinue.setOnClickListener {
            var str:String
            var delay=0
            for(i in 0 until MAX_AREA_NUM) {
                str = "$i,1,$delay,${itemCtrlPanel[i].durationSet}"
                //waterOnSet/id,0,delay,duration
                mqttConnection.publish(mqttPubTopic[MqttPub.WATER_ON_SET.ordinal], str)
                delay += itemCtrlPanel[i].durationSet
                Toast.makeText(this@MainActivity,
                    "${mqttPubTopic[MqttPub.WATER_ON_SET.ordinal]} $str",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onStart() {
        Log.d(_tag,"onStart")
        super.onStart()
        val mQTTServer = "tcp://test.mosquitto.org:1883"
        val mQTTClientID = "esp32-" + Random.nextInt(1000000, 9999999).toString()
        Log.d(_tag,"mQTTServer = [$mQTTServer], mQTTClientID = [$mQTTClientID]")
        mqttConnection = MqttConnection(mQTTServer, mQTTClientID)
        mqttConnection.connect()
        startTmrUpdateCtrlPanelUI()
    }
    private var tmrUpdateCtrlPanelUI = Timer()
    private fun startTmrUpdateCtrlPanelUI() {
        Log.d(_tag,"startTmrUpdateCtrlPanelUI")
        tmrUpdateCtrlPanelUI = Timer()

        val task = object : TimerTask() {
            override fun run() {
                for(i in 0 until MAX_AREA_NUM) {
                    itemCtrlPanel[i].durationCount = mqttConnection.itemDataSet[i].durationCount
                    itemCtrlPanel[i].waterStatus = mqttConnection.itemDataSet[i].waterIsOn
                    if(mqttConnection.lastHeartBeatTime[i]==0L
                        || (System.currentTimeMillis()-mqttConnection.lastHeartBeatTime[i] >=5000L)){
                        Log.d(_tag, "Have Not Received Data from Client $i !")
                        mqttConnection.isAlives[i] = false
                        itemCtrlPanel[i].isAlive = false
                    }else{
                        mqttConnection.isAlives[i] = true
                        itemCtrlPanel[i].isAlive = true
                    }
                }
                runOnUiThread {
                   lvAdapter.notifyDataSetChanged()
                }
            }
        }
        tmrUpdateCtrlPanelUI.schedule(task, 0, UPDATE_CTRL_PANEL_TIME)
    }
    fun stopTmrUpdateCtrlPanelUI() {
        Log.d(_tag,"stopTmrUpdateCtrlPanelUI")
        tmrUpdateCtrlPanelUI.cancel()
    }
}
data class CtrlPanelItem(
    var isAlive : Boolean = false,
    var waterStatus : Boolean = false,
    var durationSet : Int = 0,
    var durationCount : Int = 0
)
data class ClockSetItem(
    var clockSet : Int = 0,
    var clockEn : Boolean =true
)