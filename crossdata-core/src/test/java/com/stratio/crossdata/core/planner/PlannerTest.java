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

package com.stratio.crossdata.core.planner;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.stratio.crossdata.common.data.CatalogName;
import com.stratio.crossdata.common.data.ClusterName;
import com.stratio.crossdata.common.data.ColumnName;
import com.stratio.crossdata.common.data.ConnectorName;
import com.stratio.crossdata.common.data.DataStoreName;
import com.stratio.crossdata.common.data.IndexName;
import com.stratio.crossdata.common.data.TableName;
import com.stratio.crossdata.common.exceptions.IgnoreQueryException;
import com.stratio.crossdata.common.exceptions.ManifestException;
import com.stratio.crossdata.common.exceptions.PlanningException;
import com.stratio.crossdata.common.exceptions.ValidationException;
import com.stratio.crossdata.common.executionplan.ExecutionType;
import com.stratio.crossdata.common.executionplan.MetadataWorkflow;
import com.stratio.crossdata.common.executionplan.QueryWorkflow;
import com.stratio.crossdata.common.executionplan.ResultType;
import com.stratio.crossdata.common.executionplan.StorageWorkflow;
import com.stratio.crossdata.common.logicalplan.Filter;
import com.stratio.crossdata.common.metadata.ClusterMetadata;
import com.stratio.crossdata.common.metadata.ColumnMetadata;
import com.stratio.crossdata.common.metadata.ColumnType;
import com.stratio.crossdata.common.metadata.ConnectorAttachedMetadata;
import com.stratio.crossdata.common.metadata.ConnectorMetadata;
import com.stratio.crossdata.common.metadata.DataType;
import com.stratio.crossdata.common.metadata.IndexMetadata;
import com.stratio.crossdata.common.metadata.IndexType;
import com.stratio.crossdata.common.metadata.Operations;
import com.stratio.crossdata.common.metadata.TableMetadata;
import com.stratio.crossdata.common.statements.structures.ColumnSelector;
import com.stratio.crossdata.common.statements.structures.IntegerSelector;
import com.stratio.crossdata.common.statements.structures.Operator;
import com.stratio.crossdata.common.statements.structures.Relation;
import com.stratio.crossdata.common.statements.structures.Selector;
import com.stratio.crossdata.common.statements.structures.StringSelector;
import com.stratio.crossdata.common.utils.Constants;
import com.stratio.crossdata.core.MetadataManagerTestHelper;
import com.stratio.crossdata.core.metadata.MetadataManager;
import com.stratio.crossdata.core.query.IParsedQuery;
import com.stratio.crossdata.core.query.MetadataParsedQuery;
import com.stratio.crossdata.core.query.MetadataPlannedQuery;
import com.stratio.crossdata.core.query.MetadataValidatedQuery;
import com.stratio.crossdata.core.query.StorageParsedQuery;
import com.stratio.crossdata.core.query.StoragePlannedQuery;
import com.stratio.crossdata.core.query.StorageValidatedQuery;

/**
 * Planner tests considering an initial input, generating all intermediate steps,
 * and generating a ExecutionWorkflow.
 */
public class PlannerTest extends PlannerBaseTest {

    private ConnectorMetadata connector1 = null;
    private ConnectorMetadata connector2 = null;

    private ClusterName clusterName = null;

    private TableMetadata table1 = null;
    private TableMetadata table2 = null;
    private TableMetadata table3 = null;

    DataStoreName dataStoreName = null;
    Map<ClusterName, Integer> clusterWithDefaultPriority = new LinkedHashMap<>();

