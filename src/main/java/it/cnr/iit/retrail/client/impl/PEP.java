/* 
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.client.impl;

import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.commons.Client;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.Server;
import it.cnr.iit.retrail.commons.Status;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PEP extends Server implements PEPInterface {

    public final Client client;
    protected final Map<String, PepSession> sessions;
    protected final Map<String, String> sessionNameByCustomId;
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
        super(myUrl, PEPProtocolProxy.class, "PEP");
        accessRecoverableByDefault = false;
        client = new Client(pdpUrl);
        sessions = new HashMap<>();
        sessionNameByCustomId = new HashMap<>();
    }

    public void waitHeartbeat() {
        log.warn("waiting next heartbeat from server");
        try {
            // Wait heartbeat for synchronization
            heartbeat = false;
            synchronized (this) {
                while (!heartbeat) {
                    wait(watchdogPeriod);
                }
            }
            log.warn("heartbeat ok");
        } catch (InterruptedException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public void init() throws Exception {
        // register myself to event mediator since API instances will send events to listeners
        PEPMediator.getInstance().addListener(this);
        super.init();
        waitHeartbeat();
    }

    @Override
    public final synchronized boolean hasSession(PepSession session) {
        return sessions.containsKey(session.getUuid());
    }

    @Override
    public final synchronized PepSession getSession(String id) {
        String uuid = sessionNameByCustomId.get(id);
        id = uuid != null? uuid : id;
        PepSession s = sessions.get(id);
        return s;
    }

    @Override
    public final synchronized PepSession tryAccess(PepRequest req, String customId) throws Exception {
        log.debug("" + req);
        Object[] params = new Object[]{req.toElement(), myUrl.toString(), customId};
        Document doc = (Document) client.execute("UCon.tryAccess", params);
        log.info("TRYACCESS got Y {}", DomUtils.toString(doc));
        PepSession response = newPepSession(doc);
        if (response.getStatus() == Status.TRY) {
            updateSession(response);
        }
        log.info("TRYACCESS 4 got {}, obligations {}", response, response.getObligations());
        runObligations(response);
        return response;
    }

    @Override
    public final synchronized PepSession tryAccess(PepRequest req) throws Exception {
        return tryAccess(req, null);
    }

    private PepSession updateSession(PepSession s) throws IllegalAccessException, InvocationTargetException {
        PepSession old = sessions.get(s.getUuid());
        if (old == null) {
            sessions.put(s.getUuid(), s);
        } else {
            sessionNameByCustomId.remove(old.getCustomId());
            Map<String,Object> savedLocalInfo = old.getLocalInfo();
            BeanUtils.copyProperties(old, s);
            old.setLocalInfo(savedLocalInfo);
        }
        sessionNameByCustomId.put(s.getCustomId(), s.getUuid());
        return old;
    }

    private void removeSession(PepSession s) throws IllegalAccessException, InvocationTargetException {
        sessionNameByCustomId.remove(s.getCustomId());
        sessions.remove(s.getUuid());
        s.setStatus(Status.DELETED);
    }

    /**
     *
     * @return
     */
    @Override
    public Collection<PepSession> getSessions() {
        return sessions.values();
    }

    public final synchronized PepSession startAccess(String uuid, String customId) throws Exception {
        log.debug("uuid={}, customId={}", uuid, customId);
        Object[] params = new Object[]{uuid, customId};
        Document doc = (Document) client.execute("UCon.startAccess", params);
        PepSession response = newPepSession(doc);
        log.debug("STARTACCESS GOT: {}", response);
        log.info("STARTACCESS {}", DomUtils.toString(doc));
        runObligations(response);
        return updateSession(response);
    }

    public final synchronized PepSession assignCustomId(String uuid, String customId, String newCustomId) throws Exception {
        log.debug("uuid={}, customId={}, newCustomId={}", uuid, customId, newCustomId);
        Object[] params = new Object[]{uuid, customId, newCustomId};
        Document doc = (Document) client.execute("UCon.assignCustomId", params);
        PepSession response = newPepSession(doc);
        sessionNameByCustomId.remove(customId);
        if (hasSession(response)) {
            response = updateSession(response);
        }
        return response;
    }

    @Override
    public final synchronized PepSession startAccess(PepSession session) throws Exception {
        session = startAccess(session.getUuid(), session.getCustomId());
        return updateSession(session);
    }

    @Override
    public final synchronized void onRecoverAccess(PepSession session) throws Exception {
        log.warn("" + session);
        if (session.getStatus() != Status.REVOKED && shouldRecoverAccess(session)) {
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

    @Override
    public synchronized void runObligations(PepSession session) throws Exception {
        for (String obligation : session.getObligations()) {
            onObligation(session, obligation);
        }
        session.getObligations().clear();
    }

    public final synchronized PepSession endAccess(String uuid, String customId) throws Exception {
        log.debug("uuid={}, customId={}", uuid, customId);
        Object[] params = new Object[]{uuid, customId};
        Document doc = (Document) client.execute("UCon.endAccess", params);
        PepSession response = newPepSession(doc);
        log.info("ENDACCESS got {}" + response);
        runObligations(response);
        // update necessary because someone could be holding this object and the status is changed!
        response = updateSession(response);
        if (response.getDecision() == PepResponse.DecisionEnum.Permit) {
            removeSession(response);
        }
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

    @Override
    public boolean isAccessRecoverableByDefault() {
        return accessRecoverableByDefault;
    }

    @Override
    public void setAccessRecoverableByDefault(boolean accessRecoverableByDefault) {
        this.accessRecoverableByDefault = accessRecoverableByDefault;
    }
    
    protected PepSession newPepSession(Document d) throws Exception {
        return new PepSession(d);
    }

    @Override
    protected synchronized void watchdog() throws InterruptedException {
        List<String> sessionsList = new ArrayList<>(sessions.keySet());
        Object[] params = new Object[]{myUrl.toString(), sessionsList};
        Document doc;
        try {
            doc = (Document) client.execute("UCon.heartbeat", params);
            log.debug("received heartbeat: {}", DomUtils.toString(doc.getDocumentElement()));
            NodeList configs = doc.getElementsByTagName("Config");
            if(configs.getLength() > 0) {
                Element config = (Element) configs.item(0);
                watchdogPeriod = Integer.parseInt(config.getAttribute("watchdogPeriod"));
            }
            NodeList sessionList = doc.getElementsByTagName("Response");
            for (int n = 0; n < sessionList.getLength(); n++) {
                Document d = DomUtils.newDocument();
                Element e = (Element) d.importNode(sessionList.item(n), true);
                d.appendChild(e);
                PepSession pepSession = newPepSession(d);
                runObligations(pepSession);
                if (pepSession.getDecision() != PepResponse.DecisionEnum.Permit) {
                    log.warn("emulating the revocation of " + pepSession);
                    onRevokeAccess(pepSession);
                } else {
                    log.warn("recovering " + pepSession);
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
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception ex) {
            ex.printStackTrace();
            //log.error(ex.toString());
        }
    }

    @Override
    public void onObligation(PepSession session, String obligation) throws Exception {
        throw new UnsupportedOperationException("Not implemented.");
    }
}
