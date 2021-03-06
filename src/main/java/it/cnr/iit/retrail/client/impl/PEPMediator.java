/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.client.impl;

import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.client.PEPProtocol;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.impl.PepSession;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Handles events from the APIs exposed by the web server and dispatches them to
 * the proper PEP instance. PEP instances must register by addListener() on
 * creation in order for them to be dispatched async events.
 *
 * @author oneadmin
 */
public class PEPMediator implements PEPProtocol {

    static private PEPMediator instance = null;
    final private Collection<PEPInterface> listeners = new ArrayList<>();
    protected static final Logger log = LoggerFactory.getLogger(PEPMediator.class);

    static PEPMediator getInstance() {
        if (instance == null) {
            instance = new PEPMediator();
        }
        return instance;
    }

    public synchronized void addListener(PEPInterface listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(PEPInterface listener) {
        listeners.remove(listener);
    }

    @Override
    public synchronized List<Node> revokeAccess(List<Node> sessions) throws Exception {
        for (Node session: sessions) {
            //log.warn("received revocation: {}", DomUtils.toString(session));
            PepSession pepSession = new PepSession((Document) session);
            // TODO: uconUrl ignored for now, assuming only one pdp.
            URL uconUrl = pepSession.getUconUrl();
            PepSession found = null;
            for (PEPInterface listener : listeners) {
                found = listener.getSession(pepSession.getUuid());
                if (found != null) {
                    try {
                        Map<String, Object> savedLocalInfo = found.getLocalInfo();
                        BeanUtils.copyProperties(found, pepSession);
                        found.setLocalInfo(savedLocalInfo);
                        listener.onRevokeAccess(found);
                        listener.runObligations(found);
                        break;
                    } catch (Exception ex) {
                        log.error(ex.toString());
                    }
                }
            }
            if (found == null) {
                log.error("UNEXISTENT SESSION: " + pepSession);
            }
        }
        log.info("revocation done");
        return null;
    }

    @Override
    public synchronized List<Node> runObligations(List<Node> sessions) throws Exception {
        for (Node session: sessions) {
            PepSession pepSession = new PepSession((Document) session);
            // TODO: uconUrl ignored for now, assuming only one pdp.
            URL uconUrl = pepSession.getUconUrl();
            PepSession found = null;
            for (PEPInterface listener : listeners) {
                found = listener.getSession(pepSession.getUuid());
                if (found != null) {
                    try {
                        Map<String, Object> savedLocalInfo = found.getLocalInfo();
                        BeanUtils.copyProperties(found, pepSession);
                        found.setLocalInfo(savedLocalInfo);
                        listener.runObligations(found);
                        break;
                    } catch (Exception ex) {
                        log.error(ex.toString());
                    }
                }
            }

            if (found == null) {
                log.error("UNEXISTENT SESSION: " + pepSession);
            }
        }
        return null;
    }

}
