package com.example.androidtest;

//import com.nttdocomo.util.MessageDigest;

public class Util {
	public static void memsetByte(byte[] dst, int start, int length, byte value) {
		int end = start + length;
		for (int i = start; i < end; i++) {
			dst[i] = value;
		}
	}

	public static void memsetInt(int[] dst, int start, int length, int value) {
		int end = (start + length) & ~7;
		int i = start;
		if (start < end) {
			do {
				dst[i    ] = value;
				dst[i + 1] = value;
				dst[i + 2] = value;
				dst[i + 3] = value;
				i += 4;
				dst[i    ] = value;
				dst[i + 1] = value;
				dst[i + 2] = value;
				dst[i + 3] = value;
				i += 4;
			} while (i < end);
		}
		
		end = start + length;
		for (; i < end; i++) {
			dst[i] = value;			
		}
	}

	public static int min(int a, int b) {
		return b < a ? b : a;
	}
	
    public static String formatNumber(final int num, final int size) {
        String str = String.valueOf(num);
        while (str.length() < size) {
            str = "0" + str;
        }
        return str;
    }
    
//	public static String getMD5(SDbinding sd, String fileName)
//	{
//		String str = "";
//
//		try {
//			InputStream in = sd.openInputStream(fileName);
//			MessageDigest digest = MessageDigest.getInstance("MD5");
//			byte[] buff = new byte[4096];
//			int len = 0;
//			while ((len = in.read(buff, 0, buff.length)) >= 0)
//			{
//				digest.update(buff,0,len);
//			}
//			byte[] hash = digest.digest();
//			str = toHexString(hash);
//		} catch (Exception e) {
//		}
//
//		return str;
//	}
	
	private static String toHexString(byte[] arr)
	{
		StringBuffer buff = new StringBuffer(arr.length * 2 );
		for( int i = 0; i < arr.length; i++)
		{
			String b = Integer.toHexString(arr[i] & 0xff);
			b = b.toUpperCase();
			if ( b.length() == 1 )
			{
				buff.append("0");
			}
			buff.append(b);
		}
		return buff.toString();
	}
	
	public static String[] spritString(String str, int length) {
		String[] ret = new String[str.length() / length];
		
		for (int i = 0, cnt = 0; i < str.length(); i += length, cnt++) {
			ret[cnt] = str.substring(i, i + length);
		}
		
		return ret;
	}
}
