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

package com.stratio.crossdata.common.statements.structures;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FloatingPointSelectorTest {
    @Test
    public void equalsTest(){
        FloatingPointSelector as=new FloatingPointSelector("2.5");
        FloatingPointSelector as2=new FloatingPointSelector(2.5);
        Assert.assertTrue(as.equals(as2), "This selectors must be equals");
        Assert.assertTrue(as.hashCode()==as2.hashCode(),"This selector must have the same hashcode");
    }

    @Test
    public void equals2Test(){
        FloatingPointSelector as=new FloatingPointSelector("2.5");
        FloatingPointSelector as2=new FloatingPointSelector("2.5");
        Assert.assertTrue(as.equals(as2), "This selectors must be equals");
        Assert.assertTrue(as.hashCode()==as2.hashCode(),"This selector must have the same hashcode");
    }


    @Test
    public void nonEqualsTest(){
        FloatingPointSelector as=new FloatingPointSelector(2.5);
        FloatingPointSelector as2=null;
        Assert.assertFalse(as.equals(as2),"This selectors mustn't be equals");

    }
}
