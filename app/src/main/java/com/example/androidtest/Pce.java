package com.example.androidtest;

import android.content.Context;
import android.graphics.Color;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

class Pce {
	public static final int WIDTH = 360+64;
	public static final int HEIGHT = 256;

	public int Debug;
	public int vmode;
	public static final int BaseClock = 7170000;
	//public int UPeriod = 1;
	public String CartName = null;
	public boolean snd_bSound = true;
	public int[] XBuf = new int[WIDTH * HEIGHT];
	public int TimerPeriod;

	public byte[] RAM = new byte[0x8000];
	//public byte[] PopRAM = new byte[0x10000];
	public byte[][] Page = new byte[8][];
	public byte[][] ROMMap = new byte[256][];
	public byte[] VRAM;
	private byte[] vchange;
	private byte[] vchanges;
//	private byte[] PCM;
	private byte[] DMYROM;
	private byte[][] ROM;
	private byte[] WRAM;
	private byte[] IOAREA;
	private short[] SPRAM = new short[64*4];
	private byte[] SPM = new byte[(360+64)*256];
	private int[] VRAM2;
	private int[] VRAMS;
	private int ROM_size;
	private int Country;
	private int IPeriod;
//	private int TimerCount, CycleOld;
	private int scanlines_per_frame  = 263;
	private int scanline;
	private int BGONSwitch = 1, SPONSwitch = 1;
//	private boolean cart_reload;
//	private boolean populus = false;
	private int scroll = 0;
	private int[] Pal = new int[512];
	private int ScrollYDiff;
	private int oldScrollX;
	private int oldScrollY;
	private int oldScrollYDiff;
//	private int Black = 0;
	private int UCount = 0;
//	private int ACount = 0;
	private int prevline;
	private static final int[] bgw = new int[] {32,64,128,128};
	private static final int[] incsize = new int[] {1,32,64,128};
	private int usespbg = 0;
	public boolean isRefreshedScreen = false;
	public boolean isLastLine = true;
	private int[] palTable = new int[256];
	private M6502 cpu;
	public IO io;
	private Debug debug;
	private Sound sound;
	private Render render;
	private Joystick joystick;
	private PCEApu audio;


	public Pce(M6502 cpu, IO io, Sound sound, Joystick joystick, Debug debug, Render render, PCEApu audio) {
		this.cpu = cpu;
		this.io = io;
		this.sound = sound;
		this.joystick = joystick;
		this.debug = debug;
		this.render = render;
		this.audio = audio;

		IPeriod = BaseClock / (scanlines_per_frame * 60);
		debug.TRACE("IPeriod = %d\n", IPeriod);

		TimerPeriod = BaseClock / 1000 * 3 * 1024 / 21480;
		debug.TRACE("TimerPeriod = %d\n", TimerPeriod);
		vmode = 0;
		Debug = 0;

		CreatePaletteTable();
	}

	public void CreatePaletteTable() {
		for (int i = 0; i < palTable.length; i++)
		{
			palTable[i] = ((((i >> 2) & 7) * 36) << 16) & 0x00FF0000;
			palTable[i] |= (((i >> 5) * 36) << 8) & 0x0000FF00;
			palTable[i] |= (((i & 3) * 84) << 0) & 0x000000FF;
		}

		// パレットデータ配列のRGB値を、携帯画面の色深度の深さにあわせてスケールした
		// 値で再設定します
		for (int i = 0; i < palTable.length; i++) {
			int rgb = palTable[i];
			palTable[i] = Color.argb(
					255,
					(rgb >> 16) & 0xff,
					(rgb >> 8) & 0xff,
					rgb & 0xff);
		}
	}

	public void bank_set(int P, int V)
	{
	//	if (V>=ROM_size && V<0xF7) {
	//	}
		if (ROMMap[V] == IOAREA)
			Page[P] = IOAREA;
		else
			Page[P] = ROMMap[V];// - P * 0x2000;
	}

	public int _Rd6502(int A)
	{
		if ((Page[A>>13])!=IOAREA) return (Page[A>>13][A-(A&~0x1FFF)]&0xFF);
		else return IO_read(A);
	}

	public void _Wr6502(int A,int V)
	{
		if (Page[A>>13]!=IOAREA)
		{
			Page[A>>13][A-(A&~0x1FFF)] = (byte)V;
		}
		else IO_write(A,V);
	}

