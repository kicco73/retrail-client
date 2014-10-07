/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.client.impl;

import it.cnr.iit.retrail.client.PEPInterface;
import it.cnr.iit.retrail.client.PEPProtocol;
import it.cnr.iit.retrail.commons.impl.PepSession;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Handles events from the APIs exposed by the web server and dispatches them to
 the proper PEP instance. PEP instances must register by addListener() on
 creation in order for them to be dispatched async events.
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
    public synchronized Node revokeAccess(Node session) throws Exception {
        PepSession pepSession = new PepSession((Document) session);
        // TODO: uconUrl ignored for now, assuming only one pdp.
        URL uconUrl = pepSession.getUconUrl();
        PepSession found = null;

        for (PEPInterface listener : listeners) {
            found = listener.getSession(pepSession.getUuid());
            if (found != null) {
                try {
                    BeanUtils.copyProperties(found, pepSession);
                    listener.onRevokeAccess(found);
                    break;
                } catch (Exception ex) {
                    log.error(ex.toString());
                }
            }
        }

        if (found == null) {
            log.error("UNEXISTENT SESSION: " + pepSession);
        }
        return null;
    }

}
