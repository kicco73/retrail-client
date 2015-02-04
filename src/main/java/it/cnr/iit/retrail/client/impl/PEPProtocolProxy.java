/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.client.impl;

import it.cnr.iit.retrail.client.PEPProtocol;
import java.util.List;
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
    public List<Node> revokeAccess(List<Node> pepSessions) throws Exception {
        log.warn("{}", pepSessions);
        return mediator.revokeAccess(pepSessions);
    }

    @Override
    public List<Node> runObligations(List<Node> pepSessions) throws Exception {
        log.warn("{}", pepSessions);
        return mediator.runObligations(pepSessions);
    }
}
