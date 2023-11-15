It's recommended you now run the following for intellij setup:
```bash
	./gradlew clean
	./gradlew cleanIdea
	./gradlew cleanIdeaWorkspace
	./gradlew setupDevWorkspace
	./gradlew classes
	./gradlew idea
	./gradlew genIntellijRuns
```

If you wish to use eclipse, simply replace idea/intellij with eclipse in the above commands.

Do not import the gradle project when opening in recent versions of intellij.
