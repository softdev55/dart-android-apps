/*
 * Copyright 2013 Jake Wharton
 * Copyright 2014 Prateek Srivastava (@f2prateek)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.f2prateek.dart.processor;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static org.truth0.Truth.ASSERT;

public class InjectExtraWithParcelerTest {

  @Test public void serializableCollection() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.TestSerializableCollection", Joiner.on('\n').join( //
        "package test;", //
        "import android.app.Activity;", //
        "import com.f2prateek.dart.InjectExtra;", //
        "import java.lang.Object;", //
        "import java.lang.String;", //
        "import android.util.SparseArray;", //
        "public class TestSerializableCollection extends Activity {", //
        "  @InjectExtra(\"key\") SparseArray<String> extra;", //
        "}" //
    ));

    JavaFileObject expectedSource =
        JavaFileObjects.forSourceString("test/TestSerializableCollection$$ExtraInjector", Joiner.on('\n').join( //
            "package test;", //
            "import com.f2prateek.dart.Dart;", //
            "import java.lang.Object;", //
            "public class TestSerializableCollection$$ExtraInjector {", //
            "  public static void inject(Dart.Finder finder, TestSerializableCollection target, Object source) {", //
            "    Object object;", //
            "    object = finder.getExtra(source, \"key\");", //
            "    if (object == null) {", //
            "      throw new IllegalStateException(\"Required extra with key 'key' for field 'extra' was not found. If this extra is optional add '@Nullable' annotation.\");", //
            "    }", //
            "    target.extra = org.parceler.Parcels.unwrap((android.os.Parcelable) object);", //
            "  }", //
            "}" //
        ));

    ASSERT.about(javaSource())
        .that(source)
        .processedWith(ProcessorTestUtilities.dartProcessors())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedSource);
  }

  @Test public void nonSerializableNonParcelableCollection_withoutParceler() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.TestNonSerializableNonParcelableCollection_withoutParceler", Joiner.on('\n').join( //
        "package test;", //
        "import android.app.Activity;", //
        "import com.f2prateek.dart.InjectExtra;", //
        "import java.lang.Object;", //
        "import java.lang.String;", //
        "import java.util.List;", //
        "public class TestNonSerializableNonParcelableCollection_withoutParceler extends Activity {", //
        "  @InjectExtra(\"key\") List<String> extra;", //
        "}" //
    ));

    JavaFileObject expectedSource =
        JavaFileObjects.forSourceString("test/TestSerializableCollection$$ExtraInjector", Joiner.on('\n').join( //
            "package test;", //
        "import com.f2prateek.dart.Dart;", //
        "import java.lang.Object;", //
        "public class TestNonSerializableNonParcelableCollection_withoutParceler$$ExtraInjector {", //
        "  public static void inject(Dart.Finder finder, TestNonSerializableNonParcelableCollection_withoutParceler target, Object source) {", //
        "    Object object;", //
        "    object = finder.getExtra(source, \"key\");", //
        "    if (object == null) {", //
        "      throw new IllegalStateException(\"Required extra with key 'key' for field 'extra' was not found. If this extra is optional add '@Nullable' annotation.\");", //
        "    }", //
        "    target.extra = org.parceler.Parcels.unwrap((android.os.Parcelable) object);", //
        "  }", //
        "}"
            ));

    ASSERT.about(javaSource())
        .that(source)
        .processedWith(ProcessorTestUtilities.dartProcessors())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedSource);
  }

  @Test public void parcelAnnotatedType() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.TestParcelAnnotated", Joiner.on('\n').join( //
        "package test;", //
        "import android.app.Activity;", //
        "import com.f2prateek.dart.InjectExtra;", //
        "import java.lang.Object;", //
        "import java.lang.String;", //
        "import org.parceler.Parcel;", //
        "public class TestParcelAnnotated extends Activity {", //
        "  @InjectExtra(\"key\") Foo extra;", //
        "@Parcel static class Foo {}", //
        "}"
    ));

    JavaFileObject expectedSource =
        JavaFileObjects.forSourceString("test/TestParcelAnnotated$$ExtraInjector", Joiner.on('\n').join( //
            "package test;", //
            "import com.f2prateek.dart.Dart;", //
            "import java.lang.Object;", //
            "public class TestParcelAnnotated$$ExtraInjector {", //
            "  public static void inject(Dart.Finder finder, TestParcelAnnotated target, Object source) {", //
            "    Object object;", //
            "    object = finder.getExtra(source, \"key\");", //
            "    if (object == null) {", //
            "      throw new IllegalStateException(\"Required extra with key 'key' for field 'extra' was not found. If this extra is optional add '@Nullable' annotation.\");", //
            "    }", //
            "    target.extra = org.parceler.Parcels.unwrap((android.os.Parcelable) object);", //
            "  }", //
            "}" //
        ));

    ASSERT.about(javaSource())
        .that(source)
        .processedWith(ProcessorTestUtilities.dartProcessors())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedSource);
  }

  @Test public void collectionOfParcelAnnotatedType() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.TestCollectionParcel", Joiner.on('\n').join( //
        "package test;", //
        "import android.app.Activity;", //
        "import com.f2prateek.dart.InjectExtra;", //
        "import java.lang.Object;", //
        "import java.lang.String;", //
        "import java.util.List;", //
        "import org.parceler.Parcel;", //
        "public class TestCollectionParcel extends Activity {", //
        "  @InjectExtra(\"key\") List<Foo> extra;", //
        "@Parcel static class Foo {}", //
        "}"
    ));

    JavaFileObject expectedSource =
        JavaFileObjects.forSourceString("test/TestCollectionParcel$$ExtraInjector", Joiner.on('\n').join( //
            "package test;", //
            "import com.f2prateek.dart.Dart;", //
            "import java.lang.Object;", //
            "public class TestCollectionParcel$$ExtraInjector {", //
            "  public static void inject(Dart.Finder finder, TestCollectionParcel target, Object source) {", //
            "    Object object;", //
            "    object = finder.getExtra(source, \"key\");", //
            "    if (object == null) {", //
            "      throw new IllegalStateException(\"Required extra with key 'key' for field 'extra' was not found. If this extra is optional add '@Nullable' annotation.\");", //
            "    }", //
            "    target.extra = org.parceler.Parcels.unwrap((android.os.Parcelable) object);", //
            "  }", //
            "}" //
        ));

    ASSERT.about(javaSource())
        .that(source)
        .processedWith(ProcessorTestUtilities.dartProcessors())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedSource);
  }
}
