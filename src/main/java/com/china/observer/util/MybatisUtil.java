package com.china.observer.util;

import com.china.observer.entity.MybatisMethodXmlBO;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import java.util.*;
import java.util.stream.Collectors;

public class MybatisUtil {

    /**
     * 根据class接口查找xml
     * @param psiClass
     * @return
     */
    public static XmlTag findXmlByPsiClass(PsiClass psiClass) {
        //如果不是一个接口，跳过
        if (psiClass == null || !psiClass.isInterface()) {
            return null;
        }
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
        PsiClass psiClass = psiMethod.getContainingClass();
        //如果不是一个接口，跳过
        if (psiClass == null || !psiClass.isInterface()) {
            return null;
        }
        //查找所有XML
        Collection<VirtualFile> virtualFiles = findProjectFileByType(psiMethod.getProject(), XmlFileType.INSTANCE);
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
     * 根据xml查找class接口
     * @param xmlTag
     * @return
     */
    public static PsiClass findPsiClassByXmlTag(XmlTag xmlTag) {
        if (!"mapper".equals(xmlTag.getName())){
            return null;
        }
        //xml的namespace
        String namespace = xmlTag.getAttributeValue("namespace");
        if (namespace == null || "".equals(namespace)){
            return null;
        }
        //查找项目中所有Java文件
        Collection<VirtualFile> projectAllClass = findProjectFileByType(xmlTag.getProject(), JavaFileType.INSTANCE);
        //未查找到Java文件，跳过
        if (projectAllClass.size() == 0) {
            return null;
        }
        //根据namespace获取class
        PsiClass psiClass = JavaPsiFacade.getInstance(xmlTag.getProject())
                .findClass(namespace, GlobalSearchScope.allScope(xmlTag.getProject()));
        if (psiClass == null || !psiClass.isInterface()) {
            return null;
        }
        return psiClass;
    }

    /**
     * 处理xml中的节点
     * @param subTag
     * @return
     */
    public static XmlTag executeXmlTag(XmlTag subTag, Project project) {
        //获取mapper.xml中的顶级节点
        XmlTag parentTag = subTag.getParentTag();
        if (parentTag == null) {
            return null;
        }
        //新的xmlTag
        XmlTag tagFromText = createXmlTag(subTag.getText(), project);
        //新的xmlTag内部的子节点
        XmlTag[] selectSubTags = tagFromText.getSubTags();
        //mapper.xml中所有子节点
        XmlTag[] allTags = parentTag.getSubTags();
        //查找select的子节点
        for (XmlTag selectSubTag : selectSubTags) {
            if ("include".equals(selectSubTag.getName())) {
                String refId = selectSubTag.getAttributeValue("refid");
                if (refId != null) {
                    for (XmlTag subTag2 : allTags) {
                        String id2 = subTag2.getAttributeValue("id");
                        if (refId.equals(id2)) {
                            //替换include节点
                            selectSubTag.replace(createXmlTag(subTag2.getText(), project));
                        }
                    }
                }
            }
        }
        return tagFromText;
    }

    /**
     * 创建新的XmlTag
     * @param code
     * @param project
     * @return
     */
    public static XmlTag createXmlTag(String code, Project project) {
        XmlElementFactory elementFactory = XmlElementFactory.getInstance(project);
        return elementFactory.createTagFromText(code);
    }

    /**
     * 查找并构建mapper与xml的对应关系
     * @param subTags
     * @param psiElements
     * @return
     */
    public static List<MybatisMethodXmlBO> buildMapperMethodCorrXml(XmlTag[] subTags, PsiElement[] psiElements) {
        if (subTags == null || psiElements == null || subTags.length == 0 || psiElements.length == 0) {
            return Collections.emptyList();
        }
        List<MybatisMethodXmlBO> mybatisMethodXmlBOList = new ArrayList<>();
        for (PsiElement element : psiElements) {
            if (!(element instanceof PsiMethod)) {
                continue;
            }
            //该方法与xml中的子节点匹配
            PsiMethod psiMethod = (PsiMethod) element;
            for (XmlTag subTag : subTags) {
                String id = subTag.getAttributeValue("id");
                if (psiMethod.getName().equals(id)) {
                    MybatisMethodXmlBO mybatisMethodXmlBO = MybatisMethodXmlBO.buildMethodXmlEntity(element, subTag);
                    mybatisMethodXmlBOList.add(mybatisMethodXmlBO);
                }
            }
        }
        return mybatisMethodXmlBOList;
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
        String packageName = PsiElementUtil.getPackageName(containingClass) + "." + containingClass.getName();
        //检查这个class与xml中的namespace是否一致
        String namespace = rootTag.getAttributeValue("namespace");
        //校验是否一致
        if (packageName.equals(namespace)) {
            return rootTag;
        }
        return null;
    }

}
