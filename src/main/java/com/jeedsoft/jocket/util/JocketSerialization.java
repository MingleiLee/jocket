package com.jeedsoft.jocket.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class JocketSerialization
{
	public static byte[] serialize(Object object)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			oos.flush();
			return baos.toByteArray();
		}
		catch (IOException e) {
			throw new JocketRuntimeException("Failed to serialize", e);
		}
		finally {
			try {baos.close();} catch (Exception e) {}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T deserialize(byte[] bytes)
	{
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		try {
			ObjectInputStream ois = new ObjectInputStream(bais);
			return (T)ois.readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			throw new JocketRuntimeException("Failed to deserialize", e);
		}
		finally {
			try {bais.close();} catch (Exception e) {}
		}
	}
}
