package com.china.observer.util;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MybatisUtil {

    /**
     * 根据class接口查找xml
     * @param psiClass
     * @return
     */
    public static XmlTag findXmlByPsiClass(PsiClass psiClass) {
        //查找所有XML
        Collection<VirtualFile> virtualFiles = findProjectFileByType(psiClass.getProject(), XmlFileType.INSTANCE);
        if (virtualFiles.size() == 0) {
            return null;
        }
        //遍历XML文件
        for (VirtualFile virtualFile : virtualFiles) {
            //校验改文件是否是mapper.xml
            XmlTag xmlTag = checkIsMapperXml(virtualFile, psiClass);
            if (xmlTag == null) {
                continue;
            }
            return xmlTag;
        }
        return null;
    }

    /**
     * 根据method接口方法查找xml
     * @param psiMethod
     * @return
     */
    public static XmlTag findXmlByPsiMethod(PsiMethod psiMethod) {
        //查找所有XML
        Collection<VirtualFile> virtualFiles = findProjectFileByType(psiMethod.getProject(), XmlFileType.INSTANCE);
        if (virtualFiles.size() == 0) {
            return null;
        }
        PsiClass psiClass = psiMethod.getContainingClass();
        //遍历XML文件
        for (VirtualFile virtualFile : virtualFiles) {
            //校验改文件是否是mapper.xml
            XmlTag xmlTag = checkIsMapperXml(virtualFile, psiClass);
            if (xmlTag == null) {
                continue;
            }
            //获取 mapper.xml 子节点
            XmlTag[] subTags = xmlTag.getSubTags();
            for (XmlTag subTag : subTags) {
                String id = subTag.getAttributeValue("id");
                //如果方法名与mapper.xml中的id一致，可以添加图标
                if (psiMethod.getName().equals(id)) {
                    return subTag;
                }
            }
        }
        return null;
    }

    /**
     * 根据文件类型查找项目内所有的文件
     * @return
     */
    public static Collection<VirtualFile> findProjectFileByType(Project project, FileType fileType) {
        //查找范围：整个项目
        Collection<VirtualFile> virtualFiles = FileTypeIndex.getFiles(fileType,
                GlobalSearchScope.projectScope(project));
        //本地项目路径 - 忽略.idea
        String ideaPath = project.getBasePath() + "/.idea";
        return virtualFiles.stream().filter(virtualFile -> !virtualFile.getPath().startsWith(ideaPath)).collect(Collectors.toList());
    }

    /**
     * 检查是否是mapper.xml
     * @param virtualFile
     * @param containingClass
     * @return
     */
    public static XmlTag checkIsMapperXml(VirtualFile virtualFile, PsiClass containingClass) {
        //如果不是一个接口，跳过
        if (containingClass == null || !containingClass.isInterface()) {
            return null;
        }
        PsiManager psiManager = PsiManager.getInstance(containingClass.getProject());
        PsiFile file = psiManager.findFile(virtualFile);
        if (!(file instanceof XmlFile)) {
            return null;
        }
        XmlFile xmlFile = (XmlFile) file;
        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag == null || !"mapper".equals(rootTag.getName())){
            return null;
        }
        //包路径
        String packageName = PsiUtil.getPackageName(containingClass) + "." + containingClass.getName();
        //检查这个class与xml中的namespace是否一致
        String namespace = rootTag.getAttributeValue("namespace");
        //校验是否一致
        if (packageName.equals(namespace)) {
            return rootTag;
        }
        return null;
    }

}
