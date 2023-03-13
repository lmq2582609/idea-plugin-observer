package com.china.observer.ui;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

/**
 * 自定义的编辑器，用于展示代码
 */
public class CustomEditor extends TextEditorProvider {

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        // 返回 true 表示该文件可以在此处处理
        return true;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        // 获取PsiFile
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

        if (psiFile instanceof PsiJavaFile) {
            // 打开文本编辑器并返回TextEditor实例
            //Editor editor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, virtualFile), false);

            // 获取文件的文档对象
            Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document == null) {
                throw new IllegalArgumentException("No document for file: " + virtualFile.getName());
            }
            // 创建新的编辑器对象
            Editor editor = EditorFactory.getInstance().createEditor(document, project);

            return new TextEditor() {

                @Override
                public <T> @Nullable T getUserData(@NotNull Key<T> key) {
                    return null;
                }

                @Override
                public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {

                }

                @Override
                public void dispose() {
                    // 释放编辑器资源
                    EditorFactory.getInstance().releaseEditor(editor);
                }

                @Override
                public @NotNull JComponent getComponent() {
                    // 返回编辑器的组件
                    return editor.getComponent();
                }

                @Override
                public @Nullable JComponent getPreferredFocusedComponent() {
                    // 返回首选的聚焦组件
                    return editor.getContentComponent();
                }

                @Override
                public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
                    return null;
                }

                @Override
                public void setState(@NotNull FileEditorState state) {

                }

                @Override
                public boolean isModified() {
                    // 返回是否已修改
                    return false;
                }

                @Override
                public boolean isValid() {
                    // 返回编辑器是否有效
                    return true;
                }

                @Override
                public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

                }

                @Override
                public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

                }

                @Override
                public @Nullable FileEditorLocation getCurrentLocation() {
                    return null;
                }

                @Override
                public boolean canNavigateTo(@NotNull Navigatable navigatable) {
                    return false;
                }

                @Override
                public void navigateTo(@NotNull Navigatable navigatable) {

                }

                @Override
                public @NotNull Editor getEditor() {
                    return null;
                }
            };
        }

        return null;
    }

}