	public void IO_write(int A,int V) {
		switch (A & 0x1C00) {
			case 0x0000:
				switch (A & 3) {
					case 0:
						io.vdc_reg = (byte)(V & 31);
						return;

					case 1:
						return;

					case 2:
						switch (io.vdc_reg) {
							case VDC_REG.VWR:
								io.vdc_ratch = (byte)V;
								return;

							case VDC_REG.HDR:
								io.screen_w = (V+1)*8;
								break;

							case VDC_REG.MWR:
							{
								io.bg_h=((V&0x40)!=0)?64:32;
								io.bg_w=bgw[(V>>4)&3];
							}
								debug.TRACE("bg:%dx%d, V:%X\n",io.bg_w,io.bg_h, V);
								break;

							case VDC_REG.BYR:
								if (scroll == 0) {
									oldScrollX = io.VDC[VDC_REG.BXR];
									oldScrollY = io.VDC[VDC_REG.BYR];
									oldScrollYDiff = ScrollYDiff;
								}
								io.VDC[VDC_REG.BYR]=(io.VDC[VDC_REG.BYR]&0xFF00)|V;
								scroll=1;
								ScrollYDiff=scanline-1;
								return;

							case VDC_REG.BXR:
								if (scroll == 0) {
									oldScrollX = io.VDC[VDC_REG.BXR];
									oldScrollY = io.VDC[VDC_REG.BYR];
									oldScrollYDiff = ScrollYDiff;
								}
								io.VDC[VDC_REG.BXR] = ((io.VDC[VDC_REG.BXR] & 0xFF00) | V) & 0xFFFF;
								scroll=1;
								return;

							case VDC_REG.VPR: debug.TRACE("VPR %X\n",V);break;
							case VDC_REG.VDW: debug.TRACE("VDW %X\n",V);break;
							case VDC_REG.VCR: debug.TRACE("VCR %X\n",V);break;
							case VDC_REG.DCR: debug.TRACE("DCR %X\n",V);break;

						}
						io.VDC[io.vdc_reg] = ((io.VDC[io.vdc_reg] & 0xFF00) | V) & 0xFFFF;
						if (io.vdc_reg>19) {
							debug.TRACE("ignore write lo vdc%d,%02x\n",io.vdc_reg,V);
						}
						return;

					case 3:
						switch (io.vdc_reg) {
							case VDC_REG.VWR:
								VRAM[io.VDC[VDC_REG.MAWR]<<1] = io.vdc_ratch;
								VRAM[(io.VDC[VDC_REG.MAWR]<<1)+1] = (byte)V;
								vchange[io.VDC[VDC_REG.MAWR]>>4] = 1;
								vchanges[io.VDC[VDC_REG.MAWR]>>6] = 1;
								io.VDC[VDC_REG.MAWR] = (io.VDC[VDC_REG.MAWR] + io.vdc_inc) & 0xFFFF;
								io.vdc_ratch = 0;
								return;

							case VDC_REG.VDW:
								io.VDC[VDC_REG.VDW] = ((io.VDC[VDC_REG.VDW] & 0xFF) | (V << 8)) & 0xFFFF;
								io.screen_h = (io.VDC[VDC_REG.VDW]&511)+1;
								io.maxline = io.screen_h-1;
								debug.TRACE("VDWh: %X\n", io.VDC[VDC_REG.VDW]);
								return;

							case VDC_REG.LENR:
								io.VDC[VDC_REG.LENR] = ((io.VDC[VDC_REG.LENR] & 0xFF)|(V << 8)) & 0xFFFF;
								debug.TRACE("DMA:%04x %04x %04x\n",io.VDC[VDC_REG.DISTR],io.VDC[VDC_REG.SOUR],io.VDC[VDC_REG.LENR]);
								System.arraycopy(VRAM, io.VDC[VDC_REG.SOUR]<<1, VRAM, io.VDC[VDC_REG.DISTR]<<1, (io.VDC[VDC_REG.LENR]+1)<<1);
								Util.memsetByte(vchange, io.VDC[VDC_REG.DISTR]>>4, (io.VDC[VDC_REG.LENR]+1)>>4, (byte)1);
								Util.memsetByte(vchange, io.VDC[VDC_REG.DISTR]>>6, (io.VDC[VDC_REG.LENR]+1)>>6, (byte)1);
								io.VDC[VDC_REG.DISTR] = (io.VDC[VDC_REG.DISTR] + io.VDC[VDC_REG.LENR] + 1) & 0xFFFF;
								io.VDC[VDC_REG.SOUR] = (io.VDC[VDC_REG.SOUR] + io.VDC[VDC_REG.LENR] + 1) & 0xFFFF;
								io.vdc_status|=0x10;
								return;

							case VDC_REG.CR :
								io.vdc_inc = (char)incsize[(V>>3)&3];
								break;

							case VDC_REG.HDR:
								debug.TRACE("HDRh\n");
								break;

							case VDC_REG.BYR:
								if (scroll == 0) {
									oldScrollX = io.VDC[VDC_REG.BXR];
									oldScrollY = io.VDC[VDC_REG.BYR];
									oldScrollYDiff = ScrollYDiff;
								}
								io.VDC[VDC_REG.BYR] = ((io.VDC[VDC_REG.BYR] & 0xFF)|((V & 1) << 8)) & 0xFFFF;
								scroll=1;
								ScrollYDiff = scanline-1;
								return;

							case VDC_REG.SATB:
								io.VDC[VDC_REG.SATB] = ((io.VDC[VDC_REG.SATB] & 0xFF)|(V << 8)) & 0xFFFF;
								io.vdc_satb = 1;
								io.vdc_status &= ~0x08;
								return;

							case VDC_REG.BXR:
								if (scroll == 0) {
									oldScrollX = io.VDC[VDC_REG.BXR];
									oldScrollY = io.VDC[VDC_REG.BYR];
									oldScrollYDiff = ScrollYDiff;
								}
								io.VDC[VDC_REG.BXR] = ((io.VDC[VDC_REG.BXR] & 0xFF) |((V & 3) << 8)) & 0xFFFF;
								scroll=1;
								return;

							case VDC_REG.VPR:
								debug.TRACE("VPR %X\n",V);
								break;

							case VDC_REG.VCR:
								debug.TRACE("VCR %X\n",V);
								break;

							case VDC_REG.DCR:
								debug.TRACE("DCR %X\n",V);
								break;
						}

						io.VDC[io.vdc_reg] = ((io.VDC[io.vdc_reg] & 0xFF) | (V << 8)) & 0xFFFF;
						if (io.vdc_reg>19) {
							debug.TRACE("ignore write hi vdc%d,%02x\n",io.vdc_reg,V);
						}
						return;
				}
				break;

			case 0x0400:
				switch (A & 7) {
				case 0:
					debug.TRACE("VCE 0, V=%X\n", V);
					return;

				case 2:
					io.vce_reg=(io.vce_reg&0xFF00)|V;
					return;

				case 3:
					io.vce_reg=(io.vce_reg&0xFF)|((V&1)<<8);
					return;

				case 4:
					io.VCE[io.vce_reg] = (io.VCE[io.vce_reg] & 0xFF00) | V;
					{
						int n = io.vce_reg;
						int c = (io.VCE[n] >> 1);
						c = palTable[c & 0xFF];
						if (n == 0) {
							for(int i = 0; i < 256; i += 16) {
								Pal[i] = c;
							}
						} else if ((n & 15) != 0) {
							Pal[n] = c;
						}
					}
					return;

				case 5:
					io.VCE[io.vce_reg] = (io.VCE[io.vce_reg] & 0xFF) | (V << 8);
					{
						int n = io.vce_reg;
						int c = (io.VCE[n] >> 1);
						c = palTable[c & 0xFF];
						if (n == 0) {
							for (int i = 0; i < 256; i += 16) {
								Pal[i] = c;
							}
						} else if ((n & 15) != 0) {
							Pal[n] = c;
						}
					}
					io.vce_reg = (io.vce_reg + 1) & 0x1FF;
					return;

				case 1:
					debug.TRACE("VCE 1, V=" + V);
					return;

				case 6:
					debug.TRACE("VCE 6, V=" + V);
					return;

				case 7:
					debug.TRACE("VCE 7, V=" + V);
					return;
				}
				break;

			case 0x0800:
				audio.writeReg(A&0xF,V);
				if (snd_bSound && io.psg_ch < 6)
				{
					if ((A&15) <= 1)
					{
						int	i;
						for (i = 0; i < 6; i++)
							sound.writePSG(i);
					}
					else
						sound.writePSG(io.psg_ch);
				}
				switch (A & 15) {
					case 0:
						io.psg_ch = V & 7;
						return;

					case 1:
						io.psg_volume = V;
						return;

					case 2:
						io.PSG[io.psg_ch][2] = V;
						break;

					case 3:
						io.PSG[io.psg_ch][3] = V & 15;
						break;

					case 4:
						io.PSG[io.psg_ch][4] = V;
						break;

					case 5:
						io.PSG[io.psg_ch][5] = V;
						break;

					case 6:
						if ((io.PSG[io.psg_ch][4] & 0x40) != 0) {
							io.wave[io.psg_ch][0]=V&31;
						} else {
							io.wave[io.psg_ch][io.wavofs[io.psg_ch]]=V&31;
							io.wavofs[io.psg_ch]=(io.wavofs[io.psg_ch]+1)&31;
						}
						break;

					case 7:
						io.PSG[io.psg_ch][7] = V;
						break;

					case 8:
						io.psg_lfo_freq = V;
						break;

					case 9:
						io.psg_lfo_ctrl = V;
						break;

					default:
						debug.TRACE("ignored PSG write\n");
				}
				return;

			case 0x0c00:
				switch (A & 1) {
					case 0:
						io.timer_reload = V&127;
						return;
					case 1:
						V &= 1;
						if ((V!=0) && (io.timer_start==0)) {
							io.timer_counter = io.timer_reload;
						}
						io.timer_start = V;
						return;
				}
				break;

			case 0x1000:
				io.joy_select = V & 1;
				if ((V & 2) != 0) {
					io.joy_counter = 0;
				}
				return;

			case 0x1400:
				switch (A&15) {
					case 2:
						io.irq_mask = (byte)V;
						return;
					case 3:
						io.irq_status= (byte)((io.irq_status&~4)|(V&0xF8));
						return;
				}
				break;

			case 0x1800:
				switch (A & 15) {
					case 7:
						io.backup = 1;
						return;
				}
				break;
		}
		debug.TRACE("ignore I/O write %04x,%02x\n",A,V);
	}

