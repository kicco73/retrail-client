/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.client.impl;

import it.cnr.iit.retrail.client.PEPProtocol;
import java.net.MalformedURLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
public class PEPProtocolProxy implements PEPProtocol {

    protected static final Logger log = LoggerFactory.getLogger(PEPProtocolProxy.class);
    protected static final PEPMediator mediator = PEPMediator.getInstance();

    @Override
    public Node revokeAccess(Node pepSession) throws Exception {
        log.warn("{}", pepSession);
        return mediator.revokeAccess(pepSession);
    }

}