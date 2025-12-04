public class Packet
{
    private int seqnum;
    private int acknum;
    private int checksum;
    private String payload;
    private int[] sack;  // Added for SACK support
    
    public Packet(Packet p)
    {
        seqnum = p.getSeqnum();
        acknum = p.getAcknum();
        checksum = p.getChecksum();
        payload = new String(p.getPayload());
        if (p.getSack() != null) {
            sack = new int[5];
            int[] pSack = p.getSack();
            for (int i = 0; i < 5; i++) {
                sack[i] = pSack[i];
            }
        }
    }
    
    public Packet(int seq, int ack, int check, String newPayload)
    {
        seqnum = seq;
        acknum = ack;
        checksum = check;
        if (newPayload == null)
        {
            payload = "";
        }        
        else if (newPayload.length() > NetworkSimulator.MAXDATASIZE)
        {
            payload = null;
        }
        else
        {
            payload = new String(newPayload);
        }
        sack = new int[5];
        for (int i = 0; i < 5; i++) {
            sack[i] = -1;
        }
    }
    
    public Packet(int seq, int ack, int check)
    {
        seqnum = seq;
        acknum = ack;
        checksum = check;
        payload = "";
        sack = new int[5];
        for (int i = 0; i < 5; i++) {
            sack[i] = -1;
        }
    }    
        

    public boolean setSeqnum(int n)
    {
        seqnum = n;
        return true;
    }
    
    public boolean setAcknum(int n)
    {
        acknum = n;
        return true;
    }
    
    public boolean setChecksum(int n)
    {
        checksum = n;
        return true;
    }
    
    public boolean setPayload(String newPayload)
    {
        if (newPayload == null)
        {
            payload = "";
            return false;
        }        
        else if (newPayload.length() > NetworkSimulator.MAXDATASIZE)
        {
            payload = "";
            return false;
        }
        else
        {
            payload = new String(newPayload);
            return true;
        }
    }
    
    public boolean setSack(int[] newSack)
    {
        if (newSack == null || newSack.length != 5)
        {
            return false;
        }
        sack = new int[5];
        for (int i = 0; i < 5; i++)
        {
            sack[i] = newSack[i];
        }
        return true;
    }
    
    public int getSeqnum()
    {
        return seqnum;
    }
    
    public int getAcknum()
    {
        return acknum;
    }
    
    public int getChecksum()
    {
        return checksum;
    }
    
    public String getPayload()
    {
        return payload;
    }
    
    public int[] getSack()
    {
        return sack;
    }
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("seqnum: ").append(seqnum);
        sb.append("  acknum: ").append(acknum);
        sb.append("  checksum: ").append(checksum);
        sb.append("  payload: ").append(payload);
        if (sack != null) {
            sb.append("  sack: [");
            for (int i = 0; i < 5; i++) {
                if (i > 0) sb.append(", ");
                sb.append(sack[i]);
            }
            sb.append("]");
        }
        return sb.toString();
    }
    
}