	public int IO_read(int A)
	{
		int ret;

	  switch(A&0x1C00){
	  case 0x0000:
	  	switch (A & 3){
		case 0:
			ret = io.vdc_status;
			io.vdc_status=0;
			return ret;
		case 1:
			return 0;
		case 2:
			if (io.vdc_reg==2)
				return VRAM[io.VDC[VDC_REG.MARR] << 1] & 0xFF;
			else return ((io.VDC[io.vdc_reg])&0xFF);
		case 3:
			if (io.vdc_reg==2) {
				ret = VRAM[(io.VDC[VDC_REG.MARR] << 1) + 1] & 0xFF;
				io.VDC[VDC_REG.MARR]+=io.vdc_inc;

				return ret;
			} else return ((io.VDC[io.vdc_reg])>>8);
		}
		break;

	  case 0x0400:
	  	switch(A&7){
		case 4: return ((io.VCE[io.vce_reg]) & 0xFF);
		case 5: return ((io.VCE[io.vce_reg++]) >> 8);
		}
		break;

	  case 0x0800:
		switch (A&15) {
		case 0: return io.psg_ch;
		case 1: return io.psg_volume;
		case 2: return io.PSG[io.psg_ch][2];
		case 3: return io.PSG[io.psg_ch][3];
		case 4: return io.PSG[io.psg_ch][4];
		case 5: return io.PSG[io.psg_ch][5];
		case 6:
			{
				int	ofs=io.wavofs[io.psg_ch];
				io.wavofs[io.psg_ch]=(io.wavofs[io.psg_ch]+1) & 31;
				return io.wave[io.psg_ch][ofs];
			}
		case 7: return io.PSG[io.psg_ch][7];
		case 8: return io.psg_lfo_freq;
		case 9: return io.psg_lfo_ctrl;
		default: return 0xff;
		}

	  case 0x0c00:
		return io.timer_counter;

	  case 0x1000:

			ret = io.JOY[io.joy_counter] ^ 0xff;
			if ((io.joy_select&1)!=0) ret >>= 4;
			else { ret &= 15; io.joy_counter = (io.joy_counter + 1) % 5; }
			return ret | Country;

	  case 0x1400:
		switch(A&15){
		case 2: return io.irq_mask;
		case 3: ret = io.irq_status;io.irq_status = 0; return ret;
		}
		break;

	  case 0x1800:
		switch(A&15){
		case 3: return io.backup = 0;

		}
		break;
	  }
		debug.TRACE("ignore I/O read %04x\n",A);

		return 0xff;
	}

	public int Loop6502()
	{
		int dispmin, dispmax;
		int ret;

		dispmin = (io.maxline-io.minline>227 ? io.minline+((io.maxline-io.minline-227+1)>>1) : io.minline);
		dispmax = (io.maxline-io.minline>227 ? io.maxline-((io.maxline-io.minline-227+1)>>1) : io.maxline);

		scanline = (scanline + 1) % scanlines_per_frame;

		ret = 0;
		io.vdc_status &= ~VDC_Status.VDC_RasHit;
		if (scanline > io.maxline)
			io.vdc_status |= VDC_Status.VDC_InVBlank;

		if (scanline == io.minline) {
			io.vdc_status &= ~VDC_Status.VDC_InVBlank;
			prevline = dispmin;
			ScrollYDiff = 0;
			oldScrollYDiff = 0;

		} else if (scanline == io.maxline) {
			isLastLine = true;

			if (CheckSprites() != 0) {
				io.vdc_status |= VDC_Status.VDC_SpHit;
			} else {
				io.vdc_status &= ~VDC_Status.VDC_SpHit;
			}

			if (UCount != 0) {
				UCount--;
			} else {
				if (((io.VDC[VDC_REG.CR]&0x40) != 0) && (SPONSwitch!=0)) RefreshSprite(prevline,dispmax,0);
				RefreshLine(prevline,dispmax-1);
				if (((io.VDC[VDC_REG.CR]&0x40) != 0) && (SPONSwitch!=0)) RefreshSprite(prevline,dispmax,1);
				prevline=dispmax;
				UCount=Config.frameSkip;
				RefreshScreen();
			}

			// サウンド再生
			//audio.doFrame();
			//sound.writeAudioTrack();

			if (Config.sleepEnable) {
				// 1/60秒のタイミングを待ってスリープする
				sleep();
			}
		}
		if (scanline >= io.minline && scanline <= io.maxline) {
			if (scanline == (io.VDC[VDC_REG.RCR] & 1023) - 64) {
				if (((io.VDC[VDC_REG.CR]&0x04) != 0) && (UCount == 0) && dispmin <= scanline && scanline <= dispmax) {
					if (((io.VDC[VDC_REG.CR] & 0x40) != 0) && (SPONSwitch != 0)) RefreshSprite(prevline,scanline, 0);
					RefreshLine(prevline, scanline - 1);
					if (((io.VDC[VDC_REG.CR] & 0x40) != 0) && (SPONSwitch != 0)) RefreshSprite(prevline, scanline, 1);
					prevline = scanline;
				}
				io.vdc_status |= VDC_Status.VDC_RasHit;
				if (((io.VDC[VDC_REG.CR] & 0x04) != 0)) {

					ret = 1;
				}
			} else if (scroll != 0) {
				if (scanline - 1 > prevline && UCount == 0) {
					int	tmpScrollX, tmpScrollY, tmpScrollYDiff;
					tmpScrollX = io.VDC[VDC_REG.BXR];
					tmpScrollY = io.VDC[VDC_REG.BYR];
					tmpScrollYDiff = ScrollYDiff;
					io.VDC[VDC_REG.BXR] = oldScrollX;
					io.VDC[VDC_REG.BYR] = oldScrollY;
					ScrollYDiff = oldScrollYDiff;
					if (((io.VDC[VDC_REG.CR] & 0x40) != 0) && (SPONSwitch != 0)) RefreshSprite(prevline, scanline - 1, 0);
					RefreshLine(prevline,scanline-2);
					if (((io.VDC[VDC_REG.CR]&0x40)!=0) && (SPONSwitch!=0)) RefreshSprite(prevline,scanline-1,1);
					prevline=scanline - 1;
					io.VDC[VDC_REG.BXR] = tmpScrollX;
					io.VDC[VDC_REG.BYR] = tmpScrollY;
					ScrollYDiff = tmpScrollYDiff;
				}
			}
		} else {
			int rcr = (io.VDC[VDC_REG.RCR] & 1023) - 64;
			if (scanline == rcr)
			{
				if ((io.VDC[VDC_REG.CR] & 0x04) != 0) {
					io.vdc_status |= 0x04;
					ret = 1;
				}
			}
		}
		scroll = 0;
		if (scanline == io.maxline + 1) {

			int J = joystick.getState();
			if ((J & 0x10000) != 0) return 3;
			io.JOY[0] = J;

			if (io.vdc_satb == 1 || (io.VDC[VDC_REG.DCR]&0x0010) != 0)
			{
				//System.arraycopy(VRAM, io.VDC[VDC_REG.SATB]*2, SPRAM, 0, 64*8);
				int start = io.VDC[VDC_REG.SATB] << 1;
				int end = start + 64*8;
				for (int i = start, j = 0; i < end; i += 2, j++) {
					SPRAM[j] = (short)((VRAM[i] & 0xFF) | ((VRAM[i+1] & 0xFF) << 8));
				}
				io.vdc_satb=1;
				io.vdc_status&=~0x08;
			}
			if (ret==1)
				io.vdc_pendvsync = 1;
			else {
				if ((io.VDC[VDC_REG.CR] & 0x08) != 0) {
					ret = 1;
				}
			}
		}
		else
		if (scanline == Util.min(io.maxline + 5, scanlines_per_frame - 1)) {
			if (io.vdc_satb != 0) {
				io.vdc_status |= VDC_Status.VDC_SATBfinish;
				io.vdc_satb = 0;
				if ((io.VDC[VDC_REG.DCR] & 0x01) != 0) {
					ret = 1;
				}
			}
		} else if (io.vdc_pendvsync!=0 && ret != 1) {
			io.vdc_pendvsync = 0;

			if ((io.VDC[VDC_REG.CR] & 0x08) != 0) {

				ret = 1;
			}
		}
		if (ret == 1) {
			if ((io.irq_mask & 2) == 0) {
				io.irq_status |= 2;

				return ret;
			}
		}
		return 0;
	}

