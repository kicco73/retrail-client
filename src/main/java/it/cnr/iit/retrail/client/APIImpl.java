/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
public class APIImpl implements API {
    
    protected static final Logger log = LoggerFactory.getLogger(APIImpl.class);
    @Override
    public void revokeAccess(Node pepSession) {
        PEPMediator.getInstance().revokeAccess(pepSession);
    }

}
