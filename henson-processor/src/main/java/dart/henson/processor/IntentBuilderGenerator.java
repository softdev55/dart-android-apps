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

import static com.squareup.javapoet.ClassName.bestGuess;
import static com.squareup.javapoet.ClassName.get;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import dart.common.BaseGenerator;
import dart.common.ExtraInjection;
import dart.common.FieldBinding;
import dart.common.BindingTarget;
import dart.henson.Bundler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public class IntentBuilderGenerator extends BaseGenerator {

  static final String BUNDLE_BUILDER_SUFFIX = "__IntentBuilder";
  private static final String STATE_CLASS_INTERMEDIARY_PREFIX = "AfterSetting";
  private static final String STATE_CLASS_FINAL_STATE = "AllSet";

  private final BindingTarget target;

  public IntentBuilderGenerator(BindingTarget target) {
    this.target = target;
  }

  @Override
  public String brewJava() {
    TypeSpec.Builder intentBuilderTypeBuilder =
        TypeSpec.classBuilder(builderClassName()).addModifiers(Modifier.PUBLIC);

    emitFields(intentBuilderTypeBuilder);
    emitConstructor(intentBuilderTypeBuilder);
    emitGetClassDynamically(intentBuilderTypeBuilder);
    emitExtraDSLStateMachine(intentBuilderTypeBuilder);

    //build
    JavaFile javaFile =
        JavaFile.builder(target.classPackage, intentBuilderTypeBuilder.build())
            .addFileComment("Generated code from Henson. Do not modify!")
            .build();
    return javaFile.toString();
  }

  @Override
  public String getFqcn() {
    return target.classPackage + "." + builderClassName();
  }

  private String builderClassName() {
    return target.targetClassName + BUNDLE_BUILDER_SUFFIX;
  }

  private void emitFields(TypeSpec.Builder intentBuilderTypeBuilder) {
    FieldSpec.Builder intentFieldBuilder =
        FieldSpec.builder(get("android.content", "Intent"), "intent", Modifier.PRIVATE);
    intentBuilderTypeBuilder.addField(intentFieldBuilder.build());
    FieldSpec.Builder bundlerFieldBuilder =
        FieldSpec.builder(Bundler.class, "bundler", Modifier.PRIVATE);
    bundlerFieldBuilder.initializer("Bundler.create()");
    intentBuilderTypeBuilder.addField(bundlerFieldBuilder.build());
  }

  private void emitConstructor(TypeSpec.Builder intentBuilderTypeBuilder) {
    MethodSpec.Builder constructorBuilder =
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(get("android.content", "Context"), "context");
    constructorBuilder.addStatement(
        "intent = new Intent(context, getClassDynamically($S))",
        target.targetClassFqcn.replace("$", "."));
    intentBuilderTypeBuilder.addMethod(constructorBuilder.build());
  }

  private void emitGetClassDynamically(TypeSpec.Builder intentBuilderTypeBuilder) {
    MethodSpec.Builder getClassDynamicallyBuilder =
        MethodSpec.methodBuilder("getClassDynamically")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(get("java.lang", "String"), "className")
            .returns(get("java.lang", "Class"));
    getClassDynamicallyBuilder.beginControlFlow("try");
    getClassDynamicallyBuilder.addStatement("return Class.forName(className)");
    getClassDynamicallyBuilder.nextControlFlow("catch($T ex)", get("java.lang", "Exception"));
    getClassDynamicallyBuilder.addStatement("throw new RuntimeException(ex)");
    getClassDynamicallyBuilder.endControlFlow();
    intentBuilderTypeBuilder.addMethod(getClassDynamicallyBuilder.build());
  }

  private void emitExtraDSLStateMachine(TypeSpec.Builder intentBuilderTypeBuilder) {
    //separate required extras from optional extras and sort both sublists.
    List<ExtraInjection> requiredInjections = new ArrayList<>();
    List<ExtraInjection> optionalInjections = new ArrayList<>();
    for (ExtraInjection extraInjection : target.bindingMap.values()) {
      if (!extraInjection.getRequiredBindings().isEmpty()) {
        requiredInjections.add(extraInjection);
      } else {
        optionalInjections.add(extraInjection);
      }
    }

    final ExtraInjectionComparator extraInjectionComparator = new ExtraInjectionComparator();
    Collections.sort(requiredInjections, extraInjectionComparator);
    Collections.sort(optionalInjections, extraInjectionComparator);

    //getters and setters
    emitSetters(intentBuilderTypeBuilder, requiredInjections, false, false);

    final TypeSpec.Builder lastStateClassBuilder;
    if (!requiredInjections.isEmpty()) {
      lastStateClassBuilder = TypeSpec.classBuilder("AllSet").addModifiers(Modifier.PUBLIC);
    } else {
      lastStateClassBuilder = intentBuilderTypeBuilder;
    }

    emitSetters(lastStateClassBuilder, optionalInjections, true, requiredInjections.isEmpty());

    emitBuildMethod(lastStateClassBuilder);
    if (lastStateClassBuilder != intentBuilderTypeBuilder) {
      intentBuilderTypeBuilder.addType(lastStateClassBuilder.build());
    }
  }

  private void emitBuildMethod(TypeSpec.Builder builder) {
    MethodSpec.Builder getBuilder =
        MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(get("android.content", "Intent"))
            .addStatement("intent.putExtras(bundler.get())")
            .addStatement("return intent");
    builder.addMethod(getBuilder.build());
  }

  private void emitSetters(
      TypeSpec.Builder builder,
      List<ExtraInjection> bindingList,
      boolean isOptional,
      boolean areAllExtrasOptional) {

    //allow to rotate between states
    TypeSpec.Builder builderStateClass = builder;

    for (int indexInjection = 0; indexInjection < bindingList.size(); indexInjection++) {
      ExtraInjection binding = bindingList.get(indexInjection);
      final boolean isLastMandatorySetter = indexInjection == bindingList.size() - 1;

      String nextStateClassName =
          emitSetter(
              builderStateClass, binding, isLastMandatorySetter, isOptional, areAllExtrasOptional);

      //optional fields do not rotate
      //they all return the intent builder itself
      if (!isOptional) {
        builderStateClass = rotateBuilderStateClass(builder, builderStateClass, nextStateClassName);
      }
    }
  }

  private TypeSpec.Builder rotateBuilderStateClass(
      TypeSpec.Builder builder, TypeSpec.Builder builderStateClass, String nextStateClassName) {
    if (builderStateClass != builder) {
      builder.addType(builderStateClass.build());
    }
    //prepare next state class
    builderStateClass = TypeSpec.classBuilder(nextStateClassName).addModifiers(Modifier.PUBLIC);
    return builderStateClass;
  }

  /**
   * @param builder the intent builder in which to emit.
   * @param binding the binding to emit.
   * @param isLastMandatorySetter whether or not the binding is the last mandatory one.
   * @param isOptional whether or not it is optional
   * @param areAllInjectionsOptional whether or not all bindings are optional. i.e. the class only
   *     as optional bindings.
   * @return the name of the next state class to create
   */
  //TODO this method is too long, needs smart refactor
  private String emitSetter(
      TypeSpec.Builder builder,
      ExtraInjection binding,
      boolean isLastMandatorySetter,
      boolean isOptional,
      boolean areAllInjectionsOptional) {

    Collection<FieldBinding> fieldBindings = binding.getFieldBindings();
    if (fieldBindings.isEmpty()) {
      return null;
    }

    // We can have the same extra key bound to multiple fields. We generate builders by key name,
    // so we pick the first field binding and use it's type since they should all be the same
    // anyway.
    FieldBinding firstFieldBinding = fieldBindings.iterator().next();
    TypeMirror extraType = firstFieldBinding.getType();

    final String nextStateSimpleClassName;
    final boolean isInnerClass;

    if (isOptional) {
      if (areAllInjectionsOptional) {
        isInnerClass = false;
        nextStateSimpleClassName = builderClassName();
      } else {
        isInnerClass = true;
        nextStateSimpleClassName = "AllSet";
      }
    } else {
      isInnerClass = true;
      if (isLastMandatorySetter) {
        nextStateSimpleClassName = STATE_CLASS_FINAL_STATE;
      } else {
        nextStateSimpleClassName = STATE_CLASS_INTERMEDIARY_PREFIX + capitalize(binding.getKey());
      }
    }

    String nextStateClassName = builderClassName();
    if (isInnerClass) {
      nextStateClassName += "." + nextStateSimpleClassName;
    }

    String castToParcelableIfNecessary = doCreateParcelableCastIfExtraIsParcelable(extraType);
    final String firstInjectedFieldName = firstFieldBinding.getName();
    final String value = extractValue(firstFieldBinding);

    MethodSpec.Builder setterBuilder =
        MethodSpec.methodBuilder(binding.getKey())
            .addModifiers(Modifier.PUBLIC)
            .returns(bestGuess(nextStateClassName))
            .addParameter(TypeName.get(extraType), firstInjectedFieldName)
            .addStatement(
                "bundler.put($S," + castToParcelableIfNecessary + " $L)", binding.getKey(), value);

    if (isOptional) {
      setterBuilder.addStatement("return this");
    } else {
      setterBuilder.addStatement("return new $L()", nextStateClassName);
    }

    builder.addMethod(setterBuilder.build());
    return nextStateSimpleClassName;
  }

  /**
   * This method returns either an empty String or {@code "(Parcelable)"} if the extra type is
   * Parcelable. We need this explicit conversion in cases where the extra type is both Parcelable
   * and Serializable. In that case we will prefer Parcelable. Not that the extra type has to
   * directly implement Parcelable, not via a super class.
   *
   * @param extraType the type that might be parcelable.
   * @return either an empty String or {@code "(Parcelable)"} if the extra type is Parcelable
   */
  private String doCreateParcelableCastIfExtraIsParcelable(TypeMirror extraType) {
    String castToParcelableIfNecessary = "";
    if (extraType instanceof DeclaredType) {
      boolean isParcelable = false;
      final TypeElement typeElement = (TypeElement) ((DeclaredType) extraType).asElement();
      for (TypeMirror interfaceType : typeElement.getInterfaces()) {
        if ("android.os.Parcelable".equals(interfaceType.toString())) {
          isParcelable = true;
        }
      }

      if (isParcelable) {
        castToParcelableIfNecessary = "(android.os.Parcelable)";
      }
    }
    return castToParcelableIfNecessary;
  }

  private String extractValue(FieldBinding firstFieldBinding) {
    final String value;
    if (firstFieldBinding.isParcel()) {
      value = "org.parceler.Parcels.wrap(" + firstFieldBinding.getName() + ')';
    } else {
      value = firstFieldBinding.getName();
    }
    return value;
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
    @Override
    public int compare(ExtraInjection o1, ExtraInjection o2) {
      return o1.getKey().compareTo(o2.getKey());
    }
  }
}
