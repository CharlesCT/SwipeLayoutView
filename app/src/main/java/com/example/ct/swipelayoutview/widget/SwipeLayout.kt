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
import android.view.animation.OvershootInterpolator
import kotlin.math.abs
import kotlin.math.max

class SwipeLayout @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseView(context, attrs, defStyleAttr) {
    private var mScaleTouchSlop                 = 0 //用于判断是否是侧滑，
    private var mMaxVelocity:Int                = 0 //用于记录滑动的速度
    private var mPointerId                      = 0 //用于记录第一根手指的坐标
    private var mHeight                         = 0 //自己的高度
    private var mRightMenuWidth                 = 0 //右边滑动的最大距离
    private var mLimit                          = 0 //滑动判定的临界值（一般默认右侧菜单的30%），当手指抬起的时候小于这个临界值，就收起右侧菜单，大于了就展开右侧菜单。
    private var mDisplayWidth                   = 0 //内容区域的宽度

    private var mContentView :View? = null  //显示的内容区域，主要是添加点击Item事件和长按事件。


    private var isExpand                       = false       //当前的View是否已经展开

    private var isOnIntercept                  = false    //处理 如果当前的列表中有展开的View，应该拦截一切点击事件。

    private var isOpenLink                     = true  //是否开启联动关闭，默认开启
    @JvmField
    val INVALID_TAG:Int                    = -999 //禁止标志




    companion object{
        @JvmField
        val sExplands: SparseArray<SwipeLayout> = SparseArray() //记录展开的位置

    }



    private var mLastP = PointF()  //记录手指的滑动的实时坐标，手指滑动到哪，就应该记录下来。



    private var mFirstP = PointF() //判断手指起始落点，如果距离属于滑动了，就屏蔽一切点击事件，



    private var isTouching = false //防止多只手指一起滑动的，只接受第一根手指滑动，其余的手指进来直接返回true，则这个事件就不会往下传递了
    private var mVelocityTracker: VelocityTracker? = null
    /**
     * 平滑展开
     * 平滑关闭 使用属性动画来做
     */
    private var mExpandAnim: ValueAnimator? = null
    private var mCloseAnim:ValueAnimator? = null


    init {
        mScaleTouchSlop = ViewConfiguration.get(context).scaledTouchSlop //最小滑动距离有点问题，太小了，会很灵敏，重新设置为滑动距离的十分之一
        mMaxVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity //用于测量手指的瞬间速度
    }



    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        //开始布局，使用第一个View铺满页面
        var left = 0+ paddingLeft
        for (i:Int in 0..childCount){
            val childView = getChildAt(i)
            if (childView!=null&&childView.visibility != GONE) {
                childView.layout(left, paddingTop, left + childView.measuredWidth, paddingTop + childView.measuredHeight)
                left += childView.measuredWidth
            }
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        isClickable = true//令自己可点击，从而获取触摸事件
        mRightMenuWidth  = 0
        mHeight = 0
        mDisplayWidth = 0 //内容区域的
        val childCount = childCount //获取childCount
        val measureMatchParentChildren = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
        var isNeedMeasureChildHeight = false
        for (i in 0..childCount){
            val childView = getChildAt(i)
            if (childView!=null&&childView.visibility  != View.GONE){
                //设置可以点击 获取触摸事件
                childView.isClickable = true
                //开始测量
                measureChild(childView,widthMeasureSpec,heightMeasureSpec)
                val marginLayoutParams: MarginLayoutParams  = childView.layoutParams as MarginLayoutParams
                mHeight = max(mHeight, childView.measuredHeight)
                if(measureMatchParentChildren && marginLayoutParams.height == LayoutParams.MATCH_PARENT){
                    isNeedMeasureChildHeight = true
                }
                if(i>0){
                    //第一个为正常显示的item，从第二个开始进行计算右移宽度
                    mRightMenuWidth += childView.measuredWidth
                }else{
                    mContentView = childView
                    mDisplayWidth = childView.measuredWidth
                }
            }
        }
        //宽度设置为内容区域的宽度
        setMeasuredDimension(paddingLeft + paddingRight + mDisplayWidth,mHeight + paddingTop + paddingBottom)
        mLimit = mRightMenuWidth*3/10  //百分之30为滑动临界值
        if(isNeedMeasureChildHeight){
            //如果自身为warp_content,但是子View有match属性的时候，需要重新测量，让它和测量的父布局一样高。
            forceUniformHeight(widthMeasureSpec)
        }
    }


