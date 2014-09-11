/*
 */
package it.cnr.iit.retrail.client;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.PepSession;
import it.cnr.iit.retrail.commons.Server;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PEP extends Server implements Runnable {

    private static final int heartbeatPeriod = 15;
    private final XmlRpcClient client;
    private final Set<PepSession> sessions; 
    
    /**
     *
     * @param pdpUrl
     * @param myUrl
     * @throws java.net.UnknownHostException
     * @throws org.apache.xmlrpc.XmlRpcException
     */
    public PEP(URL pdpUrl, URL myUrl) throws UnknownHostException, XmlRpcException, IOException {
        super(myUrl, APIImpl.class);
        this.sessions = new HashSet<>();
        // create configuration
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(pdpUrl);
        config.setEnabledForExtensions(true);
        config.setConnectionTimeout(60 * 1000);
        config.setReplyTimeout(60 * 1000);

        client = new XmlRpcClient();
        // use Commons HttpClient as transport
        client.setTransportFactory(
                new XmlRpcCommonsTransportFactory(client));
        // set configuration
        client.setConfig(config);
    }

    public void init() {        // start heartbeat
        // register myself to event mediator since API instances will send events to listeners
        PEPMediator.getInstance().addListener(this);
        (new Thread(this)).start();
    }

    public synchronized boolean hasSession(PepSession session) {
        return sessions.contains(session);
    }
    
    public synchronized boolean tryAccess(PepAccessRequest req) throws Exception {
        Object[] params = new Object[]{req.toElement()};
        Document doc = (Document) client.execute("UCon.tryAccess", params);
        PepAccessResponse response = new PepAccessResponse(doc);
        boolean result = false;
        switch (response.decision) {
            default:
                DomUtils.write(doc);
                break;
            case Indeterminate:
                String statusMessage = doc.getElementsByTagName("StatusMessage").item(0).getTextContent();
                System.out.println("RISULTATO INDETERMINATO: " + response.message);
                break;
            case Permit:
                result = true;
                break;
            case Deny:
                result = false;
                break;
        }
        return result;
    }

    public synchronized PepSession startAccess(PepAccessRequest req) throws Exception {
        Object[] params = new Object[]{req.toElement(), myUrl.toString()};
        Document doc = (Document) client.execute("UCon.startAccess", params);
        PepSession response = new PepSession(doc);
        if(response.getId() != null)
            sessions.add(response);
        return response;
    }
    
    public synchronized void recoverAccess(PepSession session) {
        System.out.println("PEP.recoverAccess(): "+session);
        sessions.add(session);
    }
    
    public synchronized void revokeAccess(PepSession session) {
        System.out.println("PEP.revokeAccess(): "+session);
        sessions.remove(session);
    }

    public synchronized void endAccess(PepSession session) throws Exception {
        Object[] params = new Object[]{session.getId()};
        client.execute("UCon.endAccess", params);
        sessions.remove(session);
    }

    public synchronized Node echo(Node node) throws Exception {
        Object[] params = new Object[]{node};
        return (Node) client.execute("UCon.echo", params);
    }

    private synchronized void heartbeat() throws ParserConfigurationException {
        List<String> sessionsList = new ArrayList<>();
        for(PepSession s: sessions)
            sessionsList.add(s.getId());
        Object[] params = new Object[]{myUrl.toString(), sessionsList};
        Document doc = null;
        try {
            doc = (Document) client.execute("UCon.heartbeat", params);
            NodeList sessionList = doc.getElementsByTagName("Response");
            for(int n = 0; n < sessionList.getLength(); n++) {
                Document d = DomUtils.newDocument();
                Element e = (Element) d.importNode(sessionList.item(n), true);
                d.appendChild(e);
                PepSession pepSession = new PepSession(d);
                if(pepSession.decision != PepAccessResponse.DecisionEnum.Permit) {
                    System.out.println("PEP.heartbeat(): emulating the revokation of "+pepSession);
                    revokeAccess(pepSession);
                } else {
                    System.out.println("PEP.heartbeat(): recovering "+pepSession);
                    recoverAccess(pepSession);
                }
            } 
            if(sessionList.getLength() == 0) 
                    System.out.println("PEP.heartbeat(): OK -- no changes (sessions: "+sessions.size()+")");
        } catch (XmlRpcException ex) {
            Logger.getLogger(PEP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                heartbeat();
                Thread.sleep(heartbeatPeriod * 1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(PEP.class.getName()).log(Level.SEVERE, null, ex);
                return;
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(PEP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
