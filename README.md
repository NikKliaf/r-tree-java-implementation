# R*-Tree Spatial Index Implementation

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ_IDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)

## Overview

This project is a Java-based implementation of an **R*-tree spatial index** designed to handle **OpenStreetMap (OSM)** data. Developed as a university project for Database Technologies, it focuses on efficient spatial data management by organizing data into blocks stored within a datafile.

The system supports advanced spatial queries and includes a Python-based visualization module to analyze query performance and structure.

## Features

* **R*-Tree Indexing:** Efficient handling of spatial data using the R*-tree data structure.
* **Block Storage:** Organizes OSM data into blocks for optimized disk storage and retrieval.
* **Spatial Queries:**
    * **Range Queries:** Find all objects within a specific rectangular area.
    * **k-Nearest Neighbors (k-NN):** Find the *k* closest objects to a given point.
    * **Skyline Queries:** Identify interesting points based on multi-dimensional dominance.
* **Visualization:** Python scripts to visualize the R*-tree structure and query results.
* **Performance Analysis:** capabilities to record and compare query execution times.

## Project Structure

* `src/` - Contains the Java source code for the R*-tree implementation and query logic.
* `Python Visualization/` - Contains Python scripts for generating graphs and visualizing spatial data.
* `database_tech.iml` - IntelliJ IDEA module file.

## Getting Started

### Prerequisites

* **Java Development Kit (JDK):** Version 8 or higher.
* **Python:** Version 3.x (for visualization).
* **IntelliJ IDEA:** Recommended for opening and running the project (configuration files included).
* **Python Libraries:** `matplotlib` (and potentially `numpy`/`pandas` depending on the scripts).

### Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/NikKliaf/r-tree-java-implementation.git](https://github.com/NikKliaf/r-tree-java-implementation.git)
    cd r-tree-java-implementation
    ```

2.  **Open in IntelliJ IDEA:**
    * Launch IntelliJ IDEA.
    * Select **Open** and choose the cloned project folder.
    * The IDE should automatically detect the source roots and dependencies based on the `.iml` file.

3.  **Setup Python Environment (Optional for Visualization):**
    ```bash
    cd "Python Visualization"
    pip install matplotlib
    ```

## Usage

### Running the Java Application

1.  Locate the main entry point in the `src` folder (e.g., `Main.java` or similar).
2.  Ensure you have the required OpenStreetMap (OSM) data file available (referenced in the code as the source for the block storage).
3.  Run the class via IntelliJ or command line.

### Running Visualizations

To visualize the R*-tree or query results:

1.  Navigate to the visualization folder:
    ```bash
    cd "Python Visualization"
    ```
2.  Run the python script:
    ```bash
    python <script_name>.py
    ```
    *(Note: Replace `<script_name>.py` with the actual name of the python file in that directory, e.g., `plot.py` or `main.py`)*

## Data

This project is designed to ingest **OpenStreetMap** XML data. You may need to place your `.osm` file in the project root or specify its path in the main configuration/code before running the ingestion process.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request for any improvements or bug fixes.

## License

This project is open-source. Please check the repository for specific license details.
