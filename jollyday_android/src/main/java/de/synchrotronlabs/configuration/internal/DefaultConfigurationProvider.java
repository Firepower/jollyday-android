/**
 * Copyright 2012 Sven Diedrichsen 
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
 */
package de.synchrotronlabs.configuration.internal;

import java.util.Properties;
import java.util.logging.Logger;

import de.synchrotronlabs.configuration.ConfigurationProvider;

/**
 * Provider which adds jollydays default configuration.
 * 
 * 
 * @author sven
 * 
 */
public class DefaultConfigurationProvider implements ConfigurationProvider {

	private static final Logger LOG = Logger.getLogger(DefaultConfigurationProvider.class.getName());

	/**
	 * The name of the configuration file.
	 */
	private static final String CONFIG_FILE = "jollyday.properties";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.jollyday.configuration.ConfigurationProvider#addConfiguration(java
	 * .util.Properties)
	 */
	public void putConfiguration(Properties properties) {
		properties.put("manager.impl","de.synchrotronlabs.impl.XMLManager");
		properties.put("parser.impl.de.jollyday.config.Fixed","de.synchrotronlabs.parser.impl.FixedParser");
		properties.put("parser.impl.de.jollyday.config.FixedWeekdayInMonth","de.synchrotronlabs.parser.impl.FixedWeekdayInMonthParser");
		properties.put("parser.impl.de.jollyday.config.IslamicHoliday","de.synchrotronlabs.parser.impl.IslamicHolidayParser");
		properties.put("parser.impl.de.jollyday.config.ChristianHoliday","de.synchrotronlabs.parser.impl.ChristianHolidayParser");
		properties.put("parser.impl.de.jollyday.config.RelativeToFixed","de.synchrotronlabs.parser.impl.RelativeToFixedParser");
		properties.put("parser.impl.de.jollyday.config.RelativeToWeekdayInMonth","de.synchrotronlabs.parser.impl.RelativeToWeekdayInMonthParser");
		properties.put("parser.impl.de.jollyday.config.FixedWeekdayBetweenFixed","de.synchrotronlabs.parser.impl.FixedWeekdayBetweenFixedParser");
		properties.put("parser.impl.de.jollyday.config.FixedWeekdayRelativeToFixed","de.synchrotronlabs.parser.impl.FixedWeekdayRelativeToFixedParser");
		properties.put("parser.impl.de.jollyday.config.EthiopianOrthodoxHoliday","de.synchrotronlabs.parser.impl.EthiopianOrthodoxHolidayParser");
		properties.put("parser.impl.de.jollyday.config.RelativeToEasterSunday","de.synchrotronlabs.parser.impl.RelativeToEasterSundayParser");
	}

}
