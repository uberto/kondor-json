package com.ubertob.kondor.json

import com.ubertob.kondor.json.jsonnode.FieldsValues
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.outcome.asFailure
import com.ubertob.kondor.outcome.asSuccess
import com.ubertob.kondor.outcome.onFailure
import java.lang.reflect.Constructor
import kotlin.reflect.KClass

typealias ObjectFields = Map<String, Any?>


/**
 * Abstract base class for JSON converters that work with Kotlin data classes.
 *
 * JDataClass provides automatic deserialization for data classes by matching the declared
 * JSON fields to the constructor parameters in the same order. This eliminates the need
 * to override the deserializeOrThrow method.
 *
 * Usage:
 * ```
 * data class Person(val id: Int, val name: String)
 *
 * object PersonJson : JDataClass<Person>(Person::class) {
 *     val id by num(Person::id)
 *     val name by str(Person::name)
 * }
 * ```
 *
 * Important: The order of property declarations in the converter must match the order
 * of parameters in the data class constructor for automatic deserialization to work correctly.
 *
 * @param T The data class type this converter handles
 * @param klazz The Kotlin class reference for the data class
 */
abstract class JDataClass<T : Any>(klazz: KClass<T>) : JObj<T>() {

    private val kClass: KClass<T> = klazz
    val clazz: Class<T> = klazz.java

    private val constructor: Constructor<T> by lazy {
        if (!klazz.isData)
            println("Warning! The class $klazz doesn't seem to be a DataClass!")

        clazz.constructors.first() as Constructor<T>
    }

    private val kConstructor by lazy {
        kClass.constructors.first()
    }

    override fun FieldsValues.deserializeOrThrow(path: NodePath): T =
        buildInstance(getMap(), path)
            .orThrow()

    fun buildInstance(args: ObjectFields, path: NodePath) =
        try {
            // Get constructor parameters and properties in their declared order
            val constructorParams = kConstructor.parameters
            val properties = getProperties()

            // Ensure we have the same number of properties as constructor parameters
            if (properties.size != constructorParams.size) {
                throw IllegalStateException("Number of properties (${properties.size}) doesn't match constructor parameters (${constructorParams.size})")
            }

            // Create argument array by matching properties to constructor parameters by position
            val orderedArgs = properties.zip(constructorParams).map { (property, param) ->
                val value = args[property.propName]

                // If the parameter is nullable and the value is missing, pass null
                if (value == null && param.type.isMarkedNullable) {
                    null
                } else if (value == null && !param.type.isMarkedNullable) {
                    throw IllegalArgumentException("Missing required parameter: ${param.name} (property: ${property.propName})")
                } else {
                    value
                }
            }.toTypedArray()

            constructor.newInstance(*orderedArgs)
                .asSuccess()
        } catch (t: Exception) {
            ConverterJsonError(
                path,
                "Error calling constructor with signature ${constructor.description()} using params $args. Error: ${t.message}"
            ).asFailure()
        }
}

private fun <T> Constructor<T>.description(): String =
    parameterTypes
        .joinToString(prefix = "[", postfix = "]") { it.simpleName }

fun <T : Any> JDataClass<T>.testParserAndRender(times: Int = 100, generator: (index: Int) -> T) {
    repeat(times) { index ->
        val value = generator(index)

        val jn = toJsonNode(value)

        val valFromNode = fromJsonNode(jn, NodePathRoot)
            .onFailure { throw AssertionError(it) }

        assert(value == valFromNode)

        val json = toJson(value)
        val valFromJson = fromJson(json)
            .onFailure { throw AssertionError(it) }

        assert(value == valFromJson)
    }
}

