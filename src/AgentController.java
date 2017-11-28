import jade.content.ContentElement;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.Agent;
import jade.core.Location;
import jade.core.ProfileImpl;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.mobility.MobilityOntology;
import jade.core.Runtime;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Steven on 2017-11-28.
 */
public class AgentController extends Agent {
    private jade.wrapper.AgentContainer home;
    private jade.wrapper.AgentContainer[] container = null;
    private Map<String, Location> locations = new HashMap<String, Location>();
    Runtime runtime = Runtime.instance();
    @Override
    protected void setup(){
        // Register language and ontology
        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(MobilityOntology.getInstance());

        home = runtime.createAgentContainer(new ProfileImpl());

        container = new AgentContainer[2];
        jade.wrapper.AgentController a;
        home = runtime.createAgentContainer(new ProfileImpl());
        try {

            for(int i = 0; i < container.length; i++){
                container[i] = runtime.createAgentContainer(new ProfileImpl());
            }
            doWait(2000);
            sendRequest(new Action(getAMS(), new QueryPlatformLocationsAction()));

            //Receive response from AMS
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchSender(getAMS()),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage resp = blockingReceive(mt);
            ContentElement ce = getContentManager().extractContent(resp);
            Result result = (Result) ce;
            jade.util.leap.Iterator it = result.getItems().iterator();
            while (it.hasNext()) {
                Location loc = (Location)it.next();
                locations.put(loc.getName(), loc);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendRequest (Action action) {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.setLanguage(new SLCodec().getName());
        request.setOntology(MobilityOntology.getInstance().getName());
        try {
            getContentManager().fillContent(request, action);
            request.addReceiver(action.getActor());
            send(request);
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }

}