	public int TimerInt()
	{
		if (io.timer_start!=0) {
			io.timer_counter = (io.timer_counter - 1) & 0xFF;
			if (io.timer_counter > 128) {
				io.timer_counter = io.timer_reload;

				if ((io.irq_mask & 4) == 0) {
					io.irq_status |= 4;
					return 4;
				}
			}
		}
		return 0;
	}

	public int CheckSprites()
	{
		int i,x0,y0,w0,h0,x,y,w,h;
		int sprY;
		int sprX;
		int sprAtr;
		int index = 0;

		y0 = SPRAM[index];
		x0 = SPRAM[index + 1];
		sprAtr = SPRAM[index + 3];

		w0 = (((sprAtr>>8 ) & 1) + 1) << 4;
		h0 = (((sprAtr>>12) & 3) + 1) << 4;
		index++;
		for (i = 1; i < 64; i++,index += 4) {
			sprY = SPRAM[index];
			sprX = SPRAM[index + 1];
			sprAtr = SPRAM[index + 3];

			w = (((sprAtr >> 8 ) & 1) + 1) << 4;
			h = (((sprAtr >> 12) & 3) + 1) << 4;
			if ((sprX < x0 + w0) && (sprX + w > x0) && (sprY < y0 + h0) && (sprY + h > y0)) return 1;
		}
		return 0;
	}

	private void plane2pixel(int no)
	{
		int M;
		int C = no << 5;
		int L;
		int C2 = no << 3;

		for(int j = 0; j < 8; j++, C += 2, C2++) {
			M = VRAM[C];
			L = ((M&0x88)>>>3)|((M&0x44)<<6)|((M&0x22)<<15)|((M&0x11)<<24);
			M = VRAM[C + 1];
			L |= ((M&0x88)>>>2)|((M&0x44)<<7)|((M&0x22)<<16)|((M&0x11)<<25);
			M = VRAM[C + 16];
			L |= ((M&0x88)>>>1)|((M&0x44)<<8)|((M&0x22)<<17)|((M&0x11)<<26);
			M = VRAM[C + 17];
			L |= ((M&0x88))|((M&0x44)<<9)|((M&0x22)<<18)|((M&0x11)<<27);
			VRAM2[C2] = L;
		}
	}

	private void sp2pixel(int no)
	{
		int M;
		int C = no << 7;
		int C2 = no << 5;
		int i;

		for (i=0; i < 32; i++, C++, C2++) {
			int L;
			M = VRAM[C];
			L =((M&0x88)>>>3)|((M&0x44)<<6)|((M&0x22)<<15)|((M&0x11)<<24);
			M = VRAM[C + 32];
			L |= ((M&0x88)>>>2)|((M&0x44)<<7)|((M&0x22)<<16)|((M&0x11)<<25);
			M = VRAM[C + 64];
			L |= ((M&0x88)>>>1)|((M&0x44)<<8)|((M&0x22)<<17)|((M&0x11)<<26);
			M = VRAM[C + 96];
			L |= ((M&0x88))|((M&0x44)<<9)|((M&0x22)<<18)|((M&0x11)<<27);
			VRAMS[C2 + 0] = L;
		}
	}

	public void RefreshLine(int Y1, int Y2)
	{
		Y2++;
		int PP = (360 + 64) * (256 - 256) / 2 + ((360 + 64) - io.screen_w) / 2 + (360 + 64) * Y1;

		if (((io.VDC[VDC_REG.CR] & 0x80) != 0) && (BGONSwitch != 0)) {

			int y = Y1 + io.VDC[VDC_REG.BYR] - ScrollYDiff;
			int offset = y & 7;
			int h = 8 - offset;
			if (h > Y2 - Y1) h = Y2 - Y1;
			y >>= 3;
			PP -= io.VDC[VDC_REG.BXR] & 7;
			int XW = (io.screen_w >> 3) + 1;

			for (int Line = Y1; Line < Y2; y++) {
				int x = io.VDC[VDC_REG.BXR] >> 3;
				y &= io.bg_h - 1;
				for (int X1 = 0; X1 < XW; X1++, x++, PP += 8) {
					x &= io.bg_w-1;
					int index = (x + y * io.bg_w) << 1;
					int no = (VRAM[index] & 0xFF) | ((VRAM[index + 1] & 0xFF) << 8);
					int R = (no >> 12) << 4;
					no &= 0xFFF;
					if ((vchange[no] & 0xFF) != 0) {
						vchange[no] = 0;
						plane2pixel(no);
					}
					int C2 = (no << 3) + offset;
					int C = (no << 5) + (offset << 1);
					int P = PP;

					for (int i = 0; i < h; i++, P += (360 + 64), C2++, C += 2) {

						int J = (VRAM[C] | VRAM[C + 1] | VRAM[C + 16] | VRAM[C +17]);
						if (J == 0) continue;

						int L = VRAM2[C2];
						if ((J & 0x80) != 0) XBuf[P    ] = Pal[R + ((L >>>  4) & 15)];
						if ((J & 0x40) != 0) XBuf[P + 1] = Pal[R + ((L >>> 12) & 15)];
						if ((J & 0x20) != 0) XBuf[P + 2] = Pal[R + ((L >>> 20) & 15)];
						if ((J & 0x10) != 0) XBuf[P + 3] = Pal[R + ((L >>> 28)     )];
						if ((J & 0x08) != 0) XBuf[P + 4] = Pal[R + ((L       ) & 15)];
						if ((J & 0x04) != 0) XBuf[P + 5] = Pal[R + ((L >>>  8) & 15)];
						if ((J & 0x02) != 0) XBuf[P + 6] = Pal[R + ((L >>> 16) & 15)];
						if ((J & 0x01) != 0) XBuf[P + 7] = Pal[R + ((L >>> 24) & 15)];
					}
				}
				Line += h;
				PP += (360 + 64) * h - (XW << 3);
				offset = 0;
				h = Y2 - Line;
				if (h > 8) {
					h = 8;
				}
			}
	  	}
	}

