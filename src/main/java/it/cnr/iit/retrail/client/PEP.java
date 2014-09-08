/*
 */

package it.cnr.iit.retrail.client;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.Server;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class PEP extends Server {
    private final XmlRpcClient client;    

    /**
     *
     * @param pdpUrl
     * @param myUrl
     * @throws java.net.UnknownHostException
     * @throws org.apache.xmlrpc.XmlRpcException
     */
    public PEP(URL pdpUrl, URL myUrl) throws UnknownHostException, XmlRpcException, IOException {
        super(myUrl, API.class);
        // create configuration
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(pdpUrl);
        config.setEnabledForExtensions(true);
        config.setConnectionTimeout(60 * 1000);
        config.setReplyTimeout(60 * 1000);

        client = new XmlRpcClient();
        // use Commons HttpClient as transport
        client.setTransportFactory(
                new XmlRpcCommonsTransportFactory(client));
        // set configuration
        client.setConfig(config);
    }
    
    public boolean tryAccess(PepAccessRequest req) throws Exception {
        Object[] params = new Object[]{req.toElement()};
        Document doc = (Document) client.execute("UCon.tryAccess", params);
        PepAccessResponse response = new PepAccessResponse(doc);
        boolean result = false;
        switch(response.decision) {
            default:
                DomUtils.write(doc);
                break;
            case Indeterminate:
                String statusMessage = doc.getElementsByTagName("StatusMessage").item(0).getTextContent();
                System.out.println("RISULTATO INDETERMINATO: "+response.message);
                break;
            case Permit:
                result = true;
                break;
            case Deny:
                result = false;
                break;
        }
        return result;
    }
    
    public void startAccess(PepAccessRequest req) throws Exception {
        Object[] params = new Object[]{req.toElement()};
        client.execute("UCon.startAccess", params);
    }
        
    public void endAccess(PepAccessRequest req) throws Exception {
        Object[] params = new Object[]{req.toElement()};
        client.execute("UCon.endAccess", params);
    }

    public Node echo(Node node) throws Exception {
        Object[] params = new Object[]{node};
        return (Node) client.execute("UCon.echo", params);
    }
    
}
