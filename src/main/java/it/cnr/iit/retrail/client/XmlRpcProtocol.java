/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.client;

import java.net.MalformedURLException;
import org.w3c.dom.Node;

public interface XmlRpcProtocol {
    Node revokeAccess(Node pepSession) throws MalformedURLException;
}