    @BeforeClass(dependsOnMethods = {"setUp"})
    public void init() throws ManifestException {
        MetadataManagerTestHelper.HELPER.initHelper();
        dataStoreName = MetadataManagerTestHelper.HELPER.createTestDatastore();

        //Connector with join.
        Set<Operations> operationsC1 = new HashSet<>();
        operationsC1.add(Operations.PROJECT);
        operationsC1.add(Operations.SELECT_OPERATOR);
        operationsC1.add(Operations.SELECT_FUNCTIONS);
        operationsC1.add(Operations.SELECT_WINDOW);
        operationsC1.add(Operations.SELECT_GROUP_BY);
        operationsC1.add(Operations.DELETE_PK_EQ);
        operationsC1.add(Operations.CREATE_INDEX);
        operationsC1.add(Operations.DROP_INDEX);
        operationsC1.add(Operations.UPDATE_PK_EQ);
        operationsC1.add(Operations.TRUNCATE_TABLE);
        operationsC1.add(Operations.DROP_TABLE);
        operationsC1.add(Operations.PAGINATION);
        operationsC1.add(Operations.INSERT);
        operationsC1.add(Operations.INSERT_IF_NOT_EXISTS);
        operationsC1.add(Operations.INSERT_FROM_SELECT);

        //Streaming connector.
        Set<Operations> operationsC2 = new HashSet<>();
        operationsC2.add(Operations.PROJECT);
        operationsC2.add(Operations.SELECT_OPERATOR);
        operationsC2.add(Operations.FILTER_PK_EQ);
        operationsC2.add(Operations.SELECT_INNER_JOIN);
        operationsC2.add(Operations.SELECT_INNER_JOIN_PARTIALS_RESULTS);
        operationsC2.add(Operations.INSERT);

        String strClusterName = "TestCluster1";
        clusterWithDefaultPriority.put(new ClusterName(strClusterName), Constants.DEFAULT_PRIORITY);

        connector1 = MetadataManagerTestHelper.HELPER.createTestConnector("TestConnector1", dataStoreName,
                clusterWithDefaultPriority, operationsC1, "actorRef1");
        connector2 = MetadataManagerTestHelper.HELPER.createTestConnector("TestConnector2", dataStoreName,
                clusterWithDefaultPriority, operationsC2, "actorRef2");

        clusterName = MetadataManagerTestHelper.HELPER.createTestCluster(strClusterName, dataStoreName, connector1.getName(), connector2.getName());
        CatalogName catalogName = MetadataManagerTestHelper.HELPER.createTestCatalog("demo").getName();
        createTestTables(catalogName);
    }

    @AfterClass
    public void tearDown(){
        MetadataManagerTestHelper.HELPER.closeHelper();
    }

    public void createTestTables(CatalogName catalogName) {
        createTestTables(catalogName, "table1", "table2", "table3");
    }

    public void createTestTables(CatalogName catalogName, String... tableNames) {
        String[] columnNames1 = { "id", "user" };
        ColumnType[] columnTypes1 = { new ColumnType(DataType.INT), new ColumnType(DataType.TEXT) };
        String[] partitionKeys1 = { "id" };
        String[] clusteringKeys1 = { };
        table1 = MetadataManagerTestHelper.HELPER.createTestTable(clusterName, catalogName.getName(), tableNames[0],
                columnNames1, columnTypes1, partitionKeys1, clusteringKeys1, null);

        String[] columnNames2 = { "id", "email" };
        ColumnType[] columnTypes2 = { new ColumnType(DataType.INT), new ColumnType(DataType.TEXT) };
        String[] partitionKeys2 = { "id" };
        String[] clusteringKeys2 = { };
        table2 = MetadataManagerTestHelper.HELPER.createTestTable(clusterName, catalogName.getName(), tableNames[1],
                columnNames2, columnTypes2, partitionKeys2, clusteringKeys2, null);

        String[] columnNames3 = { "id_aux", "address" };
        ColumnType[] columnTypes3 = { new ColumnType(DataType.INT), new ColumnType(DataType.TEXT) };
        String[] partitionKeys3 = { "id_aux" };
        String[] clusteringKeys3 = { };
        table3 = MetadataManagerTestHelper.HELPER.createTestTable(clusterName, catalogName.getName(), tableNames[2],
                columnNames3, columnTypes3, partitionKeys3, clusteringKeys3, null);
    }

    @Test
    public void selectSingleColumn() throws ManifestException {

        init();

        String inputText = "SELECT demo.table1.id FROM demo.table1;";
        QueryWorkflow queryWorkflow = (QueryWorkflow) getPlannedQuery(inputText, "selectSingleColumn", false, table1);
        assertNotNull(queryWorkflow, "Null workflow received.");
        assertEquals(queryWorkflow.getResultType(), ResultType.RESULTS, "Invalid result type");
        assertEquals(queryWorkflow.getExecutionType(), ExecutionType.SELECT, "Invalid execution type");
        assertEquals(queryWorkflow.getActorRef(), connector1.getActorRef(), "Wrong target actor");
    }