	private void PutSprite(int Pindex, int Cindex, int C2index, int Rindex, int h, int inc)
	{
		int[] P = XBuf;
		byte[] C = VRAM;
		int[] C2 = VRAMS;
		int[] R = Pal;
		int i, J;
		int L;
		for (i = 0; i < h; i++, Cindex += inc, C2index += inc, Pindex += (360 + 64)) {
			J = ((C[Cindex    ] | C[Cindex + 32] | C[Cindex + 64] | C[Cindex + 96]) & 0xFF)
			  | ((C[Cindex + 1] | C[Cindex + 33] | C[Cindex + 65] | C[Cindex + 97]) << 8);
			if (J == 0) continue;
			L = C2[C2index + 1];
			if ((J & 0x8000) != 0) P[Pindex     ] = R[Rindex + ((L >>> 4 ) & 15)];
			if ((J & 0x4000) != 0) P[Pindex +  1] = R[Rindex + ((L >>> 12) & 15)];
			if ((J & 0x2000) != 0) P[Pindex +  2] = R[Rindex + ((L >>> 20) & 15)];
			if ((J & 0x1000) != 0) P[Pindex +  3] = R[Rindex + ((L >>> 28)     )];
			if ((J & 0x0800) != 0) P[Pindex +  4] = R[Rindex + ((L       ) & 15)];
			if ((J & 0x0400) != 0) P[Pindex +  5] = R[Rindex + ((L >>> 8 ) & 15)];
			if ((J & 0x0200) != 0) P[Pindex +  6] = R[Rindex + ((L >>> 16) & 15)];
			if ((J & 0x0100) != 0) P[Pindex +  7] = R[Rindex + ((L >>> 24) & 15)];
			L = C2[C2index];
			if ((J &   0x80) != 0) P[Pindex +  8] = R[Rindex + ((L >>> 4 ) & 15)];
			if ((J &   0x40) != 0) P[Pindex +  9] = R[Rindex + ((L >>> 12) & 15)];
			if ((J &   0x20) != 0) P[Pindex + 10] = R[Rindex + ((L >>> 20) & 15)];
			if ((J &   0x10) != 0) P[Pindex + 11] = R[Rindex + ((L >>> 28)     )];
			if ((J &   0x08) != 0) P[Pindex + 12] = R[Rindex + ((L       ) & 15)];
			if ((J &   0x04) != 0) P[Pindex + 13] = R[Rindex + ((L >>> 8 ) & 15)];
			if ((J &   0x02) != 0) P[Pindex + 14] = R[Rindex + ((L >>> 16) & 15)];
			if ((J &   0x01) != 0) P[Pindex + 15] = R[Rindex + ((L >>> 24) & 15)];
		}
	}

	private void PutSpriteHflip(int Pindex, int Cindex, int C2index, int Rindex, int h, int inc)
	{
		int[] P = XBuf;
		byte[] C = VRAM;
		int[] C2 = VRAMS;
		int[] R = Pal;
		int i, J;
		int L;
		for(i = 0; i < h; i++, Cindex += inc, C2index += inc, Pindex += (360 + 64)) {
			J = ((C[Cindex    ] | C[Cindex + 32] | C[Cindex + 64] | C[Cindex + 96]) & 0xFF)
			  | ((C[Cindex + 1] | C[Cindex + 33] | C[Cindex + 65] | C[Cindex + 97]) << 8);

			if (J == 0) continue;
			L = C2[C2index + 1];
			if ((J & 0x8000) != 0) P[Pindex + 15] = R[Rindex + ((L >>> 4 ) & 15)];
			if ((J & 0x4000) != 0) P[Pindex + 14] = R[Rindex + ((L >>> 12) & 15)];
			if ((J & 0x2000) != 0) P[Pindex + 13] = R[Rindex + ((L >>> 20) & 15)];
			if ((J & 0x1000) != 0) P[Pindex + 12] = R[Rindex + ((L >>> 28)     )];
			if ((J & 0x0800) != 0) P[Pindex + 11] = R[Rindex + ((L       ) & 15)];
			if ((J & 0x0400) != 0) P[Pindex + 10] = R[Rindex + ((L >>> 8 ) & 15)];
			if ((J & 0x0200) != 0) P[Pindex +  9] = R[Rindex + ((L >>> 16) & 15)];
			if ((J & 0x0100) != 0) P[Pindex +  8] = R[Rindex + ((L >>> 24) & 15)];
			L = C2[C2index];
			if ((J &   0x80) != 0) P[Pindex +  7] = R[Rindex + ((L >>> 4 ) & 15)];
			if ((J &   0x40) != 0) P[Pindex +  6] = R[Rindex + ((L >>> 12) & 15)];
			if ((J &   0x20) != 0) P[Pindex +  5] = R[Rindex + ((L >>> 20) & 15)];
			if ((J &   0x10) != 0) P[Pindex +  4] = R[Rindex + ((L >>> 28)     )];
			if ((J &   0x08) != 0) P[Pindex +  3] = R[Rindex + ((L       ) & 15)];
			if ((J &   0x04) != 0) P[Pindex +  2] = R[Rindex + ((L >>> 8 ) & 15)];
			if ((J &   0x02) != 0) P[Pindex +  1] = R[Rindex + ((L >>> 16) & 15)];
			if ((J &   0x01) != 0) P[Pindex     ] = R[Rindex + ((L >>> 24) & 15)];
		}
	}

	private void PutSpriteM(int Pindex, int Cindex, int C2index, int Rindex, int h, int inc, int Mindex, byte pr)
	{
		int[] P = XBuf;
		byte[] C = VRAM;
		int[] C2 = VRAMS;
		int[] R = Pal;
		byte[] M = SPM;
		int i, J;
		int L;
		for (i = 0; i < h; i++, Cindex += inc, C2index += inc, Pindex += (360 + 64), Mindex += (360 + 64)) {
			J = ((C[Cindex    ] | C[Cindex + 32] | C[Cindex + 64] | C[Cindex + 96]) & 0xFF)
			  | ((C[Cindex + 1] | C[Cindex + 33] | C[Cindex + 65] | C[Cindex + 97]) << 8);

			if (J == 0) continue;
			L = C2[C2index + 1];
			if ((J & 0x8000) != 0 && M[Mindex     ] <= pr) P[Pindex     ] = R[Rindex + ((L >>>  4) & 15)];
			if ((J & 0x4000) != 0 && M[Mindex +  1] <= pr) P[Pindex +  1] = R[Rindex + ((L >>> 12) & 15)];
			if ((J & 0x2000) != 0 && M[Mindex +  2] <= pr) P[Pindex +  2] = R[Rindex + ((L >>> 20) & 15)];
			if ((J & 0x1000) != 0 && M[Mindex +  3] <= pr) P[Pindex +  3] = R[Rindex + ((L >>> 28)     )];
			if ((J & 0x0800) != 0 && M[Mindex +  4] <= pr) P[Pindex +  4] = R[Rindex + ((L       ) & 15)];
			if ((J & 0x0400) != 0 && M[Mindex +  5] <= pr) P[Pindex +  5] = R[Rindex + ((L >>>  8) & 15)];
			if ((J & 0x0200) != 0 && M[Mindex +  6] <= pr) P[Pindex +  6] = R[Rindex + ((L >>> 16) & 15)];
			if ((J & 0x0100) != 0 && M[Mindex +  7] <= pr) P[Pindex +  7] = R[Rindex + ((L >>> 24) & 15)];
			L = C2[C2index];
			if ((J &   0x80) != 0 && M[Mindex + 8 ] <= pr) P[Pindex +  8] = R[Rindex + ((L >>>  4) & 15)];
			if ((J &   0x40) != 0 && M[Mindex + 9 ] <= pr) P[Pindex +  9] = R[Rindex + ((L >>> 12) & 15)];
			if ((J &   0x20) != 0 && M[Mindex + 10] <= pr) P[Pindex + 10] = R[Rindex + ((L >>> 20) & 15)];
			if ((J &   0x10) != 0 && M[Mindex + 11] <= pr) P[Pindex + 11] = R[Rindex + ((L >>> 28)     )];
			if ((J &   0x08) != 0 && M[Mindex + 12] <= pr) P[Pindex + 12] = R[Rindex + ((L       ) & 15)];
			if ((J &   0x04) != 0 && M[Mindex + 13] <= pr) P[Pindex + 13] = R[Rindex + ((L >>>  8) & 15)];
			if ((J &   0x02) != 0 && M[Mindex + 14] <= pr) P[Pindex + 14] = R[Rindex + ((L >>> 16) & 15)];
			if ((J &   0x01) != 0 && M[Mindex + 15] <= pr) P[Pindex + 15] = R[Rindex + ((L >>> 24) & 15)];
		}
	}

