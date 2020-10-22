
# Interactive Program Synthesis by Augmented Examples

This repository contains the source code of an interactive program synthesizer for regular expressions. It is implemented based on the interaction model proposed in our UIST 2020 paper *[Interactive Program Synthesis by Augmented Examples](https://tianyi-zhang.github.io/files/uist2020-interactive-program-synthesis.pdf)*. Users can guide the synthesis process by specifying how different parts of their examples should be treated by a synthesizer via light-weight annotations (i.e., *semantic augmentation*). Furthermore, users can ask the synthesizer to generate additional examples and corner cases to generate  by revealing how a synthesized program behave on additional examples from a hypothetical input space (i.e., *data augmentation*).

## Software Prerequisites
1. Java 1.8 or higher ([download](https://www.java.com/en/download/))

**Note:** Use `java --version` to check the versions. We recommend using the 64-bit version of Java so we can allocate more memory to the program synthesizer that runs in a JVM. If you use a 32-bit version, we can only allocate a maximum amount of 4G memory to the synthesizer theorectically. In practice, the actual allocated memory could be as low as 1G. When downloading Java, please try to download the distribution or installer with `x64` in its name (not `x86`).  

## Install from Pre-built Distribution

1. Download our software distribution [here](https://drive.google.com/open?id=1_9cEILvFzF4D32KDTJTa_890eCzbqrml).
2. Unzip the downloaded file.
3. If you are a Mac user, please copy `lib/libz3java.dylib`, `lib/com.microsoft.z3.jar`, and `lib/libz3.dylib` to `/usr/local/lib`. 
4. In terminal, go into the unzipped folder and start the server.
`java -jar ips-backend.jar edu.harvard.seas.synthesis.SynthesisServer -s lib/`
5. Launch the HTTP server. 
`java -jar ips-backend.jar edu.harvard.seas.synthesis.HTTPServer`
6. Open `http://localhost:8080` in your web browser.

**Note1:** Don't forget to add a backslash to escape a whitespace if your file path contains a whitespace.

## Install from Source Code

1. Clone this project. 
2. If you are a Mac user, please copy `lib/libz3java.dylib`, `lib/com.microsoft.z3.jar`, and `lib/libz3.dylib` to `/usr/local/lib`. 
3. Import the `back-end` folder into Eclipse as an existing Maven project ([instruction](https://vaadin.com/learn/tutorials/import-maven-project-eclipse)).
4. In Eclipse, add `-s lib/` as the runtime commandline argument of the `SynthesisServer` class ([Tutorial: How to add a commandline argument in Eclipse](https://www.codejava.net/ides/eclipse/how-to-pass-arguments-when-running-a-java-program-in-eclipse)).
5. Run `SynthesisServer` to start the synthesis server.
6. Run `HTTPServer` to start the HTTP server.
7. Open `http://localhost:8080` in your web browser.ser.

**Note1:** We use Eclipse for development, so the instructions above are based on Eclipse. You can also use other IDEs such as [IntelliJ](https://www.lagomframework.com/documentation/1.6.x/java/IntellijMaven.html). We recommend using a modern IDE since it is easier to run and debug.

**Note2:** If you want to build the project from a terminal, run `mvn package` to build and package the project. A jar of the back-end server is generated in the `target` folder. Then run the jar following Step 3-5 in the next section.

## Backend Server Usage
**Usage:** 
`java -jar ips-backend.jar -s <arg> [-n <arg>] [-t <arg>] [-h]`

**Options:**

-s,--synthesizer <arg>       (Required) specify the path for the program synthesis libraries

-n,--example-num <arg>       (Optional) specify the number of input examples generated per cluster per example seed. The default value is 5.

-t,--timeout <arg>           (Optional) specify the timeout for the synthesis. The default value is 60 seconds.

-h,--help                    Print the help information.


## Troubleshooting
1. In Mac, you may see the following error.
```libz3java.dylib cannot be opened because it is from an unidentified developer.```
The underlying synthesizer in our tool depends on a theorem prover, [Z3](https://github.com/Z3Prover/z3), developed by Microsoft Research. Please grant the permission to this app by 1) open System Preferences, 2) click Security & Privacy, 3) click General, and 4) click "Open Anyway" next to the warning of this app.

2. By default, you don't have to install Z3 by yourself as our tool has multiple distributions of Z3 for different operating systems. We have tested it on Mac OS Mojave and Catalina, Ubuntu 18.04, and Windows 10. But if you run into exceptions like `Exception in thread "main" java.lang.UnsatisfiedLinkError: ...\libz3java.dll: Can't find dependent libraries`, it means the default Z3 distributions do not work. In such a case, I would recommend you to install Z3 by yourself following instructions in [Z3's website](https://github.com/Z3Prover/z3).