    @Test
    public void selectWithFunction() throws ManifestException {

        init();

        String inputText = "SELECT getYear(demo.table1.id) AS getYear FROM demo.table1;";
        QueryWorkflow queryWorkflow = (QueryWorkflow) getPlannedQuery(inputText, "selectWithFunction", false, table1);
        assertNotNull(queryWorkflow, "Null workflow received.");
        assertEquals(queryWorkflow.getResultType(), ResultType.RESULTS, "Invalid result type");
        assertEquals(queryWorkflow.getExecutionType(), ExecutionType.SELECT, "Invalid execution type");
        assertEquals(queryWorkflow.getActorRef(), connector1.getActorRef(), "Wrong target actor");
    }

    @Test
    public void selectJoinMultipleColumns() throws ManifestException {

        init();

        String inputText = "SELECT demo.table1.id, demo.table1.user, demo.table2.id, demo.table2.email"
                + " FROM demo.table1"
                + " INNER JOIN demo.table2 ON demo.table1.id = demo.table2.id;";
        QueryWorkflow queryWorkflow = (QueryWorkflow) getPlannedQuery(
                inputText, "selectJoinMultipleColumns", false, table1, table2);
        assertNotNull(queryWorkflow, "Null workflow received.");
        assertEquals(queryWorkflow.getResultType(), ResultType.RESULTS, "Invalid result type");
        assertEquals(queryWorkflow.getExecutionType(), ExecutionType.SELECT, "Invalid execution type");
        assertEquals(queryWorkflow.getActorRef(), connector2.getActorRef(), "Wrong target actor");
    }

    @Test
    public void selectJoinMultipleColumnsDiffOnNames() throws ManifestException {

        init();

        String inputText = "SELECT demo.table1.id, demo.table1.user, demo.table3.id_aux, demo.table3.address"
                + " FROM demo.table1"
                + " INNER JOIN demo.table3 ON demo.table1.id = demo.table3.id_aux;";
        QueryWorkflow queryWorkflow = (QueryWorkflow) getPlannedQuery(
                inputText, "selectJoinMultipleColumnsDiffOnNames", false, table1, table3);
        assertNotNull(queryWorkflow, "Null workflow received.");
        assertEquals(queryWorkflow.getResultType(), ResultType.RESULTS, "Invalid result type");
        assertEquals(queryWorkflow.getExecutionType(), ExecutionType.SELECT, "Invalid execution type");
        assertEquals(queryWorkflow.getActorRef(), connector2.getActorRef(), "Wrong target actor");
    }

    @Test
    public void dropTable() throws ManifestException {

        init();

        String inputText = "DROP TABLE demo.table1;";

        IParsedQuery stmt = helperPT.testRegularStatement(inputText, "dropTable");

        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery((MetadataParsedQuery) stmt);

        MetadataPlannedQuery plan = null;
        try {
            plan = planner.planQuery(metadataValidatedQuery);
        } catch (PlanningException e) {
            fail("dropTable test failed");
        }

        MetadataWorkflow metadataWorkflow = (MetadataWorkflow) plan.getExecutionWorkflow();

        assertNotNull(metadataWorkflow, "Null workflow received.");
        assertEquals(metadataWorkflow.getResultType(), ResultType.RESULTS, "Invalid result type");
        assertEquals(metadataWorkflow.getExecutionType(), ExecutionType.DROP_TABLE, "Invalid execution type");
        assertTrue("actorRef1".equalsIgnoreCase(metadataWorkflow.getActorRef()), "Actor reference is not correct");
        assertEquals(metadataWorkflow.getTableName(), new TableName("demo", "table1"), "Table name is not correct");
    }

