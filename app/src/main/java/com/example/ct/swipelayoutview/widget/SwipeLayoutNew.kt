package com.example.ct.swipelayoutview.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.*
import android.view.MotionEvent.*
import kotlin.math.abs
import kotlin.math.max

class SwipeLayoutNew @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseView(context, attrs, defStyleAttr) {
    private var mScaleTouchSlop            = 0 // 用于判断是否是侧滑
    private var mMaxVelocity:Int           = 0 //用于记录滑动的速度
    private var mPointerId                 = 0 //计算第一根手指的
    private var mHeight                    = 0 //自己的高度
    private var mRightMenuWidth            = 0 //右边滑动的最大距离
    private var mLimit                     = 0 //滑动判定的临界值（右侧菜单的40%），当手指抬起的时候小于这个临界值，就收起右侧菜单，大于了就展开menu。
    private var mDisplayWidth              = 0

    private var isExpand                   = false //当前的View是够被展开
    private var isOnIntercept              = false    //处理 如果当前的列表中有展开的View，应该拦截一切点击事件。
    private var isOpenLink                 = true     //是否打开联动关闭
    @JvmField
    val INVALID_TAG:Int                    = -999 //禁止标志


    private var mContentView :View? = null
    companion object{
        @JvmField
        val sExplands: SparseArray<SwipeLayoutNew> = SparseArray()

    }
    private var mLastP = PointF()
    private var mFirstP = PointF() //判断手指起始落点，如果距离属于滑动了，就屏蔽一切点击事件，
    //防止多只手指一起滑动的
    private var isTouching = false
    private var mVelocityTracker: VelocityTracker? = null
    /**
     * 平滑展开
     * 平滑关闭 使用属性动画来做
     */
    private var mExpandAnim: ValueAnimator? = null
    private var mCloseAnim:ValueAnimator? = null


    init {
        mScaleTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mMaxVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    }



    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        //开始布局，使用第一个View铺满页面
        var left = mDisplayWidth - mRightMenuWidth