	private void PutSpriteHflipM(int Pindex, int Cindex, int C2index, int Rindex, int h, int inc, int Mindex, byte pr)
	{
		int[] P = XBuf;
		byte[] C = VRAM;
		int[] C2 = VRAMS;
		int[] R = Pal;
		byte[] M = SPM;
		int i, J;
		int L;
		for (i = 0; i < h; i++, Cindex += inc, C2index += inc, Pindex += (360 + 64), Mindex += (360 + 64)) {
			J = ((C[Cindex    ] | C[Cindex + 32] | C[Cindex + 64] | C[Cindex + 96]) & 0xFF)
			  | ((C[Cindex + 1] | C[Cindex + 33] | C[Cindex + 65] | C[Cindex + 97]) << 8);

			if (J == 0) continue;
			L = C2[C2index + 1];
			if ((J & 0x8000) != 0 && M[Mindex + 15] <= pr) P[Pindex + 15] = R[Rindex + ((L >>>  4) & 15)];
			if ((J & 0x4000) != 0 && M[Mindex + 14] <= pr) P[Pindex + 14] = R[Rindex + ((L >>> 12) & 15)];
			if ((J & 0x2000) != 0 && M[Mindex + 13] <= pr) P[Pindex + 13] = R[Rindex + ((L >>> 20) & 15)];
			if ((J & 0x1000) != 0 && M[Mindex + 12] <= pr) P[Pindex + 12] = R[Rindex + ((L >>> 28)     )];
			if ((J & 0x0800) != 0 && M[Mindex + 11] <= pr) P[Pindex + 11] = R[Rindex + ((L       ) & 15)];
			if ((J & 0x0400) != 0 && M[Mindex + 10] <= pr) P[Pindex + 10] = R[Rindex + ((L >>>  8) & 15)];
			if ((J & 0x0200) != 0 && M[Mindex +  9] <= pr) P[Pindex +  9] = R[Rindex + ((L >>> 16) & 15)];
			if ((J & 0x0100) != 0 && M[Mindex +  8] <= pr) P[Pindex +  8] = R[Rindex + ((L >>> 24) & 15)];
			L = C2[C2index];
			if ((J &   0x80) != 0 && M[Mindex +  7] <= pr) P[Pindex +  7] = R[Rindex + ((L >>> 4 ) & 15)];
			if ((J &   0x40) != 0 && M[Mindex +  6] <= pr) P[Pindex +  6] = R[Rindex + ((L >>> 12) & 15)];
			if ((J &   0x20) != 0 && M[Mindex +  5] <= pr) P[Pindex +  5] = R[Rindex + ((L >>> 20) & 15)];
			if ((J &   0x10) != 0 && M[Mindex +  4] <= pr) P[Pindex +  4] = R[Rindex + ((L >>> 28)     )];
			if ((J &   0x08) != 0 && M[Mindex +  3] <= pr) P[Pindex +  3] = R[Rindex + ((L       ) & 15)];
			if ((J &   0x04) != 0 && M[Mindex +  2] <= pr) P[Pindex +  2] = R[Rindex + ((L >>>  8) & 15)];
			if ((J &   0x02) != 0 && M[Mindex +  1] <= pr) P[Pindex +  1] = R[Rindex + ((L >>> 16) & 15)];
			if ((J &   0x01) != 0 && M[Mindex     ] <= pr) P[Pindex     ] = R[Rindex + ((L >>> 24) & 15)];
		}
	}

	private void PutSpriteMakeMask(int Pindex, int Cindex, int C2index, int Rindex, int h, int inc, int Mindex, byte pr)
	{
		int[] P = XBuf;
		byte[] C = VRAM;
		int[] C2 = VRAMS;
		int[] R = Pal;
		byte[] M = SPM;
		int i,J;
		int L;
		for (i = 0; i < h; i++, Cindex += inc, C2index += inc, Pindex += (360 + 64), Mindex += (360 + 64)) {
			J = ((C[Cindex    ] | C[Cindex + 32] | C[Cindex + 64] | C[Cindex + 96]) & 0xFF)
			  | ((C[Cindex + 1] | C[Cindex + 33] | C[Cindex + 65] | C[Cindex + 97]) << 8);

			if (J == 0) continue;
			L = C2[C2index + 1];
			if ((J & 0x8000) != 0) { P[Pindex     ] = R[Rindex + ((L >>>  4) & 15)]; M[Mindex     ] = pr; }
			if ((J & 0x4000) != 0) { P[Pindex +  1] = R[Rindex + ((L >>> 12) & 15)]; M[Mindex +  1] = pr; }
			if ((J & 0x2000) != 0) { P[Pindex +  2] = R[Rindex + ((L >>> 20) & 15)]; M[Mindex +  2] = pr; }
			if ((J & 0x1000) != 0) { P[Pindex +  3] = R[Rindex + ((L >>> 28)     )]; M[Mindex +  3] = pr; }
			if ((J & 0x0800) != 0) { P[Pindex +  4] = R[Rindex + ((L       ) & 15)]; M[Mindex +  4] = pr; }
			if ((J & 0x0400) != 0) { P[Pindex +  5] = R[Rindex + ((L >>>  8) & 15)]; M[Mindex +  5] = pr; }
			if ((J & 0x0200) != 0) { P[Pindex +  6] = R[Rindex + ((L >>> 16) & 15)]; M[Mindex +  6] = pr; }
			if ((J & 0x0100) != 0) { P[Pindex +  7] = R[Rindex + ((L >>> 24) & 15)]; M[Mindex +  7] = pr; }
			L = C2[C2index];
			if ((J &   0x80) != 0) { P[Pindex +  8] = R[Rindex + ((L >>>  4) & 15)]; M[Mindex +  8] = pr; }
			if ((J &   0x40) != 0) { P[Pindex +  9] = R[Rindex + ((L >>> 12) & 15)]; M[Mindex +  9] = pr; }
			if ((J &   0x20) != 0) { P[Pindex + 10] = R[Rindex + ((L >>> 20) & 15)]; M[Mindex + 10] = pr; }
			if ((J &   0x10) != 0) { P[Pindex + 11] = R[Rindex + ((L >>> 28)     )]; M[Mindex + 11] = pr; }
			if ((J &   0x08) != 0) { P[Pindex + 12] = R[Rindex + ((L       ) & 15)]; M[Mindex + 12] = pr; }
			if ((J &   0x04) != 0) { P[Pindex + 13] = R[Rindex + ((L >>>  8) & 15)]; M[Mindex + 13] = pr; }
			if ((J &   0x02) != 0) { P[Pindex + 14] = R[Rindex + ((L >>> 16) & 15)]; M[Mindex + 14] = pr; }
			if ((J &   0x01) != 0) { P[Pindex + 15] = R[Rindex + ((L >>> 24) & 15)]; M[Mindex + 15] = pr; }
		}
	}

