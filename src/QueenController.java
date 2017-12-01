import jade.content.ContentElement;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.ProfileImpl;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by Steven on 2017-11-28.
 */
public class QueenController extends GuiAgent {
    private jade.wrapper.AgentContainer home;
    private jade.wrapper.AgentContainer[] container = null;
    private Map locations = new HashMap();
    private Vector agents = new Vector();
    private int agentCnt = 0;
    private int command;
    //transient protected ControllerAgentGui myGui;

    public static final int QUIT = 0;
    public static final int NEW_AGENT = 1;
    public static final int MOVE_AGENT = 2;
    public static final int CLONE_AGENT = 3;
    public static final int KILL_AGENT = 4;
    jade.core.Runtime runtime = jade.core.Runtime.instance();
    protected void onGuiEvent(GuiEvent ev) {

    }

    @Override
    protected void setup(){
        int n = 9;

        Object[] arguments = getArguments();
        if(arguments.length > 0)
            n = Integer.parseInt((String)arguments[0]);

        try {
            // Create the container objects
            home = runtime.createAgentContainer(new ProfileImpl());
            jade.wrapper.AgentController a = null;
            for(int i = 0; i < n; i++) {
                if( i == 0) {
                    Object[] args = new Object[2];
                    args[0] = n+"";
                    a = home.createNewAgent(i+"", Queen.class.getName(), args);
                }else{
                    a = home.createNewAgent(i+"", Queen.class.getName(), null);
                }
                a.start();
            }
            doWait(2000);
        }
        catch (Exception e) { e.printStackTrace(); }

    }
}
