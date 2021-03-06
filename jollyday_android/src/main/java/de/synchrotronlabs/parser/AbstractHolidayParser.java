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
package de.synchrotronlabs.parser;

import org.joda.time.LocalDate;

import de.synchrotronlabs.config.ChronologyType;
import de.synchrotronlabs.config.Holiday;
import de.synchrotronlabs.config.MoveableHoliday;
import de.synchrotronlabs.config.MovingCondition;
import de.synchrotronlabs.config.With;
import de.synchrotronlabs.util.CalendarUtil;
import de.synchrotronlabs.util.XMLUtil;

/**
 * The abstract base class for all HolidayParser implementations.
 * 
 * @author Sven Diedrichsen
 * @version $Id: $
 */
public abstract class AbstractHolidayParser implements HolidayParser {

	/**
	 * Calendar utility class.
	 */
	protected CalendarUtil calendarUtil = new CalendarUtil();
	/**
	 * XML utility class.
	 */
	protected XMLUtil xmlUtil = new XMLUtil();

	/**
	 * Evaluates if the provided <code>Holiday</code> instance is valid for the
	 * provided year.
	 * 
	 * @param h
	 *            The holiday configuration entry to validate
	 * @param year
	 *            The year to validate against.
	 * @return is valid for the year.
	 */
	protected <T extends Holiday> boolean isValid(T h, int year) {
		return isValidInYear(h, year) && isValidForCycle(h, year);
	}

	/**
	 * Checks cyclic holidays and checks if the requested year is hit within the
	 * cycles.
	 * 
	 * @param <T>
	 *            Holiday
	 * @param h
	 *            Holiday
	 * @param year
	 *            the year to check against
	 * @return is valid
	 */
	private <T extends Holiday> boolean isValidForCycle(T h, int year) {
		if (h.getEvery() != null) {
			if (!"EVERY_YEAR".equals(h.getEvery())) {
				if ("ODD_YEARS".equals(h.getEvery())) {
					return year % 2 != 0;
				} else if ("EVEN_YEARS".equals(h.getEvery())) {
					return year % 2 == 0;
				} else {
					if (h.getValidFrom() != null) {
						int cycleYears = 0;
						if ("2_YEARS".equalsIgnoreCase(h.getEvery())) {
							cycleYears = 2;
						} else if ("3_YEARS".equalsIgnoreCase(h.getEvery())) {
							cycleYears = 3;
						} else if ("4_YEARS".equalsIgnoreCase(h.getEvery())) {
							cycleYears = 4;
						} else if ("5_YEARS".equalsIgnoreCase(h.getEvery())) {
							cycleYears = 5;
						} else if ("6_YEARS".equalsIgnoreCase(h.getEvery())) {
							cycleYears = 6;
						} else {
							throw new IllegalArgumentException("Cannot handle unknown cycle type '" + h.getEvery()
									+ "'.");
						}
						return (year - h.getValidFrom().intValue()) % cycleYears == 0;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Checks whether the holiday is within the valid date range.
	 * 
	 * @param <T>
	 * @param h
	 * @param year
	 * @return is valid.
	 */
	private <T extends Holiday> boolean isValidInYear(T h, int year) {
		return (h.getValidFrom() == null || h.getValidFrom().intValue() <= year)
				&& (h.getValidTo() == null || h.getValidTo().intValue() >= year);
	}

	/**
	 * Moves a date if there are any moving conditions for this holiday and any
	 * of them fit.
	 * 
	 * @param fm
	 *            a {@link de.synchrotronlabs.config.MoveableHoliday} object.
	 * @param fixed
	 *            a {@link org.joda.time.LocalDate} object.
	 * @return the moved date
	 */
	protected LocalDate moveDate(MoveableHoliday fm, LocalDate fixed) {
		for (MovingCondition mc : fm.getMovingCondition()) {
			if (shallBeMoved(fixed, mc)) {
				fixed = moveDate(mc, fixed);
				break;
			}
		}
		return fixed;
	}

	/**
	 * Determines if the provided date shall be substituted.
	 * 
	 * @param fixed
	 *            a {@link org.joda.time.LocalDate} object.
	 * @param mc
	 *            a {@link de.synchrotronlabs.config.MovingCondition} object.
	 * @return a boolean.
	 */
	protected boolean shallBeMoved(LocalDate fixed, MovingCondition mc) {
		return fixed.getDayOfWeek() == xmlUtil.getWeekday(mc.getSubstitute());
	}

	/**
	 * Moves the date using the FixedMoving information
	 * 
	 * @param mc
	 * @param fixed
	 * @return
	 */
	private LocalDate moveDate(MovingCondition mc, LocalDate fixed) {
		int weekday = xmlUtil.getWeekday(mc.getWeekday());
		int direction = (mc.getWith() == With.NEXT ? 1 : -1);
		while (fixed.getDayOfWeek() != weekday) {
			fixed = fixed.plusDays(direction);
		}
		return fixed;
	}

	/**
	 * <p>
	 * getEasterSunday.
	 * </p>
	 * 
	 * @param year
	 *            a int.
	 * @param ct
	 *            a {@link de.synchrotronlabs.config.ChronologyType} object.
	 * @return a {@link org.joda.time.LocalDate} object.
	 */
	protected LocalDate getEasterSunday(int year, ChronologyType ct) {
		LocalDate easterSunday;
		if (ct == ChronologyType.JULIAN) {
			easterSunday = calendarUtil.getJulianEasterSunday(year);
		} else if (ct == ChronologyType.GREGORIAN) {
			easterSunday = calendarUtil.getGregorianEasterSunday(year);
		} else {
			easterSunday = calendarUtil.getEasterSunday(year);
		}
		return easterSunday;
	}

}