	private void PutSpriteHflipMakeMask(int Pindex, int Cindex, int C2index, int Rindex, int h, int inc, int Mindex, byte pr)
	{
		int[] P = XBuf;
		byte[] C = VRAM;
		int[] C2 = VRAMS;
		int[] R = Pal;
		byte[] M = SPM;
		int i,J;
		int L;
		for (i = 0; i < h; i++, Cindex += inc, C2index += inc, Pindex += (360 + 64), Mindex += (360 + 64)) {
			J = ((C[Cindex    ] | C[Cindex + 32] | C[Cindex + 64] | C[Cindex + 96]) & 0xFF)
			  | ((C[Cindex + 1] | C[Cindex + 33] | C[Cindex + 65] | C[Cindex + 97]) << 8);

			if (J == 0) continue;
			L = C2[C2index + 1];
			if ((J & 0x8000) != 0) { P[Pindex + 15] = R[Rindex + ((L >>>  4) & 15)]; M[Mindex + 15] = pr; }
			if ((J & 0x4000) != 0) { P[Pindex + 14] = R[Rindex + ((L >>> 12) & 15)]; M[Mindex + 14] = pr; }
			if ((J & 0x2000) != 0) { P[Pindex + 13] = R[Rindex + ((L >>> 20) & 15)]; M[Mindex + 13] = pr; }
			if ((J & 0x1000) != 0) { P[Pindex + 12] = R[Rindex + ((L >>> 28)     )]; M[Mindex + 12] = pr; }
			if ((J & 0x0800) != 0) { P[Pindex + 11] = R[Rindex + ((L       ) & 15)]; M[Mindex + 11] = pr; }
			if ((J & 0x0400) != 0) { P[Pindex + 10] = R[Rindex + ((L >>>  8) & 15)]; M[Mindex + 10] = pr; }
			if ((J & 0x0200) != 0) { P[Pindex +  9] = R[Rindex + ((L >>> 16) & 15)]; M[Mindex +  9] = pr; }
			if ((J & 0x0100) != 0) { P[Pindex +  8] = R[Rindex + ((L >>> 24) & 15)]; M[Mindex +  8] = pr; }
			L = C2[C2index + 0];
			if ((J &   0x80) != 0) { P[Pindex +  7] = R[Rindex + ((L >>>  4) & 15)]; M[Mindex +  7] = pr; }
			if ((J &   0x40) != 0) { P[Pindex +  6] = R[Rindex + ((L >>> 12) & 15)]; M[Mindex +  6] = pr; }
			if ((J &   0x20) != 0) { P[Pindex +  5] = R[Rindex + ((L >>> 20) & 15)]; M[Mindex +  5] = pr; }
			if ((J &   0x10) != 0) { P[Pindex +  4] = R[Rindex + ((L >>> 28)     )]; M[Mindex +  4] = pr; }
			if ((J &   0x08) != 0) { P[Pindex +  3] = R[Rindex + ((L       ) & 15)]; M[Mindex +  3] = pr; }
			if ((J &   0x04) != 0) { P[Pindex +  2] = R[Rindex + ((L >>>  8) & 15)]; M[Mindex +  2] = pr; }
			if ((J &   0x02) != 0) { P[Pindex +  1] = R[Rindex + ((L >>> 16) & 15)]; M[Mindex +  1] = pr; }
			if ((J &   0x01) != 0) { P[Pindex     ] = R[Rindex + ((L >>> 24) & 15)]; M[Mindex     ] = pr; }
		}
	}

	public void RefreshSprite(int Y1, int Y2, int bg)
	{
		int n;
		//SPR spr = new SPR();

		int index = 256 - 4;

		if (bg==0)
			usespbg=0;

		for (n = 0; n < 64; n++, index -= 4) {
			int x, y, no, atr, inc, cgx, cgy;
			int R, C;
			int C2;
			int	pos;
			int h, t, i, j;
			int y_sum;
			int spbg;

			int sprY = SPRAM[index];
			int sprX = SPRAM[index + 1];
			int sprNo = SPRAM[index + 2];
			int sprAtr = SPRAM[index + 3];

			atr = sprAtr;
			spbg = (atr >> 7) & 1;
			if (spbg != bg)
				continue;
			y = (sprY & 1023) - 64;
			x = (sprX & 1023) - 32;
			no =  sprNo & 2047;
			cgx = (atr >> 8) & 1;
			cgy = (atr >> 12) & 3;
			cgy |= cgy >> 1;
			no = (no >> 1) & ~(cgy * 2 + cgx);
			if (y >= Y2 || y + ((cgy + 1) << 4) < Y1 || x >= io.screen_w || x + ((cgx + 1) << 4) < 0) continue;

			R = 256 + ((atr & 15) << 4);
			for (i = 0; i < (cgy << 1) + cgx + 1; i++) {
				if ((vchanges[no + i] & 0xFF) != 0) {
					vchanges[no + i] = 0;
					sp2pixel(no + i);
				}
				if (cgx == 0) i++;
			}
			C = no * 128; //VRAM
			C2 = no * 32; //VRAMS
			pos = (((360 + 64) - io.screen_w) >> 1) + (360 + 64) * y + x;
			inc = 2;
			if ((atr & 0x8000) != 0) {
				inc =- 2;
				C += 15 * 2 + (cgy << 8);
				C2 += 15 * 2 + (cgy << 6);
			}
			y_sum = 0;

			for (i = 0; i <= cgy; i++) {
				t = Y1 - y - y_sum;
				h = 16;
				if (t > 0) {
					C += t * inc;
					C2 += t * inc;
					h -= t;
					pos += t * (360 + 64);
				}
				if (h > Y2 - y - y_sum) h = Y2 - y - y_sum;
				if (spbg == 0){
					usespbg = 1;
					if ((atr & 0x0800) != 0){
					  for (j = 0; j <= cgx; j++)
						PutSpriteHflipMakeMask(pos + ((cgx - j) << 4), C + (j << 7), C2 + (j << 5), R, h, inc, pos + ((cgx - j) << 4), (byte)n);
					} else {
					  for(j=0; j <= cgx; j++)
						PutSpriteMakeMask(pos + (j << 4), C + (j << 7), C2 + (j << 5), R, h, inc, pos + (j << 4), (byte)n);
					}
				} else if (usespbg != 0) {
					if ((atr & 0x0800) != 0) {
					  for (j = 0; j <= cgx; j++)
						PutSpriteHflipM(pos + ((cgx - j) << 4), C + (j << 7), C2 + (j << 5), R, h, inc, pos + ((cgx - j) << 4), (byte)n);
					} else {
					  for (j = 0; j <= cgx; j++)
						PutSpriteM(pos + (j << 4), C + (j << 7), C2 + (j << 5), R, h, inc, pos + (j << 4), (byte)n);
					}
				} else {
					if ((atr & 0x0800) != 0) {
					  for (j = 0; j <= cgx; j++)
						PutSpriteHflip(pos + ((cgx - j) << 4), C + (j << 7), C2 + (j << 5), R, h, inc);
					} else {
					  for (j = 0; j <= cgx; j++)
						PutSprite(pos + (j << 4), C + (j << 7), C2 + (j << 5), R, h, inc);
					}
				}
				pos += h * (360 + 64);
				C += h * inc + 16 * 7 * inc;
				C2 += h * inc + 16 * inc;
				y_sum += 16;
			}
		}
	}

	public void RefreshScreen()
	{
		int dispmin,dispmax;

		dispmin = (io.maxline - io.minline > 227 ? io.minline + ((io.maxline - io.minline - 227 + 1) >> 1) : io.minline);
		dispmax = (io.maxline - io.minline > 227 ? io.maxline - ((io.maxline - io.minline - 227 + 1) >> 1) : io.maxline);

		render.setDrawRequest(XBuf,
				(WIDTH - io.screen_w) / 2,
				io.minline + dispmin,
				io.screen_w,
				dispmax - dispmin + 1);

		Util.memsetByte(SPM, io.minline*(360+64), (io.maxline-io.minline)*(360+64), (byte)0);

		this.isRefreshedScreen = true;
	}

