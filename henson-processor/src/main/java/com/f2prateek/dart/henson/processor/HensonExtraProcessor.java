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

package com.f2prateek.dart.henson.processor;

import com.f2prateek.dart.HensonNavigable;
import com.f2prateek.dart.InjectExtra;
import com.f2prateek.dart.common.AbstractDartProcessor;
import com.f2prateek.dart.common.InjectionTarget;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

import static javax.lang.model.element.ElementKind.PACKAGE;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * Henson annotation processor.
 * It will process all annotations : {@link InjectExtra} and
 * invoke {@link IntentBuilderGenerator} and {@link HensonNavigatorGenerator}.
 * It supports the annotation processor option {@code #OPTION_HENSON_PACKAGE}
 * that lets you determine in which package the generated {@Code Henson} navigator class
 * will be generated.
 * If this option is not present, then the annotation processor tries to find a common
 * package between all classes that contain the {@link InjectExtra} annotation.
 *
 * @see HensonNavigatorGenerator#findCommonPackage(java.util.Collection)
 */
public final class HensonExtraProcessor extends AbstractDartProcessor {

  public static final String OPTION_HENSON_PACKAGE = "dart.henson.package";

  private String hensonPackage;

  @Override public synchronized void init(ProcessingEnvironment env) {
    super.init(env);
    hensonPackage = env.getOptions().get(OPTION_HENSON_PACKAGE);
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    Set<String> supportTypes = new LinkedHashSet<>(super.getSupportedAnnotationTypes());
    supportTypes.add(HensonNavigable.class.getCanonicalName());
    return supportTypes;
  }

  @Override public Set<String> getSupportedOptions() {
    Set<String> supportedOptions = new LinkedHashSet<>();
    supportedOptions.addAll(super.getSupportedOptions());
    supportedOptions.add(OPTION_HENSON_PACKAGE);
    return supportedOptions;
  }

  @Override public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
    Map<TypeElement, InjectionTarget> targetClassMap = findAndParseTargets(env);

    for (Map.Entry<TypeElement, InjectionTarget> entry : targetClassMap.entrySet()) {
      TypeElement typeElement = entry.getKey();
      InjectionTarget injectionTarget = entry.getValue();

      //we unfortunately can't test that nothing is generated in a TRUTH based test
      if (!injectionTarget.isAbstractTargetClass) {
        enhanceInjectionTargetWithInheritedInjectionExtras(targetClassMap, injectionTarget);

        // Now write the IntentBuilder
        Writer writer = null;
        try {
          IntentBuilderGenerator intentBuilderGenerator =
              new IntentBuilderGenerator(injectionTarget);
          JavaFileObject jfo =
              filer.createSourceFile(intentBuilderGenerator.getFqcn(), typeElement);
          writer = jfo.openWriter();
          if (isDebugEnabled) {
            System.out.println(
                "IntentBuilder generated:\n" + intentBuilderGenerator.brewJava() + "---");
          }
          writer.write(intentBuilderGenerator.brewJava());
        } catch (IOException e) {
          error(typeElement, "Unable to write intent builder for type %s: %s", typeElement,
              e.getMessage());
        } finally {
          if (writer != null) {
            try {
              writer.close();
            } catch (IOException e) {
              error(typeElement, "Unable to close intent builder source file for type %s: %s",
                  typeElement, e.getMessage());
            }
          }
        }
      }
    }

    // Generate the Henson navigator
    Writer writer = null;

    if (!targetClassMap.values().isEmpty()) {
      Element[] allTypes = targetClassMap.keySet().toArray(new Element[targetClassMap.size()]);
      try {
        HensonNavigatorGenerator hensonNavigatorGenerator =
            new HensonNavigatorGenerator(hensonPackage, targetClassMap.values());
        JavaFileObject jfo = filer.createSourceFile(hensonNavigatorGenerator.getFqcn(), allTypes);
        writer = jfo.openWriter();
        if (isDebugEnabled) {
          System.out.println(
              "Henson navigator generated:\n" + hensonNavigatorGenerator.brewJava() + "---");
        }
        writer.write(hensonNavigatorGenerator.brewJava());
      } catch (IOException e) {
        e.printStackTrace();
        for (Element element : allTypes) {
          error(element, "Unable to write henson navigator for types %s: %s", element,
              e.getMessage());
        }
      } finally {
        if (writer != null) {
          try {
            writer.close();
          } catch (IOException e) {
            e.printStackTrace();
            for (Element element : allTypes) {
              error(element, "Unable to close intent builder source file for type %s: %s", element,
                  e.getMessage());
            }
          }
        }
      }
    }

    //return false here to let dart process the annotations too
    return false;
  }

  @Override protected Map<TypeElement, InjectionTarget> findAndParseTargets(RoundEnvironment env) {
    Map<TypeElement, InjectionTarget> targetClassMap =
        new LinkedHashMap<>();
    Set<TypeMirror> erasedTargetTypes = new LinkedHashSet<>();

    // Process each @InjectExtra elements.
    parseInjectExtraAnnotatedElements(env, targetClassMap, erasedTargetTypes);
    parseHensonNavigableAnnotatedElements(env, targetClassMap, erasedTargetTypes);

    // Try to find a parent injector for each injector.
    for (Map.Entry<TypeElement, InjectionTarget> entry : targetClassMap.entrySet()) {
      String parentClassFqcn = findParentFqcn(entry.getKey(), erasedTargetTypes);
      if (parentClassFqcn != null) {
        entry.getValue().setParentTarget(parentClassFqcn);
      }
    }

    return targetClassMap;
  }

  private void parseHensonNavigableAnnotatedElements(RoundEnvironment env,
      Map<TypeElement, InjectionTarget> targetClassMap, Set<TypeMirror> erasedTargetTypes) {
    List<TypeElement> modelTypeElements = new ArrayList<>();
    for (Element element : env.getElementsAnnotatedWith(HensonNavigable.class)) {
      try {
        parseHenson((TypeElement) element, targetClassMap, erasedTargetTypes, modelTypeElements);
      } catch (Exception e) {
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));

        error(element, "Unable to generate extra injector when parsing @HensonNavigable.\n\n%s",
            stackTrace.toString());
      }
    }
    for (TypeElement modelTypeElement : modelTypeElements) {
      targetClassMap.remove(modelTypeElement);
    }
  }

  private void parseHenson(TypeElement element, Map<TypeElement, InjectionTarget> targetClassMap,
      Set<TypeMirror> erasedTargetTypes, List<TypeElement> modelInjectTargets) {

    // Verify common generated code restrictions.
    if (!isValidUsageOfHenson(HensonNavigable.class, element)) {
      return;
    }

    // Assemble information on the injection point.
    InjectionTarget hensonNavigableTarget = getOrCreateTargetClass(targetClassMap, element);
    //get the model class of Henson annotation
    AnnotationMirror hensonAnnotationMirror = getAnnotationMirror(element, HensonNavigable.class);
    TypeMirror modelTypeMirror = getHensonModelMirror(hensonAnnotationMirror);
    if (modelTypeMirror != null) {
      TypeElement modelElement = (TypeElement) typeUtils.asElement(modelTypeMirror);
      if (!"Void".equals(modelElement.getSimpleName())) {
        if (isDebugEnabled) {
          System.out.println(String.format("HensonNavigable class %s uses model class %s\n",
              element.getSimpleName(), modelElement.getSimpleName()));
        }
        //we simply copy all extra injections from the model and add them to the target
        InjectionTarget modelInjectionTarget = getOrCreateTargetClass(targetClassMap, modelElement);
        modelInjectTargets.add(modelElement);
        hensonNavigableTarget.injectionMap.putAll(modelInjectionTarget.injectionMap);
      }
    }

    // Add the type-erased version to the valid injection targets set.
    TypeMirror erasedTargetType = typeUtils.erasure(element.asType());
    erasedTargetTypes.add(erasedTargetType);
  }

  private boolean isValidUsageOfHenson(Class<? extends Annotation> annotationClass,
      Element element) {
    boolean valid = true;

    // Verify modifiers.
    Set<Modifier> modifiers = element.getModifiers();
    if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
      error(element, "@%s class %s must not be private or static.", annotationClass.getSimpleName(),
          element.getSimpleName());
      valid = false;
    }

    // Verify containing type.
    if (element.getEnclosingElement() == null
        || element.getEnclosingElement().getKind() != PACKAGE) {
      error(element, "@%s class %s must be a top level class", annotationClass.getSimpleName(),
          element.getSimpleName());
      valid = false;
    }

    //verify there are no @InjectExtra annotated fields
    for (Element enclosedElement : element.getEnclosedElements()) {
      if (enclosedElement.getAnnotation(InjectExtra.class) != null) {
        error(element, "@%s class %s must not contain any @InjectExtra annotation",
            annotationClass.getSimpleName(), element.getSimpleName());
        valid = false;
      }
    }

    return valid;
  }

  private void enhanceInjectionTargetWithInheritedInjectionExtras(
      Map<TypeElement, InjectionTarget> targetClassMap, InjectionTarget injectionTarget) {
    InjectionTarget currentTarget = injectionTarget;
    InjectionTarget parentTarget;
    while (currentTarget != null) {
      parentTarget = findParentTarget(currentTarget.parentTarget, targetClassMap);
      if (parentTarget != null) {
        injectionTarget.injectionMap.putAll(parentTarget.injectionMap);
      }
      currentTarget = parentTarget;
    }
  }

  private InjectionTarget findParentTarget(String parentClassName,
      Map<TypeElement, InjectionTarget> targetClassMap) {
    InjectionTarget parentTarget = null;

    final Set<Map.Entry<TypeElement, InjectionTarget>> entrySet = targetClassMap.entrySet();
    for (Map.Entry<TypeElement, InjectionTarget> entryTypeToInjectionTarget : entrySet) {
      if (entryTypeToInjectionTarget.getKey()
          .getQualifiedName()
          .toString()
          .equals(parentClassName)) {
        parentTarget = entryTypeToInjectionTarget.getValue();
        break;
      }
    }

    return parentTarget;
  }

  private static AnnotationMirror getAnnotationMirror(TypeElement typeElement, Class<?> clazz) {
    String clazzName = clazz.getName();
    for (AnnotationMirror m : typeElement.getAnnotationMirrors()) {
      if (m.getAnnotationType().toString().equals(clazzName)) {
        return m;
      }
    }
    return null;
  }

  private static TypeMirror getHensonModelMirror(AnnotationMirror annotationMirror) {
    return getAnnotationValue(annotationMirror, "model");
  }

  private static TypeMirror getAnnotationValue(AnnotationMirror annotationMirror, String key) {
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror
        .getElementValues().entrySet()) {
      if (entry.getKey().getSimpleName().toString().equals(key)) {
        return (TypeMirror) entry.getValue().getValue();
      }
    }
    return null;
  }
}