        for (i:Int in 0..childCount){
            val childView = getChildAt(i)
            if(childView!=null&&childView.visibility != GONE){
                if(i!=childCount-1){
                    childView.layout(left, paddingTop, left + childView.measuredWidth, paddingTop + childView.measuredHeight)
                    left += childView.measuredWidth
                }
            }
        }
        val firstChildView = getChildAt(childCount-1)
        if (firstChildView!=null&&firstChildView.visibility != View.GONE){
            firstChildView.layout(paddingLeft, paddingTop, paddingLeft + mDisplayWidth, paddingTop + firstChildView.measuredHeight)
        }

    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        isClickable = true//令自己可点击，从而获取触摸事件
        mRightMenuWidth  = 0
        mHeight = 0
        //开始测量可以滑动的最大距离
        mDisplayWidth = 0 //为了适配GridLayoutManager，将第一个子Item的宽度为控件的宽度
        val childCount = childCount //获取childCount
        //高度不确定不需要做测量
        val measureMatchParentChildren = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
        var isNeedMeasureChildHeight = true
        for (i in 0..childCount){
            val childView = getChildAt(i)
            if (childView!=null&&childView.visibility  != View.GONE){
                //设置可以点击 获取触摸事件
                childView.isClickable = true
                //开始measureChildView
                measureChild(childView,widthMeasureSpec,heightMeasureSpec)
                val marginLayoutParams: MarginLayoutParams  = childView.layoutParams as MarginLayoutParams
                mHeight = max(mHeight, childView.measuredHeight)
                if(measureMatchParentChildren && marginLayoutParams.height == LayoutParams.MATCH_PARENT){
                    isNeedMeasureChildHeight = true
                }
                if(i!=childCount-1){
                    //第一个为正常显示的item，从第二个开始进行计算右移宽度
                    mRightMenuWidth += childView.measuredWidth
                }else{
                    mContentView = childView
                    mDisplayWidth = childView.measuredWidth
                }
            }
        }
        //宽度设置为第一个item的宽度
        setMeasuredDimension(paddingLeft + paddingRight + mDisplayWidth,mHeight + paddingTop + paddingBottom)
        mLimit = mRightMenuWidth*3/10 //百分之30为滑动临界值
        mScaleTouchSlop = mRightMenuWidth*2/10 //百分之10为视为滑动
        if(isNeedMeasureChildHeight){
            //主要是针对使用wrap_content的子View，让他和父布局一样的高
            forceUniformHeight(widthMeasureSpec)
        }
    }


    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        acquireVelocityTracker(ev)

        when(ev?.action){
            ACTION_DOWN ->{

                //防止多根手指进入滑动，只响应第一根手指，否则会出现乱滑动的情
                //dispatchTouchEvent 返回true代表整个事件结束了 后续事件就不传递了
                if (isTouching){
                    return  true
                }else{
                    isTouching = true
                }

                //设置点击坐标
                mLastP.set(ev.rawX,ev.rawY)
                mFirstP.set(ev.rawX,ev.rawY)//判断手指起始落点，如果距离属于滑动了，就屏蔽一切事件
                mPointerId = ev.getPointerId(0) //获取第一个触点的坐标，用于计算滑动速度
            }
            ACTION_MOVE->{
                val gap:Float = ev.rawX -mLastP.x
                //防止在滑动的时候，父布局上下滑动 自己滑动的时候父亲布局不滑动
                if (abs(gap) > 15 || (abs(translationX)>0&&!isExpand)){
                    //父布局不滑动
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                //gap 《 0代表手指向左滑动
                if(gap<0){
                    mContentView!!.translationX += gap
                }else if( gap>0&& mContentView!!.translationX<0){
                    //gap > 0代表菜单没有开启。开启菜单
                    //开启菜单
                    mContentView!!.translationX += gap
                }
                //越界修正
                if( mContentView!!.translationX < (-mRightMenuWidth)){
                    mContentView!!.translationX = - (mRightMenuWidth*1.0F)
                }
                if (mContentView!!.translationX > 0){
                    mContentView!!.translationX = 0F
                }
                //跟踪坐标
                mLastP.set(ev.rawX,ev.rawY)
            }
            ACTION_UP, ACTION_CANCEL->{

                val instance =  mLastP.x - mFirstP.x
                //测量瞬间速度
                mVelocityTracker?.computeCurrentVelocity(1000, mMaxVelocity.toFloat())
                val velocityTrackerX = mVelocityTracker!!.getXVelocity(mPointerId)
                if (abs(velocityTrackerX) > 1000){//瞬间速度视为滑动了
                    if(velocityTrackerX < -1000){
                        //使用展开动画
                        smoothExpand()
                    }else{
                        smoothClose()
                    }
                }else{
                    if(abs(instance)>=mLimit && instance<0&& abs(mContentView!!.translationX) <mRightMenuWidth){
                        smoothExpand()
                    }else if(abs(mContentView!!.translationX) > 0){
                        if (isExpand){
                            closeAllExpland()
                        }else{
                            smoothClose()
                        }

                    }
                }
                isTouching = false//没有手指触碰我了，不然会出现乱滑动的情况
                relaseVelocityTracker() //释放资源
            }
        }
        return super.dispatchTouchEvent(ev)
    }
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        when(ev?.action){
            ACTION_DOWN->{
                isOnIntercept = false
                if ( mContentView!!.translationX<0&&ev.rawX<(mDisplayWidth - mRightMenuWidth)){
                    //自身View展开 ，没有点击在功能区，进行关闭 ,并且拦截点击事件
                    isOnIntercept = true
                     closeAllExpland()
                }else if ( mContentView!!.translationX  >= 0F){
                    //自身view没有展开，但是点击在功能区了,进行关闭 拦截点击事件

                     closeAllExpland()
                    if(isOpenLink)
                     isOnIntercept = true

                }
            }

            ACTION_MOVE->{
                if (abs(ev.rawX - mFirstP.x)>mScaleTouchSlop){
                    return true //拦截事件 已经在滑动了
                }
            }
            ACTION_UP->{
                //如果在处于滑动了，屏蔽一切点击事件
                //如果有展开的并且没有点击在功能区，屏蔽点击事件
                if ( isOnIntercept ) {
                    return true
                }

            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context,attrs)
    }



    private fun acquireVelocityTracker(ev: MotionEvent?){
        //!!只会在你需要对某对象进行非空判断，并且需要抛出异常时才会使用到，
        //?:表示的意思是，当对象A值为null的时候，那么它就会返回后面的对象B。
        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain()

        mVelocityTracker!!.addMovement(ev)

    }
    //回收速度测量器
    private fun relaseVelocityTracker(){
        if(null != mVelocityTracker){
            mVelocityTracker?.clear()
            mVelocityTracker?.recycle()
            mVelocityTracker = null
        }
    }




    /***
     * 绘制match_parent的高度子控件
     */
    private fun forceUniformHeight(widthMeasureSpec: Int){
        val uniformMeasureSpec = MeasureSpec.makeMeasureSpec(measuredHeight,MeasureSpec.EXACTLY)//以父布局高度构建一个全铺布局
        for (i in 0..childCount){
            val childView = getChildAt(i)
            if (childView!=null&&childView.visibility != View.GONE) {
                val lp: MarginLayoutParams = childView.layoutParams as MarginLayoutParams
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    val oldWidth= lp.width
                    lp.width = childView.measuredWidth
                    measureChildWithMargins(childView,widthMeasureSpec,0,uniformMeasureSpec,0)
                    lp.width = oldWidth
                }
            }
        }

    }

    /**
     * 平滑展开菜单栏
     */
    private fun smoothExpand(){
        if(null!= mContentView){
            //屏蔽长按事件
            mContentView?.isLongClickable = false
        }
        clearAnim()
        mExpandAnim = ValueAnimator.ofInt(mContentView!!.translationX.toInt(),-mRightMenuWidth+10)
        mExpandAnim?.addUpdateListener {
                mContentView!!.translationX = (it.animatedValue as Int)*1F
        }
        //这里设置插值器效果不好看屏蔽掉
        mExpandAnim?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                //展开了
                if (position != INVALID_TAG){
                    sExplands.put(position, this@SwipeLayoutNew)
                }
                isExpand = true
            }
        })
        mExpandAnim!!.setDuration(300).start()//开始动画
    }

    /***
     * 平滑关闭菜单
     */
    private fun smoothClose(){
        //这时候应该将长按事件打开
        if (null !=  mContentView){
            mContentView?.isLongClickable = true
        }
        clearAnim()
        mCloseAnim = ValueAnimator.ofInt(mContentView!!.translationX.toInt(),0) //降到零
        mCloseAnim?.addUpdateListener {
            run{
                mContentView!!.translationX = (it.animatedValue as Int)*1.0F
                if (it.animatedValue as Int == 0) {
                    isExpand = false
                }
            }
        }

        mCloseAnim?.addListener(object :AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                //收起了
                if (position != INVALID_TAG){
                    sExplands.remove(position)
                }

            }
        })
        mCloseAnim!!.setDuration(300).start()
    }





    /**
     * 每次执行动画之前需要先停止动画
     */
    private fun clearAnim(){
        if (mExpandAnim!=null&&mExpandAnim!!.isRunning){
            mExpandAnim?.cancel()
        }
        if (mCloseAnim!=null&& mCloseAnim!!.isRunning){
            mCloseAnim?.cancel()
        }

    }


    private fun closeAllExpland(){
        if (isOpenLink){
            if (sExplands.size()>0){
                for (i in 0..sExplands.size()){
                    val key:Int = sExplands.keyAt(i)
                    sExplands.get(key)?.smoothClose()
                }
            }
        }else{
            smoothClose()
        }
    }
















}