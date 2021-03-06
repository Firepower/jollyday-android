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
 */
package de.synchrotronlabs.impl;

import android.content.res.AssetManager;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.ReadableInterval;

import de.synchrotronlabs.CalendarHierarchy;
import de.synchrotronlabs.Holiday;
import de.synchrotronlabs.HolidayManager;
import de.synchrotronlabs.config.Configuration;
import de.synchrotronlabs.config.Holidays;
import de.synchrotronlabs.parser.HolidayParser;
import de.synchrotronlabs.parser.impl.ChristianHolidayParser;
import de.synchrotronlabs.parser.impl.EthiopianOrthodoxHolidayParser;
import de.synchrotronlabs.parser.impl.FixedParser;
import de.synchrotronlabs.parser.impl.FixedWeekdayBetweenFixedParser;
import de.synchrotronlabs.parser.impl.FixedWeekdayInMonthParser;
import de.synchrotronlabs.parser.impl.FixedWeekdayRelativeToFixedParser;
import de.synchrotronlabs.parser.impl.HinduHolidayParser;
import de.synchrotronlabs.parser.impl.IslamicHolidayParser;
import de.synchrotronlabs.parser.impl.RelativeToEasterSundayParser;
import de.synchrotronlabs.parser.impl.RelativeToFixedParser;
import de.synchrotronlabs.parser.impl.RelativeToWeekdayInMonthParser;
import de.synchrotronlabs.util.ClassLoadingUtil;
import de.synchrotronlabs.util.XMLUtil;

/**
 * Manager implementation for reading data from XML files. The files with the
 * name pattern Holidays_[country].xml will be read from the system classpath.
 * It uses a list a parsers for parsing the different type of XML nodes.
 * 
 * @author Sven Diedrichsen
 * @version $Id: $
 */
public class XMLManager extends HolidayManager {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(XMLManager.class.getName());
	/**
	 * The configuration prefix for parser implementations.
	 */
	private static final String PARSER_IMPL_PREFIX = "parser.impl.";
	/**
	 * prefix of the config files.
	 */
	private static final String FILE_PREFIX = "holidays/Holidays";
	/**
	 * suffix of the config files.
	 */
	private static final String FILE_SUFFIX = ".xml";

	/**
	 * Parser cache by XML class name.
	 */
	private final Map<String, HolidayParser> parserCache = new HashMap<String, HolidayParser>();
	/**
	 * Configuration parsed on initialization.
	 */
	protected Configuration configuration;
	/**
	 * Utility class to handle class loading
	 */
	private ClassLoadingUtil classLoadingUtil = new ClassLoadingUtil();
	/**
	 * XML utility class.
	 */
	private XMLUtil xmlUtil = new XMLUtil();
	
	private Collection<HolidayParser> parsers;

