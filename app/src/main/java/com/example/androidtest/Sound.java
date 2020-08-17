package com.example.androidtest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.Timer;
import java.util.TimerTask;

class Sound extends TimerTask {

	private IO	io;
	private M6502 M;
	private Timer timer;
	private AudioTrack[] audioTracks = new AudioTrack[6];

	private int snd_dwSampleRate;
	private int soundBufferMs = 1000;
	private int[] dwOldPos = new int[6];
	private short[][] sbuf;
	private int CycleOld;

	private int n[] = new int[] {0, 0, 0, 0, 0, 0};
	private static final int N = 32;
	private int k[] = new int[] {0, 0, 0, 0, 0, 0};
	private int t;
	private int r[] = new int[6];
	private int rand_val[] = new int[] {0, 0, 0, 0, 0x51F631E4, 0x51F631E4};
	private short wave[] = new short[32];
	private int vol_tbl[] = new int[] {
		100, 451, 508, 573, 646, 728, 821, 925,
		1043, 1175, 1325, 1493, 1683, 1898, 2139, 2411,
		2718, 3064, 3454, 3893, 4388, 4947, 5576, 6285,
		7085, 7986, 9002, 10148, 11439, 12894, 14535, 16384,
	};

	private int sampleRateDiv25;
	private int baseClockDiv50;
	private int bufferSampleNum;
	public String debugMsg = "";

	//private int span;

	/**
	 * コンストラクタです。
	 * @param m6502
	 * @param io
	 */
	public Sound(M6502 m6502, IO io) {
		this.M = m6502;
		this.io = io;
		this.timer = new Timer();

		snd_dwSampleRate = 8000;
	}

	/**
	 * サウンド再生処理の初期化を行います。
	 * @param sampleRate
	 * @param volume
	 * @return
	 */
	public void initSound(int sampleRate, int volume, int soundBufferMs) {

		this.snd_dwSampleRate = sampleRate;
		this.soundBufferMs = soundBufferMs;

		// AudioTrackのリングバッファサイズを取得
		//int bufferSize = sampleRate * soundBufferMs / 1000 * 100;
		int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10;

		// 6チャンネル分のAutdioTrackを生成する
		for (int ch = 0; ch < 6; ch++) {
			this.audioTracks[ch] = new AudioTrack(
					AudioManager.STREAM_MUSIC,
					sampleRate,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					bufferSize,
					AudioTrack.MODE_STREAM);

//			//コールバック通知先の指定
//			this.audioTracks[ch].setPlaybackPositionUpdateListener(this);
//
//			//1024フレーム分再生ごとに通知をおこなう
//			this.audioTracks[ch].setPositionNotificationPeriod(1024);

			this.audioTracks[ch].play();
		}
//		pcm = PcmAudioHandler.getPCMHandler(Mode);
//		pcm.setSampleRate(snd_dwSampleRate);
//		pcm.setVolume(volume);

//		if (timer != null) {
//			timer.purge();
//			///timer.setListener(null);
//		}

//		timer.setTime(soundBufferMs);
//		timer.setListener(this);
//		timer.setRepeat(true);

		sbuf = new short[6][sampleRate * soundBufferMs / 1000];

		sampleRateDiv25 = snd_dwSampleRate / 25;
		baseClockDiv50 = Pce.BaseClock / 50;
		bufferSampleNum = snd_dwSampleRate * soundBufferMs / 1000;

		//span = snd_dwSampleRate * soundBufferMs / 20 / 1000;

		if (Config.APUEnable) {
			timer.scheduleAtFixedRate(this, 0, soundBufferMs / 2);
			//timer.start();
		}
	}

//	/**
//	 * サウンド再生を停止します。
//	 */
//	public void stopSound() {
//		if (timer != null) {
//			timer.purge();
//		}
//	}

//	/**
//	 * サウンド再生を開始します。
//	 */
//	public void startSound() {
//		if (Config.APUEnable) {
//			if (timer != null) {
//				//timer.start();
//			}
//		}
//	}

//	/**
//	 * サウンド再生処理を破棄します。
//	 */
//	public void trashSound() {
//		timer.purge();
//	}

//	/**
//	 * PCMをサウンドバッファに書き込みます。
//	 * @return
//	 */
//	public void writePCMBuffer() {
//
//		if (!Config.APUEnable) {
//			return;
//		}
//
//		for (int i = 0; i < 6; i++) {
//			writeSoundData(sbuf[i], i, sbuf[i].length);
//		}
//
//		// 各PSGチャネルのバッファを0番のバッファへミックスします。
//		mixBuffer(sbuf, sbuf[0].length);
//
//		// サウンドバッファの書き込み位置を初期化します。
//		for (int i = 0; i < dwOldPos.length; i++) {
//			dwOldPos[i] = 0;
//		}
//	}

//	/**
//	 * サウンドバッファを加算してミックスします。
//	 * @param buf
//	 * @param size
//	 */
//	private void mixBuffer(short[][] buf, int size) {
//		short[] buf0 = buf[0];
//		short[] buf1 = buf[1];
//		short[] buf2 = buf[2];
//		short[] buf3 = buf[3];
//		short[] buf4 = buf[4];
//		short[] buf5 = buf[5];
//		int size1 = size & ~3;
//
//		for (int i = 0; i < size1;) {
//			buf0[i] += (buf1[i] + buf2[i] + buf3[i] + buf4[i] + buf5[i]);
//			i++;
//			buf0[i] += (buf1[i] + buf2[i] + buf3[i] + buf4[i] + buf5[i]);
//			i++;
//			buf0[i] += (buf1[i] + buf2[i] + buf3[i] + buf4[i] + buf5[i]);
//			i++;
//			buf0[i] += (buf1[i] + buf2[i] + buf3[i] + buf4[i] + buf5[i]);
//			i++;
//		}
//		for (int i = size1; i < size; i++) {
//			buf0[i] += buf1[i] + buf2[i] + buf3[i] + buf4[i] + buf5[i];
//		}
//	}