    @Test
    public void deleteRows() throws ManifestException {

        init();

        String inputText = "DELETE FROM demo.table1 WHERE id = 3;";

        String expectedText = "DELETE FROM demo.table1 WHERE demo.table1.id = 3;";

        IParsedQuery stmt = helperPT.testRegularStatement(inputText, expectedText, "deleteRows");

        StorageValidatedQuery storageValidatedQuery = new StorageValidatedQuery((StorageParsedQuery) stmt);

        StoragePlannedQuery plan = null;
        try {
            plan = planner.planQuery(storageValidatedQuery);
        } catch (PlanningException e) {
            fail("deleteRows test failed");
        }

        StorageWorkflow storageWorkflow = (StorageWorkflow) plan.getExecutionWorkflow();

        assertNotNull(storageWorkflow, "Null workflow received.");
        assertEquals(storageWorkflow.getResultType(), ResultType.RESULTS, "Invalid result type");
        assertEquals(storageWorkflow.getExecutionType(), ExecutionType.DELETE_ROWS, "Invalid execution type");
        assertTrue("actorRef1".equalsIgnoreCase(storageWorkflow.getActorRef()), "Actor reference is not correct");
        assertEquals(storageWorkflow.getTableName(), new TableName("demo", "table1"), "Table name is not correct");

        Collection<Filter> whereClauses = new ArrayList<>();
        whereClauses.add(new Filter(Operations.DELETE_PK_EQ, new Relation(
                new ColumnSelector(new ColumnName("demo", "table1", "id")),
                Operator.EQ,
                new IntegerSelector(new TableName("demo", "table1"), 3))));

        assertEquals(storageWorkflow.getWhereClauses().size(), whereClauses.size(), "Where clauses size differs");

        assertTrue(storageWorkflow.getWhereClauses().iterator().next().toString().equalsIgnoreCase(
                        whereClauses.iterator().next().toString()),
                "Where clauses are not equal");
    }

    @Test
    public void createIndex() throws ManifestException {

        init();

        String inputText = "CREATE INDEX indexTest ON demo.table1(user);";

        String expectedText = "CREATE DEFAULT INDEX indexTest ON demo.table1(demo.table1.user);";

        IParsedQuery stmt = helperPT.testRegularStatement(inputText, expectedText, "createIndex");

        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery((MetadataParsedQuery) stmt);

        MetadataPlannedQuery plan = null;
        try {
            plan = planner.planQuery(metadataValidatedQuery);
        } catch (PlanningException e) {
            fail("createIndex test failed");
        }

        MetadataWorkflow metadataWorkflow = (MetadataWorkflow) plan.getExecutionWorkflow();

        assertNotNull(metadataWorkflow, "Null workflow received.");
        assertEquals(metadataWorkflow.getResultType(), ResultType.RESULTS, "Invalid result type");
        assertEquals(metadataWorkflow.getExecutionType(), ExecutionType.CREATE_INDEX, "Invalid execution type");
        assertTrue("actorRef1".equalsIgnoreCase(metadataWorkflow.getActorRef()), "Actor reference is not correct");

        IndexMetadata indexMetadata = metadataWorkflow.getIndexMetadata();

        assertNotNull(metadataWorkflow, "Null workflow received.");
        assertTrue("actorRef1".equalsIgnoreCase(metadataWorkflow.getActorRef()), "Actor reference is not correct");
        assertEquals(indexMetadata.getType(), IndexType.DEFAULT, "Index types differ");
        assertEquals(indexMetadata.getName(), new IndexName("demo", "table1", "indexTest"), "Index names differ");

        Map<ColumnName, ColumnMetadata> columns = new HashMap<>();
        ColumnName columnName = new ColumnName("demo", "table1", "user");
        columns.put(columnName, new ColumnMetadata(
                columnName,
                null,
                new ColumnType(DataType.TEXT)));
        assertEquals(indexMetadata.getColumns().size(), columns.size(), "Column sizes differ");
        assertEquals(indexMetadata.getColumns().values().iterator().next().getColumnType(),
                columns.values().iterator().next().getColumnType(),
                "Column types differs");
    }