    /**
     * 在这里做一次
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action ==ACTION_UP ){
            Log.v("SwipeLayout" , "进入cancel事件了！" )
        }
        acquireVelocityTracker(ev)
        when(ev?.action){
            ACTION_DOWN ->{
                //防止多根手指进入滑动，只响应第一根手指，否则会出现乱滑动的情
                if (isTouching){
                    //如果dispatchTouchEvent 返回true代表整个事件结束了 后续事件就不传递了
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
                val gap:Float = mLastP.x - ev.rawX
                //防止在滑动的时候，父布局上下滑动 自己滑动的时候父亲布局不滑动
                Log.v("SwipeLayout" , "当前设置的触摸点" +  mLastP.x + "当前点击事件的坐标： " + ev.rawX +": " +ev.x )
                if (abs(gap) > 15 || (abs(scrollX)>0&&!isExpand)){
                    //父布局不滑动
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                if(gap>0){
                    //向左滑动
                    scrollBy(gap.toInt(), 0)//滑动使用scrollBy
                }else if(scrollX>0){
                    //向右滑动,并且功能区已经展开了
                    scrollBy(gap.toInt(), 0)//滑动使用scrollBy
                }

                //越界修正
                if(scrollX < 0){
                    scrollTo(0,0)
                }
                if (scrollX > mRightMenuWidth){
                    scrollTo(mRightMenuWidth,0)
                }
                //跟踪坐标
                mLastP.set(ev.rawX,ev.rawY)
            }
            ACTION_UP, ACTION_CANCEL->{
                //测量瞬间速度
                mVelocityTracker?.computeCurrentVelocity(1000, mMaxVelocity.toFloat())
                val velocityTrackerX = mVelocityTracker!!.getXVelocity(mPointerId)
                if (abs(velocityTrackerX) > 1000){//瞬间速度视为滑动了
                    if(velocityTrackerX < -1000 && !isExpand){
                        //使用展开动画
                        smoothExpand()
                    }else if(isExpand){
                        smoothClose()
                    }
                }else{
                    if(abs(scrollX)>=mLimit && !isExpand){
                        smoothExpand()
                    }else if(abs(scrollX) > 0){
                        smoothClose()//回弹
                    }
                }
                isTouching = false//没有手指触碰我了，不然会出现乱滑动的情况
                relaseVelocityTracker() //释放资源
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * 并不是每次都会调用的，一个完整的事件是从down-move....-up or cancle
     * 如当前的ViewGroup拦截除了down以外的任何一个事件，onInterceptTouchEvent都不会在调用
     */

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        when(ev?.action){
            ACTION_DOWN->{
                isOnIntercept = false
                if (scrollX>0&&ev.rawX<(mDisplayWidth - mRightMenuWidth)){
                    //自身View展开 ，没有点击在功能区，进行关闭 拦截点击事件
                     closeAllExpland()
                    isOnIntercept = true
                }else if (scrollX<=0&& sExplands.size()>0 ){
                    //自身view没有展开，但是点击在功能区了,进行关闭 拦截点击事件
                     closeAllExpland()
                    if (isOpenLink)
                         isOnIntercept = true
                }
            }
            ACTION_MOVE->{
                if (abs(ev.rawX - mFirstP.x)>mScaleTouchSlop){
                    return true //拦截事件 已经在滑动了
                }
            }
            ACTION_UP->{
                if (isOnIntercept ) {
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
        Log.v("SwipeLayout" , "进入展开了动画了" )
        if(null!= mContentView){
            //屏蔽长按事件
            mContentView?.isLongClickable = false
        }
        clearAnim()
        mExpandAnim = ValueAnimator.ofInt(scrollX,mRightMenuWidth)
        mExpandAnim?.addUpdateListener { animation ->
            run {
                scrollTo(animation.animatedValue as Int, 0)
                    if (animation.animatedValue as Int == mRightMenuWidth) {
                        isExpand = true
                    }
            }
        }
        mExpandAnim?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                //展开了
                if (position != INVALID_TAG){
                    sExplands.put(position, this@SwipeLayout)
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
        Log.v("SwipeLayout" , "进入关闭了动画了" )
        //这时候应该将长按事件打开
        if (null !=  mContentView){
            mContentView?.isLongClickable = true
        }
        clearAnim()
        mCloseAnim = ValueAnimator.ofInt(scrollX,0) //降到零
        mCloseAnim?.addUpdateListener { animation ->
            run {
                scrollTo(animation.animatedValue as Int, 0)
                if (animation.animatedValue as Int == 0) {
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