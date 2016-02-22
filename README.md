# methodCoverage

This simple java agent will report all of the methods that are called during an execution, in all classes (exlcuding anonymous classes that are defined by the Java 8 `java.lang.Unsafe.defineClass` mechanism, which is currently mainly used for under the covers lambda implementation). It does *not* track usage of native methods.

Usage:

`mvn package`

`java -javaagent:/path/to/MethodCoverageRecorder-0.0.1-SNAPSHOT.jar=/path/to/outputfile <rest of java command>`

Sample output:

```
java/util/jar/Attributes$Name.hashCode()I
sun/misc/PerfCounter.getFindClasses()Lsun/misc/PerfCounter;
java/util/IdentityHashMap$IdentityHashMapIterator.<init>(Ljava/util/IdentityHashMap;)V
java/io/FileWriter.<init>(Ljava/io/File;)V
org/apache/maven/surefire/common/junit4/JUnit4RunListener.testFinished(Lorg/junit/runner/Description;)V
org/junit/runner/Description.createTestDescription(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/annotation/Annotation;)Lorg/junit/runner/Description;
java/util/zip/InflaterInputStream.ensureOpen()V
org/junit/runner/notification/RunNotifier$2.notifyListener(Lorg/junit/runner/notification/RunListener;)V
java/util/zip/ZipFile$ZipFileInflaterInputStream.fill()V
org/junit/runners/BlockJUnit4ClassRunner.withPotentialTimeout(Lorg/junit/runners/model/FrameworkMethod;Ljava/lang/Object;Lorg/junit/runners/model/Statement;)Lorg/junit/runners/model/Statement;
java/util/Formatter$FormatSpecifier.print(Ljava/lang/Object;Ljava/util/Locale;)V
java/util/IdentityHashMap$IdentityHashMapIterator.<init>(Ljava/util/IdentityHashMap;Ljava/util/IdentityHashMap$1;)V
org/apache/maven/surefire/suite/RunResult.<init>(IIIIILjava/lang/String;Z)V
org/junit/runner/Result$Listener.testRunFinished(Lorg/junit/runner/Result;)V
```