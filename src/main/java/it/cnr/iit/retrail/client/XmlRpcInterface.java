/*
 */

package it.cnr.iit.retrail.client;

import java.net.MalformedURLException;
import org.w3c.dom.Node;

public interface XmlRpcInterface {
    Node revokeAccess(Node pepSession) throws MalformedURLException;
}
