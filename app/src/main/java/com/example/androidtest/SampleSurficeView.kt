package com.example.androidtest

import android.content.Context
import android.util.Log
import android.view.KeyEvent
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
}