    @Test
    public void dropIndex() throws ManifestException {

        init();

        String inputText = "DROP INDEX demo.table1.indexTest;";

        String expectedText = "DROP INDEX demo.table1.index[indexTest];";

        IParsedQuery stmt = helperPT.testRegularStatement(inputText, expectedText, "dropIndex");

        MetadataValidatedQuery metadataValidatedQuery = new MetadataValidatedQuery((MetadataParsedQuery) stmt);

        IndexName indexName = new IndexName("demo", "table1", "indexTest");
        Map<ColumnName, ColumnMetadata> cols = new HashMap<>();
        ColumnMetadata colMetadata = new ColumnMetadata(
                new ColumnName("demo", "table1", "user"),
                null,
                new ColumnType(DataType.TEXT));
        cols.put(colMetadata.getName(), colMetadata);
        IndexMetadata indexMetadata = new IndexMetadata(
                indexName,
                cols,
                IndexType.CUSTOM,
                new HashMap<Selector, Selector>());
        table1.addIndex(indexMetadata.getName(), indexMetadata);

        MetadataPlannedQuery plan = null;
        try {
            plan = planner.planQuery(metadataValidatedQuery);
        } catch (PlanningException e) {
            fail("dropIndex test failed");
        }

        MetadataWorkflow metadataWorkflow = (MetadataWorkflow) plan.getExecutionWorkflow();

        assertNotNull(metadataWorkflow, "Null workflow received.");
        assertTrue("actorRef1".equalsIgnoreCase(metadataWorkflow.getActorRef()), "Actor reference is not correct");
        assertNotNull(metadataWorkflow, "Null workflow received.");
        assertEquals(metadataWorkflow.getResultType(), ResultType.RESULTS, "Invalid result type");
        assertEquals(metadataWorkflow.getExecutionType(), ExecutionType.DROP_INDEX, "Invalid execution type");
        assertTrue("actorRef1".equalsIgnoreCase(metadataWorkflow.getActorRef()), "Actor reference is not correct");

        indexMetadata = metadataWorkflow.getIndexMetadata();

        Map<ColumnName, ColumnMetadata> columns = new HashMap<>();
        ColumnName columnName = new ColumnName("demo", "table1", "user");
        columns.put(columnName, new ColumnMetadata(
                columnName,
                null,
                new ColumnType(DataType.TEXT)));
        assertEquals(indexMetadata.getColumns().size(), columns.size(), "Column sizes differ");

        ColumnMetadata columnMetadata = indexMetadata.getColumns().values().iterator().next();
        ColumnMetadata staticColumnMetadata = columns.values().iterator().next();

        assertEquals(columnMetadata.getColumnType(), staticColumnMetadata.getColumnType(), "Column types differs");
        assertEquals(columnMetadata.getName(), staticColumnMetadata.getName(), "Column names differ");
    }

    @Test
    public void updateTable() throws ManifestException {

        init();

        String inputText = "UPDATE demo.table1 SET user = 'DataHub' WHERE id = 1;";

        String expectedText = "UPDATE demo.table1 SET demo.table1.user = 'DataHub' WHERE demo.table1.id = 1;";

        IParsedQuery stmt = helperPT.testRegularStatement(inputText, expectedText, "updateTable");

        StorageValidatedQuery storageValidatedQuery = new StorageValidatedQuery((StorageParsedQuery) stmt);

        StoragePlannedQuery plan = null;
        try {
            plan = planner.planQuery(storageValidatedQuery);
        } catch (PlanningException e) {
            fail("updateTable test failed");
        }

        StorageWorkflow storageWorkflow = (StorageWorkflow) plan.getExecutionWorkflow();

        assertNotNull(storageWorkflow, "Null workflow received.");
        assertTrue("actorRef1".equalsIgnoreCase(storageWorkflow.getActorRef()), "Actor reference is not correct");

        assertEquals(storageWorkflow.getTableName(), new TableName("demo", "table1"), "Table names differ");

        List<Relation> relations = new ArrayList<>();
        Selector leftSelector = new ColumnSelector(new ColumnName("demo", "table1", "user"));
        Selector rightTerm = new StringSelector(new TableName("demo", "table1"), "DataHub");
        relations.add(new Relation(leftSelector, Operator.ASSIGN, rightTerm));
        assertEquals(storageWorkflow.getAssignments().size(), 1, "Wrong assignments size");
        assertEquals(storageWorkflow.getAssignments().size(), relations.size(), "Assignments sizes differ");
        assertTrue(storageWorkflow.getAssignments().iterator().next().toString().equalsIgnoreCase(
                        relations.iterator().next().toString()),
                "Assignments differ");

        List<Filter> filters = new ArrayList<>();
        ColumnSelector firstSelector = new ColumnSelector(new ColumnName("demo", "table1", "id"));
        IntegerSelector secondSelector = new IntegerSelector(new TableName("demo", "table1"), 1);
        Relation relation = new Relation(firstSelector, Operator.EQ, secondSelector);
        filters.add(new Filter(Operations.UPDATE_PK_EQ, relation));
        assertEquals(storageWorkflow.getWhereClauses().size(), 1, "Wrong where clauses size");
        assertEquals(storageWorkflow.getWhereClauses().size(), filters.size(), "Where clauses sizes differ");
        assertTrue(storageWorkflow.getWhereClauses().iterator().next().toString().equalsIgnoreCase(
                        filters.iterator().next().toString()),
                "Where clauses differ");
    }

