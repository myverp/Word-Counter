import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class WordCounterGUI {
    private static ConcurrentHashMap<String, Integer> wordCountMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // Create the GUI frame
        JFrame frame = new JFrame("Word Counter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400); // Increased initial window size

        // Text area for input
        JTextArea textArea = new JTextArea(10, 40);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        // Button to initiate word counting
        JButton countButton = new JButton("Count Words");
        countButton.setPreferredSize(new Dimension(150, 40)); // Enlarged button size

        // Text area for displaying results
        JTextArea resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        // Progress bar for task status
        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false); // Hidden until counting starts

        // Action for the button
        countButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String inputText = textArea.getText();
                resultArea.setText(""); // Clear previous results
                wordCountMap.clear(); // Clear previous history
                progressBar.setVisible(true);
                progressBar.setValue(0);

                // Run word counting method in a separate thread
                new Thread(() -> {
                    try {
                        countWords(inputText, resultArea, progressBar);
                    } catch (InterruptedException | ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });

        // Panel with vertical layout for components
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10)); // Layout with padding between components

        // Adding components to the panel
        panel.add(new JScrollPane(textArea), BorderLayout.NORTH); // Text area at the top
        panel.add(countButton, BorderLayout.CENTER); // Button at the center
        panel.add(progressBar, BorderLayout.WEST); // Progress bar on the left
        panel.add(new JScrollPane(resultArea), BorderLayout.SOUTH); // Result area at the bottom

        // Add panel to frame
        frame.add(panel);
        frame.setVisible(true);
    }

    public static void countWords(String text, JTextArea resultArea, JProgressBar progressBar) throws InterruptedException, ExecutionException {
        String[] words = text.split("\\s+");
        int numThreads = 4; // Number of threads
        int chunkSize = words.length / numThreads;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            int start = i * chunkSize;
            int end = (i == numThreads - 1) ? words.length : (i + 1) * chunkSize;

            Callable<Void> task = () -> {
                for (int j = start; j < end; j++) {
                    wordCountMap.merge(words[j].toLowerCase(), 1, Integer::sum);
                }
                return null;
            };

            futures.add(executor.submit(task));
        }

        // Checking task completion and updating progress
        int totalTasks = futures.size();
        int completedTasks = 0;

        while (completedTasks < totalTasks) {
            completedTasks = 0;
            for (Future<?> future : futures) {
                if (future.isDone()) {
                    completedTasks++;
                }
            }
            // Update progress bar value
            int progress = (int) (((double) completedTasks / totalTasks) * 100);
            progressBar.setValue(progress);

            // Brief pause to avoid excessive updates
            Thread.sleep(100);
        }

        executor.shutdown();

        // Display results
        for (Map.Entry<String, Integer> entry : wordCountMap.entrySet()) {
            resultArea.append(entry.getKey() + ": " + entry.getValue() + "\n");
        }
        progressBar.setVisible(false); // Hide progress bar upon completion
    }
}
