package com.jeedsoft.jocket.storage.redis;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.jeedsoft.jocket.util.JocketBase64;
import com.jeedsoft.jocket.util.JocketSerialization;

public class JocketRedisObjectNotation
{
	private static Map<Class<?>, String> classToPrefixMap = new HashMap<>();
	
	private static Map<String, Converter<?>> prefixToConverterMap = new HashMap<>();
	
	private static final int MAX_LONG_DIGITS = Long.valueOf(Long.MAX_VALUE).toString().length();
	
	static
	{
		register("B", Boolean.class, new BooleanConverter());
		register("C", Character.class, new CharacterConverter());
		register("D", Double.class, new DoubleConverter());
		register("F", Float.class, new FloatConverter());
		register("I", Integer.class, new IntegerConverter());
		register("L", Long.class, new LongConverter());
		register("O", Serializable.class, new SerializableConverter());
		register("S", String.class, new StringConverter());
		register("U", UUID.class, new UuidConverter());
		register("Byte", Byte.class, new ByteConverter());
		register("Date", Date.class, new DateConverter());
		register("Short", Short.class, new ShortConverter());
	}
	
	private static <T> void register(String prefix, Class<T> cls, Converter<T> converter)
	{
		classToPrefixMap.put(cls, prefix);
		prefixToConverterMap.put(prefix, converter);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> String toString(T object)
	{
		if (object == null) {
			return "null";
		}
		Class<?> cls = object.getClass();
		if (cls == Long.class) {
			return ((Long)object).toString();
		}
		String prefix = classToPrefixMap.get(cls);
		if (prefix == null) {
			if (!(object instanceof Serializable)) {
				throw new IllegalArgumentException("value is not serializable. class=" + cls);
			}
			prefix = "O";
		}
		Converter<T> converter = (Converter<T>)prefixToConverterMap.get(prefix);
		return prefix + ":" + converter.toString(object);
	}
	
	public static Map<String, String> toStringMap(Map<String, Object> objectMap)
	{
		Map<String, String> stringMap = new HashMap<>();
		for (Map.Entry<String, Object> entry: objectMap.entrySet()) {
			stringMap.put(entry.getKey(), toString(entry.getValue()));
		}
		return stringMap;
	}
	
	public static Map<String, Object> toObjectMap(Map<String, String> stringMap)
	{
		Map<String, Object> objectMap = new HashMap<>();
		for (Map.Entry<String, String> entry: stringMap.entrySet()) {
			objectMap.put(entry.getKey(), toObject(entry.getValue()));
		}
		return objectMap;
	}

	@SuppressWarnings("unchecked")
	public static <T> T toObject(String string)
	{
		if (string == null || string.length() == 0) {
			throw new IllegalArgumentException("argument cannot be empty");
		}
		if (string.equals("null")) {
			return null;
		}
		int index = string.indexOf(':');
		if (index == -1) {
			if (string.length() < MAX_LONG_DIGITS) {
				try {
					return (T)Long.valueOf(string);
				}
				catch (Exception e) {
					throw new IllegalArgumentException("':' character is required. argument: " + string);
				}
			}
			throw new IllegalArgumentException("':' character is required. argument: " + string);
		}
		Converter<T> converter = (Converter<T>)prefixToConverterMap.get(string.substring(0, index));
		if (converter == null) {
			throw new IllegalArgumentException("no converter defined. argument: " + string);
		}
		return converter.fromString(string.substring(index + 1));
	}
	
	private static interface Converter<T>
	{
		String toString(T object);
		
		T fromString(String string);
	}
	
	private static class BooleanConverter implements Converter<Boolean>
	{
		@Override
		public String toString(Boolean object)
		{
			return object.toString();
		}

		@Override
		public Boolean fromString(String string)
		{
			return Boolean.valueOf(string);
		}
	}

	private static class ByteConverter implements Converter<Byte>
	{
		@Override
		public String toString(Byte object)
		{
			return object.toString();
		}

		@Override
		public Byte fromString(String string)
		{
			return Byte.valueOf(string);
		}
	}
	
	private static class ShortConverter implements Converter<Short>
	{
		@Override
		public String toString(Short object)
		{
			return object.toString();
		}

		@Override
		public Short fromString(String string)
		{
			return Short.valueOf(string);
		}
	}

	private static class IntegerConverter implements Converter<Integer>
	{
		@Override
		public String toString(Integer object)
		{
			return object.toString();
		}

		@Override
		public Integer fromString(String string)
		{
			return Integer.valueOf(string);
		}
	}
	
	private static class LongConverter implements Converter<Long>
	{
		@Override
		public String toString(Long object)
		{
			return object.toString();
		}

		@Override
		public Long fromString(String string)
		{
			return Long.valueOf(string);
		}
	}
	
	private static class FloatConverter implements Converter<Float>
	{
		@Override
		public String toString(Float object)
		{
			return object.toString();
		}

		@Override
		public Float fromString(String string)
		{
			return Float.valueOf(string);
		}
	}

	private static class DoubleConverter implements Converter<Double>
	{
		@Override
		public String toString(Double object)
		{
			return object.toString();
		}

		@Override
		public Double fromString(String string)
		{
			return Double.valueOf(string);
		}
	}
	
	private static class CharacterConverter implements Converter<Character>
	{
		@Override
		public String toString(Character object)
		{
			return object.toString();
		}

		@Override
		public Character fromString(String string)
		{
			return string.charAt(0);
		}
	}

	private static class StringConverter implements Converter<String>
	{
		@Override
		public String toString(String object)
		{
			return object;
		}

		@Override
		public String fromString(String string)
		{
			return string;
		}
	}

	private static class DateConverter implements Converter<Date>
	{
		private ThreadLocal<DateFormat> format = new ThreadLocal<DateFormat>()
		{
			@Override
			protected DateFormat initialValue()
			{
				return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			}
		};
		
		@Override
		public String toString(Date object)
		{
			return format.get().format(object);
		}

		@Override
		public Date fromString(String string)
		{
			try {
				return format.get().parse(string);
			}
			catch (ParseException e) {
				throw new IllegalArgumentException("cannot parse '" + string + "' to date", e);
			}
		}
	}

	private static class UuidConverter implements Converter<UUID>
	{
		@Override
		public String toString(UUID object)
		{
			return object.toString();
		}

		@Override
		public UUID fromString(String string)
		{
			return UUID.fromString(string);
		}
	}
	
	private static class SerializableConverter implements Converter<Serializable>
	{
		@Override
		public String toString(Serializable object)
		{
			return JocketBase64.encode(JocketSerialization.serialize(object));
		}

		@Override
		public Serializable fromString(String string)
		{
			return JocketSerialization.deserialize(JocketBase64.decode(string));
		}
	}
}
