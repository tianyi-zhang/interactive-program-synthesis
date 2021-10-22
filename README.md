
# Interactive Program Synthesis by Augmented Examples

This repository contains the source code of an interactive program synthesizer for regular expressions. It is implemented based on the interaction model proposed in our UIST 2020 paper *[Interactive Program Synthesis by Augmented Examples](https://tianyi-zhang.github.io/files/uist2020-interactive-program-synthesis.pdf)*. Users can guide the synthesis process by specifying how different parts of their examples should be treated by a synthesizer via light-weight annotations (i.e., *semantic augmentation*). Furthermore, users can ask the synthesizer to generate additional examples and corner cases to generate  by revealing how a synthesized program behave on additional examples from a hypothetical input space (i.e., *data augmentation*).

## Video Tutorial ([link](https://www.youtube.com/watch?v=dMyd9i31NxE))

## Software Prerequisites
1. Java 1.8 or higher ([download](https://www.java.com/en/download/))

**Note:** Use `java --version` to check the versions. We recommend using the 64-bit version of Java so we can allocate more memory to the program synthesizer that runs in a JVM. If you use a 32-bit version, we can only allocate a maximum amount of 4G memory to the synthesizer theorectically. In practice, the actual allocated memory could be as low as 1G. When downloading Java, please try to download the distribution or installer with `x64` in its name (not `x86`).  

## Install from Pre-built Distribution

1. Download our software distribution [here](https://drive.google.com/file/d/1__OffkKOnTEYXQHBAQM94dfKBrtB1ilE/view?usp=sharing).
2. Unzip the downloaded file.
3. In terminal, go into the unzipped folder and start the server.
`java -cp ips-backend.jar edu.harvard.seas.synthesis.HTTPServer -s lib/`
4. Open `http://localhost:8080` in your web browser.

**Note1:** Don't forget to add a backslash to escape a whitespace if your file path contains a whitespace.

## Install from Source Code

1. Clone this project. 
2. Import the `back-end` folder into Eclipse as an existing Maven project ([instruction](https://vaadin.com/learn/tutorials/import-maven-project-eclipse)).
3. In Eclipse, add `-s lib/` as the runtime commandline argument of the `HTTPServer` class ([Tutorial: How to add a commandline argument in Eclipse](https://www.codejava.net/ides/eclipse/how-to-pass-arguments-when-running-a-java-program-in-eclipse)).
4. Run `HTTPServer` to start the server.
5. Open `http://localhost:8080` in your web browser.ser.

**Note1:** We use Eclipse for development, so the instructions above are based on Eclipse. You can also use other IDEs such as [IntelliJ](https://www.lagomframework.com/documentation/1.6.x/java/IntellijMaven.html). We recommend using a modern IDE since it is easier to run and debug.

**Note2:** If you want to build the project from a terminal, run `mvn package` to build and package the project. A jar of the back-end server is generated in the `target` folder. Then run the jar following Step 3-5 in the next section.

**Note3:** Run `sh package.sh` to build the distribution.

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
    
2. Our synthesizer only works with Z3 4.8.9 or lower. The newer Z3 has changed their class signatures and are no longer compatible with our code. 


3. If the synthesis progress bar is stuck at 96% for a while (e.g., more than 2 minutes), it is likely that Z3 libraries are not found in your machine. To confirm this, please a) check if there is a `resnax-error` file, b) check if `resnax-error` has exceptions like `Exception in thread "main" java.lang.UnsatisfiedLinkError: ...\libz3java.dll: Can't find dependent libraries`. By default, I have included the Z3 libraries for Windows/Linux/Mac in the `lib` folder and these libraries will be dynamically linked and loaded to run Z3. Yet in some versions of OS, this default mechanism doesn't work, causing errors like `UnsatisfiedLinkError`. I have found several workarounds. Please try each of them one by one until the problem is solved. If none of the following solutions work, please please feel free to contact me (tianyi@g.harvard.edu) and I am happy to help. 
    - (Mac OS) If you are a Mac user, Mac OS has some restrictions on loading dynamic libraries and such restrictions have subtle differences across Mac OS versions. According to the [offical doc from Apple](https://developer.apple.com/library/archive/documentation/DeveloperTools/Conceptual/DynamicLibraries/100-Articles/DynamicLibraryUsageGuidelines.html#//apple_ref/doc/uid/TP40001928-SW21), the working directory of the current process, `/usr/lib/`, `/usr/local/lib`, `~/lib` are the several locations MacOS searches for. Yet different MacOS versions may purge dynamic libraries from some of these directories due to System Integrity Protection. For example, in Mac OSX Catalina 10.15.7, only copying Z3 libraries (`libz3java.dylib`, `com.microsoft.z3.jar`, and `libz3.dylib`) to the current directory (i.e., the `ips` folder) works. But in some other Mac OS versions such as Mojave and High Sierra, copying these files to `/usr/local/lib` works. So I recommend you to try to copy these Z3 libraries to each of the paths until the `UnsatisfiedLinkError` issue is solved.
    - (Linux) The default mechanism works in Ubuntu 14.04 and 18.04 (haven't tested 16.04 yet). But I run into this `UnsatisfiedLinkError` when installing our tool in Amazon Linux AMI. It seems Amazon EC2 doesn't allow to set custom dynamic library paths. The only way I can solve it is by placing those Z3 libraries in the default LD_LIBRARY_PATH, e.g., `/usr/lib`, `/usr/lib64`, and also removing the `Djava.library.path` argument in [this line of code](https://github.com/tianyi-zhang/interactive-program-synthesis/blob/main/back-end/src/main/java/edu/harvard/seas/synthesis/ResnaxRunner.java#L278). If your machine environment also does not allow you to set custome dynamic library paths, this solution is worth trying first.
    - If none of the solutions above work on your machine, maybe it's because none of the Z3 distributions included in our tool is compatible with your machine. In such a case, I recommend you to manually install Z3 and its Java bindings following the instructions [here](https://github.com/Z3Prover/z3). I also copied the essential steps here.
      ```bash
      python scripts/mk_make.py --java ### Please include the --java flag to build Java bindings
      cd build
      make
      sudo make install
      ```

      To test if Z3 is successfully installed, please run `z3` in command line.
