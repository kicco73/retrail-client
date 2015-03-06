/* 
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.client.impl;

import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.commons.impl.Client;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.Server;
import it.cnr.iit.retrail.commons.StateType;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PEP extends Server implements PEPInterface {
    public final static int version = 2;
    
    protected final Client client;
    protected final Map<String, PepSession> sessions;
    protected final Map<String, String> sessionNameByCustomId;
    private final String uconInterfaceName;
    private boolean heartbeat = false;

    /**
     *
     * @param pdpUrl
     * @param myUrl
     * @throws java.lang.Exception
     */
    public PEP(URL pdpUrl, URL myUrl) throws Exception {
        super(myUrl, PEPProtocolProxy.class, "PEP");
        client = new Client(pdpUrl);
        sessions = new HashMap<>();
        sessionNameByCustomId = new HashMap<>();
        uconInterfaceName = pdpUrl.getProtocol().equals("https")? "UConS" : "UCon";
    }

    public void waitHeartbeat() {
        log.warn("waiting next heartbeat from server");
        try {
            // Wait heartbeat for synchronization
            heartbeat = false;
            synchronized (this) {
                while (!heartbeat) {
                    wait(getWatchdogPeriod());
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
        id = uuid != null ? uuid : id;
        PepSession s = sessions.get(id);
        return s;
    }

    @Override
    public final synchronized PepSession tryAccess(PepRequest req, String customId) throws Exception {
        log.debug("" + req);
        Object[] params = new Object[]{req.toElement(), myUrl.toString(), customId};
        Document doc = (Document) client.execute(uconInterfaceName+".tryAccess", params);
        //log.info("TRYACCESS got Y {}", DomUtils.toString(doc));
        PepSession response = newPepSession(doc);
        if (response.getStateType() != StateType.END) { // FIXME was == TRY
            updateSession(response);
        }
        //log.info("TRYACCESS got {}, obligations {}", response, response.getObligations());
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
            old = s;
        } else {
            sessionNameByCustomId.remove(old.getCustomId());
            Map<String, Object> savedLocalInfo = old.getLocalInfo();
            BeanUtils.copyProperties(old, s);
            old.setLocalInfo(savedLocalInfo);
        }
        sessionNameByCustomId.put(s.getCustomId(), s.getUuid());
        return old;
    }

    private void removeSession(PepSession s) throws IllegalAccessException, InvocationTargetException {
        sessionNameByCustomId.remove(s.getCustomId());
        sessions.remove(s.getUuid());
        s.setStateType(StateType.END); // FIXME was DELETED (btw: is it still needed in v2.0? try without)
    }

    /**
     *
     * @return
     */
    @Override
    public Collection<PepSession> getSessions() {
        return sessions.values();
    }

    public final synchronized PepSession apply(String actionName, String uuid, String customId, Object[] args) throws Exception {
        log.debug("uuid={}, customId={}", uuid, customId);
        Object[] params = new Object[]{actionName, uuid, customId, args};
        Document doc = (Document) client.execute(uconInterfaceName+".apply", params);
        PepSession response = newPepSession(doc);
        runObligations(response);
        return updateSession(response);
    }
    
    @Override
    public final synchronized PepSession apply(PepSession session, String actionName, Object...args) throws Exception {
        session = apply(actionName, session.getUuid(), session.getCustomId(), args);
        return updateSession(session);
    }
    public final synchronized PepSession assignCustomId(String uuid, String customId, String newCustomId) throws Exception {
        log.debug("uuid={}, customId={}, newCustomId={}", uuid, customId, newCustomId);
        Object[] params = new Object[]{uuid, customId, newCustomId};
        Document doc = (Document) client.execute(uconInterfaceName+".assignCustomId", params);
        PepSession response = null;
        if(doc != null && (doc instanceof Document)) {
            response = newPepSession(doc);
            sessionNameByCustomId.remove(customId);
            if (hasSession(response)) {
                response = updateSession(response);
            }
        }
        return response;
    }

    public final synchronized PepSession startAccess(String uuid, String customId) throws Exception {
        PepSession session = apply("startAccess", uuid, customId, new Object[]{});
        return updateSession(session);
    }

    @Override
    public final synchronized PepSession startAccess(PepSession session) throws Exception {
        return startAccess(session.getUuid(), session.getCustomId());
    }

    @Override
    public synchronized void onRecoverAccess(PepSession session) throws Exception {
        log.info("recovered " + session);
    }

    @Override
    public synchronized void onRevokeAccess(PepSession session) throws Exception {
        log.warn("revoked {}", session);
    }

    @Override
    public synchronized void runObligations(PepSession session) throws Exception {
        for (String obligation : session.getObligations()) {
            onObligation(session, obligation);
        }
        session.getObligations().clear();
    }

    // Protected for testing purposes
    public final synchronized PepSession endAccess(String uuid, String customId) throws Exception {
        log.debug("uuid={}, customId={}", uuid, customId);
        Object[] params = new Object[]{uuid, customId};
        Node responseDocument = (Node) client.execute(uconInterfaceName+".endAccess", params);
        PepSession response = newPepSession(((Document) responseDocument).getDocumentElement());
        //log.info("ENDACCESS got {}" + response);
        runObligations(response);
        // update necessary because someone could be holding this object and the status is changed!
        response = updateSession(response);
        if (response.getDecision() == PepResponse.DecisionEnum.Permit) {
            removeSession(response);
        }
        return response;
    }

    // for testing purposes
    public final synchronized List<PepSession> endAccess(List<String> uuidList, List<String> customIdList) throws Exception {
        Object[] params = new Object[]{uuidList, customIdList};
        Object[] responses = (Object[]) client.execute(uconInterfaceName+".endAccess", params);
        List<PepSession> pepSessions = new ArrayList<>(responses.length);
        for(Object responseDocument: responses) {
            PepSession response = newPepSession(((Document) responseDocument).getDocumentElement());
            //log.info("ENDACCESS got {}" + response);
            runObligations(response);
            // update necessary because someone could be holding this object and the status is changed!
            response = updateSession(response);
            if (response.getDecision() == PepResponse.DecisionEnum.Permit) {
                removeSession(response);
            }
            pepSessions.add(response);
        }
        return pepSessions;
    }

    @Override
    public final synchronized PepSession endAccess(PepSession session) throws Exception {
        return endAccess(session.getUuid(), session.getCustomId());
    }

    @Override
    public final synchronized void applyChanges(PepSession session, PepRequest req) throws Exception {
        Object[] params = new Object[]{req.toElement(), session.getUuid()};
        client.execute(uconInterfaceName+".applyChanges", params);
    }

    @Override
    public final synchronized List<PepSession> endAccess(List<PepSession> sessions) throws Exception {
        List<String> uuidList = new ArrayList<>(sessions.size());
        List<String> customIdList = new ArrayList<>(sessions.size());
        for(PepSession session: sessions) {
            uuidList.add(session.getUuid());
            customIdList.add(session.getCustomId());
        }
        return endAccess(uuidList, customIdList);
    }

    public synchronized Node echo(Node node) throws Exception {
        log.info("");
        Object[] params = new Object[]{node};
        return (Node) client.execute(uconInterfaceName+".echo", params);
    }

    protected PepSession newPepSession(Document d) throws Exception {
        return new PepSession(d);
    }

    protected PepSession newPepSession(Element e) throws Exception {
        return new PepSession(e);
    }

    @Override
    protected synchronized void watchdog() throws InterruptedException {
        List<String> sessionsList = new ArrayList<>(sessions.keySet());
        Object[] params = new Object[]{myUrl.toString(), sessionsList};
        Document doc;
        try {
            doc = (Document) client.execute(uconInterfaceName+".heartbeat", params);
            log.debug("received heartbeat: {}", DomUtils.toString(doc.getDocumentElement()));
            NodeList configs = doc.getElementsByTagNameNS("*", "Config");
            if (configs.getLength() > 0) {
                Element config = (Element) configs.item(0);
                int serverVersion = Integer.parseInt(config.getAttribute("version"));
                if(serverVersion > version)
                    throw new RuntimeException("Server is v"+serverVersion+", while we are v"+version+": client needs to be updated");
                setWatchdogPeriod(Integer.parseInt(config.getAttribute("watchdogPeriod")));
            }
            NodeList sessionList = doc.getElementsByTagNameNS("*", "Response");
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
                    //pepSession = updateSession(pepSession);
                    onRecoverAccess(pepSession);
                }
            }
            if (sessionList.getLength() == 0) {
                log.debug("OK -- no changes (sessions: " + sessions.size() + ")");
            }
            heartbeat = true;
            notifyAll();
        } catch (XmlRpcException | ParserConfigurationException ex) {
            log.error("while parsing heartbeat response: {}", ex.toString());
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception ex) {
            log.error("unexpected exception: {}", ex.toString());
        }
    }

    @Override
    public void onObligation(PepSession session, String obligation) throws Exception {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public void startRecording(File outputFile) throws Exception {
        client.startRecording(outputFile);
    }
    
    @Override
    public void continueRecording(File outputFile, long millis) throws Exception {
        client.continueRecording(outputFile, millis);
    }

    @Override
    public boolean isRecording() {
        return client.isRecording();
    }
    
    @Override
    public void stopRecording() {
        client.stopRecording();
    }

    @Override
    public SSLContext trustAllPeers() throws Exception {
        return client.trustAllPeers();
    }

    @Override
    public SSLContext trustAllPeers(InputStream keyStore, String password) throws Exception {
        trustAllPeers();
        return super.trustAllPeers(keyStore, password);
    }
    
    @Override
    public void term() throws InterruptedException {
        if(isRecording())
            stopRecording();
        super.term();
    }

}
