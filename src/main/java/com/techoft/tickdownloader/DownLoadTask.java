package com.techoft.tickdownloader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by Fuad(fuadhaque@gmail.com) on 27.03.2014.
 */
public class DownLoadTask implements Runnable {
    private static final Logger logger = LogManager.getLogger(DownLoadTask.class.getName());
    private DateTimeFormatter dtfm = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");
    private String symbol;
    private LocalDate downloadDate;
    private LocalDateTime beginDateTime;
    private LocalDateTime endDateTime;

    public DownLoadTask(String symbol, LocalDate downloadDate){
        this.symbol = symbol;
        this.downloadDate = downloadDate;
        this.beginDateTime = downloadDate.atTime(0, 0 , 0);
        this.endDateTime = downloadDate.atTime(23, 59, 59);
    }

    public void run(){
        try {
            // creates a socket connection to localhost (IP address 127.0.0.1) on port 9100.
            // This is that port that IQFeed listens on for lookup requests.
            Socket iqfSocket = new Socket(InetAddress.getByName("localhost"), ConfigurationManager.getIQFLookUpPortNumber());

            // buffer to incomming data.
            BufferedReader sin = new BufferedReader(new InputStreamReader(iqfSocket.getInputStream()));

            // buffer for out going commands.
            BufferedWriter sout = new BufferedWriter(new OutputStreamWriter(iqfSocket.getOutputStream()));

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
            BufferedWriter tickWriter = Files.newBufferedWriter(pathToFile);
            String iqFeedRequest = String.format("HTT,%1$s,%2$s,%3$s,,,,1,,%n", symbol, beginDateTime.format(dtfm), endDateTime.format(dtfm));
            logger.info(iqFeedRequest);

            sout.write(iqFeedRequest);
            sout.flush();

            //read reply to our first command S,SET PROTOCOL,5.0
            String sLine = sin.readLine();

            while((sLine = sin.readLine()) != null){
                if(sLine.indexOf("!ENDMSG!", 0) > -1 || sLine.equals("E,!SYNTAX_ERROR!,") || sLine.indexOf("!NO_DATA!", 0) > -1){
                    logger.info(sLine);
                    break;
                }else{
                    tickWriter.write(String.format("%s%n", sLine));
                }
            }

            tickWriter.flush();
            tickWriter.close();

            iqfSocket.close();

            ConfigurationManager.getInstance().UpdateSymbolRegistry(symbol, downloadDate);

        }catch(Exception e){
            logger.error(e);
        }
    }
}
