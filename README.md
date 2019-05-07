# Introduction
jar-module is a plugin for maven to package the jar.

The first option is to clean a jar file from the directory entries and to set the main-class 
in module-main.class as found in the manifest file. 

The main option is to call jlink to generate a java with only the used modules as runtime.
All needed jar are copied in one directory to update the automatic modules to be real modules.  
After that the call to jlink is possible.

The other option is to generate  

# Requirements
Java version 9 or newer.

Maven to build the maven-plugin.

Maven to use the maven-plugin.

# Usage
<pre><code>
    &lt;plugin>
        &lt;groupId>com.mtag.tools.maven&lt;/groupId>
        &lt;artifactId>jar-module-maven-plugin&lt;/artifactId>
        &lt;version>1.0-SNAPSHOT&lt;/version>
        &lt;configuration>
            &lt;repack>true&lt;/repack>
            &lt;openmodule>false&lt;/openmodule>
            &lt;appName>LinkListe&lt;/appName>
            &lt;appMenuGroup>MT AG&lt;/appMenuGroup>
            &lt;appVendor>MT AG&lt;/appVendor>
        &lt;/configuration>
        &lt;executions>
            &lt;execution>
                &lt;id>make-assembly&lt;/id>
                &lt;phase>package&lt;/phase>
                &lt;goals>
                    &lt;goal>jlink&lt;/goal>
                    &lt;goal>javapackager&lt;/goal>
                    &lt;goal>jar&lt;/goal>
                &lt;/goals>
            &lt;/execution>
        &lt;/executions>
    &lt;/plugin>
</code>
</pre>

The goal <code>jar</code> sets the <code>main-class</code> if it is a module and the main class is not set
but the <code>Main-Class</code> is set in the manifest. Cleans the jar if <code>repack</code> is true.
 
The goal <code>jlink</code> calls the <code>jlink</code> in the sub dir <code>modules</code> and copies 
all <code>jars</code> from the dependencies to the <code>modules</code> folder.
The new <code>java runtime</code> is found in the <code>run</code> sub folder

The goal <code>javapackager</code> calls the javapackager to create an installer for the jar.
 