    @Test
    public void truncateTable() throws ManifestException {

        init();

        String inputText = "TRUNCATE demo.table1;";

        IParsedQuery stmt = helperPT.testRegularStatement(inputText, "truncateTable");

        StorageValidatedQuery storageValidatedQuery = new StorageValidatedQuery((StorageParsedQuery) stmt);

        StoragePlannedQuery plan = null;
        try {
            plan = planner.planQuery(storageValidatedQuery);
        } catch (PlanningException e) {
            fail("truncateTable test failed");
        }

        StorageWorkflow storageWorkflow = (StorageWorkflow) plan.getExecutionWorkflow();

        assertNotNull(storageWorkflow, "Null workflow received.");
        assertTrue("actorRef1".equalsIgnoreCase(storageWorkflow.getActorRef()), "Actor reference is not correct");

        assertEquals(storageWorkflow.getTableName(), new TableName("demo", "table1"), "Table names differ");
        assertEquals(storageWorkflow.getExecutionType(), ExecutionType.TRUNCATE_TABLE, "Execution types differ");

    }

    @Test
    public void selectGroupBy() throws ManifestException {

        init();

        String inputText =
                "SELECT demo.table1.id, shorten(demo.table1.user) AS shorten FROM demo.table1 GROUP BY demo.table1.id;";

        QueryWorkflow queryWorkflow = (QueryWorkflow) getPlannedQuery(
                inputText, "selectGroupBy", false, table1);

        assertNotNull(queryWorkflow, "Null workflow received.");
        assertEquals(queryWorkflow.getResultType(), ResultType.RESULTS, "Invalid result type");
        assertEquals(queryWorkflow.getExecutionType(), ExecutionType.SELECT, "Invalid execution type");
        assertEquals(queryWorkflow.getActorRef(), connector1.getActorRef(), "Wrong target actor");
    }

    @Test
    public void pagination() throws ManifestException {

        init();

        String inputText = "SELECT * FROM demo.table1;";
        QueryWorkflow queryWorkflow = (QueryWorkflow) getPlannedQuery(
                inputText, "pagination", false, table1);
        int expectedPagedSize = 5;
        assertEquals(queryWorkflow.getWorkflow().getPagination(), expectedPagedSize, "Pagination plan failed.");
    }

    @Test
    public void testJoinWithStreaming() throws ManifestException {

        init();

        String inputText = "SELECT * FROM demo.table1 WITH WINDOW 5 MINUTES " +
                "INNER JOIN demo.table2 ON demo.table2.id_aux = demo.table1.id;";
        QueryWorkflow queryWorkflow = (QueryWorkflow) getPlannedQuery(
                inputText, "testJoinWithStreaming", false, table1, table2);
        assertEquals(queryWorkflow.getExecutionType(), ExecutionType.SELECT, "Planner failed.");
        assertNotNull(queryWorkflow.getTriggerStep(), "Planner failed.");
        assertNotNull(queryWorkflow.getNextExecutionWorkflow(), "Planner failed.");
        assertNotNull(queryWorkflow, "Planner failed");
    }

    @Test
    public void testInsertIntoFromSelectDirect() throws ManifestException {

        init();

        String inputText = "INSERT INTO demo.table1 (demo.table1.id, demo.table1.user) SELECT * FROM demo.table2;";
        StorageWorkflow storageWorkflow = null;
        try {
            storageWorkflow = (StorageWorkflow) getPlannedStorageQuery(
                    inputText, "testInsertIntoFromSelectDirect", false);
        } catch (ValidationException e) {
            fail(e.getMessage());
        } catch (IgnoreQueryException e) {
            fail(e.getMessage());
        }
        assertNotNull(storageWorkflow, "Planner failed");
        assertEquals(storageWorkflow.getExecutionType(), ExecutionType.INSERT_FROM_SELECT, "Planner failed.");
        assertNotNull(storageWorkflow.getPreviousExecutionWorkflow(), "Planner failed.");
        assertNotNull(storageWorkflow.getPreviousExecutionWorkflow().getTriggerStep(), "Planner failed.");
    }

