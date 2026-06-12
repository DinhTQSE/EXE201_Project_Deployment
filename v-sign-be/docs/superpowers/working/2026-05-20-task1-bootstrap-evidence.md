# Task 1 Bootstrap Evidence (RED -> GREEN)

Date: 2026-05-20

Maven executable used:

`C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd`

## RED Phase

Temporary change applied in `src/test/java/com/vsign/backend/BootSmokeTest.java`:

```java
@Test
void contextLoads() {
    assertTrue(false, "Intentional RED evidence for Task 1");
}
```

Command:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -q -Dtest=BootSmokeTest test
```

Output (run without escalation; blocked by sandboxed network):

```text
[ERROR] [ERROR] Some problems were encountered while processing the POMs:
[FATAL] Non-resolvable parent POM for com.vsign:backend:0.0.1-SNAPSHOT: The following artifacts could not be resolved: org.springframework.boot:spring-boot-starter-parent:pom:3.3.0 (absent): Could not transfer artifact org.springframework.boot:spring-boot-starter-parent:pom:3.3.0 from/to central (https://repo.maven.apache.org/maven2): Permission denied: getsockopt and 'parent.relativePath' points at no local POM @ line 7, column 13
 @
[ERROR] The build could not read 1 project -> [Help 1]
[ERROR]
[ERROR]   The project com.vsign:backend:0.0.1-SNAPSHOT (C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\pom.xml) has 1 error
[ERROR]     Non-resolvable parent POM for com.vsign:backend:0.0.1-SNAPSHOT: The following artifacts could not be resolved: org.springframework.boot:spring-boot-starter-parent:pom:3.3.0 (absent): Could not transfer artifact org.springframework.boot:spring-boot-starter-parent:pom:3.3.0 from/to central (https://repo.maven.apache.org/maven2): Permission denied: getsockopt and 'parent.relativePath' points at no local POM @ line 7, column 13 -> [Help 2]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/ProjectBuildingException
[ERROR] [Help 2] http://cwiki.apache.org/confluence/display/MAVEN/UnresolvableModelException
Access is denied.
```

Output (run with escalation; intended RED test failure):

```text
23:48:48.550 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.vsign.backend.BootSmokeTest]: BootSmokeTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
23:48:48.604 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.vsign.backend.VSignBackendApplication for test class com.vsign.backend.BootSmokeTest
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.0)

2026-05-20T23:48:48.898+07:00  INFO 3884 --- [v-sign-backend] [           main] com.vsign.backend.BootSmokeTest          : Starting BootSmokeTest using Java 21.0.11 with PID 3884 (started by KHAI in C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE)
2026-05-20T23:48:48.899+07:00  INFO 3884 --- [v-sign-backend] [           main] com.vsign.backend.BootSmokeTest          : No active profile set, falling back to 1 default profile: "default"
2026-05-20T23:48:49.670+07:00  INFO 3884 --- [v-sign-backend] [           main] com.vsign.backend.BootSmokeTest          : Started BootSmokeTest in 0.97 seconds (process running for 1.531)
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 1.735 s <<< FAILURE! -- in com.vsign.backend.BootSmokeTest
[ERROR] com.vsign.backend.BootSmokeTest.contextLoads -- Time elapsed: 0.505 s <<< FAILURE!
org.opentest4j.AssertionFailedError: Intentional RED evidence for Task 1 ==> expected: <true> but was: <false>
	at org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
	at org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
	at org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
	at org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
	at org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
	at com.vsign.backend.BootSmokeTest.contextLoads(BootSmokeTest.java:12)
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)

[ERROR] Failures:
[ERROR]   BootSmokeTest.contextLoads:12 Intentional RED evidence for Task 1 ==> expected: <true> but was: <false>
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test (default-test) on project backend: There are test failures.
[ERROR]
[ERROR] Please refer to C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\target\surefire-reports for the individual test results.
[ERROR] Please refer to dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
WARNING: A Java agent has been loaded dynamically (C:\Users\KHAI\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.16\byte-buddy-agent-1.14.16.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
```

## GREEN Phase

Restored final intended `BootSmokeTest`:

```java
@Test
void contextLoads() {
}
```

Command:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' -q -Dtest=BootSmokeTest test
```

Output (run without escalation; blocked by sandboxed network):

```text
[ERROR] [ERROR] Some problems were encountered while processing the POMs:
[FATAL] Non-resolvable parent POM for com.vsign:backend:0.0.1-SNAPSHOT: The following artifacts could not be resolved: org.springframework.boot:spring-boot-starter-parent:pom:3.3.0 (absent): Could not transfer artifact org.springframework.boot:spring-boot-starter-parent:pom:3.3.0 from/to central (https://repo.maven.apache.org/maven2): Permission denied: getsockopt and 'parent.relativePath' points at no local POM @ line 7, column 13
 @
[ERROR] The build could not read 1 project -> [Help 1]
[ERROR]
[ERROR]   The project com.vsign:backend:0.0.1-SNAPSHOT (C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\pom.xml) has 1 error
[ERROR]     Non-resolvable parent POM for com.vsign:backend:0.0.1-SNAPSHOT: The following artifacts could not be resolved: org.springframework.boot:spring-boot-starter-parent:pom:3.3.0 (absent): Could not transfer artifact org.springframework.boot:spring-boot-starter-parent:pom:3.3.0 from/to central (https://repo.maven.apache.org/maven2): Permission denied: getsockopt and 'parent.relativePath' points at no local POM @ line 7, column 13 -> [Help 2]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/ProjectBuildingException
[ERROR] [Help 2] http://cwiki.apache.org/confluence/display/MAVEN/UnresolvableModelException
Access is denied.
```

Output (run with escalation; GREEN pass):

```text
23:49:16.008 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.vsign.backend.BootSmokeTest]: BootSmokeTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
23:49:16.065 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.vsign.backend.VSignBackendApplication for test class com.vsign.backend.BootSmokeTest
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.0)

2026-05-20T23:49:16.337+07:00  INFO 8108 --- [v-sign-backend] [           main] com.vsign.backend.BootSmokeTest          : Starting BootSmokeTest using Java 21.0.11 with PID 8108 (started by KHAI in C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE)
2026-05-20T23:49:16.337+07:00  INFO 8108 --- [v-sign-backend] [           main] com.vsign.backend.BootSmokeTest          : No active profile set, falling back to 1 default profile: "default"
2026-05-20T23:49:17.170+07:00  INFO 8108 --- [v-sign-backend] [           main] com.vsign.backend.BootSmokeTest          : Started BootSmokeTest in 1.021 seconds (process running for 1.565)
WARNING: A Java agent has been loaded dynamically (C:\Users\KHAI\.m2\repository\net\bytebuddy\byte-buddy-agent\1.14.16\byte-buddy-agent-1.14.16.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

Final code state: GREEN (passing).
