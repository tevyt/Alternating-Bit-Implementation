import java.util.LinkedList;
import java.util.Queue;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(int entity, String dataSent)
     *       Passes "dataSent" up to layer 5 from "entity" [A or B]
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          create a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    SenderState senderState; //Class containing Senders information.
    ReceiverState receiverState; //Similarly contains receivers state.

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!

    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   long seed)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to ensure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
      System.out.println("Received Message from Layer5 Created packet: " + senderState.addPacketToQueue(message));
      if(!senderState.hasUnacknowledgedPackets()) {
          Packet p = senderState.preparePacket();
          senderState.setNextExpectedAcknowledgementNumber();
          System.out.println("Sent Packet: " + p);
          toLayer3(0, p);
          startTimer(0, 32);
      }
    }

    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet){
        Packet nextPacket;
      stopTimer(0);
        boolean expectedACK = expectedPacket(senderState.getAwaitedAcknowledgementNumber() ,packet.getAcknum() );
        boolean corrupted = isCorrupted(packet , packet.getChecksum());
        if(!expectedACK  || corrupted){
         Packet retransmissionPacket = senderState.packet;
            if(corrupted)
                System.out.println("Corrupted ACK , retransmitting: " + retransmissionPacket);
            else
                System.out.println("Received NACK retransmitting packet: " + retransmissionPacket);
            toLayer3(0, retransmissionPacket);
            startTimer(0, 32);
        }
       else if((nextPacket = senderState.nextPacket()) != null){
            System.out.println("Received ACK: " + packet);
            System.out.println("Sent Packet: " + nextPacket);
           toLayer3(0, nextPacket);
            senderState.setNextExpectedAcknowledgementNumber();
           startTimer(0 , 32);
       }else{
             System.out.println("Received ACK: " + packet);
             senderState.setNoUnacknowledgedPackets();
       }
    }

    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
      Packet retransmissionPacket = senderState.packet;
	  System.out.println("Timeout retransmitting packet: " + retransmissionPacket);
      toLayer3(0, retransmissionPacket);
      startTimer(0, 32);
    }


    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        senderState = new SenderState(); //Initialize Sender state values.
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        System.out.println("Received packet: " + packet);
        if(!isCorrupted(packet, packet.getChecksum())){
            Packet ack = createACK(packet.getSeqnum() , "ACK");
            System.out.println("Sent ACK: " + ack);
            toLayer3(1, ack);
            if(packet.getSeqnum() == receiverState.expectedSequenceNumber)
                receiverState.incrementExpectedSequenceNumber();
        }else{
            Packet nack = createACK(receiverState.expectedSequenceNumber , "NACK");
            System.out.println("Corrupted Packet");
            System.out.println("Sent NACK: " + nack);
            toLayer3(1 , nack);
        }
    }

    private Packet createACK(int ackNumber , String message) {
        int sequenceNumber = ackNumber;
        int checkSum = computeCheckSum(sequenceNumber, ackNumber, message);
        return new Packet(sequenceNumber, ackNumber, checkSum, message);
    }

    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        receiverState = new ReceiverState();
    }

	public static void main(String []args) {

	  StudentNetworkSimulator S = new StudentNetworkSimulator(10, 0.1, 0.1, 1000, 2, 123);
	  S.runSimulator();

	}

    //Inner class to store state that the sender must maintain.
    static class SenderState{
        private final static int NO_UNACKNOWLEDGED_PACKETS = -1;
        private int sequenceNumber;
        private int acknowledgementNumber; //indicates the acknowledgement number that the sender expects.
        private Queue<Packet> packets; //Store unsent packets from Layer 5.
        private Packet packet; //A copy of the last unacknowledged packet for retransmission.

        public SenderState(){
            this.sequenceNumber = 0;
            this.acknowledgementNumber = NO_UNACKNOWLEDGED_PACKETS;//-1 indicates there are no
            this.packets = new LinkedList<>();
        }


        //Retrieve the ACK number that the sender awaits.
        public int getAwaitedAcknowledgementNumber() {
            return acknowledgementNumber;
        }

        //Set AcknowledgementNumber to the sequence number of a packet.
        public void setNextExpectedAcknowledgementNumber() {
            this.acknowledgementNumber = (sequenceNumber-1) % 2;//The previously delivered sequence number
        }

        //Return a copy of the last unacknowledged packet.
        public Packet getLastUnacknowledgedPacket(){
            return packet;
        }

        public void setPacket(Packet packet) {
            this.packet = new Packet(packet);
        }

        public Packet addPacketToQueue(Message message){
            int sequenceNumber = this.sequenceNumber++ % 2;//If this is an even numbered packet its sequence number should be 0 otherwise it should be one.
            int checkSum = computeCheckSum(sequenceNumber , sequenceNumber , message.getData());
            Packet packet = new Packet(sequenceNumber , sequenceNumber , checkSum, message.getData());
            packets.add(packet);
            return packet;
        }

        public boolean hasUnacknowledgedPackets(){
            return acknowledgementNumber != NO_UNACKNOWLEDGED_PACKETS;
        }

        public Packet preparePacket(){
           Packet packet = packets.poll(); //Get the next packet in the Queue
           if(packet != null){
              setPacket(packet);
           }
           return packet;
        }

        public Packet nextPacket(){
            return packets.poll();
        }

        public void setNoUnacknowledgedPackets(){
           this.acknowledgementNumber = NO_UNACKNOWLEDGED_PACKETS;
        }


    }


    private static int computeCheckSum(int sequenceNumber , int acknowledgementNumber , String message){
        int sum = sequenceNumber + acknowledgementNumber + characterSum(message.toCharArray());
        return sum;
    }

    private static int computeCheckSum(Packet p){
        return p.getSeqnum() + p.getAcknum() + characterSum(p.getPayload().toCharArray());
    }

    private static int characterSum(char[] characters){
        int sum = 0;
        for(int current: characters){
            sum += current;
        }
        return sum;
    }

    private static boolean expectedPacket(int number , int packet ){
       return  number == packet;
    }

    private boolean isCorrupted(Packet packet , int checksum){
       return computeCheckSum(packet) != checksum;
    }

    private static class ReceiverState{
        int expectedSequenceNumber;

        public ReceiverState(){
            this.expectedSequenceNumber = 0;
        }

        public void incrementExpectedSequenceNumber(){
            expectedSequenceNumber = (expectedSequenceNumber + 1) % 2; //Even numbered transmissions will have a sequence number of 1
        }
    }
}
