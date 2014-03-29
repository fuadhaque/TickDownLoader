package com.techoft.tickdownloader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Fuad(fuadhaque@gmail.com) on 27.03.2014.
 */
public class ConfigurationManager {
    //config file
    private static final String configFileName = "config.xml";

    // symbol registry
    private static final String symbolRegistryFileName = "symbolRegistry.csv";
    private static Map<String, LocalDate> symbolRegistry = new HashMap<String, LocalDate>();
    private Lock symbolRegistryLock = new ReentrantLock();

    //Log4J
    private static final Logger logger = LogManager.getLogger(ConfigurationManager.class.getName());

    //IQfeed connection parameters
    private static String iqfProductId;
    private static String iqfLogin;
    private static String iqfPassword;
    private static int iqfLookUpPortNumber;
    private static int iqfAdminPortNumber;

    //string array of symbols from config.xml
    private static String[] symbols;

    private static ConfigurationManager instance = new ConfigurationManager();

    private ConfigurationManager(){
        String inputLine;

        try {
            // read symbol registry file
            BufferedReader reader = new BufferedReader(new FileReader(new File(symbolRegistryFileName)));
            while ((inputLine = reader.readLine()) != null) {
                if (inputLine.isEmpty()) {
                    continue;
                }
                String[] data = inputLine.split(";");
                symbolRegistry.put(data[0], LocalDate.parse(data[1]));
            }

            reader.close();

            ////////// read config.xml using XPath ////////////////
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document configFile = builder.parse(new FileInputStream(configFileName));
            XPath xPath =  XPathFactory.newInstance().newXPath();

            //read symbols list
            symbols = (xPath.compile("/Configuration/Symbols").evaluate(configFile)).split(" ");

            // get IQFeed parameters
            iqfProductId = xPath.compile("/Configuration/IQFeedProductId").evaluate(configFile);
            iqfLogin = xPath.compile("/Configuration/IQFeedLogin").evaluate(configFile);
            iqfPassword = xPath.compile("/Configuration/IQFeedPassword").evaluate(configFile);
            iqfLookUpPortNumber = Integer.valueOf(xPath.compile("/Configuration/IQFeedPortNumber").evaluate(configFile));
            iqfAdminPortNumber = Integer.valueOf(xPath.compile("/Configuration/IQFeedAdminPortNumber").evaluate(configFile));
        }catch(Exception e){
            logger.error(e);
        }
    }

    public static ConfigurationManager getInstance(){
        return instance;
    }

    public static String[] getSymbols(){
        return symbols;
    }

    public static Map<String, LocalDate> getSymbolsRegistry(){
        return symbolRegistry;
    }

    public static String getIQFProductId(){
        return iqfProductId;
    }

    public static String getIQFLogin(){
        return iqfLogin;
    }

    public static String getIQFPassword(){
        return iqfPassword;
    }

    public static int getIQFLookUpPortNumber(){return iqfLookUpPortNumber;}

    public static int getIQFAdminPortNumber(){return iqfAdminPortNumber;}

    public void SaveSymbolRegistry(){
        try {
            //all symbols downloaded, update symbolRegistry
            BufferedWriter symbHistoryWriter = new BufferedWriter(new FileWriter(new File(symbolRegistryFileName)));
            for (Map.Entry<String, LocalDate> entry : symbolRegistry.entrySet()) {
                symbHistoryWriter.write(String.format("%1$s;%2$s%n", entry.getKey(), entry.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
            }
            symbHistoryWriter.flush();
            symbHistoryWriter.close();
        }catch(Exception e){
            logger.error(e);
        }
    }

    // update symbol registry
    public void UpdateSymbolRegistry(String symbol, LocalDate lastDownLoadDate){
        symbolRegistryLock.lock();

        if(!symbolRegistry.containsKey(symbol)) {
            symbolRegistry.put(symbol, lastDownLoadDate);
        }else{
            if(symbolRegistry.get(symbol).isBefore(lastDownLoadDate)){
                symbolRegistry.put(symbol, lastDownLoadDate);
            }
        }

        symbolRegistryLock.unlock();
    }
}
