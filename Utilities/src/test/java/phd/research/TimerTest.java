package phd.research;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mockStatic;

/**
 * @author Jordan Doyle
 */

public class TimerTest {

    private Timer timer;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration duration;

    @Before
    public void setUp() {
        this.timer = new Timer();
        Clock startClock = Clock.fixed(Instant.parse("2022-01-20T10:15:30Z"), ZoneId.of("UTC"));
        this.startTime = LocalDateTime.now(startClock);
        Clock endClock = Clock.fixed(Instant.parse("2022-01-20T10:15:45Z"), ZoneId.of("UTC"));
        this.endTime = LocalDateTime.now(endClock);
        this.duration = Duration.between(this.startTime, this.endTime);
    }

    @Test
    public void testConstructor() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy-HH:mm:ss");
        LocalDateTime currentTime = LocalDateTime.now();

        assertEquals("Date timer format not set correctly in constructor.", formatter.format(currentTime),
                this.timer.getFormatter().format(currentTime)
                    );
    }

    @Test
    public void testStartNoReset() {
        try (MockedStatic<LocalDateTime> mockedStatic = mockStatic(LocalDateTime.class)) {
            mockedStatic.when(LocalDateTime::now).thenReturn(this.startTime);
            assertEquals("Wrong start time returned when starting timer.", "20/01/22-10:15:30", this.timer.start());
        }
    }

    @Test
    public void testStartWithReset() {
        try (MockedStatic<LocalDateTime> mockedStatic = mockStatic(LocalDateTime.class)) {
            mockedStatic.when(LocalDateTime::now).thenReturn(this.startTime);
            this.timer.start();

            mockedStatic.when(LocalDateTime::now).thenReturn(this.endTime);
            this.timer.end();

            assertEquals("Wrong start time returned when starting timer with reset.", "20/01/22-10:15:45",
                    timer.start(true)
                        );
        }
    }

    @Test(expected = TimerException.class)
    public void testTimerStartedException() {
        this.timer.start();
        this.timer.start();
    }

    @Test(expected = TimerException.class)
    public void testTimerEndedException() {
        this.timer.start();
        this.timer.end();
        this.timer.start();
    }

    @Test
    public void testEnd() {
        try (MockedStatic<LocalDateTime> mockedStatic = mockStatic(LocalDateTime.class)) {
            mockedStatic.when(LocalDateTime::now).thenReturn(this.startTime);
            this.timer.start();

            mockedStatic.when(LocalDateTime::now).thenReturn(this.endTime);
            assertEquals("Wrong end time returned when stopping timer.", "20/01/22-10:15:45", this.timer.end());
        }
    }

    @Test(expected = TimerException.class)
    public void testNoStartBeforeEndException() {
        this.timer.end();
    }

    @Test(expected = TimerException.class)
    public void testAlreadyEndedException() {
        this.timer.start();
        this.timer.end();
        this.timer.end();
    }

    @Test
    public void testSecondsDuration() {
        try (MockedStatic<LocalDateTime> mockedStatic = mockStatic(LocalDateTime.class)) {
            mockedStatic.when(LocalDateTime::now).thenReturn(this.startTime);
            this.timer.start();

            mockedStatic.when(LocalDateTime::now).thenReturn(this.endTime);
            this.timer.end();

            try (MockedStatic<Duration> durationMockedStatic = mockStatic(Duration.class)) {
                durationMockedStatic.when(() -> Duration.between(this.startTime, this.endTime))
                        .thenReturn(this.duration);

                assertEquals("Wrong number of seconds returned from duration method.", 15L,
                        this.timer.secondsDuration()
                            );
            }
        }
    }

    @Test(expected = TimerException.class)
    public void testStartNullDurationException() {
        this.timer.secondsDuration();
    }

    @Test(expected = TimerException.class)
    public void testEndNullDurationException() {
        this.timer.start();
        this.timer.secondsDuration();
    }

    @Test
    public void testResetDateTimer() {
        this.timer.start();
        this.timer.end();
        this.timer.resetDateTimer();
        assertNull("Timer reset failed. Start time is not null.", this.timer.getStartTime());
        assertNull("Timer reset failed. End time is not null.", this.timer.getEndTime());
    }
}