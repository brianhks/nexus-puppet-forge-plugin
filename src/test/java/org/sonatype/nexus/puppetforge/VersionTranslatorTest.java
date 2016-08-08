package org.sonatype.nexus.puppetforge;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 Created by bhawkins on 8/5/16.
 */
public class VersionTranslatorTest
{
	@Test
	public void testSomeVersions()
	{
		assertEquals("[3.2.0,5.0.0)", VersionTranslator.translateVersion(">= 3.2.0 < 5.0.0", null));

		assertEquals("[1.0,2.0)", VersionTranslator.translateVersion("1.x", null));

		assertEquals("1", VersionTranslator.translateVersion("1", null));
	}
}
