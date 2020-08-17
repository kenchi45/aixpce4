package com.example.androidtest

//import javax.microedition.io.Connector;
//
//import com.nttdocomo.ui.Display;
//
//import dojapcmsound.PcmAudioHandler;
object Config {
    @JvmField
	var pad1A = 0
    @JvmField
	var pad1B = 0
    @JvmField
	var pad1Select = 0
    @JvmField
	var pad1Start = 0
    @JvmField
	var pad1Up = 0
    @JvmField
	var pad1Down = 0
    @JvmField
	var pad1Left = 0
    @JvmField
	var pad1Right = 0
    var keyStateSave = 0
    var keyStateLoad = 0
    var keyUp = 0
    var keyDown = 0
    var keyOK = 0
    var keyCancel = 0
    var keyMenu = 0
    var keyPadSetting = 0
    @JvmField
	var pad1AHold = false
    @JvmField
	var pad1BHold = false
    @JvmField
	var pad1ARapid = false
    @JvmField
	var pad1BRapid = false
    @JvmField
	var frameSkip = 0
    @JvmField
	var sleepEnable = false
    var vSyncWaitEnable = false
    var dispFPSEnable = false
    var APUVolume = 0
    var APUBuffer = 0
    @JvmField
	var APUEnable = true
    var APUStereoEnable = false
    var ScreenSetting = 0
    var busyLoopElimination = false
    var dispDebugMessage = false
    var APUSampleRate = 0
    var ScreenRotate = 0
    const val SCREEN_SETTING_NORMAL = 0
    const val SCREEN_SETTING_REVERSE = 1
    const val SCREEN_SETTING_SCALED = 2
    const val SCREEN_SETTING_SCALED2 = 3