	public void clearVideoBuffer() {
		Util.memsetInt(XBuf, io.minline*(360+64), (io.maxline-io.minline)*(360+64), Pal[0]);
	}

	public boolean CartLoad(String name, Context context)
	{
		InputStream is = null;
		int fsize;

		try	{
			is = context.getAssets().open(name);
			//is = context.openFileInput(name);
			fsize = (int)is.skip(Long.MAX_VALUE);

			is = context.getAssets().open(name);
			//is = context.openFileInput(name);
			is.skip(fsize & 0x1fff);

			fsize &= ~0x1fff;
			ROM_size = fsize / 0x2000;

			int ROMmask = 1;
			while (ROMmask < ROM_size) ROMmask <<= 1;

			ROM = new byte[ROMmask][];
			for (int i = 0; i < ROMmask; i++) {
				ROM[i] = new byte[0x2000];
				Util.memsetByte(ROM[i], 0, ROM[i].length, (byte)0xFF);
			}

			for (int i = 0; i < ROM_size; i++) {
				ROM[i] = new byte[0x2000];
				is.read(ROM[i], 0, 0x2000);
			}

		} catch (Exception e) {
			debug.TRACE(name + "is not found");
			return false;
		}
		finally {
			if (is != null) {
				try	{
					is.close();
				}
				catch (IOException e) {
				}
			}
		}

		return true;
	}

	public void ResetPCE(M6502 M)
	{
		M.A = 0;
		M.AfterCLI = 0;
		M.IBackup = 0;
		M.ICount = 0;
		M.IPeriod = 0;
		M.IRequest = 0;

		M.NF = 0;
		M.P = 0;

		M.S = 0;
		M.Trace = 0;
		M.Trap = 0;
		M.TrapBadOps = 0;
		M.User = 0;
		M.VF = 0;
		M.X = 0;
		M.Y = 0;
		M.ZF = 0;

//		TimerCount = TimerPeriod;
		M.IPeriod = IPeriod;
		M.TrapBadOps = 1;

		scanline = 0;
		io.vdc_status=0;
		io.vdc_inc = 1;
		io.minline = 0;
		io.maxline = 255;
		io.irq_mask = 0;
		io.psg_volume = 0;
		io.psg_ch = 0;
		for (int i = 0; i < 6; i++)
		{
			io.PSG[i][4] = 0x80;
		}
//		CycleOld = 0;
		cpu.Reset6502(this);
	}

	public boolean InitPCE(String cartName, String backmemname, Context context)
	{
		try
		{
			int i,ROMmask;
			this.CartName = cartName;
			if (!CartLoad(cartName, context)) return false;

			DMYROM = new byte[0x2000];
			Util.memsetByte(DMYROM, 0, 0x2000, (byte)0xFF);

			WRAM = new byte[0x2000];
			Util.memsetByte(WRAM, 0, 0x2000, (byte)0);

			VRAM = new byte[0x20000];
			VRAM2 = new int[0x20000];
			VRAMS = new int[0x20000];

			//render.crear();

			IOAREA = new byte[0x2000];
			Util.memsetByte(IOAREA, 0, 0x2000, (byte)0xFF);

			vchange = new byte[0x20000 / 32];
			Util.memsetByte(vchange, 0, 0x20000 / 32, (byte)1);

			vchanges = new byte[0x20000 / 128];
			Util.memsetByte(vchanges, 0, 0x20000 / 128, (byte)1);

			Util.memsetInt(XBuf, 0, XBuf.length, 0);

			ROMmask = 1;
			while (ROMmask < ROM_size) ROMmask <<= 1;
			ROMmask--;
			debug.TRACE("ROMmask=%02X, ROM_size=%02X\n", ROMmask, ROM_size);

			for (i = 0; i < 0xF7; i++) {
				if (ROM_size == 0x30) {
					switch (i & 0x70) 	{
					case 0x00:
					case 0x10:
					case 0x50:
						ROMMap[i] = ROM[(i & ROMmask)];
						break;
					case 0x20:
					case 0x60:
						ROMMap[i] = ROM[((i - 0x20) & ROMmask)];
						break;
					case 0x30:
					case 0x70:
						ROMMap[i] = ROM[((i - 0x10) & ROMmask)];
						break;
					case 0x40:
						ROMMap[i] = ROM[((i - 0x20) & ROMmask)];
						break;
					}
				}
				else
					ROMMap[i] = ROM[(i & ROMmask)];
			}

	//		if (populus) {
	//			ROMMap[0x40] = PopRAM + (0) * 0x2000;
	//			ROMMap[0x41] = PopRAM + (1) * 0x2000;
	//			ROMMap[0x42] = PopRAM + (2) * 0x2000;
	//			ROMMap[0x43] = PopRAM + (3) * 0x2000;
	//		}

			ROMMap[0xF7] = WRAM;
			ROMMap[0xF8] = RAM;
			ROMMap[0xF9] = RAM;// + 0x2000;
			ROMMap[0xFA] = RAM;// + 0x4000;
			ROMMap[0xFB] = RAM;// + 0x6000;
			ROMMap[0xFF] = IOAREA;

	//		FILE *fp;
	//		fp = fopen(backmemname, "rb");
	//		if (fp == NULL)
	//			LogDump("Can't open %s\n", backmemname);
	//		else
	//		{
	//			fread(WRAM, 0x2000, 1, fp);
	//			fclose(fp);
	//		}
		} catch (Exception ex) {
			return false;
		}

		return true;
	}

	public void TrashPCE(String backmemname)
	{
//		fp = fopen(backmemname, "wb");
//		if (fp == NULL)
//			LogDump("Can't open %s\n", backmemname);
//		else
//		{
//			fwrite(WRAM, 0x2000, 1, fp);
//			fclose(fp);
//		}
	}

	public void saveState(DataOutputStream os) throws IOException {
		os.write(RAM);
		os.write(VRAM);
		os.write(vchange);
		for (int i = 0; i < SPRAM.length; i++) {
			os.writeShort(SPRAM[i]);
		}
		for (int i = 0; i < Pal.length; i++) {
			os.writeInt(Pal[i]);
		}
		os.writeInt(scanline);
		io.saveState(os);
	}

	public void loadState(DataInputStream is) throws IOException {
		is.read(RAM);
		is.read(VRAM);
		is.read(vchange);
		for (int i = 0; i < SPRAM.length; i++) {
			SPRAM[i] = is.readShort();
		}
		for (int i = 0; i < Pal.length; i++) {
			Pal[i] = is.readInt();
		}
		scanline = is.readInt();
		io.loadState(is);
	}

	public void clearVChange() {
		Util.memsetByte(vchange, 0, vchange.length, (byte)1);
		Util.memsetByte(vchanges, 0, vchanges.length, (byte)1);
	}

	private final static int FPS = 60;

	private final static int INTERVAL = 1000 / FPS;

	private long lastTime = System.currentTimeMillis();

	/**
	 * 1/60の時間間隔をの待って、スリープします。
	 */
	private void sleep() {
		long wait = INTERVAL - (System.currentTimeMillis() - lastTime);
		while (wait > 1) {
			try {
				Thread.sleep(wait / 2);
			} catch (Exception e) {
			}
			wait = INTERVAL - (System.currentTimeMillis() - lastTime);
		}
		lastTime = System.currentTimeMillis();
	}
}
