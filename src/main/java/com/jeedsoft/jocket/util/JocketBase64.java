package com.jeedsoft.jocket.util;

public class JocketBase64
{
	private static final char[] ENCODE_MAP = 
	{
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
		'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
		'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
		'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
		'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
		'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', '+', '/'
	};
	
	private static final int[] DECODE_MAP =
	{
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
		52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,
		-1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
		15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
		-1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
		41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1
	};
	
	public static String encode(String s)
	{
		try {
			return encode(s.getBytes("utf-8"));
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static String encode(byte[] bytes)
	{
		int n = bytes.length;
		StringBuilder sb = new StringBuilder(1 + n * 4 / 3);
		for (int i = 0; i < n; i += 3) {
			int count = Math.min(3, n - i);
			int data = 0;
			for (int j = 0; j < count; ++j) {
				data |= ((int)bytes[i + j] & 0xFF) << (16 - 8 * j);
			}
			for (int j = 0; j <= count; ++j) {
				sb.append(ENCODE_MAP[(data >> (18 - 6 * j)) & 0x3F]);
			}
		}
		while (sb.length() % 4 != 0) {
			sb.append('=');
		}
		return sb.toString();
	}

	public static byte[] decode(String s)
	{
		int n = s.length();
		while (n > 0 && s.charAt(n - 1) == '=') {
			--n;
		}
		byte[] bytes = new byte[n * 3 / 4];
		for (int i = 0, k = 0; i < n; i += 4) {
			int count = Math.min(3, n - i - 1);
			int data = 0;
			for (int j = 0; j <= count; ++j) {
				data |= DECODE_MAP[s.charAt(i + j)] << (18 - 6 * j);
			}
			for (int j = 0; j < count; ++j) {
				bytes[k++] = (byte)(data >> (16 - 8 * j));
			}
		}
		return bytes;
	}

	public static String decodeToString(String s)
	{
		byte[] bytes = decode(s);
		return new String(bytes);
	}
}
