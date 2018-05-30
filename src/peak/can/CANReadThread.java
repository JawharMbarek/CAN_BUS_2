package peak.can;

import peak.can.basic.*;

import java.util.HashMap;

/**
 * The CANReadThread class extends Thread class and is used to process readed CAN Messages.
 * In addition, the class provides different read mode that are "By Timer" or "By Event".
 * It is possible to read CAN Messages with its Time Stamp.
 *
 * @author Jonathan Urban/Uwe Wilhelm/Fabrice Vergnaud
 * @version 1.1
 * @LastChange $Date: 2016-05-13 14:54:19 +0200 (ven. 13 mai 2016) $
 * @Copyright (C) 1999-2014  PEAK-System Technik GmbH, Darmstadt
 * more Info at http://www.peak-system.com
 */
public class CANReadThread extends Thread implements IRcvEventProcessor {
    // PCANBasic instance used to call read functions
    private PCANBasic pcanBasic;
    // Collection which stores all connected channels
    private ChannelItem item = null;
    // Collection to store readed CAN Messages
    private HashMap<Integer, TableDataRow> dataRowCollection;
    // Used to read CAN Messages with its Time stamp
    private Boolean readTimeStamp = false;
    private int messageID = 0;

    /**
     * @return states if timestamp is used when reading CAN messages
     */
    public Boolean getReadTimeStamp() {
        return readTimeStamp;
    }

    /**
     * @param useReadEx states if timestamp must be used when reading CAN messages
     */
    public void setReadTimeStamp(Boolean useReadEx) {
        this.readTimeStamp = useReadEx;
    }

    /**
     * @param pcanbasic         PCANBasic instance used to call read functions
     * @param item              Reference to the connected channels
     * @param dataRowCollection Reference to the Collection which store readed CAN Messages
     */
    public CANReadThread(PCANBasic pcanbasic, ChannelItem item, HashMap<Integer, TableDataRow> dataRowCollection) {
        this.pcanBasic = pcanbasic;
        this.dataRowCollection = dataRowCollection;
        this.item = item;
    }

    /**
     * Starts thread process
     */
    public void run() {
        while (true) {
            synchronized (item) {
                if ((item != null) && (item.getWorking())) {
                    callAPIFunctionRead(item.getHandle());
                }
            }
            // Sleep Time
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Calls the PCANBasic Read Function according the readTimeStamp parameter
     *
     * @param handle The handle of a PCAN Channel
     */
    public void callAPIFunctionRead(TPCANHandle handle) {
        //Local variables
        TPCANMsg canMessage = null;
        TPCANTimestamp rcvTime = null;
        TableDataRow dataRow = null;
        TPCANStatus ret;

        try {
            do {
                // Create new CAN Message
                canMessage = new TPCANMsg();
                // Create new TimeStamp object
                rcvTime = new TPCANTimestamp();


                //If TimeStamp is needed
                if (readTimeStamp)
                    // We execute the "Read" function of the PCANBasic
                    ret = pcanBasic.Read(handle, canMessage, rcvTime);
                    //If TimeStamp is not needed
                else
                    // We execute the "Read" function of the PCANBasic
                    ret = pcanBasic.Read(handle, canMessage, null);

                //Process result
                if (ret == TPCANStatus.PCAN_ERROR_OK) {

                    //Gets CAN Message ID
                    //messageID = canMessage.getID();
                    //System.out.println(messageID);

                    //Critical Area: dataRowCollection is used in multiple threads
//                    synchronized (MainJFrame.token) {
//                        // Searchs dataRowCollection contains CAN Message ID
//                        if (dataRowCollection.containsKey(messageID))
//                            // Update it
//                            dataRow = (TableDataRow) dataRowCollection.get(messageID);
//                        else
//                            // Create a new TableDataRow object
//                            dataRow = new TableDataRow();
//
//                        // Sets Message content
//                        dataRow.setMessage(canMessage);
//
//                        // Sets readTimeStamp if need be
//                        if (readTimeStamp)
//                            dataRow.setRcvTime(rcvTime);
//                        else
//                            dataRow.setRcvTime((TPCANTimestamp)null);
//
//                        // Sets counter
//                        dataRow.setCounter(dataRow.getCounter() + 1);
//
//                        //Put Message In the dataRowCollection
//                        dataRowCollection.put(messageID, dataRow);
                    dataRow = new TableDataRow();
                    dataRow.setMessage(canMessage);

                    if (readTimeStamp)
                        dataRow.setRcvTime(rcvTime);
                    else
                        dataRow.setRcvTime((TPCANTimestamp) null);

                    dataRow.setCounter(1);
                    dataRowCollection.put(messageID, dataRow);
                    System.out.println(messageID + "\t key = " + dataRow.getMsgId() + " value = " + dataRow.getMsgData());
                    messageID++;
//                    }
                } else {
                    dataRowCollection.clear();
                }
            } while (ret != TPCANStatus.PCAN_ERROR_QRCVEMPTY || ret == TPCANStatus.PCAN_ERROR_OK);
            // Free local variables
            canMessage = null;
            rcvTime = null;
        } catch (Exception e) {
            System.out.println("CANReadThread Exception:" + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    // This function is called by the JNI library when a CAN Receive-Event is detected
    public void processRcvEvent(TPCANHandle channel) {
        if (item.getHandle() == channel) {
            // Process a PCANBasic read call
            callAPIFunctionRead(channel);
            return;
        }
    }
}
