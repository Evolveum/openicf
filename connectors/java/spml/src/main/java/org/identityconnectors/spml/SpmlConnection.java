/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.spml;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.openspml.v2.client.Spml2Client;
import org.openspml.v2.msg.spml.ErrorCode;
import org.openspml.v2.msg.spml.ExecutionMode;
import org.openspml.v2.msg.spml.LookupRequest;
import org.openspml.v2.msg.spml.LookupResponse;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.openspml.v2.msg.spml.Request;
import org.openspml.v2.msg.spml.Response;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spml.StatusCode;
import org.openspml.v2.util.Spml2Exception;
import org.openspml.v2.util.Spml2ExceptionWithResponse;


/**
 * A Connection to a SPML 2.0 Server
 */
public class SpmlConnection  {
    private ScriptExecutorFactory  _factory;
    private Log                    log = Log.getLog(SpmlConnection.class);
    private Spml2Client            _client;
    private SpmlConfiguration      _configuration;
    private Map<Object, Object>    _memory;
    private ScriptExecutor         _preSendExecutor;
    private ScriptExecutor         _postReceiveExecutor;

    /**
     * This method is used to create a new connection for the pool.
     * <p>
     * Once the connection has been made, the PostConnectScript, if specified, is run.
     * The following variables are made available to the PostConnectScript:
     * <ul>
     * <li>connection -- the Connection just made</li>
     * <li>username -- the username specified in the Configuration</li>
     * <li>password -- the password specified in the Configuration</li>
     * <li>memory -- a Map<String, Object> persisted between script invocations, which can be used as a store</li>
     * </ul>
     * @param client -- The SPML2CLient used for the Connection
     * @param configuration -- the SPMLConfiguration containing the connection parameters
     */
    public SpmlConnection(Spml2Client client, SpmlConfiguration configuration) {
        _client = client;
        _configuration = configuration;
        _factory = ScriptExecutorFactory.newInstance(configuration.getScriptingLanguage());

        String preCommand = _configuration.getPreSendCommand();
        String postCommand = _configuration.getPostReceiveCommand();
        try {
            if (preCommand!=null && preCommand.length()>0)
                _preSendExecutor = _factory.newScriptExecutor(getClass().getClassLoader(), preCommand, true);
        } catch (Exception e) {
            throw new ConnectorException(_configuration.getMessage(SpmlMessages.PRESEND_SCRIPT_ERROR), e);
        }
        try {
            if (postCommand!=null && postCommand.length()>0)
                _postReceiveExecutor = _factory.newScriptExecutor(getClass().getClassLoader(), postCommand, true);
        } catch (Exception e) {
            throw new ConnectorException(_configuration.getMessage(SpmlMessages.POSTRECEIVE_SCRIPT_ERROR), e);
        }
        _memory = new HashMap<Object, Object>();
        String postConnectCommand = _configuration.getPostConnectCommand();
        try {
            if (postConnectCommand!=null && postConnectCommand.length()>0) {
                ScriptExecutor executor = _factory.newScriptExecutor(getClass().getClassLoader(), postConnectCommand, true);
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("connection", this);
                map.put("username", _configuration.getUserName());
                map.put("password", _configuration.getPassword());
                map.put("memory", getMemory());
                executor.execute(map);
            }
        } catch (Exception e) {
            log.error(e, "error in SpmlConnection constructor");
            throw new ConnectorException(_configuration.getMessage(SpmlMessages.POSTCONNECT_SCRIPT_ERROR), e);
        }
        log.info("created SpmlConnection");
    }

    protected Map<Object, Object> getMemory() {
        return _memory;
    }

    private String toString(Request request) {
        return MessageFormat.format("Request Type=''{0}'',ID=''{1}'',Mode=''{2}''",
                request.getElementName(),
                request.getRequestID(),
                request.getExecutionMode());
    }

