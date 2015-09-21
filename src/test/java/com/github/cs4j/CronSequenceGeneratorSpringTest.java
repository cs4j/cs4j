package com.github.cs4j;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;


/**
 * Original CronSequenceGenerator tests from Spring package.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("deprecation")
public class CronSequenceGeneratorSpringTest extends Assert {

    @Test
    public void testAt50Seconds() {
        assertEquals(new Date(2012, 6, 2, 1, 0).getTime(),
                new CronSequenceGenerator("*/15 * 1-4 * * *").next(new Date(2012, 6, 1, 9, 53, 50).getTime()));
    }

    @Test
    public void testAt0Seconds() {
        assertEquals(new Date(2012, 6, 2, 1, 0).getTime(),
                new CronSequenceGenerator("*/15 * 1-4 * * *").next(new Date(2012, 6, 1, 9, 53).getTime()));
    }

    @Test
    public void testAt0Minutes() {
        assertEquals(new Date(2012, 6, 2, 1, 0).getTime(),
                new CronSequenceGenerator("0 */2 1-4 * * *").next(new Date(2012, 6, 1, 9, 0).getTime()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWith0Increment() {
        new CronSequenceGenerator("*/0 * * * * *").next(new Date(2012, 6, 1, 9, 0).getTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithNegativeIncrement() {
        new CronSequenceGenerator("*/-1 * * * * *").next(new Date(2012, 6, 1, 9, 0).getTime());
    }

}
