package com.example.androidtest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//import com.nttdocomo.device.StorageDevice;
//import com.nttdocomo.fs.AccessToken;
//import com.nttdocomo.fs.DoJaStorageService;
//import com.nttdocomo.fs.File;
//import com.nttdocomo.fs.Folder;
//import com.nttdocomo.io.FileDataOutput;
//import com.nttdocomo.io.FileEntity;

class M6502 {
	public int PC;

	public int A, P, X, Y, S;

	public int VF;

	public int IPeriod, ICount;

	public int IRequest;

	public int AfterCLI;

	public int ZF;

	public int IBackup;

	public int NF;

	public int User;

	public int TrapBadOps;

	public int Trap;

	public int Trace;

	public int[] MPR = new int[8];

	private static final int[] Cycles = new int[] {
		8, 7, 3,  4, 6, 4, 6, 7, 3, 2, 2, 0, 7, 5, 7, 6, 
		2, 7, 7,  4, 6, 4, 6, 7, 2, 5, 2, 0, 7, 5, 7, 6, 
		7, 7, 3,  4, 4, 4, 6, 7, 3, 2, 2, 0, 5, 5, 7, 6, 
		2, 7, 7,  0, 4, 4, 6, 7, 2, 5, 2, 0, 5, 5, 7, 6, 
		7, 7, 3,  4, 8, 4, 6, 7, 3, 2, 2, 0, 4, 5, 7, 6, 
		2, 7, 7,  5, 0, 4, 6, 7, 2, 5, 3, 0, 0, 5, 7, 6, 
		7, 7, 2,  0, 4, 4, 6, 7, 3, 2, 2, 0, 7, 5, 7, 6, 
		2, 7, 7, 17, 4, 4, 6, 7, 2, 5, 3, 0, 7, 5, 7, 6, 
		4, 7, 2,  7, 4, 4, 4, 7, 2, 2, 2, 0, 5, 5, 5, 6, 
		2, 7, 7,  8, 4, 4, 4, 7, 2, 5, 2, 0, 5, 5, 5, 6, 
		2, 7, 2,  7, 4, 4, 4, 7, 2, 2, 2, 0, 5, 5, 5, 6, 
		2, 7, 7,  8, 4, 4, 4, 7, 2, 5, 2, 0, 5, 5, 5, 6, 
		2, 7, 2, 17, 4, 4, 6, 7, 2, 2, 2, 0, 5, 5, 7, 6, 
		2, 7, 7, 17, 0, 4, 6, 7, 2, 5, 3, 0, 0, 5, 7, 6, 
		2, 7, 0, 17, 4, 4, 6, 7, 2, 2, 2, 0, 5, 5, 7, 6, 
		2, 7, 7, 17, 2, 4, 6, 7, 2, 5, 3, 0, 0, 5, 7, 0
	};

	private int CycleCountOld = 0;
	
	private Debug debug = new Debug();

	public void Reset6502(Pce pce) {
		int wk1;
		byte page[][] = pce.Page;

		MPR[7] = 0x00;
		pce.bank_set(7, 0x00);
		MPR[6] = 0x05;
		pce.bank_set(6, 0x05);
		MPR[5] = 0x04;
		pce.bank_set(5, 0x04);
		MPR[4] = 0x03;
		pce.bank_set(4, 0x03);
		MPR[3] = 0x02;
		pce.bank_set(3, 0x02);
		MPR[2] = 0x01;
		pce.bank_set(2, 0x01);
		MPR[1] = 0xF8;
		pce.bank_set(1, 0xF8);
		MPR[0] = 0xFF;
		pce.bank_set(0, 0xFF);

		A = X = Y = 0x00;
		P = 0x04;
		NF = VF = 0;
		ZF = 0xFF;
		S = 0xFF;
		PC = (PC & 0xFF00) | (page[(wk1 = (0xFFFE)) >> 13][wk1 - (wk1 & ~0x1FFF)] & 0xFF);
		PC = (PC & 0xFF) | (((page[(wk1 = (0xFFFE + 1)) >> 13][wk1 - (wk1 & ~0x1FFF)] & 0xFF)) << 8);
		ICount = IPeriod;
		IRequest = 0;
		AfterCLI = 0;
		User = 0;
	}

	public void Int6502(Pce pce, int Type) {
		int wk1;
		int J = 0;

		if ((Type == 2) || ((P & 0x04) == 0)) {
			ICount -= 7;
			pce.RAM[0x100 + S] = (byte)((PC) >> 8);
			S = (S - 1) & 0xFF;
			pce.RAM[0x100 + S] = (byte)((PC) & 0xFF);
			S = (S - 1) & 0xFF;
			pce.RAM[0x100 + S] = (byte)(((P & ~(0x10 | 0x20)) & ~(0x80 | 0x40 | 0x02)) | (NF & 0x80) | (VF & 0x40) | ((ZF != 0) ? 0 : 0x02));
			S = (S - 1) & 0xFF;
			P &= ~0x08;
			if (Type == 2) {
				J = 0xFFFC;
			} else {
				P |= 0x04;
				switch (Type) {
				case 1:
					J = 0xFFF8;
					break;
				case 8:
					J = 0xFFF6;
					break;
				case 4:
					J = 0xFFFA;
					break;
				}
			}
			PC = (PC & 0xFF00) | readRam(pce, J);
			PC = (PC & 0xFF) | (readRam(pce, J + 1) << 8);
		} else {
			IRequest |= Type;
		}
	}
	
//	private FileEntity fe = null;
//	private FileDataOutput out = null;
//
//	public void InitSDLog() {
//		StorageDevice sd = StorageDevice.getInstance("/ext0");
//		Folder folder;
//		try {
//			folder = sd.getFolder((AccessToken)DoJaStorageService.getAccessToken(0,DoJaStorageService.SHARE_APPLICATION ));
//			try
//			{
//				folder.getFile("doja_log.txt").delete();
//			} catch (IOException ex) {
//			}
//			File file = folder.createFile("doja_log.txt");
//			fe = file.open(File.MODE_WRITE_ONLY);
//			out = fe.openDataOutput();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	int count = 0;
	
	public void CloseLog() {
//		try {
//			out.close();
//			fe.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	private int readRam(Pce pce, int adress) {
		return pce.Page[adress >> 13][adress - (adress & ~0x1FFF)] & 0xFF;
	}
		
