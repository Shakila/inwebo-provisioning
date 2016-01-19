/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.identity.provisioning.connector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.provisioning.ProvisioningOperation;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.ProvisionedIdentifier;
import org.wso2.carbon.identity.provisioning.ProvisioningEntity;
import org.wso2.carbon.identity.provisioning.ProvisioningEntityType;
import org.wso2.carbon.identity.provisioning.AbstractOutboundProvisioningConnector;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;

import java.util.Properties;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.MimeHeaders;

public class InweboProvisioningConnector extends AbstractOutboundProvisioningConnector {

    private static final Log log = LogFactory.getLog(InweboProvisioningConnector.class);
    private InweboProvisioningConnectorConfig configHolder;

    @Override
    public void init(Property[] provisioningProperties) throws IdentityProvisioningException {
        Properties configs = new Properties();

        if (provisioningProperties != null && provisioningProperties.length > 0) {
            for (Property property : provisioningProperties) {
                configs.put(property.getName(), property.getValue());
                if (IdentityProvisioningConstants.JIT_PROVISIONING_ENABLED.equals(property
                        .getName())) {
                    if ("1".equals(property.getValue())) {
                        jitProvisioningEnabled = true;
                    }
                }
            }
        }
        configHolder = new InweboProvisioningConnectorConfig(configs);
    }

    @Override
    public ProvisionedIdentifier provision(ProvisioningEntity provisioningEntity)
            throws IdentityProvisioningException {
        String provisionedId = null;
        if (provisioningEntity != null) {
            String login = provisioningEntity.getEntityName().toString();
            String userId = configHolder.getValue("UserId");
            String serviceId = configHolder.getValue("ServiceId");
            String p12file = configHolder.getValue("P12file");
            String p12password = configHolder.getValue("test");
            String firstName = configHolder.getValue("FirstName");
            String name = configHolder.getValue("Name");
            String mail = configHolder.getValue("Mail");
            String phone = configHolder.getValue("PhoneNumber");
            String status = configHolder.getValue("Status");
            String role = configHolder.getValue("Role");
            String access = configHolder.getValue("Access");
            String codeType = configHolder.getValue("CodeType");

            if (provisioningEntity.isJitProvisioning() && !isJitProvisioningEnabled()) {
                log.debug("JIT provisioning disabled for inwebo connector");
                return null;
            }
            if (provisioningEntity.getEntityType() == ProvisioningEntityType.USER) {
                if (provisioningEntity.getOperation() == ProvisioningOperation.POST) {
                    provisionedId = createAUser(provisioningEntity, login, userId, serviceId, p12file, p12password, firstName,
                            name, mail, phone, status, role, access, codeType);
                } else if (provisioningEntity.getOperation() == ProvisioningOperation.DELETE) {
                    deleteUser(provisioningEntity, p12file, p12password, serviceId);
                } else {
                    throw new IdentityProvisioningException("Unsupported provisioning opertaion.");
                }
            } else {
                throw new IdentityProvisioningException("Unsupported provisioning opertaion.");
            }
        }
        // creates a provisioned identifier for the provisioned user.
        ProvisionedIdentifier identifier = new ProvisionedIdentifier();
        identifier.setIdentifier(provisionedId);
        return identifier;
    }

    private String createAUser(ProvisioningEntity provisioningEntity, String login, String userId, String serviceId, String p12file, String p12password, String firstName, String name,
                               String mail, String phone, String status, String role, String access, String codeType)
            throws IdentityProvisioningException {
        boolean isDebugEnabled = log.isDebugEnabled();
        String provisionedId = null;
        try {
            Util.setHttpsClientCert(p12file, p12password);
        } catch (Exception e) {
            throw new IdentityProvisioningException("Error while adding certificate", e);

        }
        try {
            UserCreation userCreation = new UserCreation(login, userId, serviceId, firstName, name, mail,
                    phone, status, role, access, codeType, p12file, p12password);
//            provisionedId = userCreation.invokeSOAP();
            sendCall();
        } catch (Exception e) {
            throw new IdentityProvisioningException("Error while creating the user", e);
        }
        return provisionedId;
    }

