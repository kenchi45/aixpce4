package com.example.androidtest;

public class VDC_Status {
	public static final int VDC_CR = 0x01;
	public static final int VDC_OR = 0x02;
	public static final int VDC_RR = 0x04;
	public static final int VDC_DS = 0x08;
	public static final int VDC_DV = 0x10;
	public static final int VDC_VD = 0x20;
	public static final int VDC_BSY = 0x40;
	public static final int VDC_SpHit = VDC_CR;
	public static final int VDC_Over = VDC_OR;
	public static final int VDC_RasHit = VDC_RR;
	public static final int VDC_InVBlank = VDC_VD;
	public static final int VDC_DMAfinish = VDC_DV;
	public static final int VDC_SATBfinish = VDC_DS;
}
