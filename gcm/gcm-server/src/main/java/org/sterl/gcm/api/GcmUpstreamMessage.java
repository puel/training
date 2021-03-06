/**
 * Copyright 2016 Paul Sterl
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific 
 * language governing permissions and limitations under the License.
 */
package org.sterl.gcm.api;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * https://developers.google.com/cloud-messaging/xmpp-server-ref#upstream
 * 
 * Base class should be considered to be able to overwrite / define a custom data type here.
 */
@Data @JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GcmUpstreamMessage {
    /**
     * This parameter specifies who sent the message.
     * The value is the registration token of the client app.
     */
    protected String from;
    /** This parameter specifies the application package name of the client app that sent the message. */
    protected String category;
    /** This parameter specifies the key-value pairs of the message's payload. */
    protected Map<String, Object> data = new HashMap<>();
    
    // additional fields only for ACK and NACK
    
    protected String to;
    @JsonProperty("message_id")
    protected String messageId;
    /**
     * This parameter specifies an ack message from an app server to CCS. For upstream messages, it should always be set to ack.
     * 
     * But also e.g. "receipt".
     */
    @JsonProperty("message_type")
    protected String messageType;
}
