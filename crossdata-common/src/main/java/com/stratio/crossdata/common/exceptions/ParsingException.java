/*
 * Licensed to STRATIO (C) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.  The STRATIO (C) licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.stratio.crossdata.common.exceptions;

import java.util.List;

/**
 * Parsing exception thrown by the Driver if the statement could not be parsed.
 */
public class ParsingException extends RuntimeException {

    /**
     * Serial version UID in order to be {@link java.io.Serializable}.
     */
    private static final long serialVersionUID = -1125608075378630223L;

    private final List<String> errors;

//TODO:javadoc
    public ParsingException(String message) {
        super(message);
        this.errors = null;
    }

//TODO:javadoc
    public ParsingException(Exception e) {
        super(e.getMessage());
        this.errors=null;
    }

//TODO:javadoc
    public ParsingException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

//TODO:javadoc
    public List<String> getErrors() {
        return this.errors;
    }

}
