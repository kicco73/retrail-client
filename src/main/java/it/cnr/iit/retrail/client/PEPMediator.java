/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.client;

import it.cnr.iit.retrail.commons.PepSession;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
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
public class PEPMediator implements API {

    static private PEPMediator instance = null;
    final private Collection<PEP> listeners = new ArrayList<>();
    protected static final Logger log = LoggerFactory.getLogger(PEPMediator.class);

    static PEPMediator getInstance() {
        if (instance == null) {
            instance = new PEPMediator();
        }
        return instance;
    }

    public synchronized void addListener(PEP listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(PEP listener) {
        listeners.remove(listener);
    }

    @Override
    public synchronized Node revokeAccess(Node session, String pdpUrl) throws MalformedURLException {
        // TODO: pdpUrl ignored for now, assuming only one pdp.
        PepSession pepSession = new PepSession((Document) session);
        boolean found = false;

        for (PEP listener : listeners) {
            if (found = listener.hasSession(pepSession)) {
                try {
                    listener.revokeAccess(pepSession);
                    break;
                } catch (Exception ex) {
                    log.error(ex.toString());
                }
            }
        }

        if (!found) {
            log.error("UNEXISTENT SESSION: " + pepSession);
        }
        return null;
    }

}
