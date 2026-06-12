Airway Analysis System
Educational and Research-Based Decision Support Tool

--------------------------------------------------

1. PROJECT OVERVIEW

The Airway Analysis System is a Java and JavaFX desktop application developed for educational and research-based airway structure analysis.

The system processes CT airway case data, converts the data into image slices, builds airway graphs, extracts structural airway features, compares airway cases using similarity analysis, detects suspicious airway regions, and uses pathfinding to trace routes to suspicious airway areas.

The system is intended for:
- educational demonstrations,
- research-based structural airway analysis,
- feature extraction experiments,
- graph-based airway visualization,
- suspicious airway structure highlighting.

The system DOES NOT:
- diagnose disease,
- replace medical professionals,
- provide clinically validated conclusions.

Recommended wording:

“The system provides educational and research-based airway analysis support. It highlights structural patterns that may require expert review, but it does not provide a medical diagnosis.”

--------------------------------------------------

2. MAIN FEATURES

The system includes:

- CT airway case processing
- NIfTI (.nii / .nii.gz) support
- PNG slice generation
- Airway compression into a final airway image
- Binary mask generation
- Edge detection
- Zhang-Suen skeletonization
- Airway graph construction
- Airway feature extraction
- KNN similarity comparison
- Suspicious airway node detection
- Dijkstra pathfinding
- JavaFX visualization

--------------------------------------------------

3. TECHNOLOGIES USED

Programming Language:
- Java

GUI Framework:
- JavaFX

IDE Used:
- Eclipse IDE

Custom Data Structures:
- ArrayList
- HashMap
- MinHeap
- Graph structures

Algorithms Used:
- Zhang-Suen Thinning Algorithm
- Sobel Edge Detection
- KNN Similarity Comparison
- Dijkstra Shortest Path Algorithm

--------------------------------------------------

4. FOLDER STRUCTURE

Your ZIP submission should contain:

src/
dist/
ss/

src/ contains the source code, README file, dataset link, and setup instructions.

dist/ contains the executable JAR file.

ss/ contains the PowerPoint presentation slides.

--------------------------------------------------

5. SYSTEM REQUIREMENTS

- Windows 10 or Windows 11
- Java JDK installed
- JavaFX SDK installed
- Eclipse IDE (recommended)
- At least 8 GB RAM
- Recommended: 16 GB RAM

--------------------------------------------------

6. DATASET DOWNLOAD

The project uses the AeroPath airway CT dataset.

Direct download link:

https://zenodo.org/records/10069289/files/AeroPath.zip?download=1

Backup dataset page:

https://zenodo.org/records/10069289

Download the ZIP file, extract it, and select a CT case folder when running the program.

--------------------------------------------------

7. RUNTIME VM ARGUMENTS

The program should be run using the following VM arguments:

-Xms512m -Xmx6g --module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls

These arguments are required because the application processes large CT airway data and may need additional heap memory during CT conversion, PNG slice generation, airway compression, graph construction, and feature extraction. The JavaFX module path is also included so that the JavaFX user interface can run correctly.

--------------------------------------------------

8. HOW TO ADD VM ARGUMENTS IN ECLIPSE

Step 1:
Right click the project.

Step 2:
Select:
Run As → Run Configurations

Step 3:
Select the project.

Step 4:
Open:
Arguments

Step 5:
Paste the VM arguments inside:
VM Arguments

--------------------------------------------------

9. RUNNING THE PROGRAM

Run:

Main.java

or

MainUI.java

depending on the project setup.

--------------------------------------------------

10. HOW TO USE THE PROGRAM

Step 1 — Launch the Application

Run the project using:
Main.java

or:
MainUI.java

--------------------------------------------------

Step 2 — Select a CT Case Folder

When the program opens:
- Click the button used to process/select a case.
- Choose a CT case folder containing:
  - CT scan
  - Airway labels
  - Lung labels

--------------------------------------------------

Step 3 — Process the Case

The system will:
- convert CT data into PNG slices,
- generate airway masks,
- compress the airway slices,
- preprocess the image,
- generate the airway graph,
- extract airway features.

Depending on the computer specifications, this may take some time.

--------------------------------------------------

Step 4 — View Results

The interface will display:
- compressed airway image,
- edge image,
- graph visualization,
- extracted airway features,
- suspicious airway regions,
- similarity comparison results.

--------------------------------------------------

Step 5 — Similarity Detection

The system compares the current case against saved dataset cases using KNN similarity comparison.

