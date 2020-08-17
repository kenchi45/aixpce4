package com.example.androidtest;

public class PCEApu {

//	private final PSGi psgi_;

	private final int NUM_OF_TRACKS = 6;

	private int[] freq_ = new int[NUM_OF_TRACKS];

	private int[] firstByte_ = new int[NUM_OF_TRACKS];

	private int[] volume_ = new int[NUM_OF_TRACKS];

	private boolean skip_ = false;

	private int curCh_ = 0;



//	PCEApu ( PSGi psgi ) {
//		psgi_ = psgi;
//	}

	public void init() {
		freq_ = new int[NUM_OF_TRACKS];
		firstByte_ = new int[NUM_OF_TRACKS];
		volume_ = new int[NUM_OF_TRACKS];
	}


	public void writeReg(int value, int V){
		switch (value) {
			case 0:
				if (V > (NUM_OF_TRACKS - 1)) {
					skip_ = true;
					return;
				} else {
					curCh_ = V & 7;
					skip_ = false;
				}
				break;
			case 1:
//				psgi_.setMasterVolume(V);
				break;
			case 2:
				if (skip_) return;
				firstByte_[curCh_] = V & 0xFF ;
				break;
			case 3:
				if (skip_) return;
				setFreq(curCh_, V);
				break;
			case 4:
				if (skip_) return;
				setVolume(curCh_, V);
				break;
			case 5:
				if (skip_) return;
				if ((V & 0xF0) == 0)
					setVolume(curCh_, 0);
				break;
			default:
				break;
		}
	}

	private void setFreq(int ch, int value) {
		int i = (value  & 0xFF) << 8 | firstByte_[curCh_];
		freq_[curCh_] = i > 0 ? 3585000 / i / 32 : 1;
	}

	private void setVolume(int ch, int value) {
		volume_[curCh_] = (value & 0xF) * 3;
	}

	public void doFrame() {
//		for (int i = 0; i < NUM_OF_TRACKS; i++){
//			psgi_.setChFreq(i, freq_[i]);
//			psgi_.setChVolume(i, volume_[i]);
//		}
//		psgi_.doFrame();
	}

}