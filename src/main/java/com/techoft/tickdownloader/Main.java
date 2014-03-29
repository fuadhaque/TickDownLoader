package com.techoft.tickdownloader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Fuad(fuadhaque@gmail.com) on 27.03.2014.
 */
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class.getName());

    public static void main(String[] args) {
        TickDownLoader tickDownLoader = new TickDownLoader();
        tickDownLoader.Run();
    }


}
