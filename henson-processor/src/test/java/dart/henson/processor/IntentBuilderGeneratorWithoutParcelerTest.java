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

package dart.henson.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.common.base.Joiner;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

/** For tests related to Parceler, but Parceler is not available. */
public class IntentBuilderGeneratorWithoutParcelerTest {

  @Test
  public void intentBuilderGenerator_should_generateCode_when_extraIsSerializableCollection() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join( //
                    "package test.navigation;",
                    "import java.util.ArrayList;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    ArrayList<String> extra;",
                    "}"));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.Test__IntentBuilder",
            Joiner.on('\n')
                .join( //
                    "package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "import java.util.ArrayList;",
                    "public class Test__IntentBuilder {",
                    "  public static RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    final ResolvedAllSet resolvedAllSet = new ResolvedAllSet(bundler, intent);",
                    "    return new RequiredSequence<>(bundler, resolvedAllSet);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> RequiredSequence<ALL_SET> getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return new RequiredSequence<>(bundler, allSetState);",
                    "  }",
                    "  public static class RequiredSequence<ALL_SET extends AllSet> extends RequiredStateSequence<ALL_SET> {",
                    "    public RequiredSequence(Bundler bundler, ALL_SET allRequiredSetState) {",
                    "      super(bundler, allRequiredSetState);",
                    "    }",
                    "    public ALL_SET extra(ArrayList<String> extra) {",
                    "      bundler.put(\"extra\", extra);",
                    "      return allRequiredSetState;",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"));

    Compilation compilation =
        javac()
            .withProcessors(ProcessorTestUtilities.hensonProcessorWithoutParceler())
            .compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_extraIsNonSerializableCollection() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join( //
                    "package test.navigation;",
                    "import java.util.List;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    List<String> extra;",
                    "}"));

    Compilation compilation =
        javac()
            .withProcessors(ProcessorTestUtilities.hensonProcessorWithoutParceler())
            .compile(source);
    assertThat(compilation)
        .hadErrorContaining(
            "The fields of class annotated with @DartModel must be primitive, Serializable or Parcelable (test.navigation.TestNavigationModel.extra).");
  }

  @Test
  public void
      intentBuilderGenerator_should_fail_when_extraIsAnnotatedWithParceler_and_parcelerIsOff() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join( //
                    "package test.navigation;",
                    "import dart.DartModel;",
                    "import org.parceler.Parcel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    Foo extra;",
                    "    @Parcel static class Foo {}",
                    "}"));

    Compilation compilation =
        javac()
            .withProcessors(ProcessorTestUtilities.hensonProcessorWithoutParceler())
            .compile(source);
    assertThat(compilation)
        .hadErrorContaining(
            "The fields of class annotated with @DartModel must be primitive, Serializable or Parcelable (test.navigation.TestNavigationModel.extra).");
  }

  @Test
  public void
      intentBuilderGenerator_should_fail_when_extraIsCollectionOfElementAnnotatedWithParceler_and_parcelerIsOff() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join( //
                    "package test.navigation;",
                    "import java.util.List;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    List<Foo> extra;",
                    "    @Parcel static class Foo {}",
                    "}"));

    Compilation compilation =
        javac()
            .withProcessors(ProcessorTestUtilities.hensonProcessorWithoutParceler())
            .compile(source);
    assertThat(compilation)
        .hadErrorContaining(
            "The fields of class annotated with @DartModel must be primitive, Serializable or Parcelable (test.navigation.TestNavigationModel.extra).");
  }

  @Test
  public void intentBuilderGenerator_should_generateCode_when_extraIsParcelable() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join( //
                    "package test.navigation;",
                    "import java.util.List;",
                    "import android.os.Parcelable;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    Foo extra;",
                    "    class Foo implements Parcelable {",
                    "        public void writeToParcel(android.os.Parcel out, int flags) {",
                    "        }",
                    "        public int describeContents() {",
                    "            return 0;",
                    "        }",
                    "    }",
                    "}"));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.Test__IntentBuilder",
            Joiner.on('\n')
                .join( //
                    "package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "public class Test__IntentBuilder {",
                    "  public static RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    final ResolvedAllSet resolvedAllSet = new ResolvedAllSet(bundler, intent);",
                    "    return new RequiredSequence<>(bundler, resolvedAllSet);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> RequiredSequence<ALL_SET> getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return new RequiredSequence<>(bundler, allSetState);",
                    "  }",
                    "  public static class RequiredSequence<ALL_SET extends AllSet> extends RequiredStateSequence<ALL_SET> {",
                    "    public RequiredSequence(Bundler bundler, ALL_SET allRequiredSetState) {",
                    "      super(bundler, allRequiredSetState);",
                    "    }",
                    "    public ALL_SET extra(TestNavigationModel.Foo extra) {",
                    "      bundler.put(\"extra\",(android.os.Parcelable) extra);",
                    "      return allRequiredSetState;",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"));

    Compilation compilation =
        javac()
            .withProcessors(ProcessorTestUtilities.hensonProcessorWithoutParceler())
            .compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void
      intentBuilderGenerator_should_generateCode_when_extraIsParcelableThatExtendsParcelable() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join( //
                    "package test.navigation;",
                    "import java.util.List;",
                    "import android.os.Parcelable;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    Foo extra;",
                    "    class FooParent implements Parcelable {",
                    "        public void writeToParcel(android.os.Parcel out, int flags) {",
                    "        }",
                    "        public int describeContents() {",
                    "            return 0;",
                    "        }",
                    "    }",
                    "    class Foo extends FooParent implements Parcelable {",
                    "        public void writeToParcel(android.os.Parcel out, int flags) {",
                    "        }",
                    "        public int describeContents() {",
                    "            return 0;",
                    "        }",
                    "    }",
                    "}"));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.Test__IntentBuilder",
            Joiner.on('\n')
                .join( //
                    "package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "public class Test__IntentBuilder {",
                    "  public static RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    final ResolvedAllSet resolvedAllSet = new ResolvedAllSet(bundler, intent);",
                    "    return new RequiredSequence<>(bundler, resolvedAllSet);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> RequiredSequence<ALL_SET> getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return new RequiredSequence<>(bundler, allSetState);",
                    "  }",
                    "  public static class RequiredSequence<ALL_SET extends AllSet> extends RequiredStateSequence<ALL_SET> {",
                    "    public RequiredSequence(Bundler bundler, ALL_SET allRequiredSetState) {",
                    "      super(bundler, allRequiredSetState);",
                    "    }",
                    "    public ALL_SET extra(TestNavigationModel.Foo extra) {",
                    "      bundler.put(\"extra\",(android.os.Parcelable) extra);",
                    "      return allRequiredSetState;",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"));

    Compilation compilation =
        javac()
            .withProcessors(ProcessorTestUtilities.hensonProcessorWithoutParceler())
            .compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }
}
