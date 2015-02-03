/*
 *
 * JAQPOT Quattro
 *
 * JAQPOT Quattro and the components shipped with it (web applications and beans)
 * are licenced by GPL v3 as specified hereafter. Additional components may ship
 * with some other licence as will be specified therein.
 *
 * Copyright (C) 2014-2015 KinkyDesign (Charalambos Chomenides, Pantelis Sopasakis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Source code:
 * The source code of JAQPOT Quattro is available on github at:
 * https://github.com/KinkyDesign/JaqpotQuattro
 * All source files of JAQPOT Quattro that are stored on github are licenced
 * with the aforementioned licence. 
 */
package org.jaqpot.core.model.builder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StringBufferInputStream;
import java.io.StringWriter;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.jaqpot.core.model.MetaInfo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author chung
 */
public class MetaInfoBuilderTest {

    public MetaInfoBuilderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testConstructMetainfo() {
        MetaInfoBuilder builder = MetaInfoBuilder.builder();
        builder.addTitles("title1", "title2").
                addComments("comment1", "comment2").
                addSeeAlso("see1", "see2", "see3");
        MetaInfo meta = builder.build();
        assertTrue(meta.getTitles().contains("title1"));
        assertTrue(meta.getTitles().contains("title2"));
        assertTrue(meta.getComments().contains("comment1"));
        assertTrue(meta.getComments().contains("comment2"));
        assertTrue(meta.getSeeAlso().contains("see1"));
        assertTrue(meta.getSeeAlso().contains("see2"));
        assertTrue(meta.getSeeAlso().contains("see3"));
        assertEquals(3, meta.getSeeAlso().size());
        assertNull(meta.getRights());
        assertNull(meta.getDate());
    }

    @Test
    public void testConstructMetainfoAddNull() {
        MetaInfoBuilder builder = MetaInfoBuilder.builder();
        MetaInfo meta = builder.addTitles((String[]) null).build();
        assertNull(meta.getTitles());
        assertNull(meta.getComments());
    }

    @Test
    public void testJacksonSerialization() throws IOException {
        /*
         * This is to make sure that our (simple) Jackson-related
         * annotations in MetaInfo work fine!
         */
        MetaInfoBuilder builder = MetaInfoBuilder.builder();
        MetaInfo meta = builder.
                addTitles("title1", "title2").
                addComments("comment1", "comment2", "comment3").
                addCreators("creator1").
                addSubjects((String[]) null).build();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT); // optional

        PipedInputStream in = new PipedInputStream(1024);
        PipedOutputStream out = new PipedOutputStream(in);

        mapper.writeValue(out, meta);

        MetaInfo recovered = mapper.readValue(in, MetaInfo.class);

        assertNotNull(recovered.getTitles());
        assertNotNull(recovered.getComments());
        assertNotNull(recovered.getCreators());
        assertNull(recovered.getSubjects());
        assertEquals(recovered.getTitles(), meta.getTitles());
        assertEquals(recovered.getCreators(), meta.getCreators());
        assertEquals(recovered.getComments(), meta.getComments());
        assertEquals(recovered.getDate(), meta.getDate());
    }
}