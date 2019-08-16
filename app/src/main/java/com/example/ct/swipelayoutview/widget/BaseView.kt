package com.example.ct.swipelayoutview.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

abstract class BaseView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    var position :Int = -999 //标识ID ，用于记录展开的菜单。

}