package com.github.onbassnaga.wiremockjsonplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for WireMock file operations.
 */
public class WireMockUtils {

    /**
     * Checks if the given file is a WireMock mapping file.
     *
     * @param file The file to check
     * @return true if the file is a WireMock mapping file, false otherwise
     */
    public static boolean isMappingFile(VirtualFile file) {
        if (file == null) return false;

        // Check if the file has a .json extension
        String extension = file.getExtension();
        if (extension == null || !extension.equalsIgnoreCase("json")) {
            return false;
        }

        // Check if the file is in a "mappings" directory or has a parent directory that is in a "mappings" directory
        VirtualFile parent = file.getParent();
        while (parent != null) {
            if ("mappings".equals(parent.getName())) {
                return true;
            }
            parent = parent.getParent();
        }

        return false;
    }

    /**
     * Checks if the given file is a WireMock body file.
     *
     * @param file The file to check
     * @return true if the file is a WireMock body file, false otherwise
     */
    public static boolean isBodyFile(VirtualFile file) {
        if (file == null) return false;

        // Check if the file is in a "__files" directory or has a parent directory that is in a "__files" directory
        VirtualFile parent = file.getParent();
        while (parent != null) {
            if ("__files".equals(parent.getName())) {
                return true;
            }
            parent = parent.getParent();
        }

        return false;
    }

    /**
     * Finds the "__files" directory by traversing up from the given directory.
     *
     * @param startDir The directory to start from
     * @return The "__files" directory if found, null otherwise
     */
    public static VirtualFile findFilesDirectory(VirtualFile startDir) {
        if (startDir == null) return null;

        // Traverse up to find the "__files" directory
        VirtualFile currentDir = startDir;
        while (currentDir != null) {
            VirtualFile filesDir = currentDir.findChild("__files");
            if (filesDir != null && filesDir.isDirectory()) {
                return filesDir;
            }
            currentDir = currentDir.getParent();
        }

        return null;
    }

    /**
     * Gets the corresponding body file for a bodyFileName value in a mapping file.
     *
     * @param mappingFile The mapping file
     * @param bodyFileName The bodyFileName value from the mapping file
     * @return The VirtualFile of the body file if it exists, null otherwise
     */
    public static VirtualFile getBodyFile(VirtualFile mappingFile, String bodyFileName) {
        if (mappingFile == null || bodyFileName == null || bodyFileName.isEmpty()) {
            return null;
        }

        // Find the mappings directory
        VirtualFile mappingsDir = null;
        VirtualFile parent = mappingFile.getParent();
        while (parent != null) {
            if ("mappings".equals(parent.getName())) {
                mappingsDir = parent;
                break;
            }
            parent = parent.getParent();
        }

        if (mappingsDir == null) return null;

        // Find the "__files" directory
        VirtualFile filesDir = findFilesDirectory(mappingsDir);
        if (filesDir == null) return null;

        // Normalize the bodyFileName path (it might start with /)
        String normalizedBodyFileName = bodyFileName;
        if (normalizedBodyFileName.startsWith("/")) {
            normalizedBodyFileName = normalizedBodyFileName.substring(1);
        }

        // Split the path and navigate through the directory structure
        String[] pathParts = normalizedBodyFileName.split("/");
        VirtualFile currentDir = filesDir;

        // Navigate through all directories in the path except the last part (which is the filename)
        for (int i = 0; i < pathParts.length - 1; i++) {
            currentDir = currentDir.findChild(pathParts[i]);
            if (currentDir == null) return null;
        }

        // Return the file if it exists
        return currentDir.findChild(pathParts[pathParts.length - 1]);
    }

    /**
     * Finds all mapping files that reference a specific body file.
     *
     * @param project The current project
     * @param bodyFile The body file to find references to
     * @return A list of mapping files that reference the body file
     */
    public static List<PsiFile> findMappingFilesReferencingBodyFile(Project project, VirtualFile bodyFile) {
        if (project == null || !isBodyFile(bodyFile)) {
            return new ArrayList<>();
        }

        // Find the "__files" directory
        VirtualFile filesDir = null;
        VirtualFile parent = bodyFile.getParent();
        while (parent != null) {
            if ("__files".equals(parent.getName())) {
                filesDir = parent;
                break;
            }
            parent = parent.getParent();
        }

        if (filesDir == null) return new ArrayList<>();

        // Find the "mappings" directory at the same level as "__files"
        VirtualFile rootDir = filesDir.getParent();
        if (rootDir == null) return new ArrayList<>();

        VirtualFile mappingsDir = rootDir.findChild("mappings");
        if (mappingsDir == null) return new ArrayList<>();

        // Get the relative path of the body file from the "__files" directory
        String bodyFilePath = getRelativePath(filesDir, bodyFile);

        // Find all JSON files in the mappings directory and its subdirectories
        List<PsiFile> mappingFiles = new ArrayList<>();
        findJsonFilesRecursively(project, mappingsDir, mappingFiles);

        // Filter to only include mapping files that reference this body file
        List<PsiFile> result = new ArrayList<>();
        for (PsiFile psiFile : mappingFiles) {
            String text = psiFile.getText();

            // Check if the file contains a reference to the body file
            if (text.contains("\"bodyFileName\"") && 
                (text.contains("\"" + bodyFilePath + "\"") || 
                 text.contains("\"/" + bodyFilePath + "\""))) {
                result.add(psiFile);
            }
        }

        return result;
    }

    /**
     * Recursively finds all JSON files in a directory and its subdirectories.
     *
     * @param project The current project
     * @param dir The directory to search in
     * @param result The list to add found files to
     */
    private static void findJsonFilesRecursively(Project project, VirtualFile dir, List<PsiFile> result) {
        if (dir == null) return;

        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) {
                findJsonFilesRecursively(project, child, result);
            } else if ("json".equalsIgnoreCase(child.getExtension())) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(child);
                if (psiFile != null) {
                    result.add(psiFile);
                }
            }
        }
    }

    /**
     * Gets the relative path of a file from a base directory.
     *
     * @param baseDir The base directory
     * @param file The file to get the relative path for
     * @return The relative path as a string
     */
    public static String getRelativePath(VirtualFile baseDir, VirtualFile file) {
        Path basePath = Paths.get(baseDir.getPath());
        Path filePath = Paths.get(file.getPath());
        return basePath.relativize(filePath).toString().replace("\\", "/");
    }
}