	public void writePSG(int ch) {
		int Cycle;
		int dwNewPos;

		if ((M.User - CycleOld) < 0) {
			CycleOld = M.User;
		}
		Cycle = M.User - CycleOld;
		dwNewPos = sampleRateDiv25 * (Cycle >> 1) / baseClockDiv50;
		if (dwNewPos > bufferSampleNum) {
//			TRACE("sound buffer overrun\n");
			dwNewPos = snd_dwSampleRate * soundBufferMs * 3 / 4 / 1000;
		}
		if (dwNewPos > dwOldPos[ch]) {
			writeBuffer(dwOldPos[ch], ch, dwNewPos - dwOldPos[ch]);
			dwOldPos[ch] = dwNewPos;
		}
	}

	private int mseq(int[] rand_val, int index) {
		if ((rand_val[index] & 0x00080000) != 0) {
			rand_val[index] = ((rand_val[index] ^ 0x0004) << 1) + 1;
			return 1;
		} else {
			rand_val[index] <<= 1;
			return 0;
		}
	}

//	private void writeSoundData(short[] buf, int ch, int dwSize) {
//		int dwNewPos;
//
//		dwNewPos = dwSize;
//		if (dwOldPos[ch] < dwNewPos) {
//			writeBuffer(dwOldPos[ch], ch, dwNewPos - dwOldPos[ch]);
//		}
//
//		CycleOld = M.User;
//		System.arraycopy(sbuf[ch], 0, buf, 0, dwSize);
//		if (dwOldPos[ch] >= dwNewPos) {
//			if (dwOldPos[ch] >= snd_dwSampleRate*soundBufferMs / (1000 * 100 / 95)) {
//				int size = snd_dwSampleRate*soundBufferMs / 4 / 1000;
//				System.arraycopy(sbuf[ch], dwNewPos, sbuf[ch], 0, size);
//				dwOldPos[ch] = size;
//			} else {
//				System.arraycopy(sbuf[ch], dwNewPos, sbuf[ch], 0, dwOldPos[ch] - dwNewPos);
//				dwOldPos[ch] = dwOldPos[ch] - dwNewPos;
//			}
//		} else {
//			dwOldPos[ch] = 0;
//		}
//	}

