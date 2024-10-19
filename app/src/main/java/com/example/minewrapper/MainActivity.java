package com.example.minewrapper;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final int ROWS = 6; // Updated number of rows
    private static final int COLS = 4; // Updated number of columns
    private static final int NUM_MINES = 5; // You may adjust this based on the grid size
    private Button[][] buttons = new Button[ROWS][COLS]; // Adjusted dimensions
    private boolean[][] mines = new boolean[ROWS][COLS]; // Adjusted dimensions
    private int[][] adjacentMines = new int[ROWS][COLS]; // Adjusted dimensions
    private boolean[][] revealed = new boolean[ROWS][COLS]; // Adjusted dimensions
    private GridLayout gridLayout;

    private Handler inactivityHandler;  // Handler to manage inactivity
    private Runnable inactivityRunnable;  // Runnable to execute after inactivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridLayout = findViewById(R.id.gridLayout);
        Button restartButton = findViewById(R.id.restartButton);

        gridLayout.setRowCount(ROWS);
        gridLayout.setColumnCount(COLS);

        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartGame();
            }
        });

        initializeGame(); // Start the game when the app is opened
        setupInactivityHandler();  // Set up the inactivity handler
    }

    private void setupInactivityHandler() {
        inactivityHandler = new Handler();
        inactivityRunnable = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Please make a move!", Toast.LENGTH_SHORT).show();  // Guidance message
            }
        };
    }


        private void restartGame() {
            // Clear existing game state
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    revealed[row][col] = false;  // Reset revealed state
                    buttons[row][col].setEnabled(true);  // Enable all buttons
                    buttons[row][col].setText("");  // Clear button text
                    buttons[row][col].setBackgroundResource(R.drawable.cell_background); // Reset background to default
                }
            }
            initializeMines();  // Re-initialize the mines and related state
            calculateAdjacentMines(); // Re-calculate adjacent mines
            displayHeuristicOnCells(); // Show heuristic values on buttons

            resetInactivityTimer(); // Reset timer on game restart
        }


        private void initializeGame() {
        gridLayout.removeAllViews();  // Clear any previous views

        // Initialize buttons and add them to the grid layout
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                buttons[row][col] = new Button(this);
                buttons[row][col].setLayoutParams(new GridLayout.LayoutParams());
                buttons[row][col].setBackgroundResource(R.drawable.cell_background);
                buttons[row][col].setTextColor(Color.BLACK); // Set text color to black
                buttons[row][col].setTag(row + "," + col);
                buttons[row][col].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resetInactivityTimer(); // Reset the timer on click
                        String tag = (String) v.getTag();
                        String[] coordinates = tag.split(",");
                        int row = Integer.parseInt(coordinates[0]);
                        int col = Integer.parseInt(coordinates[1]);
                        handleCellClick(row, col);
                    }
                });
                buttons[row][col].setEnabled(true); // Ensure button is enabled
                gridLayout.addView(buttons[row][col]);
            }
        }
        initializeMines();
        calculateAdjacentMines();
        displayHeuristicOnCells();
    }
    private void changeButtonColors(int color) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                buttons[row][col].setBackgroundColor(color); // Change button color
            }
        }
    }


    private void resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable); // Remove previous callbacks
        inactivityHandler.postDelayed(inactivityRunnable, 20000); // Set delay for 20 seconds
    }
    private void initializeMines() {
        // Reset mines array
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                mines[row][col] = false;  // Clear existing mines
            }
        }

        Random random = new Random();
        int placedMines = 0;

        while (placedMines < NUM_MINES) {
            int row = random.nextInt(ROWS);
            int col = random.nextInt(COLS);
            if (!mines[row][col]) {  // Only place a mine if there's not already one there
                mines[row][col] = true;  // Place the mine
                placedMines++;  // Increment count of placed mines
            }
        }
    }


    private void displayHeuristicOnCells() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (!revealed[row][col]) {
                    int heuristicCost = calculateHeuristicCost(row, col);
                    buttons[row][col].setText(String.valueOf(heuristicCost));  // Display cost for testing
                }
            }
        }
    }

    private void calculateAdjacentMines() {
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (mines[row][col]) continue;

                int mineCount = 0;
                for (int i = 0; i < 8; i++) {
                    int newRow = row + dx[i];
                    int newCol = col + dy[i];
                    if (isValid(newRow, newCol) && mines[newRow][newCol]) {
                        mineCount++;
                    }
                }
                adjacentMines[row][col] = mineCount;
            }
        }
    }

    private void handleCellClick(int row, int col) {
        if (mines[row][col]) {
            buttons[row][col].setText("💣");  // Mark mine
            Toast.makeText(this, "Game Over!", Toast.LENGTH_SHORT).show();
            revealAllMines();
            disableAllButtons();  // Disable all buttons after game over
            changeButtonColors(Color.RED); // Change color to red for game over
        } else {
            aStarFindSafePath(row, col);
            if (areOnlyMinesLeft()) {
                Toast.makeText(this, "All safe cells revealed! You win!", Toast.LENGTH_LONG).show();
                revealAllMines(); // Reveal all mines
                changeButtonColors(Color.parseColor("#90EE90")); // Light green for win
                disableAllButtons(); // Disable all buttons to prevent further clicks
            }
        }
    }

    private void revealAllMines() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (mines[row][col]) {
                    buttons[row][col].setText("💣");  // Show mine
                    buttons[row][col].setBackgroundColor(Color.RED); // Change mine cell to red
                } else {
                    buttons[row][col].setBackgroundColor(Color.parseColor("#90EE90")); // Change safe cell to light green
                }
            }
        }
    }

    private boolean areOnlyMinesLeft() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                // If a cell is not revealed and not a mine, return false
                if (!revealed[row][col] && !mines[row][col]) {
                    return false; // There are still safe cells left
                }
            }
        }
        return true; // Only mines are left
    }

    private void aStarFindSafePath(int startRow, int startCol) {
        PriorityQueue<Cell> openSet = new PriorityQueue<>(Comparator.comparingInt(cell -> cell.fCost));
        HashSet<Cell> closedSet = new HashSet<>();

        Cell start = new Cell(startRow, startCol, 0, calculateHeuristicCost(startRow, startCol));
        openSet.add(start);

        while (!openSet.isEmpty()) {
            Cell current = openSet.poll();

            // If the cell has been revealed, continue
            if (revealed[current.row][current.col]) continue;

            // Reveal the current cell
            revealCell(current.row, current.col);

            // If the current cell has adjacent mines, stop expanding
            if (adjacentMines[current.row][current.col] > 0) break;

            // Explore neighbors
            int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
            int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

            for (int i = 0; i < 8; i++) {
                int newRow = current.row + dx[i];
                int newCol = current.col + dy[i];

                if (isValid(newRow, newCol) && !revealed[newRow][newCol]) {
                    int gCost = current.gCost + 1;
                    int hCost = calculateHeuristicCost(newRow, newCol);
                    Cell neighbor = new Cell(newRow, newCol, gCost, hCost);

                    // If not in closed set or has a better path, add to open set
                    if (!closedSet.contains(neighbor) || gCost < neighbor.gCost) {
                        openSet.add(neighbor);
                    }
                }
            }
            closedSet.add(current);
        }
    }

    private void revealCell(int row, int col) {
        if (revealed[row][col]) return;  // Already revealed cell, skip it

        revealed[row][col] = true;  // Mark this cell as revealed
        buttons[row][col].setEnabled(false);  // Disable the button
        buttons[row][col].setText(String.valueOf(adjacentMines[row][col]));  // Display adjacent mine count

        // If there are no adjacent mines, keep revealing its neighbors via A*
        if (adjacentMines[row][col] == 0) {
            int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
            int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

            for (int i = 0; i < 8; i++) {
                int newRow = row + dx[i];
                int newCol = col + dy[i];
                if (isValid(newRow, newCol) && !revealed[newRow][newCol]) {
                    revealCell(newRow, newCol);  // Recursive reveal
                }
            }
        }
    }

    private boolean isValid(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }



    private void disableAllButtons() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                buttons[row][col].setEnabled(false);
            }
        }
    }

    private int calculateHeuristicCost(int row, int col) {
        // A simple heuristic function (can be modified)
        return adjacentMines[row][col];
    }

    // Cell class for A* algorithm
    private static class Cell {
        int row, col;
        int gCost; // Cost from start to this cell
        int hCost; // Heuristic cost to target
        int fCost; // Total cost (gCost + hCost)

        Cell(int row, int col, int gCost, int hCost) {
            this.row = row;
            this.col = col;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost; // Total cost
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Cell)) return false;
            Cell other = (Cell) obj;
            return this.row == other.row && this.col == other.col;
        }

        @Override
        public int hashCode() {
            return 31 * row + col; // Generate a unique hash for each cell
        }
    }
}
