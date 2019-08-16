package com.example.ct.swipelayoutview

import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import com.example.ct.swipelayoutview.widget.adapter.RecycleCommonAdapter
import com.example.ct.swipelayoutview.widget.listener.ActionListener
import com.example.ct.swipelayoutview.widget.listener.ItemOnClickListener
import kotlinx.android.synthetic.main.activity_main.*
import com.example.ct.swipelayoutview.widget.adapter.RecycleCommonAdapterNew

class MainActivity : AppCompatActivity() {
    private lateinit var mOldAdapter: RecycleCommonAdapter
    private lateinit var mNewApter: RecycleCommonAdapterNew
    private var  mUIHanlder: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recycle_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false)
        mUIHanlder = Handler()
        //mutableList 是可变的list，可以进行put的操作，其余的只读操作
        val data = arrayListOf<String>("这里是1","这里是2","这里是3","这里是4","这里是5","这里是6","这里是7",
            "这里是8","这里是9","这里是10","这里是11","这里是12","这里是13","这里是14"
        )
        mOldAdapter = RecycleCommonAdapter(this,data)
        mNewApter = RecycleCommonAdapterNew(this,data)
        mNewApter.mItemOnClickListener = object : ItemOnClickListener {
            override fun itemOnclick(position: Int, obj: Any?) {
                if (obj is String){
                    Toast.makeText(this@MainActivity,"点击的条目是: " + (position+1)+ "点击的内容是" + obj, Toast.LENGTH_SHORT).show()
                }
            }
        }
        mOldAdapter.mItemOnClickListener = object : ItemOnClickListener {
            override fun itemOnclick(position: Int, obj: Any?) {
                if (obj is String){
                    Toast.makeText(this@MainActivity,"点击的条目是: " + (position+1)+ "点击的内容是" + obj, Toast.LENGTH_SHORT).show()
                }
            }
        }
        mNewApter.mActionListener = object : ActionListener {

            override fun onDel(position: Int, obj: Any?) {
                Toast.makeText(this@MainActivity,"点击按钮是删除，位置是第"+ position+ "条", Toast.LENGTH_SHORT).show()
            }

            override fun onTop(position: Int, obj: Any?) {
                Toast.makeText(this@MainActivity,"点击按钮是置顶，位置是第"+ position+ "条", Toast.LENGTH_SHORT).show()

            }

            override fun unRead(position: Int, obj: Any?) {
                Toast.makeText(this@MainActivity,"点击按钮是未读，位置是第"+ position+ "条", Toast.LENGTH_SHORT).show()

            }
        }
        mOldAdapter.mActionListener = object : ActionListener {

            override fun onDel(position: Int, obj: Any?) {
                Toast.makeText(this@MainActivity,"点击按钮是删除，位置是第"+ position+ "条", Toast.LENGTH_SHORT).show()
            }

            override fun onTop(position: Int, obj: Any?) {
                Toast.makeText(this@MainActivity,"点击按钮是置顶，位置是第"+ position+ "条", Toast.LENGTH_SHORT).show()

            }

            override fun unRead(position: Int, obj: Any?) {
                Toast.makeText(this@MainActivity,"点击按钮是未读，位置是第"+ position+ "条", Toast.LENGTH_SHORT).show()

            }
        }
        val divider = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            divider.setDrawable(resources.getDrawable(R.drawable.comon_divier_bg, null))
        }else{
            @Suppress("DEPRECATION")
            divider.setDrawable(resources.getDrawable(R.drawable.comon_divier_bg))
        }
        recycle_view.addItemDecoration(divider)
        recycle_view.adapter = mOldAdapter


        btn_old.setOnClickListener {
            if ( recycle_view.adapter is RecycleCommonAdapterNew ){
                recycle_view.adapter = mOldAdapter
                mUIHanlder?.let {
                    it.post{ mOldAdapter.notifyDataSetChanged() }
                }
            }
        }
        btn_new.setOnClickListener {
            if ( recycle_view.adapter is RecycleCommonAdapter ){
                recycle_view.adapter = mNewApter
                mUIHanlder?.let {
                    it.post{ mNewApter.notifyDataSetChanged() }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mUIHanlder?.let{it.removeCallbacksAndMessages(null)}
        mUIHanlder = null
    }



}
