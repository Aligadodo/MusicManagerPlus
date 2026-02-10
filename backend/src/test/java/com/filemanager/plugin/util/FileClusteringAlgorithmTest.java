package com.filemanager.plugin.util;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class FileClusteringAlgorithmTest {

    @Test
    public void testClusterFiles() {
        List<File> files = new ArrayList<>();
        files.add(new File("Song 1 - Artist.mp3"));
        files.add(new File("Song 2 - Artist.mp3"));
        files.add(new File("Song 3 - Artist.mp3"));
        files.add(new File("Different Song - Other Artist.mp3"));

        FileClusteringAlgorithm algorithm = new FileClusteringAlgorithm(0.7, "默认");

        List<FileClusteringAlgorithm.FileCluster> clusters = algorithm.clusterFiles(files);

        assertNotNull(clusters);
        assertFalse(clusters.isEmpty());
        System.out.println("Clusters: " + clusters.size());
    }

    @Test
    public void testClusterFilesWithEmptyList() {
        List<File> files = new ArrayList<>();

        FileClusteringAlgorithm algorithm = new FileClusteringAlgorithm(0.7, "默认");

        List<FileClusteringAlgorithm.FileCluster> clusters = algorithm.clusterFiles(files);

        assertNotNull(clusters);
        assertTrue(clusters.isEmpty());
    }

    @Test
    public void testClusterFilesWithSingleFile() {
        List<File> files = new ArrayList<>();
        files.add(new File("Single Song - Artist.mp3"));

        FileClusteringAlgorithm algorithm = new FileClusteringAlgorithm(0.7, "默认");

        List<FileClusteringAlgorithm.FileCluster> clusters = algorithm.clusterFiles(files);

        assertNotNull(clusters);
        assertTrue(clusters.isEmpty());
    }
}