package com.example.smartwater

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView

class SWAdapter(context: Context, data: ArrayList<CtrlPanelItem>, private val layout: Int) :
    ArrayAdapter<CtrlPanelItem>(context, layout, data)
{
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val viewHolder: ViewHolder
        val view: View

        if (convertView == null) {
            view = View.inflate(parent.context, layout, null)
            viewHolder = ViewHolder()
            viewHolder.btnWaterStart = view.findViewById(R.id.btn_water_start)
            viewHolder.tvDurationSet = view.findViewById(R.id.tv_duration_set)
            viewHolder.tvDurationCount = view.findViewById(R.id.tv_duration_count)
            viewHolder.btnWaterStopOne = view.findViewById(R.id.btn_water_stop_one)
            viewHolder.imgWaterLight = view.findViewById(R.id.img_water_light)
            viewHolder.seekBar = view.findViewById(R.id.seek_duration_set)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as ViewHolder
        }

        val item = getItem(position) ?: return view
        val btnNameStrings = arrayOf(
            "一區啟動", "二區啟動", "三區啟動", "四區啟動"
        )

        if(position<btnNameStrings.size){
            viewHolder.btnWaterStart.text = btnNameStrings[position]
        }else{
            viewHolder.btnWaterStart.text = "${position+1}區啟動"
        }
        item.isAlive= true
        if(item.isAlive) {
            if (item.waterStatus) {
                viewHolder.imgWaterLight.setImageResource(R.drawable.img_light_water_on)
            } else {
                viewHolder.imgWaterLight.setImageResource(R.drawable.img_light_water_off)
            }
            viewHolder.tvDurationSet.text = String.format("%02d:%02d",(item.durationSet)/60,(item.durationSet)%60)
            viewHolder.tvDurationCount.text = String.format("%02d:%02d",(item.durationCount)/60,(item.durationCount)%60)
        }else{
            viewHolder.btnWaterStopOne.text = "--"
            viewHolder.imgWaterLight.setImageResource(R.drawable.img_light_water_disable)
            viewHolder.tvDurationSet.text = "--:--"
            viewHolder.tvDurationCount.text = "--:--"
        }
        if (convertView == null) {
            viewHolder.seekBar.progress = item.durationSet
        }
        //viewHolder.btnWaterStopOne = view.findViewById(R.id.btn_water_start)
        //val btnWaterStopOne = convertView?.findViewById<Button>(R.id.btn_water_stop_one)
        //btnWaterStopOne?.tag = "btnWaterStopOne_$position"
        viewHolder.btnWaterStopOne.setOnClickListener {
            if(item.isAlive)
                (parent as? AdapterView<*>)?.onItemClickListener?.onItemClick(parent, viewHolder.btnWaterStopOne, position, getItemId(position))
        }
        //val btnWaterStart = convertView?.findViewById<Button>(R.id.btn_water_start)
        //btnWaterStart?.tag = "btnWaterStart_$position"
        //viewHolder.btnWaterStart = view.findViewById(R.id.btn_water_start)
        viewHolder.btnWaterStart.setOnClickListener {
            if(item.isAlive)
                (parent as? AdapterView<*>)?.onItemClickListener?.onItemClick(parent, viewHolder.btnWaterStart, position, getItemId(position))
        }
        viewHolder.seekBar.isEnabled = item.isAlive
        viewHolder.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewHolder.tvDurationSet.text =
                    String.format("%02d:%02d", progress / 60, progress % 60)
                if (seekBar != null) {
                    item.durationSet = seekBar.progress
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        return view
    }

    class ViewHolder {
        lateinit var btnWaterStart: Button
        lateinit var tvDurationSet: TextView
        lateinit var tvDurationCount: TextView
        lateinit var btnWaterStopOne: Button
        lateinit var imgWaterLight: ImageView
        lateinit var seekBar: SeekBar
    }
}

