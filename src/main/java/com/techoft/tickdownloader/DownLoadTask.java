package com.techoft.tickdownloader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Created by Fuad(fuadhaque@gmail.com) on 27.03.2014.
 */
public class DownLoadTask implements Runnable {
    private static final Logger logger = LogManager.getLogger(DownLoadTask.class.getName());
    private Map<String, LocalDate> symbolsRegistry = ConfigurationManager.getSymbolsRegistry();
    private DateTimeFormatter dtfm = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");
    private String symbol;

    private LocalDate yesterday = LocalDate.now().minusDays(1);
    private LocalDateTime beginDateTime;
    private LocalDateTime endDateTime;

    private Socket iqfSocket;
    private BufferedWriter tickWriter;
    private BufferedReader sin;
    private BufferedWriter sout;

    public DownLoadTask(String symbol){
        this.symbol = symbol;
    }

    public void run(){
        //set last download default value to minus 180 days
        LocalDate lastDownloadDate = LocalDate.now().minusDays(180);

        //get last successful download date
        if(symbolsRegistry.containsKey(symbol)){
            lastDownloadDate = symbolsRegistry.get(symbol);
        }

        try {
            while (lastDownloadDate.compareTo(yesterday) < 0) {
                lastDownloadDate = lastDownloadDate.plusDays(1);

                // may throw exception
                downloadTicks(lastDownloadDate);

                // this would not execute in case of an exception
                ConfigurationManager.getInstance().UpdateSymbolRegistry(symbol, lastDownloadDate);

            }
        }catch(Exception e){
            logger.error(e);
        }
    }

    private void downloadTicks(LocalDate downloadDate) throws Exception{
        beginDateTime = downloadDate.atTime(0, 0, 0);
        endDateTime = downloadDate.atTime(23, 59, 59);

        try{
            // creates a socket connection to localhost (IP address 127.0.0.1) on port 9100.
            // This is that port that IQFeed listens on for lookup requests.
            iqfSocket = new Socket(InetAddress.getByName("localhost"), ConfigurationManager.getIQFLookUpPortNumber());

            // buffer to incomming data.
            sin = new BufferedReader(new InputStreamReader(iqfSocket.getInputStream()));

            // buffer for out going commands.
            sout = new BufferedWriter(new OutputStreamWriter(iqfSocket.getOutputStream()));

            // Set the lookup port to protocol 5.0 to allow for millisecond times,
            // market center, trade conditions, etc
            sout.write("S,SET PROTOCOL,5.0\r\n");
            sout.flush();

            Path pathToFile = Paths.get("ticks", symbol,
                    String.valueOf(downloadDate.getYear()),
                    String.format("%02d", downloadDate.getMonthValue()),
                    String.format("%02d.txt", downloadDate.getDayOfMonth()));

            if (!Files.exists(pathToFile)) {
                Files.createDirectories(pathToFile.getParent());
                Files.createFile(pathToFile);
            }
            tickWriter = Files.newBufferedWriter(pathToFile);
            String iqFeedRequest = String.format("HTT,%1$s,%2$s,%3$s,,,,1,,%n", symbol, beginDateTime.format(dtfm), endDateTime.format(dtfm));
            logger.info(iqFeedRequest);

            sout.write(iqFeedRequest);
            sout.flush();

            //read reply to our first command S,SET PROTOCOL,5.0
            String sLine = sin.readLine();

            while((sLine = sin.readLine()) != null){
                if(sLine.indexOf("!ENDMSG!", 0) > -1 || sLine.equals("E,!SYNTAX_ERROR!,") || sLine.equals("E,Invalid symbol.,") || sLine.indexOf("!NO_DATA!", 0) > -1){
                    logger.info("[" + symbol + "]>>>" + sLine);
                    break;
                }else{
                    tickWriter.write(String.format("%s%n", sLine));
                }
            }
        }catch(Exception e){
            throw e;
        }finally{
            try {
                tickWriter.close();
                iqfSocket.close();
            } catch (IOException e) {
                throw e;
            }
        }
    }
}
