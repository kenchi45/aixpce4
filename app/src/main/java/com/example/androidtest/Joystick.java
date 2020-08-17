package com.example.androidtest;

class Joystick {
	
	public static final int JOY_A = 1;
	public static final int JOY_B = 2;
	public static final int JOY_SELECT = 4;
	public static final int JOY_START = 8;
	public static final int JOY_UP = 0x10;
	public static final int JOY_DOWN = 0x40;
	public static final int JOY_LEFT = 0x80;
	public static final int JOY_RIGHT = 0x20;
	private int rapidCount = 0;

	public int getState() {
		int joy = 0;

		// TODO: キー入力
		int state = 0;//canvas.getKeypadState();
		
		state = PadRapidAndHold(state);
		
		if ((state & Config.pad1A) != 0) {
			joy |= JOY_A;
		}
		if ((state & Config.pad1B) != 0) {
			joy |= JOY_B;
		}
		if ((state & Config.pad1Select) != 0) {		
			joy |= JOY_SELECT;
		}
		if ((state & Config.pad1Start) != 0) {
			joy |= JOY_START;
		}
		if ((state & Config.pad1Up) != 0) {
			joy |= JOY_UP;
		}
		if ((state & Config.pad1Down) != 0) {
			joy |= JOY_DOWN;
		}
		if ((state & Config.pad1Left) != 0) {
			joy |= JOY_LEFT;
		}
		if ((state & Config.pad1Right) != 0) {
			joy |= JOY_RIGHT;
		}
		
		return joy;
	}
		
	/**
	 * 設定値に応じて、キーステートに連射およびホールド状態を反映させます。
	 * @param keyState
	 * @return
	 */
	public int PadRapidAndHold(int keyState) {
		rapidCount = (rapidCount + 1) & 3;

		if (Config.pad1AHold && Config.pad1ARapid && (keyState & Config.pad1A) != 0) {
			keyState |= Config.pad1A;
		} else 	if (Config.pad1AHold) {
			keyState |= Config.pad1A;
			if (Config.pad1ARapid && (keyState & Config.pad1A) != 0) {
				if (rapidCount != 0) {
					keyState ^= Config.pad1A;
				}
			}
		}
		
		if (Config.pad1BHold && Config.pad1BRapid && (keyState & Config.pad1B) != 0) {
			keyState |= Config.pad1B;
		} else 	if (Config.pad1BHold) {
			keyState |= Config.pad1B;
			if (Config.pad1BRapid && (keyState & Config.pad1B) != 0) {
				if (rapidCount != 0) {
					keyState ^= Config.pad1B;
				}
			}
		}
		
		return keyState;
	}
}
