package com.example.smartwater

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import kotlin.collections.ArrayList

class ClockSetAdapter(context: Context, data: ArrayList<ClockSetItem>, private val layout: Int) : ArrayAdapter<ClockSetItem>(context, layout, data) {
    @SuppressLint("ViewHolder", "SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //依據傳入的 Layout 建立畫面
        val view = View.inflate(parent.context, layout, null)
        val item = getItem(position) ?: return view
        val btn = view.findViewById<Button>(R.id.btn_water_clock)
        btn.text = String.format("%02d:%02d", item.clockSet/60, item.clockSet%60)
        btn.setOnClickListener {
            //Toast.makeText(context, "You clicked the clock button ${position+1}！", Toast.LENGTH_SHORT).show()
            (parent as? AdapterView<*>)?.onItemClickListener?.onItemClick(parent, view, position, getItemId(position))
        }
        return view
    }
}