package com.techoft.tickdownloader;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Fuad(fuadhaque@gmail.com) on 28.03.2014.
 */
public class TickDownLoader {
    private int numberOfThreads;
    private String[] symbols = ConfigurationManager.getSymbols();

    //Log4j ver 2
    private static final Logger logger = LogManager.getLogger(TickDownLoader.class.getName());

    public TickDownLoader(){
        //number of threads = number of processors + 1
        numberOfThreads = Runtime.getRuntime().availableProcessors() + 1;
        launchIQFeed();
    }

    // Launch IQFeed
    private void launchIQFeed(){
        try {
            String connectionString = String.format("IQConnect.exe -product %1$s -version 0.1.0.0 -login %2$s -password %3$s",
                    ConfigurationManager.getIQFProductId(),
                    ConfigurationManager.getIQFLogin(),
                    ConfigurationManager.getIQFPassword());
            Runtime.getRuntime().exec(connectionString);

            // verify everything is ready to send commands.
            boolean bConnected = false;

            // connect to the admin port.
            Socket sockAdmin = new Socket(InetAddress.getByName("localhost"), ConfigurationManager.getIQFAdminPortNumber());
            BufferedReader bufreadAdmin = new BufferedReader(new InputStreamReader(sockAdmin.getInputStream()));
            BufferedWriter bufwriteAdmin = new BufferedWriter(new OutputStreamWriter(sockAdmin.getOutputStream()));

            String sAdminLine = "";

            // loop while we are still connected to the admin port or until we are connected
            while (((sAdminLine = bufreadAdmin.readLine()) != null) && !bConnected) {
                if (sAdminLine.indexOf(",Connected,") > -1) {
                    logger.info("IQConnect is connected to the server.");
                    bConnected = true;
                } else if (sAdminLine.indexOf(",Not Connected,") > -1) {
                    //log.info("IQConnect is Not Connected.\r\nSending connect command.");
                    bufwriteAdmin.write("S,CONNECT\r\n");
                    bufwriteAdmin.flush();
                }
            }
            // cleanup admin port connection
            sockAdmin.shutdownOutput();
            sockAdmin.shutdownInput();
            sockAdmin.close();
            bufreadAdmin.close();
            bufwriteAdmin.close();
        }catch(Exception e){
            logger.error(e);
        }
    }

    public void Run(){
        // return, if no symbols in config.xml
        if(symbols.length == 0){
            logger.info("No symbols in config.xml");
            return;
        }

        try{
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

            for(String symbol : symbols ){
                executor.execute(new DownLoadTask(symbol));
            }

            // This will make the executor accept no new threads and finish all existing threads in the queue
            executor.shutdown();

            // Wait until all threads are finished
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

            //save symbol registry
            ConfigurationManager.getInstance().SaveSymbolRegistry();
        }catch(Exception e){
            logger.error(e);
        }
    }
}
