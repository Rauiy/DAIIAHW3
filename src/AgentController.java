import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.Agent;
import jade.core.Location;
import jade.core.ProfileImpl;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.mobility.MobilityOntology;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.core.Runtime;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

/**
 * Created by Steven on 2017-11-28.
 */
public class AgentController extends Agent {
    private jade.wrapper.AgentContainer home;
    private jade.wrapper.AgentContainer[] container = null;

    Runtime runtime = Runtime.instance();
    @Override
    protected void setup(){
        home = runtime.createAgentContainer(new ProfileImpl());

        container = new AgentContainer[2];
        jade.wrapper.AgentController a;
        home = runtime.createAgentContainer(new ProfileImpl());
        try {
            a = home.createNewAgent("Original Auctioneer", CuratorAgent.class.getName(), null);
            for(int i = 0; i < container.length; i++){
                container[i] = runtime.createAgentContainer(new ProfileImpl());
                a = container[i].createNewAgent("" + i, CuratorAgent.class.getName(), null);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
