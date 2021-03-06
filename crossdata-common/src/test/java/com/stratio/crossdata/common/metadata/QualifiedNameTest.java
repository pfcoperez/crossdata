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

package com.stratio.crossdata.common.metadata;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.stratio.crossdata.common.data.QualifiedNames;

public class QualifiedNameTest {
    @Test
    public void testCatalogQualifiedName() {
        String name = QualifiedNames.getCatalogQualifiedName("test");
        Assert.assertEquals(name, "test");
    }

    @Test
    public void testTableQualifiedName() {
        String name = QualifiedNames.getTableQualifiedName("test", "taBle");
        Assert.assertEquals(name, "test.taBle");
    }

    @Test
    public void testColumnQualifiedName() {
        String name = QualifiedNames.getColumnQualifiedName("test", "taBle", "column");
        Assert.assertEquals(name, "test.taBle.column");
    }

    @Test
    public void testClusterQualifiedName() {
        String name = QualifiedNames.getClusterQualifiedName("Test");
        Assert.assertEquals(name, "cluster.Test");
    }

    @Test
    public void testConnectorQualifiedName() {
        String name = QualifiedNames.getConnectorQualifiedName("teSt");
        Assert.assertEquals(name, "connector.teSt");
    }

    @Test
    public void testDataStoreQualifiedName() {
        String name = QualifiedNames.getDataStoreQualifiedName("TesT");
        Assert.assertEquals(name, "datastore.TesT");
    }

}
