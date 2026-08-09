# DOT//CORE Alpha 0.2.1 build fix

Fixed Android build configuration:

- Android Gradle Plugin: 9.4.0 -> 9.3.1 (stable 9.3 line)
- Gradle bootstrap: 9.6.1 -> 9.5.0
- Gradle wrapper properties: 9.6.1 -> 9.5.0
- Linux and Windows bootstrap scripts now use the same Gradle version.

Desktop run:

    ./gradlew -PskipAndroid :lwjgl3:run

Android build/install:

    ./gradlew :android:installDebug

JDK 17 is required for AGP 9.3.
