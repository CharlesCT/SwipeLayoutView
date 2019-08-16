package com.example.ct.swipelayoutview.widget.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.ct.swipelayoutview.R
import com.example.ct.swipelayoutview.widget.adapter.holder.CommonViewHolder
import com.example.ct.swipelayoutview.widget.listener.ActionListener
import com.example.ct.swipelayoutview.widget.listener.ItemOnClickListener


class RecycleCommonAdapterNew(mContext:Context, private var mData:ArrayList<String>) : RecyclerView.Adapter<CommonViewHolder>(){


    private var mInflater: LayoutInflater? = null
    var mItemOnClickListener: ItemOnClickListener? = null
    var mActionListener: ActionListener? = null

    init {
        //初始化加载器
        mInflater = LayoutInflater.from(mContext)
    }


    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): CommonViewHolder {
        val view = mInflater?.inflate(R.layout.recycle_view_test_item_new,p0,false)
        return CommonViewHolder(view!!)

    }

    override fun getItemCount(): Int = mData.size


    override fun onBindViewHolder(holder: CommonViewHolder, position: Int) {

        //绑定数据
        holder.tvContent?.text = mData[position]
        holder.swipeLayout!!.position = position
        Log.e("Swipe","绑定的position是："+ holder.swipeLayout!!.position)
        /**
        .let{}的基本使用
        object.let{
        it.todo()
        ...
        }

        //另一种用途 判断object为null的操作
        object?.let{//表示object不为null的条件下，才会去执行let函数体
        it.todo()
        }
         */
        mItemOnClickListener?.let { holder.tvContent?.setOnClickListener {
            mItemOnClickListener!!.itemOnclick(position,mData[position]) } }
        mActionListener?.let {
            holder.btDelete!!.setOnClickListener {
                mActionListener!!.onDel(position, mData[position])

            }
            holder.btTop!!.setOnClickListener {
                mActionListener!!.onTop(position, mData[position])

            }
            holder.btUnRead!!.setOnClickListener {
                mActionListener!!.unRead(position, mData[position])

            }

        }
    }

}