	public int Run6502(Pce pce) {
		int J;
		int K;
		int wk1;
		int I;
		int pc = PC;
		int a = A;
		int p = P;
		int x = X;
		int y = Y;
		int s = S;
		int zf = ZF;
		int nf = NF;
		//final byte page[][] = pce.Page;
		final byte ram[] = pce.RAM;
		
		int cycle;
		final int msk = ~0x1FFF;
				
		for (;;) {
			
			I = readRam(pce, pc++);

			cycle = Cycles[I];
			switch (I) {

			// Implied 	CSH 	$d4 	1 	3 
			case 0xD4:
				break;
				
			// Implied 	CSL 	$54 	1 	3 
			case 0x54:
				break;

			// Relative 	BSR $rrrr 	$44 	2 	8 
			case 0x44:
				ram[0x100 + s] = (byte)(pc >> 8);
				s = (s - 1) & 0xFF;
				ram[0x100 + s] = (byte)(pc & 0xFF);
				s = (s - 1) & 0xFF;
				
			// Relative 	BRA $rrrr 	$80 	2 	4 
			case 0x80:
				pc += (byte) readRam(pce, pc) + 1;
				break;

			// Indexed Indirect 	JMP ($aaaa,X) 	$7c 	3 	7 
			case 0x7C:
				K = readRam(pce, pc) 
				  | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				K += x;
				pc = readRam(pce, K)
				   | ((readRam(pce, K + 1)) << 8);
				break;

			// Implied 	PHX 	$da 	1 	3 
			case 0xDA:
				ram[0x100 + s] = (byte)x;
				s = (s - 1) & 0xFF;
				break;
				
			// Implied 	PHY 	$5a 	1 	3 
			case 0x5A:
				ram[0x100 + s] = (byte)y;
				s = (s - 1) & 0xFF;
				break;
				
			// Implied 	PLX 	$fa 	1 	4 
			case 0xFA:
				s = (s + 1) & 0xFF;
				x = (ram[0x100 + s] & 0xFF);
				zf = nf = x;
				break;
				
			// Implied 	PLY 	$7a 	1 	4 
			case 0x7A:
				s = (s + 1) & 0xFF;
				y = (ram[0x100 + s] & 0xFF);
				zf = nf = y;
				break;

			// Implied 	CLA 	$62 	1 	2 
			case 0x62:
				a = 0;
				break;
				
			// Implied 	CLX 	$82 	1 	2 
			case 0x82:
				x = 0;
				break;
				
			// Implied 	CLY 	$c2 	1 	2 
			case 0xC2:
				y = 0;
				break;

			// Implied 	SXY 	$02 	1 	3 
			case 0x02:
				I = x;
				x = y;
				y = I;
				break;
				
			// Implied 	SAX 	$22 	1 	3 
			case 0x22:
				I = a;
				a = x;
				x = I;
				break;
				
			// Implied 	SAY 	$42 	1 	3 
			case 0x42:
				I = a;
				a = y;
				y = I;
				break;

			case 0x3A:
				a = (a - 1) & 0xFF;
				zf = nf = a;
				break;
				
			// Accumulator 	INC A 	$1a 	1 	2 
			case 0x1A:
				a = (a + 1) & 0xFF;
				zf = nf = a;
				break;

			// Indirect 	ADC ($zz) 	$72 	3 	7 
			case 0x72:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) + (I & 0x0F) + (p & 0x01);
					K |= (((a >> 4) + (I >> 4)) << 8);
					if ((K & 0xFF) > 9) {
						K = (K & 0xFF) + 6;
						K |= (((K >> 8) + 1) << 8);
					}
					if ((K >> 8) > 9)
						K = (K & 0xFF) | (((K >> 8) + 6) << 8);
					a = (K & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
					zf = nf = a;
					cycle++;
				} else {
					K = a + I + (p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0x01 : 0);
					VF = (~(a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Indirect 	AND ($zz) 	$32 	3 	7 
			case 0x32:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				a &= I;
				zf = nf = a;
				break;
				
			// Indirect 	CMP ($zz) 	$d2 	3 	7 
			case 0xD2:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				K = a - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Indirect 	EOR ($zz) 	$52 	3 	7 
			case 0x52:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				a ^= I;
				zf = nf = a;
				break;
				
			// Indirect 	LDA ($zz) 	$b2 	3 	7 
			case 0xB2:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				a = pce._Rd6502(J);
				zf = nf = a;
				break;
				
			// Indirect 	ORA ($zz) 	$12 	3 	7 
			case 0x12:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				a |= I;
				zf = nf = a;
				break;
				
			// Indirect 	STA ($zz) 	$92 	3 	7 
			case 0x92:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				pce._Wr6502(J, a);
				break;
				
			// Indirect 	SBC ($zz) 	$f2 	3 	7 
			case 0xF2:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (K & 0xFF00) | (a & 0x0F) - (I & 0x0F) - (~p & 0x01);
					if (((K & 0xFF) & 0x10) != 0)
						K = (K & 0xFF00) | (K & 0xFF) - 6;
					K = (K & 0xFF) | (((a >> 4) - (I >> 4) - ((((K & 0xFF) & 0x10) == 0x10) ? 1 : 0)) << 8);
					if (((K >> 8) & 0x10) != 0)
						K = (K & 0xFF) | (((K >> 8) - 6) << 8);
					a = (K & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((((K >> 8) & 0x10) != 0) ? 0 : 0x01);
					zf = nf = a;
					cycle++;
				} else {
					K = a - I - (~p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0 : 0x01);
					VF = ((a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;

			// Immediate 	BIT #$ii 	$89 	2 	2 
			case 0x89:
				I = readRam(pce, pc++);
				nf = VF = I;
				zf = I & a;
				break;
				
			// Zero Page, X 	BIT $zz,X 	$34 	2 	4 
			case 0x34:
				I = (ram[(byte) (readRam(pce, pc++) + x)] & 0xFF);
				nf = VF = I;
				zf = I & a;
				break;
				
			// Absolute, X 	BIT $aaaa,X 	$3c 	3 	5 
			case 0x3C:
				J = readRam(pce, pc)
				  | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				nf = VF = I;
				zf = I & a;
				break;

			// Zero Page 	STZ $zz 	$64 	2 	4 
			case 0x64:
				ram[(readRam(pce, pc++))] = 0x00;
				break;
				
			// Zero Page, X 	STZ $zz,X 	$74 	2 	4 
			case 0x74:
				ram[(readRam(pce, pc++) + x) & 0xFF] = 0x00;
				break;
				
			// Absolute 	STZ $aaaa 	$9c 	3 	5 
			case 0x9C:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				pce._Wr6502(J, 0x00);
				break;
				
			// Absolute, X 	STZ $aaaa,X 	$9e 	3 	5 
			case 0x9E:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				pce._Wr6502(J, 0x00);
				break;
				
			// Implied 	SET 	$f4 	1 	2 
			case 0xF4:
				I = readRam(pce, pc++);
				cycle += Cycles[I] + 3;
				switch (I) {
				case 0x65:
					I = (ram[(readRam(pce, pc++))] & 0xFF);
					{
						int index = (x);
						if ((p & 0x08) != 0) {
							K = ((ram[index] & 0x0F) + (I & 0x0F) + (p & 0x01))
							  | ((((ram[index] & 0xFF) >> 4) + (I >> 4)) << 8);
							if ((K & 0xFF) > 9) {
								K = (K & 0xFF) + 6;
								K |= (((K >> 8) + 1) << 8);
							}
							if ((K >> 8) > 9)
								K = (K & 0xFF) | (((K >> 8) + 6) << 8);
							ram[index] = (byte)(((K & 0xFF) & 0x0F) | ((K >> 8) << 4));
							p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
							zf = nf = (ram[index] & 0xFF);
							cycle++;
						} else {
							K = (ram[index] & 0xFF) + I + (p & 0x01);
							p &= ~0x01;
							p |= (((K >> 8) != 0) ? 0x01 : 0);
							VF = (~((ram[index] & 0xFF) ^ I) & ((ram[index] & 0xFF) ^ (K & 0xFF))) >> 1;
							zf = nf = (K & 0xFF);
							ram[index] = (byte)(K & 0xFF);
						}
					}
					break;
				
				// Absolute 	ADC $aaaa 	$6d 	3 	5 
				case 0x6D:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					I = pce._Rd6502(J);
					{
						int index = (x);
						if ((p & 0x08) != 0) {
							K = ((ram[index] & 0x0F) + (I & 0x0F) + (p & 0x01))
							  | ((((ram[index] & 0xFF) >> 4) + (I >> 4)) << 8);
							if ((K & 0xFF) > 9) {
								K = (K & 0xFF) + 6;
								K |= (((K >> 8) + 1) << 8);
							}
							if ((K >> 8) > 9)
								K = (K & 0xFF) | (((K >> 8) + 6) << 8);
							ram[index] = (byte)(((K & 0xFF) & 0x0F) | ((K >> 8) << 4));
							p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
							zf = nf = (ram[index] & 0xFF);
							cycle++;
						} else {
							K = (ram[index] & 0xFF) + I + (p & 0x01);
							p &= ~0x01;
							p |= (((K >> 8) != 0) ? 0x01 : 0);
							VF = (~((ram[index] & 0xFF) ^ I) & ((ram[index] & 0xFF) ^ (K & 0xFF))) >> 1;
							zf = nf = (K & 0xFF);
							ram[index] = (byte)(K & 0xFF);
						}
					}
					break;
					
				// Immediate 	ADC #$ii 	$69 	2 	2 
				case 0x69:
					I = readRam(pce, pc++);
					{
						int index = (x);
						if ((p & 0x08) != 0) {
							K = ((ram[index] & 0x0F) + (I & 0x0F) + (p & 0x01))
							  | ((((ram[index] & 0xFF) >> 4) + (I >> 4)) << 8);
							if ((K & 0xFF) > 9) {
								K = (K & 0xFF) + 6;
								K |= (((K >> 8) + 1) << 8);
							}
							if ((K >> 8) > 9)
								K = (K & 0xFF) | (((K >> 8) + 6) << 8);
							ram[index] = (byte)(((K & 0xFF) & 0x0F) | ((K >> 8) << 4));
							p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
							zf = nf = (ram[index] & 0xFF);
							cycle++;
						} else {
							K = (ram[index] & 0xFF) + I + (p & 0x01);
							p &= ~0x01;
							p |= (((K >> 8) != 0) ? 0x01 : 0);
							VF = (~((ram[index] & 0xFF) ^ I) & ((ram[index] & 0xFF) ^ (K & 0xFF))) >> 1;
							zf = nf = (K & 0xFF);
							ram[index] = (byte)(K & 0xFF);
						}
					}
					break;
					
				// Zero Page, X 	ADC $zz,X 	$75 	2 	4 
				case 0x75:
					I = (ram[(byte) (readRam(pce, pc++) + x)] & 0xFF);
					{
						int index = (x);
						if ((p & 0x08) != 0) {
							K = ((ram[index] & 0x0F) + (I & 0x0F) + (p & 0x01))
							  | ((((ram[index] & 0xFF) >> 4) + (I >> 4)) << 8);
							if ((K & 0xFF) > 9) {
								K = (K & 0xFF) + 6;
								K |= (((K >> 8) + 1) << 8);
							}
							if ((K >> 8) > 9)
								K = (K & 0xFF) | (((K >> 8) + 6) << 8);
							ram[index] = (byte)(((K & 0xFF) & 0x0F) | ((K >> 8) << 4));
							p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
							zf = nf = (ram[index] & 0xFF);
							cycle++;
						} else {
							K = (ram[index] & 0xFF) + I + (p & 0x01);
							p &= ~0x01;
							p |= (((K >> 8) != 0) ? 0x01 : 0);
							VF = (~((ram[index] & 0xFF) ^ I) & ((ram[index] & 0xFF) ^ (K & 0xFF))) >> 1;
							zf = nf = (K & 0xFF);
							ram[index] = (byte)(K & 0xFF);
						}
					}
					break;
				
				// Absolute, Y 	ADC $aaaa,Y 	$79 	3 	5 
				case 0x79:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					J = (J + y) & 0xFFFF;
					I = pce._Rd6502(J);
					{
						int index = (x);
						if ((p & 0x08) != 0) {
							K = ((ram[index] & 0x0F) + (I & 0x0F) + (p & 0x01))
							  | ((((ram[index] & 0xFF) >> 4) + (I >> 4)) << 8);
							if ((K & 0xFF) > 9) {
								K = (K & 0xFF) + 6;
								K |= (((K >> 8) + 1) << 8);
							}
							if ((K >> 8) > 9)
								K = (K & 0xFF) | (((K >> 8) + 6) << 8);
							ram[index] = (byte)(((K & 0xFF) & 0x0F) | ((K >> 8) << 4));
							p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
							zf = nf = (ram[index] & 0xFF);
							cycle++;
						} else {
							K = (ram[index] & 0xFF) + I + (p & 0x01);
							p &= ~0x01;
							p |= (((K >> 8) != 0) ? 0x01 : 0);
							VF = (~((ram[index] & 0xFF) ^ I) & ((ram[index] & 0xFF) ^ (K & 0xFF))) >> 1;
							zf = nf = (K & 0xFF);
							ram[index] = (byte)(K & 0xFF);
						}
					}
					break;
				
				// Absolute, X 	ADC $aaaa,X 	$7d 	3 	5 
				case 0x7D:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					J += x;
					I = pce._Rd6502(J);
					{
						int index = (x);
						if ((p & 0x08) != 0) {
							K = ((ram[index] & 0x0F) + (I & 0x0F) + (p & 0x01))
							  | ((((ram[index] & 0xFF) >> 4) + (I >> 4)) << 8);
							if ((K & 0xFF) > 9) {
								K = (K & 0xFF) + 6;
								K |= (((K >> 8) + 1) << 8);
							}
							if ((K >> 8) > 9)
								K = (K & 0xFF) | (((K >> 8) + 6) << 8);
							ram[index] = (byte)(((K & 0xFF) & 0x0F) | ((K >> 8) << 4));
							p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
							zf = nf = (ram[index] & 0xFF);
							cycle++;
						} else {
							K = (ram[index] & 0xFF) + I + (p & 0x01);
							p &= ~0x01;
							p |= (((K >> 8) != 0) ? 0x01 : 0);
							VF = (~((ram[index] & 0xFF) ^ I) & ((ram[index] & 0xFF) ^ (K & 0xFF))) >> 1;
							zf = nf = (K & 0xFF);
							ram[index] = (byte)(K & 0xFF);
						}
					}
					break;
					
				// Indexed Indirect 	ADC ($zz,X) 	$61 	2 	7 
				case 0x61:
					K = (readRam(pce, pc++) + x) & 0xFF;
					J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
					I = pce._Rd6502(J);
					{
						int index = (x);
						if ((p & 0x08) != 0) {
							K = (ram[index] & 0x0F) + (I & 0x0F)	+ (p & 0x01);
							K |= ((((ram[index] & 0xFF) >> 4) + (I >> 4)) << 8);
							if ((K & 0xFF) > 9) {
								K = (K & 0xFF) + 6;
								K |= (((K >> 8) + 1) << 8);
							}
							if ((K >> 8) > 9)
								K = (K & 0xFF) | (((K >> 8) + 6) << 8);
							ram[index] = (byte)(((K & 0xFF) & 0x0F) | ((K >> 8) << 4));
							p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
							zf = nf = (ram[index] & 0xFF);
							cycle++;
						} else {
							K = (ram[index] & 0xFF) + I + (p & 0x01);
							p &= ~0x01;
							p |= (((K >> 8) != 0) ? 0x01 : 0);
							VF = (~((ram[index] & 0xFF) ^ I) & ((ram[index] & 0xFF) ^ (K & 0xFF))) >> 1;
							zf = nf = (K & 0xFF);
							ram[index] = (byte)(K & 0xFF);
						}
					}
					break;
					
				// Indirect, Index 	ADC ($zz),Y 	$71 	2 	7 
				case 0x71:
					K = (readRam(pce, pc++));
					J = (((ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8)) + y) & 0xFFFF;
					I = pce._Rd6502(J);
					{
						int index = (x);
						if ((p & 0x08) != 0) {
							K = ((ram[index] & 0x0F) + (I & 0x0F) + (p & 0x01))
							  | ((((ram[index] & 0xFF) >> 4) + (I >> 4)) << 8);
							if ((K & 0xFF) > 9) {
								K = (K & 0xFF) + 6;
								K |= (((K >> 8) + 1) << 8);
							}
							if ((K >> 8) > 9)
								K = (K & 0xFF) | (((K >> 8) + 6) << 8);
							ram[index] = (byte)(((K & 0xFF) & 0x0F) | ((K >> 8) << 4));
							p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
							zf = nf = (ram[index] & 0xFF);
							cycle++;
						} else {
							K = (ram[index] & 0xFF) + I + (p & 0x01);
							p &= ~0x01;
							p |= (((K >> 8) != 0) ? 0x01 : 0);
							VF = (~((ram[index] & 0xFF) ^ I) & ((ram[index] & 0xFF) ^ (K & 0xFF))) >> 1;
							zf = nf = (K & 0xFF);
							ram[index] = (byte)(K & 0xFF);
						}
					}
					break;
					
				// Indirect 	ADC ($zz) 	$72 	3 	7 
				case 0x72:
					K = (readRam(pce, pc++));
					J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
					I = pce._Rd6502(J);
					{
						int index = (x);
						if ((p & 0x08) != 0) {
							K = ((ram[index] & 0x0F) + (I & 0x0F) + (p & 0x01))
							| ((((ram[index] & 0xFF) >> 4) + (I >> 4)) << 8);
							if ((K & 0xFF) > 9) {
								K = (K & 0xFF) + 6;
								K |= (((K >> 8) + 1) << 8);
							}
							if ((K >> 8) > 9)
								K = (K & 0xFF) | (((K >> 8) + 6) << 8);
							ram[index] = (byte)(((K & 0xFF) & 0x0F) | ((K >> 8) << 4));
							p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
							zf = nf = (ram[index] & 0xFF);
							cycle++;
						} else {
							K = (ram[index] & 0xFF) + I + (p & 0x01);
							p &= ~0x01;
							p |= (((K >> 8) != 0) ? 0x01 : 0);
							VF = (~((ram[index] & 0xFF) ^ I) & ((ram[index] & 0xFF) ^ (K & 0xFF))) >> 1;
							zf = nf = (K & 0xFF);
							ram[index] = (byte)(K & 0xFF);
						}
					}
					break;

				// Zero Page 	AND $zz 	$25 	2 	4 
				case 0x25:
					I = (ram[(readRam(pce, pc++))] & 0xFF);
					ram[x] = (byte)((ram[x] & 0xFF) & I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Absolute 	AND $aaaa 	$2d 	3 	5 
				case 0x2D:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) & I);
					zf = nf = (ram[x] & 0xFF);
					break;
				
					// Immediate 	AND #$ii 	$29 	2 	2 
				case 0x29:
					I = readRam(pce, pc++);
					ram[x] = (byte)((ram[x] & 0xFF) & I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Zero Page, X 	AND $zz,X 	$35 	2 	4 
				case 0x35:
					I = (ram[(readRam(pce, pc++) + x) & 0xFF] & 0xFF);
					ram[x] = (byte)((ram[x] & 0xFF) & I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Absolute, Y 	AND $aaaa,Y 	$39 	3 	5 
				case 0x39:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					J = (J + y) & 0xFFFF;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) & I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Absolute, X 	AND $aaaa,X 	$3d 	3 	5 
				case 0x3D:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					J += x;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) & I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Indexed Indirect 	AND ($zz,X) 	$21 	2 	7 
				case 0x21:
					K = (readRam(pce, pc++) + x) & 0xFF;
					J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) & I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Indirect, Index 	AND ($zz),Y 	$31 	2 	7 
				case 0x31:
					K = (readRam(pce, pc++));
					J = (((ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8)) + y) & 0xFFFF;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) & I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Indirect 	AND ($zz) 	$32 	3 	7 
				case 0x32:
					K = (readRam(pce, pc++));
					J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) & I);
					zf = nf = (ram[x] & 0xFF);
					break;

				// Zero Page 	EOR $zz 	$45 	2 	4 
				case 0x45:
					I = (ram[(readRam(pce, pc++))] & 0xFF);
					ram[x] = (byte)((ram[x] & 0xFF) ^ I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Absolute 	EOR $aaaa 	$4d 	3 	5 
				case 0x4D:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) ^ I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				case 0x49:
					I = readRam(pce, pc++);
					ram[x] = (byte)((ram[x] & 0xFF) ^ I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Zero Page, X 	EOR $zz,X 	$55 	2 	4 
				case 0x55:
					I = (ram[(readRam(pce, pc++) + x) & 0xFF] & 0xFF);
					ram[x] = (byte)((ram[x] & 0xFF) ^ I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Absolute, Y 	EOR $aaaa,Y 	$59 	3 	5 
				case 0x59:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					J = (J + y) & 0xFFFF;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) ^ I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Absolute, X 	EOR $aaaa,X 	$5d 	3 	5 
				case 0x5D:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					J += x;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) ^ I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Indexed Indirect 	EOR ($zz,X) 	$41 	2 	7 
				case 0x41:
					K = (readRam(pce, pc++) + x) & 0xFF;
					J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) ^ I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Indirect, Index 	EOR ($zz),Y 	$51 	2 	7 
				case 0x51:
					K = (readRam(pce, pc++));
					J = (((ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8)) + y) & 0xFFFF;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) ^ I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Indirect 	EOR ($zz) 	$52 	3 	7 
				case 0x52:
					K = (readRam(pce, pc++));
					J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) ^ I);
					zf = nf = (ram[x] & 0xFF);
					break;