    /**
     * @param provisioningEntity
     * @throws IdentityProvisioningException
     */
    private void deleteUser(ProvisioningEntity provisioningEntity, String p12file, String p12password, String serviceId)
            throws IdentityProvisioningException {
        try {
            Util.setHttpsClientCert(p12file, p12password);
        } catch (Exception e) {
            throw new IdentityProvisioningException("Error while adding certificate", e);
        }
        try {
            String userId = provisioningEntity.getIdentifier().getIdentifier();
            UserDeletion UserDeletion = new UserDeletion(userId, serviceId);
            UserDeletion.deleteUser();
        } catch (IdentityProvisioningException e) {
            throw new IdentityProvisioningException("Error while creating the user", e);
        }
    }

    public static void sendCall() {
        try {
            Util.setHttpsClientCert("/Users/Shaki/Desktop/inwebo/testComp.p12", "hellohai");
            // Create SOAP Connection
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            // Send SOAP Message to SOAP Server
            String url = "https://api.myinwebo.com/services/ConsoleAdmin";
            SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(), url);

            // Process the SOAP Response
            //printSOAPResponse(soapResponse);

            soapConnection.close();
        } catch (Exception e) {
            System.err.println("Error occurred while sending SOAP Request to Server");
            e.printStackTrace();
        }
    }

    private static SOAPMessage createSOAPRequest() throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        String serverURI = "http://console.inwebo.com";

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("con", serverURI);

        SOAPBody soapBody = envelope.getBody();
        SOAPElement soapBodyElem = soapBody.addChildElement("loginCreate", "con");
        SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("userid", "con");
        soapBodyElem1.addTextNode("1234");
        SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("serviceid", "con");
        soapBodyElem2.addTextNode("1111");
        SOAPElement soapBodyElem3 = soapBodyElem.addChildElement("login", "con");
        soapBodyElem3.addTextNode("testuser");
        SOAPElement soapBodyElem4 = soapBodyElem.addChildElement("firstname", "con");
        soapBodyElem4.addTextNode("S");
        SOAPElement soapBodyElem5 = soapBodyElem.addChildElement("name", "con");
        soapBodyElem5.addTextNode("S");
        SOAPElement soapBodyElem6 = soapBodyElem.addChildElement("mail", "con");
        soapBodyElem6.addTextNode("a@gmail.com");
        SOAPElement soapBodyElem7 = soapBodyElem.addChildElement("phone", "con");
        soapBodyElem7.addTextNode("771234567");
        SOAPElement soapBodyElem8 = soapBodyElem.addChildElement("status", "con");
        soapBodyElem8.addTextNode("1");
        SOAPElement soapBodyElem9 = soapBodyElem.addChildElement("role", "con");
        soapBodyElem9.addTextNode("1");
        SOAPElement soapBodyElem10 = soapBodyElem.addChildElement("access", "con");
        soapBodyElem10.addTextNode("1");
        SOAPElement soapBodyElem11 = soapBodyElem.addChildElement("codetype", "con");
        soapBodyElem11.addTextNode("1");
        SOAPElement soapBodyElem12 = soapBodyElem.addChildElement("lang", "con");
        soapBodyElem12.addTextNode("En");
        SOAPElement soapBodyElem13 = soapBodyElem.addChildElement("extrafields", "con");
        soapBodyElem13.addTextNode("");

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.removeHeader("content-type");
        headers.removeHeader("Content-Length");
        headers.removeHeader("Transfer-Encoding");
        headers.addHeader("SOAPAction", serverURI  + "/services/ConsoleAdmin");
        headers.addHeader("Content-Type", "text/xml");
        headers.addHeader("FORCE_HTTP_1.0", "true");
//        headers.addHeader("Content-Length", "411");
        soapMessage.saveChanges();

        /* Print the request message */
        System.out.print("Request SOAP Message = ");
        soapMessage.writeTo(System.out);
        System.out.println();

        return soapMessage;
    }

    /**
     * Method used to print the SOAP Response
     */
    private static void printSOAPResponse(SOAPMessage soapResponse) throws Exception {
//        TransformerFactory transformerFactory = TransformerFactory.newInstance();
//        Transformer transformer = transformerFactory.newTransformer();
//        Source sourceContent = soapResponse.getSOAPPart().getContent();
//        System.out.print("\nResponse SOAP Message = ");
//        StreamResult result = new StreamResult(System.out);
//        transformer.transform(sourceContent, result);
    }
}