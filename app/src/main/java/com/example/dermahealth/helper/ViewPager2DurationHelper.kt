package com.example.dermahealth.helper

import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.viewpager2.widget.ViewPager2
import java.lang.reflect.Field

class ViewPager2DurationHelper(private val viewPager: ViewPager2) {

    private var customDuration: Int = 800 // default 800ms

    fun setScrollDuration(duration: Int) {
        customDuration = duration
        try {
            val recyclerViewField: Field = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(viewPager) as androidx.recyclerview.widget.RecyclerView

            val mViewFlingerField: Field =
                androidx.recyclerview.widget.RecyclerView::class.java.getDeclaredField("mViewFlinger")
            mViewFlingerField.isAccessible = true
            val viewFlinger = mViewFlingerField.get(recyclerView)

            val interpolator = DecelerateInterpolator()
            val scroller = object : Scroller(viewPager.context, interpolator) {
                override fun startScroll(
                    startX: Int, startY: Int, dx: Int, dy: Int, duration: Int
                ) {
                    // 👇 force custom duration instead of system duration
                    super.startScroll(startX, startY, dx, dy, customDuration)
                }
            }

            val scrollerField: Field = viewFlinger!!::class.java.getDeclaredField("mScroller")
            scrollerField.isAccessible = true
            scrollerField.set(viewFlinger, scroller)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
