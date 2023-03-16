package com.china.observer.service.impl;

import com.china.observer.entity.MethodXmlBO;
import com.china.observer.service.MybatisIconGutterService;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;
import java.util.*;
import java.util.stream.Collectors;

/**
 * mybatis xml图标处理
 */
public class MybatisXmlIconGutterServiceImpl implements MybatisIconGutterService {
    @Override
    public List<RelatedItemLineMarkerInfo<PsiElement>> gutterIconHandler(PsiElement element) {
        // 检查元素是否是XML标记
        if (!(element instanceof XmlTag)) {
            return Collections.emptyList();
        }
        // 获取XML标记，不是mapper跳过
        XmlTag xmlTag = (XmlTag) element;
        if (!"mapper".equals(xmlTag.getName())){
            return Collections.emptyList();
        }
        //xml的namespace
        String namespace = xmlTag.getAttributeValue("namespace");
        if (namespace == null || "".equals(namespace)){
            return Collections.emptyList();
        }
        //查找项目中所有Java文件
        Collection<VirtualFile> projectAllXML = FileTypeIndex.getFiles(JavaFileType.INSTANCE,
                GlobalSearchScope.projectScope(xmlTag.getProject()));
        //未查找到Java文件，跳过
        if (projectAllXML.size() == 0) {
            return Collections.emptyList();
        }
        //根据namespace获取class
        PsiClass psiClass = JavaPsiFacade.getInstance(xmlTag.getProject())
                .findClass(namespace, GlobalSearchScope.allScope(xmlTag.getProject()));
        if (psiClass == null || !psiClass.isInterface()) {
            return Collections.emptyList();
        }
        List<MethodXmlBO> methodXmlBOList = new ArrayList<>();
        //接口与xml根节点跳转位置已确定
        MethodXmlBO entity = MethodXmlBO.buildMethodXmlEntity(psiClass, xmlTag);
        methodXmlBOList.add(entity);
        //接口内所有方法
        PsiMethod[] allMethods = psiClass.getAllMethods();
        //存储方法与xml的对应关系
        XmlTag[] subTags = xmlTag.getSubTags();
        for (XmlTag subTag : subTags) {
            String id = subTag.getAttributeValue("id");
            for (PsiMethod method : allMethods) {
                if (method.getName().equals(id)) {
                    MethodXmlBO methodXmlBO = MethodXmlBO.buildMethodXmlEntity(method, subTag);
                    methodXmlBOList.add(methodXmlBO);
                }
            }
        }
        //创建图标
        if (methodXmlBOList.size() > 0) {
            NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementingMethod)
                            .setAlignment(GutterIconRenderer.Alignment.RIGHT);
            return methodXmlBOList.stream()
                    .map(b -> {
                        //目标 -> 接口
                        builder.setTarget(b.getElement());
                        //图标文字
                        builder.setTooltipText("Go To " + ((PsiNameIdentifierOwner) b.getElement()).getName());
                        //创建
                        return builder.createLineMarkerInfo(b.getXml());
                    }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
