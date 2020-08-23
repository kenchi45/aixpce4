package com.example.androidtest

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceView


class SampleSurficeView(context: Context?) : SurfaceView(context) {
    private val cb: SampleHolderCallBack

    init {
        val holder = holder
        cb = SampleHolderCallBack(context)
        holder.addCallback(cb)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.v("KeyDown", "KeyCode=$keyCode");
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.v("KeyDown", "KeyCode=$keyCode");
        return super.onKeyUp(keyCode, event)
    }

    var stickPointX = 0
    var stickPointY = 0
    var buttonPointX = 0
    var buttonPointY = 0

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var keyState = 0;
        val count = event.pointerCount

        for (i in 0 until count) {
            val pid = event.getPointerId(i)
            val x = event.getX(i).toInt()
            val y = event.getY(i).toInt()

            if (x < (this.width / 2)) {
                // スティック操作処理
                val dx = x - stickPointX;
                val dy = y - stickPointY;

                if (dx > 0) {
                    Log.d("aixpce", "onTouchEvent: right")
                    keyState = keyState or Config.pad1Right
                }
                if (dx < 0) {
                    Log.d("aixpce", "onTouchEvent: left")
                    keyState = keyState or Config.pad1Left
                }
                if (dy > 0) {
                    Log.d("aixpce", "onTouchEvent: down")
                    keyState = keyState or Config.pad1Down
                }
                if (dy < 0) {
                    Log.d("aixpce", "onTouchEvent: up")
                    keyState = keyState or Config.pad1Up
                }
                stickPointX = x
                stickPointY = y
            } else {
                // ボタン操作処理
                if (y < (this.height / 2)) {
                    Log.d("aixpce", "onTouchEvent: start")
                    keyState = keyState or Config.pad1Start
                } else {
                    if (x > (this.width / 3 * 2)) {
                        Log.d("aixpce", "onTouchEvent: pad1A")
                        keyState = keyState or Config.pad1A
                    } else {
                        Log.d("aixpce", "onTouchEvent: pad1B")
                        keyState = keyState or Config.pad1B
                    }
                }
            }
        }

        cb.callbackKeyInput(keyState)
        return true
    }
}