/*
 * Thrifty
 *
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED ON AN  *AS IS* BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING
 * WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE,
 * FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.microsoft.thrifty.gen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.thrifty.schema.EnumType;
import com.microsoft.thrifty.schema.Loader;
import com.microsoft.thrifty.schema.Location;
import com.microsoft.thrifty.schema.NamespaceScope;
import com.microsoft.thrifty.schema.Schema;
import com.microsoft.thrifty.schema.ThriftType;
import com.microsoft.thrifty.schema.parser.EnumElement;
import com.microsoft.thrifty.schema.parser.EnumMemberElement;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@Ignore("Used to manually inspect code generation")
public class ThriftyCodeGeneratorTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void enumGeneration() {
        Location location = Location.get("", "");
        EnumElement ee = EnumElement.builder(location)
                .documentation("A generated enum")
                .name("BuildStatus")
                .members(ImmutableList.of(
                        EnumMemberElement.builder(location)
                                .documentation("Represents a successful build")
                                .name("OK")
                                .value(0)
                                .build(),

                        EnumMemberElement.builder(location)
                                .documentation("Represents a failed build")
                                .name("FAIL")
                                .value(1)
                                .build()))
                .build();
        ImmutableMap<NamespaceScope, String> namespaces = ImmutableMap.of(NamespaceScope.JAVA, "com.test.enums");
        EnumType et = new EnumType(ee, ThriftType.enumType("BuildStatus", namespaces), namespaces);

        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(mock(Schema.class));
        TypeSpec generated = gen.buildEnum(et);

        JavaFile file = JavaFile.builder("com.test.enums", generated).build();
        String code = file.toString();

        assertThat(code, is("test"));
    }

    @Test
    public void structGeneration() throws Exception {
        String thrift = "" +
                "namespace java com.test.struct\n" +
                "\n" +
                "struct Str {\n" +
                "  1: list<i32> numbers,\n" +
                "  2: string name,\n" +
                "  3: map<string, list<i16>> addresses\n" +
                "}\n" +
                "\n" +
                "exception Boom {}";

        File f = tmp.newFile();
        write(f, thrift);

        Loader loader = new Loader();
        loader.addThriftFile(f.getAbsolutePath());

        Schema schema = loader.load();

        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(mock(Schema.class));
        TypeSpec type = gen.buildStruct(schema.structs().get(0));

        JavaFile file = JavaFile.builder("com.test.struct", type).build();
        String code = file.toString();

        assertThat(code, is("foo"));
    }

    @Test
    public void fieldInitializers() throws Exception {
        String thrift = "namespace java com.microsoft.thrifty\n" +
                "\n" +
                "enum Foo {\n" +
                "  FOO = 0,\n" +
                "  BAR = 5,\n" +
                "  BAZ = 10, \n" +
                "  QUUX = 15" +
                "}\n" +
                "\n" +
                "struct Init {\n" +
                "  1: required set<Foo> f = [10],\n" +
                "  2: optional string b = \"bar\" ,\n" +
                "}";

        File f = tmp.newFile();
        write(f, thrift);

        Loader loader = new Loader();
        loader.addThriftFile(f.getAbsolutePath());

        Schema schema = loader.load();

        ThriftyCodeGenerator gen = new ThriftyCodeGenerator(schema);
        TypeSpec spec = gen.buildStruct(schema.structs().get(0));
        JavaFile file = JavaFile.builder("com.microsoft.thrifty", spec).build();
        String code = file.toString();
        file.writeTo(System.err);
        assertThat(code, is("foo"));
    }

    private void write(File file, String text) throws IOException {
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        BufferedWriter buf = new BufferedWriter(writer);
        try {
            buf.write(text);
            buf.flush();
        } finally {
            buf.close();
            writer.close();
        }
    }
}