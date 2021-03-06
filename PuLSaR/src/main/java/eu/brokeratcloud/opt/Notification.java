package eu.brokeratcloud.opt;

import eu.brokeratcloud.common.BrokerObject;
import eu.brokeratcloud.persistence.annotations.*;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@RdfSubject(
	uri="http://www.brokeratcloud.eu/v1/opt/NOTIFICATION"
)
public class Notification extends BrokerObject {
	@XmlAttribute
	@RdfPredicate
	protected String service;
	@XmlAttribute
	@RdfPredicate
	protected String message;
	
	public String getService() { return service; }
	public void setService(String s) { service = s; }
	public String getMessage() { return message; }
	public void setMessage(String s) { message = s; }
	
	public String toString() {
		return 	"Notification: {\n"+super.toString()+
				"\tservice = "+service+"\n\tmessage = "+message+"}\n";
	}
}
