package com.example.androidtest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.SurfaceHolder
import kotlin.system.measureTimeMillis

class SampleHolderCallBack(private val context: Context?) : SurfaceHolder.Callback, Runnable {
    private val cpu: M6502 = M6502()
    private val io: IO = IO()
    private val sound: Sound = Sound(cpu, io)
    private val debug: Debug = Debug()
    private val joystick: Joystick = Joystick()
    private val audio: PCEApu = PCEApu()
    private val render: Render = Render(Pce.WIDTH, Pce.HEIGHT)
    private val pce: Pce =  Pce(cpu, io, sound, joystick, debug, render, audio)
    private var frameCount = 0
    private var appTime: Long = 0
    private var doReset = false
    private var romFileName: String? = null
    private var holder: SurfaceHolder? = null
    private var thread: Thread? = null
    private var isAttached = true
    private var width = 0f
    private var height = 0f
    private val paint = Paint()

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        // TODO 自動生成されたメソッド・スタブ
        this.width = width.toFloat()
        this.height = height.toFloat()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // TODO 自動生成されたメソッド・スタブ
        this.holder = holder

        Config.setupDefault()
        // TODO: 設定の読み込みはコメントアウトしておく
        //Config.load()
        audio.init()

        romFileName = "rtype1.pce"
        pce.InitPCE(romFileName, "", context)
        pce.ResetPCE(cpu)

        // サウンド初期化
        initSound();

        paint.style = Paint.Style.FILL_AND_STROKE;
        paint.strokeWidth = 1f;
        paint.textSize = 25f;
        paint.color = Color.argb(255, 0,255, 255);

        thread = Thread(this)
        thread!!.start() //スレッドを開始
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // TODO 自動生成されたメソッド・スタブ
        isAttached = false
        thread = null //スレッドを終了
    }

    override fun run() {

        var cpuTimeAvg = 0f;
        var cpuTimeSum = 0L;
        var ppuTimeAvg = 0f;
        var ppuTimeSum = 0L;
        var allTimeAvg = 0f;
        var allTimeSum = 0L;


        // TODO 自動生成されたメソッド・スタブ
        // メインループ（無限ループ）
        while (isAttached) {
            val allTime = measureTimeMillis {

//            // ROMファイル選択されるまで、メニューを表示する
//            while (romFileName == null) {
//                dispMenu(g)
//                if (isExit) {
//                    return
//                }
//            }
                val startAppTime = System.currentTimeMillis()
                if (doReset) {
                    pce.ResetPCE(cpu)
                    doReset = false
                }

                // キー状態を取得
                // TODO: キー入力
                val keyState: Int = 0;//getKeypadState()

                // PAD以外のキー入力された場合の処理
//            if (keyState and com.example.androidtest.Config.keyMenu != 0 && !isMenuCanceled) {
//                dispMenu(g)
//            } else if (keyState and com.example.androidtest.Config.keyPadSetting != 0 && !isMenuCanceled) {
//                dispPadSetting(g)
//            } else if (keyState and com.example.androidtest.Config.keyStateSave != 0 && nessageCount == 0) {
//                saveState()
//            } else if (keyState and com.example.androidtest.Config.keyStateLoad != 0 && nessageCount == 0) {
//                loadState()
//            } else if (keyState and com.example.androidtest.Config.keyMenu == 0) {
//                isMenuCanceled = false
//            }

                val cpuTime = measureTimeMillis {
                    // エミュレーション実行
                    if (cpu.Run6502(pce) == -1) {
                        return
                    }
                }

                cpuTimeSum += cpuTime;
                if ((frameCount % 60) == 0) {
                    cpuTimeAvg = cpuTimeSum / 60.0F
                    cpuTimeSum = 0L;
                }

                //描画処理を開始
                val canvas: Canvas = holder!!.lockCanvas()

                // ゲーム画面描画
                val ppuTime = measureTimeMillis {
                    render.draw(width.toInt(), height.toInt())
                }

                ppuTimeSum += ppuTime;
                if ((frameCount % 60) == 0) {
                    ppuTimeAvg = ppuTimeSum / 60.0F
                    ppuTimeSum = 0L;
                }

                val screenScaleX = width / render.bitmap.width
                val screenScaleY = height / render.bitmap.height
                val screenScale = if (screenScaleX < screenScaleY) screenScaleX else screenScaleY

                val screenOffsetX = if (width > render.bitmap.width * screenScale) {
                    ((width - render.bitmap.width * screenScale ) / 2).toInt()
                } else 0
                val screenOffsetY = if (height > render.bitmap.height * screenScale) {
                    ((height - render.bitmap.height * screenScale ) / 2).toInt()
                } else 0

                val srcRect = Rect(0, 0, render.bitmap.width, render.bitmap.height)
                val dstRect = Rect(screenOffsetX, screenOffsetY,
                    screenOffsetX +  (render.bitmap.width * screenScale).toInt(),
                    screenOffsetY + (render.bitmap.height * screenScale).toInt())

                canvas.drawBitmap(render.bitmap, srcRect, dstRect, null)

                canvas.drawText("cpu=$cpuTimeAvg[ms]", 250f, 100f, paint);
                canvas.drawText("ppu=$ppuTimeAvg[ms]", 250f, 150f, paint);
                canvas.drawText("all=$allTimeAvg[ms]", 250f, 200f, paint);

                //描画処理を終了
                holder!!.unlockCanvasAndPost(canvas)

                //sound.writeAudioTrack();

                // ビデオバッファクリア
                pce.clearVideoBuffer()
                appTime += System.currentTimeMillis() - startAppTime
                frameCount += Config.frameSkip + 1
            }

            allTimeSum += allTime;
            if ((frameCount % 60) == 0) {
                allTimeAvg = allTimeSum / 60.0F
                allTimeSum = 0L;
            }
        }
    }

    /**
     * サウンドを設定値で初期化します。
     */
    private fun initSound() {
        sound.initSound(
            Config.APUSampleRate,
            Config.APUVolume,
            Config.APUBuffer
        )
    }
}