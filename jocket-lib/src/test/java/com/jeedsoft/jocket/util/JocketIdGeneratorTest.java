package com.jeedsoft.jocket.util;

import org.junit.Assert;
import org.junit.Test;

public class JocketIdGeneratorTest
{
	@Test
	public void test()
	{
        String id = JocketIdGenerator.generate();
        Assert.assertEquals(20, id.length());
	}
}
