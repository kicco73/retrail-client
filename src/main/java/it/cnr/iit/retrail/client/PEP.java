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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PEP extends Server implements PEPInterface {

    protected final Client client;
    protected final Map<String, PepSession> sessions;
    private boolean accessRecoverableByDefault;
    private boolean heartbeat = false;

    /**
     *
     * @param pdpUrl
     * @param myUrl
     * @throws java.net.UnknownHostException
     * @throws org.apache.xmlrpc.XmlRpcException
     */
    public PEP(URL pdpUrl, URL myUrl) throws XmlRpcException, UnknownHostException {
        super(myUrl, XmlRpc.class, "PEP");
        accessRecoverableByDefault = false;
        client = new Client(pdpUrl);
        sessions = new HashMap<>();
    }

    public void waitHeartbeat() {
        log.warn("waiting next heartbeat from server");
        try {
            // Wait heartbeat for synchronization
            heartbeat = false;
            synchronized(this) {
                while(!heartbeat)
                    wait(watchdogPeriod);
            }
            log.warn("heartbeat ok");
        } catch (InterruptedException ex) {
            log.error(ex.getMessage());
        }
    }
    
    @Override
    public void init() throws IOException {
        // register myself to event mediator since API instances will send events to listeners
        PEPMediator.getInstance().addListener(this);
        super.init();
        waitHeartbeat();
    }

    @Override
    public final synchronized boolean hasSession(PepSession session) {
        return sessions.containsKey(session.getUuid());
    }

    public final synchronized PepSession tryAccess(PepAccessRequest req, String customId) throws Exception {
        log.info("" + req);
        Object[] params = new Object[]{req.toElement(), myUrl.toString(), customId};
        Document doc = (Document) client.execute("UCon.tryAccess", params);
        PepSession response = new PepSession(doc);
        if (response.getUuid() != null) {
            updateSession(response);
        }
        log.debug("end " + req);
        return response;
    }

    @Override
    public final synchronized PepSession tryAccess(PepAccessRequest req) throws Exception {
        return tryAccess(req, null);
    }

    private void updateSession(PepSession s) throws IllegalAccessException, InvocationTargetException {
        PepSession old = sessions.get(s.getUuid());
        if(old == null)
            sessions.put(s.getUuid(), s);
        else {
            BeanUtils.copyProperties(old, s);
        }
    }
    
    private void removeSession(PepSession s) throws IllegalAccessException, InvocationTargetException {
        // update necessary because someone could be holding this object and the status is changed!
        updateSession(s);
        sessions.remove(s.getUuid());
    }
    
    public final synchronized PepSession startAccess(String uuid, String customId) throws Exception {
        log.info("uuid={}, customId={}", uuid, customId);
        Object[] params = new Object[]{uuid, customId};
        Document doc = (Document) client.execute("UCon.startAccess", params);
        PepSession response = new PepSession(doc);
        updateSession(response);
        log.info("got: {}", response);
        return response;
    }

    public final synchronized void assignCustomId(String uuid, String customId, String newCustomId) throws Exception {
        log.warn("uuid={}, customId={}, newCustomId={}", uuid, customId, newCustomId);
        Object[] params = new Object[]{uuid, customId, newCustomId};
        Document doc = (Document) client.execute("UCon.assignCustomId", params);
        PepSession response = new PepSession(doc);
        if(hasSession(response))
            updateSession(response);
    }

    @Override
    public final synchronized PepSession startAccess(PepSession session) throws Exception {
        return startAccess(session.getUuid(), session.getCustomId());
    }

    @Override
    public final synchronized void onRecoverAccess(PepSession session) throws Exception {
        log.warn("" + session);
        if (session.getStatus() != PepSession.Status.REVOKED && shouldRecoverAccess(session)) {
            log.warn("recovering " + session);
            updateSession(session);
        } else {
            log.warn("discarding " + session);
            endAccess(session);
        }
    }

    protected boolean shouldRecoverAccess(PepSession session) {
        log.warn("defaults to " + isAccessRecoverableByDefault());
        return isAccessRecoverableByDefault();
    }

    @Override
    public synchronized void onRevokeAccess(PepSession session) throws Exception {
        log.warn("calling endAccess for {}", session);
        endAccess(session);
    }

    public final synchronized PepSession endAccess(String uuid, String customId) throws Exception {
        log.info("uuid={}, customId={}", uuid, customId);
        Object[] params = new Object[]{uuid, customId};
        Document doc = (Document) client.execute("UCon.endAccess", params);
        PepSession response = new PepSession(doc);
        log.info("got {}" + response);
        removeSession(response);
        return response;
    }

    @Override
    public final synchronized PepSession endAccess(PepSession session) throws Exception {
        return endAccess(session.getUuid(), session.getCustomId());
    }

    public synchronized Node echo(Node node) throws Exception {
        log.info("");
        Object[] params = new Object[]{node};
        return (Node) client.execute("UCon.echo", params);
    }

    public boolean isAccessRecoverableByDefault() {
        return accessRecoverableByDefault;
    }

    public void setAccessRecoverableByDefault(boolean accessRecoverableByDefault) {
        this.accessRecoverableByDefault = accessRecoverableByDefault;
    }
    
    @Override
    protected synchronized void watchdog() {
        List<String> sessionsList = new ArrayList<>(sessions.keySet());
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
                    onRevokeAccess(pepSession);
                } else {
                    log.info("recovering " + pepSession);
                    onRecoverAccess(pepSession);
                }
            }
            if (sessionList.getLength() == 0) {
                log.debug("OK -- no changes (sessions: " + sessions.size() + ")");
            }
            heartbeat = true;
            notifyAll();
        } catch (XmlRpcException | ParserConfigurationException ex) {
            log.error(ex.toString());
        } catch (Exception ex) {
            log.error(ex.toString());
        }
    }
}