	private void writeBuffer(int index, int ch, int dwSize) {
		final short[] buf = sbuf[ch];
		int dwPos;
		int lvol;
		short slvol;
		int Tp;

		if ((io.PSG[ch][4] & 0x80) == 0) {
			n[ch] = k[ch] = 0;
			for (dwPos = 0; dwPos < dwSize; dwPos++) {
				buf[index++] = 0;
			}
		} else if ((io.PSG[ch][4] & 0x40) != 0) {
			wave[0] = (short)((io.wave[ch][0] - 16) * 702);
			lvol = Math.max((io.psg_volume >> 3) & 0x1E, (io.psg_volume << 1) & 0x1E) + (io.PSG[ch][4] & 0x1F) +
				   Math.max((io.PSG[ch][5] >> 3) & 0x1E, (io.PSG[ch][5] << 1) & 0x1E);
			lvol = lvol - 60;
			if (lvol < 0) lvol = 0;
			lvol = wave[0] * vol_tbl[lvol] >> 15;
			slvol = (short)lvol;
			for (dwPos = 0; dwPos < dwSize; dwPos++) {
				buf[index++] = slvol;
			}
		} else if (ch >= 4 && ((io.PSG[ch][7] & 0x80) != 0)) {
			int Np = (io.PSG[ch][7] & 0x1F);
			lvol = Math.max((io.psg_volume >> 3) & 0x1E, (io.psg_volume << 1) & 0x1E) + (io.PSG[ch][4] & 0x1F) +
				   Math.max((io.PSG[ch][5] >> 3) & 0x1E, (io.PSG[ch][5] << 1) & 0x1E);
			lvol = lvol - 60;
			if (lvol < 0) lvol = 0;
			lvol = vol_tbl[lvol];
			for (dwPos = 0; dwPos < dwSize; dwPos++) {
				k[ch] += 3000 + (Np << 9);
				t = k[ch] / snd_dwSampleRate;
				if (t >= 1) {
					r[ch] = mseq(rand_val, ch);
					k[ch] -= snd_dwSampleRate * t;
				}
				buf[index++] = (short)((r[ch] != 0 ? 10*702 : -10*702) * lvol >> 15);
			}
		} else {
			for (int i = 0; i < 32; i++) {
				wave[i] = (short)((io.wave[ch][i] - 16) * 702);
			}

			Tp = io.PSG[ch][2] + (io.PSG[ch][3] << 8);
			if (Tp == 0) {
				for (dwPos = 0; dwPos < dwSize; dwPos++) {
					buf[index++] = 0;
				}
			} else {
				lvol = Math.max((io.psg_volume >> 3) & 0x1E, (io.psg_volume << 1) & 0x1E) + (io.PSG[ch][4] & 0x1F) +
					   Math.max((io.PSG[ch][5] >> 3) & 0x1E, (io.PSG[ch][5] << 1) & 0x1E);
				lvol = lvol-60;
				if (lvol < 0) lvol = 0;
				lvol = vol_tbl[lvol];
				for (dwPos = 0; dwPos < dwSize; dwPos++) {
					buf[index++] = (short)(wave[n[ch]]*lvol >> 15);
					k[ch] += N * 1118608 / Tp;
					t = k[ch] / (10 * snd_dwSampleRate);
					n[ch] = (n[ch] + t) & (N-1);
					k[ch] -= 10 * snd_dwSampleRate * t;
				}
			}
		}
	}

	@Override
	public void run() {

		// サウンドバッファのデータをaudioTrackに書き込む
		for (int ch = 0; ch < 6; ch++) {
			audioTracks[ch].write(sbuf[ch], 0, dwOldPos[ch], AudioTrack.WRITE_NON_BLOCKING);
			// バッファの位置を初期化
			dwOldPos[ch] = 0;
		}
	}

	public void writeAudioTrack() {
		// サウンドバッファのデータをaudioTrackに書き込む
		for (int ch = 0; ch < 6; ch++) {
			audioTracks[ch].write(sbuf[ch], 0, dwOldPos[ch], AudioTrack.WRITE_NON_BLOCKING);
			// バッファの位置を初期化
			dwOldPos[ch] = 0;
		}
	}
}