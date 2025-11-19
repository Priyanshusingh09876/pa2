import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Time;

public class StudentNetworkSimulator extends NetworkSimulator {
    /*
     * Predefined Constants (static member variables):
     *
     * int MAXDATASIZE : the maximum size of the Message data and
     * Packet payload
     *
     * int A : a predefined integer that represents entity A
     * int B : a predefined integer that represents entity B
     *
     * Predefined Member Methods:
     *
     * void stopTimer(int entity):
     * Stops the timer running at "entity" [A or B]
     * void startTimer(int entity, double increment):
     * Starts a timer running at "entity" [A or B], which will expire in
     * "increment" time units, causing the interrupt handler to be
     * called. You should only call this with A.
     * void toLayer3(int callingEntity, Packet p)
     * Puts the packet "p" into the network from "callingEntity" [A or B]
     * void toLayer5(String dataSent)
     * Passes "dataSent" up to layer 5
     * double getTime()
     * Returns the current time in the simulator. Might be useful for
     * debugging.
     * int getTraceLevel()
     * Returns TraceLevel
     * void printEventList()
     * Prints the current event list to stdout. Might be useful for
     * debugging, but probably not.
     *
     *
     * Predefined Classes:
     *
     * Message: Used to encapsulate a message coming from layer 5
     * Constructor:
     * Message(String inputData):
     * creates a new Message containing "inputData"
     * Methods:
     * boolean setData(String inputData):
     * sets an existing Message's data to "inputData"
     * returns true on success, false otherwise
     * String getData():
     * returns the data contained in the message
     * Packet: Used to encapsulate a packet
     * Constructors:
     * Packet (Packet p):
     * creates a new Packet that is a copy of "p"
     * Packet (int seq, int ack, int check, String newPayload)
     * creates a new Packet with a sequence field of "seq", an
     * ack field of "ack", a checksum field of "check", and a
     * payload of "newPayload"
     * Packet (int seq, int ack, int check)
     * chreate a new Packet with a sequence field of "seq", an
     * ack field of "ack", a checksum field of "check", and
     * an empty payload
     * Methods:
     * boolean setSeqnum(int n)
     * sets the Packet's sequence field to "n"
     * returns true on success, false otherwise
     * boolean setAcknum(int n)
     * sets the Packet's ack field to "n"
     * returns true on success, false otherwise
     * boolean setChecksum(int n)
     * sets the Packet's checksum to "n"
     * returns true on success, false otherwise
     * boolean setPayload(String newPayload)
     * sets the Packet's payload to "newPayload"
     * returns true on success, false otherwise
     * int getSeqnum()
     * returns the contents of the Packet's sequence field
     * int getAcknum()
     * returns the contents of the Packet's ack field
     * int getChecksum()
     * returns the checksum of the Packet
     * int getPayload()
     * returns the Packet's payload
     *
     */

