package com.github.onbassnaga.wiremockjsonplugin;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * Line marker provider for WireMock mapping files.
 * Adds "Create file" or "Go to file" links for bodyFileName references.
 */
public class WireMockLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        // Only process JSON string literals
        if (!(element instanceof JsonStringLiteral)) return;

        // Check if the file is a WireMock mapping file
        VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
        if (!WireMockUtils.isMappingFile(virtualFile)) return;

        // Check if this is a bodyFileName property value
        JsonProperty property = PsiTreeUtil.getParentOfType(element, JsonProperty.class);
        if (property == null || !"bodyFileName".equals(property.getName())) return;

        // Get the bodyFileName value
        String bodyFileName = ((JsonStringLiteral) element).getValue();

        // Find the corresponding body file
        VirtualFile bodyFile = WireMockUtils.getBodyFile(virtualFile, bodyFileName);

        if (bodyFile != null) {
            // Body file exists, add "Go to file" marker
            addGoToFileMarker(element, bodyFile, result);
        }
    }

    /**
     * Adds a "Go to file" marker for an existing body file.
     */
    private void addGoToFileMarker(
            PsiElement element,
            VirtualFile bodyFile,
            Collection<? super RelatedItemLineMarkerInfo<?>> result
    ) {

        // Convert VirtualFile to PsiFile
        PsiFile psiFile = PsiManager.getInstance(element.getProject()).findFile(bodyFile);
        if (psiFile == null) return;

        // Create a list with the target file
        Collection<PsiFile> targets = Collections.singletonList(psiFile);

        // Create a line marker with the action
        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(AllIcons.General.OpenDisk)
                .setTargets(targets)
                .setTooltipText("Go to file")
                .setPopupTitle("Navigate to Body File")
                .setAlignment(GutterIconRenderer.Alignment.RIGHT);

        result.add(builder.createLineMarkerInfo(element));
    }

    /**
     * Creates a body file and opens it in the editor.
     */
    public void createBodyFile(Project project, String bodyFileName, VirtualFile mappingFile) {
        try {
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

            if (mappingsDir == null) return;

            // Find or create the __files directory
            VirtualFile rootDir = mappingsDir.getParent();
            if (rootDir == null) return;

            VirtualFile filesDir = rootDir.findChild("__files");
            if (filesDir == null) {
                filesDir = WriteCommandAction.runWriteCommandAction(project, 
                    (com.intellij.openapi.util.ThrowableComputable<VirtualFile, IOException>) () -> 
                    rootDir.createChildDirectory(this, "__files")
                );
            }

            // Normalize the bodyFileName path (it might start with /)
            String normalizedBodyFileName = bodyFileName;
            if (normalizedBodyFileName.startsWith("/")) {
                normalizedBodyFileName = normalizedBodyFileName.substring(1);
            }

            // Split the path and create the directory structure
            String[] pathParts = normalizedBodyFileName.split("/");
            VirtualFile currentDir = filesDir;

            // Create all directories in the path except the last part (which is the filename)
            for (int i = 0; i < pathParts.length - 1; i++) {
                String dirName = pathParts[i];
                VirtualFile childDir = currentDir.findChild(dirName);
                if (childDir == null) {
                    final VirtualFile parentDir = currentDir;
                    final String finalDirName = dirName;
                    childDir = WriteCommandAction.runWriteCommandAction(project, 
                        (com.intellij.openapi.util.ThrowableComputable<VirtualFile, IOException>) () -> 
                        parentDir.createChildDirectory(this, finalDirName)
                    );
                }
                currentDir = childDir;
            }

            // Create the file
            String fileName = pathParts[pathParts.length - 1];
            final VirtualFile dir = currentDir;
            final String finalFileName = fileName;

            VirtualFile bodyFile = WriteCommandAction.runWriteCommandAction(project, 
                (com.intellij.openapi.util.ThrowableComputable<VirtualFile, IOException>) () -> {
                    VirtualFile file = dir.createChildData(this, finalFileName);
                    VfsUtil.saveText(file, "{\n  \n}");
                    return file;
                }
            );

            // Open the file in the editor
            if (bodyFile != null) {
                FileEditorManager.getInstance(project).openTextEditor(
                        new OpenFileDescriptor(project, bodyFile),
                        true
                );
            }
        } catch (IOException e) {
            // Handle the exception (e.g., log it or show an error message)
            com.intellij.openapi.diagnostic.Logger.getInstance(WireMockLineMarkerProvider.class)
                .error("Error creating body file", e);
        }
    }
}
