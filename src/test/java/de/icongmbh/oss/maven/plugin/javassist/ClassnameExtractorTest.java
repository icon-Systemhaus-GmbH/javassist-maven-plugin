package de.icongmbh.oss.maven.plugin.javassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class ClassnameExtractorTest {

    @Test
    public void testExtractClassNameFromFile() throws IOException {
        //given
        
        //when
        String className = ClassnameExtractor.extractClassNameFromFile(new File("/foo"), new File("/foo/bar/MyClass.class"));
        
        //then
        assertEquals("bar.MyClass", className);
    }

    @Test
    public void testIterateClassnames() throws IOException {
        //given
        
        //when
        Iterator<String> classNameIterator = ClassnameExtractor.iterateClassnames(new File("/foo"), new ArrayList<File>(Arrays.asList( new File[] { new File("/foo/bar/MyClass.class"), new File("/foo/bar2/MyClass2.class") })).iterator());
        
        //then
        assertNotNull(classNameIterator);
        String first = classNameIterator.next();
        assertEquals("bar.MyClass", first);
        assertTrue(classNameIterator.hasNext());
        String second = classNameIterator.next();
        assertEquals("bar2.MyClass2", second);
        assertFalse(classNameIterator.hasNext());
        classNameIterator.remove();
    }

    @Test
    public void testListClassnames() throws IOException {
        //given
        
        //when
        List<String> classNameList = ClassnameExtractor.listClassnames(new File("/foo"), Arrays.asList( new File[] { new File("/foo/bar/MyClass.class"), new File("/foo/bar2/MyClass2.class") }));
        
        //then
        assertNotNull(classNameList);
        assertEquals("bar.MyClass", classNameList.get(0));
        assertEquals("bar2.MyClass2", classNameList.get(1));
        assertEquals(2,classNameList.size());
    }

}