    /**
     * 初期設定値を設定します。
     */
    fun setupDefault() {
//		pad1A = (1 << Display.KEY_3);
//		pad1B = (1 << Display.KEY_2);
//		pad1Select = (1 << Display.KEY_5);
//		pad1Start = (1 << Display.KEY_6);
//		pad1Up = (1 << Display.KEY_UP);
//		pad1Down = (1 << Display.KEY_DOWN);
//		pad1Left = (1 << Display.KEY_LEFT);
//		pad1Right = (1 << Display.KEY_RIGHT);
//		keyStateSave = (1 << Display.KEY_1);
//		keyStateLoad = (1 << Display.KEY_4);
//		keyUp = (1 << Display.KEY_UP) | (1 << Display.KEY_RIGHT);
//		keyDown = (1 << Display.KEY_DOWN) | (1 << Display.KEY_LEFT);
//		keyOK = (1 << Display.KEY_SELECT);
//		keyCancel = (1 << Display.KEY_IAPP);
//		keyMenu = (1 << Display.KEY_SOFT1);
//		keyPadSetting = (1 << Display.KEY_SOFT2);
        pad1AHold = false
        pad1BHold = false
        pad1ARapid = false
        pad1BRapid = false
        frameSkip = 0
        sleepEnable = false
        vSyncWaitEnable = false
        dispFPSEnable = false
        //		APUMode1 = PcmAudioHandler.MODE_SA702i;
//		APUMode2 = 0;
        APUVolume = 100
        APUBuffer = 300
        APUEnable = true
        APUStereoEnable = false
        ScreenSetting = 0
        busyLoopElimination = false
        dispDebugMessage = false
        APUSampleRate = 44100
        ScreenRotate = 0
    } //	/**
    //	 * 設定値をスクラッチパッドからロードします。
    //	 * @return
    //	 */
    //	public static boolean load() {
    //		DataInputStream input = null;
    //		try {
    //			input = Connector.openDataInputStream("scratchpad:///0;pos=0");
    //			if (input == null) {
    //				return false;
    //			}
    //			if (!(input.readChar() == 'C'
    //				&& input.readChar() == 'O'
    //				&& input.readChar() == 'N'
    //				&& input.readChar() == 'F')) {
    //				return false;
    //			}
    //			pad1A = input.readInt();
    //			pad1B = input.readInt();
    //			pad1Select = input.readInt();
    //			pad1Start = input.readInt();
    //			pad1Up = input.readInt();
    //			pad1Down = input.readInt();
    //			pad1Left = input.readInt();
    //			pad1Right = input.readInt();
    //			keyStateSave = input.readInt();
    //			keyStateLoad = input.readInt();
    //			keyUp = input.readInt();
    //			keyDown = input.readInt();
    //			keyOK = input.readInt();
    //			keyCancel = input.readInt();
    //			keyMenu = input.readInt();
    //			keyPadSetting = input.readInt();
    //			pad1AHold = input.readBoolean();
    //			pad1BHold = input.readBoolean();
    //			pad1ARapid = input.readBoolean();
    //			pad1BRapid = input.readBoolean();
    //			frameSkip = input.readInt();
    //			sleepEnable = input.readBoolean();
    //			vSyncWaitEnable = input.readBoolean();
    //			dispFPSEnable = input.readBoolean();
    //			APUMode1 = input.readInt();
    //			APUMode2 = input.readInt();
    //			APUVolume = input.readInt();
    //			APUBuffer = input.readInt();
    //			APUEnable = input.readBoolean();
    //			APUStereoEnable = input.readBoolean();
    //			ScreenSetting = input.readInt();
    //			busyLoopElimination = input.readBoolean();
    //			dispDebugMessage = input.readBoolean();
    //			APUSampleRate = input.readInt();
    //			ScreenRotate = input.readInt();
    //		} catch (IOException e) {
    //			e.printStackTrace();
    //			return false;
    //		} finally {
    //			try {
    //				if (input != null) {
    //					input.close();
    //				}
    //			} catch (IOException e) {
    //				e.printStackTrace();
    //				return false;
    //			}
    //		}
    //		return true;
    //	}
    //	/**
    //	 * 設定値をスクラッチパッドへ保存します。
    //	 * @return
    //	 */
    //	public static boolean save(){
    //		DataOutputStream output = null;
    //		try {
    //			output = Connector.openDataOutputStream("scratchpad:///0;pos=0");
    //			output.writeChars("CONF");
    //			output.writeInt(pad1A);
    //			output.writeInt(pad1B);
    //			output.writeInt(pad1Select);
    //			output.writeInt(pad1Start);
    //			output.writeInt(pad1Up);
    //			output.writeInt(pad1Down);
    //			output.writeInt(pad1Left);
    //			output.writeInt(pad1Right);
    //			output.writeInt(keyStateSave);
    //			output.writeInt(keyStateLoad);
    //			output.writeInt(keyUp);
    //			output.writeInt(keyDown);
    //			output.writeInt(keyOK);
    //			output.writeInt(keyCancel);
    //			output.writeInt(keyMenu);
    //			output.writeInt(keyPadSetting);
    //			output.writeBoolean(pad1AHold);
    //			output.writeBoolean(pad1BHold);
    //			output.writeBoolean(pad1ARapid);
    //			output.writeBoolean(pad1BRapid);
    //			output.writeInt(frameSkip);
    //			output.writeBoolean(sleepEnable);
    //			output.writeBoolean(vSyncWaitEnable);
    //			output.writeBoolean(dispFPSEnable);
    //			output.writeInt(APUMode1);
    //			output.writeInt(APUMode2);
    //			output.writeInt(APUVolume);
    //			output.writeInt(APUBuffer);
    //			output.writeBoolean(APUEnable);
    //			output.writeBoolean(APUStereoEnable);
    //			output.writeInt(ScreenSetting);
    //			output.writeBoolean(busyLoopElimination);
    //			output.writeBoolean(dispDebugMessage);
    //			output.writeInt(APUSampleRate);
    //			output.writeInt(ScreenRotate);
    //		} catch (IOException e) {
    //			e.printStackTrace();
    //			return false;
    //		} finally {
    //			try {
    //				output.close();
    //			} catch (IOException e) {
    //				e.printStackTrace();
    //				return false;
    //			}
    //		}
    //		return true;
    //	}
}