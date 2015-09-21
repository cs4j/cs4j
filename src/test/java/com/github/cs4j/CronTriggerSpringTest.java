package com.github.cs4j;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Original CronSequenceGenerator tests from Spring package.
 *
 * @author Dave Syer
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@RunWith(Parameterized.class)
public class CronTriggerSpringTest extends Assert {

    private final Calendar calendar = new GregorianCalendar();

    private final Date date;

    private final TimeZone timeZone;


    public CronTriggerSpringTest(Date date, TimeZone timeZone) {
        this.date = date;
        this.timeZone = timeZone;
    }

    @Parameters(name = "date [{0}], time zone [{1}]")
    public static List<Object[]> getParameters() {
        List<Object[]> list = new ArrayList<>();
        list.add(new Object[]{new Date(), TimeZone.getTimeZone("PST")});
        list.add(new Object[]{new Date(), TimeZone.getTimeZone("CET")});
        return list;
    }

    private static void roundup(Calendar calendar) {
        calendar.add(Calendar.SECOND, 1);
        calendar.set(Calendar.MILLISECOND, 0);
    }


    @Before
    public void setUp() {
        calendar.setTimeZone(timeZone);
        calendar.setTime(date);
        roundup(calendar);
    }

    @Test
    public void testMatchAll() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("* * * * * *", timeZone);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testMatchLastSecond() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("* * * * * *", timeZone);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.SECOND, 58);
        assertMatchesNextSecond(trigger, calendar);
    }

    @Test
    public void testMatchSpecificSecond() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("10 * * * * *", timeZone);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.SECOND, 9);
        assertMatchesNextSecond(trigger, calendar);
    }

    @Test
    public void testIncrementSecondByOne() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("11 * * * * *", timeZone);
        calendar.set(Calendar.SECOND, 10);
        Date date = calendar.getTime();
        calendar.add(Calendar.SECOND, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementSecondWithPreviousExecutionTooEarly() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("11 * * * * *", timeZone);
        calendar.set(Calendar.SECOND, 11);
        Date date = calendar.getTime();

        calendar.add(Calendar.MINUTE, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementSecondAndRollover() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("10 * * * * *", timeZone);
        calendar.set(Calendar.SECOND, 11);
        Date date = calendar.getTime();
        calendar.add(Calendar.SECOND, 59);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testSecondRange() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("10-15 * * * * *", timeZone);
        calendar.set(Calendar.SECOND, 9);
        assertMatchesNextSecond(trigger, calendar);
        calendar.set(Calendar.SECOND, 14);
        assertMatchesNextSecond(trigger, calendar);
    }

    @Test
    public void testIncrementMinute() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 * * * * *", timeZone);
        calendar.set(Calendar.MINUTE, 10);
        Date date = calendar.getTime();
        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        calendar.add(Calendar.MINUTE, 1);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testIncrementMinuteByOne() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 11 * * * *", timeZone);
        calendar.set(Calendar.MINUTE, 10);
        Date date = calendar.getTime();

        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementMinuteAndRollover() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 10 * * * *", timeZone);
        calendar.set(Calendar.MINUTE, 11);
        calendar.set(Calendar.SECOND, 0);
        Date date = calendar.getTime();
        calendar.add(Calendar.MINUTE, 59);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementHour() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 * * * *", timeZone);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        Date date = calendar.getTime();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        calendar.set(Calendar.HOUR_OF_DAY, 13);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementHourAndRollover() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 * * * *", timeZone);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DAY_OF_MONTH, 10);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 0);
        Date date = calendar.getTime();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 11);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementDayOfMonth() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 0 * * *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        assertEquals(2, calendar.get(Calendar.DAY_OF_MONTH));
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        assertEquals(3, calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testIncrementDayOfMonthByOne() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("* * * 10 * *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 9);
        Date date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementDayOfMonthAndRollover() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("* * * 10 * *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 11);
        Date date = calendar.getTime();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 10);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testDailyTriggerInShortMonth() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 0 * * *", timeZone);
        calendar.set(Calendar.MONTH, 8); // September: 30 days
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Date date = calendar.getTime();
        calendar.set(Calendar.MONTH, 9); // October
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.set(Calendar.DAY_OF_MONTH, 2);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testDailyTriggerInLongMonth() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 0 * * *", timeZone);
        calendar.set(Calendar.MONTH, 7); // August: 31 days and not a daylight saving boundary
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Date date = calendar.getTime();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 31);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.set(Calendar.MONTH, 8); // September
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testDailyTriggerOnDaylightSavingBoundary() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 0 * * *", timeZone);
        calendar.set(Calendar.MONTH, 9); // October: 31 days and a daylight saving boundary in CET
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Date date = calendar.getTime();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 31);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.set(Calendar.MONTH, 10); // November
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementMonth() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 0 1 * *", timeZone);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Date date = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MONTH, 10);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.set(Calendar.MONTH, 11);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementMonthAndRollover() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 0 1 * *", timeZone);
        calendar.set(Calendar.MONTH, 11);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        calendar.set(Calendar.YEAR, 2010);
        Date date = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.YEAR, 2011);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.set(Calendar.MONTH, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testMonthlyTriggerInLongMonth() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 0 31 * *", timeZone);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Date date = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testMonthlyTriggerInShortMonth() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 0 1 * *", timeZone);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        Date date = calendar.getTime();
        calendar.set(Calendar.MONTH, 10);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testIncrementDayOfWeekByOne() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("* * * * * 2", timeZone);
        calendar.set(Calendar.DAY_OF_WEEK, 2);
        Date date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_WEEK, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
        assertEquals(Calendar.TUESDAY, calendar.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void testIncrementDayOfWeekAndRollover() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("* * * * * 2", timeZone);
        calendar.set(Calendar.DAY_OF_WEEK, 4);
        Date date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 6);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
        assertEquals(Calendar.TUESDAY, calendar.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void testSpecificMinuteSecond() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("55 5 * * * *", timeZone);
        calendar.set(Calendar.MINUTE, 4);
        calendar.set(Calendar.SECOND, 54);
        Date date = calendar.getTime();

        calendar.add(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 55);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.add(Calendar.HOUR, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testSpecificHourSecond() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("55 * 10 * * *", timeZone);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.SECOND, 54);
        Date date = calendar.getTime();

        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 55);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.add(Calendar.MINUTE, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testSpecificMinuteHour() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("* 5 10 * * *", timeZone);
        calendar.set(Calendar.MINUTE, 4);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        Date date = calendar.getTime();
        calendar.add(Calendar.MINUTE, 1);
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.SECOND, 0);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        // next trigger is in one second because second is wildcard
        calendar.add(Calendar.SECOND, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testSpecificDayOfMonthSecond() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("55 * * 3 * *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 2);
        calendar.set(Calendar.SECOND, 54);
        Date date = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 55);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.add(Calendar.MINUTE, 1);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    @Test
    public void testSpecificDate() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("* * * 3 11 *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 2);
        calendar.set(Calendar.MONTH, 9);
        Date date = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MONTH, 10); // 10=November
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());

        calendar.add(Calendar.SECOND, 1);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonExistentSpecificDate() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 0 31 6 *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 10);
        calendar.set(Calendar.MONTH, 2);
        Date date = calendar.getTime();
        trigger.next(date.getTime());
    }

    @Test
    public void testLeapYearSpecificDate() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 0 29 2 *", timeZone);
        calendar.set(Calendar.YEAR, 2007);
        calendar.set(Calendar.DAY_OF_MONTH, 10);
        calendar.set(Calendar.MONTH, 1); // 2=February
        Date date = calendar.getTime();

        calendar.set(Calendar.YEAR, 2008);
        calendar.set(Calendar.DAY_OF_MONTH, 29);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        calendar.add(Calendar.YEAR, 4);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testWeekDaySequence() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 0 7 ? * MON-FRI", timeZone);
        // This is a Saturday
        calendar.set(2009, Calendar.SEPTEMBER, 26);
        Date date = calendar.getTime();
        // 7 am is the trigger time
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        // Add two days because we start on Saturday
        calendar.add(Calendar.DAY_OF_MONTH, 2);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        // Next day is a week day so add one
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTimeInMillis(), date.getTime());
    }

    @Test
    public void testDayOfWeekIndifferent() throws Exception {
        CronSequenceGenerator trigger1 = new CronSequenceGenerator("* * * 2 * *", timeZone);
        CronSequenceGenerator trigger2 = new CronSequenceGenerator("* * * 2 * ?", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testSecondIncrementer() throws Exception {
        CronSequenceGenerator trigger1 = new CronSequenceGenerator("57,59 * * * * *", timeZone);
        CronSequenceGenerator trigger2 = new CronSequenceGenerator("57/2 * * * * *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testSecondIncrementerWithRange() throws Exception {
        CronSequenceGenerator trigger1 = new CronSequenceGenerator("1,3,5 * * * * *", timeZone);
        CronSequenceGenerator trigger2 = new CronSequenceGenerator("1-6/2 * * * * *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testHourIncrementer() throws Exception {
        CronSequenceGenerator trigger1 = new CronSequenceGenerator("* * 4,8,12,16,20 * * *", timeZone);
        CronSequenceGenerator trigger2 = new CronSequenceGenerator("* * 4/4 * * *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testDayNames() throws Exception {
        CronSequenceGenerator trigger1 = new CronSequenceGenerator("* * * * * 0-6", timeZone);
        CronSequenceGenerator trigger2 = new CronSequenceGenerator("* * * * * TUE,WED,THU,FRI,SAT,SUN,MON", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testSundayIsZero() throws Exception {
        CronSequenceGenerator trigger1 = new CronSequenceGenerator("* * * * * 0", timeZone);
        CronSequenceGenerator trigger2 = new CronSequenceGenerator("* * * * * SUN", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testSundaySynonym() throws Exception {
        CronSequenceGenerator trigger1 = new CronSequenceGenerator("* * * * * 0", timeZone);
        CronSequenceGenerator trigger2 = new CronSequenceGenerator("* * * * * 7", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testMonthNames() throws Exception {
        CronSequenceGenerator trigger1 = new CronSequenceGenerator("* * * * 1-12 *", timeZone);
        CronSequenceGenerator trigger2 = new CronSequenceGenerator("* * * * FEB,JAN,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testMonthNamesMixedCase() throws Exception {
        CronSequenceGenerator trigger1 = new CronSequenceGenerator("* * * * 2 *", timeZone);
        CronSequenceGenerator trigger2 = new CronSequenceGenerator("* * * * Feb *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSecondInvalid() throws Exception {
        new CronSequenceGenerator("77 * * * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSecondRangeInvalid() throws Exception {
        new CronSequenceGenerator("44-77 * * * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinuteInvalid() throws Exception {
        new CronSequenceGenerator("* 77 * * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinuteRangeInvalid() throws Exception {
        new CronSequenceGenerator("* 44-77 * * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHourInvalid() throws Exception {
        new CronSequenceGenerator("* * 27 * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHourRangeInvalid() throws Exception {
        new CronSequenceGenerator("* * 23-28 * * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDayInvalid() throws Exception {
        new CronSequenceGenerator("* * * 45 * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDayRangeInvalid() throws Exception {
        new CronSequenceGenerator("* * * 28-45 * *", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMonthInvalid() throws Exception {
        new CronSequenceGenerator("0 0 0 25 13 ?", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMonthInvalidTooSmall() throws Exception {
        new CronSequenceGenerator("0 0 0 25 0 ?", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDayOfMonthInvalid() throws Exception {
        new CronSequenceGenerator("0 0 0 32 12 ?", timeZone);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMonthRangeInvalid() throws Exception {
        new CronSequenceGenerator("* * * * 11-13 *", timeZone);
    }

    @Test
    public void testWhitespace() throws Exception {
        CronSequenceGenerator trigger1 = new CronSequenceGenerator("*  *  * *  1 *", timeZone);
        CronSequenceGenerator trigger2 = new CronSequenceGenerator("* * * * 1 *", timeZone);
        assertEquals(trigger1, trigger2);
    }

    @Test
    public void testMonthSequence() throws Exception {
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 30 23 30 1/3 ?", timeZone);
        calendar.set(2010, Calendar.DECEMBER, 30);
        Date date = calendar.getTime();
        // set expected next trigger time
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.MONTH, 1);

        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTime(), date);

        // Next trigger is 3 months latter
        calendar.add(Calendar.MONTH, 3);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTime(), date);

        // Next trigger is 3 months latter
        calendar.add(Calendar.MONTH, 3);
        date = new Date(trigger.next(date.getTime()));
        assertEquals(calendar.getTime(), date);
    }

    @Test
    public void testDaylightSavingMissingHour() throws Exception {
        // This trigger has to be somewhere in between 2am and 3am
        CronSequenceGenerator trigger = new CronSequenceGenerator("0 10 2 * * *", timeZone);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        calendar.set(Calendar.MONTH, Calendar.MARCH);
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.SECOND, 54);
        Date date = calendar.getTime();
        if (timeZone.equals(TimeZone.getTimeZone("CET"))) {
            // Clocks go forward an hour so 2am doesn't exist in CET for this date
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 10);
        calendar.set(Calendar.SECOND, 0);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

    private void assertMatchesNextSecond(CronSequenceGenerator trigger, Calendar calendar) {
        Date date = calendar.getTime();
        roundup(calendar);
        assertEquals(calendar.getTimeInMillis(), trigger.next(date.getTime()));
    }

}
