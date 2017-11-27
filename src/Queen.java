import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.states.MsgReceiver;
import sun.plugin2.message.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Created by Steven on 2017-11-27.
 */
public class Queen extends Agent {
    boolean placed = false;
    int n = 4;
    int index;
    int column;
    Random r = new Random();
    int[] placements;
    ArrayList<AID> queens;


    public void setup(){
        System.out.println(getLocalName() + ": G'day m8");
        queens = new ArrayList<>();
        Object[] args = getArguments();

        if(args != null && args.length > 0){
            index = 0;
            registerAtDf();
            n = Integer.parseInt((String)args[0]);
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.MatchOntology("INIT"));
            System.out.println(getLocalName() + ": Waiting for participants");
            queens.add(getAID());
            addBehaviour(new waitForParticipants(this, mt, System.currentTimeMillis() + 10000, null, null));
            placements = new int[n];
            resetMat();
        }
        else {
            while (queens.size() <= 0) {
                findQueen();
            }

            addBehaviour(new join());
        }
        // Start behaviour
        //addBehaviour(new BoardPlacingBehaviour());
    }

    private void resetMat(){
        for(int i = 0; i < n; i++)
            placements[i] = -1;
    }

    public void findQueen() {
        DFAgentDescription template = new DFAgentDescription();
        // to find the right service type imm
        ServiceDescription sd = new ServiceDescription();
        sd.setType("QUEEN");
        template.addServices(sd);
        try {
            // To get all available services, don't define a template
            DFAgentDescription[] result = DFService.search(this, template);
            // System.out.print("Found the following agents: ");
            // Should only exist one agent of each, so take the first one
            if(result.length > 0)
                queens.add(result[0].getName());

        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

    }
    public void registerAtDf(){
        // Register the tour guide service in the yellow pages
        DFAgentDescription template = new DFAgentDescription();
        template.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("QUEEN");
        sd.setName(getLocalName());
        template.addServices(sd);
        try {
            DFService.register(this, template);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private class waitForParticipants extends MsgReceiver{
        public waitForParticipants(Agent a, MessageTemplate mt, long deadline, DataStore s, Object msgKey) {
            super(a, mt, deadline, s, msgKey);
        }

        @Override
        protected void handleMessage(ACLMessage msg){
            AID participant = msg.getSender();
            queens.add(participant);
        }

        @Override
        public int onEnd(){
            if(queens.size() < n){
                reset();
                addBehaviour(this);
                System.out.println(index+ ": Still waiting for participants");
            }else{
                System.out.println(index + ": all participants joined");
                addBehaviour(new sendAIDs());

            }
            return super.onEnd();
        }
    }

    private class sendAIDs extends OneShotBehaviour{

        @Override
        public void action() {

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setOntology("INIT");
            try {
                msg.setContentObject(queens);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //msg.setContent(i+"");
            for(int i = 1; i < queens.size(); i++)
                msg.addReceiver(queens.get(i));
            send(msg);


            addBehaviour(new BoardPlacingBehaviour());
        }
    }

    private class join extends OneShotBehaviour{

        @Override
        public void action() {
            ACLMessage join = new ACLMessage(ACLMessage.REQUEST);
            join.setOntology("INIT");
            join.setConversationId("JOIN");
            join.addReceiver(queens.get(0));
            send(join);
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchOntology("INIT"));
            addBehaviour(new waitForStart(myAgent,mt,System.currentTimeMillis() + 5000, null, null));
        }
    }

    private class waitForStart extends MsgReceiver{
        public waitForStart(Agent a, MessageTemplate mt, long deadline, DataStore s, Object msgKey) {
            super(a, mt, deadline, s, msgKey);
        }

        @Override
        protected void handleMessage(ACLMessage msg){
            try {
                queens = (ArrayList<AID>) msg.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
            n = queens.size();
            placements = new int[n];
            resetMat();

            for(int i = 1; i < n; i++){
                if(queens.get(i).getLocalName().equals(getLocalName()))
                    index = i;
            }


            addBehaviour(new BoardPlacingBehaviour());
        }
    }

    ACLMessage predReply;
    MessageTemplate mt = MessageTemplate.MatchOntology("PLACEMENT");
    private class BoardPlacingBehaviour extends CyclicBehaviour{
        public BoardPlacingBehaviour() {
            System.out.println(index + ": CyclicBehaviour initialized");
            counter = 0;
        }

        int counter;
        @Override

        public void action() {
            if(index == 0 && !placed){
               // block(1000); // wait to make sure everyone has initialized
                placed = true;
                addBehaviour(new findMySpot(true));
            }

            ACLMessage msg = blockingReceive(mt, 1000);
            if(msg != null){
                String str = msg.getContent();

                switch (msg.getPerformative()){
                    case ACLMessage.PROPOSE:
                        String[] args = str.split("x");
                        int row = Integer.parseInt(args[0]);
                        int col = Integer.parseInt(args[1]);
                        placements[row] = col;
                        if(row+1 == index){
                            //System.out.println(index + ": predecessor has been placed");
                            predReply = msg.createReply();
                            addBehaviour(new findMySpot(true));
                        }
                        else if(index == 0 && row != n-1) {
                            //System.out.println(index + ": a queen has found a spot");
                            //printMatrix();
                        }
                        else if (index == 0 && row == n-1){
                            counter++;
                            System.out.println(index + ": found a solution, total solutions: " + counter);
                            printMatrix();

                            //Find next solution
                            ACLMessage next = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                            next.addReceiver(queens.get(n-1));
                            next.setOntology("PLACEMENT");
                            send(next);
                        }
                        break;
                    case ACLMessage.REJECT_PROPOSAL:
                        if(index == 0 && column == n-1) {
                            System.out.println(index + ": We are done");
                            break;
                        }

                        //System.out.println(index + ": successor couldn't find a placement try find a new one");
                        addBehaviour(new findMySpot(false));
                        break;
                }
            }
        }
    }

    private class findMySpot extends OneShotBehaviour{
        public findMySpot(boolean firstTime) {
            if(firstTime)
                column = 0;
            else
                column++;


        }

        @Override
        public void action() {
            while(collision(column)){
                column++;
                if(column >= n){
                    //System.out.println(index + ": Couldn't find a placement");
                    predReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    predReply.setOntology("PLACEMENT");
                    send(predReply);
                    return;
                }
            }

            //System.out.println(index + ": Found the placement " + index + "x" + column);
            placements[index] = column;
            //printMatrix();

            ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
            message.setOntology("PLACEMENT");
            message.setConversationId("DONE");
            message.setContent(index + "x" + column);
            for(AID a : queens)
                message.addReceiver(a);

            send(message);

        }
    }

    private boolean collision(int column){
        if(column >= n)
            return true;

        int dRow, dCol;
        for(int i = 0; i < index; i++){
            if(placements[i] == column)
                return true;

            dRow = Math.abs(index-i);
            dCol = Math.abs(column-placements[i]);

            if(dRow == dCol)
                return true;
        }

        return false;
    }

    private void printMatrix(){
        for(int i = 0; i < n; i++){
            for(int j = 0; j < n; j++){
                if(placements[i] == j)
                    System.out.print("[" + i + "]");
                else
                    System.out.print("[-]");
            }
            System.out.println();
        }
    }


}