    @Test
    public void testInsertIntoFromSelectWithTwoPhases() throws ManifestException {
        MetadataManagerTestHelper.HELPER.insertDataStore("greatDatastore", "greatCluster");

        //Create Connector
        Set<Operations> greatOperations = new HashSet<>();
        greatOperations.add(Operations.PROJECT);
        greatOperations.add(Operations.SELECT_OPERATOR);
        greatOperations.add(Operations.SELECT_FUNCTIONS);
        greatOperations.add(Operations.SELECT_WINDOW);
        greatOperations.add(Operations.SELECT_GROUP_BY);
        greatOperations.add(Operations.DELETE_PK_EQ);
        greatOperations.add(Operations.CREATE_INDEX);
        greatOperations.add(Operations.DROP_INDEX);
        greatOperations.add(Operations.UPDATE_PK_EQ);
        greatOperations.add(Operations.TRUNCATE_TABLE);
        greatOperations.add(Operations.DROP_TABLE);
        greatOperations.add(Operations.PAGINATION);
        greatOperations.add(Operations.INSERT);
        greatOperations.add(Operations.INSERT_IF_NOT_EXISTS);

        String strClusterName = "greatCluster";
        clusterWithDefaultPriority.put(new ClusterName(strClusterName), Constants.DEFAULT_PRIORITY);

        ConnectorMetadata connector3 = MetadataManagerTestHelper.HELPER.createTestConnector(
                "greatConnector",
                new DataStoreName("greatDatastore"),
                clusterWithDefaultPriority,
                greatOperations,
                "greatActorRef");

        clusterName = MetadataManagerTestHelper.HELPER.createTestCluster(
                strClusterName, new DataStoreName("greatDatastore"),
                connector3.getName());
        CatalogName catalogName = MetadataManagerTestHelper.HELPER.createTestCatalog("greatCatalog").getName();
        createTestTables(catalogName, "table4", "table5", "table6");

        // Generate query
        String inputText = "INSERT INTO greatCatalog.table4 (greatCatalog.table4.id, greatCatalog.table4.user)"
                + " SELECT * FROM greatCatalog.table5;";
        StorageWorkflow storageWorkflow = null;
        try {
            storageWorkflow = (StorageWorkflow) getPlannedStorageQuery(
                    inputText, "testInsertIntoFromSelectWithTwoPhases", false);
        } catch (ValidationException e) {
            fail(e.getMessage());
        } catch (IgnoreQueryException e) {
            fail(e.getMessage());
        }

        try {
            // DETACH CLUSTER
            ClusterMetadata clusterMetadata =
                    MetadataManager.MANAGER.getCluster(clusterName);

            Map<ConnectorName, ConnectorAttachedMetadata> connectorAttachedRefs =
                    clusterMetadata.getConnectorAttachedRefs();

            connectorAttachedRefs.remove(connector3.getName());
            clusterMetadata.setConnectorAttachedRefs(connectorAttachedRefs);

            MetadataManager.MANAGER.createCluster(clusterMetadata, false);


            ConnectorMetadata connectorMetadata = MetadataManager.MANAGER.getConnector(connector3.getName());
            connectorMetadata.getClusterRefs().remove(clusterName);
            connectorMetadata.getClusterProperties().remove(clusterName);
            connectorMetadata.getClusterPriorities().remove(clusterName);

            MetadataManager.MANAGER.createConnector(connectorMetadata, false);

            // DELETE OTHER STRUCTURES
            MetadataManager.MANAGER.deleteCluster(clusterName, false);
            MetadataManager.MANAGER.deleteConnector(connector3.getName());
            MetadataManager.MANAGER.deleteCatalog(new CatalogName("greatCatalog"), false);
        } catch (Exception e) {
            fail("Test failed: " + System.lineSeparator() + e.getMessage());
        }

        assertNotNull(storageWorkflow, "Planner failed");
        assertEquals(storageWorkflow.getExecutionType(), ExecutionType.INSERT_BATCH, "Planner failed.");
        assertNotNull(storageWorkflow.getPreviousExecutionWorkflow(), "Planner failed.");
        assertNotNull(storageWorkflow.getPreviousExecutionWorkflow().getTriggerStep(), "Planner failed.");
    }

}
