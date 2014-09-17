/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.client;

import java.net.MalformedURLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
public class APIImpl implements API {

    protected static final Logger log = LoggerFactory.getLogger(APIImpl.class);
    protected static final PEPMediator mediator = PEPMediator.getInstance();

    @Override
    public Node revokeAccess(Node pepSession) throws MalformedURLException {
        log.warn("{}", pepSession);
        return mediator.revokeAccess(pepSession);
    }

}
