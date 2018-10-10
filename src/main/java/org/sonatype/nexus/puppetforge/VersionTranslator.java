/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sonatype.nexus.puppetforge;

import com.github.jknack.semver.*;
import org.slf4j.Logger;

/**
 Created by bhawkins on 8/5/16.
 */
public class VersionTranslator
{
	private static class RangeVersion
	{
		public String leftSymbol = "(";
		public String leftVersion = "";
		public String rightVersion = "";
		public String rightSymbol = ")";

		public String toString()
		{
			return (leftSymbol+leftVersion+","+rightVersion+rightSymbol);
		}
	}


	private static void translateRelationalOp(RelationalOp op, RangeVersion rangeVersion)
	{
		if (op == null)
			return;
		if (op instanceof RelationalOp.GreaterThan)
		{
			rangeVersion.leftSymbol = "(";
			rangeVersion.leftVersion = op.getSemver().toString();
		}
		else if (op instanceof RelationalOp.GreatherThanEqualsTo)
		{
			rangeVersion.leftSymbol = "[";
			rangeVersion.leftVersion = op.getSemver().toString();
		}
		else if (op instanceof RelationalOp.LessThan)
		{
			rangeVersion.rightSymbol = ")";
			rangeVersion.rightVersion = op.getSemver().toString();
		}
		else if (op instanceof RelationalOp.LessThanEqualsTo)
		{
			rangeVersion.rightSymbol = "]";
			rangeVersion.rightVersion = op.getSemver().toString();
		}
	}

	private static String printVersion(Version version)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(version.getMajor())
				.append(".")
				.append(version.getMinor());

		return sb.toString();
	}


	public static String translateVersion(String version, Logger log)
	{
		String ret = "[0.1,)";

		if (version != null)
		{
			Semver semver = Semver.create(version);

			if (semver instanceof com.github.jknack.semver.Version)
			{
				ret = semver.toString();
			}
			else if (semver instanceof RelationalOp)
			{
				RangeVersion rangeVersion = new RangeVersion();
				translateRelationalOp((RelationalOp) semver, rangeVersion);
				ret = rangeVersion.toString();
			}
			else if (semver instanceof AndExpression)
			{
				RangeVersion rangeVersion = new RangeVersion();
				translateRelationalOp((RelationalOp) ((AndExpression) semver).getLeft(), rangeVersion);
				translateRelationalOp((RelationalOp) ((AndExpression) semver).getRight(), rangeVersion);
				ret = rangeVersion.toString();
			}
			else if (semver instanceof Range)
			{
				Range range = (Range) semver;

				if (range.type().equals(Semver.Type.X_RANGE))
				{
					ret = "[" + printVersion(range.getLeft()) + "," +
							printVersion(range.getLeft().nextMajor()) + ")";
				}
				else
					ret = range.toString();
			}
		}

		if (log != null)
			log.debug("Translating "+version+" to "+ret);
		return ret;
	}
}
