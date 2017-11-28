import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.SimpleAchieveREResponder;
import jade.proto.states.MsgReceiver;

import java.util.Objects;
import java.util.Random;

/**
 * Created by Steven on 2017-11-20.
 */
public class CuratorAgent extends Agent{
    private int myBid = 3000;
    private int sBid = myBid;
    private AuctionItem item;
    private AID artistManager = null;
    private int strategy = 0; // 0 = increase by flat value, 1 = increase incrementally, 2 = increase decremental
    private double modifier = 500; // 1.2; // 1000
    private double sMod = modifier;
    private Random r = new Random();
    private String aName;
    // Template for receiving auction begin
    final MessageTemplate informTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("START"),
            MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchOntology("AUCTION")));

    // Template for matching all auction messages
    final MessageTemplate auctionTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("ITEM"),
            MessageTemplate.MatchOntology("AUCTION"));
    protected void setup() {
        Object[] args = getArguments();
        if(args != null && args.length > 0)
            aName = (String)args[0];
        init();
    }

    private void init(){
        if(strategy == 0)
            myBid += 500;
        if(strategy == 2)
            myBid += 1000;

        sBid = myBid;
        sMod = modifier;

        System.out.println(getLocalName() + ": Starting with strategy: " + strategy + " and modifier: " + modifier);

        while (artistManager == null) {
            artistManager = findAgent(this, "AUCTION");
        }

        addBehaviour(new joinAuction());
    }

    protected void afterClone(){
        System.out.println("I am " + getLocalName());
        //participant clone
        if(getLocalName().contains("pc1")){
            strategy = 1;
            modifier = 1.1;
            init();
        }
        else if(getLocalName().contains("pc2")){
            strategy = 2;
            modifier = r.nextInt(1000)+500;
            init();
        }
    }

    private void startAuctionActions(Agent myAgent){
        SequentialBehaviour sb = new SequentialBehaviour();
        sb.addSubBehaviour(new receiveInfo(this, informTemplate, System.currentTimeMillis()+30000, null, null));
        sb.addSubBehaviour(new receiveAuctions(this, auctionTemplate));

        myAgent.addBehaviour(sb);
    }

    private class joinAuction extends OneShotBehaviour{
        @Override
        public void action() {

            System.out.println(getLocalName()+": Joining auction");
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setConversationId("JOIN");
            msg.addReceiver(artistManager);
            myAgent.send(msg);
            startAuctionActions(myAgent);
        }
    }

    private class receiveInfo extends MsgReceiver {
        receiveInfo(Agent a, MessageTemplate mt, long dl, DataStore ds, Object msgKey) {
            super(a, mt, dl, ds, msgKey);
        }

        @Override
        protected void handleMessage(ACLMessage msg) {
            if(msg == null)
                System.out.println(getLocalName() + ": Never got ready msg");
            else {
              //  System.out.println(myAgent.getLocalName() + " is ready for auction!");
                myBid = sBid;
                modifier = sMod;
            }
        }
    }

    public class receiveAuctions extends SimpleAchieveREResponder {
        boolean done = false;

        public receiveAuctions(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        public ACLMessage prepareResponse(ACLMessage request) {
            ACLMessage response = null;
            try{
                switch (request.getPerformative()) {
                    case ACLMessage.CFP:
                        item = (AuctionItem) request.getContentObject();
                        //System.out.println(myAgent.getLocalName() + ": item: " + item.getName() + " price: " + item.getCurrentPrice() + " my bid: " + myBid);
                        if (item.getCurrentPrice() <= myBid) {
                            response = request.createReply();
                            response.setContent("test");
                            response.setPerformative(ACLMessage.PROPOSE);
                        }
                        else{
                            //System.out.println(getLocalName() + ": too expensive");
                            increaseBid();
                        }
                        break;
                    case ACLMessage.ACCEPT_PROPOSAL:
                        System.out.println(myAgent.getLocalName() + " has won the " + item.getName());
                        break;
                    case ACLMessage.REJECT_PROPOSAL:
                        System.out.println(myAgent.getLocalName() + " has lost the " + item.getName());
                        // Use strategy change bid
                        increaseBid();
                        break;
                    case ACLMessage.INFORM:
                        done = true;
                        String str = request.getContent();
                        //System.out.println(getLocalName() + " auction ended, reason: " + str);
                        //System.out.println(getLocalName() + ": had the strategy: " + strategy + " and modifier: " + modifier);
                        break;
                    default:
                        response = request.createReply();
                        response.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                }

            } catch (UnreadableException e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response){
            return null;
        }

        @Override
        public boolean done() {
            return done;
        }
    }

    private void increaseBid(){
        switch (strategy){
            case 0:
                // Flat bid increase, always same increase
                myBid += modifier;
                break;
            case 1:
                // Incremented bid increase, begin low end high
                myBid = (int)(myBid*modifier);
                break;
            case 2:
                // Decremented bid increase, begin high end low
                modifier = modifier * 0.9;
                myBid += modifier;
                break;
            default:
                // If nothing is said add a flat modifier
                modifier += 500;
                break;
        }
    }

    private AID findAgent(Agent myAgent, String type) {
        AID tmp = null;
        DFAgentDescription template = new DFAgentDescription();
        // to find the right service type imm
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        template.addServices(sd);
        try {
            // To get all available services, don't define a template
            DFAgentDescription[] result = DFService.search(myAgent, template);
            // System.out.print("Found the following agents: ");
            // Should only exist one agent of each, so take the first one
            if(result.length > 0){
               for(DFAgentDescription dfad: result){
                    if(aName == null || dfad.getName().getLocalName().equals(aName)){
                        tmp = dfad.getName();
                        //System.out.println(getLocalName()+ ": Found auctioneer " + tmp.getLocalName());
                        break;
                    }
               }

            }

        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        return tmp;
    }
}
