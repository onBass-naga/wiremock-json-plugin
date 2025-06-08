package com.github.onbassnaga.wiremockjsonplugin;

import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Annotator for WireMock mapping files.
 * Underlines the value of "bodyFileName" properties in the editor and makes them clickable.
 */
public class WireMockAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
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
            // Body file exists, add a hyperlink to navigate to it
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE)
                    .tooltip("Click to navigate to body file")
                    .withFix(new com.intellij.codeInsight.intention.impl.BaseIntentionAction() {
                        @NotNull
                        @Override
                        public String getText() {
                            return "Go to body file";
                        }

                        @NotNull
                        @Override
                        public String getFamilyName() {
                            return "Navigation";
                        }

                        @Override
                        public boolean isAvailable(@NotNull Project project, com.intellij.openapi.editor.Editor editor, com.intellij.psi.PsiFile file) {
                            return true;
                        }

                        @Override
                        public void invoke(@NotNull Project project, com.intellij.openapi.editor.Editor editor, com.intellij.psi.PsiFile file) {
                            FileEditorManager.getInstance(project).openTextEditor(
                                    new OpenFileDescriptor(project, bodyFile),
                                    true
                            );
                        }
                    })
                    .create();
        } else {
            // Body file doesn't exist, add a hyperlink to create it
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(element)
                    .textAttributes(DefaultLanguageHighlighterColors.HIGHLIGHTED_REFERENCE)
                    .tooltip("Click to create body file")
                    .withFix(new com.intellij.codeInsight.intention.impl.BaseIntentionAction() {
                        @NotNull
                        @Override
                        public String getText() {
                            return "Create body file";
                        }

                        @NotNull
                        @Override
                        public String getFamilyName() {
                            return "Creation";
                        }

                        @Override
                        public boolean isAvailable(@NotNull Project project, com.intellij.openapi.editor.Editor editor, com.intellij.psi.PsiFile file) {
                            return true;
                        }

                        @Override
                        public void invoke(@NotNull Project project, com.intellij.openapi.editor.Editor editor, com.intellij.psi.PsiFile file) {
                            // Delegate to the line marker provider to create the file
                            new WireMockLineMarkerProvider().createBodyFile(project, bodyFileName, virtualFile);
                        }
                    })
                    .create();
        }
    }
}
