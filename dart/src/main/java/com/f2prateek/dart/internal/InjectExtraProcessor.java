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

package com.f2prateek.dart.internal;

import com.f2prateek.dart.InjectExtra;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

public final class InjectExtraProcessor extends AbstractProcessor {
  public static final String SUFFIX = "$$ExtraInjector";
  public static final String INTENT_BUILDER_SUFFIX = "IntentBuilder";

  private Elements elementUtils;
  private Types typeUtils;
  private Filer filer;

  @Override public synchronized void init(ProcessingEnvironment env) {
    super.init(env);

    elementUtils = env.getElementUtils();
    typeUtils = env.getTypeUtils();
    filer = env.getFiler();
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    Set<String> supportTypes = new LinkedHashSet<String>();
    supportTypes.add(InjectExtra.class.getCanonicalName());
    return supportTypes;
  }

  @Override public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
    Map<TypeElement, ExtraInjector> targetClassMap = findAndParseTargets(env);

    for (Map.Entry<TypeElement, ExtraInjector> entry : targetClassMap.entrySet()) {
      TypeElement typeElement = entry.getKey();
      ExtraInjector extraInjector = entry.getValue();

      try {
        JavaFileObject jfo = filer.createSourceFile(extraInjector.getFqcn(), typeElement);
        Writer writer = jfo.openWriter();
        writer.write(extraInjector.brewJava());
        writer.flush();
        writer.close();
      } catch (IOException e) {
        error(typeElement, "Unable to write injector for type %s: %s", typeElement, e.getMessage());
      }

      // Now write the IntentBuilder
      try {
        IntentBuilder intentBuilder = new IntentBuilder(extraInjector.getClassPackage(),
            typeElement.getSimpleName() + INTENT_BUILDER_SUFFIX, extraInjector.getTargetClass(),
            extraInjector.getInjectionMap());
        System.out.println(intentBuilder.brewJava());
        JavaFileObject jfo = filer.createSourceFile(intentBuilder.getFqcn(), typeElement);
        Writer writer = jfo.openWriter();
        writer.write(intentBuilder.brewJava());
        writer.flush();
        writer.close();
      } catch (IOException e) {
        error(typeElement, "Unable to write intent builder for type %s: %s", typeElement,
            e.getMessage());
      }
    }