    /**
     * Send a Request to the SPML server.
     * <p>Before the Request is sent, the PreSendScript, if specified, is run.
     * The following variables are made available to the PreSendScript:
     * <ul>
     * <li>request -- the SPML2 Request about to be sent</li>
     * <li>memory -- a Map<String, Object> persisted between script invocations, which can be used as a store</li>
     * </ul>
     * <p>After the Response is received, the PostReceiveScript, if specified, is run.
     * The following variables are made available to the PostReceiveScript:
     * <ul>
     * <li>response -- the SPML2 Response just received</li>
     * <li>memory -- a Map<String, Object> persisted between script invocations, which can be used as a store</li>
     * </ul>
     * 
     * @param req -- the SPML2 Request object
     * @return an SPML2 Response
     * @throws Spml2ExceptionWithResponse
     * @throws Spml2Exception
     */
    public Response send(Request req) throws Spml2ExceptionWithResponse, Spml2Exception {
        log.info("send(''{0}'')", toString(req));
        try {
            if (_preSendExecutor!=null) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("request", req);
                map.put("memory", _memory);
                _preSendExecutor.execute(map);
            }
        } catch (Exception e) {
            log.error(e, "error in send");
            throw new ConnectorException(_configuration.getMessage(SpmlMessages.PRESEND_SCRIPT_ERROR), e);
        }
        Response response = _client.send(req);
        try {
            if (_postReceiveExecutor!=null) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("response", response);
                map.put("memory", _memory);
                _postReceiveExecutor.execute(map);
            }
        } catch (Exception e) {
            log.error(e, "error in receive");
            throw new ConnectorException(_configuration.getMessage(SpmlMessages.POSTRECEIVE_SCRIPT_ERROR), e);
        }
        return response;
    }

    /**
     * Disposes of a Connection.
     * <p>
     * Before the Connection is disposed, the PreDisconnectScript, if specified, is run.
     * The following variables are made available to the PreDisconnectScript:
     * <ul>
     * <li>connection -- the Connection about to be disposed</li>
     * <li>username -- the username specified in the Configuration</li>
     * <li>password -- the password specified in the Configuration</li>
     * <li>memory -- a Map<String, Object> persisted between script invocations, which can be used as a store</li>
     * </ul>
     * See {@link Connection#dispose()}
     */
    public void dispose() {
        String preCommand = _configuration.getPreDisconnectCommand();
        if (preCommand!=null && preCommand.length()>0) {
            ScriptExecutor executor = _factory.newScriptExecutor(getClass().getClassLoader(), preCommand, true);
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("connection", this);
            map.put("username", _configuration.getUserName());
            map.put("password", _configuration.getPassword());
            map.put("memory", _memory);
            try {
                executor.execute(map);
            } catch (Exception e) {
                throw new ConnectorException(_configuration.getMessage(SpmlMessages.PREDISCONNECT_SCRIPT_ERROR), e);
            }
        }
    }
    /**
     * {@inheritDoc}
     */
    public void test() {
        try {
            if (_configuration.getTargetNames()==null ||
                _configuration.getTargetNames().length==0) {
                throw new ConnectorException(_configuration.getMessage(SpmlMessages.MAPPING_REQUIRED));
            }
            get("random name", _configuration.getTargetNames()[0]);
        } catch (Exception e) {
            throw ConnectorException.wrap(e);
        }
    }

    private void get(String uid, String targetId) {
        try {
            LookupRequest request = new LookupRequest();
            PSOIdentifier psoId = new PSOIdentifier();
            psoId.setTargetID(targetId);
            psoId.setID(uid);
            log.info("get(''{0}'')", uid);
            request.setPsoID(psoId);
            request.setRequestID(uid);
            request.setReturnData(ReturnData.EVERYTHING);
            request.setExecutionMode(ExecutionMode.SYNCHRONOUS);
            LookupResponse response = (LookupResponse)send(request);
            if (!response.getStatus().equals(StatusCode.SUCCESS)) {
                if (response.getError()!=ErrorCode.NO_SUCH_IDENTIFIER)
                    throw new ConnectorException(asString(response.getErrorMessages()));
            }
        } catch (Spml2ExceptionWithResponse e) {
            log.error(e, "get failed:''{0}''", e.getResponse().getError());
            if (e.getResponse().getError()!=ErrorCode.NO_SUCH_IDENTIFIER)
                throw new ConnectorException(asString(e.getResponse().getErrorMessages()));
        } catch (Exception e) {
            log.error(e, "get failed");
            throw ConnectorException.wrap(e);
        }
    }

    protected String asString(String[] strings) {
        if (strings.length==0)
            return "";
        StringBuffer buffer = new StringBuffer();
        for (String string : strings)
            buffer.append("\n"+string);
        return buffer.toString().substring(1);
    }


}
