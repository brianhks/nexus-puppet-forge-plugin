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

import org.sonatype.nexus.proxy.repository.Repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 Created by bhawkins on 9/15/15.
 */
public class RepositoryUtils
{
	private RepositoryUtils() {
	}

	public static File getBaseDir(Repository repository)
			throws URISyntaxException, MalformedURLException
	{
		String localUrl = repository.getLocalUrl();
		if (isFile(localUrl)) {
			return new File(localUrl);
		}
		return new File(new URL(localUrl).toURI());
	}

	private static boolean isFile(String localUrl) {
		return localUrl.startsWith("/");
	}
}
