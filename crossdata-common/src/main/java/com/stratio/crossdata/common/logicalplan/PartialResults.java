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

package com.stratio.crossdata.common.logicalplan;

import java.util.Set;

import com.stratio.crossdata.common.data.ResultSet;
import com.stratio.crossdata.common.metadata.Operations;

/**
 * Logical step to represent the movement of partial results between different connectors.
 */
public class PartialResults extends TransformationStep{

    private static final long serialVersionUID = 2180826506482075833L;
    private ResultSet results = null;

    /**
     * Class constructor.
     *
     * @param operations The operations to be applied.
     */
    public PartialResults(Set<Operations> operations) {
        super(operations);
    }

    public void setResults(ResultSet results) {
        this.results = results;
    }

    public ResultSet getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "PARTIAL RESULTS";
    }
}
