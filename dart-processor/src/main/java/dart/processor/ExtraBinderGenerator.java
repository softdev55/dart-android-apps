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

package dart.processor;

import static dart.common.util.DartModelUtil.DART_MODEL_SUFFIX;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import dart.Dart;
import dart.common.BaseGenerator;
import dart.common.Binding;
import dart.common.ExtraBindingTarget;
import dart.common.ExtraInjection;
import dart.common.FieldBinding;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

/**
 * Creates Java code to bind extras into an activity or a {@link android.os.Bundle}.
 *
 * <p>{@link Dart} to use this code at runtime.
 */
public class ExtraBinderGenerator extends BaseGenerator {

  private final ExtraBindingTarget target;

  public ExtraBinderGenerator(ExtraBindingTarget target) {
    this.target = target;
  }

  @Override
  public String brewJava() {
    TypeSpec.Builder binderTypeSpec =
        TypeSpec.classBuilder(binderClassName()).addModifiers(Modifier.PUBLIC);
    emitBind(binderTypeSpec);
    JavaFile javaFile =
        JavaFile.builder(target.classPackage, binderTypeSpec.build())
            .addFileComment("Generated code from Dart. Do not modify!")
            .build();
    return javaFile.toString();
  }

  @Override
  public String getFqcn() {
    return target.classPackage + "." + binderClassName();
  }

  private String binderClassName() {
    return target.className + DART_MODEL_SUFFIX + Dart.EXTRA_BINDER_SUFFIX;
  }

  private void emitBind(TypeSpec.Builder builder) {
    MethodSpec.Builder bindBuilder =
        MethodSpec.methodBuilder("bind")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(ClassName.get(Dart.Finder.class), "finder")
            .addParameter(ClassName.bestGuess(target.getFQN() + DART_MODEL_SUFFIX), "target")
            .addParameter(ClassName.get(Object.class), "source");

    if (target.parentPackage != null) {
      // Emit a call to the superclass binder, if any.
      bindBuilder.addStatement(
          "$T.bind(finder, target, source)",
          ClassName.bestGuess(
              target.getParentFQN() + DART_MODEL_SUFFIX + Dart.EXTRA_BINDER_SUFFIX));
    }

    // Local variable in which all extras will be temporarily stored.
    bindBuilder.addStatement("Object object");

    // Loop over each extras binding and emit it.
    for (ExtraInjection binding : target.bindingMap.values()) {
      emitExtraInjection(bindBuilder, binding);
    }

    builder.addMethod(bindBuilder.build());
  }

  private void emitExtraInjection(MethodSpec.Builder builder, ExtraInjection binding) {
    builder.addStatement("object = finder.getExtra(source, $S)", binding.getKey());

    List<Binding> requiredBindings = binding.getRequiredBindings();
    if (!requiredBindings.isEmpty()) {
      builder
          .beginControlFlow("if (object == null)")
          .addStatement(
              "throw new IllegalStateException(\"Required extra with key '$L' for $L "
                  + "was not found. If this extra is optional add '@Nullable' annotation.\")",
              binding.getKey(),
              emitHumanDescription(requiredBindings))
          .endControlFlow();
      emitFieldBindings(builder, binding);
    } else {
      // an optional extra, wrap it in a check to keep original value, if any
      builder.beginControlFlow("if (object != null)");
      emitFieldBindings(builder, binding);
      builder.endControlFlow();
    }
  }

  private void emitFieldBindings(MethodSpec.Builder builder, ExtraInjection binding) {
    Collection<FieldBinding> fieldBindings = binding.getFieldBindings();
    if (fieldBindings.isEmpty()) {
      return;
    }

    for (FieldBinding fieldBinding : fieldBindings) {
      builder.addCode("target.$L = ", fieldBinding.getName());

      if (fieldBinding.isParcel()) {
        builder.addCode("org.parceler.Parcels.unwrap((android.os.Parcelable) object);\n");
      } else {
        emitCast(builder, fieldBinding.getType());
        builder.addCode("object;\n");
      }
    }
  }

  private void emitCast(MethodSpec.Builder builder, TypeMirror fieldType) {
    builder.addCode("($T) ", TypeName.get(fieldType));
  }

  //TODO add android annotations dependency to get that annotation plus others.

  /** Visible for testing */
  String emitHumanDescription(List<Binding> bindings) {
    StringBuilder builder = new StringBuilder();
    switch (bindings.size()) {
      case 1:
        builder.append(bindings.get(0).getDescription());
        break;
      case 2:
        builder
            .append(bindings.get(0).getDescription())
            .append(" and ")
            .append(bindings.get(1).getDescription());
        break;
      default:
        for (int i = 0, count = bindings.size(); i < count; i++) {
          Binding requiredField = bindings.get(i);
          if (i != 0) {
            builder.append(", ");
          }
          if (i == count - 1) {
            builder.append("and ");
          }
          builder.append(requiredField.getDescription());
        }
        break;
    }
    return builder.toString();
  }
}
