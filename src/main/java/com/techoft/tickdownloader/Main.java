package com.techoft.tickdownloader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;

/**
 * Created by Fuad(fuadhaque@gmail.com) on 27.03.2014.
 */
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Instant start = Instant.now();

        TickDownLoader tickDownLoader = new TickDownLoader();
        tickDownLoader.Run();

        Instant end = Instant.now();

        long elapsedMilliSecs = Duration.between(start, end).toMillis();
        double elapsedMinutes = (elapsedMilliSecs / 1000.0) / 60.0;

        logger.info(String.format("Elapsed time: %1$.2f mins (%2$d ms)", elapsedMinutes, elapsedMilliSecs));
    }


}
