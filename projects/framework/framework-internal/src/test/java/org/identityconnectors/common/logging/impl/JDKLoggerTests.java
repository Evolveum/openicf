package org.identityconnectors.common.logging.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.identityconnectors.common.logging.Log.Level;
import org.junit.Test;

public class JDKLoggerTests {

    @Test
    public void testLog() {
        JDKLogger logger = new JDKLogger();
        Logger jdkLogger = logger.getJDKLogger(JDKLoggerTests.class.getName());
        assertNotNull(jdkLogger);
        // We do not want to have output in stderr
        jdkLogger.setUseParentHandlers(false);
        final List<LogRecord> records = new ArrayList<LogRecord>();
        jdkLogger.addHandler(new Handler() {
            @Override
            public void close() throws SecurityException {
            }

            @Override
            public void flush() {
            }

            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }
        });
        logger.log(JDKLoggerTests.class, "method1", Level.ERROR, "Msg", new Exception());
        assertEquals("Must log one record", 1, records.size());
        assertEquals("method1", records.get(0).getSourceMethodName());
        assertEquals("Msg", records.get(0).getMessage());
        assertNotNull(records.get(0).getThrown());
    }

    @Test
    public void testIsLoggable() {
        JDKLogger logger = new JDKLogger();
        Logger jdkLogger = logger.getJDKLogger(JDKLoggerTests.class.getName());

        LogManager.getLoggingMXBean().setLoggerLevel(JDKLoggerTests.class.getName(), java.util.logging.Level.INFO.getName());
        assertEquals(java.util.logging.Level.INFO, jdkLogger.getLevel());
        assertTrue(logger.isLoggable(JDKLoggerTests.class, Level.INFO));

        LogManager.getLoggingMXBean().setLoggerLevel(JDKLoggerTests.class.getName(), java.util.logging.Level.WARNING.getName());
        assertEquals(java.util.logging.Level.WARNING, jdkLogger.getLevel());
        assertTrue(logger.isLoggable(JDKLoggerTests.class, Level.WARN));

        LogManager.getLoggingMXBean().setLoggerLevel(JDKLoggerTests.class.getName(), java.util.logging.Level.INFO.getName());
        assertEquals(java.util.logging.Level.INFO, jdkLogger.getLevel());
        assertFalse(logger.isLoggable(JDKLoggerTests.class, Level.OK));

        LogManager.getLoggingMXBean().setLoggerLevel(JDKLoggerTests.class.getName(), java.util.logging.Level.OFF.getName());
        assertFalse(logger.isLoggable(JDKLoggerTests.class, Level.WARN));

        LogManager.getLoggingMXBean().setLoggerLevel(JDKLoggerTests.class.getName(), java.util.logging.Level.ALL.getName());
        assertTrue(logger.isLoggable(JDKLoggerTests.class, Level.OK));
    }

    @Test
    public void testCreateJDKLogger() {
        JDKLogger logger = new JDKLogger();
        Logger jdkLogger1 = logger.getJDKLogger(JDKLoggerTests.class.getName());
        assertSame(Logger.getLogger(JDKLoggerTests.class.getName()), jdkLogger1);
        Logger jdkLogger2 = logger.getJDKLogger(JDKLoggerTests.class.getName());
        assertSame(jdkLogger1, jdkLogger2);
        Logger jdkLogger3 = logger.getJDKLogger(Integer.class.getName());
        Logger jdkLogger4 = logger.getJDKLogger(Long.class.getName());
        Logger jdkLogger5 = logger.getJDKLogger(Byte.class.getName());
        Logger jdkLogger6 = logger.getJDKLogger(Byte.class.getName());
        assertNotSame(jdkLogger3, jdkLogger4);
        assertNotSame(jdkLogger3, jdkLogger5);
        assertSame(jdkLogger5, jdkLogger6);
        Logger jdkLogger7 = logger.getJDKLogger(JDKLoggerTests.class.getName());
        assertSame(jdkLogger1, jdkLogger7);
    }

    @Test
    public void testMultithreaded() {
        final JDKLogger logger = new JDKLogger();
        int size = 100;
        CyclicBarrier barier = new CyclicBarrier(size);
        List<Thread> threads = new ArrayList<Thread>(size);
        Set<String> keys = new HashSet<String>();
        for (int i = 0; i < 100; i++) {
            String key = "" + i % 10;
            keys.add(key);
            Thread thread = new Thread(new CreateLogger(barier, logger, key));
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
        assertEquals(keys, logger.getMap().keySet());
    }

    private static class CreateLogger implements Runnable {
        final CyclicBarrier barier;
        final JDKLogger logger;
        final String key;

        private CreateLogger(CyclicBarrier barier, JDKLogger logger, String key) {
            this.barier = barier;
            this.logger = logger;
            this.key = key;
        }

        public void run() {
            try {
                barier.await();
            } catch (Exception e1) {
            }
            logger.getJDKLogger(key);
        }
    }
}
