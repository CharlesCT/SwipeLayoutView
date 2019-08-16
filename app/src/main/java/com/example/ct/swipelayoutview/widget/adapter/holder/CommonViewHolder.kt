package com.example.ct.swipelayoutview.widget.adapter.holder

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.example.ct.swipelayoutview.R
import com.example.ct.swipelayoutview.widget.BaseView


class CommonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
     var swipeLayout: BaseView? = null
     var tvContent: TextView? = null
     var btDelete: TextView? = null
     var btTop: TextView? = null
     var btUnRead: TextView? = null
     init {
         tvContent = itemView.findViewById(R.id.tv_content)
         btTop = itemView.findViewById(R.id.btnTop)
         btUnRead = itemView.findViewById(R.id.btnUnRead)
         btDelete = itemView.findViewById(R.id.btnDelete)
         swipeLayout = itemView.findViewById(R.id.swipe_layout)
     }
 }