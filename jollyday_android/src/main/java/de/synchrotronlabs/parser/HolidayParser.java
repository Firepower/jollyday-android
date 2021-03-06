
/**
 * Copyright 2010 Sven Diedrichsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *
 * @author sven
 * @version $Id: $
 */
package de.synchrotronlabs.parser;

import java.util.Set;

import de.synchrotronlabs.Holiday;
import de.synchrotronlabs.config.Holidays;
public interface HolidayParser {
	/**
	 * <p>parse.</p>
	 *
	 * @param year a int.
	 * @param holidays a {@link java.util.Set} object.
	 * @param config a {@link de.synchrotronlabs.config.Holidays} object.
	 */
	void parse(int year, Set<Holiday> holidays, Holidays config);
}
