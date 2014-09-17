/*
 */
package it.cnr.iit.retrail.client;

import it.cnr.iit.retrail.commons.Client;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.PepSession;
import it.cnr.iit.retrail.commons.Server;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PEP extends Server {

    private final Client client;
    private final Set<PepSession> sessions;

    /**
     *
     * @param pdpUrl
     * @param myUrl
     * @throws java.net.UnknownHostException
     * @throws org.apache.xmlrpc.XmlRpcException
     */
    public PEP(URL pdpUrl, URL myUrl) throws XmlRpcException, UnknownHostException {
        super(myUrl, APIImpl.class);
        client = new Client(pdpUrl);
        sessions = new HashSet<>();
    }

    @Override
    public void init() throws IOException {
        // register myself to event mediator since API instances will send events to listeners
        PEPMediator.getInstance().addListener(this);
        super.init();
    }

    public synchronized boolean hasSession(PepSession session) {
        return sessions.contains(session);
    }

    public synchronized PepSession tryAccess(PepAccessRequest req) throws Exception {
        log.info("begin " + req);
        Object[] params = new Object[]{req.toElement(), myUrl.toString(), null};
        Document doc = (Document) client.execute("UCon.tryAccess", params);
        PepSession response = new PepSession(doc);
        if (response.getSystemId() != null) {
            sessions.add(response);
        }
        log.info("end " + req);
        return response;
    }

    public synchronized PepSession startAccess(PepSession session) throws Exception {
        log.info("begin " + session);
        Object[] params = new Object[]{session.getSystemId(), session.getCustomId()};
        Document doc = (Document) client.execute("UCon.startAccess", params);
        PepSession response = new PepSession(doc);
        log.info("end " + session);
        return response;
    }

    public synchronized void recoverAccess(PepSession session) {
        log.info("" + session);
        sessions.add(session);
    }

    public synchronized void revokeAccess(PepSession session) throws Exception {
        log.warn("calling endAccess for {}", session);
        endAccess(session);
    }

    public synchronized void endAccess(PepSession session) throws Exception {
        log.info("" + session);
        Object[] params = new Object[]{session.getSystemId(), session.getCustomId()};
        client.execute("UCon.endAccess", params);
        sessions.remove(session);
    }

    public synchronized Node echo(Node node) throws Exception {
        log.info("");
        Object[] params = new Object[]{node};
        return (Node) client.execute("UCon.echo", params);
    }

    @Override
    protected synchronized void watchdog() {
        List<String> sessionsList = new ArrayList<>();
        for (PepSession s : sessions) {
            sessionsList.add(s.getSystemId());
        }
        Object[] params = new Object[]{myUrl.toString(), sessionsList};
        Document doc;
        try {
            doc = (Document) client.execute("UCon.heartbeat", params);
            NodeList sessionList = doc.getElementsByTagName("Response");
            for (int n = 0; n < sessionList.getLength(); n++) {
                Document d = DomUtils.newDocument();
                Element e = (Element) d.importNode(sessionList.item(n), true);
                d.appendChild(e);
                PepSession pepSession = new PepSession(d);
                if (pepSession.decision != PepAccessResponse.DecisionEnum.Permit) {
                    log.info("emulating the revocation of " + pepSession);
                    revokeAccess(pepSession);
                } else {
                    log.info("recovering " + pepSession);
                    recoverAccess(pepSession);
                }
            }
            if (sessionList.getLength() == 0) {
                log.debug("OK -- no changes (sessions: " + sessions.size() + ")");
            }
        } catch (XmlRpcException | ParserConfigurationException ex) {
            log.error(ex.toString());
        } catch (Exception ex) {
            log.error(ex.toString());
        }
    }

}
