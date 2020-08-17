package com.example.androidtest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class IO {
	public int[] VDC = new int[32];
	public int[] VCE = new int[0x200];
	public int vce_reg = 0;
	public char vdc_inc = 0;
	public 	char vdc_raster_count = 0;
	public byte vdc_reg = 0;
	public byte vdc_status = 0;
	public byte vdc_ratch = 0;
	public byte vce_ratch = 0;
	public byte vdc_satb = 0;
	public byte vdc_pendvsync = 0;
	public int bg_h = 0;
	public int bg_w = 0;
	public int screen_w = 0;
	public int screen_h = 0;
	public int scroll_y = 0;
	public int minline = 0;
	public int maxline = 0;
	public int[] JOY = new int[16];
	public int joy_select = 0;
	public int joy_counter = 0;
	public int[][] PSG = new int[8][8];
	public int[][] wave = new int[8][32];
	public int[] wavofs = new int[8];
	public int psg_ch = 0;
	public int psg_volume = 0;
	public int psg_lfo_freq = 0;
	public int psg_lfo_ctrl = 0;

	public int timer_reload = 0;
	public int timer_start = 0;
	public int timer_counter = 0;

	public byte irq_mask = 0;
	public byte irq_status = 0;

	public int backup = 0;
	public int adpcm_firstread = 0;
	public int adpcm_ptr = 0;
	public char adpcm_rptr = 0;
	public char adpcm_wptr = 0;

	public void saveState(DataOutputStream os) throws IOException {
		for (int i = 0; i < VDC.length; i++) {
			os.writeInt(VDC[i]);
		}
		for (int i = 0;i < VCE.length; i++) {
			os.writeInt(VCE[i]);
		}
		os.writeInt(vce_reg);
		os.writeChar(vdc_inc);
		os.writeChar(vdc_raster_count);
		os.writeByte(vdc_reg);
		os.writeByte(vdc_status);
		os.writeByte(vdc_ratch);
		os.writeByte(vce_ratch);
		os.writeByte(vdc_satb);
		os.writeByte(vdc_pendvsync);
		os.writeInt(bg_h);
		os.writeInt(bg_w);
		os.writeInt(screen_w);
		os.writeInt(screen_h);
		os.writeInt(scroll_y);
		os.writeInt(minline);
		os.writeInt(maxline);
		for (int i = 0; i < JOY.length; i++) {
			os.writeInt(JOY[i]);
		}
		os.writeInt(joy_select);
		os.writeInt(joy_counter);
		for (int i = 0; i < PSG.length; i++) {
			for (int j = 0; j < PSG[i].length; j++) {
				os.writeInt(PSG[i][j]);
			}
		}
		for (int i = 0; i < wave.length; i++) {
			for (int j = 0; j < wave[i].length; j++) {
				os.writeInt(wave[i][j]);
			}
		}
		for (int i = 0; i < wavofs.length; i++) {
			os.writeInt(wavofs[i]);
		}
		os.writeInt(psg_ch);
		os.writeInt(psg_volume);
		os.writeInt(psg_lfo_freq);
		os.writeInt(psg_lfo_ctrl);
		os.writeInt(timer_reload);
		os.writeInt(timer_start);
		os.writeInt(timer_counter);
		os.writeByte(irq_mask);
		os.writeByte(irq_status);
		os.writeInt(backup);
		os.writeInt(adpcm_firstread);
		os.writeInt(adpcm_ptr);
		os.writeChar(adpcm_rptr);
		os.writeChar(adpcm_wptr);
	}

	public void loadState(DataInputStream is) throws IOException {
		for (int i = 0; i < VDC.length; i++) {
			VDC[i] = is.readInt();
		}
		for (int i = 0;i < VCE.length; i++) {
			VCE[i] = is.readInt();
		}
		vce_reg = is.readInt();
		vdc_inc = is.readChar();
		vdc_raster_count = is.readChar();
		vdc_reg = is.readByte();
		vdc_status = is.readByte();
		vdc_ratch = is.readByte();
		vce_ratch = is.readByte();
		vdc_satb = is.readByte();
		vdc_pendvsync = is.readByte();
		bg_h = is.readInt();
		bg_w = is.readInt();
		screen_w = is.readInt();
		screen_h = is.readInt();
		scroll_y = is.readInt();
		minline = is.readInt();
		maxline = is.readInt();
		for (int i = 0; i < JOY.length; i++) {
			JOY[i] = is.readInt();
		}
		joy_select = is.readInt();
		joy_counter = is.readInt();
		for (int i = 0; i < PSG.length; i++) {
			for (int j = 0; j < PSG[i].length; j++) {
				PSG[i][j] = is.readInt();
			}
		}
		for (int i = 0; i < wave.length; i++) {
			for (int j = 0; j < wave[i].length; j++) {
				wave[i][j] = is.readInt();
			}
		}
		for (int i = 0; i < wavofs.length; i++) {
			wavofs[i] = is.readInt();
		}
		psg_ch = is.readInt();
		psg_volume = is.readInt();
		psg_lfo_freq = is.readInt();
		psg_lfo_ctrl = is.readInt();
		timer_reload = is.readInt();
		timer_start = is.readInt();
		timer_counter = is.readInt();
		irq_mask = is.readByte();
		irq_status = is.readByte();
		backup = is.readInt();
		adpcm_firstread = is.readInt();
		adpcm_ptr = is.readInt();
		adpcm_rptr = is.readChar();
		adpcm_wptr = is.readChar();
	}
}
