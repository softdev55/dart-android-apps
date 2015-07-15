package com.f2prateek.dart.henson.processor;

import android.content.Context;
import android.content.Intent;
import com.f2prateek.dart.common.BaseGenerator;
import com.f2prateek.dart.common.ExtraInjection;
import com.f2prateek.dart.common.FieldBinding;
import com.f2prateek.dart.common.InjectionTarget;
import com.f2prateek.dart.henson.Bundler;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

public class IntentBuilderBuilder extends BaseGenerator {
  public static final String BUNDLE_BUILDER_SUFFIX = "$$IntentBuilder";

  public IntentBuilderBuilder(InjectionTarget target) {
    super(target);
  }

  private String builderClassName() {
    return target.className + BUNDLE_BUILDER_SUFFIX;
  }

  @Override public String brewJava() {
    TypeSpec.Builder intentBuilderTypeBuilder =
        TypeSpec.classBuilder(builderClassName()).addModifiers(Modifier.PUBLIC);

    emitFields(intentBuilderTypeBuilder);
    emitConstructor(intentBuilderTypeBuilder);
    emitGettersAndSetters(intentBuilderTypeBuilder);

    //build
    JavaFile javaFile = JavaFile.builder(target.classPackage, intentBuilderTypeBuilder.build())
        .addFileComment("Generated code from Dart. Do not modify!")
        .build();
    return javaFile.toString();
  }

  private void emitFields(TypeSpec.Builder intentBuilderTypeBuilder) {
    FieldSpec.Builder intentFieldBuilder =
        FieldSpec.builder(Intent.class, "intent", Modifier.PRIVATE);
    intentBuilderTypeBuilder.addField(intentFieldBuilder.build());
    FieldSpec.Builder bundlerFieldBuilder =
        FieldSpec.builder(Bundler.class, "bundler", Modifier.PRIVATE);
    bundlerFieldBuilder.initializer("Bundler.create()");
    intentBuilderTypeBuilder.addField(bundlerFieldBuilder.build());
  }

  private void emitConstructor(TypeSpec.Builder intentBuilderTypeBuilder) {
    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(Context.class, "context")
        .addStatement("intent = new Intent(context, $L)", target.className + ".class");
    intentBuilderTypeBuilder.addMethod(constructorBuilder.build());
  }

  private void emitGettersAndSetters(TypeSpec.Builder intentBuilderTypeBuilder) {
    //separate required extras from optional extras and sort both sublists.
    List<ExtraInjection> requiredInjections = new ArrayList<>();
    List<ExtraInjection> optionalInjections = new ArrayList<>();
    for (ExtraInjection extraInjection : target.injectionMap.values()) {
      if (!extraInjection.getRequiredBindings().isEmpty()) {
        requiredInjections.add(extraInjection);
      } else {
        optionalInjections.add(extraInjection);
      }
    }

    final ExtraInjectionComparator extraInjectionComparator = new ExtraInjectionComparator();
    requiredInjections.sort(extraInjectionComparator);
    optionalInjections.sort(extraInjectionComparator);

    //getters and setters
    emitSetters(intentBuilderTypeBuilder, requiredInjections, false, false);

    final TypeSpec.Builder lastStateClassBuilder;
    if (!requiredInjections.isEmpty()) {
      lastStateClassBuilder = TypeSpec.classBuilder("AllSet").addModifiers(Modifier.PUBLIC);
    } else {
      lastStateClassBuilder = intentBuilderTypeBuilder;
    }

    emitSetters(lastStateClassBuilder, optionalInjections, true, requiredInjections.isEmpty());

    emitGetter(lastStateClassBuilder);
    if (lastStateClassBuilder != intentBuilderTypeBuilder) {
      intentBuilderTypeBuilder.addType(lastStateClassBuilder.build());
    }
  }

  @Override public String getFqcn() {
    return target.getFqcn() + BUNDLE_BUILDER_SUFFIX;
  }

