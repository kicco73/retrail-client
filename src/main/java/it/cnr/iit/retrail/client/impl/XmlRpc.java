/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.client.impl;

import it.cnr.iit.retrail.client.XmlRpcProtocol;
import java.net.MalformedURLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
public class XmlRpc implements XmlRpcProtocol {

    protected static final Logger log = LoggerFactory.getLogger(XmlRpc.class);
    protected static final PEPMediator mediator = PEPMediator.getInstance();

    @Override
    public Node revokeAccess(Node pepSession) throws MalformedURLException {
        log.warn("{}", pepSession);
        return mediator.revokeAccess(pepSession);
    }

}
