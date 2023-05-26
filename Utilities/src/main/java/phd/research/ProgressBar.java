package phd.research;

import java.util.stream.Stream;

/**
 * @author Jordan Doyle
 */

public class ProgressBar {

    private static final int BAR_SIZE = 50;
    private static final char DONE = '#';
    private static final char TODO = '-';

    public static void printProgress(int current, int total) {
        StringBuilder builder = new StringBuilder("Progress : [");

        if (current == total) {
            String doneMessage = " DONE ";
            int doneLength = (BAR_SIZE - doneMessage.length()) / 2;
            Stream.generate(() -> DONE).limit(doneLength).forEach(builder::append);
            builder.append(doneMessage);
            Stream.generate(() -> DONE).limit(doneLength).forEach(builder::append);
        } else {
            float percentageDone = ((float) current / total) * 100;
            int doneLength = (int) (percentageDone * BAR_SIZE) / 100;
            Stream.generate(() -> DONE).limit(doneLength).forEach(builder::append);
            Stream.generate(() -> TODO).limit(BAR_SIZE - doneLength).forEach(builder::append);
        }

        builder.append("] ").append(current).append(" / ").append(total);
        System.out.print(builder + "\r");
    }
}
