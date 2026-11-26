# Food Bites Ordering System

## How to Run

Run the program from a **terminal**, not IntelliJ's Run button. The password prompt only hides your typing in a real terminal.

1. Open a terminal in the project's main folder (the one that contains `src`).
   - In IntelliJ, open the **Terminal** tab (`Alt+F12`).

2. Compile the program:
```bat
javac -d out\manual -sourcepath src src\FoodBitesApp.java
```

3. Run the program:
```bat
java -cp out\manual FoodBitesApp
```

> **Note:** Run these commands from the project's main folder, or the program won't find its data files.
