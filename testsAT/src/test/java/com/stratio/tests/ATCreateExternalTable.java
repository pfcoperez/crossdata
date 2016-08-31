/*
 * Copyright (C) 2015 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.tests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.stratio.cucumber.testng.CucumberRunner;
import com.stratio.tests.utils.BaseTest;
import com.stratio.tests.utils.ThreadProperty;

import cucumber.api.CucumberOptions;

@CucumberOptions(features = {
		"src/test/resources/features/Catalog/CreateCassandraExternalTables.feature",
		"src/test/resources/features/Catalog/CreateMongoDBExternalTables.feature"
	})
public class ATCreateExternalTable extends BaseTest {


	public ATCreateExternalTable() {
	}

	@BeforeClass(groups = {"basic"})
	public void setUp() {
		ThreadProperty.set("Driver", "context");
		ThreadProperty.set("Connector", "external");
	}

	@AfterClass(groups = {"basic"})
	public void cleanUp() {

	}

	@Test(enabled = false, groups = {"advanced"})
	public void ATCreateExternalTable() throws Exception {
		new CucumberRunner(this.getClass()).runCukes();
	}

}
