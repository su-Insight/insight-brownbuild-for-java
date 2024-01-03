# insight-brownbuild-for-java

A rewrite of the **Java version** from the Ubisoft open source project ***ubisoft-laforge-brownbuild***. The original project is based on Go language and Python implementation, the original address of the project is `https://github.com/ubisoft/ubisoft-laforge-brownbuild`

This projects aims at identifying brown builds (unreliable builds) from the CI build jobs. In this folder, you'll find the source code to extract words from build jobs' log files and to create process the extracted vocabulary and classify the jobs using a XGBoost model.

## Execute

This is a Java project managed using `Maven`. To use and execute this project, you need to ensure that you have a Java environment installed(Development environment: Java 8).Then you can either directly execute the project using the JAR file I've already generated in `/execute`, or build the project yourself using the `mvn` command. For the latter, this requires you to have an installed Maven environment.

## Dataset

To be able to run the script, you should have a folder containing job logs with title:

`{builddate}_{buildid}_{commitid}_{classification}_{buildname}.log`

- {builddate} is the date at which the build was started in the following format YYYY_MM_DD_HH_MM_SS
- {buildid} is the id of the build job
- {commitid} is the cl / commit hash that was built
- {classification} shows if the build failed (1) or succeeded (0)
- {buildname} is the name of the build job

A dataset already scrapted is provided with this project. You can find it under `graphviz/`. Five zip are provided and all the job logs of those 5 zips should be put in a same directory, for example, in `./dataset/graphviz/`.

Caution: unzipped, `graphviz.zip` contains 37GB of data.

## View

The project offers two display modes to showcase the processing procedure: Verbose mode and Minimal mode.

> **Verbose mode:** `Default mode,` displaying the information of file handling and quantities.
>
> **Minimal mode:** Only display the progress of file processing using a progress bar.

You can explicitly specify them via the command line by `-m` and `-v` when you execute the program. If omitted, it represents the Verbose mode.

## Vocabulary extraction

The vocabulary extraction is done using the `MainExtract.class` file.

Certainly! You have two options to execute:

**Using the provided JAR:** You can directly run the project by executing the provided JAR file.

```shell
cd execute
java -jar VocabularyExtract-jar-with-dependencies.jar 5 ../dataset/graphviz/ ../dataset/graphviz_extracted/ -m
```

> **java -jar \<jarName\> \<proc\> \<pathIn\> \<pathOut\> [mode]**
>
> The meanings of the three parameters are as follows:
>
> - **processor:** The number of threads.
> - **pathIn:** The input path containing log files.
> - **pathOut: **The file path for the output of vocabulary extraction.
> - **mode:** The display modes to showcase the processing procedure,`-v`for Verbose mode and `-m`for Minimal mode.

**Building with Maven:** Alternatively, you can build and run the project using the `mvn` command in a Maven environment.

```shell
mvn package	 # execute the command in the path to pom.xml file.
cd target
java -jar VocabularyExtract-jar-with-dependencies.jar 5 ../dataset/graphviz/ ../dataset/graphviz_extracted/
```

Output:

```shell
# Verbose mode:-v
...
File ../dataset/graphviz/2017_10_03_01_33_26_34842399_57998deb8fd7a6d27386fd666feb4d411a925ec4_0_portablesourcepackaging.log is processing by thread: pool-1-thread-1   876/9873
...
Thread pool-1-thread-1 has processed.

Done:../dataset/graphviz_extracted/
--- 0h18m14.217s--- 

# Minimal mode:-m
[███████████████████████████████████████████████████]  100% 
Done:../dataset/graphviz_extracted/
--- 0h17m44.985s--- 
```



