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

package com.stratio.crossdata.core.structures;

import java.io.Serializable;
import java.util.List;

import com.stratio.crossdata.common.statements.structures.Selector;

/**
 * GroupByClause class that has the metadata of the group by of statement.
 */
public class HavingClause implements Serializable {

    private static final long serialVersionUID = -1822936795045576938L;

    private List<Selector> selectorIdentifier;

    /**
     * Basic constructor class.
     */
    public HavingClause() {
    }

    /**
     * Constructor class.
     * @param selectorIdentifier The selector identifier.
     */
    public HavingClause(
            List<Selector> selectorIdentifier) {
        this.selectorIdentifier = selectorIdentifier;
    }

    public List<Selector> getSelectorIdentifier() {
        return selectorIdentifier;
    }

    public void setSelectorIdentifier(List<Selector> selectorIdentifier) {
        this.selectorIdentifier = selectorIdentifier;
    }
}