  private void emitGetter(TypeSpec.Builder builder) {
    MethodSpec.Builder getBuilder = MethodSpec.methodBuilder("get")
        .addModifiers(Modifier.PUBLIC)
        .returns(Intent.class)
        .addStatement("intent.putExtras(bundler.get())")
        .addStatement("return intent");
    builder.addMethod(getBuilder.build());
  }

  private void emitSetters(TypeSpec.Builder builder, List<ExtraInjection> injectionList,
      boolean isOptional, boolean areAllExtrasOptional) {

    // Loop over each extras injection and emit it.
    TypeSpec.Builder builderStateClass = builder;

    for (int indexInjection = 0; indexInjection < injectionList.size(); indexInjection++) {
      ExtraInjection injection = injectionList.get(indexInjection);
      String nextStateClassName =
          emitSetter(builderStateClass, injection, indexInjection == injectionList.size() - 1,
              isOptional, areAllExtrasOptional);

      if (!isOptional) {
        if (builderStateClass != builder) {
          builder.addType(builderStateClass.build());
        }
        builderStateClass = TypeSpec.classBuilder(nextStateClassName).addModifiers(Modifier.PUBLIC);
      }
    }
  }

  private String emitSetter(TypeSpec.Builder builder, ExtraInjection injection,
      boolean isLastMandatorySetter, boolean isOptional, boolean areAllExtrasOptional) {

    Collection<FieldBinding> fieldBindings = injection.getFieldBindings();
    if (fieldBindings.isEmpty()) {
      return null;
    }

    // We can have the same extra key bound to multiple fields. We generate builders by key name,
    // so we pick the first field binding and use it's type since they should all be the same
    // anyway.
    FieldBinding firstFieldBinding = fieldBindings.iterator().next();
    TypeMirror extraType = firstFieldBinding.getType();

    final String value;
    if (firstFieldBinding.isParcel()) {
      value = "org.parceler.Parcels.wrap(" + injection.getKey() + ')';
    } else {
      value = injection.getKey();
    }

    final ClassName nextStateClassName;
    final String nextStateSimpleClassName;
    final String nextStateName;

    if (isOptional) {
      if (areAllExtrasOptional) {
        nextStateName = builderClassName();
        nextStateSimpleClassName = builderClassName();
      } else {
        nextStateSimpleClassName = "AllSet";
        nextStateName = builderClassName() + "." + nextStateSimpleClassName;
      }
    } else {
      if (isLastMandatorySetter) {
        nextStateSimpleClassName = "AllSet";
      } else {
        nextStateSimpleClassName = "AfterSetting" + capitalize(injection.getKey());
      }

      nextStateName = builderClassName() + "." + nextStateSimpleClassName;
    }
    nextStateClassName = ClassName.bestGuess(nextStateName);

    MethodSpec.Builder setterBuilder = MethodSpec.methodBuilder(injection.getKey())
        .addModifiers(Modifier.PUBLIC)
        .returns(nextStateClassName)
        .addParameter(ClassName.bestGuess(getType(extraType)), injection.getKey())
        .addStatement("bundler.put($S,$L)", injection.getKey(), value);

    if (isOptional) {
      setterBuilder.addStatement("return this");
    } else {
      setterBuilder.addStatement("return new $L()", nextStateName);
    }

    builder.addMethod(setterBuilder.build());
    return nextStateSimpleClassName;
  }

  private String capitalize(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Key can't be null.");
    }

    if (key.length() == 0) {
      return "";
    }

    char firstUpper = Character.toUpperCase(key.charAt(0));
    if (key.length() == 1) {
      return Character.toString(firstUpper);
    }

    String rest = key.substring(1);
    return firstUpper + rest;
  }

  private static class ExtraInjectionComparator implements Comparator<ExtraInjection> {
    @Override public int compare(ExtraInjection o1, ExtraInjection o2) {
      return o1.getKey().compareTo(o2.getKey());
    }
  }
}
