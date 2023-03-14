package com.china.observer.ui;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;

/**
 * 给XML加上图标
 */
public class GutterMapperXmlIcon extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        // 检查元素是否是XML标记
        if (!(element instanceof XmlTag)) {
            return;
        }
        // 获取XML标记
        XmlTag xmlTag = (XmlTag) element;
        //xml的namespace
        String namespace = xmlTag.getNamespace();
        //将当前class与xml进行匹配
        PsiManager psiManager = PsiManager.getInstance(xmlTag.getProject());
        //查找项目中所有Java文件
        Collection<VirtualFile> projectAllXML = FileTypeIndex.getFiles(JavaFileType.INSTANCE,
                GlobalSearchScope.projectScope(xmlTag.getProject()));
        //未查找到Java文件，跳过
        if (projectAllXML.size() == 0) {
            return;
        }
        // 通过JavaPsiFacade获取项目中的PsiClass对象
        PsiClass psiClass = JavaPsiFacade.getInstance(xmlTag.getProject())
                .findClass(namespace, GlobalSearchScope.allScope(xmlTag.getProject()));
        if (psiClass != null) {
            // 找到了对应的PsiClass对象
            // ...
        }

        for (VirtualFile virtualFile : projectAllXML) {
            PsiFile javaFile = psiManager.findFile(virtualFile);
            if (!(javaFile instanceof PsiJavaFile)) {
                continue;
            }
            //获取java文件
            ((PsiJavaFile) javaFile).getClasses();







        }



    }

}
