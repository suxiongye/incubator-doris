// Modifications copyright (C) 2017, Baidu.com, Inc.
// Copyright 2017 The Apache Software Foundation

// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.baidu.palo.analysis;

import com.baidu.palo.common.AnalysisException;
import com.baidu.palo.common.InternalException;
import com.baidu.palo.mysql.privilege.MockedAuth;
import com.baidu.palo.mysql.privilege.PaloAuth;
import com.baidu.palo.qe.ConnectContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import mockit.Mocked;
import mockit.internal.startup.Startup;

public class AlterClusterStmtTest {

    private static Analyzer analyzer;

    @Mocked
    private PaloAuth auth;
    @Mocked
    private ConnectContext ctx;

    static {
        Startup.initializeIfPossible();
    }

    @Before()
    public void setUp() {
        analyzer = AccessTestUtil.fetchAdminAnalyzer(true);
        MockedAuth.mockedAuth(auth);
        MockedAuth.mockedConnectContext(ctx, "root", "192.168.1.1");
    }

    @Test
    public void testAnalyzeNormal() throws InternalException, AnalysisException {
        final Map<String, String> properties = new HashMap();
        properties.put("instance_num", "2");
        final AlterClusterStmt stmt = new AlterClusterStmt("testCluster", properties);
        stmt.analyze(analyzer);
        Assert.assertEquals("testCluster", stmt.getAlterClusterName());
        String sql = "ALTER CLUSTER " + "testCluster" + " PROPERTIES(\"instance_num\"=" + "\"" + "2" + "\")";
        Assert.assertEquals(sql, stmt.toSql());
    }

    @Test(expected = AnalysisException.class)
    public void testNoPropertiesFail() throws InternalException, AnalysisException {
        final AlterClusterStmt stmt = new AlterClusterStmt("testCluster", null);
        stmt.analyze(analyzer);
        Assert.fail("no exception");
    }

    @Test(expected = AnalysisException.class)
    public void testParamNumberFormatError() throws InternalException, AnalysisException {
        final Map<String, String> properties = new HashMap();
        properties.put("instance_num", "0xfffffff");
        final AlterClusterStmt stmt = new AlterClusterStmt("testCluster", properties);
        stmt.analyze(analyzer);
        Assert.fail("no exception");
    }

}
