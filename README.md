# Introduction
jar-module is a plugin for maven to package the jar.

The first option is to clean a jar file from the directory entries and to set the main-class 
in module-main.class as found in the manifest file. 

The main option is to call jlink to generate a java with only the used modules as runtime.
All needed jar are copied in one directory to update the automatic modules to be real modules.  
After that the call to jlink is possible.

The other option is to generate an installer for windows, macOS, linux or an image. It uses the 
javapackager and it's required tools. Only available for Java 9 and 10 of Oracle JDK. 
Not in Open JDK.   

# Documentation
The generated site documentation of the maven plugin jar-module can be viewed at 

https://mt-ag.github.io/jar-module-maven-plugin/

# Requirements
Java version 9 or newer.

Maven to build the maven-plugin.

Maven to use the maven-plugin.

For goal javapackager an Oracle JDK 9 or 10 is needed.

# Usage
```xml
<plugin>
    <groupId>com.mt-ag.tools.maven</groupId>
    <artifactId>jar-module-maven-plugin</artifactId>
    <version>1.0</version>
    <configuration>
        <repack>true</repack>
        <openmodule>false</openmodule>
        <appName>LinkListe</appName>
        <appMenuGroup>MT AG</appMenuGroup>
        <appVendor>MT AG</appVendor>
    </configuration>
    <executions>
        <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
                <goal>jlink</goal>
                <goal>javapackager</goal>
                <goal>jar</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The goal <code>jar</code> sets the <code>main-class</code> if it is a module and the main class is not set
but the <code>Main-Class</code> is set in the manifest. Cleans the jar if <code>repack</code> is true.
 
The goal <code>jlink</code> calls the <code>jlink</code> in the sub dir <code>modules</code> and copies 
all <code>jars</code> from the dependencies to the <code>modules</code> folder.
The new <code>java runtime</code> is found in the <code>run</code> sub folder. It is packed as 
<code>zip-file</code> in the artifact name ending with <code>.run.zip</code>. The classifier is 
<code>run</code> in the additional artifact.

The goal <code>javapackager</code> calls the javapackager to create an installer for the jar. The installer
is packed into the <code>zip-file</code> in the artifact name ending with <code>.install.zip</code>. The 
classifier is <code>install</code> in the additional artifact.
 
