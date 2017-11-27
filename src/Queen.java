import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import sun.plugin2.message.Message;

import java.util.ArrayList;
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
    List<AID> queens;

    public void setup(){
        System.out.println(getLocalName() + ": G'day m8");
        queens = new ArrayList<>();
        Object[] args = getArguments();
        if(args.length > 0){
            index = Integer.parseInt((String)args[0]);
        }
        if(args.length > 1){
            n = Integer.parseInt((String)args[1]);
        }
        placements = new int[n];

        registerAtDf();
        while(queens.size() < n - 1){
            findQueens();
        }

        // Start behaviour
        addBehaviour(new BoardPlacingBehaviour());
    }

    public void findQueens() {
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
            for(DFAgentDescription dfad: result){
                queens.add(dfad.getName());
            }


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

    public void addRecipient(ACLMessage msg){
        for(AID a: queens)
            msg.addReceiver(a);
    }

    ACLMessage predReply;
    MessageTemplate mt = MessageTemplate.MatchOntology("PLACEMENT");
    private class BoardPlacingBehaviour extends CyclicBehaviour{
        public BoardPlacingBehaviour() {
            System.out.println(getLocalName() + ": CyclicBehaviour initialized");
        }

        @Override

        public void action() {
            if(index == 0 && !placed){
                block(1000); // wait to make sure everyone has initialized
                column = r.nextInt(n);
                placements[index] = column;
                ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
                message.setOntology("PLACEMENT");
                message.setConversationId("DONE");
                message.setContent(index + "x" + column);

                addRecipient(message); // Add all queens as recipient
                send(message);
                System.out.println(getLocalName() + " Found the placement " + index + "x" + column);
                placed = true;
            }

            ACLMessage msg = blockingReceive(mt, 5000);
            if(msg != null){
                String str = msg.getContent();

                switch (msg.getPerformative()){
                    case ACLMessage.PROPOSE:
                        String[] args = str.split("x");
                        int row = Integer.parseInt(args[0]);
                        int col = Integer.parseInt(args[1]);
                        placements[row] = col;
                        if(row+1 == index){
                            System.out.println(getLocalName() + " predecessor has been placed");
                            predReply = msg.createReply();
                            addBehaviour(new findMySpot(true));
                        }
                        break;
                    case ACLMessage.REJECT_PROPOSAL:
                        System.out.println(getLocalName() + " successor couldn't find a placement try find a new one");
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
            if(column >= n && index == 0)
                column = 0;

            while(collision(column)){
                column++;
                if(column >= n){
                    System.out.println(getLocalName() + " Couldn't find a placement");
                    predReply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    predReply.setOntology("PLACEMENT");
                    send(predReply);
                    return;
                }
            }

            System.out.println(getLocalName() + " Found the placement " + index + "x" + column);
            placements[index] = column;

            placed = true;
            ACLMessage message = new ACLMessage(ACLMessage.PROPOSE);
            message.setOntology("PLACEMENT");
            message.setConversationId("DONE");
            message.setContent(index + "x" + column);
            addRecipient(message);
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

            }
        }
    }

}
