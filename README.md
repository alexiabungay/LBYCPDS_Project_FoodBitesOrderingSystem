# Food Bites Ordering System

## How to Run

Run the program from a **terminal**, not IntelliJ's Run button. The password masking only works when typing in a real terminal.

1. Open a terminal in the project root folder (the one that contains `src`).
   - In IntelliJ, open the **Terminal** tab (`Alt+F12`).
2. Compile the program:
   javac -d out\manual -sourcepath src src\FoodBitesApp.java
3. Run the program:
   java -cp out\manual FoodBitesApp

> **Note:** Always start the program from the project root. It reads its data from `src/data` using a relative path.
