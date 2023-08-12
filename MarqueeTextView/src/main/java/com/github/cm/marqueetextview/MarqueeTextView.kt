package com.github.cm.marqueetextview


import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.util.AttributeSet
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.withSave


/**
 * @Author:cooliceman
 * @Data: 2023/8/1
 */
class MarqueeTextView : AppCompatTextView {
    private var mFps = 60
    /*
     * 创建一个内部TextView用于确保实际文字占用空间和在TextView中显示的一致,
     * 根据paint进行文字长度计算可能出现和实际显示到TextView时占用宽度不符的情况,出现文字被截断几个像素,并且不滚动的情况
     */
    private lateinit var mTextView: TextView
    private val mFrameCallback: FrameCallback = object : FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (layoutDirection == LAYOUT_DIRECTION_RTL) {
                mLeftX += BASE_FPS / mFps * mSpeed
            } else {
                mLeftX -= BASE_FPS / mFps * mSpeed
            }

            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
    private var mLeftX = 0f

    /**
     * 文字滚动时，头尾的最小间隔距离
     */
    private var mSpace = DEFAULT_SPACE

    /**
     * 文字滚动速度
     */
    private var mSpeed = DEFAULT_SPEED * 2

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MarqueeTextView)
            mSpace =
                typedArray.getDimensionPixelSize(R.styleable.MarqueeTextView_space, DEFAULT_SPACE)
            val speedDp = typedArray.getFloat(R.styleable.MarqueeTextView_speed, DEFAULT_SPEED)
            mSpeed = dpToPx(speedDp, context)
            typedArray.recycle()
        } else {
            mSpeed = dpToPx(DEFAULT_SPEED, context)
        }
        mTextView = TextView(context, attrs)
        mTextView.layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        mTextView.maxLines = 1
        maxLines = 1
        mTextView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom -> restartScroll() }
    }

    override fun setText(text: CharSequence, type: BufferType) {
        super.setText(text, type)
        //执行父类构造函数时，如果AttributeSet中有text参数会先调用setText，此时mTextView尚未初始化
        if (::mTextView.isInitialized) {
            mTextView.text = text
            requestLayout()
        }
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        //执行父类构造函数时，如果AttributeSet中有textSize参数会先调用setTextSize，此时mTextView尚未初始化
        if (::mTextView.isInitialized) {
            mTextView.textSize = size
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mTextView.measure(MeasureSpec.UNSPECIFIED, heightMeasureSpec)

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mTextView.layout(left, top, left + mTextView.measuredWidth, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            onDrawRTL(canvas)
            return
        }
        if (mTextView.measuredWidth <= width) {
            //当文字宽度小于控件宽度时，不滚动
            mTextView.draw(canvas)
        } else {
            //当左移位移超过文字实际长+space时，重置mLeftX,达到循环滚动效果
            if (mLeftX < -mTextView.measuredWidth - mSpace) {
                mLeftX += (mTextView.measuredWidth + mSpace).toFloat()
            }
            canvas.withSave {
                canvas.translate(mLeftX, 0f)
                mTextView.draw(canvas)
            }

            //当文字已经完整显示,在可见区右侧添加第二次绘制,产生首尾相接连续滚动效果
            if (mLeftX + (mTextView.measuredWidth - width) < 0) {
                canvas.withSave {
                    canvas.translate(mTextView.measuredWidth + mLeftX + mSpace, 0f)
                    mTextView.draw(canvas)
                }
            }
        }
    }

    private fun onDrawRTL(canvas: Canvas) {
        if (mTextView.measuredWidth <= width) {
            //当文字宽度小于控件宽度时，不滚动
            canvas.withSave {
                canvas.translate((width - mTextView.measuredWidth).toFloat(), 0f)
                mTextView.draw(canvas)
            }

        } else {
            //当右移位移超过文字实际长+space时，重置mLeftX,达到循环滚动效果
            if (mLeftX > mTextView.measuredWidth + mSpace) {
                mLeftX -= mTextView.measuredWidth + mSpace
            }

            canvas.withSave {
                canvas.translate(-(mTextView.measuredWidth - width) + mLeftX, 0f)
                mTextView.draw(canvas)
            }

            //当文字已经完整显示,在可见区左侧添加第二次绘制,产生首尾相接连续滚动效果
            if (mLeftX - (mTextView.measuredWidth - width) > 0) {
                canvas.withSave {
                    canvas.translate(-mTextView.measuredWidth - mSpace + mLeftX - (mTextView.measuredWidth - width) , 0f)
                    mTextView.draw(canvas)
                }
            }
        }
    }

    private fun updateFps() {
        mFps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display!!.refreshRate.toInt()
        } else {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.refreshRate.toInt()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(mFrameCallback)
    }

    private fun startScroll() {
        updateFps()
        Choreographer.getInstance().postFrameCallback(mFrameCallback)
    }

    fun pauseScroll() {
        Choreographer.getInstance().removeFrameCallback(mFrameCallback)
    }

    private fun stopScroll() {
        mLeftX = 0f
        Choreographer.getInstance().removeFrameCallback(mFrameCallback)
    }

    private fun restartScroll() {
        stopScroll()
        startScroll()
    }

    // 将px值转换为sp值
    private fun dpToPx(dp: Float, context: Context): Float {
        val density = context.resources.displayMetrics.density
        return dp * density
    }

    companion object {
        /**
         * Unit: PX
         */
        private const val DEFAULT_SPACE = 100

        /**
         * Unit: DP
         */
        private const val DEFAULT_SPEED = 0.5f
        private const val BASE_FPS = 60f
    }
}
