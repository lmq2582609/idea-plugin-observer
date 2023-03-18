package com.china.observer.util;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.EditorTextField;

import java.io.IOException;
import java.util.Enumeration;
import java.util.StringJoiner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PsiUtil {

    /**
     * 创建代码编辑器
     * @param psi
     * @return
     */
    public static <T extends PsiElement> EditorTextField createEditorTextField(T psi, Project project) {
        //格式化代码
        String code = PsiUtil.formatCode(psi);
        //只读
        Document document = EditorFactory.getInstance().createDocument(code);
        document.setReadOnly(true);
        PsiFile psiFile = psi.getContainingFile();
        Language language = psiFile.getLanguage();
        EditorTextField editorTextField = new EditorTextField(document, project, language.getAssociatedFileType());
        //获取默认样式
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        editorTextField.setFont(scheme.getFont(EditorFontType.PLAIN));
        editorTextField.setOneLineMode(false);
        return editorTextField;
    }

    /**
     * 格式化代码
     * 从编辑器中获取的代码，不对齐，需要处理
     * @param psi
     * @return
     */
    public static <T extends PsiElement> String formatCode(T psi) {
        String formattedCode = WriteCommandAction.runWriteCommandAction(psi.getProject(), (Computable<String>) () -> {
            // 在这里执行您的代码，例如修改代码风格
            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(psi.getProject());
            codeStyleManager.reformat(psi);
            return psi.getText();
        });
        return formattedCode;
    }

    /**
     * 判断是否是本地文件
     * 本地文件Protocol为file，第三方文件Protocol为jar
     * @param psiClass
     * @return
     */
    public static boolean isLocalPsiClass(PsiClass psiClass) {
        PsiFile psiFile = psiClass.getContainingFile();
        if (psiFile != null && psiFile.getVirtualFile() != null) {
            String protocol = psiFile.getVirtualFile().getFileSystem().getProtocol();
            return "file".equals(protocol);
        }
        return false;
    }


    /**
     * 根据class获取包路径
     * @param psiClass
     * @return
     */
    public static String getPackageName(PsiClass psiClass) {
        String packageName = "";
        PsiFile containingFile = psiClass.getContainingFile();
        if (containingFile instanceof PsiJavaFile) {
            //本地java文件
            packageName = ((PsiJavaFile) containingFile).getPackageName();
        } else if (containingFile.getVirtualFile() != null && containingFile.getVirtualFile().isInLocalFileSystem()) {
            try {
                String classFilePath = containingFile.getVirtualFile().getPath();
                JarFile jarFile = new JarFile(classFilePath.substring(0, classFilePath.indexOf("!")));
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        String className = entry.getName().replace('/', '.').substring(0, entry.getName().length() - 6);
                        if (className.equals(psiClass.getQualifiedName())) {
                            packageName = className.substring(0, className.lastIndexOf('.'));
                            break;
                        }
                    }
                }
                jarFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return packageName;
    }


}
