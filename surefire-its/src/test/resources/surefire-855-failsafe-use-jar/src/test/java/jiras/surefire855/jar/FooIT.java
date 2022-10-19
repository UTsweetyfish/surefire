package jiras.surefire855.jar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.contentOf;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;

public final class FooIT
{
    private static final String ARTIFACT_FILE_NAME = "jiras-surefire-855-jar-1.0.jar";

    private static final String MAIN_RESOURCE = "/main/surefire855.properties";

    private static final String TEST_RESOURCE = "/jiras/surefire855/jar/properties/surefire855.properties";

    private static File surefireDir()
        throws IOException
    {
        String bootPath = System.getProperty( "surefire.real.class.path" );
        return bootPath == null ? null : new File( bootPath ).getParentFile();
    }

    private static File[] surefireProviderProperties()
        throws IOException
    {
        return surefireDir().listFiles( new FileFilter()
        {
            public boolean accept( File pathname )
            {
                try
                {
                    return isSurefireProviderProperties( pathname );
                }
                catch ( IOException e )
                {
                    return false;
                }
            }
        } );
    }

    /**
     * See BooterSerializer#serialize().
     */
    private static boolean isSurefireProviderProperties( File pathname )
        throws IOException
    {
        pathname = pathname.getCanonicalFile();
        String fileName = pathname.getName();
        return pathname.isFile() && fileName.startsWith( "surefire" ) && !fileName.startsWith( "surefire_" )
            && fileName.endsWith( "tmp" );
    }

    private static boolean isSurefireBooter( File pathname )
        throws IOException
    {
        pathname = pathname.getCanonicalFile();
        String fileName = pathname.getName();
        return pathname.isFile() && fileName.startsWith( "surefirebooter" ) && fileName.endsWith( ".jar" );
    }

    private static String manifestClassPath( Class clazz )
        throws IOException
    {
        File booter = new File( System.getProperty( "surefire.real.class.path" ) );
        assertThat( booter ).exists();
        assertThat( booter ).isFile();
        JarFile jarFile = new JarFile( booter );
        try
        {
            Manifest manifest = jarFile.getManifest();
            return manifest.getMainAttributes().getValue( "Class-Path" );
        }
        finally
        {
            jarFile.close();
        }
    }

    private static Properties loadProperties( Class clazz, String resourcePath )
        throws IOException
    {
        InputStream is = clazz.getResourceAsStream( resourcePath );
        Properties prop = new Properties();
        prop.load( is );
        is.close();
        return prop;
    }

    private static Properties loadMainProperties( Class clazz )
        throws IOException
    {
        return loadProperties( clazz, MAIN_RESOURCE );
    }

    private static Properties loadTestProperties( Class clazz )
        throws IOException
    {
        return loadProperties( clazz, TEST_RESOURCE );
    }

    @Test
    public void shouldBeJarWithForking()
        throws IOException
    {
        assumeThat( System.getProperty( "forkMode" ), is( not( "never" ) ) );

        String classPath = manifestClassPath( getClass() );
        System.out.println( "CLASS PATH:" );
        System.out.println( classPath );

        assertThat( classPath, not( containsString( "/target/classes" ) ) );
        assertThat( classPath, containsString( "/target/" + ARTIFACT_FILE_NAME ) );

        File[] descriptors = surefireProviderProperties();
        assertThat( descriptors ).hasSize( 1 );
        assertThat( descriptors ).doesNotContainNull();
        assertThat( descriptors[0] ).isFile();

        String surefireProperties = contentOf( descriptors[0] );
        Properties properties = new Properties();
        properties.load( new StringReader( surefireProperties ) );
        System.out.println( properties.toString() );
        File actualArtifact = new File( properties.getProperty( "classPathUrl.1" ) ).getCanonicalFile();
        File expectedArtifact = new File( "target/" + ARTIFACT_FILE_NAME ).getCanonicalFile();
        assertThat( actualArtifact ).isFile();
        assertThat( expectedArtifact ).isFile();
        assertThat( actualArtifact ).isEqualTo( expectedArtifact );
    }

    @Test
    public void jarShouldExistWhenNotForking()
        throws Exception
    {
        assumeThat( System.getProperty( "forkMode" ), is( "never" ) );

        assertThat( surefireDir() ).isNull();
        assertThat( new File( "target/" + ARTIFACT_FILE_NAME ).getCanonicalFile() ).isFile();
    }

    @Test
    public void shouldAlwaysHaveResources()
        throws IOException
    {
        assertThat( loadTestProperties( getClass() ).getProperty( "issue" ), is( "SUREFIRE-855" ) );
        assertThat( loadMainProperties( getClass() ).getProperty( "issue" ), is( "SUREFIRE-855" ) );
    }
}
