/*******************************************************************************
 * Copyright 2010,2012
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.bigdata.io.hadoop;

import java.net.URI;

import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.MiniDFSCluster.Builder;
import org.junit.Test;
import org.springframework.core.io.Resource;

public class HdfsResourceLoaderTest
    extends TestCase
{
    private static final Configuration conf = new Configuration();

    private static final int DFS_REPLICATION_INTERVAL = 1;

    private static final Path TEST_ROOT_DIR_PATH =

    new Path(System.getProperty("test.build.data", "build/test/data"));

    // Number of datanodes in the cluster

    private static final int DATANODE_COUNT = 1;

    private MiniDFSCluster cluster;

    private DistributedFileSystem fs;

    private String defaultdir;

    private static Path getTestPath(String fileName)
    {
        return new Path(TEST_ROOT_DIR_PATH, fileName);
    }

    @Override
    protected void setUp()
        throws Exception
    {
        Builder builder = new MiniDFSCluster.Builder(conf);
        // , DATANODE_COUNT, true, null);
        cluster = builder.clusterId("testcluster").build();
        cluster.waitActive();

        fs = cluster.getFileSystem();

        defaultdir = "hdfs://localhost:" + cluster.getNameNodePort() + "/";
        conf.set("fs.default.dir", defaultdir);
        fs.copyFromLocalFile(new Path("src/test/resources/texts/example1.txt"),
                new Path(defaultdir));
        RemoteIterator<LocatedFileStatus> x = fs.listFiles(new Path(defaultdir), true);
        while (x.hasNext()) {
            LocatedFileStatus status = x.next();
            System.out.println(status.getPath().toString());
        }
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        cluster.shutdown();
    }

    @Test
    public void test()
        throws Exception
    {
        HdfsResourceLoader loader = new HdfsResourceLoader(conf, new URI(defaultdir));

        Resource[] result = loader.getResources(defaultdir + "*.txt");
        assertEquals(1, result.length);
    }
}