/*
Possible strategy to generate metadata needed to call constructor on Person using
annotation on Kondor converter.
This would remove the need of having same orderconstructor args order and converter fields.


Using the annotation processor to generate metadata that can be used to call the constructor
params in the right order.
We doon't need to annotate each `Person` or JPerson,
 directly Instead of placing `@ConstructorMetadata` on `Person`,
you can annotate another class that **contains** a field of type `Person`,
and the annotation processor will generate metadata for `Person`.

---

## **Approach**
1. Annotate a **container class** that has a field of type `Person`.
2. The annotation processor scans the fields in this container class.
3. If a field's type is a **custom class** (like `Person`), extract its constructor metadata.
4. Generate metadata for that field's class (`Person`) instead of the container.

---

### **Step 1: Annotate a Container Class**
```java
@ConstructorMetadata
public class DataHolder {
    public Person person;
}
```
Here, we annotate `DataHolder`, **not** `Person`.

---

### **Step 2: Modify the Annotation Processor**
We update the processor to:
- Look for fields in annotated classes.
- Identify custom types (e.g., `Person`).
- Extract the constructor parameters of those types.
- Generate metadata for those types.

```java
import com.google.auto.service.AutoService;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("ConstructorMetadata")
@SupportedSourceVersion(SourceVersion.RELEASE_17)  // Change to your Java version
public class ConstructorMetadataProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(ConstructorMetadata.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement classElement = (TypeElement) element;

                for (Element enclosed : classElement.getEnclosedElements()) {
                    if (enclosed.getKind() == ElementKind.FIELD) {
                        VariableElement field = (VariableElement) enclosed;
                        TypeElement fieldType = (TypeElement) typeUtils.asElement(field.asType());

                        if (fieldType != null && fieldType.getKind() == ElementKind.CLASS) {
                            generateMetadataForClass(fieldType);
                        }
                    }
                }
            }
        }
        return true;
    }

    private void generateMetadataForClass(TypeElement classElement) {
        String className = classElement.getSimpleName().toString();
        String packageName = elementUtils.getPackageOf(classElement).toString();
        String metadataClassName = className + "Metadata";

        try {
            Writer writer = processingEnv.getFiler().createSourceFile(packageName + "." + metadataClassName).openWriter();
            writer.write("package " + packageName + ";\n\n");
            writer.write("import java.util.LinkedHashMap;\nimport java.util.Map;\n\n");
            writer.write("public class " + metadataClassName + " {\n");

            // Generate a map for constructor parameters
            writer.write("    public static final Map<String, Class<?>> CONSTRUCTOR_PARAMS = new LinkedHashMap<>();\n\n");
            writer.write("    static {\n");

            // Get the primary constructor (assumption: first constructor found)
            List<? extends Element> members = classElement.getEnclosedElements();
            for (Element member : members) {
                if (member.getKind() == ElementKind.CONSTRUCTOR) {
                    ExecutableElement constructor = (ExecutableElement) member;
                    for (VariableElement param : constructor.getParameters()) {
                        String paramName = param.getSimpleName().toString();
                        String paramType = param.asType().toString();
                        writer.write("        CONSTRUCTOR_PARAMS.put(\"" + paramName + "\", " + paramType + ".class);\n");
                    }
                    break; // Stop after first constructor
                }
            }

            writer.write("    }\n}");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

---

### **Step 3: Define the `Person` Class**
```java
public class Person {
    public String name;
    public int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

---

### **Step 4: Use the Generated Metadata**
After compilation, the processor will generate `PersonMetadata.java`:
```java
package mypackage;

import java.util.LinkedHashMap;
import java.util.Map;

public class PersonMetadata {
    public static final Map<String, Class<?>> CONSTRUCTOR_PARAMS = new LinkedHashMap<>();

    static {
        CONSTRUCTOR_PARAMS.put("name", String.class);
        CONSTRUCTOR_PARAMS.put("age", int.class);
    }
}
```

Now, we can dynamically create `Person` instances using this metadata.

```java
import java.lang.reflect.Constructor;
import java.util.Map;

public class DynamicInstanceCreator {
    public static <T> T createInstance(Class<T> clazz, Map<String, Object> paramMap) throws Exception {
        // Load metadata dynamically
        Class<?> metadataClass = Class.forName(clazz.getName() + "Metadata");
        Map<String, Class<?>> constructorParams = (Map<String, Class<?>>) metadataClass.getDeclaredField("CONSTRUCTOR_PARAMS").get(null);

        Constructor<?> constructor = clazz.getConstructors()[0];
        Object[] args = new Object[constructor.getParameterCount()];

        int i = 0;
        for (String paramName : constructorParams.keySet()) {
            args[i++] = paramMap.getOrDefault(paramName, null);
        }

        return (T) constructor.newInstance(args);
    }

    public static void main(String[] args) throws Exception {
        Map<String, Object> paramMap = Map.of("name", "Alice", "age", 30);
        Person person = createInstance(Person.class, paramMap);
        System.out.println(person);
    }
}
```

---

## **Summary**
✔ **Annotation is placed on a container class (`DataHolder`)**
✔ **The annotation processor scans its fields**
✔ **If a field is a custom class (`Person`), metadata is generated for it**
✔ **Metadata is used at runtime to dynamically instantiate the object**

Would this work for your needs? 🚀
 */