    return true;
  }

  private Map<TypeElement, ExtraInjector> findAndParseTargets(RoundEnvironment env) {
    Map<TypeElement, ExtraInjector> targetClassMap =
        new LinkedHashMap<TypeElement, ExtraInjector>();
    Set<TypeMirror> erasedTargetTypes = new LinkedHashSet<TypeMirror>();

    // Process each @InjectExtra elements.
    for (Element element : env.getElementsAnnotatedWith(InjectExtra.class)) {
      try {
        parseInjectExtra(element, targetClassMap, erasedTargetTypes);
      } catch (Exception e) {
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));

        error(element, "Unable to generate extra injector for @InjectExtra.\n\n%s",
            stackTrace.toString());
      }
    }

    // Try to find a parent injector for each injector.
    for (Map.Entry<TypeElement, ExtraInjector> entry : targetClassMap.entrySet()) {
      String parentClassFqcn = findParentFqcn(entry.getKey(), erasedTargetTypes);
      if (parentClassFqcn != null) {
        entry.getValue().setParentInjector(parentClassFqcn + SUFFIX);
      }
    }

    return targetClassMap;
  }

  private boolean isValidForGeneratedCode(Class<? extends Annotation> annotationClass,
      String targetThing, Element element) {
    boolean hasError = false;
    TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

    // Verify method modifiers.
    Set<Modifier> modifiers = element.getModifiers();
    if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
      error(element, "@%s %s must not be private or static. (%s.%s)",
          annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
          element.getSimpleName());
      hasError = true;
    }

    // Verify containing type.
    if (enclosingElement.getKind() != CLASS) {
      error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
          annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
          element.getSimpleName());
      hasError = true;
    }

    // Verify containing class visibility is not private.
    if (enclosingElement.getModifiers().contains(PRIVATE)) {
      error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
          annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
          element.getSimpleName());
      hasError = true;
    }

    return hasError;
  }

  private void parseInjectExtra(Element element, Map<TypeElement, ExtraInjector> targetClassMap,
      Set<TypeMirror> erasedTargetTypes) {
    boolean hasError = false;
    TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

    // Verify common generated code restrictions.
    hasError |= isValidForGeneratedCode(InjectExtra.class, "fields", element);

    if (hasError) {
      return;
    }

    // Assemble information on the injection point.
    String name = element.getSimpleName().toString();
    String key = element.getAnnotation(InjectExtra.class).value();
    TypeMirror type = element.asType();
    boolean required = isRequiredInjection(element);
    boolean parcel =
        hasAnnotationWithFQCN(typeUtils.asElement(element.asType()), "org.parceler.Parcel");

    ExtraInjector extraInjector = getOrCreateTargetClass(targetClassMap, enclosingElement);
    extraInjector.addField(isNullOrEmpty(key) ? name : key, name, type, required, parcel);

    // Add the type-erased version to the valid injection targets set.
    TypeMirror erasedTargetType = typeUtils.erasure(enclosingElement.asType());
    erasedTargetTypes.add(erasedTargetType);
  }

  /**
   * Returns {@code true} if the an annotation is found on the given element with the given class
   * name (not fully qualified).
   */
  private static boolean hasAnnotationWithName(Element element, String simpleName) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      String annotationName = mirror.getAnnotationType().asElement().getSimpleName().toString();
      if (simpleName.equals(annotationName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if an injection is deemed to be not required. This happens if it is
   * annotated with any annotation named {@code Optional} or {@code Nullable}.
   */
  private static boolean isRequiredInjection(Element element) {
    return !hasAnnotationWithName(element, "Nullable") && !hasAnnotationWithName(element,
        "Optional");
  }

  /**
   * Returns {@code true} if the an annotation is found on the given element with the given class
   * name (must be a fully qualified class name).
   */
  private static boolean hasAnnotationWithFQCN(Element element, String annotationClassNameName) {
    if (element != null) {
      for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
        if (annotationMirror.getAnnotationType()
            .asElement()
            .toString()
            .equals(annotationClassNameName)) {
          return true;
        }
      }
    }
    return false;
  }

  private ExtraInjector getOrCreateTargetClass(Map<TypeElement, ExtraInjector> targetClassMap,
      TypeElement enclosingElement) {
    ExtraInjector extraInjector = targetClassMap.get(enclosingElement);
    if (extraInjector == null) {
      String targetType = enclosingElement.getQualifiedName().toString();
      String classPackage = getPackageName(enclosingElement);
      String className = getClassName(enclosingElement, classPackage) + SUFFIX;

      extraInjector = new ExtraInjector(classPackage, className, targetType);
      targetClassMap.put(enclosingElement, extraInjector);
    }
    return extraInjector;
  }

  private static String getClassName(TypeElement type, String packageName) {
    int packageLen = packageName.length() + 1;
    return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
  }

  /** Finds the parent injector type in the supplied set, if any. */
  private String findParentFqcn(TypeElement typeElement, Set<TypeMirror> parents) {
    TypeMirror type;
    while (true) {
      type = typeElement.getSuperclass();
      if (type.getKind() == TypeKind.NONE) {
        return null;
      }
      typeElement = (TypeElement) ((DeclaredType) type).asElement();
      if (containsTypeMirror(parents, type)) {
        String packageName = getPackageName(typeElement);
        return packageName + "." + getClassName(typeElement, packageName);
      }
    }
  }

  /**
   * Returns true if the string is null or 0-length.
   *
   * @param str the string to be examined
   * @return true if str is null or zero length
   */
  private static boolean isNullOrEmpty(String str) {
    return str == null || str.trim().length() == 0;
  }

  private boolean containsTypeMirror(Collection<TypeMirror> mirrors, TypeMirror query) {
    // Ensure we are checking against a type-erased version for normalization purposes.
    query = typeUtils.erasure(query);

    for (TypeMirror mirror : mirrors) {
      if (typeUtils.isSameType(mirror, query)) {
        return true;
      }
    }
    return false;
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  private void error(Element element, String message, Object... args) {
    processingEnv.getMessager().printMessage(ERROR, String.format(message, args), element);
  }

  private String getPackageName(TypeElement type) {
    return elementUtils.getPackageOf(type).getQualifiedName().toString();
  }
}
