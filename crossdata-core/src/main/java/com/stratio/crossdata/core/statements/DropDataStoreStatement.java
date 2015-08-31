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

package com.stratio.crossdata.core.statements;

import com.stratio.crossdata.core.validator.requirements.ValidationRequirements;
import com.stratio.crossdata.core.validator.requirements.ValidationTypes;

/**
 * Drop datastore statement class.
 */
public class DropDataStoreStatement extends MetadataStatement {

    /**
     * The datastore name.
     */
    private String name;

    /**
     * Constructor class.
     * @param name The datastore name.
     */
    public DropDataStoreStatement(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "DROP DATASTORE " + name;
    }

    @Override
    public ValidationRequirements getValidationRequirements() {
        return new ValidationRequirements().add(ValidationTypes.MUST_EXIST_DATASTORE);
    }

    /**
     * Get the name of the datastore to drop.
     * @return A String.
     */
    public String getName() {
        return name;
    }
}