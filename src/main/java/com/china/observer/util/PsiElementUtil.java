package com.china.observer.util;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.Language;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.Enumeration;
import java.util.StringJoiner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PsiElementUtil {

    /**
     * 获取当前光标所处的文档
     * @param project
     * @param editor
     * @return
     */
    public static PsiElement findPsiElementByDocument(Project project, Editor editor) {
        //通过项目的文档管理器，获取当前编辑器展示的文件
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null) {
            return null;
        }
        //获取展示的文件中，光标所在的PsiElement
        return psiFile.findElementAt(editor.getCaretModel().getOffset());
    }

    /**
     * 展示代码
     * @param psiElement
     */
    public static void showCode(PsiElement psiElement, Editor editor) {
        EditorTextField editorTextField = createEditorTextField(psiElement, psiElement.getProject());
        JBScrollPane scrollPane = new JBScrollPane(editorTextField);
        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, editor.getComponent())
                .setRequestFocus(true)
                .setResizable(true)
                .setMovable(true)
                .setTitle("Show Code")
                .createPopup();
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                if (editorTextField.getEditor() != null) {
                    EditorFactory.getInstance().releaseEditor(editorTextField.getEditor());
                }
            }
        });
        popup.showInFocusCenter();
    }

    /**
     * 创建代码编辑器
     * @param psi
     * @return
     */
    public static EditorTextField createEditorTextField(PsiElement psi, Project project) {
        //格式化代码
        String code = formatCode(psi);
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
     * 格式化Psi中的代码
     * 从编辑器中获取的代码，不对齐，需要处理
     * @param psiElement
     * @return
     */
    public static String formatCode(PsiElement psiElement) {
        if (psiElement.isWritable()) {
            //如果是可读可写的，格式化代码
            return WriteCommandAction.runWriteCommandAction(psiElement.getProject(), (Computable<String>) () -> {
                CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(psiElement.getProject());
                codeStyleManager.reformat(psiElement);
                return psiElement.getText();
            });
        } else {
            //只读的，不能修改
            return psiElement.getText();
        }
    }

    /**
     * 查找类中的字段
     * @param psiClass
     * @param fieldName
     * @return
     */
    public static PsiField selectClassField(PsiClass psiClass, String fieldName) {
        //先使用findFiledByName查找一遍
        PsiField psiField = psiClass.findFieldByName(fieldName, true);
        if (psiField != null) {
            return psiField;
        }
        //未查找到，再遍历查找一遍，但此处不能查找父类字段
        PsiField[] fields = psiClass.getFields();
        fieldName = fieldName.toLowerCase();
        for (PsiField f : fields) {
            if (f.getName().toLowerCase().equals(fieldName)) {
                return f;
            }
        }
        return null;
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
