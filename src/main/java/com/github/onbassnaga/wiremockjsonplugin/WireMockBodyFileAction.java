package com.github.onbassnaga.wiremockjsonplugin;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Action for WireMock body files.
 * Shows a popup menu with mapping files that reference the body file.
 */
public class WireMockBodyFileAction extends AnAction {

    public WireMockBodyFileAction() {
        super("Find Mapping Files", "Find mapping files that reference this body file", null);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

        // Only enable for body files in a project
        e.getPresentation().setEnabledAndVisible(project != null &&
                                                WireMockUtils.isBodyFile(file));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (!WireMockUtils.isBodyFile(file)) return;

        // Find mapping files that reference this body file
        List<PsiFile> mappingFiles = WireMockUtils.findMappingFilesReferencingBodyFile(project, file);

        if (mappingFiles.isEmpty()) {
            // No mapping files reference this body file
            return;
        }

        // Create actions for each mapping file
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        
        for (PsiFile mappingFile : mappingFiles) {
            AnAction action = new AnAction(mappingFile.getName()) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    // Open the mapping file
                    VirtualFile virtualFile = mappingFile.getVirtualFile();
                    if (virtualFile == null) return;
                    
                    FileEditorManager.getInstance(project).openTextEditor(
                        new OpenFileDescriptor(project, virtualFile),
                        true
                    );
                }

                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.BGT;
                }
            };
            
            actionGroup.add(action);
        }

        // Show popup with the actions
        JBPopupFactory.getInstance()
            .createActionGroupPopup(
                "Mapping Files",
                actionGroup,
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.NUMBERING,
                true
            )
            .showInBestPositionFor(e.getDataContext());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}