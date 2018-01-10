package com.jeedsoft.jocket.connection;

import java.util.UUID;

public class JocketSessionIdGenerator
{
	private static final char[] CHARS = 
	{
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
		'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
		'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
		'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
		'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
		'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', '-', '_'
	};

	public static String generate()
	{
		StringBuilder sb = new StringBuilder();
		UUID uuid = UUID.randomUUID();
		long a = uuid.getMostSignificantBits();
		long b = uuid.getLeastSignificantBits();
		b = ((a & 0xFL) << 60) | (b & 0xFFF_FFFF_FFFF_FFFFL);
		a = ((a >>> 4) & 0xFFL) | ((a >>> 8) & 0xFF_FFFF_FFFF_FF00L);
		for (int i = 0; i < 20; ++i) {
			sb.append(CHARS[(int)b & 0x3F]);
			b = ((a & 0x3F) << 58) | (b >>> 6);
			a >>>= 6;
		}
		return sb.toString();
	}
	
	public static void main(String[] args)
	{
		for (int i = 0; i < 10; ++i) {
			System.out.println(generate());
		}
	}
}