    /*
     * Please use the following variables in your routines.
     * int WindowSize : the window size
     * double RxmtInterval : the retransmission timeout
     * int LimitSeqNo : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
    private int Base;
    private int ReceiverBase;
    private int CurrentSequenceNumber;
    private int MaxBufferSize;
    private int OriginalPacketsTransmitted = 0;
    private int layer5 = 0;
    private int retransmission = 0;
    private int acks = 0;
    private int corruptedcount = 0;
    private double lost = 0;
    private double corruptedratio;
    private double averagertt;
    private Map<Integer, Double> packetrttcalc;
    private double averagecommunicationtime;
    private Map<Integer, Double> packetcommunicationcalc;
    private Packet[] ackbuffer;
    private Queue<Message> sendingBuffer;
    private Map<Integer, Packet> receiverBuffer;
    private int rttcounter = 0;
    private int totalbytes = 0;
    private int goodbytes = 0;
    private int totalgoodbytes = 0;
    private Queue<Integer> goodputBuffer;
    private Map<Integer, Double> packetdelay;
    private double packetdelaytotal =0;
    private double totalrttstart;
    private double totalrttend;

    // Add any necessary class variables here. Remember, you cannot use
    // these variables to send messages error free! They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor. Don't touch!
    public StudentNetworkSimulator(int numMessages,
            double loss,
            double corrupt,
            double avgDelay,
            int trace,
            int seed,
            int winsize,
            double delay) {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        WindowSize = winsize;
        LimitSeqNo = winsize * 2; // set appropriately; assumes SR here!
        RxmtInterval = delay;
    }

    protected boolean isCorrupted(Packet packet) {// Method used to compare checksum in received packet with the
                                                  // checksum created using data in received packet

        int checksum = packet.getChecksum();
        int verifyChecksum = packet.getSeqnum();
        verifyChecksum += packet.getAcknum();
        if (packet.getPayload() != null) {
            for (char c : packet.getPayload().toCharArray()) {
                int csum = c & 0xFF;
                verifyChecksum += csum;
            }
        }
        verifyChecksum = verifyChecksum & 0xFF;

        if (checksum != verifyChecksum) {
            return true;
        } else {
            return false;
        }
    }

    protected Packet createPacket(Message message) {// Method for creating packet from messages

        int checksum = createChecksum(CurrentSequenceNumber, -1, message.getData());
        Packet p = new Packet(CurrentSequenceNumber, -1, checksum, message.getData());
        goodbytes = (p.getPayload().getBytes(StandardCharsets.UTF_16).length);
        totalbytes = totalbytes + 12 + goodbytes;
        totalgoodbytes = totalgoodbytes+ goodbytes;
        goodputBuffer.add(CurrentSequenceNumber);
        return p;

    }

    protected int createChecksum(int seq, int ack, String data) {// Generating checksum for a incoming message or packet.

        int checksum = seq + ack;
        if (data != null) {
            for (char c : data.toCharArray()) {
                int csum = c & 0xFF;
                checksum += csum;
            }
        }
        checksum = checksum & 0xFF;
        return checksum;

    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send. It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message) {
        System.out.println("aOutput(): called");
        System.out.println("Upper Layer msg: " + message.getData());
        if (CurrentSequenceNumber == 0)
            totalrttstart = getTime();
        if (CurrentSequenceNumber < Base + WindowSize) {// If new message is in window then send to B
            Packet p = createPacket(message);
            toLayer3(A, p);
            if (p.getSeqnum() == Base) {
                stopTimer(A);
                startTimer(A, RxmtInterval);
            }
            packetrttcalc.put(p.getSeqnum(), getTime());
            packetcommunicationcalc.put(p.getSeqnum(), getTime());
            ackbuffer[CurrentSequenceNumber % LimitSeqNo] = p;
            packetdelay.put(CurrentSequenceNumber,getTime());
            CurrentSequenceNumber++;

        } else if (CurrentSequenceNumber >= Base + WindowSize) {
            if (sendingBuffer.size() <= MaxBufferSize) {// Buffer it if less than buffer size else exit.
                // buffer it
                sendingBuffer.add(message);
            } else {// End program if Maximum buffer size is exceeded
                System.out.println("Exceeded Maximum Buffed Size.....Shut Down");
                System.exit(1);
            }

        }

    }

    // This routine will be called whenever a packet sent from the B-side
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side. "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet) {
        System.out.println("aInput(): called");
        boolean corrupted = isCorrupted(packet);// Checking if received ack is corrupted
        if (corrupted == false) {

            if (packet.getAcknum() >= Base && packet.getAcknum() < CurrentSequenceNumber) {// When received packet ack
                                                                                           // is in window
                OriginalPacketsTransmitted = OriginalPacketsTransmitted + (packet.getAcknum() - Base) + 1;
                if (packetrttcalc.get(Base) != null) {// Calculating Rtt
                    double startTime = packetrttcalc.remove(Base);
                    averagertt = averagertt + (getTime() - startTime);
                    rttcounter++;
                }
                double time = getTime();
                for (int i = Base; i <= packet.getAcknum(); i++) {// Calculating Communication time
                    if (packetcommunicationcalc.containsKey(i)) {
                        double startTime = packetcommunicationcalc.remove(i);
                        averagecommunicationtime = averagecommunicationtime + (time - startTime);
                    }
                    if (packetdelay.containsKey(i)) {
                        double startTime = packetdelay.remove(i);
                        packetdelaytotal = packetdelaytotal + (time - startTime);
                    }
                }
                Base = packet.getAcknum() + 1;
                stopTimer(A);
                if (Base < CurrentSequenceNumber) {
                    startTimer(A, RxmtInterval); // Restart timer for the next oldest unacknowledged packet in window
                }
                if (Base == CurrentSequenceNumber && sendingBuffer.size() != 0) {
                    startTimer(A, RxmtInterval);

                }
                // If window was full, checking once window opens up for messages in buffer.
                // Base updated to value after ack.
                while (sendingBuffer.size() != 0 && CurrentSequenceNumber < Base + WindowSize) {// Checking if all message in windowhave already been sent, if not we check buffer and send.
                    Message m = sendingBuffer.remove();
                    Packet p = createPacket(m);
                    toLayer3(A, p);
                    packetdelay.put(p.getSeqnum(),getTime());
                    packetcommunicationcalc.put(p.getSeqnum(), getTime());
                    ackbuffer[CurrentSequenceNumber % LimitSeqNo] = p;
                    CurrentSequenceNumber++;
                }
            } else if (packet.getAcknum() == Base - 1) {// Scenario where a duplicate packet is received.
                if (ackbuffer[Base % LimitSeqNo] != null) {
                    stopTimer(A);
                    if(goodputBuffer.contains(Base)&& !receiverBuffer.containsKey(Base)){
                    goodputBuffer.remove(Base);
                    totalgoodbytes = totalgoodbytes- (ackbuffer[Base % LimitSeqNo].getPayload().getBytes(StandardCharsets.UTF_16).length);
                    }
                    toLayer3(A, ackbuffer[Base % LimitSeqNo]);
                    totalbytes = totalbytes + 12 + 40 + (ackbuffer[Base % LimitSeqNo].getPayload().getBytes(StandardCharsets.UTF_16).length);
                    retransmission++;
                    startTimer(A, RxmtInterval);
                }

            }
        } else {// Scenario when the ack received is corrupted
            System.out.println("aInput(): got corrupted ACK/NAK");
            corruptedcount++;
        }
    }

    // This routine will be called when A's timer expires (thus generating a
    // timer interrupt). You'll probably want to use this routine to control
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped.
    protected void aTimerInterrupt() {
        System.out.println("aTimerInterrupt(): called");
        toLayer3(A, ackbuffer[Base % LimitSeqNo]);
        if(goodputBuffer.contains(Base)&& !receiverBuffer.containsKey(Base)){
            goodputBuffer.remove(Base);
            totalgoodbytes = totalgoodbytes- (ackbuffer[Base % LimitSeqNo].getPayload().getBytes(StandardCharsets.UTF_16).length);
        }
        totalbytes = totalbytes + 12 + 40 + (ackbuffer[Base % LimitSeqNo].getPayload().getBytes(StandardCharsets.UTF_16).length);
        startTimer(A, RxmtInterval);
        packetrttcalc.remove(Base);
        retransmission++;
    }

    // This routine will be called once, before any of your other A-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit() {
        Base = FirstSeqNo;
        CurrentSequenceNumber = FirstSeqNo;
        ackbuffer = new Packet[LimitSeqNo];
        sendingBuffer = new LinkedList<>();
        MaxBufferSize = 50;
        packetrttcalc = new HashMap<>();
        packetcommunicationcalc = new HashMap<>();
        packetdelay = new HashMap<>();
        goodputBuffer= new LinkedList<>();

    }

    // This routine will be called whenever a packet sent from the A-side
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side. "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet) {
        System.out.println("bInput(): B getting " + packet.getPayload());
        boolean corrupted = isCorrupted(packet);
        if (corrupted == false) {
            if (packet.getSeqnum() >= ReceiverBase && packet.getSeqnum() < ReceiverBase + WindowSize) {// Check if packet in window
                if (packet.getSeqnum() == ReceiverBase) {// If packet is receiver base then send it along with all packets in buffer
                    System.out.println("bInput(): expecting pkt " + ReceiverBase + ", getting pkt " + packet.getSeqnum());
                    toLayer5(packet.getPayload());
                    layer5++;
                    ReceiverBase++;
                    while (receiverBuffer.containsKey(ReceiverBase)) {// Sending buffered messages
                        Packet p = receiverBuffer.remove(ReceiverBase);
                        toLayer5(p.getPayload());
                        layer5++;
                        ReceiverBase++;
                    }
                } else {// If packet is not the base then we buffer it and send ack.
                    receiverBuffer.put(packet.getSeqnum(), packet);
                    packetrttcalc.remove(packet.getSeqnum());
                }
            }
            Packet ackPacket = new Packet(-1, ReceiverBase - 1, -1, null);// Sending acknowledgement to sender
            ackPacket.setChecksum(createChecksum(-1, ReceiverBase - 1, null));
            totalbytes = totalbytes + 12;
            toLayer3(B, ackPacket);
            acks++;
        } else {// Scenario when the received data packet is corrupted
            
            corruptedcount++;
        }

    }

    // This routine will be called once, before any of your other B-side
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit() {
        ReceiverBase = FirstSeqNo;
        receiverBuffer = new HashMap<>();

    }

    // Use to print final statistics
    protected void Simulation_done() {
        totalrttend = getTime();
        lost = (double) (retransmission - corruptedcount)/ (double) ((OriginalPacketsTransmitted + retransmission) + acks);
        corruptedratio = (double) (corruptedcount)/ (double) ((OriginalPacketsTransmitted + retransmission) + acks - (retransmission - (corruptedcount)));
        // TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO
        // NOT CHANGE THE FORMAT OF PRINTED OUTPUT
        System.out.println("\n\n===============STATISTICS=======================");
        System.out.println("Number of original packets transmitted by A:" + OriginalPacketsTransmitted);
        System.out.println("Number of retransmissions by A:" + retransmission);
        System.out.println("Number of data packets delivered to layer 5 at B:" + layer5);
        System.out.println("Number of ACK packets sent by B:" + acks);
        System.out.println("Number of corrupted packets:" + (corruptedcount));
        System.out.println("Ratio of lost packets:" + lost);
        System.out.println("Ratio of corrupted packets:" + corruptedratio);
        System.out.println("Average RTT:" + averagertt / (double) rttcounter);
        System.out.println("Average communication time:" + averagecommunicationtime / (double) OriginalPacketsTransmitted);
        System.out.println("==================================================");

        // PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
        System.out.println("\nEXTRA:");
        System.out.println("Total Rtt is:" + (totalrttend - totalrttstart));
        System.out.println("Throughput is:" + (totalbytes) / (averagecommunicationtime / 1000)+ "bytes/second");
        System.out.println("Goodput is:" + (totalgoodbytes) / (averagecommunicationtime / 1000)+ "bytes/second");
        System.out.println("Average packet delay is:"+(packetdelaytotal/(double)OriginalPacketsTransmitted));
        // EXAMPLE GIVEN BELOW
        // System.out.println("Example statistic you want to check e.g. number of ACK
        // packets received by A :" + "<YourVariableHere>");
    }

}
