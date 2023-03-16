package com.china.observer.util;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
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
    public static <T extends PsiElement> EditorTextField createEditorTextField(T psi) {
        //格式化代码
        String code = PsiUtil.formatCode(psi.getText());
        //只读
        Document document = EditorFactory.getInstance().createDocument(code);
        document.setReadOnly(true);
        PsiFile psiFile = psi.getContainingFile();
        Language language = psiFile.getLanguage();
        EditorTextField editorTextField = new EditorTextField(document, psi.getProject(), language.getAssociatedFileType());
        //获取默认样式
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        editorTextField.setFont(scheme.getFont(EditorFontType.PLAIN));
        editorTextField.setOneLineMode(false);
        return editorTextField;
    }

    /**
     * 格式化代码
     * 从编辑器中获取的代码，不对齐，需要处理
     * @param code
     * @return
     */
    public static String formatCode(String code) {
        //处理文本-格式化代码
        String[] split = code.split("\\n");
        StringJoiner sj = new StringJoiner("\n");
        for (String s : split) {
            //如果第1个字符是空格，则删除4个空格
            if (Character.isWhitespace(s.charAt(0))) {
                sj.add(s.substring(4));
            } else {
                sj.add(s);
            }
        }
        return sj.toString();
    }

    /**
     * 判断是否是本地文件
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


    public static String getPackageName(PsiClass psiClass) {
        String packageName = "";
        PsiFile containingFile = psiClass.getContainingFile();
        if (containingFile instanceof PsiJavaFile) {
            packageName = ((PsiJavaFile) containingFile).getPackageName();
        } else if (containingFile.getVirtualFile() != null && containingFile.getVirtualFile().isInLocalFileSystem()) {
            String classFilePath = containingFile.getVirtualFile().getPath();
            try {
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