	/**
	 * {@inheritDoc}
	 * 
	 * Calls
	 * <code>Set&lt;LocalDate&gt; getHolidays(int year, Configuration c, String... args)</code>
	 * with the configuration from initialization.
	 */
	@Override
	public Set<Holiday> getHolidays(int year, final String... args) {
		Set<Holiday> holidaySet = Collections.synchronizedSet(new HashSet<Holiday>());
		getHolidays(year, configuration, holidaySet, args);
		return holidaySet;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Calls <code>getHolidays(year, args)</code> for each year within the
	 * interval and returns a list of holidays which are then contained in the
	 * interval.
	 */
	@Override
	public Set<Holiday> getHolidays(ReadableInterval interval, final String... args) {
		if (interval == null) {
			throw new IllegalArgumentException("Interval is NULL.");
		}
		Set<Holiday> holidays = new HashSet<Holiday>();
		for (int year = interval.getStart().getYear(); year <= interval.getEnd().getYear(); year++) {
			Set<Holiday> yearHolidays = getHolidays(year, args);
			for (Holiday h : yearHolidays) {
				if (interval.contains(h.getDate().toDateTimeAtStartOfDay())) {
					holidays.add(h);
				}
			}
		}
		return holidays;
	}

	/**
	 * Parses the provided configuration for the provided year and fills the
	 * list of holidays.
	 * 
	 * @param year
	 * @param c
	 * @param holidaySet
	 * @param args
	 */
	private void getHolidays(int year, final Configuration c, Set<Holiday> holidaySet, final String... args) {
		if (LOG.isLoggable(Level.FINER)) {
			LOG.finer("Adding holidays for " + c.getDescription());
		}
		parseHolidays(year, holidaySet, c.getHolidays());
		if (args != null && args.length > 0) {
			String hierarchy = args[0];
			for (Configuration config : c.getSubConfigurations()) {
				if (hierarchy.equalsIgnoreCase(config.getHierarchy())) {
					getHolidays(year, config, holidaySet, copyOfRange(args, 1, args.length));
					break;
				}
			}
		}
	}

	/**
	 * Copies the specified range from the original array to a new array. This
	 * is a replacement for Java 1.6 Arrays.copyOfRange() specialized in String.
	 * 
	 * @param original
	 *            the original array to copy range from
	 * @param from
	 *            the start of the range to copy from the original array
	 * @param to
	 *            the inclusive end of the range to copy from the original array
	 * @return the copied range
	 */
	private String[] copyOfRange(final String[] original, int from, int to) {
		int newLength = to - from;
		if (newLength < 0) {
			throw new IllegalArgumentException(from + " > " + to);
		}
		String[] copy = new String[newLength];
		System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
		return copy;
	}

	/**
	 * Iterates of the list of parsers and calls parse on each of them.
	 * 
	 * @param year
	 * @param holidays
	 * @param config
	 */
	private void parseHolidays(int year, Set<Holiday> holidays, final Holidays config) {
		Collection<HolidayParser> parsers = getParsers(config);
		for (HolidayParser p : parsers) {
			HolidayParserRunner holidayParserRunner = new HolidayParserRunner(year, holidays, config, p);
			holidayParserRunner.run();
		}
	}

	/**
	 * Private class which is used to asyncronisly parse holiday configuration.
	 * 
	 * @author Sven
	 * 
	 */
	private static class HolidayParserRunner implements Runnable {

		private final int year;
		private final Set<Holiday> holidays;
		private final Holidays config;
		private final HolidayParser parser;

		public HolidayParserRunner(int year, Set<Holiday> holidays, final Holidays config, HolidayParser parser) {
			this.year = year;
			this.holidays = holidays;
			this.config = config;
			this.parser = parser;
		}

		public void run() {
			parser.parse(year, holidays, config);
		}

	}

	/**
	 * Creates a list of parsers by reading the configuration and trying to find
	 * an <code>HolidayParser</code> implementation for by XML class type.
	 * 
	 * @param config
	 * @return A list of parsers to for this configuration.
	 */
	private Collection<HolidayParser> getParsers(final Holidays config) {
		if (parsers == null) {
			parsers = new ArrayList<>();
			parsers.add(new ChristianHolidayParser());
			parsers.add(new EthiopianOrthodoxHolidayParser());
			parsers.add(new FixedParser());
			parsers.add(new FixedWeekdayBetweenFixedParser());
			parsers.add(new FixedWeekdayInMonthParser());
			parsers.add(new FixedWeekdayRelativeToFixedParser());
			parsers.add(new HinduHolidayParser());
			parsers.add(new IslamicHolidayParser());
			parsers.add(new RelativeToEasterSundayParser());
			parsers.add(new RelativeToFixedParser());
			parsers.add(new RelativeToWeekdayInMonthParser());
		}
		return parsers;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Initializes the XMLManager by loading the holidays XML file as resource
	 * from the classpath. When the XML file is found it will be unmarshalled
	 * with JAXB to some Java classes.
	 */
	@Override
	public void init(final String calendar, AssetManager am) {

		String configurationFileName = getConfigurationFileName(calendar);
		InputStream inputStream = null;
		try {
			inputStream = am.open(configurationFileName);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate configuration.", e);
		}
		init(calendar, inputStream);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Initializes the XMLManager by loading the holidays XML file as resource
	 * from the classpath. When the XML file is found it will be unmarshalled
	 * with JAXB to some Java classes.
	 */
	@Override
	public void init(String calendar, final InputStream inputStream) {
		try {
			configuration = xmlUtil.unmarshallConfiguration(inputStream);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot instantiate configuration.", e);
		}
		validateConfigurationHierarchy(configuration);
		logHierarchy(configuration, 0);
	}

	/**
	 * Logs the hierarchy structure.
	 * 
	 * @param c
	 *            Configuration to log hierarchy for.
	 * @param level
	 *            a int.
	 */
	protected static void logHierarchy(final Configuration c, int level) {
		if (LOG.isLoggable(Level.FINER)) {
			StringBuilder space = new StringBuilder();
			for (int i = 0; i < level; i++) {
				space.append("-");
			}
			LOG.finer(space + " " + c.getDescription() + "(" + c.getHierarchy() + ").");
			for (Configuration sub : c.getSubConfigurations()) {
				logHierarchy(sub, level + 1);
			}
		}
	}

	/**
	 * Returns the configuration file name for the country.
	 * 
	 * @param country
	 *            a {@link java.lang.String} object.
	 * @return file name
	 */
	public static String getConfigurationFileName(final String country) {
		return FILE_PREFIX + "_" + country + FILE_SUFFIX;
	}

	/**
	 * Validates the content of the provided configuration by checking for
	 * multiple hierarchy entries within one configuration. It traverses down
	 * the configuration tree.
	 * 
	 * @param c
	 *            a {@link de.synchrotronlabs.config.Configuration} object.
	 */
	protected static void validateConfigurationHierarchy(final Configuration c) {
		Map<String, Integer> hierarchyMap = new HashMap<String, Integer>();
		Set<String> multipleHierarchies = new HashSet<String>();
		for (Configuration subConfig : c.getSubConfigurations()) {
			String hierarchy = subConfig.getHierarchy();
			if (!hierarchyMap.containsKey(hierarchy)) {
				hierarchyMap.put(hierarchy, Integer.valueOf(1));
			} else {
				int count = hierarchyMap.get(hierarchy).intValue();
				hierarchyMap.put(hierarchy, Integer.valueOf(++count));
				multipleHierarchies.add(hierarchy);
			}
		}
		if (multipleHierarchies.size() > 0) {
			StringBuilder msg = new StringBuilder();
			msg.append("Configuration for " + c.getHierarchy()
					+ " contains  multiple SubConfigurations with the same hierarchy id. ");
			for (String hierarchy : multipleHierarchies) {
				msg.append(hierarchy + " " + hierarchyMap.get(hierarchy).toString() + " times ");
			}
			throw new IllegalArgumentException(msg.toString().trim());
		}
		for (Configuration subConfig : c.getSubConfigurations()) {
			validateConfigurationHierarchy(subConfig);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Returns the configurations hierarchy.<br>
	 * i.e. Hierarchy 'us' -> Children 'al','ak','ar', ... ,'wv','wy'. Every
	 * child might itself have children. The ids be used to call
	 * getHolidays()/isHoliday().
	 */
	@Override
	public CalendarHierarchy getCalendarHierarchy() {
		return createConfigurationHierarchy(configuration, null);
	}

	/**
	 * Creates the configuration hierarchy for the provided configuration.
	 * 
	 * @param c
	 * @return configuration hierarchy
	 */
	private static CalendarHierarchy createConfigurationHierarchy(final Configuration c, CalendarHierarchy h) {
		h = new CalendarHierarchy(h, c.getHierarchy());
		h.setFallbackDescription(c.getDescription());
		for (Configuration sub : c.getSubConfigurations()) {
			CalendarHierarchy subHierarchy = createConfigurationHierarchy(sub, h);
			h.getChildren().put(subHierarchy.getId(), subHierarchy);
		}
		return h;
	}

}
