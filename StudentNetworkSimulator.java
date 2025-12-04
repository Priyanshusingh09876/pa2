import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class StudentNetworkSimulator extends NetworkSimulator {
    /*
     * Predefined Constants (static member variables):
     *
     * int MAXDATASIZE : the maximum size of the Message data and
     * Packet payload
     *
     * int A : a predefined integer that represents entity A
     * int B : a predefined integer that represents entity B
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
    
    // Protocol mode: 0 = Stop-and-Wait, 1 = SR, 2 = GBN with SACK
    private static final int STOP_AND_WAIT = 0;
    private static final int SELECTIVE_REPEAT = 1;
    private static final int GBN_WITH_SACK = 2;
    private int protocolMode = SELECTIVE_REPEAT; // Default to SR
    
    // Special window sizes to trigger different protocols
    private static final int SAW_WINDOW = 1;
    private static final int SR_WINDOW = 8;
    private static final int GBN_WINDOW = 16;
    
    // Common variables
    private int Base;
    private int NextSeqNum;
    private int ExpectedSeqNum;
    private Map<Integer, Packet> senderBuffer;
    private Map<Integer, Packet> receiverBuffer;
    private Queue<Packet> waitingQueue;
    private Map<Integer, Double> rttStartTimes;
    private Map<Integer, Double> commStartTimes;
    private Set<Integer> ackedPackets;
    
    // Statistics
    private int originalPacketsTransmitted = 0;
    private int retransmissions = 0;
    private int packetsToLayer5 = 0;
    private int acksSent = 0;
    private int corruptedPackets = 0;
    private double totalRTT = 0;
    private int rttCount = 0;
    private double totalCommTime = 0;
    private int commTimeCount = 0;
    private int totalDataBytes = 0;
    private int totalGoodputBytes = 0;
    
    // For GBN with SACK
    private Set<Integer> sackedPackets;
    private LinkedList<Integer> recentReceived;
    
    // Constructor
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
        RxmtInterval = delay;
        
        // Automatically set protocol mode based on window size
        if (winsize == 1) {
            protocolMode = STOP_AND_WAIT;
            LimitSeqNo = 2;
            System.out.println("Protocol: Stop-and-Wait (Window Size = 1)");
        } else if (winsize == 16) {
            protocolMode = GBN_WITH_SACK;
            LimitSeqNo = winsize * 2;
            System.out.println("Protocol: Go-Back-N with SACK (Window Size = 16)");
        } else {
            protocolMode = SELECTIVE_REPEAT;
            LimitSeqNo = winsize * 2;
            System.out.println("Protocol: Selective Repeat (Window Size = " + winsize + ")");
        }
    }

    // Calculate checksum
    protected int calculateChecksum(int seqnum, int acknum, String payload) {
        int checksum = seqnum + acknum;
        if (payload != null) {
            for (char c : payload.toCharArray()) {
                checksum += (c & 0xFF);
            }
        }
        return checksum & 0xFFFF;
    }

    // Verify checksum
    protected boolean isCorrupted(Packet packet) {
        if (packet == null) return true;
        int calculated = calculateChecksum(packet.getSeqnum(), 
                                          packet.getAcknum(), 
                                          packet.getPayload());
        return calculated != packet.getChecksum();
    }

    // Create data packet
    protected Packet makeDataPacket(int seqnum, String data) {
        int checksum = calculateChecksum(seqnum, -1, data);
        return new Packet(seqnum, -1, checksum, data);
    }

    // Create ACK packet (with optional SACK for GBN mode)
    protected Packet makeAckPacket(int acknum) {
        int checksum = calculateChecksum(-1, acknum, null);
        Packet ack = new Packet(-1, acknum, checksum, null);
        
        // Add SACK info for GBN mode
        if (protocolMode == GBN_WITH_SACK && recentReceived != null && !recentReceived.isEmpty()) {
            int[] sack = new int[5];
            Arrays.fill(sack, -1);
            int idx = 0;
            for (Integer seq : recentReceived) {
                if (idx < 5 && seq > acknum) {
                    sack[idx++] = seq;
                }
            }
            // Note: You need to modify Packet.java to add setSack method
            // For now, we'll encode SACK in payload
            StringBuilder sackStr = new StringBuilder();
            for (int i = 0; i < 5 && sack[i] != -1; i++) {
                if (i > 0) sackStr.append(",");
                sackStr.append(sack[i]);
            }
            if (sackStr.length() > 0) {
                ack.setPayload("SACK:" + sackStr.toString());
                checksum = calculateChecksum(-1, acknum, ack.getPayload());
                ack.setChecksum(checksum);
            }
        }
        
        return ack;
    }

    // A_output - called when layer 5 has data to send
    protected void aOutput(Message message) {
        if (traceLevel >= 2) {
            System.out.println("A_output: got message from layer 5: " + message.getData());
        }
        
        if (protocolMode == STOP_AND_WAIT) {
            aOutputStopAndWait(message);
        } else if (protocolMode == SELECTIVE_REPEAT) {
            aOutputSelectiveRepeat(message);
        } else if (protocolMode == GBN_WITH_SACK) {
            aOutputGBN(message);
        }
    }
    
    // Stop-and-Wait A_output
    private void aOutputStopAndWait(Message message) {
        if (NextSeqNum == Base) {
            // Can send immediately
            Packet packet = makeDataPacket(NextSeqNum, message.getData());
            senderBuffer.put(NextSeqNum, packet);
            toLayer3(A, packet);
            startTimer(A, RxmtInterval);
            
            rttStartTimes.put(NextSeqNum, getTime());
            commStartTimes.put(NextSeqNum, getTime());
            originalPacketsTransmitted++;
            totalDataBytes += 12 + message.getData().length();
            
            NextSeqNum = (NextSeqNum + 1) % LimitSeqNo;
            
            if (traceLevel >= 2) {
                System.out.println("SAW A_output: sent packet " + packet.getSeqnum());
            }
        } else {
            // Must buffer
            waitingQueue.offer(makeDataPacket(NextSeqNum, message.getData()));
            NextSeqNum = (NextSeqNum + 1) % LimitSeqNo;
            if (traceLevel >= 2) {
                System.out.println("SAW A_output: buffered message");
            }
        }
    }
    
    // Selective Repeat A_output
    private void aOutputSelectiveRepeat(Message message) {
        int seqnum = NextSeqNum;
        
        // Check if within window
        if (isInSenderWindow(seqnum)) {
            Packet packet = makeDataPacket(seqnum, message.getData());
            senderBuffer.put(seqnum, packet);
            toLayer3(A, packet);
            
            if (seqnum == Base) {
                stopTimer(A);
                startTimer(A, RxmtInterval);
            }
            
            rttStartTimes.put(seqnum, getTime());
            commStartTimes.put(seqnum, getTime());
            originalPacketsTransmitted++;
            totalDataBytes += 12 + message.getData().length();
            
            NextSeqNum = (NextSeqNum + 1) % LimitSeqNo;
            
            if (traceLevel >= 2) {
                System.out.println("SR A_output: sent packet " + seqnum);
            }
        } else {
            // Buffer for later
            if (waitingQueue.size() < 50) {
                waitingQueue.offer(makeDataPacket(NextSeqNum, message.getData()));
                NextSeqNum = (NextSeqNum + 1) % LimitSeqNo;
                if (traceLevel >= 2) {
                    System.out.println("SR A_output: buffered message, queue size: " + waitingQueue.size());
                }
            } else {
                System.err.println("SR A_output: Buffer full, dropping message");
                System.exit(1);
            }
        }
    }
    
    // GBN with SACK A_output
    private void aOutputGBN(Message message) {
        int seqnum = NextSeqNum;
        
        if (isInSenderWindow(seqnum)) {
            Packet packet = makeDataPacket(seqnum, message.getData());
            senderBuffer.put(seqnum, packet);
            toLayer3(A, packet);
            
            if (Base == NextSeqNum) {
                startTimer(A, RxmtInterval);
            }
            
            rttStartTimes.put(seqnum, getTime());
            commStartTimes.put(seqnum, getTime());
            originalPacketsTransmitted++;
            totalDataBytes += 12 + message.getData().length();
            
            NextSeqNum = (NextSeqNum + 1) % LimitSeqNo;
            
            if (traceLevel >= 2) {
                System.out.println("GBN A_output: sent packet " + seqnum);
            }
        } else {
            if (waitingQueue.size() < 50) {
                waitingQueue.offer(makeDataPacket(NextSeqNum, message.getData()));
                NextSeqNum = (NextSeqNum + 1) % LimitSeqNo;
                if (traceLevel >= 2) {
                    System.out.println("GBN A_output: buffered message");
                }
            } else {
                System.err.println("GBN A_output: Buffer full");
                System.exit(1);
            }
        }
    }

    // A_input - called when ACK arrives at A
    protected void aInput(Packet packet) {
        if (traceLevel >= 2) {
            System.out.println("A_input: got packet " + packet);
        }
        
        if (isCorrupted(packet)) {
            corruptedPackets++;
            if (traceLevel >= 1) {
                System.out.println("A_input: packet corrupted");
            }
            return;
        }
        
        if (protocolMode == STOP_AND_WAIT) {
            aInputStopAndWait(packet);
        } else if (protocolMode == SELECTIVE_REPEAT) {
            aInputSelectiveRepeat(packet);
        } else if (protocolMode == GBN_WITH_SACK) {
            aInputGBN(packet);
        }
    }
    
    // Stop-and-Wait A_input
    private void aInputStopAndWait(Packet packet) {
        int acknum = packet.getAcknum();
        
        if (acknum == Base) {
            // Correct ACK
            stopTimer(A);
            
            // Update RTT
            if (rttStartTimes.containsKey(acknum)) {
                double rtt = getTime() - rttStartTimes.remove(acknum);
                totalRTT += rtt;
                rttCount++;
            }
            
            // Update comm time
            if (commStartTimes.containsKey(acknum)) {
                double commTime = getTime() - commStartTimes.remove(acknum);
                totalCommTime += commTime;
                commTimeCount++;
            }
            
            Base = (Base + 1) % LimitSeqNo;
            
            // Send next buffered packet if any
            if (!waitingQueue.isEmpty() && Base == (NextSeqNum - waitingQueue.size() + LimitSeqNo) % LimitSeqNo) {
                Packet next = waitingQueue.poll();
                senderBuffer.put(next.getSeqnum(), next);
                toLayer3(A, next);
                startTimer(A, RxmtInterval);
                
                rttStartTimes.put(next.getSeqnum(), getTime());
                commStartTimes.put(next.getSeqnum(), getTime());
                originalPacketsTransmitted++;
                
                if (traceLevel >= 2) {
                    System.out.println("SAW A_input: sent buffered packet " + next.getSeqnum());
                }
            }
        } else {
            // Duplicate or wrong ACK - ignore in Stop-and-Wait
            if (traceLevel >= 2) {
                System.out.println("SAW A_input: ignoring ACK for " + acknum);
            }
        }
    }
    
    // Selective Repeat A_input
    private void aInputSelectiveRepeat(Packet packet) {
        int acknum = packet.getAcknum();
        
        // Cumulative ACK
        if (isInSenderWindow(acknum) || acknum == (Base - 1 + LimitSeqNo) % LimitSeqNo) {
            if (acknum == (Base - 1 + LimitSeqNo) % LimitSeqNo) {
                // Duplicate ACK - retransmit base
                if (senderBuffer.containsKey(Base)) {
                    toLayer3(A, senderBuffer.get(Base));
                    retransmissions++;
                    totalDataBytes += 12 + senderBuffer.get(Base).getPayload().length();
                    stopTimer(A);
                    startTimer(A, RxmtInterval);
                    
                    if (traceLevel >= 1) {
                        System.out.println("SR A_input: duplicate ACK, retransmitting " + Base);
                    }
                }
            } else {
                // New cumulative ACK
                int oldBase = Base;
                
                // Mark all packets up to acknum as ACKed
                while (Base != (acknum + 1) % LimitSeqNo) {
                    if (rttStartTimes.containsKey(Base)) {
                        double rtt = getTime() - rttStartTimes.remove(Base);
                        totalRTT += rtt;
                        rttCount++;
                    }
                    if (commStartTimes.containsKey(Base)) {
                        double commTime = getTime() - commStartTimes.remove(Base);
                        totalCommTime += commTime;
                        commTimeCount++;
                    }
                    senderBuffer.remove(Base);
                    ackedPackets.add(Base);
                    Base = (Base + 1) % LimitSeqNo;
                }
                
                stopTimer(A);
                if (!senderBuffer.isEmpty() && senderBuffer.containsKey(Base)) {
                    startTimer(A, RxmtInterval);
                }
                
                // Send buffered packets
                while (!waitingQueue.isEmpty() && isInSenderWindow(NextSeqNum - waitingQueue.size())) {
                    Packet pkt = waitingQueue.poll();
                    int seq = pkt.getSeqnum();
                    senderBuffer.put(seq, pkt);
                    toLayer3(A, pkt);
                    
                    rttStartTimes.put(seq, getTime());
                    commStartTimes.put(seq, getTime());
                    originalPacketsTransmitted++;
                    totalDataBytes += 12 + pkt.getPayload().length();
                    
                    if (traceLevel >= 2) {
                        System.out.println("SR A_input: sent buffered packet " + seq);
                    }
                }
                
                if (traceLevel >= 2) {
                    System.out.println("SR A_input: cumulative ACK moved window from " + oldBase + " to " + Base);
                }
            }
        }
    }
    
    // GBN with SACK A_input
    private void aInputGBN(Packet packet) {
        int acknum = packet.getAcknum();
        
        // Process SACK info if present
        if (packet.getPayload() != null && packet.getPayload().startsWith("SACK:")) {
            String sackInfo = packet.getPayload().substring(5);
            if (!sackInfo.isEmpty()) {
                String[] sacks = sackInfo.split(",");
                for (String s : sacks) {
                    try {
                        int sackNum = Integer.parseInt(s);
                        sackedPackets.add(sackNum);
                        if (traceLevel >= 2) {
                            System.out.println("GBN A_input: SACK for " + sackNum);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid SACK
                    }
                }
            }
        }
        
        if (acknum >= Base || acknum == (Base - 1 + LimitSeqNo) % LimitSeqNo) {
            if (acknum == (Base - 1 + LimitSeqNo) % LimitSeqNo) {
                // Duplicate ACK - retransmit all unSACKed packets
                stopTimer(A);
                boolean anyRetransmitted = false;
                
                for (int seq = Base; seq != NextSeqNum; seq = (seq + 1) % LimitSeqNo) {
                    if (!sackedPackets.contains(seq) && senderBuffer.containsKey(seq)) {
                        toLayer3(A, senderBuffer.get(seq));
                        retransmissions++;
                        totalDataBytes += 12 + senderBuffer.get(seq).getPayload().length();
                        anyRetransmitted = true;
                        
                        if (traceLevel >= 1) {
                            System.out.println("GBN A_input: retransmitting unSACKed packet " + seq);
                        }
                    }
                }
                
                if (anyRetransmitted) {
                    startTimer(A, RxmtInterval);
                }
            } else {
                // Cumulative ACK
                while (Base != (acknum + 1) % LimitSeqNo) {
                    if (rttStartTimes.containsKey(Base)) {
                        double rtt = getTime() - rttStartTimes.remove(Base);
                        totalRTT += rtt;
                        rttCount++;
                    }
                    if (commStartTimes.containsKey(Base)) {
                        double commTime = getTime() - commStartTimes.remove(Base);
                        totalCommTime += commTime;
                        commTimeCount++;
                    }
                    senderBuffer.remove(Base);
                    sackedPackets.remove(Base);
                    Base = (Base + 1) % LimitSeqNo;
                }
                
                stopTimer(A);
                if (Base != NextSeqNum) {
                    startTimer(A, RxmtInterval);
                }
                
                // Send buffered packets
                while (!waitingQueue.isEmpty() && isInSenderWindow(NextSeqNum - waitingQueue.size())) {
                    Packet pkt = waitingQueue.poll();
                    senderBuffer.put(pkt.getSeqnum(), pkt);
                    toLayer3(A, pkt);
                    
                    rttStartTimes.put(pkt.getSeqnum(), getTime());
                    commStartTimes.put(pkt.getSeqnum(), getTime());
                    originalPacketsTransmitted++;
                    totalDataBytes += 12 + pkt.getPayload().length();
                }
            }
        }
    }

    // A_timerinterrupt - called when A's timer expires
    protected void aTimerInterrupt() {
        if (traceLevel >= 2) {
            System.out.println("A_timerinterrupt: timer expired");
        }
        
        if (protocolMode == STOP_AND_WAIT) {
            // Retransmit single packet
            if (senderBuffer.containsKey(Base)) {
                toLayer3(A, senderBuffer.get(Base));
                startTimer(A, RxmtInterval);
                retransmissions++;
                totalDataBytes += 12 + senderBuffer.get(Base).getPayload().length();
                
                // Remove from RTT calculation
                rttStartTimes.remove(Base);
                
                if (traceLevel >= 1) {
                    System.out.println("SAW timeout: retransmitting packet " + Base);
                }
            }
        } else if (protocolMode == SELECTIVE_REPEAT) {
            // Retransmit only base packet
            if (senderBuffer.containsKey(Base)) {
                toLayer3(A, senderBuffer.get(Base));
                startTimer(A, RxmtInterval);
                retransmissions++;
                totalDataBytes += 12 + senderBuffer.get(Base).getPayload().length();
                
                // Remove from RTT calculation
                rttStartTimes.remove(Base);
                
                if (traceLevel >= 1) {
                    System.out.println("SR timeout: retransmitting packet " + Base);
                }
            }
        } else if (protocolMode == GBN_WITH_SACK) {
            // Retransmit all unACKed, unSACKed packets
            boolean anyRetransmitted = false;
            for (int seq = Base; seq != NextSeqNum; seq = (seq + 1) % LimitSeqNo) {
                if (!sackedPackets.contains(seq) && senderBuffer.containsKey(seq)) {
                    toLayer3(A, senderBuffer.get(seq));
                    retransmissions++;
                    totalDataBytes += 12 + senderBuffer.get(seq).getPayload().length();
                    anyRetransmitted = true;
                    
                    // Remove from RTT calculation
                    rttStartTimes.remove(seq);
                    
                    if (traceLevel >= 1) {
                        System.out.println("GBN timeout: retransmitting packet " + seq);
                    }
                }
            }
            
            if (anyRetransmitted) {
                startTimer(A, RxmtInterval);
            }
        }
    }

    // A_init - initialize A side
    protected void aInit() {
        Base = FirstSeqNo;
        NextSeqNum = FirstSeqNo;
        senderBuffer = new HashMap<>();
        waitingQueue = new LinkedList<>();
        rttStartTimes = new HashMap<>();
        commStartTimes = new HashMap<>();
        ackedPackets = new HashSet<>();
        
        if (protocolMode == GBN_WITH_SACK) {
            sackedPackets = new HashSet<>();
        }
        
        System.out.println("A_init: Protocol mode = " + 
            (protocolMode == STOP_AND_WAIT ? "Stop-and-Wait" : 
             protocolMode == SELECTIVE_REPEAT ? "Selective Repeat" : "GBN with SACK"));
        System.out.println("A_init: Window size = " + WindowSize);
    }

    // B_input - called when packet arrives at B
    protected void bInput(Packet packet) {
        if (traceLevel >= 2) {
            System.out.println("B_input: got packet " + packet);
        }
        
        if (isCorrupted(packet)) {
            corruptedPackets++;
            if (traceLevel >= 1) {
                System.out.println("B_input: packet corrupted");
            }
            
            // Send duplicate ACK for last correctly received in-order packet
            if (ExpectedSeqNum > 0) {
                Packet ack = makeAckPacket((ExpectedSeqNum - 1 + LimitSeqNo) % LimitSeqNo);
                toLayer3(B, ack);
                acksSent++;
                totalDataBytes += 12;
                
                if (traceLevel >= 2) {
                    System.out.println("B_input: sent duplicate ACK for " + ack.getAcknum());
                }
            }
            return;
        }
        
        if (protocolMode == STOP_AND_WAIT) {
            bInputStopAndWait(packet);
        } else if (protocolMode == SELECTIVE_REPEAT) {
            bInputSelectiveRepeat(packet);
        } else if (protocolMode == GBN_WITH_SACK) {
            bInputGBN(packet);
        }
    }
    
    // Stop-and-Wait B_input
    private void bInputStopAndWait(Packet packet) {
        int seqnum = packet.getSeqnum();
        
        if (seqnum == ExpectedSeqNum) {
            // In-order packet
            toLayer5(packet.getPayload());
            packetsToLayer5++;
            totalGoodputBytes += packet.getPayload().length();
            
            ExpectedSeqNum = (ExpectedSeqNum + 1) % LimitSeqNo;
            
            // Send ACK
            Packet ack = makeAckPacket(seqnum);
            toLayer3(B, ack);
            acksSent++;
            totalDataBytes += 12;
            
            if (traceLevel >= 2) {
                System.out.println("SAW B_input: delivered packet " + seqnum + ", sent ACK");
            }
        } else {
            // Out of order or duplicate - send duplicate ACK
            Packet ack = makeAckPacket((ExpectedSeqNum - 1 + LimitSeqNo) % LimitSeqNo);
            toLayer3(B, ack);
            acksSent++;
            totalDataBytes += 12;
            
            if (traceLevel >= 2) {
                System.out.println("SAW B_input: out-of-order/duplicate packet " + seqnum + 
                                 ", expected " + ExpectedSeqNum);
            }
        }
    }
    
    // Selective Repeat B_input
    private void bInputSelectiveRepeat(Packet packet) {
        int seqnum = packet.getSeqnum();
        
        if (isInReceiverWindow(seqnum)) {
            if (seqnum == ExpectedSeqNum) {
                // In-order packet
                toLayer5(packet.getPayload());
                packetsToLayer5++;
                totalGoodputBytes += packet.getPayload().length();
                ExpectedSeqNum = (ExpectedSeqNum + 1) % LimitSeqNo;
                
                // Check buffer for subsequent in-order packets
                while (receiverBuffer.containsKey(ExpectedSeqNum)) {
                    Packet buffered = receiverBuffer.remove(ExpectedSeqNum);
                    toLayer5(buffered.getPayload());
                    packetsToLayer5++;
                    totalGoodputBytes += buffered.getPayload().length();
                    ExpectedSeqNum = (ExpectedSeqNum + 1) % LimitSeqNo;
                    
                    if (traceLevel >= 2) {
                        System.out.println("SR B_input: delivered buffered packet " + 
                                         (ExpectedSeqNum - 1 + LimitSeqNo) % LimitSeqNo);
                    }
                }
                
                if (traceLevel >= 2) {
                    System.out.println("SR B_input: delivered packet(s), new expected = " + ExpectedSeqNum);
                }
            } else {
                // Out-of-order but in window - buffer it
                if (!receiverBuffer.containsKey(seqnum)) {
                    receiverBuffer.put(seqnum, packet);
                    if (traceLevel >= 2) {
                        System.out.println("SR B_input: buffered out-of-order packet " + seqnum);
                    }
                }
            }
        }
        
        // Always send cumulative ACK
        Packet ack = makeAckPacket((ExpectedSeqNum - 1 + LimitSeqNo) % LimitSeqNo);
        toLayer3(B, ack);
        acksSent++;
        totalDataBytes += 12;
        
        if (traceLevel >= 2) {
            System.out.println("SR B_input: sent cumulative ACK for " + ack.getAcknum());
        }
    }
    
    // GBN with SACK B_input
    private void bInputGBN(Packet packet) {
        int seqnum = packet.getSeqnum();
        
        // Update recent received list for SACK
        if (!recentReceived.contains(seqnum)) {
            recentReceived.add(seqnum);
            if (recentReceived.size() > 5) {
                recentReceived.removeFirst();
            }
        }
        
        if (seqnum == ExpectedSeqNum) {
            // In-order packet
            toLayer5(packet.getPayload());
            packetsToLayer5++;
            totalGoodputBytes += packet.getPayload().length();
            ExpectedSeqNum = (ExpectedSeqNum + 1) % LimitSeqNo;
            
            // Check buffer for subsequent packets
            while (receiverBuffer.containsKey(ExpectedSeqNum)) {
                Packet buffered = receiverBuffer.remove(ExpectedSeqNum);
                toLayer5(buffered.getPayload());
                packetsToLayer5++;
                totalGoodputBytes += buffered.getPayload().length();
                ExpectedSeqNum = (ExpectedSeqNum + 1) % LimitSeqNo;
            }
            
            if (traceLevel >= 2) {
                System.out.println("GBN B_input: delivered packet(s), new expected = " + ExpectedSeqNum);
            }
        } else if (isInReceiverWindow(seqnum)) {
            // Out-of-order but in window - buffer for SACK
            if (!receiverBuffer.containsKey(seqnum)) {
                receiverBuffer.put(seqnum, packet);
                if (traceLevel >= 2) {
                    System.out.println("GBN B_input: buffered packet " + seqnum + " for SACK");
                }
            }
        }
        
        // Send cumulative ACK with SACK
        Packet ack = makeAckPacket((ExpectedSeqNum - 1 + LimitSeqNo) % LimitSeqNo);
        toLayer3(B, ack);
        acksSent++;
        totalDataBytes += 12;
        if (ack.getPayload() != null) {
            totalDataBytes += ack.getPayload().length();
        }
        
        if (traceLevel >= 2) {
            System.out.println("GBN B_input: sent ACK for " + ack.getAcknum() + 
                             (ack.getPayload() != null ? " with " + ack.getPayload() : ""));
        }
    }

    // B_init - initialize B side
    protected void bInit() {
        ExpectedSeqNum = FirstSeqNo;
        receiverBuffer = new HashMap<>();
        
        if (protocolMode == GBN_WITH_SACK) {
            recentReceived = new LinkedList<>();
        }
        
        System.out.println("B_init: Expecting first packet with seqnum = " + ExpectedSeqNum);
    }

    // Helper method to check if sequence number is in sender window
    private boolean isInSenderWindow(int seqnum) {
        if (WindowSize == 1) {
            return seqnum == Base;
        }
        
        int distance = (seqnum - Base + LimitSeqNo) % LimitSeqNo;
        return distance < WindowSize;
    }
    
    // Helper method to check if sequence number is in receiver window
    private boolean isInReceiverWindow(int seqnum) {
        int distance = (seqnum - ExpectedSeqNum + LimitSeqNo) % LimitSeqNo;
        return distance < WindowSize;
    }

    // Print final statistics
    protected void Simulation_done() {
        // Calculate final statistics
        double lossRatio = 0;
        double corruptRatio = 0;
        double avgRTT = 0;
        double avgCommTime = 0;
        double throughput = 0;
        double goodput = 0;
        
        int totalPackets = originalPacketsTransmitted + retransmissions + acksSent;
        int lostPackets = retransmissions - corruptedPackets;
        
        if (totalPackets > 0) {
            lossRatio = (double) lostPackets / totalPackets;
            if (totalPackets - lostPackets > 0) {
                corruptRatio = (double) corruptedPackets / (totalPackets - lostPackets);
            }
        }
        
        if (rttCount > 0) {
            avgRTT = totalRTT / rttCount;
        }
        
        if (commTimeCount > 0) {
            avgCommTime = totalCommTime / commTimeCount;
        }
        
        double totalTime = getTime();
        if (totalTime > 0) {
            throughput = totalDataBytes / totalTime;
            goodput = totalGoodputBytes / totalTime;
        }
        
        System.out.println("\n\n===============STATISTICS=======================");
        System.out.println("Protocol: " + 
            (protocolMode == STOP_AND_WAIT ? "Stop-and-Wait" : 
             protocolMode == SELECTIVE_REPEAT ? "Selective Repeat" : "GBN with SACK"));
        System.out.println("Number of original packets transmitted by A: " + originalPacketsTransmitted);
        System.out.println("Number of retransmissions by A: " + retransmissions);
        System.out.println("Number of data packets delivered to layer 5 at B: " + packetsToLayer5);
        System.out.println("Number of ACK packets sent by B: " + acksSent);
        System.out.println("Number of corrupted packets: " + corruptedPackets);
        System.out.printf("Ratio of lost packets: %.6f\n", lossRatio);
        System.out.printf("Ratio of corrupted packets: %.6f\n", corruptRatio);
        System.out.printf("Average RTT: %.4f\n", avgRTT);
        System.out.printf("Average communication time: %.4f\n", avgCommTime);
        System.out.println("==================================================");
        
        System.out.println("\nEXTRA STATISTICS:");
        System.out.printf("Total simulation time: %.4f\n", totalTime);
        System.out.printf("Throughput: %.2f bytes/time unit\n", throughput);
        System.out.printf("Goodput: %.2f bytes/time unit\n", goodput);
        System.out.printf("Average packet delay: %.4f\n", avgCommTime);
        System.out.println("==================================================");
    }
}