				// Zero Page 	ORA $zz 	$05 	2 	4 
				case 0x05:
					I = (ram[(readRam(pce, pc++))] & 0xFF);
					ram[x] = (byte)((ram[x] & 0xFF) | I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Absolute 	ORA $aaaa 	$0d 	3 	5 
				case 0x0D:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) | I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Immediate 	ORA #$ii 	$09 	2 	2 
				case 0x09:
					I = readRam(pce, pc++);
					ram[x] = (byte)((ram[x] & 0xFF) | I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Zero Page, X 	ORA $zz,X 	$15 	2 	4 
				case 0x15:
					I = (ram[(readRam(pce, pc++) + x) & 0xFF] & 0xFF);
					ram[x] = (byte)((ram[x] & 0xFF) | I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Absolute, Y 	ORA $aaaa,Y 	$19 	3 	5 
				case 0x19:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					J = (J + y) & 0xFFFF;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) | I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Absolute, X 	ORA $aaaa,X 	$1d 	3 	5 
				case 0x1D:
					J = readRam(pce, pc)
					  | ((readRam(pce, pc + 1)) << 8);
					pc += 2;
					J += x;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) | I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Indexed Indirect 	ORA ($zz,X) 	$01 	2 	7 
				case 0x01:
					K = (readRam(pce, pc++) + x) & 0xFF;
					J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) | I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Indirect, Index 	ORA ($zz),Y 	$11 	2 	7 
				case 0x11:
					K = (readRam(pce, pc++));
					J = (((ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8)) + y) & 0xFFFF;
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) | I);
					zf = nf = (ram[x] & 0xFF);
					break;
					
				// Indirect 	ORA ($zz) 	$12 	3 	7 
				case 0x12:
					K = (readRam(pce, pc++));
					J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
					I = pce._Rd6502(J);
					ram[x] = (byte)((ram[x] & 0xFF) | I);
					zf = nf = (ram[x] & 0xFF);
					break;

				default:
					debug.TRACE("no sense SET\n");
					cycle -= Cycles[I];
					pc--;
					break;
				}
				break;

			// Immediate 	ST0 #$ii 	$03 	2 	5 
			case 0x03:
				pce.IO_write(0, readRam(pce, pc++));
				break;
				
			// Immediate 	ST1 #$ii 	$13 	2 	5 
			case 0x13:
				pce.IO_write(2, readRam(pce, pc++));
				break;
				
			// Immediate 	ST2 #$ii 	$23 	2 	5 
			case 0x23:
				pce.IO_write(3, readRam(pce, pc++));
				break;

			// Immediate 	TMA #$ii 	$43 	2 	4 
			case 0x43:
				I = readRam(pce, pc++);
				{
					int i;
					for (i = 0; i < 8; i++, I >>= 1) {
						if ((I & 1) != 0)
							break;
					}
					a = MPR[i];
				}
				break;

			// Immediate 	TAM #$ii 	$53 	2 	5 
			case 0x53:
				I = readRam(pce, pc++);
				{
					int i;
					for (i = 0; i < 8; i++, I >>= 1) {
						if ((I & 1) != 0) {
							MPR[i] = a;
							pce.bank_set(i, a);
							break;
						}
					}
				}
				break;

			// Block Transfer 	TDD $ssss, $dddd, $llll 	$c3 	7 	17 + 6 * llll 
			case 0xC3:
				{
					int src, dist, length;
					src = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					dist = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					length = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					cycle = length * 6;
					do {
						pce._Wr6502(dist--, pce._Rd6502(src));
						src--;
					} while (--length > 0);
				}
				break;

			// Block Transfer 	TII $ssss, $dddd, $llll 	$73 	7 	17 + 6 * llll 
			case 0x73:
				{
					int src, dist, length;
					src = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					dist = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					length = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					cycle = length * 6;
					do {
						pce._Wr6502(dist++, pce._Rd6502(src));
						src = (src + 1) & 0xFFFF;
					} while (--length > 0);
				}
				break;

			// Block Transfer 	TIA $ssss, $dddd, $llll 	$e3 	7 	17 + 6 * llll 
			case 0xE3:
				{
					int src, dist, length;
					src = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					dist = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					length = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					cycle = length * 6;
					do {
						pce._Wr6502(dist, pce._Rd6502(src));
						src = (src + 1) & 0xFFFF;
						if (--length == 0)
							break;
						pce._Wr6502(dist + 1, pce._Rd6502(src));
						src = (src + 1) & 0xFFFF;
					} while (--length > 0);
				}
				break;

			// Block Transfer 	TAI $ssss, $dddd, $llll 	$f3 	7 	17 + 6 * llll 
			case 0xF3:
				{
					int src, dist, length;
					src = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					dist = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					length = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					cycle = length * 6;
					do {
						pce._Wr6502(dist++, pce._Rd6502(src));
						if (--length == 0)
							break;
						pce._Wr6502(dist++, pce._Rd6502(src + 1));
					} while (--length > 0);
				}
				break;

			// Block Transfer 	TIN $ssss, $dddd, $llll 	$d3 	7 	17 + 6 * llll 
			case 0xD3:
				{
					int src, dist, length;
					src = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					dist = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					length = (readRam(pce, pc) | (readRam(pce, pc + 1) << 8));
					pc += 2;
					cycle = length * 6;
					do {
						pce._Wr6502(dist, pce._Rd6502(src));
						src = (src + 1) & 0xFFFF;
					} while (--length > 0);
				}
				break;

			// Zero Page 	TRB $zz 	$14 	2 	6 
			case 0x14:
				J = (readRam(pce, pc++));
				I = (ram[J] & 0xFF);
				nf = VF = I;
				I &= ~a;
				zf = I;
				ram[J] = (byte)I;
				break;
				
			// Absolute 	TRB $aaaa 	$1c 	3 	7 
			case 0x1C:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				nf = VF = I;
				I &= ~a;
				zf = I;
				pce._Wr6502(J, I);
				break;

			// Zero Page 	TSB $zz 	$04 	2 	6 
			case 0x04:
				J = (readRam(pce, pc++));
				I = (ram[J] & 0xFF);
				nf = VF = I;
				I |= a;
				zf = I;
				ram[J] = (byte)I;
				break;
				
			// Absolute 	TSB $aaaa 	$0c 	3 	7 
			case 0x0C:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				nf = VF = I;
				I |= a;
				zf = I;
				pce._Wr6502(J, I);
				break;

			// Immediate, Zero Page 	TST #$ii, $zz 	$83 	3 	7 
			case 0x83:
				I = readRam(pce, pc++);
				J = ram[(readRam(pce, pc++))] & 0xFF;
				nf = VF = J;
				zf = I & J;
				break;
				
			// Immediate, Zero Page, X 	TST #$ii, $zz, X 	$a3 	3 	7 
			case 0xA3:
				I = readRam(pce, pc++);
				J = ram[(readRam(pce, pc++) + x) & 0xFF] & 0xFF;
				nf = VF = J;
				zf = I & J;
				break;
				
			// Immediate, Absolute 	TST #$ii, $aaaa 	$93 	4 	8 
			case 0x93:
				I = readRam(pce, pc++);
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J = (J & 0xFF00) | pce._Rd6502(J);
				nf = VF = ((J) & 0xFF);
				zf = I & ((J) & 0xFF);
				break;
				
			// Immediate, Absolute, X 	TST #$ii, $aaaa, X 	$b3 	4 	8 
			case 0xB3:
				I = readRam(pce, pc++);
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				J = (J & 0xFF00) | pce._Rd6502(J);
				nf = VF = ((J) & 0xFF);
				zf = I & ((J) & 0xFF);
				break;

			// Zero Page, Relative 	BBR0 $zz, $rrrr 	$0f 	3 	6* 
			case 0x0F:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x01) != 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;
				
			// Zero Page, Relative 	BBR1 $zz, $rrrr 	$1f 	3 	6* 
			case 0x1F:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x02) != 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;
				
			// Zero Page, Relative 	BBR2 $zz, $rrrr 	$2f 	3 	6* 
			case 0x2F:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x04) != 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;
			
			// Zero Page, Relative 	BBR3 $zz, $rrrr 	$3f 	3 	6* 
			case 0x3F:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x08) != 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;
				
			// Zero Page, Relative 	BBR4 $zz, $rrrr 	$4f 	3 	6* 
			case 0x4F:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x10) != 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;
				
			// Zero Page, Relative 	BBR5 $zz, $rrrr 	$5f 	3 	6* 
			case 0x5F:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x20) != 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;
				
			// Zero Page, Relative 	BBR6 $zz, $rrrr 	$6f 	3 	6* 
			case 0x6F:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x40) != 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;
				
			// Zero Page, Relative 	BBR7 $zz, $rrrr 	$7f 	3 	6* 
			case 0x7F:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x80) != 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;

			// Zero Page, Relative 	BBS0 $zz, $rrrr 	$8f 	3 	6* 
			case 0x8F:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x01) != 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;
				
			// Zero Page, Relative 	BBS1 $zz, $rrrr 	$9f 	3 	6* 
			case 0x9F:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x02) != 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;
				
			// Zero Page, Relative 	BBS2 $zz, $rrrr 	$af 	3 	6* 
			case 0xAF:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x04) != 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;
				
			// Zero Page, Relative 	BBS3 $zz, $rrrr 	$bf 	3 	6* 
			case 0xBF:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x08) != 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;
				
			// Zero Page, Relative 	BBS4 $zz, $rrrr 	$cf 	3 	6* 
			case 0xCF:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x10) != 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;
				
			// Zero Page, Relative 	BBS5 $zz, $rrrr 	$df 	3 	6* 
			case 0xDF:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x20) != 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;
				
			// Zero Page, Relative 	BBS6 $zz, $rrrr 	$ef 	3 	6* 
			case 0xEF:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x40) != 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;
				
			// Zero Page, Relative 	BBS7 $zz, $rrrr 	$ff 	3 	6* 
			case 0xFF:
				if (((ram[(readRam(pce, pc++))] & 0xFF) & 0x80) != 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;

			// Zero Page 	RMB0 $zz 	$07 	2 	7 
			case 0x07:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) & ~0x01);
				break;
				
			// Zero Page 	RMB1 $zz 	$17 	2 	7 
			case 0x17:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) & ~0x02);
				break;
				
			// Zero Page 	RMB2 $zz 	$27 	2 	7 
			case 0x27:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) & ~0x04);
				break;
				
			// Zero Page 	RMB3 $zz 	$37 	2 	7 
			case 0x37:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) & ~0x08);
				break;
				
			// Zero Page 	RMB4 $zz 	$47 	2 	7 
			case 0x47:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) & ~0x10);
				break;
				
			// Zero Page 	RMB5 $zz 	$57 	2 	7 
			case 0x57:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) & ~0x20);
				break;
				
			// Zero Page 	RMB6 $zz 	$67 	2 	7 
			case 0x67:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) & ~0x40);
				break;
				
			// Zero Page 	RMB7 $zz 	$77 	2 	7 
			case 0x77:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) & ~0x80);
				break;

			// Zero Page 	SMB0 $zz 	$87 	2 	7 
			case 0x87:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) | 0x01);
				break;
				
			// Zero Page 	SMB1 $zz 	$97 	2 	7 
			case 0x97:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) | 0x02);
				break;
				
			// Zero Page 	SMB2 $zz 	$a7 	2 	7 
			case 0xA7:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) | 0x04);
				break;
				
			// Zero Page 	SMB3 $zz 	$b7 	2 	7 
			case 0xB7:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) | 0x08);
				break;
				
			// Zero Page 	SMB4 $zz 	$c7 	2 	7 
			case 0xC7:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) | 0x10);
				break;
				
			// Zero Page 	SMB5 $zz 	$d7 	2 	7 
			case 0xD7:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) | 0x20);
				break;
				
			// Zero Page 	SMB6 $zz 	$e7 	2 	7 
			case 0xE7:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) | 0x40);
				break;
				
			// Zero Page 	SMB7 $zz 	$f7 	2 	7 
			case 0xF7:
				wk1 = readRam(pce, pc++);
				ram[wk1] = (byte)((ram[wk1] & 0xFF) | 0x80);
				break;

			// Relative 	BPL $rrrr 	$10 	2 	2(4 if branch taken) 
			case 0x10:
				if ((nf & 0x80) != 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;
				
			// Relative 	BMI $rrrr 	$30 	2 	2(4 if branch taken) 
			case 0x30:
				if ((nf & 0x80) != 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;
				
			// Relative 	BNE $rrrr 	$d0 	2 	2(4 if branch taken) 
			case 0xD0:
				if (zf == 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;
				
			// Relative 	BEQ $rrrr 	$f0 	2 	2(4 if branch taken) 
			case 0xF0:
				if (zf == 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;
				
			// Relative 	BCC $rrrr 	$90 	2 	2(4 if branch taken) 
			case 0x90:
				if ((p & 0x01) != 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;
				
			// Relative 	BCS $rrrr 	$b0 	2 	2(4 if branch taken) 
			case 0xB0:
				if ((p & 0x01) != 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;
				
			// Relative 	BVC $rrrr 	$50 	2 	2 
			case 0x50:
				if ((VF & 0x40) != 0)
					pc++;
				else {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				}
				break;
				
			// Relative 	BVS $rrrr 	$70 	2 	2 
			case 0x70:
				if ((VF & 0x40) != 0) {
					pc += (byte) readRam(pce, pc) + 1;
					cycle += 2;
				} else
					pc++;
				break;

			// Absolute, X 	ROR $aaaa,X 	$7e 	3 	7 
			case 0x40:
				I = p;
				s = (s + 1) & 0xFF;
				p = (ram[0x100 + s] & 0xFF);
				nf = VF = p;
				zf = (((p & 0x02) != 0) ? 0 : 1);
				if ((IRequest != 0) && (I & 0x04) != 0 && (p & 0x04) == 0) {
					AfterCLI = 1;
					IBackup = ICount;
					ICount = 0;
				}
				s++;
				pc = (pc & 0xFF00) | (ram[0x100 + s] & 0xFF);
				s++;
				pc = (pc & 0xFF) | (((ram[0x100 + s] & 0xFF)) << 8);
				break;

			// Implied 	RTS 	$60 	1 	7 
			case 0x60:
				s = (s + 1) & 0xFF;
				pc = (pc & 0xFF00) | (ram[0x100 + s] & 0xFF);
				s = (s + 1) & 0xFF;
				pc = (pc & 0xFF) | (((ram[0x100 + s] & 0xFF)) << 8);
				pc++;
				break;

			// Absolute 	JSR $aaaa 	$20 	3 	7 
			case 0x20:
				K = readRam(pce, pc++);
				K |= ((readRam(pce, pc)) << 8);
				ram[0x100 + s] = (byte)(pc >> 8);
				s = (s - 1) & 0xFF;
				ram[0x100 + s] = (byte)(pc & 0xFF);
				s = (s - 1) & 0xFF;
				pc = K;
				break;

			// Absolute 	JMP $aaaa 	$4c 	3 	4 
			case 0x4C:
				K = readRam(pce, pc) 
				  | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				pc = K;
				break;

			// Indirect 	JMP ($aaaa) 	$6c 	3 	7 
			case 0x6C:
				K = readRam(pce, pc) 
				  | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				pc = (pc & 0xFF00)
						| readRam(pce, K++);
				pc = (pc & 0xFF)
						| ((readRam(pce, K)) << 8);
				break;

			// Implied 	BRK 	$00 	1 	8 
			case 0x00:
				pc++;
				ram[0x100 + s] = (byte)(pc >> 8);
				s = (s - 1) & 0xFF;
				ram[0x100 + s] = (byte)(pc & 0xFF);
				s = (s - 1) & 0xFF;
				ram[0x100 + s] = (byte)(((p & ~0x20 | 0x10) & ~(0x80 | 0x40 | 0x02)) | (nf & 0x80) | (VF & 0x40) | ((zf != 0) ? 0 : 0x02));
				s = (s - 1) & 0xFF;
				p = (p | 0x04) & ~0x08;
				pc = readRam(pce, 0xFFF6);
				pc |= ((readRam(pce, 0xFFF6 + 1)) << 8);
				debug.TRACE("BRK instruction\n");
				break;

			// Implied 	CLI 	$58 	1 	2 
			case 0x58:
				if ((IRequest != 0) && (p & 0x04) != 0) {
					AfterCLI = 1;
					IBackup = ICount;
					ICount = 0;
				}
				p &= ~0x04;
				break;

			// Implied 	PLP 	$28 	1 	4 
			case 0x28:
				s = (s + 1) & 0xFF;
				I = (ram[0x100 + s] & 0xFF);
				nf = VF = I;
				zf = (((I & 0x02) != 0) ? 0 : 1);
				if ((IRequest != 0) && ((I ^ p) & ~I & 0x04) != 0) {
					AfterCLI = 1;
					IBackup = ICount;
					ICount = 0;
				}
				p = I;
				break;

			// Implied 	PHP 	$08 	1 	3 
			case 0x08:
				ram[0x100 + s] = (byte)(((p & ~0x20 | 0x10) & ~(0x80 | 0x40 | 0x02))
						| (nf & 0x80) | (VF & 0x40) | ((zf != 0) ? 0 : 0x02));
				s = (s - 1) & 0xFF;
				break;
				
			// Implied 	CLC 	$18 	1 	2 
			case 0x18:
				p &= ~0x01;
				break;
				
			// Implied 	CLV 	$b8 	1 	2 
			case 0xB8:
				VF = 0;
				break;
				
			// Implied 	CLD 	$d8 	1 	2 
			case 0xD8:
				p &= ~0x08;
				break;
				
			// Implied 	SEC 	$38 	1 	2 
			case 0x38:
				p |= 0x01;
				break;
				
			// Implied 	SED 	$f8 	1 	2 
			case 0xF8:
				p |= 0x08;
				break;
				
			// Implied 	SEI 	$78 	1 	2 
			case 0x78:
				p |= 0x04;
				break;
				
			// Implied 	PHA 	$48 	1 	3 
			case 0x48:
				ram[0x100 + s] = (byte)a;
				s = (s - 1) & 0xFF;
				break;
				
			// Implied 	PLA 	$68 	1 	4 
			case 0x68:
				s = (s + 1) & 0xFF;
				a = (ram[0x100 + s] & 0xFF);
				zf = nf = a;
				break;
				
			// Implied 	TYA 	$98 	1 	2 
			case 0x98:
				a = y;
				zf = nf = a;
				break;
				
			// Implied 	TAY 	$a8 	1 	2 
			case 0xA8:
				y = a;
				zf = nf = y;
				break;
				
			// Implied 	INY 	$c8 	1 	2 
			case 0xC8:
				y = (y + 1) & 0xFF;
				zf = nf = y;
				break;
				
			// Implied 	DEY 	$88 	1 	2 
			case 0x88:
				y = (y - 1) & 0xFF;
				zf = nf = y;
				break;
				
			// Implied 	TXA 	$8a 	1 	2 
			case 0x8A:
				a = x;
				zf = nf = a;
				break;
				
			// Implied 	TAX 	$aa 	1 	2 
			case 0xAA:
				x = a;
				zf = nf = x;
				break;
				
			// Implied 	INX 	$e8 	1 	2 
			case 0xE8:
				x = (x + 1) & 0xFF;
				zf = nf = x;
				break;
				
			// Implied 	DEX 	$ca 	1 	2 
			case 0xCA:
				x = (x - 1) & 0xFF;
				zf = nf = x;
				break;
				
			// Implied 	NOP 	$ea 	1 	2 
			case 0xEA:
				break;
				
			// Implied 	TXS 	$9a 	1 	2 
			case 0x9A:
				s = x;
				break;
				
			// Implied 	TSX 	$ba 	1 	2 
			case 0xBA:
				x = s;
				zf = nf = x;
				break;

			// Zero Page 	BIT $zz 	$24 	2 	4
			case 0x24:
				I = (ram[(readRam(pce, pc++))] & 0xFF);
				nf = VF = I;
				zf = I & a;
				break;
				
			// Absolute 	BIT $aaaa 	$2c 	3 	5 
			case 0x2C:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				nf = VF = I;
				zf = I & a;
				break;

			// Zero Page 	ORA $zz 	$05 	2 	4 
			case 0x05:
				I = (ram[(readRam(pce, pc++))] & 0xFF);
				a |= I;
				zf = nf = a;
				break;
			
			// Zero Page 	ASL $zz 	$06 	2 	6 
			case 0x06:
				J = (readRam(pce, pc++));
				I = (ram[J] & 0xFF);
				p &= ~0x01;
				p |= I >> 7;
				I = (I << 1) & 0xFF;
				zf = nf = I;
				ram[J] = (byte)I;
				break;
				
			// Zero Page 	AND $zz 	$25 	2 	4 
			case 0x25:
				I = (ram[(readRam(pce, pc++))] & 0xFF);
				a &= I;
				zf = nf = a;
				break;
				
			// Zero Page 	ROL $zz 	$26 	2 	6 
			case 0x26:
				J = (readRam(pce, pc++));
				I = (ram[J] & 0xFF);
				K = ((I << 1) | (p & 0x01)) & 0xFF;
				p &= ~0x01;
				p |= I >> 7;
				I = K;
				zf = nf = I;
				ram[J] = (byte)I;
				break;
				
			// Zero Page 	EOR $zz 	$45 	2 	4 
			case 0x45:
				I = (ram[(readRam(pce, pc++))] & 0xFF);
				a ^= I;
				zf = nf = a;
				break;
				
			// Zero Page 	LSR $zz 	$46 	2 	6 
			case 0x46:
				J = (readRam(pce, pc++));
				I = (ram[J] & 0xFF);
				p &= ~0x01;
				p |= I & 0x01;
				I >>= 1;
				zf = nf = I;
				ram[J] = (byte)I;
				break;
				
			// Zero Page 	ADC $zz 	$65 	2 	4 
			case 0x65:
				I = (ram[(readRam(pce, pc++))] & 0xFF);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) + (I & 0x0F) + (p & 0x01);
					K |= (((a >> 4) + (I >> 4)) << 8);
					if ((K & 0xFF) > 9) {
						K = (K & 0xFF) + 6;
						K |= (((K >> 8) + 1) << 8);
					}
					if ((K >> 8) > 9)
						K = (K & 0xFF) | (((K >> 8) + 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
					zf = nf = a;
					cycle++;
				} else {
					K = a + I + (p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0x01 : 0);
					VF = (~(a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Zero Page 	ROR $zz 	$66 	2 	6 
			case 0x66:
				J = (readRam(pce, pc++));
				I = (ram[J] & 0xFF);
				K = (I >> 1) | (p << 7);
				p &= ~0x01;
				p |= I & 0x01;
				I = (K & 0xFF);
				zf = nf = I;
				ram[J] = (byte)I;
				break;
				
			// Zero Page 	STY $zz 	$84 	2 	4 
			case 0x84:
				ram[(readRam(pce, pc++))] = (byte)y;
				break;
				
			// Zero Page 	STA $zz 	$85 	2 	4 
			case 0x85:
				ram[(readRam(pce, pc++))] = (byte)a;
				break;
				
			// Zero Page 	STX $zz 	$86 	2 	4 
			case 0x86:
				ram[(readRam(pce, pc++))] = (byte)x;
				break;
				
			// Zero Page 	LDY $zz 	$a4 	2 	4 
			case 0xA4:
				y = (ram[(readRam(pce, pc++))] & 0xFF);
				zf = nf = y;
				break;
				
			// Zero Page 	LDA $zz 	$a5 	2 	4 
			case 0xA5:
				a = (ram[(readRam(pce, pc++))] & 0xFF);
				zf = nf = a;
				break;
				
			// Zero Page 	LDX $zz 	$a6 	2 	4 
			case 0xA6:
				x = (ram[(readRam(pce, pc++))] & 0xFF);
				zf = nf = x;
				break;
				
			// Zero Page 	CPY $zz 	$c4 	2 	4 
			case 0xC4:
				I = (ram[(readRam(pce, pc++))] & 0xFF);
				K = y - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Zero Page 	CMP $zz 	$c5 	2 	4 
			case 0xC5:
				I = (ram[(readRam(pce, pc++))] & 0xFF);
				K = a - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Zero Page 	DEC $zz 	$c6 	2 	6 
			case 0xC6:
				J = (readRam(pce, pc++));
				I = (ram[J] & 0xFF);
				I = (I - 1) & 0xFF;
				zf = nf = I;
				ram[J] = (byte)I;
				break;
				
			// Zero Page 	CPX $zz 	$e4 	2 	4 
			case 0xE4:
				I = (ram[(readRam(pce, pc++))] & 0xFF);
				K = x - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Zero Page 	SBC $zz 	$e5 	2 	4 
			case 0xE5:
				I = (ram[(readRam(pce, pc++))] & 0xFF);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) - (I & 0x0F) - (~p & 0x01);
					if (((K & 0xFF) & 0x10) != 0)
						K = (K & 0xFF00) | (K & 0xFF) - 6;
					K = (K & 0xFF) | (((a >> 4) - (I >> 4) - ((((K & 0xFF) & 0x10) == 0x10) ? 1 : 0)) << 8);
					if (((K >> 8) & 0x10) != 0)
						K = (K & 0xFF) | (((K >> 8) - 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((((K >> 8) & 0x10) != 0) ? 0 : 0x01);
					zf = nf = a;
					cycle++;
				} else {
					K = a - I - (~p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0 : 0x01);
					VF = ((a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;

			// Zero Page 	INC $zz 	$e6 	2 	6 
			case 0xE6:
				J = (readRam(pce, pc++));
				I = (ram[J] & 0xFF);
				I = (I + 1) & 0xFF;
				zf = nf = I;
				ram[J] = (byte)I;
				break;
				
			// Absolute 	ORA $aaaa 	$0d 	3 	5 
			case 0x0D:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				a |= I;
				zf = nf = a;
				break;
				
			// Absolute 	ASL $aaaa 	$0e 	3 	7 
			case 0x0E:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				p &= ~0x01;
				p |= I >> 7;
				I = (I << 1) & 0xFF;
				zf = nf = I;
				pce._Wr6502(J, I);
				break;
				
			// Absolute 	AND $aaaa 	$2d 	3 	5 
			case 0x2D:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				a &= I;
				zf = nf = a;
				break;
				
			// Absolute 	ROL $aaaa 	$2e 	3 	7 
			case 0x2E:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				K = ((I << 1) | (p & 0x01)) & 0xFF;
				p &= ~0x01;
				p |= I >> 7;
				I = K;
				zf = nf = I;
				pce._Wr6502(J, I);
				break;
				
			// Absolute 	EOR $aaaa 	$4d 	3 	5 
			case 0x4D:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				a ^= I;
				zf = nf = a;
				break;
				
			// Absolute 	LSR $aaaa 	$4e 	3 	7 
			case 0x4E:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				p &= ~0x01;
				p |= I & 0x01;
				I >>= 1;
				zf = nf = I;
				pce._Wr6502(J, I);
				break;
			
			// Absolute 	ADC $aaaa 	$6d 	3 	5 
			case 0x6D:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) + (I & 0x0F) + (p & 0x01);
					K |= (((a >> 4) + (I >> 4)) << 8);
					if ((K & 0xFF) > 9) {
						K = (K & 0xFF) + 6;
						K |= (((K >> 8) + 1) << 8);
					}
					if ((K >> 8) > 9)
						K = (K & 0xFF) | (((K >> 8) + 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
					zf = nf = a;
					cycle++;
				} else {
					K = a + I + (p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0x01 : 0);
					VF = (~(a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Absolute 	ROR $aaaa 	$6e 	3 	7 
			case 0x6E:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				K = (I >> 1) | (p << 7);
				p &= ~0x01;
				p |= I & 0x01;
				I = (K & 0xFF);
				zf = nf = I;
				pce._Wr6502(J, I);
				break;
				
			// Absolute 	STY $aaaa 	$8c 	3 	5 
			case 0x8C:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				pce._Wr6502(J, y);
				break;
				
			// Absolute 	STA $aaaa 	$8d 	3 	5 
			case 0x8D:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				pce._Wr6502(J, a);
				break;
				
			// Absolute 	STX $aaaa 	$8e 	3 	5 
			case 0x8E:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				pce._Wr6502(J, x);
				break;
				
			// Absolute 	LDY $aaaa 	$ac 	3 	5 
			case 0xAC:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				y = pce._Rd6502(J);
				zf = nf = y;
				break;
				
			// Absolute 	LDA $aaaa 	$ad 	3 	5 
			case 0xAD:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				a = pce._Rd6502(J);
				zf = nf = a;
				break;
				
			// Absolute 	LDX $aaaa 	$ae 	3 	5 
			case 0xAE:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				x = pce._Rd6502(J);
				zf = nf = x;
				break;
				
			// Absolute 	CPY $aaaa 	$cc 	3 	5 
			case 0xCC:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				K = y - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Absolute 	CMP $aaaa 	$cd 	3 	5 
			case 0xCD:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				K = a - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Absolute 	DEC $aaaa 	$ce 	3 	7 
			case 0xCE:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				I = (I - 1) & 0xFF;
				zf = nf = I;
				pce._Wr6502(J, I);
				break;
				
			// Absolute 	CPX $aaaa 	$ec 	3 	5 
			case 0xEC:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				K = x - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Absolute 	SBC $aaaa 	$ed 	3 	5 
			case 0xED:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) - (I & 0x0F) - (~p & 0x01);
					if (((K & 0xFF) & 0x10) != 0)
						K = (K & 0xFF00) | (K & 0xFF) - 6;
					K = (K & 0xFF)
							| (((a >> 4) - (I >> 4) - ((((K & 0xFF) & 0x10) == 0x10) ? 1 : 0)) << 8);
					if (((K >> 8) & 0x10) != 0)
						K = (K & 0xFF) | (((K >> 8) - 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((((K >> 8) & 0x10) != 0) ? 0 : 0x01);
					zf = nf = a;
					cycle++;
				} else {
					K = a - I - (~p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0 : 0x01);
					VF = ((a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Absolute 	INC $aaaa 	$ee 	3 	7 
			case 0xEE:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				I = pce._Rd6502(J);
				I = (I + 1) & 0xFF;
				zf = nf = I;
				pce._Wr6502(J, I);
				break;

			// Immediate 	ORA #$ii 	$09 	2 	2 
			case 0x09:
				I = readRam(pce, pc++);
				a |= I;
				zf = nf = a;
				break;
				
			case 0x29:
				I = readRam(pce, pc++);
				a &= I;
				zf = nf = a;
				break;
				
			// Immediate 	EOR #$ii 	$49 	2 	2 
			case 0x49:
				I = readRam(pce, pc++);
				a ^= I;
				zf = nf = a;
				break;
				
			case 0x69:
				I = readRam(pce, pc++);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) + (I & 0x0F) + (p & 0x01);
					K |= (((a >> 4) + (I >> 4)) << 8);
					if ((K & 0xFF) > 9) {
						K = (K & 0xFF) + 6;
						K |= (((K >> 8) + 1) << 8);
					}
					if ((K >> 8) > 9)
						K = (K & 0xFF) | (((K >> 8) + 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
					zf = nf = a;
					cycle++;
				} else {
					K = a + I + (p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0x01 : 0);
					VF = (~(a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Immediate 	LDY #$ii 	$a0 	2 	2 
			case 0xA0:
				y = readRam(pce, pc++);
				zf = nf = y;
				break;
				
			// Immediate 	LDX #$ii 	$a2 	2 	2 
			case 0xA2:
				x = readRam(pce, pc++);
				zf = nf = x;
				break;
				
			// Immediate 	LDA #$ii 	$a9 	2 	2 
			case 0xA9:
				a = readRam(pce, pc++);
				zf = nf = a;
				break;
				
			// Immediate 	CPY #$ii 	$c0 	2 	2 
			case 0xC0:
				I = readRam(pce, pc++);
				K = y - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Immediate 	CMP #$ii 	$c9 	2 	2 
			case 0xC9:
				I = readRam(pce, pc++);
				K = a - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Immediate 	CPX #$ii 	$e0 	2 	2 
			case 0xE0:
				I = readRam(pce, pc++);
				K = x - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Immediate 	SBC #$ii 	$e9 	2 	2 
			case 0xE9:
				I = readRam(pce, pc++);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) - (I & 0x0F) - (~p & 0x01);
					if (((K & 0xFF) & 0x10) != 0)
						K = (K & 0xFF00) | (K & 0xFF) - 6;
					K = (K & 0xFF)
							| (((a >> 4) - (I >> 4) - ((((K & 0xFF) & 0x10) == 0x10) ? 1
									: 0)) << 8);
					if (((K >> 8) & 0x10) != 0)
						K = (K & 0xFF) | (((K >> 8) - 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((((K >> 8) & 0x10) != 0) ? 0 : 0x01);
					zf = nf = a;
					cycle++;
				} else {
					K = a - I - (~p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0 : 0x01);
					VF = ((a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;

			// Zero Page, X 	ORA $zz,X 	$15 	2 	4 
			case 0x15:
				I = (ram[(readRam(pce, pc++) + x) & 0xFF] & 0xFF);
				a |= I;
				zf = nf = a;
				break;
				
			// Zero Page, X 	ASL $zz,X 	$16 	2 	6 
			case 0x16:
				J = (readRam(pce, pc++) + x) & 0xFF;
				I = (ram[J] & 0xFF);
				p &= ~0x01;
				p |= I >> 7;
				I = (I << 1) & 0xFF;
				zf = nf = I;
				ram[J] = (byte)I;
				break;
				
			// Zero Page, X 	AND $zz,X 	$35 	2 	4 
			case 0x35:
				I = (ram[(readRam(pce, pc++) + x) & 0xFF] & 0xFF);
				a &= I;
				zf = nf = a;
				break;
				
			// Zero Page, X 	ROL $zz,X 	$36 	2 	6 
			case 0x36:
				J = (readRam(pce, pc++) + x) & 0xFF;
				I = (ram[J] & 0xFF);
				K = ((I << 1) | (p & 0x01)) & 0xFF;
				p &= ~0x01;
				p |= I >> 7;
				I = K;
				zf = nf = I;
				ram[J] = (byte)I;
				break;
				
			// Zero Page, X 	EOR $zz,X 	$55 	2 	4 
			case 0x55:
				I = (ram[(readRam(pce, pc++) + x) & 0xFF] & 0xFF);
				a ^= I;
				zf = nf = a;
				break;
				
			// Zero Page, X 	LSR $zz,X 	$56 	2 	6 
			case 0x56:
				J = (readRam(pce, pc++) + x) & 0xFF;
				I = (ram[J] & 0xFF);
				p &= ~0x01;
				p |= I & 0x01;
				I >>= 1;
				zf = nf = I;
				ram[J] = (byte)I;
				break;
			
			// Zero Page, X 	ADC $zz,X 	$75 	2 	4 
			case 0x75:
				I = (ram[(readRam(pce, pc++) + x)] & 0xFF) & 0xFF;
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) + (I & 0x0F) + (p & 0x01);
					K |= (((a >> 4) + (I >> 4)) << 8);
					if ((K & 0xFF) > 9) {
						K = (K & 0xFF) + 6;
						K |= (((K >> 8) + 1) << 8);
					}
					if ((K >> 8) > 9)
						K = (K & 0xFF) | (((K >> 8) + 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
					zf = nf = a;
					cycle++;
				} else {
					K = a + I + (p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0x01 : 0);
					VF = (~(a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Zero Page, X 	ROR $zz,X 	$76 	2 	6 
			case 0x76:
				J = (readRam(pce, pc++) + x) & 0xFF;
				I = (ram[J] & 0xFF);
				K = (I >> 1) | (p << 7);
				p &= ~0x01;
				p |= I & 0x01;
				I = (K & 0xFF);
				zf = nf = I;
				ram[J] = (byte)I;
				break;
				
			// Zero Page, X 	STY $zz,X 	$94 	2 	4 
			case 0x94:
				ram[(readRam(pce, pc++) + x) & 0xFF] = (byte)y;
				break;
				
			// Zero Page, X 	STA $zz,X 	$95 	2 	4 
			case 0x95:
				ram[(readRam(pce, pc++) + x) & 0xFF] = (byte)a;
				break;
				
			// Zero Page, Y 	STX $zz,Y 	$96 	2 	4 
			case 0x96:
				ram[(readRam(pce, pc++) + y) & 0xFF] = (byte)x;
				break;
				
			// Zero Page, X 	LDY $zz,X 	$b4 	2 	4 
			case 0xB4:
				y = (ram[(readRam(pce, pc++) + x) & 0xFF] & 0xFF);
				zf = nf = y;
				break;
				
			// Zero Page, X 	LDA $zz,X 	$b5 	2 	4 
			case 0xB5:
				a = (ram[(readRam(pce, pc++) + x) & 0xFF] & 0xFF);
				zf = nf = a;
				break;
				
			// Zero Page, Y 	LDX $zz,Y 	$b6 	2 	4 
			case 0xB6:
				x = (ram[(readRam(pce, pc++) + y) & 0xFF] & 0xFF);
				zf = nf = x;
				break;
				
			// Zero Page, X 	CMP $zz,X 	$d5 	2 	4 
			case 0xD5:
				I = (ram[(readRam(pce, pc++) + x) & 0xFF] & 0xFF);
				K = a - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Zero Page, X 	DEC $zz,X 	$d6 	2 	6 
			case 0xD6:
				J = (readRam(pce, pc++) + x) & 0xFF;
				I = (ram[J] & 0xFF);
				I = (I - 1) & 0xFF;
				zf = nf = I;
				ram[J] = (byte)I;
				break;
				
			// Zero Page, X 	SBC $zz,X 	$f5 	2 	4 
			case 0xF5:
				I = (ram[(readRam(pce, pc++) + x) & 0xFF] & 0xFF);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) - (I & 0x0F) - (~p & 0x01);
					if (((K & 0xFF) & 0x10) != 0)
						K = (K & 0xFF00) | (K & 0xFF) - 6;
					K = (K & 0xFF)
							| (((a >> 4) - (I >> 4) - ((((K & 0xFF) & 0x10) == 0x10) ? 1
									: 0)) << 8);
					if (((K >> 8) & 0x10) != 0)
						K = (K & 0xFF) | (((K >> 8) - 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((((K >> 8) & 0x10) != 0) ? 0 : 0x01);
					zf = nf = a;
					cycle++;
				} else {
					K = a - I - (~p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0 : 0x01);
					VF = ((a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Zero Page, X 	INC $zz,X 	$f6 	2 	6 
			case 0xF6:
				J = (readRam(pce, pc++) + x) & 0xFF;
				I = (ram[J] & 0xFF);
				I = (I + 1) & 0xFF;
				zf = nf = I;
				ram[J] = (byte)I;
				break;

			// Absolute, Y 	ORA $aaaa,Y 	$19 	3 	5 
			case 0x19:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				a |= I;
				zf = nf = a;
				break;
				
			// Absolute, X 	ORA $aaaa,X 	$1d 	3 	5 
			case 0x1D:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				a |= I;
				zf = nf = a;
				break;
				
			// Absolute, X 	ASL $aaaa,X 	$1e 	3 	7 
			case 0x1E:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				p &= ~0x01;
				p |= I >> 7;
				I = (I << 1) & 0xFF;
				zf = nf = I;
				pce._Wr6502(J, I);
				break;
				
			// Absolute, Y 	AND $aaaa,Y 	$39 	3 	5 
			case 0x39:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				a &= I;
				zf = nf = a;
				break;
				
			// Absolute, X 	AND $aaaa,X 	$3d 	3 	5 
			case 0x3D:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				a &= I;
				zf = nf = a;
				break;
				
			// Absolute, X 	ROL $aaaa,X 	$3e 	3 	7 
			case 0x3E:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				K = ((I << 1) | (p & 0x01)) & 0xFF;
				p &= ~0x01;
				p |= I >> 7;
				I = (K & 0xFF);
				zf = nf = I;
				pce._Wr6502(J, I);
				break;
				
			// Absolute, Y 	EOR $aaaa,Y 	$59 	3 	5 
			case 0x59:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				a ^= I;
				zf = nf = a;
				break;
				
			// Absolute, X 	EOR $aaaa,X 	$5d 	3 	5 
			case 0x5D:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				a ^= I;
				zf = nf = a;
				break;
				
			// Absolute, X 	LSR $aaaa,X 	$5e 	3 	7 
			case 0x5E:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				p &= ~0x01;
				p |= I & 0x01;
				I >>= 1;
				zf = nf = I;
				pce._Wr6502(J, I);
				break;
			
			// Absolute, Y 	ADC $aaaa,Y 	$79 	3 	5 
			case 0x79:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) + (I & 0x0F) + (p & 0x01);
					K |= (((a >> 4) + (I >> 4)) << 8);
					if ((K & 0xFF) > 9) {
						K = (K & 0xFF) + 6;
						K |= (((K >> 8) + 1) << 8);
					}
					if ((K >> 8) > 9)
						K = (K & 0xFF) | (((K >> 8) + 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
					zf = nf = a;
					cycle++;
				} else {
					K = a + I + (p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0x01 : 0);
					VF = (~(a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Absolute, X 	ADC $aaaa,X 	$7d 	3 	5 
			case 0x7D:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) + (I & 0x0F) + (p & 0x01);
					K |= (((a >> 4) + (I >> 4)) << 8);
					if ((K & 0xFF) > 9) {
						K = (K & 0xFF) + 6;
						K |= (((K >> 8) + 1) << 8);
					}
					if ((K >> 8) > 9)
						K = (K & 0xFF) | (((K >> 8) + 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
					zf = nf = a;
					cycle++;
				} else {
					K = a + I + (p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0x01 : 0);
					VF = (~(a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Absolute, X 	ROR $aaaa,X 	$7e 	3 	7 
			case 0x7E:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				K = (I >> 1) | (p << 7);
				p &= ~0x01;
				p |= I & 0x01;
				I = (K & 0xFF);
				zf = nf = I;
				pce._Wr6502(J, I);
				break;
				
			// Absolute, Y 	STA $aaaa,Y 	$99 	3 	5 
			case 0x99:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J = (J + y) & 0xFFFF;
				pce._Wr6502(J, a);
				break;
				
			// Absolute, X 	STA $aaaa,X 	$9d 	3 	5 
			case 0x9D:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				pce._Wr6502(J, a);
				break;
			
			// Absolute, Y 	LDA $aaaa,Y 	$b9 	3 	5 
			case 0xB9:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J = (J + y) & 0xFFFF;
				a = pce._Rd6502(J);
				zf = nf = a;
				break;
				
			// Absolute, X 	LDY $aaaa,X 	$bc 	3 	5 
			case 0xBC:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				y = pce._Rd6502(J);
				zf = nf = y;
				break;
				
			// Absolute, X 	LDA $aaaa,X 	$bd 	3 	5 
			case 0xBD:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				a = pce._Rd6502(J);
				zf = nf = a;
				break;
				
			// Absolute, Y 	LDX $aaaa,Y 	$be 	3 	5 
			case 0xBE:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J = (J + y) & 0xFFFF;
				x = pce._Rd6502(J);
				zf = nf = x;
				break;
				
			// Absolute, Y 	CMP $aaaa,Y 	$d9 	3 	5
			case 0xD9:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				K = a - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Absolute, X 	CMP $aaaa,X 	$dd 	3 	5 
			case 0xDD:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				K = a - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Absolute, X 	DEC $aaaa,X 	$de 	3 	7 
			case 0xDE:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				I = (I - 1) & 0xFF;
				zf = nf = I;
				pce._Wr6502(J, I);
				break;
				
			// Absolute, Y 	SBC $aaaa,Y 	$f9 	3 	5 
			case 0xF9:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) - (I & 0x0F) - (~p & 0x01);
					if (((K & 0xFF) & 0x10) != 0)
						K = (K & 0xFF00) | (K & 0xFF) - 6;
					K = (K & 0xFF)
							| (((a >> 4) - (I >> 4) - ((((K & 0xFF) & 0x10) == 0x10) ? 1
									: 0)) << 8);
					if (((K >> 8) & 0x10) != 0)
						K = (K & 0xFF) | (((K >> 8) - 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((((K >> 8) & 0x10) != 0) ? 0 : 0x01);
					zf = nf = a;
					cycle++;
				} else {
					K = a - I - (~p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0 : 0x01);
					VF = ((a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Absolute, X 	SBC $aaaa,X 	$fd 	3 	5 
			case 0xFD:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) - (I & 0x0F) - (~p & 0x01);
					if (((K & 0xFF) & 0x10) != 0)
						K = (K & 0xFF00) | (K & 0xFF) - 6;
					K = (K & 0xFF) | (((a >> 4) - (I >> 4) - ((((K & 0xFF) & 0x10) == 0x10) ? 1 : 0)) << 8);
					if (((K >> 8) & 0x10) != 0)
						K = (K & 0xFF) | (((K >> 8) - 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((((K >> 8) & 0x10) != 0) ? 0 : 0x01);
					zf = nf = a;
					cycle++;
				} else {
					K = a - I - (~p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0 : 0x01);
					VF = ((a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Absolute, X 	INC $aaaa,X 	$fe 	3 	7 
			case 0xFE:
				J = readRam(pce, pc)
				   | ((readRam(pce, pc + 1)) << 8);
				pc += 2;
				J += x;
				I = pce._Rd6502(J);
				I = (I + 1) & 0xFF;
				zf = nf = I;
				pce._Wr6502(J, I);
				break;

			// Indexed Indirect 	ORA ($zz,X) 	$01 	2 	7 
			case 0x01:
				K = (readRam(pce, pc++) + x) & 0xFF;
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				a |= I;
				zf = nf = a;
				break;
				
			// Indirect, Index 	ORA ($zz),Y 	$11 	2 	7 
			case 0x11:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				a |= I;
				zf = nf = a;
				break;
				
			// Indexed Indirect 	AND ($zz,X) 	$21 	2 	7 
			case 0x21:
				K = (readRam(pce, pc++) + x) & 0xFF;
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				a &= I;
				zf = nf = a;
				break;
				
			// Indirect, Index 	AND ($zz),Y 	$31 	2 	7 
			case 0x31:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				a &= I;
				zf = nf = a;
				break;
				
			// Indexed Indirect 	EOR ($zz,X) 	$41 	2 	7 
			case 0x41:
				K = (readRam(pce, pc++) + x) & 0xFF;
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				a ^= I;
				zf = nf = a;
				break;
				
			// Indirect, Index 	EOR ($zz),Y 	$51 	2 	7 
			case 0x51:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				a ^= I;
				zf = nf = a;
				break;
			
			// Indexed Indirect 	ADC ($zz,X) 	$61 	2 	7 
			case 0x61:
				K = (readRam(pce, pc++) + x) & 0xFF;
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) + (I & 0x0F) + (p & 0x01);
					K |= (((a >> 4) + (I >> 4)) << 8);
					if ((K & 0xFF) > 9) {
						K = (K & 0xFF) + 6;
						K |= (((K >> 8) + 1) << 8);
					}
					if ((K >> 8) > 9)
						K = (K & 0xFF) | (((K >> 8) + 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
					zf = nf = a;
					cycle++;
				} else {
					K = a + I + (p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0x01 : 0);
					VF = (~(a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			case 0x71:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (a & 0x0F) + (I & 0x0F) + (p & 0x01);
					K |= (((a >> 4) + (I >> 4)) << 8);
					if ((K & 0xFF) > 9) {
						K = (K & 0xFF) + 6;
						K |= (((K >> 8) + 1) << 8);
					}
					if ((K >> 8) > 9)
						K = (K & 0xFF) | (((K >> 8) + 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((K >> 8) > 15 ? 0x01 : 0);
					zf = nf = a;
					cycle++;
				} else {
					K = a + I + (p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0x01 : 0);
					VF = (~(a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Indexed Indirect 	STA ($zz,X) 	$81 	2 	7 
			case 0x81:
				K = (readRam(pce, pc++) + x) & 0xFF;
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				pce._Wr6502(J, a);
				break;
				
			// Indirect, Index 	STA ($zz),Y 	$91 	2 	7 
			case 0x91:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				J = (J + y) & 0xFFFF;
				pce._Wr6502(J, a);
				break;
				
			// Indexed Indirect 	LDA ($zz,X) 	$a1 	2 	7 
			case 0xA1:
				K = (readRam(pce, pc++) + x) & 0xFF;
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				a = pce._Rd6502(J);
				zf = nf = a;
				break;
				
			// Indirect, Index 	LDA ($zz),Y 	$b1 	2 	7 
			case 0xB1:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				J = (J + y) & 0xFFFF;
				a = pce._Rd6502(J);
				zf = nf = a;
				break;
				
			// Indexed Indirect 	CMP ($zz,X) 	$c1 	2 	7 
			case 0xC1:
				K = (readRam(pce, pc++) + x) & 0xFF;
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				K = a - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Indirect, Index 	CMP ($zz),Y 	$d1 	2 	7 
			case 0xD1:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				K = a - I;
				p &= ~0x01;
				p |= (((K >> 8) != 0) ? 0 : 0x01);
				zf = nf = (K & 0xFF);
				break;
				
			// Indexed Indirect 	SBC ($zz,X) 	$e1 	2 	7 
			case 0xE1:
				K = (readRam(pce, pc++) + x) & 0xFF;
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (K & 0xFF00) | (a & 0x0F) - (I & 0x0F) - (~p & 0x01);
					if (((K & 0xFF) & 0x10) != 0)
						K = (K & 0xFF00) | (K & 0xFF) - 6;
					K = (K & 0xFF) | (((a >> 4) - (I >> 4) - ((((K & 0xFF) & 0x10) == 0x10) ? 1 : 0)) << 8);
					if (((K >> 8) & 0x10) != 0)
						K = (K & 0xFF) | (((K >> 8) - 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((((K >> 8) & 0x10) != 0) ? 0 : 0x01);
					zf = nf = a;
					cycle++;
				} else {
					K = a - I - (~p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0 : 0x01);
					VF = ((a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;
				
			// Indirect, Index 	SBC ($zz),Y 	$f1 	2 	7 
			case 0xF1:
				K = (readRam(pce, pc++));
				J = (ram[K]  & 0xFF) | ((ram[K + 1] & 0xFF) << 8);
				J = (J + y) & 0xFFFF;
				I = pce._Rd6502(J);
				if ((p & 0x08) != 0) {
					K = (K & 0xFF00) | (a & 0x0F) - (I & 0x0F) - (~p & 0x01);
					if (((K & 0xFF) & 0x10) != 0)
						K = (K & 0xFF00) | (K & 0xFF) - 6;
					K = (K & 0xFF) | (((a >> 4) - (I >> 4) - ((((K & 0xFF) & 0x10) == 0x10) ? 1 : 0)) << 8);
					if (((K >> 8) & 0x10) != 0)
						K = (K & 0xFF) | (((K >> 8) - 6) << 8);
					a = ((K & 0xFF) & 0x0F) | ((K >> 8) << 4);
					p = (p & ~0x01) | ((((K >> 8) & 0x10) != 0) ? 0 : 0x01);
					zf = nf = a;
					cycle++;
				} else {
					K = a - I - (~p & 0x01);
					p &= ~0x01;
					p |= (((K >> 8) != 0) ? 0 : 0x01);
					VF = ((a ^ I) & (a ^ (K & 0xFF))) >> 1;
					zf = nf = (K & 0xFF);
					a = (K & 0xFF);
				}
				break;

			// Accumulator 	ASL A 	$0a 	1 	2 
			case 0x0A:
				p &= ~0x01;
				p |= a >> 7;
				a = (a << 1) & 0xFF;
				zf = nf = a;
				break;
				
			// Accumulator 	ROL A 	$2a 	1 	2 
			case 0x2A:
				K = ((a << 1) | (p & 0x01)) & 0xFF;
				p &= ~0x01;
				p |= a >> 7;
				a = K;
				zf = nf = a;
				break;
				
			// Accumulator 	LSR A 	$4a 	1 	2 
			case 0x4A:
				p &= ~0x01;
				p |= a & 0x01;
				a >>= 1;
				zf = nf = a;
				break;
				
			// Accumulator 	ROR A 	$6a 	1 	2 
			case 0x6A:
				K = (a >> 1) | (p << 7);
				p &= ~0x01;
				p |= a & 0x01;
				a = (K & 0xFF);
				zf = nf = a;
				break;	

			default:
				if (TrapBadOps != 0) {
					debug.TRACE("[M6502 %lX] Unrecognized instruction: $%02X at pc=$%04X\n",
						User, readRam(pce, pc - 1), (char) (pc - 1));
					Trace = 1;
				}
				break;
			}

			ICount -= cycle;
			User += cycle;
			
			if (ICount <= 0) {
				if (AfterCLI != 0) {
					if ((IRequest & 4) != 0) {
						IRequest &= ~4;
						I = 4;
					} else	if ((IRequest & 1) != 0) {
						IRequest &= ~1;
						I = 1;
					} else if ((IRequest & 8) != 0) {
						IRequest &= ~8;
						I = 8;
					}

					ICount = 0;
					if (IRequest == 0) {
						ICount = IBackup;
						AfterCLI = 0;
					}
				} else {
					I = pce.Loop6502();
					ICount += IPeriod;
				}
				
				if (I != 0) {
					PC = pc;
					P = p;
					S = s;
					ZF = zf;
					NF = nf;
					Int6502(pce, I);
					pc = PC;
					p = P;
					s = S;
				}

				if ((long) (User - CycleCountOld) > (long) pce.TimerPeriod << 1)
					CycleCountOld = User;
				
				if (pce.isRefreshedScreen) {
					PC = pc;
					A = a;
					P = p;
					X = x;
					Y = y;
					S = s;
					ZF = zf;
					NF = nf;
					pce.isRefreshedScreen = false;
					return pc;
				} else if (pce.isLastLine) {
					pce.isLastLine = false;
				}
			} else {
				if (User - CycleCountOld >= pce.TimerPeriod) {
					CycleCountOld += pce.TimerPeriod;
					I = pce.TimerInt();
					if (I != 0) {
						PC = pc;
						P = p;
						S = s;
						ZF = zf;
						NF = nf;
						Int6502(pce, I);
						pc = PC;
						p = P;
						s = S;
					}
				}
			}
		}
	}
	
	public void saveState(DataOutputStream os) throws IOException {
		os.writeInt(PC);
		os.writeInt(A);
		os.writeInt(P);
		os.writeInt(X);
		os.writeInt(Y);
		os.writeInt(S);
		os.writeInt(VF);
		os.writeInt(IPeriod);
		os.writeInt(ICount);
		os.writeInt(IRequest);
		os.writeInt(AfterCLI);
		os.writeInt(ZF);
		os.writeInt(IBackup);
		os.writeInt(NF);
		os.writeInt(User);
		os.writeInt(TrapBadOps);
		os.writeInt(Trap);
		os.writeInt(Trace);
		for (int i = 0; i < MPR.length; i++) {
			os.writeInt(MPR[i]);
		}
		os.writeInt(CycleCountOld);
	}
	
	public void loadState(DataInputStream is) throws IOException {
		PC = is.readInt();
		A = is.readInt();
		P = is.readInt();
		X = is.readInt();
		Y = is.readInt();
		S = is.readInt();
		VF = is.readInt();
		IPeriod = is.readInt();
		ICount = is.readInt();
		IRequest = is.readInt();
		AfterCLI = is.readInt();
		ZF = is.readInt();
		IBackup = is.readInt();
		NF = is.readInt();
		User = is.readInt();
		TrapBadOps = is.readInt();
		Trap = is.readInt();
		Trace = is.readInt();
		for (int i = 0; i < MPR.length; i++) {
			MPR[i] = is.readInt();
		}
		CycleCountOld = is.readInt();
	}
}