The interface displays:
- closest matching airway cases,
- similarity percentages.

--------------------------------------------------

Step 6 — Suspicious Node Detection

The system highlights suspicious airway regions based on:
- abnormal tapering,
- abrupt endings,
- high tortuosity,
- abnormal branching.

--------------------------------------------------

Step 7 — Pathfinding

The system can trace a route from the airway start node to suspicious airway regions using Dijkstra pathfinding.

This helps visualize where suspicious structures are located inside the airway graph.

--------------------------------------------------

11. FEATURE INTERPRETATION

These are educational and research-based heuristic thresholds.

They are NOT clinical diagnostic thresholds.

Branch Count:
- Less than 50 → Very Low
- 50–99 → Low
- 100–150 → Normal
- 151–200 → Healthy / well-developed tree
- More than 200 → High / possible noise

Branch Point Count:
- Less than 30 → Very Low
- 30–59 → Low
- 60–90 → Normal
- 91–120 → Healthy branching pattern
- More than 120 → High / possible noise

Average Tapering:
- <= 0.03 → Normal
- > 0.03 and <= 0.05 → Slightly High
- > 0.05 → High / suspicious

Average Tortuosity:
- <= 1.2 → Normal
- > 1.2 and <= 1.3 → Slightly High
- > 1.3 → High / suspicious

Abrupt Ending Ratio:
- <= 0.2 → Normal
- > 0.2 and <= 0.3 → Slightly High
- > 0.3 → High / suspicious

--------------------------------------------------

12. IMPORTANT DISCLAIMER

This system is:
- educational,
- research-based,
- experimental.

It is NOT:
- a medical diagnosis tool,
- clinically validated software,
- a replacement for healthcare professionals.

Recommended wording:

“The system highlights structural airway patterns that may require expert review but does not provide medical diagnoses.”

--------------------------------------------------

13. COMMON ERRORS AND SOLUTIONS

Problem:
JavaFX Runtime Error

Solution:
- Verify JavaFX SDK path
- Verify VM arguments
- Verify JavaFX libraries are correctly configured

--------------------------------------------------

Problem:
OutOfMemoryError

Solution:
Increase heap memory using:

-Xmx6g

--------------------------------------------------

Problem:
Dataset Not Loading

Solution:
- Verify dataset folder structure
- Verify .nii.gz files exist
- Verify file names are correct

--------------------------------------------------

Problem:
Similarity Results Look Incorrect

Possible causes:
- threshold inconsistencies,
- noisy binary masks,
- graph construction instability,
- incomplete airway masks.

--------------------------------------------------

14. AUTHORS

223030665 – Frank Bahle Ntakirutimana

222109098 – Wonderful Sandile Ngwenya

223212417 – Eze Ozoma Israel

223075495 – Albert Tembe

University of Johannesburg

Third-Year Computer Science Mini Project

2026

--------------------------------------------------

15. GROUP PROJECT VIDEO LINK

https://youtu.be/OXwjcWS1nB8

--------------------------------------------------

16. FINAL NOTES

User Interface Overview

The user interface allows the user to:
- Select and process CT airway cases
- View compressed airway images
- View edge-detected airway images
- View airway graph visualizations
- Extract airway structural features
- Compare airway cases using similarity detection
- Detect suspicious airway regions
- Run pathfinding to suspicious airway nodes
- View similarity percentages and feature results

The interface was designed using JavaFX and provides a visual educational workflow for airway-analysis processing.

--------------------------------------------------

Airway Analysis System – Mini Project

This project is a Java and JavaFX desktop application developed for educational and research-based airway analysis. The system processes CT airway case data, converts CT scans into PNG slices, generates airway graphs, extracts structural airway features, performs similarity comparison using KNN, detects suspicious airway regions, and uses Dijkstra pathfinding to trace routes to suspicious airway nodes.

The system demonstrates concepts from:
- Medical image preprocessing
- Graph theory
- Pathfinding algorithms
- Feature extraction
- Similarity detection
- Custom data structures
- JavaFX desktop development

Main features:
- CT airway case processing
- PNG slice generation
- Airway compression
- Edge detection and binary masking
- Zhang-Suen skeletonization
- Airway graph construction
- Feature extraction
- KNN similarity comparison
- Suspicious airway detection
- Dijkstra shortest-path visualization

Important Disclaimer:
This system is intended for educational and research purposes only. It is not clinically validated and does not provide medical diagnoses. The system highlights structural airway patterns that may require expert review.