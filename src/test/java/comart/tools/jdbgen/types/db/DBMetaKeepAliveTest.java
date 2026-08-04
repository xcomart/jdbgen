package comart.tools.jdbgen.types.db;

import comart.tools.jdbgen.types.JDBConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The keep-alive settings are only useful if a misconfigured one stays out of
 * the way and a failing one keeps trying.
 */
public class DBMetaKeepAliveTest {

    private static JDBConnection connection(boolean use, String sec, String query) {
        JDBConnection conn = new JDBConnection();
        conn.setName("sample");
        conn.setUseKeepAlive(use);
        conn.setKeepAliveSec(sec);
        conn.setKeepAliveQuery(query);
        return conn;
    }

    @Test
    public void completeSettingIsAccepted() {
        assertEquals(30, DBMeta.keepAliveSeconds(connection(true, "30", "select 1")));
    }

    @Test
    public void paddedIntervalIsAccepted() {
        assertEquals(15, DBMeta.keepAliveSeconds(connection(true, " 15 ", "select 1")));
    }

    @Test
    public void disabledSettingIsIgnored() {
        assertEquals(0, DBMeta.keepAliveSeconds(connection(false, "30", "select 1")));
    }

    @Test
    public void missingConnectionIsIgnored() {
        assertEquals(0, DBMeta.keepAliveSeconds(null));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "0", "-1", "abc", "30s", "1.5"})
    public void unusableIntervalIsIgnored(String sec) {
        assertEquals(0, DBMeta.keepAliveSeconds(connection(true, sec, "select 1")));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  \t "})
    public void unusableQueryIsIgnored(String query) {
        assertEquals(0, DBMeta.keepAliveSeconds(connection(true, "30", query)));
    }

    @Test
    public void failingTaskKeepsBeingScheduled() throws Exception {
        CountDownLatch runs = new CountDownLatch(3);
        ScheduledExecutorService exec = DBMeta.startKeepAlive(
                "sample", 20, TimeUnit.MILLISECONDS, () -> {
                    runs.countDown();
                    throw new IllegalStateException("connection is gone");
                });
        try {
            assertTrue(runs.await(10, TimeUnit.SECONDS),
                    "a throwing keep-alive must not cancel the following rounds");
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    public void schedulerRunsOnANamedDaemonThread() throws Exception {
        CountDownLatch ran = new CountDownLatch(1);
        AtomicReference<Thread> thread = new AtomicReference<>();
        ScheduledExecutorService exec = DBMeta.startKeepAlive(
                "sample", 20, TimeUnit.MILLISECONDS, () -> {
                    thread.set(Thread.currentThread());
                    ran.countDown();
                });
        try {
            assertTrue(ran.await(10, TimeUnit.SECONDS));
        } finally {
            exec.shutdownNow();
        }
        Thread t = thread.get();
        assertNotNull(t);
        assertTrue(t.isDaemon(), "keep-alive must not hold the JVM up");
        assertTrue(t.getName().contains("sample"), "unexpected thread name: " + t.getName());
    }

    @Test
    public void schedulerIsUsableWithoutAConnectionName() throws Exception {
        CountDownLatch ran = new CountDownLatch(1);
        ScheduledExecutorService exec = DBMeta.startKeepAlive(
                null, 20, TimeUnit.MILLISECONDS, ran::countDown);
        try {
            assertTrue(ran.await(10, TimeUnit.SECONDS));
        } finally {
            exec.shutdownNow();
        }
    }
}
