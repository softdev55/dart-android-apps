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

import com.google.common.base.Joiner;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

public class IntentBuilderGeneratorTest {

  @Test
  public void intentBuilderGenerator_should_generateIntentBuilder_when_navigationModelIsDefined_and_containsExtras() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    String extra;",
                    "}"
                ));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class TestNavigationModel__IntentBuilder {",
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
                    "    public ALL_SET extra(String extra) {",
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
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.TestNavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void intentBuilderGenerator_should_generateIntentBuilder_when_navigationModelIsDefined_and_doesNotContainExtras() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "}"
                ));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "public class TestNavigationModel__IntentBuilder {",
                    "  public static ResolvedAllSet getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    return new ResolvedAllSet(bundler, intent);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> ALL_SET getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return allSetState;",
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
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.TestNavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test(expected = AssertionError.class)
  public void intentBuilderGenerator_should_notGenerateIntentBuilder_when_navigationModelIsNotDefined_and_containsExtras() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.BindExtra;",
                    "public class TestNavigationModel {",
                    "    @BindExtra(\"key\") String extra;",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation).generatedSourceFile(
        "test.navigation.TestNavigationModel__IntentBuilder");
  }

  @Test
  public void intentBuilderGenerator_should_generateIntentBuilder_when_navigationModelIsDefined_and_containsOptionalExtras() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@interface Nullable {}",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    @Nullable String extra;",
                    "}"
                ));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import java.lang.String;",
                    "public class TestNavigationModel__IntentBuilder {",
                    "  public static ResolvedAllSet getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    return new ResolvedAllSet(bundler, intent);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> ALL_SET getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return allSetState;",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF extra(String extra) {",
                    "      bundler.put(\"extra\", extra);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.TestNavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void intentBuilderGenerator_should_generateIntentBuilder_when_navigationModelIsDefined_and_containsOptionalAndRequiredExtras() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@interface Nullable {}",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    String extra1;",
                    "    @Nullable String extra2;",
                    "}"
                ));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class TestNavigationModel__IntentBuilder {",
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
                    "    public ALL_SET extra1(String extra1) {",
                    "      bundler.put(\"extra1\", extra1);",
                    "      return allRequiredSetState;",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF extra2(String extra2) {",
                    "      bundler.put(\"extra2\", extra2);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.TestNavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void intentBuilderGenerator_should_useCustomKey() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "import dart.BindExtra;",
                    "@interface Nullable {}",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    @BindExtra(\"key1\") String extra1;",
                    "    @Nullable @BindExtra(\"key2\") String extra2;",
                    "}"
                ));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class TestNavigationModel__IntentBuilder {",
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
                    "    public ALL_SET key1(String extra1) {",
                    "      bundler.put(\"key1\", extra1);",
                    "      return allRequiredSetState;",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF key2(String extra2) {",
                    "      bundler.put(\"key2\", extra2);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.TestNavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void intentBuilderGenerator_should_notConsiderEmptyBindExtraAnnotations() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "import dart.BindExtra;",
                    "@interface Nullable {}",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    @BindExtra String extra1;",
                    "    @Nullable @BindExtra String extra2;",
                    "}"
                ));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class TestNavigationModel__IntentBuilder {",
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
                    "    public ALL_SET extra1(String extra1) {",
                    "      bundler.put(\"extra1\", extra1);",
                    "      return allRequiredSetState;",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF extra2(String extra2) {",
                    "      bundler.put(\"extra2\", extra2);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.TestNavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void intentBuilderGenerator_should_generateOnlyOneSetter_when_thereAreRepeatedKeys() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.BindExtra;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    @BindExtra(\"key\") String extra1;",
                    "    @BindExtra(\"key\") String extra2;",
                    "    @BindExtra(\"key\") String extra3;",
                    "}"
                ));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class TestNavigationModel__IntentBuilder {",
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
                    "    public ALL_SET key(String extra1) {",
                    "      bundler.put(\"key\", extra1);",
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
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.TestNavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_navigationModelIsPrivate() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.OuterClass",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "public class OuterClass {",
                    "  @DartModel",
                    "  private class TestNavigationModel {",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation).hadErrorContaining(
        "@DartModel class TestNavigationModel must not be private, static or abstract.");
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_navigationModelIsStatic() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.OuterClass",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "public class OuterClass {",
                    "  @DartModel",
                    "  static class TestNavigationModel {",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation).hadErrorContaining(
        "@DartModel class TestNavigationModel must not be private, static or abstract.");
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_navigationModelIsAbstract() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public abstract class TestNavigationModel {",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation).hadErrorContaining(
        "@DartModel class TestNavigationModel must not be private, static or abstract.");
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_navigationModelIsNotAClass() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public interface TestNavigationModel {",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation).hadErrorContaining(
        "@DartModel class TestNavigationModel must not be private, static or abstract.");
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_navigationModelIsInnerClass() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.OuterClass",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "public class OuterClass {",
                    "  @DartModel",
                    "  class TestNavigationModel {",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation).hadErrorContaining(
        "@DartModel class TestNavigationModel must be a top level class.");
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_navigationModelSuffixIsWrong() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "class TestModel {",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation).hadErrorContaining(
        "@DartModel class TestModel does not follow the naming convention: my.package.TargetComponentNavigationModel.");
  }

  @Test
  public void intentBuilderGenerator_should_notTakeIntoAccountStaticFinalExtras() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "import dart.BindExtra;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    private static final String MY_KEY = \"key\";",
                    "    @BindExtra(MY_KEY) String extra;",
                    "}"
                ));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class TestNavigationModel__IntentBuilder {",
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
                    "    public ALL_SET key(String extra) {",
                    "      bundler.put(\"key\", extra);",
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
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.TestNavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_extraIsPrivate() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    private String extra;",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation).hadErrorContaining(
        "@DartModel fields must not be private or static. (test.navigation.TestNavigationModel.extra)");
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_extraIsStatic() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    static String extra;",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation).hadErrorContaining(
        "@DartModel fields must not be private or static. (test.navigation.TestNavigationModel.extra)");
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_extraIsInvalidType() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    Object extra;",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .hadErrorContaining(
            "@DartModel fields must be a primitive or Serializable or Parcelable (test.navigation.TestNavigationModel.extra). If you use Parceler, all types supported by Parceler are allowed.");
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_extraKeyIsInvalid() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "import dart.BindExtra;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    @BindExtra(\"my.key\") String extra;",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation).hadErrorContaining(
        "@BindExtra element keys have to be valid java variable identifiers (test.navigation.TestNavigationModel, extra).");
  }

  @Test
  public void intentBuilderGenerator_should_acceptPrimitiveExtras() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    boolean aBool;",
                    "    byte aByte;",
                    "    short aShort;",
                    "    int anInt;",
                    "    long aLong;",
                    "    char aChar;",
                    "    float aFloat;",
                    "    double aDouble;",
                    "}"
                ));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "public class TestNavigationModel__IntentBuilder {",
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
                    "    public AfterSettingABool aBool(boolean aBool) {",
                    "      bundler.put(\"aBool\", aBool);",
                    "      return new AfterSettingABool();",
                    "    }",
                    "    public class AfterSettingABool {",
                    "      public AfterSettingAByte aByte(byte aByte) {",
                    "        bundler.put(\"aByte\", aByte);",
                    "        return new AfterSettingAByte();",
                    "      }",
                    "    }",
                    "    public class AfterSettingAByte {",
                    "      public AfterSettingAChar aChar(char aChar) {",
                    "        bundler.put(\"aChar\", aChar);",
                    "        return new AfterSettingAChar();",
                    "      }",
                    "    }",
                    "    public class AfterSettingAChar {",
                    "      public AfterSettingADouble aDouble(double aDouble) {",
                    "        bundler.put(\"aDouble\", aDouble);",
                    "        return new AfterSettingADouble();",
                    "      }",
                    "    }",
                    "    public class AfterSettingADouble {",
                    "      public AfterSettingAFloat aFloat(float aFloat) {",
                    "        bundler.put(\"aFloat\", aFloat);",
                    "        return new AfterSettingAFloat();",
                    "      }",
                    "    }",
                    "    public class AfterSettingAFloat {",
                    "      public AfterSettingALong aLong(long aLong) {",
                    "        bundler.put(\"aLong\", aLong);",
                    "        return new AfterSettingALong();",
                    "      }",
                    "    }",
                    "    public class AfterSettingALong {",
                    "      public AfterSettingAShort aShort(short aShort) {",
                    "        bundler.put(\"aShort\", aShort);",
                    "        return new AfterSettingAShort();",
                    "      }",
                    "    }",
                    "    public class AfterSettingAShort {",
                    "      public ALL_SET anInt(int anInt) {",
                    "        bundler.put(\"anInt\", anInt);",
                    "        return allRequiredSetState;",
                    "      }",
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
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.TestNavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void intentBuilderGenerator_should_useParcelable_when_extraIsSerializableAndParcelableExtra() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import android.os.Parcelable;",
                    "import java.io.Serializable;",
                    "import dart.DartModel;",
                    "class Extra implements Serializable, Parcelable {",
                    "  public void writeToParcel(android.os.Parcel out, int flags) {",
                    "  }",
                    "  public int describeContents() {",
                    "    return 0;",
                    "  }",
                    "}",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    Extra extra;",
                    "}"));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "public class TestNavigationModel__IntentBuilder {",
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
                    "    public ALL_SET extra(Extra extra) {",
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
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.TestNavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void intentBuilderGenerator_should_keepExtraGenerics() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import java.util.ArrayList;", //
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel {",
                    "    ArrayList<String> list;",
                    "}"
                ));

    JavaFileObject builderSource =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "import java.util.ArrayList;",
                    "public class TestNavigationModel__IntentBuilder {",
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
                    "    public ALL_SET list(ArrayList<String> list) {",
                    "      bundler.put(\"list\", list);",
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
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessorWithoutParceler())
            .compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.TestNavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource);
  }

  @Test
  public void intentBuilderGenerator_should_generateRightIntentBuilders_when_childHasRequiredAndOptionals_and_parentHasRequiredAndOptionals() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@interface Nullable {}",
                    "@DartModel",
                    "public class Test1NavigationModel extends Test2NavigationModel {",
                    "    String extra1;",
                    "    @Nullable String optExtra1;",
                    "}",
                    "@DartModel",
                    "class Test2NavigationModel {",
                    "    String extra2;",
                    "    @Nullable String optExtra2;",
                    "}"
                ));

    JavaFileObject builderSource1 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class Test1NavigationModel__IntentBuilder {",
                    "  public static RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test1\"));",
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
                    "    public Test2NavigationModel__IntentBuilder.RequiredSequence<ALL_SET> extra1(String extra1) {",
                    "      bundler.put(\"extra1\", extra1);",
                    "      return Test2NavigationModel__IntentBuilder.getInitialState(bundler, allRequiredSetState);",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends Test2NavigationModel__IntentBuilder.AllSet<SELF> {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra1(String optExtra1) {",
                    "      bundler.put(\"optExtra1\", optExtra1);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    JavaFileObject builderSource2 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test2NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class Test2NavigationModel__IntentBuilder {",
                    "  public static RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test2\"));",
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
                    "    public ALL_SET extra2(String extra2) {",
                    "      bundler.put(\"extra2\", extra2);",
                    "      return allRequiredSetState;",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra2(String optExtra2) {",
                    "      bundler.put(\"optExtra2\", optExtra2);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test1NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource1);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test2NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource2);
  }

  @Test
  public void intentBuilderGenerator_should_generateRightIntentBuilders_when_childHasRequiredAndOptionals_and_parentHasOptionals() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@interface Nullable {}",
                    "@DartModel",
                    "public class Test1NavigationModel extends Test2NavigationModel {",
                    "    String extra1;",
                    "    @Nullable String optExtra1;",
                    "}",
                    "@DartModel",
                    "class Test2NavigationModel {",
                    "    @Nullable String optExtra2;",
                    "}"
                ));

    JavaFileObject builderSource1 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class Test1NavigationModel__IntentBuilder {",
                    "  public static RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test1\"));",
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
                    "    public ALL_SET extra1(String extra1) {",
                    "      bundler.put(\"extra1\", extra1);",
                    "      return Test2NavigationModel__IntentBuilder.getInitialState(bundler, allRequiredSetState);",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends Test2NavigationModel__IntentBuilder.AllSet<SELF> {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra1(String optExtra1) {",
                    "      bundler.put(\"optExtra1\", optExtra1);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    JavaFileObject builderSource2 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test2NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import java.lang.String;",
                    "public class Test2NavigationModel__IntentBuilder {",
                    "  public static ResolvedAllSet getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test2\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    return new ResolvedAllSet(bundler, intent);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> ALL_SET getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return allSetState;",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra2(String optExtra2) {",
                    "      bundler.put(\"optExtra2\", optExtra2);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test1NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource1);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test2NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource2);
  }

  @Test
  public void intentBuilderGenerator_should_generateRightIntentBuilders_when_childHasOptionals_and_parentHasRequiredAndOptionals() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@interface Nullable {}",
                    "@DartModel",
                    "public class Test1NavigationModel extends Test2NavigationModel {",
                    "    @Nullable String optExtra1;",
                    "}",
                    "@DartModel",
                    "class Test2NavigationModel {",
                    "    String extra2;",
                    "    @Nullable String optExtra2;",
                    "}"
                ));

    JavaFileObject builderSource1 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.Bundler;",
                    "import java.lang.String;",
                    "public class Test1NavigationModel__IntentBuilder {",
                    "  public static Test2NavigationModel__IntentBuilder.RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test1\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    final ResolvedAllSet resolvedAllSet = new ResolvedAllSet(bundler, intent);",
                    "    return Test2NavigationModel__IntentBuilder.getInitialState(bundler, resolvedAllSet);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> Test2NavigationModel__IntentBuilder.RequiredSequence<ALL_SET> getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return Test2NavigationModel__IntentBuilder.getInitialState(bundler, allSetState);",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends Test2NavigationModel__IntentBuilder.AllSet<SELF> {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra1(String optExtra1) {",
                    "      bundler.put(\"optExtra1\", optExtra1);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    JavaFileObject builderSource2 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test2NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class Test2NavigationModel__IntentBuilder {",
                    "  public static RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test2\"));",
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
                    "    public ALL_SET extra2(String extra2) {",
                    "      bundler.put(\"extra2\", extra2);",
                    "      return allRequiredSetState;",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra2(String optExtra2) {",
                    "      bundler.put(\"optExtra2\", optExtra2);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test1NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource1);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test2NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource2);
  }

  @Test
  public void intentBuilderGenerator_should_generateRightIntentBuilders_when_childHasOptionals_and_parentHasOptionals() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@interface Nullable {}",
                    "@DartModel",
                    "public class Test1NavigationModel extends Test2NavigationModel {",
                    "    @Nullable String optExtra1;",
                    "}",
                    "@DartModel",
                    "class Test2NavigationModel {",
                    "    @Nullable String optExtra2;",
                    "}"
                ));

    JavaFileObject builderSource1 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.Bundler;",
                    "import java.lang.String;",
                    "public class Test1NavigationModel__IntentBuilder {",
                    "  public static ResolvedAllSet getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test1\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    return new ResolvedAllSet(bundler, intent);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> ALL_SET getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return Test2NavigationModel__IntentBuilder.getInitialState(bundler, allSetState);",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends Test2NavigationModel__IntentBuilder.AllSet<SELF> {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra1(String optExtra1) {",
                    "      bundler.put(\"optExtra1\", optExtra1);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    JavaFileObject builderSource2 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test2NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import java.lang.String;",
                    "public class Test2NavigationModel__IntentBuilder {",
                    "  public static ResolvedAllSet getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test2\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    return new ResolvedAllSet(bundler, intent);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> ALL_SET getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return allSetState;",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra2(String optExtra2) {",
                    "      bundler.put(\"optExtra2\", optExtra2);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test1NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource1);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test2NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource2);
  }

  @Test
  public void intentBuilderGenerator_should_fail_when_parentIsNotANavigationModel() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.TestNavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@DartModel",
                    "public class TestNavigationModel extends SuperClass {",
                    "    String extra;",
                    "}",
                    "class SuperClass {",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation).hadErrorContaining(
        "@DartModel test.navigation.TestNavigationModel parent does not have an IntentBuilder. Is test.navigation.SuperClass a @DartModel?");
  }

  @Test
  public void intentBuilderGenerator_should_generateRightIntentBuilders_when_childHasOptionals_and_parentHasNoExtras_and_grandParentHasRequiredAndOptionals() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@interface Nullable {}",
                    "@DartModel",
                    "public class Test1NavigationModel extends Test2NavigationModel {",
                    "    @Nullable String optExtra1;",
                    "}",
                    "@DartModel",
                    "class Test2NavigationModel extends Test3NavigationModel {",
                    "}",
                    "@DartModel",
                    "class Test3NavigationModel {",
                    "    String extra3;",
                    "    @Nullable String optExtra3;",
                    "}"
                ));

    JavaFileObject builderSource1 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.Bundler;",
                    "import java.lang.String;",
                    "public class Test1NavigationModel__IntentBuilder {",
                    "  public static Test3NavigationModel__IntentBuilder.RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test1\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    final ResolvedAllSet resolvedAllSet = new ResolvedAllSet(bundler, intent);",
                    "    return Test2NavigationModel__IntentBuilder.getInitialState(bundler, resolvedAllSet);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> Test3NavigationModel__IntentBuilder.RequiredSequence<ALL_SET> getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return Test2NavigationModel__IntentBuilder.getInitialState(bundler, allSetState);",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends Test2NavigationModel__IntentBuilder.AllSet<SELF> {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra1(String optExtra1) {",
                    "      bundler.put(\"optExtra1\", optExtra1);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    JavaFileObject builderSource2 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test2NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.Bundler;",
                    "public class Test2NavigationModel__IntentBuilder {",
                    "  public static Test3NavigationModel__IntentBuilder.RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test2\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    final ResolvedAllSet resolvedAllSet = new ResolvedAllSet(bundler, intent);",
                    "    return Test3NavigationModel__IntentBuilder.getInitialState(bundler, resolvedAllSet);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> Test3NavigationModel__IntentBuilder.RequiredSequence<ALL_SET> getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return Test3NavigationModel__IntentBuilder.getInitialState(bundler, allSetState);",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends Test3NavigationModel__IntentBuilder.AllSet<SELF> {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    JavaFileObject builderSource3 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test3NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class Test3NavigationModel__IntentBuilder {",
                    "  public static RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test3\"));",
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
                    "    public ALL_SET extra3(String extra3) {",
                    "      bundler.put(\"extra3\", extra3);",
                    "      return allRequiredSetState;",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra3(String optExtra3) {",
                    "      bundler.put(\"optExtra3\", optExtra3);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test1NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource1);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test2NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource2);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test3NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource3);
  }

  @Test
  public void intentBuilderGenerator_should_generateRightIntentBuilders_when_childHasRequiredAndOptionals_and_parentHasNoExtras_and_grandParentHasRequiredAndOptionals() {
    JavaFileObject source =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import dart.DartModel;",
                    "@interface Nullable {}",
                    "@DartModel",
                    "public class Test1NavigationModel extends Test2NavigationModel {",
                    "    String extra1;",
                    "    @Nullable String optExtra1;",
                    "}",
                    "@DartModel",
                    "class Test2NavigationModel extends Test3NavigationModel {",
                    "}",
                    "@DartModel",
                    "class Test3NavigationModel {",
                    "    String extra3;",
                    "    @Nullable String optExtra3;",
                    "}"
                ));

    JavaFileObject builderSource1 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test1NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class Test1NavigationModel__IntentBuilder {",
                    "  public static RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test1\"));",
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
                    "    public Test3NavigationModel__IntentBuilder.RequiredSequence<ALL_SET> extra1(String extra1) {",
                    "      bundler.put(\"extra1\", extra1);",
                    "      return Test2NavigationModel__IntentBuilder.getInitialState(bundler, allRequiredSetState);",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends Test2NavigationModel__IntentBuilder.AllSet<SELF> {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra1(String optExtra1) {",
                    "      bundler.put(\"optExtra1\", optExtra1);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    JavaFileObject builderSource2 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test2NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.Bundler;",
                    "public class Test2NavigationModel__IntentBuilder {",
                    "  public static Test3NavigationModel__IntentBuilder.RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test2\"));",
                    "    final Bundler bundler = Bundler.create();",
                    "    final ResolvedAllSet resolvedAllSet = new ResolvedAllSet(bundler, intent);",
                    "    return Test3NavigationModel__IntentBuilder.getInitialState(bundler, resolvedAllSet);",
                    "  }",
                    "  public static <ALL_SET extends AllSet> Test3NavigationModel__IntentBuilder.RequiredSequence<ALL_SET> getInitialState(Bundler bundler,",
                    "      ALL_SET allSetState) {",
                    "    return Test3NavigationModel__IntentBuilder.getInitialState(bundler, allSetState);",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends Test3NavigationModel__IntentBuilder.AllSet<SELF> {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    JavaFileObject builderSource3 =
        JavaFileObjects.forSourceString(
            "test.navigation.Test3NavigationModel__IntentBuilder",
            Joiner.on('\n')
                .join("package test.navigation;",
                    "import static dart.henson.ActivityClassFinder.getClassDynamically;",
                    "import android.content.Context;",
                    "import android.content.Intent;",
                    "import dart.henson.AllRequiredSetState;",
                    "import dart.henson.Bundler;",
                    "import dart.henson.RequiredStateSequence;",
                    "import java.lang.String;",
                    "public class Test3NavigationModel__IntentBuilder {",
                    "  public static RequiredSequence<ResolvedAllSet> getInitialState(Context context) {",
                    "    final Intent intent = new Intent(context, getClassDynamically(\"test.navigation.Test3\"));",
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
                    "    public ALL_SET extra3(String extra3) {",
                    "      bundler.put(\"extra3\", extra3);",
                    "      return allRequiredSetState;",
                    "    }",
                    "  }",
                    "  public static class AllSet<SELF extends AllSet<SELF>> extends AllRequiredSetState {",
                    "    public AllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "    public SELF optExtra3(String optExtra3) {",
                    "      bundler.put(\"optExtra3\", optExtra3);",
                    "      return (SELF) this;",
                    "    }",
                    "  }",
                    "  public static class ResolvedAllSet extends AllSet<ResolvedAllSet> {",
                    "    public ResolvedAllSet(Bundler bundler, Intent intent) {",
                    "      super(bundler, intent);",
                    "    }",
                    "  }",
                    "}"
                ));

    Compilation compilation =
        javac().withProcessors(ProcessorTestUtilities.hensonProcessors()).compile(source);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test1NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource1);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test2NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource2);
    assertThat(compilation)
        .generatedSourceFile("test.navigation.Test3NavigationModel__IntentBuilder")
        .hasSourceEquivalentTo(builderSource3);
  }
}
